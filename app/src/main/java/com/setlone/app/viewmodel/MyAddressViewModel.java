package com.setlone.app.viewmodel;

import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.repository.EthereumNetworkRepositoryType;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.service.AssetDefinitionService;
import com.setlone.app.service.TokensService;
import com.setlone.app.service.WalletAddressService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MyAddressViewModel extends BaseViewModel {
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokenService;
    private final AssetDefinitionService assetDefinitionService;
    private final WalletAddressService walletAddressService;

    @Inject
    MyAddressViewModel(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService,
            AssetDefinitionService assetDefinitionService,
            WalletAddressService walletAddressService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenService = tokensService;
        this.assetDefinitionService = assetDefinitionService;
        this.walletAddressService = walletAddressService;
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
     */
    public String getAddressForNetwork(String walletAddress, long chainId) {
        if (com.setlone.app.repository.EthereumNetworkBase.isTronNetwork(chainId)) {
            return walletAddressService.getNetworkAddress(walletAddress, chainId);
        }
        return walletAddress;
    }
}
