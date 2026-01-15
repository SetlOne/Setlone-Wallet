package com.setlone.app.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.setlone.app.entity.Wallet;

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
 * 향후 개선: Trident SDK 통합 시 이 클래스를 리팩토링하여 SDK 사용
 * - Trident SDK: io.github.tronprotocol:trident (버전 확인 필요)
 * - GitHub: https://github.com/tronprotocol/trident
 */
public class TronService
{
    private static final String TAG = "TronService";
    private static final String TRON_API_BASE_URL = "https://api.trongrid.io";
    private static final BigInteger SUN_PER_TRX = BigInteger.valueOf(1_000_000L); // 1 TRX = 1,000,000 SUN
    
    private final OkHttpClient httpClient;
    private final Gson gson;

    public TronService(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * TRON 주소의 잔액 조회 (SUN 단위, 1 TRX = 1,000,000 SUN)
     * API: GET https://api.trongrid.io/v1/accounts/{address}
     */
    public Single<BigInteger> getBalance(String tronAddress)
    {
        return Single.fromCallable(() -> {
            String url = TRON_API_BASE_URL + "/v1/accounts/" + tronAddress;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful() || response.body() == null)
                {
                    Timber.e("Failed to get TRON balance: %s", response.code());
                    return BigInteger.ZERO;
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
                        return BigInteger.valueOf(balance);
                    }
                }
                
                return BigInteger.ZERO;
            }
            catch (Exception e)
            {
                Timber.e(e, "Error getting TRON balance for: %s", tronAddress);
                return BigInteger.ZERO;
            }
        })
        .subscribeOn(Schedulers.io());
    }

    /**
     * 최신 블록 정보 가져오기 (RefBlockBytes, RefBlockHash 생성용)
     * @return RefBlockInfo (refBlockBytes, refBlockHash 포함)
     */
    public Single<RefBlockInfo> getLatestBlock()
    {
        return Single.fromCallable(() -> {
            String url = TRON_API_BASE_URL + "/wallet/getnowblock";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("{}", MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful() || response.body() == null)
                {
                    throw new Exception("Failed to get latest block: " + response.code());
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
                
                return new RefBlockInfo(refBlockBytes, refBlockHash);
            }
            catch (Exception e)
            {
                Timber.e(e, "Error getting latest block");
                throw e;
            }
        })
        .subscribeOn(Schedulers.io());
    }
    
    /**
     * TRON 트랜잭션 서명
     * @param fromAddress 발신 주소 (TRON Base58)
     * @param toAddress 수신 주소 (TRON Base58)
     * @param amount 전송 금액 (SUN 단위)
     * @param privateKeyHex 프라이빗 키 (16진수)
     * @return 서명된 트랜잭션 (hex)
     * 
     * TODO: wallet-core 4.5.0에서 AnySigner 및 Tron protobuf 지원 확인 필요
     * 현재는 구현 보류 - Trident SDK 또는 직접 구현 필요
     */
    public Single<String> signTransaction(String fromAddress, String toAddress, BigInteger amount, String privateKeyHex)
    {
        return Single.error(new UnsupportedOperationException(
                "TRON transaction signing is not yet fully implemented. " +
                "Requires wallet-core AnySigner support or Trident SDK integration. " +
                "Please check wallet-core 4.5.0 documentation for TRON signing support."));
        
        /* TODO: wallet-core AnySigner 확인 후 활성화
        return getLatestBlock()
                .map(refBlockInfo -> {
                    // wallet-core의 AnySigner를 사용하여 TRON 트랜잭션 서명
                    Tron.SigningInput.Builder inputBuilder = Tron.SigningInput.newBuilder();
                    
                    // TransferContract 생성
                    Tron.TransferContract transferContract = Tron.TransferContract.newBuilder()
                            .setOwnerAddress(hexStringToBase58Address(fromAddress))
                            .setToAddress(hexStringToBase58Address(toAddress))
                            .setAmount(amount.longValue())
                            .build();
                    
                    // Transaction 생성
                    Tron.Transaction.Builder txBuilder = Tron.Transaction.newBuilder();
                    Tron.Transaction.Contract contract = Tron.Transaction.Contract.newBuilder()
                            .setType(Tron.Transaction.Contract.ContractType.TransferContract)
                            .setParameter(transferContract.toByteString())
                            .build();
                    
                    txBuilder.addContract(contract);
                    
                    // RefBlock 설정
                    Tron.Transaction.BlockHeader.Builder blockHeader = Tron.Transaction.BlockHeader.newBuilder();
                    blockHeader.setRefBlockBytes(com.google.protobuf.ByteString.copyFrom(refBlockInfo.refBlockBytes));
                    blockHeader.setRefBlockHash(com.google.protobuf.ByteString.copyFrom(refBlockInfo.refBlockHash));
                    blockHeader.setTimestamp(System.currentTimeMillis());
                    
                    Tron.Transaction.raw.Builder rawBuilder = Tron.Transaction.raw.newBuilder();
                    rawBuilder.setRefBlockBytes(com.google.protobuf.ByteString.copyFrom(refBlockInfo.refBlockBytes));
                    rawBuilder.setRefBlockHash(com.google.protobuf.ByteString.copyFrom(refBlockInfo.refBlockHash));
                    rawBuilder.setTimestamp(System.currentTimeMillis());
                    rawBuilder.addContract(contract);
                    
                    Tron.Transaction transaction = Tron.Transaction.newBuilder()
                            .setRawData(rawBuilder.build())
                            .build();
                    
                    inputBuilder.setTransaction(transaction);
                    inputBuilder.setPrivateKey(com.google.protobuf.ByteString.copyFrom(hexStringToByteArray(privateKeyHex)));
                    
                    // AnySigner로 서명
                    byte[] signedTx = AnySigner.sign(inputBuilder.build().toByteArray(), CoinType.TRON);
                    
                    // 서명된 트랜잭션을 hex 문자열로 변환
                    return bytesToHex(signedTx);
                })
                .subscribeOn(Schedulers.io());
        */
    }
    
    /**
     * TRON 트랜잭션 전송
     * @param wallet 지갑 정보
     * @param toAddress 수신 주소 (TRON Base58 형식)
     * @param amount 전송 금액 (SUN 단위)
     * @param privateKeyHex 서명용 프라이빗 키 (16진수)
     * @return 트랜잭션 해시
     * 
     * TODO: signTransaction 구현 완료 후 활성화
     */
    public Single<String> sendTransaction(Wallet wallet, String toAddress, BigInteger amount, String privateKeyHex)
    {
        return Single.error(new UnsupportedOperationException(
                "TRON transaction sending is not yet fully implemented. " +
                "Requires wallet-core AnySigner support or Trident SDK integration. " +
                "Please check wallet-core 4.5.0 documentation for TRON signing support."));
        
        /* TODO: signTransaction 구현 완료 후 활성화
        return signTransaction(wallet.address, toAddress, amount, privateKeyHex)
                .flatMap(signedTxHex -> {
                    // 서명된 트랜잭션 브로드캐스트
                    String url = TRON_API_BASE_URL + "/wallet/broadcasttransaction";
                    String requestBody = "{\"txID\":\"" + signedTxHex + "\",\"raw_data\":{}}";
                    
                    Request request = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                            .build();
                    
                    try (Response response = httpClient.newCall(request).execute())
                    {
                        if (!response.isSuccessful() || response.body() == null)
                        {
                            throw new Exception("Failed to broadcast transaction: " + response.code());
                        }
                        
                        String responseBody = response.body().string();
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        
                        if (json.has("result") && json.get("result").getAsBoolean())
                        {
                            // 트랜잭션 해시 반환
                            return Single.just(signedTxHex);
                        }
                        else
                        {
                            String error = json.has("Error") ? json.get("Error").getAsString() : "Unknown error";
                            throw new Exception("Transaction broadcast failed: " + error);
                        }
                    }
                    catch (Exception e)
                    {
                        Timber.e(e, "Error broadcasting TRON transaction");
                        return Single.error(e);
                    }
                })
                .subscribeOn(Schedulers.io());
        */
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
