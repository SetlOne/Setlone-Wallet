package com.setlone.app.viewmodel;

import android.app.Activity;
import android.content.Context;

import com.setlone.app.entity.CreateWalletCallbackInterface;
import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.entity.Wallet;
import com.setlone.app.repository.EthereumNetworkRepositoryType;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.service.AssetDefinitionService;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.TokensService;
import com.setlone.app.service.WalletAddressService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.setlone.ethereum.EthereumNetworkBase.TRON_ID;

@HiltViewModel
public class MyAddressViewModel extends BaseViewModel {
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokenService;
    private final AssetDefinitionService assetDefinitionService;
    private final WalletAddressService walletAddressService;
    private final KeyService keyService;

    @Inject
    MyAddressViewModel(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService,
            AssetDefinitionService assetDefinitionService,
            WalletAddressService walletAddressService,
            KeyService keyService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenService = tokensService;
        this.assetDefinitionService = assetDefinitionService;
        this.walletAddressService = walletAddressService;
        this.keyService = keyService;
    }

    public TokensService getTokenService() {
        return tokenService;
    }

    public NetworkInfo getNetworkByChain(long chainId) {
        return ethereumNetworkRepository.getNetworkByChain(chainId);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
    
    /**
     * 네트워크에 맞는 지갑 주소 가져오기
     * TRON 네트워크일 경우 TRON 주소를 반환
     * 주소가 없으면 기본 주소(ETH 주소)를 반환 (Activity에서 생성 여부 확인)
     */
    public String getAddressForNetwork(String walletAddress, long chainId) {
        if (com.setlone.app.repository.EthereumNetworkBase.isTronNetwork(chainId)) {
            String tronAddress = walletAddressService.getNetworkAddress(walletAddress, chainId);
            // TRON 주소가 없고 기본 주소(ETH 주소)와 같으면 생성되지 않은 것
            // TRON 주소는 T로 시작하므로 이를 확인
            if (tronAddress != null && !tronAddress.equals(walletAddress) && tronAddress.startsWith("T")) {
                return tronAddress;
            }
            // TRON 주소가 없으면 기본 주소 반환 (Activity에서 생성 필요)
            Timber.d("TRON address not found for wallet: %s, returning default address", walletAddress);
            return walletAddress;
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
        if (existingTronAddress != null && !existingTronAddress.equals(wallet.address)) {
            // 이미 TRON 주소가 있으면 콜백 실행
            if (callback != null) {
                callback.run();
            }
            return;
        }

        // TRON 주소가 없으면 니모닉을 가져와서 생성
        keyService.getMnemonic(wallet, activity, new CreateWalletCallbackInterface() {
            @Override
            public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level) {}

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
                // Realm 트랜잭션은 UI 스레드에서 실행할 수 없으므로 백그라운드 스레드로 이동
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
}
