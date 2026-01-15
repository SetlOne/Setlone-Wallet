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
import com.setlone.app.ui.ImportTokenActivity;
import com.setlone.app.web3.entity.Web3Transaction;
import com.setlone.hardware.SignatureFromKey;

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

    @Inject
    public SendViewModel(MyAddressRouter myAddressRouter,
                         EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                         TokensService tokensService,
                         CreateTransactionInteract createTransactionInteract,
                         GasService gasService,
                         AssetDefinitionService assetDefinitionService,
                         KeyService keyService,
                         WalletAddressService walletAddressService,
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

    public void requestSignature(Web3Transaction finalTx, Wallet wallet, long chainId)
    {
        createTransactionInteract.requestSignature(finalTx, wallet, chainId, this);
    }

    public void sendTransaction(Wallet wallet, long chainId, Web3Transaction tx, SignatureFromKey signatureFromKey)
    {
        createTransactionInteract.sendTransaction(wallet, chainId, tx, signatureFromKey);
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
