package com.setlone.app.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.entity.Wallet;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.repository.EthereumNetworkRepositoryType;
import com.setlone.app.util.TronConstants;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;
// TODO: wallet-core 4.5.0에서 AnySigner 확인 필요
// import wallet.core.jni.AnySigner;
import wallet.core.jni.CoinType;
// TODO: wallet-core 4.5.0에서 Tron protobuf 지원 확인 필요
// import wallet.core.jni.proto.Tron;

/**
 * TRON 네트워크 전용 서비스 클래스
 * TRON HTTP API를 직접 호출하여 잔액 조회, 트랜잭션 전송 등을 처리
 * 
 * 참고: TRON은 EVM 호환이 아니므로 별도의 처리가 필요합니다.
 * 
 * 현재 구현:
 * - NetworkInfo의 rpcUrls를 사용하여 동적으로 엔드포인트 선택
 * - 엔드포인트 실패 시 자동으로 백업 엔드포인트로 전환
 * - wallet.core를 사용한 주소 생성 및 트랜잭션 서명
 * 
 * 향후 개선: Trident SDK 통합 시 이 클래스를 리팩토링하여 SDK 사용
 * - Trident SDK: io.github.tronprotocol:trident (버전 확인 필요)
 * - GitHub: https://github.com/tronprotocol/trident
 * - 참고: https://github.com/exxuslee/Tron, https://github.com/centerprime/Tron-Android-SDK
 */
public class TronService
{
    private static final String TAG = "TronService";
    private static final BigInteger SUN_PER_TRX = BigInteger.valueOf(1_000_000L); // 1 TRX = 1,000,000 SUN
    
    // QuickNode Rate Limiting: 초당 15회 제한
    // 최소 요청 간격: 1000ms / 15 = 약 67ms, 안전을 위해 70ms로 설정
    private static final long QUICKNODE_MIN_REQUEST_INTERVAL_MS = 70L;
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final EthereumNetworkRepositoryType networkRepository;
    
    // Rate limiting을 위한 마지막 요청 시간 추적 (엔드포인트별)
    private final java.util.Map<String, Long> lastRequestTime = new java.util.concurrent.ConcurrentHashMap<>();

    public TronService(OkHttpClient httpClient, Gson gson, EthereumNetworkRepositoryType networkRepository)
    {
        this.httpClient = httpClient;
        this.gson = gson;
        this.networkRepository = networkRepository;
    }
    
    /**
     * QuickNode 엔드포인트인지 확인
     */
    private boolean isQuickNodeEndpoint(String baseUrl)
    {
        return baseUrl != null && baseUrl.contains("quiknode.pro");
    }
    
    /**
     * Rate limiting 적용: QuickNode 엔드포인트의 경우 초당 15회 제한
     * @param baseUrl 엔드포인트 URL
     */
    private void applyRateLimit(String baseUrl) throws InterruptedException
    {
        if (isQuickNodeEndpoint(baseUrl))
        {
            Long lastTime = lastRequestTime.get(baseUrl);
            long currentTime = System.currentTimeMillis();
            
            if (lastTime != null)
            {
                long timeSinceLastRequest = currentTime - lastTime;
                if (timeSinceLastRequest < QUICKNODE_MIN_REQUEST_INTERVAL_MS)
                {
                    long waitTime = QUICKNODE_MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                    Timber.d("Rate limiting: waiting %d ms for QuickNode endpoint", waitTime);
                    Thread.sleep(waitTime);
                }
            }
            
            lastRequestTime.put(baseUrl, System.currentTimeMillis());
        }
    }
    
    /**
     * TRON 네트워크의 API 엔드포인트 가져오기
     * NetworkInfo의 rpcUrls 배열에서 첫 번째 엔드포인트 반환
     * 실패 시 백업 엔드포인트로 자동 전환
     */
    private String getTronApiBaseUrl()
    {
        NetworkInfo networkInfo = EthereumNetworkBase.getNetworkInfo(TronConstants.TRON_ID);
        if (networkInfo != null && networkInfo.rpcUrls != null && networkInfo.rpcUrls.length > 0)
        {
            return networkInfo.rpcUrls[0]; // 첫 번째 엔드포인트 사용
        }
        // 기본값 (fallback)
        return "https://api.trongrid.io";
    }
    
    /**
     * TRON 네트워크의 모든 백업 엔드포인트 가져오기
     */
    private String[] getTronApiEndpoints()
    {
        NetworkInfo networkInfo = EthereumNetworkBase.getNetworkInfo(TronConstants.TRON_ID);
        if (networkInfo != null && networkInfo.rpcUrls != null && networkInfo.rpcUrls.length > 0)
        {
            return networkInfo.rpcUrls;
        }
        // 기본값 (fallback)
        return new String[]{"https://api.trongrid.io", "https://tron.drpc.org", "https://tron-rpc.publicnode.com"};
    }

