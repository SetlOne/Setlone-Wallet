package com.setlone.app.entity;
import com.setlone.app.entity.cryptokeys.KeyEncodingType;
import com.setlone.app.service.KeyService;

public interface ImportWalletCallback
{
    void walletValidated(String address, KeyEncodingType type, KeyService.AuthenticationLevel level);
}
