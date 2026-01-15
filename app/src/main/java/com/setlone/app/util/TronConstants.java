package com.setlone.app.util;

/**
 * TRON 네트워크 전용 상수 클래스
 * 이더리움 코드와 분리하여 TRON 전용 상수 관리
 */
public class TronConstants
{
    /**
     * TRON Chain ID (메타데이터용)
     * 실제 트랜잭션 서명에는 사용되지 않으며, UI 및 네트워크 식별에만 사용
     * 실제로는 CoinType.TRON (195) 사용
     */
    public static final long TRON_ID = 728126428L; // 0x2b6653dc
    
    /**
     * TRON CoinType (BIP44)
     * wallet-core에서 실제 주소 생성 및 서명에 사용
     */
    public static final int TRON_COIN_TYPE = 195;
    
    /**
     * TRON Derivation Path
     */
    public static final String TRON_DERIVATION_PATH = "m/44'/195'/0'/0/0";
    
    /**
     * TRON 네트워크 이름
     */
    public static final String TRON_NETWORK_NAME = "TRON";
    
    /**
     * TRON 네이티브 토큰 심볼
     */
    public static final String TRON_SYMBOL = "TRX";
    
    /**
     * TRON 주소 길이 (Base58)
     */
    public static final int TRON_ADDRESS_LENGTH = 34;
    
    /**
     * TRON 주소 접두사
     */
    public static final String TRON_ADDRESS_PREFIX = "T";
    
    /**
     * 1 TRX = 1,000,000 SUN
     */
    public static final long SUN_PER_TRX = 1_000_000L;
    
    /**
     * TRON Explorer URL
     */
    public static final String TRON_EXPLORER_URL = "https://tronscan.org/#/transaction/";
    
    private TronConstants()
    {
        // Utility class - prevent instantiation
    }
}
