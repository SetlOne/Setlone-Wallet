package com.setlone.app.service;

import com.setlone.app.entity.Wallet;
import com.setlone.app.service.RealmManager;
import com.setlone.app.repository.entity.RealmWalletAddressMapping;
import com.setlone.app.util.TronAddressGenerator;
import com.setlone.app.util.TronUtils;

import static com.setlone.ethereum.EthereumNetworkBase.TRON_ID;

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
                        Timber.d("Generated TRON address: %s for wallet: %s (chainId: %d)", tronAddress, walletAddress, TRON_ID);
                        storeNetworkAddress(r, walletAddress, TRON_ID, tronAddress);
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
     */
    private void storeNetworkAddress(Realm realm, String walletAddress, long chainId, String networkAddress)
    {
        String key = walletAddress + "-" + chainId;
        RealmWalletAddressMapping mapping = realm.where(RealmWalletAddressMapping.class)
                .equalTo("walletAddressChainId", key)
                .findFirst();
        
        if (mapping == null)
        {
            // Primary key가 있는 Realm 객체는 createObject(Class, primaryKey) 형식으로 생성해야 함
            // createObject로 생성할 때 primary key가 이미 설정되므로, setWalletAddressChainId를 호출하면 안됨
            mapping = realm.createObject(RealmWalletAddressMapping.class, key);
            // Primary key는 이미 설정되었으므로, 다른 필드만 설정
            mapping.setWalletAddress(walletAddress);
            mapping.setChainId(chainId);
            Timber.d("Created new RealmWalletAddressMapping with key: %s", key);
        }
        else
        {
            Timber.d("Updating existing RealmWalletAddressMapping with key: %s", key);
        }
        
        mapping.setNetworkAddress(networkAddress);
        Timber.d("Stored network address: %s for wallet: %s, chainId: %d", networkAddress, walletAddress, chainId);
        // insertOrUpdate는 필요 없음 (이미 managed 객체이므로 변경사항이 자동 저장됨)
    }
    
    /**
     * 특정 네트워크의 주소 가져오기
     * @param walletAddress 기본 지갑 주소
     * @param chainId 네트워크 체인 ID
     * @return 해당 네트워크의 주소, 없으면 기본 주소 반환
     */
    public String getNetworkAddress(String walletAddress, long chainId)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            String key = walletAddress + "-" + chainId;
            RealmWalletAddressMapping mapping = realm.where(RealmWalletAddressMapping.class)
                    .equalTo("walletAddressChainId", key)
                    .findFirst();
            
            if (mapping != null && mapping.getNetworkAddress() != null)
            {
                Timber.d("Found network address: %s for wallet: %s, chainId: %d", mapping.getNetworkAddress(), walletAddress, chainId);
                return mapping.getNetworkAddress();
            }
            else
            {
                Timber.d("No network address mapping found for wallet: %s, chainId: %d, key: %s", walletAddress, chainId, key);
            }
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
        return getNetworkAddress(walletAddress, TRON_ID);
    }
}
