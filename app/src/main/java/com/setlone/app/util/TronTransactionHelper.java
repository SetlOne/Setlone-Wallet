package com.setlone.app.util;

import com.setlone.app.entity.Wallet;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.WalletAddressService;

import java.math.BigInteger;

import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;

/**
 * TRON 트랜잭션 처리 헬퍼 클래스
 * 프라이빗 키 추출 및 TRON 주소 변환 등 유틸리티 제공
 */
public class TronTransactionHelper
{
    /**
     * 지갑에서 TRON 프라이빗 키 추출
     * @param wallet 지갑 정보
     * @param mnemonic 니모닉 문구
     * @return 프라이빗 키 (16진수 문자열)
     */
    public static String getTronPrivateKey(Wallet wallet, String mnemonic) throws Exception
    {
        if (wallet.type != com.setlone.app.entity.WalletType.HDKEY)
        {
            throw new UnsupportedOperationException("Only HD wallets are supported for TRON");
        }
        
        HDWallet hdWallet = new HDWallet(mnemonic, "");
        PrivateKey tronPk = hdWallet.getKeyForCoin(CoinType.TRON);
        byte[] privateKeyBytes = tronPk.data();
        
        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder hex = new StringBuilder();
        for (byte b : privateKeyBytes)
        {
            hex.append(String.format("%02x", b));
        }
        
        return hex.toString();
    }
    
    /**
     * 지갑의 TRON 주소 가져오기
     * @param wallet 지갑 정보
     * @param walletAddressService 주소 매핑 서비스
     * @return TRON 주소 (Base58)
     */
    public static String getTronAddress(Wallet wallet, WalletAddressService walletAddressService)
    {
        // 먼저 매핑된 주소 확인
        String tronAddress = walletAddressService.getTronAddress(wallet.address);
        
        // 매핑이 없으면 기본 주소 반환 (하위 호환성)
        if (tronAddress == null || tronAddress.equals(wallet.address))
        {
            // TRON 주소가 아닌 경우, 주소 매핑이 없을 수 있음
            return wallet.address; // 임시로 기본 주소 반환
        }
        
        return tronAddress;
    }
    
    /**
     * TRX를 SUN으로 변환
     * @param trx TRX 금액
     * @return SUN 금액
     */
    public static BigInteger trxToSun(BigInteger trx)
    {
        return trx.multiply(BigInteger.valueOf(1_000_000L));
    }
    
    /**
     * SUN을 TRX로 변환
     * @param sun SUN 금액
     * @return TRX 금액
     */
    public static BigInteger sunToTrx(BigInteger sun)
    {
        return sun.divide(BigInteger.valueOf(1_000_000L));
    }
}
