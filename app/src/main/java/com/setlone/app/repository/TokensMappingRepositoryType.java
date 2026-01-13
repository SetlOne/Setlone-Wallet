package com.setlone.app.repository;

import com.setlone.app.entity.ContractType;
import com.setlone.app.entity.tokendata.TokenGroup;
import com.setlone.token.entity.ContractAddress;

public interface TokensMappingRepositoryType
{
    TokenGroup getTokenGroup(long chainId, String address, ContractType type);

    ContractAddress getBaseToken(long chainId, String address);
}
