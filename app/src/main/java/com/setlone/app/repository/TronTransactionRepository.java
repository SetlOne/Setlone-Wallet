package com.setlone.app.repository;

import com.setlone.app.entity.Wallet;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.TronService;
import com.setlone.app.service.WalletAddressService;
import com.setlone.app.util.TronTransactionHelper;
import com.setlone.app.util.TronUtils;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;

/**
 * TRON 트랜잭션 전용 Repository
 * TRON은 EVM 호환이 아니므로 별도로 처리
 */
public class TronTransactionRepository
{
    private final TronService tronService;
    private final WalletAddressService walletAddressService;
    private final KeyService keyService;
    
    public TronTransactionRepository(
            TronService tronService,
            WalletAddressService walletAddressService,
            KeyService keyService)
    {
        this.tronService = tronService;
        this.walletAddressService = walletAddressService;
        this.keyService = keyService;
    }
    
    /**
     * TRON 트랜잭션 서명 및 전송
     * @param wallet 지갑 정보
     * @param toAddress 수신 주소 (TRON Base58 형식)
     * @param amount 전송 금액 (SUN 단위, 1 TRX = 1,000,000 SUN)
     * @param mnemonic 니모닉 문구 (서명용)
     * @return 트랜잭션 해시
     */
    public Single<String> sendTransaction(Wallet wallet, String toAddress, BigInteger amount, String mnemonic)
    {
        return Single.fromCallable(() -> {
            // TRON 주소 확인
            String fromTronAddress = walletAddressService.getTronAddress(wallet.address);
            if (!TronUtils.isValidTronAddress(fromTronAddress))
            {
                throw new IllegalArgumentException("Invalid TRON address: " + fromTronAddress);
            }
            
            if (!TronUtils.isValidTronAddress(toAddress))
            {
                throw new IllegalArgumentException("Invalid TRON destination address: " + toAddress);
            }
            
            // TRON 프라이빗 키 추출
            String privateKeyHex = TronTransactionHelper.getTronPrivateKey(wallet, mnemonic);
            
            return privateKeyHex;
        })
        .flatMap(privateKeyHex -> {
            // TRON 트랜잭션 전송
            String fromTronAddress = walletAddressService.getTronAddress(wallet.address);
            return tronService.sendTransaction(wallet, toAddress, amount, privateKeyHex);
        })
        .subscribeOn(Schedulers.io());
    }
    
    /**
     * TRON 잔액 조회
     * @param tronAddress TRON 주소 (Base58)
     * @return 잔액 (SUN 단위)
     */
    public Single<BigInteger> getBalance(String tronAddress)
    {
        if (!TronUtils.isValidTronAddress(tronAddress))
        {
            return Single.error(new IllegalArgumentException("Invalid TRON address: " + tronAddress));
        }
        
        return tronService.getBalance(tronAddress)
                .subscribeOn(Schedulers.io());
    }
    
    /**
     * 지갑의 TRON 주소 가져오기
     * @param wallet 지갑 정보
     * @return TRON 주소 (Base58)
     */
    public String getTronAddress(Wallet wallet)
    {
        return walletAddressService.getTronAddress(wallet.address);
    }
}
