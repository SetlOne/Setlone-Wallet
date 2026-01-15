package com.setlone.app.util;

import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;

/**
 * TRON 주소 생성 유틸리티
 * wallet-core의 CoinType.TRON 지원 여부를 확인하고,
 * 지원하지 않으면 Trident SDK나 직접 구현 필요
 */
public class TronAddressGenerator
{
    /**
     * 니모닉에서 TRON 주소 생성
     * @param mnemonic 니모닉 문구
     * @return TRON 주소 (Base58 형식)
     */
    public static String generateTronAddressFromMnemonic(String mnemonic) throws Exception
    {
        HDWallet wallet = new HDWallet(mnemonic, "");
        
        // wallet-core가 TRON을 지원하는지 확인
        try
        {
            PrivateKey pk = wallet.getKeyForCoin(CoinType.TRON);
            String address = CoinType.TRON.deriveAddress(pk);
            return address;
        }
        catch (Exception e)
        {
            // wallet-core가 TRON을 지원하지 않는 경우
            throw new UnsupportedOperationException(
                    "wallet-core does not support TRON. Trident SDK integration required. " +
                    "Error: " + e.getMessage());
        }
    }

    /**
     * 프라이빗 키에서 TRON 주소 생성
     * @param privateKeyHex 16진수 프라이빗 키
     * @return TRON 주소 (Base58 형식)
     */
    public static String generateTronAddressFromPrivateKey(String privateKeyHex) throws Exception
    {
        // wallet-core 4.5.0에서 TRON 지원 확인
        try
        {
            // 프라이빗 키를 PrivateKey 객체로 변환
            byte[] privateKeyBytes = hexStringToByteArray(privateKeyHex);
            PrivateKey privateKey = new PrivateKey(privateKeyBytes);
            
            // TRON 주소 생성
            return CoinType.TRON.deriveAddress(privateKey);
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(
                    "Failed to generate TRON address from private key. " +
                    "wallet-core TRON support may not be available. Error: " + e.getMessage());
        }
    }

    /**
     * 16진수 문자열을 바이트 배열로 변환
     */
    private static byte[] hexStringToByteArray(String hex)
    {
        // 0x 접두사 제거
        if (hex.startsWith("0x") || hex.startsWith("0X"))
        {
            hex = hex.substring(2);
        }
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
