package com.setlone.app.service;

import com.setlone.app.entity.Wallet;
import com.setlone.app.service.RealmManager;
import com.setlone.app.repository.entity.RealmWalletAddressMapping;
import com.setlone.app.util.TronAddressGenerator;
import com.setlone.app.util.TronConstants;
import com.setlone.app.util.TronUtils;

import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;
import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;

import java.util.HashMap;
import java.util.Map;

/**
 * 네트워크별 지갑 주소 매핑 서비스
 * 같은 니모닉에서 파생된 여러 네트워크의 주소를 관리
 */
public class WalletAddressService
{
    private final RealmManager realmManager;
    
    public WalletAddressService(RealmManager realmManager)
    {
        this.realmManager = realmManager;
    }
    
    /**
     * 지갑 주소에 대한 모든 네트워크 주소 생성 및 저장
     * @param walletAddress 기본 지갑 주소 (ETH 주소)
     * @param mnemonic 니모닉 문구
     */
    public void generateAndStoreNetworkAddresses(String walletAddress, String mnemonic)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransaction(r -> {
                try
                {
                    HDWallet wallet = new HDWallet(mnemonic, "");
                    
                    // ETH 주소 저장 (기본)
                    storeNetworkAddress(r, walletAddress, 1L, walletAddress); // MAINNET_ID = 1
                    
                    // TRON 주소 생성 및 저장
                    try
                    {
                        PrivateKey tronPk = wallet.getKeyForCoin(CoinType.TRON);
                        String tronAddress = CoinType.TRON.deriveAddress(tronPk);
                        Timber.d("Generated TRON address: %s for wallet: %s (chainId: %d)", tronAddress, walletAddress, TronConstants.TRON_ID);
                        storeNetworkAddress(r, walletAddress, TronConstants.TRON_ID, tronAddress);
                        Timber.d("TRON address stored successfully: %s for wallet: %s", tronAddress, walletAddress);
                    }
                    catch (Exception e)
                    {
                        Timber.e(e, "Failed to generate TRON address for wallet: %s", walletAddress);
                    }
                    
                    // 다른 주요 네트워크 주소도 생성 가능
                    // 필요시 추가 네트워크 주소 생성 로직 추가
                }
                catch (Exception e)
                {
                    Timber.e(e, "Error generating network addresses for wallet: %s", walletAddress);
                }
            });
        }
    }
    
    /**
     * 네트워크별 주소 저장
     * 주소는 소문자로 정규화하여 저장 (대소문자 불일치 문제 방지)
     */
    private void storeNetworkAddress(Realm realm, String walletAddress, long chainId, String networkAddress)
    {
        // EVM 주소는 소문자로 정규화 (TRON 주소는 그대로 유지)
        String normalizedWalletAddress = walletAddress;
        if (walletAddress != null && walletAddress.startsWith("0x")) {
            normalizedWalletAddress = walletAddress.toLowerCase();
        }
        
        String key = normalizedWalletAddress + "-" + chainId;
        RealmWalletAddressMapping mapping = realm.where(RealmWalletAddressMapping.class)
                .equalTo("walletAddressChainId", key)
                .findFirst();
        
        if (mapping == null)
        {
            // Primary key가 있는 Realm 객체는 createObject(Class, primaryKey) 형식으로 생성해야 함
            // createObject로 생성할 때 primary key가 이미 설정되므로, setWalletAddressChainId를 호출하면 안됨
            mapping = realm.createObject(RealmWalletAddressMapping.class, key);
            // Primary key는 이미 설정되었으므로, 다른 필드만 설정
            mapping.setWalletAddress(normalizedWalletAddress);
            mapping.setChainId(chainId);
            Timber.d("Created new RealmWalletAddressMapping with key: %s", key);
        }
        else
        {
            Timber.d("Updating existing RealmWalletAddressMapping with key: %s", key);
        }
        
        mapping.setNetworkAddress(networkAddress);
        Timber.d("Stored network address: %s for wallet: %s, chainId: %d", networkAddress, normalizedWalletAddress, chainId);
        // insertOrUpdate는 필요 없음 (이미 managed 객체이므로 변경사항이 자동 저장됨)
    }
    
    /**
     * 특정 네트워크의 주소 가져오기
     * @param walletAddress 기본 지갑 주소 (ETH 주소)
     * @param chainId 네트워크 체인 ID
     * @return 해당 네트워크의 주소, 없으면 기본 주소 반환
     */
    public String getNetworkAddress(String walletAddress, long chainId)
    {
        if (walletAddress == null || walletAddress.isEmpty()) {
            Timber.w("Wallet address is null or empty");
            return walletAddress;
        }
        
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            // EVM 주소는 소문자로 정규화하여 조회 (TRON 주소는 그대로 유지)
            String normalizedWalletAddress = walletAddress;
            if (walletAddress.startsWith("0x")) {
                normalizedWalletAddress = walletAddress.toLowerCase();
            }
            
            // 1차 시도: 정규화된 주소로 조회
            String key = normalizedWalletAddress + "-" + chainId;
            RealmWalletAddressMapping mapping = realm.where(RealmWalletAddressMapping.class)
                    .equalTo("walletAddressChainId", key)
                    .findFirst();
            
            if (mapping != null && mapping.getNetworkAddress() != null)
            {
                Timber.d("Found network address: %s for wallet: %s (normalized), chainId: %d", 
                        mapping.getNetworkAddress(), normalizedWalletAddress, chainId);
                return mapping.getNetworkAddress();
            }
            
            // 2차 시도: 원본 주소로도 조회 (checksum 형식일 수 있음)
            if (!normalizedWalletAddress.equals(walletAddress)) {
                String originalKey = walletAddress + "-" + chainId;
                mapping = realm.where(RealmWalletAddressMapping.class)
                        .equalTo("walletAddressChainId", originalKey)
                        .findFirst();
                
                if (mapping != null && mapping.getNetworkAddress() != null)
                {
                    Timber.d("Found network address: %s for wallet: %s (original), chainId: %d", 
                            mapping.getNetworkAddress(), walletAddress, chainId);
                    // 발견된 주소를 정규화된 키로 마이그레이션 (다음 조회를 위해, 비동기)
                    migrateToNormalizedKey(walletAddress, normalizedWalletAddress, chainId, mapping.getNetworkAddress());
                    return mapping.getNetworkAddress();
                }
            }
            
            // 3차 시도: walletAddress 필드로 대소문자 무시 조회 (TRON 주소만)
            if (chainId == TronConstants.TRON_ID) {
                RealmResults<RealmWalletAddressMapping> mappings = realm.where(RealmWalletAddressMapping.class)
                        .equalTo("walletAddress", normalizedWalletAddress, io.realm.Case.INSENSITIVE)
                        .equalTo("chainId", chainId)
                        .findAll();
                
                if (mappings.size() > 0) {
                    mapping = mappings.first();
                    if (mapping != null && mapping.getNetworkAddress() != null) {
                        Timber.d("Found TRON address via case-insensitive search: %s for wallet: %s, chainId: %d", 
                                mapping.getNetworkAddress(), normalizedWalletAddress, chainId);
                        // 발견된 주소를 정규화된 키로 마이그레이션 (비동기)
                        migrateToNormalizedKey(mapping.getWalletAddress(), normalizedWalletAddress, chainId, mapping.getNetworkAddress());
                        return mapping.getNetworkAddress();
                    }
                }
            }
            
            Timber.d("No network address mapping found for wallet: %s, chainId: %d, tried keys: [%s, %s]", 
                    walletAddress, chainId, key, !normalizedWalletAddress.equals(walletAddress) ? walletAddress + "-" + chainId : "N/A");
        }
        catch (Exception e)
        {
            Timber.e(e, "Error getting network address for wallet: %s, chainId: %d", walletAddress, chainId);
        }
        
        // 매핑이 없으면 기본 주소 반환 (하위 호환성)
        Timber.d("Returning default address (wallet address) for wallet: %s, chainId: %d", walletAddress, chainId);
        return walletAddress;
    }
    
    /**
     * 기존 주소 매핑을 정규화된 키로 마이그레이션
     * 별도 트랜잭션에서 실행
     */
    private void migrateToNormalizedKey(String oldWalletAddress, String normalizedWalletAddress, long chainId, String networkAddress)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance()) {
            realm.executeTransaction(r -> {
                String oldKey = oldWalletAddress + "-" + chainId;
                String newKey = normalizedWalletAddress + "-" + chainId;
                
                if (oldKey.equals(newKey)) {
                    return; // 이미 정규화된 키
                }
                
                // 새로운 키로 이미 매핑이 있는지 확인
                RealmWalletAddressMapping existingNewMapping = r.where(RealmWalletAddressMapping.class)
                        .equalTo("walletAddressChainId", newKey)
                        .findFirst();
                
                if (existingNewMapping != null) {
                    return; // 이미 정규화된 매핑이 존재
                }
                
                // 기존 매핑 찾기
                RealmWalletAddressMapping oldMapping = r.where(RealmWalletAddressMapping.class)
                        .equalTo("walletAddressChainId", oldKey)
                        .findFirst();
                
                if (oldMapping != null) {
                    // 새로운 키로 매핑 생성
                    RealmWalletAddressMapping newMapping = r.createObject(RealmWalletAddressMapping.class, newKey);
                    newMapping.setWalletAddress(normalizedWalletAddress);
                    newMapping.setChainId(chainId);
                    newMapping.setNetworkAddress(networkAddress);
                    
                    // 기존 매핑 삭제
                    oldMapping.deleteFromRealm();
                    
                    Timber.d("Migrated address mapping from %s to %s", oldKey, newKey);
                }
            });
        } catch (Exception e) {
            Timber.w(e, "Failed to migrate address mapping for wallet: %s", normalizedWalletAddress);
        }
    }
    
    /**
     * 지갑의 모든 네트워크 주소 가져오기
     * @param walletAddress 기본 지갑 주소
     * @return Map<chainId, address>
     */
    public Map<Long, String> getAllNetworkAddresses(String walletAddress)
    {
        Map<Long, String> addresses = new HashMap<>();
        
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            RealmResults<RealmWalletAddressMapping> mappings = realm.where(RealmWalletAddressMapping.class)
                    .equalTo("walletAddress", walletAddress, io.realm.Case.INSENSITIVE)
                    .findAll();
            
            for (RealmWalletAddressMapping mapping : mappings)
            {
                if (mapping.getNetworkAddress() != null)
                {
                    addresses.put(mapping.getChainId(), mapping.getNetworkAddress());
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Error getting all network addresses for wallet: %s", walletAddress);
        }
        
        return addresses;
    }
    
    /**
     * TRON 주소 가져오기 (편의 메서드)
     */
    public String getTronAddress(String walletAddress)
    {
        return getNetworkAddress(walletAddress, TronConstants.TRON_ID);
    }
}
