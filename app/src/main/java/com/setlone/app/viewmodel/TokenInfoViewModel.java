package com.setlone.app.viewmodel;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.setlone.app.entity.CreateWalletCallbackInterface;
import com.setlone.app.entity.Wallet;
import com.setlone.app.entity.tokens.TokenPerformance;
import com.setlone.app.entity.tokens.TokenPortfolio;
import com.setlone.app.entity.tokens.TokenStats;
import com.setlone.app.service.AssetDefinitionService;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.TokensService;
import com.setlone.app.service.WalletAddressService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@HiltViewModel
public class TokenInfoViewModel extends BaseViewModel {
    private final MutableLiveData<String> marketPrice = new MutableLiveData<>();
    private final MutableLiveData<TokenPortfolio> portfolio = new MutableLiveData<>();
    private final MutableLiveData<TokenPerformance> performance = new MutableLiveData<>();
    private final MutableLiveData<TokenStats> stats = new MutableLiveData<>();

    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final WalletAddressService walletAddressService;
    private final KeyService keyService;
    
    @Nullable
    private Disposable disposable;

    @Inject
    public TokenInfoViewModel(AssetDefinitionService assetDefinitionService,
                              TokensService tokensService,
                              WalletAddressService walletAddressService,
                              KeyService keyService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.walletAddressService = walletAddressService;
        this.keyService = keyService;
    }

    public LiveData<String> marketPrice()
    {
        return marketPrice;
    }

    public LiveData<TokenPortfolio> portfolio()
    {
        return portfolio;
    }

    public LiveData<TokenPerformance> performance()
    {
        return performance;
    }

    public LiveData<TokenStats> stats()
    {
        return stats;
    }

    public TokensService getTokensService() { return tokensService; }

    public AssetDefinitionService getAssetDefinitionService() { return assetDefinitionService; }
    
    /**
     * 네트워크에 맞는 지갑 주소 가져오기
     * TRON 네트워크일 경우 TRON 주소를 반환
     */
    public String getAddressForNetwork(String walletAddress, long chainId)
    {
        if (com.setlone.app.repository.EthereumNetworkBase.isTronNetwork(chainId)) {
            String tronAddress = walletAddressService.getNetworkAddress(walletAddress, chainId);
            // TRON 주소가 있고 유효한 경우 반환
            if (tronAddress != null && !tronAddress.equals(walletAddress) && tronAddress.startsWith("T")) {
                return tronAddress;
            }
        }
        return walletAddress;
    }
    
    /**
     * TRON 주소가 생성되었는지 확인
     */
    public boolean hasTronAddress(String walletAddress) {
        String tronAddress = walletAddressService.getTronAddress(walletAddress);
        return tronAddress != null && !tronAddress.equals(walletAddress) && tronAddress.startsWith("T");
    }
    
    /**
     * TRON 주소가 없을 때 생성 시도
     * @param wallet 지갑 객체
     * @param activity Activity (인증 필요 시 사용)
     * @param callback 생성 완료 콜백
     */
    public void generateTronAddressIfNeeded(Wallet wallet, Activity activity, Runnable callback) {
        // 이미 TRON 주소가 있는지 확인
        String existingTronAddress = walletAddressService.getTronAddress(wallet.address);
        if (existingTronAddress != null && !existingTronAddress.equals(wallet.address) && existingTronAddress.startsWith("T")) {
            // 이미 TRON 주소가 있으면 콜백 실행
            if (callback != null) {
                callback.run();
            }
            return;
        }

        // TRON 주소가 없으면 니모닉을 가져와서 생성
        keyService.getMnemonic(wallet, activity, new CreateWalletCallbackInterface() {
            @Override
            public void HDKeyCreated(String address, android.content.Context ctx, KeyService.AuthenticationLevel level) {}

            @Override
            public void keyFailure(String message) {
                Timber.e("Failed to get mnemonic for TRON address generation: %s", message);
                if (callback != null) {
                    callback.run();
                }
            }

            @Override
            public void cancelAuthentication() {
                Timber.d("Authentication cancelled for TRON address generation");
                if (callback != null) {
                    callback.run();
                }
            }

            @Override
            public void fetchMnemonic(String mnemonic) {
                // 니모닉을 받으면 백그라운드 스레드에서 TRON 주소 생성 및 저장
                disposable = Single.fromCallable(() -> {
                    walletAddressService.generateAndStoreNetworkAddresses(wallet.address, mnemonic);
                    Timber.d("TRON address generated for wallet: %s", wallet.address);
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    success -> {
                        if (callback != null) {
                            callback.run();
                        }
                    },
                    error -> {
                        Timber.e(error, "Failed to generate TRON address for wallet: %s", wallet.address);
                        if (callback != null) {
                            callback.run();
                        }
                    }
                );
            }
        });
    }
    
    @Override
    protected void onCleared() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        super.onCleared();
    }

    public void fetchPortfolio()
    {
        TokenPortfolio tokenPortfolio = new TokenPortfolio();

        // TODO: Do calculations here

        portfolio.postValue(tokenPortfolio);
    }

    public void fetchPerformance()
    {
        TokenPerformance tokenPerformance = new TokenPerformance();

        // TODO: Do calculations here

        performance.postValue(tokenPerformance);
    }

    public void fetchStats()
    {
        TokenStats tokenStats = new TokenStats();

        // TODO: Do calculations here

        stats.postValue(tokenStats);
    }
}
