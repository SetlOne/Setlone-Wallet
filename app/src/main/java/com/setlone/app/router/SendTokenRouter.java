package com.setlone.app.router;


import android.app.Activity;
import android.content.Intent;

import com.setlone.app.C;
import com.setlone.app.entity.QRResult;
import com.setlone.app.entity.Wallet;
import com.setlone.app.entity.tokens.Token;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.ui.SendActivity;
import com.setlone.app.ui.TronSendActivity;

public class SendTokenRouter {
    public void open(Activity context, String address, String symbol, int decimals, Wallet wallet, Token token, long chainId) {
        // TRON 네트워크인 경우 TronSendActivity 사용
        Class<?> activityClass = EthereumNetworkBase.isTronNetwork(chainId) 
                ? TronSendActivity.class 
                : SendActivity.class;
        
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_AMOUNT, (QRResult)null);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
    }
}
