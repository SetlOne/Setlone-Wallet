package com.setlone.app.repository;

import com.setlone.app.entity.Wallet;
import com.setlone.app.service.AccountKeystoreService;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.WalletAddressService;
import com.setlone.app.util.TronConstants;
import com.setlone.app.util.TronUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.realm.Realm;
import timber.log.Timber;

public class WalletRepository implements WalletRepositoryType
{
	private final PreferenceRepositoryType preferenceRepositoryType;
	private final AccountKeystoreService accountKeystoreService;
	private final EthereumNetworkRepositoryType networkRepository;
	private final WalletDataRealmSource walletDataRealmSource;
	private final KeyService keyService;
	private final WalletAddressService walletAddressService;

	public WalletRepository(PreferenceRepositoryType preferenceRepositoryType, AccountKeystoreService accountKeystoreService, EthereumNetworkRepositoryType networkRepository, WalletDataRealmSource walletDataRealmSource, KeyService keyService, WalletAddressService walletAddressService)
	{
		this.preferenceRepositoryType = preferenceRepositoryType;
		this.accountKeystoreService = accountKeystoreService;
		this.networkRepository = networkRepository;
		this.walletDataRealmSource = walletDataRealmSource;
		this.keyService = keyService;
		this.walletAddressService = walletAddressService;
	}

	@Override
	public Single<Wallet[]> fetchWallets()
	{
		return accountKeystoreService.fetchAccounts()
				.flatMap(wallets -> walletDataRealmSource.populateWalletData(wallets, keyService))
				.map(wallets -> {
					// ETH 지갑 목록에 TRON 지갑 추가
					List<Wallet> allWallets = new ArrayList<>();
					
					for (Wallet ethWallet : wallets)
					{
						// ETH 지갑 추가
						allWallets.add(ethWallet);
						
						// 해당 ETH 지갑의 TRON 주소 조회
						String tronAddress = walletAddressService.getTronAddress(ethWallet.address);
						
						// TRON 주소가 있고, ETH 주소와 다르고, T로 시작하면 TRON 지갑 추가
						if (tronAddress != null && !tronAddress.equals(ethWallet.address) && TronUtils.isValidTronAddress(tronAddress))
						{
							// TRON 지갑 생성 (원본 ETH 주소 저장)
							Wallet tronWallet = new Wallet(tronAddress);
							tronWallet.originalEthAddress = ethWallet.address;
							tronWallet.type = ethWallet.type; // 같은 타입 유지
							tronWallet.name = ethWallet.name + " (TRON)"; // 이름에 TRON 표시
							tronWallet.walletCreationTime = ethWallet.walletCreationTime;
							tronWallet.lastBackupTime = ethWallet.lastBackupTime;
							tronWallet.authLevel = ethWallet.authLevel;
							tronWallet.balanceSymbol = "TRX";
							
							allWallets.add(tronWallet);
							Timber.d("Added TRON wallet: %s for ETH wallet: %s", tronAddress, ethWallet.address);
						}
					}
					
					Wallet[] result = allWallets.toArray(new Wallet[0]);
					
					if (preferenceRepositoryType.getCurrentWalletAddress() == null && result.length > 0)
					{
						preferenceRepositoryType.setCurrentWalletAddress(result[0].address);
					}
					return result;
				});
	}

	@Override
	public Single<Wallet> findWallet(String address)
	{
		return fetchWallets()
				.flatMap(wallets -> {
					if (wallets.length == 0) return Single.error(new NoWallets("No wallets"));
					Wallet firstWallet = null;
					for (Wallet wallet : wallets)
					{
						if (address == null || wallet.sameAddress(address))
						{
							return Single.just(wallet);
						}
						if (firstWallet == null) firstWallet = wallet;
					}

					return Single.just(firstWallet);
				});
	}

	@Override
	public Single<Wallet> createWallet(String password)
	{
		return accountKeystoreService.createAccount(password);
	}

	@Override
	public Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword)
	{
		return accountKeystoreService.importKeystore(store, password, newPassword);
	}

	@Override
	public Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword)
	{
		return accountKeystoreService.importPrivateKey(privateKey, newPassword);
	}

	@Override
	public Single<String> exportWallet(Wallet wallet, String password, String newPassword)
	{
		return accountKeystoreService.exportAccount(wallet, password, newPassword);
	}

	@Override
	public Completable deleteWallet(String address, String password)
	{
		return accountKeystoreService.deleteAccount(address, password);
	}

	@Override
	public Single<Wallet> deleteWalletFromRealm(Wallet wallet)
	{
		return walletDataRealmSource.deleteWallet(wallet);
	}

	@Override
	public Completable setDefaultWallet(Wallet wallet)
	{
		return Completable.fromAction(() -> preferenceRepositoryType.setCurrentWalletAddress(wallet.address));
	}

	@Override
	public void updateBackupTime(String walletAddr)
	{
		walletDataRealmSource.updateBackupTime(walletAddr);
	}

	@Override
	public void updateWarningTime(String walletAddr)
	{
		walletDataRealmSource.updateWarningTime(walletAddr);
	}

	@Override
	public Single<Boolean> getWalletBackupWarning(String walletAddr)
	{
		return walletDataRealmSource.getWalletBackupWarning(walletAddr);
	}

	@Override
	public Single<String> getWalletRequiresBackup(String walletAddr)
	{
		return walletDataRealmSource.getWalletRequiresBackup(walletAddr);
	}

	@Override
	public void setIsDismissed(String walletAddr, boolean isDismissed)
	{
		walletDataRealmSource.setIsDismissed(walletAddr, isDismissed);
	}

	@Override
	public Single<Wallet> getDefaultWallet()
	{
		return Single.fromCallable(preferenceRepositoryType::getCurrentWalletAddress)
				.flatMap(this::findWallet);
	}

	@Override
	public Single<Wallet[]> storeWallets(Wallet[] wallets)
	{
		return walletDataRealmSource.storeWallets(wallets);
	}

	@Override
	public Single<Wallet> storeWallet(Wallet wallet)
	{
		return walletDataRealmSource.storeWallet(wallet);
	}

	@Override
	public void updateWalletData(Wallet wallet, Realm.Transaction.OnSuccess onSuccess)
	{
		walletDataRealmSource.updateWalletData(wallet, onSuccess);
	}

	@Override
	public void updateWalletItem(Wallet wallet, WalletItem item, Realm.Transaction.OnSuccess onSuccess)
	{
		walletDataRealmSource.updateWalletItem(wallet, item, onSuccess);
	}

	@Override
	public Single<String> getName(String address)
	{
		return walletDataRealmSource.getName(address);
	}

	@Override
    public boolean keystoreExists(String address)
    {
		Wallet[] wallets = fetchWallets().blockingGet();
		for (Wallet w : wallets)
		{
			if (w.sameAddress(address)) return true;
		}
		return false;
    }

    @Override
	public Realm getWalletRealm()
	{
		return walletDataRealmSource.getWalletRealm();
	}
}
