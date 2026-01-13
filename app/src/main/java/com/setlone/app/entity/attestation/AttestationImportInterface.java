package com.setlone.app.entity.attestation;

import com.setlone.app.entity.tokens.TokenCardMeta;

public interface AttestationImportInterface
{
    void attestationImported(TokenCardMeta newToken);
    void importError(String error);
    void smartPassValidation(SmartPassReturn validation);
}
