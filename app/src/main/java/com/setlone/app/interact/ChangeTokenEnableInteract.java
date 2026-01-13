package com.setlone.app.interact;

import com.setlone.app.entity.Wallet;
import com.setlone.app.repository.TokenRepositoryType;
import com.setlone.token.entity.ContractAddress;

import io.reactivex.Completable;

public class ChangeTokenEnableInteract
{
    private final TokenRepositoryType tokenRepository;

    public ChangeTokenEnableInteract(TokenRepositoryType tokenRepository)
    {
        this.tokenRepository = tokenRepository;
    }

    public Completable setEnable(Wallet wallet, ContractAddress cAddr, boolean enabled)
    {
        tokenRepository.setEnable(wallet, cAddr, enabled);
        tokenRepository.setVisibilityChanged(wallet, cAddr);
        return Completable.fromAction(() -> {});
    }
}
