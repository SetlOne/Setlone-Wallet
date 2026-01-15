package com.setlone.app.viewmodel;

import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.repository.EthereumNetworkRepository;
import com.setlone.app.repository.EthereumNetworkRepositoryType;
import com.setlone.app.repository.PreferenceRepositoryType;
import com.setlone.app.service.AnalyticsServiceType;
import com.setlone.app.service.TokensService;
import com.setlone.app.ui.widget.entity.NetworkItem;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NetworkToggleViewModel extends BaseViewModel
{
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final PreferenceRepositoryType preferenceRepository;

    @Inject
    public NetworkToggleViewModel(EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                                  TokensService tokensService,
                                  PreferenceRepositoryType preferenceRepository,
                                  AnalyticsServiceType analyticsService)
    {
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.preferenceRepository = preferenceRepository;
        setAnalyticsService(analyticsService);
    }

    public NetworkInfo[] getNetworkList()
    {
        return networkRepository.getAvailableNetworkList();
    }

    public void setFilterNetworks(List<Long> selectedItems, boolean hasSelected, boolean shouldBlankUserSelection)
    {
        NetworkInfo activeNetwork = networkRepository.getActiveBrowserNetwork();
        long activeNetworkId = -99;
        if (activeNetwork != null)
        {
            activeNetworkId = networkRepository.getActiveBrowserNetwork().chainId;
        }

        //Mark dappbrowser network as deselected if appropriate
        boolean deselected = true;
        Long[] selectedIds = new Long[selectedItems.size()];
        int index = 0;
        for (Long selectedId : selectedItems)
        {
            // TRON 네트워크는 항상 활성화 상태 유지
            if (EthereumNetworkRepository.isTronNetwork(selectedId))
            {
                // TRON은 항상 활성화
            }
            else if (EthereumNetworkRepository.hasRealValue(selectedId) && activeNetworkId == selectedId)
            {
                deselected = false;
            }
            selectedIds[index++] = selectedId;
        }

        if (deselected) networkRepository.setActiveBrowserNetwork(null);
        networkRepository.setFilterNetworkList(selectedIds);
        tokensService.setupFilter(hasSelected && !shouldBlankUserSelection);

        if (shouldBlankUserSelection) preferenceRepository.blankHasSetNetworkFilters();

        preferenceRepository.commit();
    }

    public NetworkInfo getNetworkByChain(long chainId)
    {
        return networkRepository.getNetworkByChain(chainId);
    }

    public List<NetworkItem> getNetworkList(boolean isMainNet)
    {
        List<NetworkItem> networkList = new ArrayList<>();
        List<Long> filterIds = networkRepository.getSelectedFilters();

        for (NetworkInfo info : getNetworkList())
        {
            if (info != null)
            {
                // TRON 네트워크는 제외 (별도 리스트로 관리)
                if (EthereumNetworkRepository.isTronNetwork(info.chainId))
                {
                    continue;
                }
                // EVM 메인넷 또는 테스트넷 분류
                if (EthereumNetworkRepository.hasRealValue(info.chainId) == isMainNet)
                {
                    networkList.add(new NetworkItem(info.name, info.chainId, filterIds.contains(info.chainId)));
                }
            }
        }

        return networkList;
    }
    
    /**
     * TRON 네트워크 리스트 가져오기
     */
    public List<NetworkItem> getTronNetworkList()
    {
        List<NetworkItem> networkList = new ArrayList<>();
        List<Long> filterIds = networkRepository.getSelectedFilters();

        for (NetworkInfo info : getNetworkList())
        {
            if (info != null && EthereumNetworkRepository.isTronNetwork(info.chainId))
            {
                // TRON 네트워크는 기본적으로 체크되어 있음
                boolean isSelected = filterIds.contains(info.chainId);
                // 필터 리스트가 비어있으면 자동으로 체크 (기본 활성화)
                if (filterIds.isEmpty())
                {
                    isSelected = true;
                }
                networkList.add(new NetworkItem(info.name, info.chainId, isSelected));
            }
        }

        return networkList;
    }

    public void removeCustomNetwork(long chainId)
    {
        networkRepository.removeCustomRPCNetwork(chainId);
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public List<Long> getActiveNetworks()
    {
        return networkRepository.getFilterNetworkList();
    }

    public void setTestnetEnabled(boolean enabled)
    {
        preferenceRepository.setTestnetEnabled(enabled);
    }
}
