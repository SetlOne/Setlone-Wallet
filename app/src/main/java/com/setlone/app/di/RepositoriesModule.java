package com.setlone.app.di;

import static com.setlone.app.service.KeystoreAccountService.KEYSTORE_FOLDER;

import android.content.Context;

import com.setlone.app.repository.CoinbasePayRepository;
import com.setlone.app.repository.CoinbasePayRepositoryType;
import com.setlone.app.repository.EthereumNetworkRepository;
import com.setlone.app.repository.EthereumNetworkRepositoryType;
import com.setlone.app.repository.OnRampRepository;
import com.setlone.app.repository.OnRampRepositoryType;
import com.setlone.app.repository.PreferenceRepositoryType;
import com.setlone.app.repository.SharedPreferenceRepository;
import com.setlone.app.repository.SwapRepository;
import com.setlone.app.repository.SwapRepositoryType;
import com.setlone.app.repository.TokenLocalSource;
import com.setlone.app.repository.TokenRepository;
import com.setlone.app.repository.TokenRepositoryType;
import com.setlone.app.repository.TokensMappingRepository;
import com.setlone.app.repository.TokensMappingRepositoryType;
import com.setlone.app.repository.TokensRealmSource;
import com.setlone.app.repository.TransactionLocalSource;
import com.setlone.app.repository.TransactionRepository;
import com.setlone.app.repository.TransactionRepositoryType;
import com.setlone.app.repository.TransactionsRealmCache;
import com.setlone.app.repository.WalletDataRealmSource;
import com.setlone.app.repository.WalletRepository;
import com.setlone.app.repository.WalletRepositoryType;
import com.setlone.app.service.AccountKeystoreService;
import com.setlone.app.service.SetlOneNotificationService;
import com.setlone.app.service.SetlOneService;
import com.setlone.app.service.AnalyticsService;
import com.setlone.app.service.AnalyticsServiceType;
import com.setlone.app.service.AssetDefinitionService;
import com.setlone.app.service.GasService;
import com.setlone.app.service.IPFSService;
import com.setlone.app.service.IPFSServiceType;
import com.setlone.app.service.KeyService;
import com.setlone.app.service.KeystoreAccountService;
import com.setlone.app.service.NotificationService;
import com.setlone.app.service.OpenSeaService;
import com.setlone.app.service.RealmManager;
import com.setlone.app.service.SwapService;
import com.setlone.app.service.TickerService;
import com.setlone.app.service.TokensService;
import com.setlone.app.service.TransactionsNetworkClient;
import com.setlone.app.service.TransactionsNetworkClientType;
import com.setlone.app.service.TransactionsService;
import com.setlone.app.service.TransactionNotificationService;
import com.google.gson.Gson;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoriesModule
{
    @Singleton
    @Provides
    PreferenceRepositoryType providePreferenceRepository(@ApplicationContext Context context)
    {
        return new SharedPreferenceRepository(context);
    }

    @Singleton
    @Provides
    AccountKeystoreService provideAccountKeyStoreService(@ApplicationContext Context context, KeyService keyService)
    {
        File file = new File(context.getFilesDir(), KEYSTORE_FOLDER);
        return new KeystoreAccountService(file, context.getFilesDir(), keyService);
    }

    @Singleton
    @Provides
    TickerService provideTickerService(OkHttpClient httpClient, PreferenceRepositoryType sharedPrefs, TokenLocalSource localSource)
    {
        return new TickerService(httpClient, sharedPrefs, localSource);
    }

    @Singleton
    @Provides
    EthereumNetworkRepositoryType provideEthereumNetworkRepository(
        PreferenceRepositoryType preferenceRepository,
        @ApplicationContext Context context
    )
    {
        return new EthereumNetworkRepository(preferenceRepository, context);
    }

    @Singleton
    @Provides
    WalletRepositoryType provideWalletRepository(
        PreferenceRepositoryType preferenceRepositoryType,
        AccountKeystoreService accountKeystoreService,
        EthereumNetworkRepositoryType networkRepository,
        WalletDataRealmSource walletDataRealmSource,
        KeyService keyService)
    {
        return new WalletRepository(
            preferenceRepositoryType, accountKeystoreService, networkRepository, walletDataRealmSource, keyService);
    }

    @Singleton
    @Provides
    TransactionRepositoryType provideTransactionRepository(
        EthereumNetworkRepositoryType networkRepository,
        AccountKeystoreService accountKeystoreService,
        TransactionLocalSource inDiskCache,
        TransactionsService transactionsService,
        com.setlone.app.service.TronService tronService)
    {
        return new TransactionRepository(
            networkRepository,
            accountKeystoreService,
            inDiskCache,
            transactionsService,
            tronService);
    }

    @Singleton
    @Provides
    OnRampRepositoryType provideOnRampRepository(@ApplicationContext Context context)
    {
        return new OnRampRepository(context);
    }

    @Singleton
    @Provides
    SwapRepositoryType provideSwapRepository(@ApplicationContext Context context)
    {
        return new SwapRepository(context);
    }

    @Singleton
    @Provides
    CoinbasePayRepositoryType provideCoinbasePayRepository()
    {
        return new CoinbasePayRepository();
    }

    @Singleton
    @Provides
    TransactionLocalSource provideTransactionInDiskCache(RealmManager realmManager)
    {
        return new TransactionsRealmCache(realmManager);
    }

    @Singleton
    @Provides
    TransactionsNetworkClientType provideBlockExplorerClient(
        OkHttpClient httpClient,
        Gson gson,
        RealmManager realmManager)
    {
        return new TransactionsNetworkClient(httpClient, gson, realmManager);
    }

    @Singleton
    @Provides
    TokenRepositoryType provideTokenRepository(
        EthereumNetworkRepositoryType ethereumNetworkRepository,
        TokenLocalSource tokenLocalSource,
        @ApplicationContext Context context,
        TickerService tickerService,
        RealmManager realmManager)
    {
        return new TokenRepository(
            ethereumNetworkRepository,
            tokenLocalSource,
            context,
            tickerService,
            realmManager);
    }

    @Singleton
    @Provides
    TokenLocalSource provideRealmTokenSource(RealmManager realmManager, EthereumNetworkRepositoryType ethereumNetworkRepository, TokensMappingRepositoryType tokensMappingRepository)
    {
        return new TokensRealmSource(realmManager, ethereumNetworkRepository, tokensMappingRepository);
    }

    @Singleton
    @Provides
    WalletDataRealmSource provideRealmWalletDataSource(RealmManager realmManager)
    {
        return new WalletDataRealmSource(realmManager);
    }
    
    @Singleton
    @Provides
    com.setlone.app.service.WalletAddressService provideWalletAddressService(RealmManager realmManager)
    {
        return new com.setlone.app.service.WalletAddressService(realmManager);
    }
    
    @Singleton
    @Provides
    com.setlone.app.repository.TronTransactionRepository provideTronTransactionRepository(
        com.setlone.app.service.TronService tronService,
        com.setlone.app.service.WalletAddressService walletAddressService,
        KeyService keyService)
    {
        return new com.setlone.app.repository.TronTransactionRepository(
            tronService, walletAddressService, keyService);
    }

    @Singleton
    @Provides
    TokensService provideTokensServices(EthereumNetworkRepositoryType ethereumNetworkRepository,
                                        TokenRepositoryType tokenRepository,
                                        TickerService tickerService,
                                        OpenSeaService openseaService,
                                        AnalyticsServiceType analyticsService,
                                        OkHttpClient client)
    {
        return new TokensService(ethereumNetworkRepository, tokenRepository, tickerService, openseaService, analyticsService, client);
    }

    @Singleton
    @Provides
    IPFSServiceType provideIPFSService(OkHttpClient client)
    {
        return new IPFSService(client);
    }

    @Singleton
    @Provides
    TransactionsService provideTransactionsServices(TokensService tokensService,
                                                    EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                                                    TransactionsNetworkClientType transactionsNetworkClientType,
                                                    TransactionLocalSource transactionLocalSource,
                                                    TransactionNotificationService transactionNotificationService)
    {
        return new TransactionsService(tokensService, ethereumNetworkRepositoryType, transactionsNetworkClientType, transactionLocalSource, transactionNotificationService);
    }

    @Singleton
    @Provides
    GasService provideGasService(EthereumNetworkRepositoryType ethereumNetworkRepository, OkHttpClient client, RealmManager realmManager)
    {
        return new GasService(ethereumNetworkRepository, client, realmManager);
    }
    
    @Singleton
    @Provides
    com.setlone.app.service.TronService provideTronService(OkHttpClient client, Gson gson)
    {
        return new com.setlone.app.service.TronService(client, gson);
    }

    @Singleton
    @Provides
    OpenSeaService provideOpenseaService()
    {
        return new OpenSeaService();
    }

    @Singleton
    @Provides
    SwapService provideSwapService()
    {
        return new SwapService();
    }

    @Singleton
    @Provides
    SetlOneService provideFeemasterService(OkHttpClient okHttpClient, Gson gson)
    {
        return new SetlOneService(okHttpClient, gson);
    }

    @Singleton
    @Provides
    NotificationService provideNotificationService(@ApplicationContext Context ctx)
    {
        return new NotificationService(ctx);
    }

    @Singleton
    @Provides
    AssetDefinitionService providingAssetDefinitionServices(IPFSServiceType ipfsService, @ApplicationContext Context ctx, NotificationService notificationService, RealmManager realmManager,
                                                            TokensService tokensService, TokenLocalSource tls,
                                                            SetlOneService alphaService)
    {
        return new AssetDefinitionService(ipfsService, ctx, notificationService, realmManager, tokensService, tls, alphaService);
    }

    @Singleton
    @Provides
    KeyService provideKeyService(@ApplicationContext Context ctx, AnalyticsServiceType analyticsService)
    {
        return new KeyService(ctx, analyticsService);
    }

    @Singleton
    @Provides
    AnalyticsServiceType provideAnalyticsService(@ApplicationContext Context ctx, PreferenceRepositoryType preferenceRepository)
    {
        return new AnalyticsService(ctx, preferenceRepository);
    }

    @Singleton
    @Provides
    TokensMappingRepositoryType provideTokensMappingRepository(@ApplicationContext Context ctx)
    {
        return new TokensMappingRepository(ctx);
    }

    @Singleton
    @Provides
    TransactionNotificationService provideTransactionNotificationService(@ApplicationContext Context ctx,
                                                                         PreferenceRepositoryType preferenceRepositoryType)
    {
        return new TransactionNotificationService(ctx, preferenceRepositoryType);
    }

    @Singleton
    @Provides
    SetlOneNotificationService provideSetlOneNotificationService(WalletRepositoryType walletRepository)
    {
        return new SetlOneNotificationService(walletRepository);
    }
}
