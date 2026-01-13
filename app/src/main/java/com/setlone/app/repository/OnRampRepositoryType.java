package com.setlone.app.repository;

import com.setlone.app.entity.OnRampContract;
import com.setlone.app.entity.tokens.Token;

public interface OnRampRepositoryType {
    String getUri(String address, Token token);

    OnRampContract getContract(Token token);
}
