package com.setlone.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.MutableLiveData;

import com.setlone.app.C;
import com.setlone.app.entity.ContractType;
import com.setlone.app.entity.GasEstimate;
import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.entity.SignAuthenticationCallback;
import com.setlone.app.entity.TransactionReturn;
import com.setlone.app.entity.Wallet;
import com.setlone.app.entity.tokens.Token;
import com.setlone.app.entity.tokens.TokenInfo;
import com.setlone.app.interact.CreateTransactionInteract;
import com.setlone.app.repository.EthereumNetworkRepositoryType;
import com.setlone.app.repository.TokenRepository;
import com.setlone.app.router.MyAddressRouter;
import com.setlone.app.service.AnalyticsServiceType;
import com.setlone.app.service.AssetDefinitionService;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.service.GasService;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.TokensService;
import com.setlone.app.service.TransactionSendHandlerInterface;
import com.setlone.app.service.WalletAddressService;
import com.setlone.app.repository.TronTransactionRepository;
import com.setlone.app.ui.ImportTokenActivity;
import com.setlone.app.web3.entity.Web3Transaction;
import com.setlone.hardware.SignatureFromKey;
import com.setlone.app.util.TronUtils;

import timber.log.Timber;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class SendViewModel extends BaseViewModel implements TransactionSendHandlerInterface
{
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionError = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final GasService gasService;
    private final AssetDefinitionService assetDefinitionService;
    private final KeyService keyService;
    private final CreateTransactionInteract createTransactionInteract;
    private final WalletAddressService walletAddressService;
    private final TronTransactionRepository tronTransactionRepository;

    @Inject
    public SendViewModel(MyAddressRouter myAddressRouter,
                         EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                         TokensService tokensService,
                         CreateTransactionInteract createTransactionInteract,
                         GasService gasService,
                         AssetDefinitionService assetDefinitionService,
                         KeyService keyService,
                         WalletAddressService walletAddressService,
                         TronTransactionRepository tronTransactionRepository,
                         AnalyticsServiceType analyticsService)
    {
        this.myAddressRouter = myAddressRouter;
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.gasService = gasService;
        this.assetDefinitionService = assetDefinitionService;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.walletAddressService = walletAddressService;
        this.tronTransactionRepository = tronTransactionRepository;
        setAnalyticsService(analyticsService);
    }

    public MutableLiveData<TransactionReturn> transactionFinalised()
    {
        return transactionFinalised;
    }

    public MutableLiveData<TransactionReturn> transactionError()
    {
        return transactionError;
    }

    public void showContractInfo(Context ctx, Wallet wallet, Token token)
    {
        myAddressRouter.open(ctx, wallet, token);
    }

    public NetworkInfo getNetworkInfo(long chainId)
    {
        return networkRepository.getNetworkByChain(chainId);
    }

    public Token getToken(long chainId, String tokenAddress)
    {
        return tokensService.getToken(chainId, tokenAddress);
    }
    
    /**
     * 네트워크에 맞는 지갑 주소 가져오기
     * TRON 네트워크일 경우 TRON 주소를 반환
     */
    public String getAddressForNetwork(String walletAddress, long chainId) {
        if (com.setlone.app.repository.EthereumNetworkBase.isTronNetwork(chainId)) {
            return walletAddressService.getNetworkAddress(walletAddress, chainId);
        }
        return walletAddress;
    }

    public void showImportLink(Context context, String importTxt)
    {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    public void fetchToken(long chainId, String address, String walletAddress)
    {
        tokensService.update(address, chainId, ContractType.NOT_SET)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokenInfo -> gotTokenUpdate(tokenInfo, walletAddress), this::onError).isDisposed();
    }

    private void gotTokenUpdate(TokenInfo tokenInfo, String walletAddress)
    {
        disposable = tokensService.addToken(tokenInfo, walletAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(finalisedToken::postValue, this::onError);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }

    public void startGasCycle(long chainId)
    {
        gasService.startGasPriceCycle(chainId);
    }

    public void onDestroy()
    {
        gasService.stopGasPriceCycle();
    }

    public byte[] getTransactionBytes(Token token, String sendAddress, BigDecimal sendAmount)
    {
        byte[] txBytes;
        if (token.isEthereum())
        {
            txBytes = new byte[0];
        }
        else
        {
            txBytes = TokenRepository.createTokenTransferData(sendAddress, sendAmount.toBigInteger());
        }

        return txBytes;
    }

    public Single<GasEstimate> calculateGasEstimate(Wallet wallet, byte[] transactionBytes, long chainId, String sendAddress, BigDecimal sendAmount)
    {
        return gasService.calculateGasEstimate(transactionBytes, chainId, sendAddress, sendAmount.toBigInteger(), wallet, BigInteger.ZERO);
    }

    public void getAuthentication(Activity activity, Wallet wallet, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }
    
    /**
     * TRON 전송을 위한 니모닉 가져오기
     */
    public void getMnemonic(Activity activity, Wallet wallet, com.setlone.app.entity.CreateWalletCallbackInterface callback)
    {
        keyService.getMnemonic(wallet, activity, callback);
    }

    public void requestSignature(Web3Transaction finalTx, Wallet wallet, long chainId)
    {
        createTransactionInteract.requestSignature(finalTx, wallet, chainId, this);
    }

    public void sendTransaction(Wallet wallet, long chainId, Web3Transaction tx, SignatureFromKey signatureFromKey)
    {
        // TRON 네트워크는 별도 처리
        if (EthereumNetworkBase.isTronNetwork(chainId))
        {
            // TRON은 Web3Transaction 대신 직접 처리
            // 이 메서드는 TRON 전용 sendTronTransaction으로 리다이렉트
            Timber.w("sendTransaction called for TRON network, use sendTronTransaction instead");
            return;
        }
        
        createTransactionInteract.sendTransaction(wallet, chainId, tx, signatureFromKey);
    }
    
    /**
     * TRON 전용 트랜잭션 전송
     * @param wallet 지갑 정보
     * @param toAddress 수신 주소 (TRON Base58)
     * @param amount 전송 금액 (TRX 단위)
     * @param mnemonic 니모닉 문구
     * @return 트랜잭션 해시
     */
    public Single<String> sendTronTransaction(Wallet wallet, String toAddress, BigDecimal amount, String mnemonic)
    {
        // TRX를 SUN으로 변환 (1 TRX = 1,000,000 SUN)
        BigInteger amountSun = amount.multiply(new BigDecimal(1_000_000L)).toBigInteger();
        
        return tronTransactionRepository.sendTransaction(wallet, toAddress, amountSun, mnemonic)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * TRON 네트워크인지 확인
     */
    public boolean isTronNetwork(long chainId)
    {
        return EthereumNetworkBase.isTronNetwork(chainId);
    }

    @Override
    public void transactionFinalised(TransactionReturn txData)
    {
        transactionFinalised.postValue(txData);
    }

    @Override
    public void transactionError(TransactionReturn txError)
    {
        transactionError.postValue(txError);
    }

    public GasService getGasService()
    {
        return gasService;
    }
}
