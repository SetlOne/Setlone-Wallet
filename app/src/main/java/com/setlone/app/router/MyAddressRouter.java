package com.setlone.app.router;

import android.content.Context;
import android.content.Intent;

import com.setlone.app.C;
import com.setlone.app.entity.tokens.Token;
import com.setlone.app.entity.Wallet;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.ui.MyAddressActivity;

public class MyAddressRouter {

    public void open(Context context, Wallet wallet) {
        Intent intent = new Intent(context, MyAddressActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public void open(Context context, Wallet wallet, Token token) {
        // TRON 네트워크인 경우에도 MyAddressActivity 사용 (이미 TRON 처리 로직 포함)
        Intent intent = new Intent(context, MyAddressActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    /**
     * 현재 선택된 네트워크의 chainId를 전달하여 주소 화면 열기
     * TRON 네트워크일 경우 TronAddressActivity 사용
     */
    public void open(Context context, Wallet wallet, long chainId) {
        // TRON 네트워크인 경우 TronAddressActivity 사용
        Class<?> activityClass = EthereumNetworkBase.isTronNetwork(chainId) 
                ? com.setlone.app.ui.TronAddressActivity.class 
                : MyAddressActivity.class;
        
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }
}
