package com.setlone.app.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * 네트워크별 지갑 주소 매핑
 * 같은 니모닉에서 파생된 여러 네트워크의 주소를 저장
 * 
 * PrimaryKey: walletAddress-chainId (예: "0x123...-1", "T9yD1...-728126428")
 */
public class RealmWalletAddressMapping extends RealmObject
{
    @PrimaryKey
    private String walletAddressChainId; // "walletAddress-chainId" 형식
    
    private String walletAddress; // 기본 지갑 주소 (ETH 주소)
    private long chainId;         // 네트워크 체인 ID
    private String networkAddress; // 해당 네트워크의 주소 (ETH 주소 또는 TRON 주소)
    
    public String getWalletAddressChainId()
    {
        return walletAddressChainId;
    }
    
    public void setWalletAddressChainId(String walletAddress, long chainId)
    {
        this.walletAddressChainId = walletAddress + "-" + chainId;
        this.walletAddress = walletAddress;
        this.chainId = chainId;
    }
    
    public String getWalletAddress()
    {
        return walletAddress;
    }
    
    public void setWalletAddress(String walletAddress)
    {
        this.walletAddress = walletAddress;
    }
    
    public long getChainId()
    {
        return chainId;
    }
    
    public void setChainId(long chainId)
    {
        this.chainId = chainId;
    }
    
    public String getNetworkAddress()
    {
        return networkAddress;
    }
    
    public void setNetworkAddress(String networkAddress)
    {
        this.networkAddress = networkAddress;
    }
}
