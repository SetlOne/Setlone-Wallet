package com.setlone.app.entity;

import static com.setlone.app.repository.EthereumNetworkBase.COVALENT;
import static com.setlone.ethereum.EthereumNetworkBase.GOERLI_ID;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.setlone.app.entity.transactionAPI.TransferFetchType;
import com.setlone.app.repository.EthereumNetworkRepository;
import com.setlone.app.util.Utils;

public class NetworkInfo extends com.setlone.ethereum.NetworkInfo
{
    private final String ETHERSCAN_API = ".etherscan.";
    private final String BLOCKSCOUT_API = "blockscout";
    // Note: Polygon now uses Etherscan V2 API (api.etherscan.io/v2/api?chainid=137)
    // The old api.polygonscan.com is deprecated
    // private final String MATIC_API = "polygonscan"; // Deprecated - no longer used
    private final String OKX_API = "oklink";
    private final String ARBISCAN_API = "https://api.arbiscan";
    private final String PALM_API = "explorer.palm";

    public  String etherscanAPI = null; //This is used by the API call to fetch transactions
    public  String[] rpcUrls;

    public NetworkInfo(
            String name,
            String symbol,
            String[] rpcServerUrl,
            String etherscanUrl,
            long chainId,
            String etherscanAPI,
            boolean isCustom) {
        super(name, symbol, rpcServerUrl[0], etherscanUrl, chainId, isCustom);
        this.etherscanAPI = etherscanAPI;
        this.rpcUrls = rpcServerUrl;
    }

    public NetworkInfo(
            String name,
            String symbol,
            String[] rpcServerUrl,
            String etherscanUrl,
            long chainId,
            String etherscanAPI) {
        super(name, symbol, rpcServerUrl[0], etherscanUrl, chainId, false);
        this.etherscanAPI = etherscanAPI;
        this.rpcUrls = rpcServerUrl;
    }

    public String getShortName()
    {
        int index = this.name.indexOf(" (Test)");
        if (index > 0) return this.name.substring(0, index);
        else if (this.name.length() > 10) return this.symbol;
        else return this.name;
    }

    public TransferFetchType[] getTransferQueriesUsed()
    {
        if (etherscanAPI == null || TextUtils.isEmpty(etherscanAPI) || etherscanAPI.contains(COVALENT))
        {
            return new TransferFetchType[0];
        }
        else if (chainId == GOERLI_ID || etherscanAPI.startsWith(ARBISCAN_API))
        {
            return new TransferFetchType[]{TransferFetchType.ERC_20, TransferFetchType.ERC_721};
        }
        // Etherscan V2 API (api.etherscan.io/v2/api) is used for Ethereum, Polygon, Base, Arbitrum, etc.
        // All these chains use the same endpoint with chainid parameter
        else if (etherscanAPI.contains(ETHERSCAN_API) || etherscanAPI.contains(OKX_API) || etherscanAPI.contains("basescan.org"))
        {
            return new TransferFetchType[]{TransferFetchType.ERC_20, TransferFetchType.ERC_721, TransferFetchType.ERC_1155};
        }
        else if (etherscanAPI.contains(BLOCKSCOUT_API))
        {
            return new TransferFetchType[]{TransferFetchType.ERC_20}; // assume it only supports tokenTx, eg Blockscout, Palm
        }
        else //play it safe, assume other API has ERC20
        {
            return new TransferFetchType[]{TransferFetchType.ERC_20, TransferFetchType.ERC_721};
        }
    }

    @Nullable
    public Uri getEtherscanUri(String transactionHash) {
        if (etherscanUrl != null)
        {
            return Uri.parse(etherscanUrl)
                    .buildUpon()
                    .appendEncodedPath(transactionHash)
                    .build();
        }
        else
        {
            return Uri.EMPTY;
        }
    }

    public Uri getEtherscanAddressUri(String value)
    {
        if (etherscanUrl != null)
        {
            String explorer = etherscanUrl;
            if (Utils.isAddressValid(value))
            {
                explorer = explorer.substring(0, explorer.lastIndexOf("tx/"));
                explorer += "address/";
            }
            else if (!Utils.isTransactionHash(value))
            {
                return Uri.EMPTY;
            }

            return Uri.parse(explorer)
                    .buildUpon()
                    .appendEncodedPath(value)
                    .build();
        }
        else
        {
            return Uri.EMPTY;
        }
    }

    public boolean hasRealValue()
    {
        return EthereumNetworkRepository.hasRealValue(this.chainId);
    }
}
