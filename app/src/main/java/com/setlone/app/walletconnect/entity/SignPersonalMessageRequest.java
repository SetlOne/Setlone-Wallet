package com.setlone.app.walletconnect.entity;

import com.setlone.token.entity.EthereumMessage;
import com.setlone.token.entity.SignMessageType;
import com.setlone.token.entity.Signable;

public class SignPersonalMessageRequest extends BaseRequest
{
    public SignPersonalMessageRequest(String params)
    {
        super(params, WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE);
    }

    @Override
    public Signable getSignable()
    {
        return new EthereumMessage(getMessage(), "", 0, SignMessageType.SIGN_PERSONAL_MESSAGE);
    }

    @Override
    public Signable getSignable(long callbackId, String origin)
    {
        return new EthereumMessage(getMessage(), origin, callbackId, SignMessageType.SIGN_PERSONAL_MESSAGE);
    }

    @Override
    public String getWalletAddress()
    {
        return params.get(1);
    }
}
