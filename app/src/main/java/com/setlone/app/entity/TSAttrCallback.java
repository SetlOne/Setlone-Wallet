package com.setlone.app.entity;

import com.setlone.token.entity.TokenScriptResult;

import java.util.List;

public interface TSAttrCallback
{
    void showTSAttributes(List<TokenScriptResult.Attribute> attrs, boolean updateRequired);
}