    /**
     * TRON 주소의 잔액 조회 (SUN 단위, 1 TRX = 1,000,000 SUN)
     * API: GET {baseUrl}/v1/accounts/{address}
     * 엔드포인트 실패 시 자동으로 백업 엔드포인트로 전환
     */
    public Single<BigInteger> getBalance(String tronAddress)
    {
        return Single.fromCallable(() -> {
            String[] endpoints = getTronApiEndpoints();
            Exception lastException = null;
            
            for (String baseUrl : endpoints)
            {
                try
                {
                    // Rate limiting 적용 (QuickNode의 경우)
                    applyRateLimit(baseUrl);
                    
                    String url = baseUrl + "/v1/accounts/" + tronAddress;
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .build();

                    try (Response response = httpClient.newCall(request).execute())
                    {
                        if (!response.isSuccessful() || response.body() == null)
                        {
                            Timber.w("Failed to get TRON balance from %s: %s", baseUrl, response.code());
                            continue; // 다음 엔드포인트 시도
                        }

                        String responseBody = response.body().string();
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        
                        // TRON API 응답 구조: { "data": [{ "balance": 1000000 }] }
                        if (json.has("data") && json.getAsJsonArray("data").size() > 0)
                        {
                            JsonObject accountData = json.getAsJsonArray("data").get(0).getAsJsonObject();
                            if (accountData.has("balance"))
                            {
                                long balance = accountData.get("balance").getAsLong();
                                Timber.d("TRON balance retrieved from %s: %d SUN", baseUrl, balance);
                                return BigInteger.valueOf(balance);
                            }
                        }
                        
                        // 응답은 성공했지만 balance가 없는 경우 (0 잔액)
                        return BigInteger.ZERO;
                    }
                }
                catch (Exception e)
                {
                    Timber.w(e, "Error getting TRON balance from %s", baseUrl);
                    lastException = e;
                    continue; // 다음 엔드포인트 시도
                }
            }
            
            // 모든 엔드포인트 실패
            Timber.e(lastException, "All TRON endpoints failed for balance query");
            return BigInteger.ZERO;
        })
        .subscribeOn(Schedulers.io());
    }

    /**
     * 최신 블록 정보 가져오기 (RefBlockBytes, RefBlockHash 생성용)
     * 엔드포인트 실패 시 자동으로 백업 엔드포인트로 전환
     * @return RefBlockInfo (refBlockBytes, refBlockHash 포함)
     */
    public Single<RefBlockInfo> getLatestBlock()
    {
        return Single.fromCallable(() -> {
            String[] endpoints = getTronApiEndpoints();
            Exception lastException = null;
            
            for (String baseUrl : endpoints)
            {
                try
                {
                    // Rate limiting 적용 (QuickNode의 경우)
                    applyRateLimit(baseUrl);
                    
                    String url = baseUrl + "/wallet/getnowblock";
                    Request request = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create("{}", MediaType.parse("application/json")))
                            .build();

                    try (Response response = httpClient.newCall(request).execute())
                    {
                        if (!response.isSuccessful() || response.body() == null)
                        {
                            Timber.w("Failed to get latest block from %s: %s", baseUrl, response.code());
                            continue; // 다음 엔드포인트 시도
                        }

                        String responseBody = response.body().string();
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        
                        // 블록 번호와 해시 추출
                        long blockNumber = json.get("block_header").getAsJsonObject()
                                .get("raw_data").getAsJsonObject()
                                .get("number").getAsLong();
                        String blockHash = json.get("blockID").getAsString();
                        
                        // RefBlockBytes: 블록 번호의 마지막 2바이트 (little-endian)
                        byte[] refBlockBytes = ByteBuffer.allocate(2)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .putShort((short) (blockNumber & 0xFFFF))
                                .array();
                        
                        // RefBlockHash: 블록 해시의 중간 8바이트 (역순)
                        byte[] refBlockHash = hexStringToByteArray(blockHash.substring(16, 32));
                        
                        Timber.d("Latest block retrieved from %s: %d", baseUrl, blockNumber);
                        return new RefBlockInfo(refBlockBytes, refBlockHash);
                    }
                }
                catch (Exception e)
                {
                    Timber.w(e, "Error getting latest block from %s", baseUrl);
                    lastException = e;
                    continue; // 다음 엔드포인트 시도
                }
            }
            
            // 모든 엔드포인트 실패
            Timber.e(lastException, "All TRON endpoints failed for latest block");
            throw new Exception("Failed to get latest block from all endpoints", lastException);
        })
        .subscribeOn(Schedulers.io());
    }
    
    /**
     * TRON 트랜잭션 생성 및 서명
     * TRON API를 사용하여 트랜잭션을 생성하고, wallet.core의 PrivateKey로 서명
     * 
     * @param fromAddress 발신 주소 (TRON Base58)
     * @param toAddress 수신 주소 (TRON Base58)
     * @param amount 전송 금액 (SUN 단위)
     * @param privateKeyHex 프라이빗 키 (16진수)
     * @return 서명된 트랜잭션 (JSON 문자열, 브로드캐스트용)
     */
    public Single<String> signTransaction(String fromAddress, String toAddress, BigInteger amount, String privateKeyHex)
    {
        return createTransaction(fromAddress, toAddress, amount)
                .flatMap(transactionJson -> {
                    try
                    {
                        // 1. 트랜잭션 JSON에서 raw_data_hex 추출
                        JsonObject txJson = gson.fromJson(transactionJson, JsonObject.class);
                        if (!txJson.has("raw_data_hex"))
                        {
                            return Single.error(new Exception("Failed to get raw_data_hex from transaction"));
                        }
                        
                        String rawDataHex = txJson.get("raw_data_hex").getAsString();
                        byte[] rawData = hexStringToByteArray(rawDataHex);
                        
                        // 2. raw_data의 SHA256 해시 계산
                        byte[] txHash = wallet.core.jni.Hash.sha256(rawData);
                        
                        // 3. ECDSA 서명 (wallet.core의 PrivateKey 사용)
                        byte[] privateKeyBytes = hexStringToByteArray(privateKeyHex);
                        wallet.core.jni.PrivateKey privateKey = new wallet.core.jni.PrivateKey(privateKeyBytes);
                        byte[] signature = privateKey.sign(txHash, wallet.core.jni.Curve.SECP256K1);
                        
                        // 4. 서명을 Base64로 인코딩하여 트랜잭션에 추가
                        String signatureBase64 = java.util.Base64.getEncoder().encodeToString(signature);
                        
                        // 5. 서명된 트랜잭션 JSON 구성
                        JsonObject signedTx = txJson.deepCopy();
                        com.google.gson.JsonArray signatures = new com.google.gson.JsonArray();
                        signatures.add(signatureBase64);
                        signedTx.add("signature", signatures);
                        
                        Timber.d("TRON transaction signed successfully");
                        return Single.just(signedTx.toString());
                    }
                    catch (Exception e)
                    {
                        Timber.e(e, "Error signing TRON transaction");
                        return Single.error(e);
                    }
                })
                .subscribeOn(Schedulers.io());
    }
    
    /**
     * TRON API를 사용하여 트랜잭션 생성
     * API: POST {baseUrl}/wallet/createtransaction
     * 엔드포인트 실패 시 자동으로 백업 엔드포인트로 전환
     */
    private Single<String> createTransaction(String fromAddress, String toAddress, BigInteger amount)
    {
        return Single.fromCallable(() -> {
            String[] endpoints = getTronApiEndpoints();
            Exception lastException = null;
            
            // 요청 본문 구성
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("owner_address", fromAddress);
            requestBody.addProperty("to_address", toAddress);
            requestBody.addProperty("amount", amount.longValue());
            String requestBodyStr = requestBody.toString();
            
            for (String baseUrl : endpoints)
            {
                try
                {
                    // Rate limiting 적용 (QuickNode의 경우)
                    applyRateLimit(baseUrl);
                    
                    String url = baseUrl + "/wallet/createtransaction";
                    Request request = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(
                                    requestBodyStr,
                                    MediaType.parse("application/json")))
                            .build();
                    
                    try (Response response = httpClient.newCall(request).execute())
                    {
                        if (!response.isSuccessful() || response.body() == null)
                        {
                            Timber.w("Failed to create transaction from %s: %s", baseUrl, response.code());
                            continue; // 다음 엔드포인트 시도
                        }
                        
                        String responseBody = response.body().string();
                        Timber.d("TRON transaction created from %s: %s", baseUrl, responseBody);
                        return responseBody;
                    }
                }
                catch (Exception e)
                {
                    Timber.w(e, "Error creating TRON transaction from %s", baseUrl);
                    lastException = e;
                    continue; // 다음 엔드포인트 시도
                }
            }
            
            // 모든 엔드포인트 실패
            Timber.e(lastException, "All TRON endpoints failed for transaction creation");
            throw new Exception("Failed to create transaction from all endpoints", lastException);
        })
        .subscribeOn(Schedulers.io());
    }
    
    /**
     * TRON 트랜잭션 전송
     * @param fromTronAddress 발신 주소 (TRON Base58 형식)
     * @param toAddress 수신 주소 (TRON Base58 형식)
     * @param amount 전송 금액 (SUN 단위)
     * @param privateKeyHex 서명용 프라이빗 키 (16진수)
     * @return 트랜잭션 해시 (txID)
     */
    public Single<String> sendTransaction(String fromTronAddress, String toAddress, BigInteger amount, String privateKeyHex)
    {
        return signTransaction(fromTronAddress, toAddress, amount, privateKeyHex)
                .flatMap(signedTxJson -> {
                    // 서명된 트랜잭션 브로드캐스트
                    // 엔드포인트 실패 시 자동으로 백업 엔드포인트로 전환
                    String[] endpoints = getTronApiEndpoints();
                    Exception lastException = null;
                    
                    for (String baseUrl : endpoints)
                    {
                        try
                        {
                            // Rate limiting 적용 (QuickNode의 경우)
                            applyRateLimit(baseUrl);
                            
                            String url = baseUrl + "/wallet/broadcasttransaction";
                            Request request = new Request.Builder()
                                    .url(url)
                                    .post(RequestBody.create(
                                            signedTxJson,
                                            MediaType.parse("application/json")))
                                    .build();
                            
                            try (Response response = httpClient.newCall(request).execute())
                            {
                                if (!response.isSuccessful() || response.body() == null)
                                {
                                    Timber.w("Failed to broadcast transaction from %s: %s", baseUrl, response.code());
                                    continue; // 다음 엔드포인트 시도
                                }
                                
                                String responseBody = response.body().string();
                                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                                
                                Timber.d("TRON broadcast response from %s: %s", baseUrl, responseBody);
                                
                                if (json.has("result") && json.get("result").getAsBoolean())
                                {
                                    // 트랜잭션 해시 (txID) 반환
                                    String txID = json.has("txid") ? json.get("txid").getAsString() : "";
                                    if (txID.isEmpty() && json.has("txID"))
                                    {
                                        txID = json.get("txID").getAsString();
                                    }
                                    Timber.d("TRON transaction broadcasted successfully from %s: %s", baseUrl, txID);
                                    return Single.just(txID);
                                }
                                else
                                {
                                    String error = json.has("Error") ? json.get("Error").getAsString() : 
                                                  json.has("message") ? json.get("message").getAsString() : "Unknown error";
                                    Timber.w("Transaction broadcast failed from %s: %s", baseUrl, error);
                                    continue; // 다음 엔드포인트 시도
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            Timber.w(e, "Error broadcasting TRON transaction from %s", baseUrl);
                            lastException = e;
                            continue; // 다음 엔드포인트 시도
                        }
                    }
                    
                    // 모든 엔드포인트 실패
                    Timber.e(lastException, "All TRON endpoints failed for transaction broadcast");
                    return Single.error(new Exception("Failed to broadcast transaction from all endpoints", lastException));
                })
                .subscribeOn(Schedulers.io());
    }
    
    /**
     * RefBlock 정보 클래스
     */
    public static class RefBlockInfo
    {
        public final byte[] refBlockBytes;  // 2 bytes
        public final byte[] refBlockHash;    // 8 bytes
        
        public RefBlockInfo(byte[] refBlockBytes, byte[] refBlockHash)
        {
            this.refBlockBytes = refBlockBytes;
            this.refBlockHash = refBlockHash;
        }
    }
    
    /**
     * 16진수 문자열을 바이트 배열로 변환
     */
    private byte[] hexStringToByteArray(String hex)
    {
        if (hex.startsWith("0x") || hex.startsWith("0X"))
        {
            hex = hex.substring(2);
        }
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private String bytesToHex(byte[] bytes)
    {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes)
        {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Base58 주소를 hex로 변환 (간단한 구현, 실제로는 Base58 디코딩 필요)
     * TODO: 실제 Base58 디코딩 구현 필요
     */
    private com.google.protobuf.ByteString hexStringToBase58Address(String address)
    {
        // Base58 주소를 hex로 변환하는 로직 필요
        // 임시로 주소를 그대로 사용 (실제 구현 필요)
        return com.google.protobuf.ByteString.copyFrom(address.getBytes());
    }

    /**
     * TRON 네트워크 정보 조회
     */
    public Single<TronNetworkInfo> getNetworkInfo()
    {
        return Single.fromCallable(() -> {
            // TODO: TRON API를 사용하여 네트워크 정보 조회
            return new TronNetworkInfo();
        });
    }

    /**
     * TRON 네트워크 정보 클래스
     */
    public static class TronNetworkInfo
    {
        public long latestBlockNumber;
        public long blockTime;
        public String chainId;
    }
}
