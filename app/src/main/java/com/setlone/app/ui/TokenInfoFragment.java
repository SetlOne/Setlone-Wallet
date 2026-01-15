package com.setlone.app.ui;


import static com.setlone.app.service.TickerService.chainPairs;
import static com.setlone.app.service.TickerService.coinGeckoChainIdToAPIName;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.setlone.app.C;
import com.setlone.app.R;
import com.setlone.app.entity.Wallet;
import com.setlone.app.entity.tokens.Token;
import com.setlone.app.entity.tokens.TokenPortfolio;
import com.setlone.app.service.TickerService;
import com.setlone.app.ui.widget.entity.HistoryChart;
import com.setlone.app.util.TabUtils;
import com.setlone.app.viewmodel.TokenInfoViewModel;
import com.setlone.app.web3.Web3TokenView;
import com.setlone.app.web3.entity.Address;
import com.setlone.app.widget.TokenInfoCategoryView;
import com.setlone.app.widget.TokenInfoHeaderView;
import com.setlone.app.widget.TokenInfoView;
import com.setlone.ethereum.EthereumNetworkBase;
import com.setlone.token.entity.TicketRange;
import com.setlone.token.entity.ViewType;
import com.setlone.token.tools.Convert;
import com.setlone.token.tools.TokenDefinition;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

@AndroidEntryPoint
public class TokenInfoFragment extends BaseFragment {
    public static final int CHART_1D = 0;
    public static final int CHART_1W = 1;
    public static final int CHART_1M = 2;
    public static final int CHART_3M = 3;
    public static final int CHART_1Y = 4;

    private TokenInfoViewModel viewModel;

    private Token token;
    private LinearLayout tokenInfoHeaderLayout;
    private LinearLayout tokenInfoLayout;
    private HistoryChart historyChart;

    private TokenInfoHeaderView tokenInfoHeaderView;
    private TokenInfoView portfolioBalance;
    private TokenInfoView portfolioProfit24Hr;
    private TokenInfoView portfolioProfitTotal;
    private TokenInfoView portfolioShare;
    private TokenInfoView portfolioAverageCost;
    private TokenInfoView portfolioPaidFees;
    private TokenInfoView performance1D;
    private TokenInfoView performance1W;
    private TokenInfoView performance1M;
    private TokenInfoView performance1Y;
    private TokenInfoView statsMarketCap;
    private TokenInfoView statsTradingVolume;
    private TokenInfoView statsMaxVolume;
    private TokenInfoView stats1YearLow;
    private TokenInfoView stats1YearHigh;
    private TokenInfoView contractAddress;
    private LinearLayout webWrapper;
    private Web3TokenView tokenScriptView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_token_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            viewModel = new ViewModelProvider(this)
                    .get(TokenInfoViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));

            initTabLayout(view);
            historyChart = view.findViewById(R.id.history_chart);
            tokenInfoHeaderLayout = view.findViewById(R.id.layout_token_header);
            tokenInfoLayout = view.findViewById(R.id.layout_token_info);
            webWrapper = view.findViewById(R.id.layout_webwrapper);

            //TODO: Work out how to source these
            //portfolioBalance = new TokenInfoView(getContext(), "Balance");
            //portfolioProfit24Hr = new TokenInfoView(getContext(), "24-H Return");
            //portfolioProfitTotal = new TokenInfoView(getContext(), "Profit/Loss");
            //portfolioShare = new TokenInfoView(getContext(), "Portfolio Share");
            //portfolioAverageCost = new TokenInfoView(getContext(), "Average Cost");
            //portfolioPaidFees = new TokenInfoView(getContext(), "Paid Fees");
            performance1D = new TokenInfoView(getContext(), "1 Day");
            performance1D.setHasPrefix(true);
            performance1W = new TokenInfoView(getContext(), "1 Week");
            performance1W.setHasPrefix(true);
            performance1M = new TokenInfoView(getContext(), "1 Month");
            performance1M.setHasPrefix(true);
            performance1Y = new TokenInfoView(getContext(), "1 Year");
            performance1Y.setHasPrefix(true);
            statsMarketCap = new TokenInfoView(getContext(), "Market Cap");
            statsTradingVolume = new TokenInfoView(getContext(), "Current Volume");
            statsMaxVolume = new TokenInfoView(getContext(), "Max Volume");
            stats1YearLow = new TokenInfoView(getContext(), "1 Year Low");
            stats1YearHigh = new TokenInfoView(getContext(), "1 Year High");
            // TRON 네이티브 토큰인 경우 지갑의 TRON 주소를 표시
            String addressToDisplay = token.tokenInfo.address;
            String labelText = getString(R.string.contract_address);
            
            if (com.setlone.app.repository.EthereumNetworkBase.isTronNetwork(token.tokenInfo.chainId) && token.isEthereum()) {
                // TRON 네이티브 토큰 (TRX)인 경우 지갑 주소를 TRON 주소로 변환
                Wallet wallet = getArguments().getParcelable(C.Key.WALLET);
                String walletAddress = wallet != null ? wallet.address : viewModel.getTokensService().getCurrentAddress();
                
                if (walletAddress != null) {
                    String tronAddress = viewModel.getAddressForNetwork(walletAddress, token.tokenInfo.chainId);
                    if (tronAddress != null && !tronAddress.equals(walletAddress) && tronAddress.startsWith("T")) {
                        // TRON 주소가 이미 있음
                        addressToDisplay = tronAddress;
                        labelText = getString(R.string.my_wallet_address); // TRON 주소는 "지갑 주소"로 표시
                    } else {
                        // TRON 주소가 없으면 임시로 "로딩 중..." 표시하고 생성 시도
                        addressToDisplay = getString(R.string.loading);
                        labelText = getString(R.string.my_wallet_address);
                        
                        // TRON 주소 생성 시도 (비동기)
                        if (wallet != null && getActivity() != null) {
                            viewModel.generateTronAddressIfNeeded(wallet, getActivity(), () -> {
                                // 생성 완료 후 주소 다시 가져오기
                                if (getActivity() != null && contractAddress != null) {
                                    String newTronAddress = viewModel.getAddressForNetwork(walletAddress, token.tokenInfo.chainId);
                                    if (newTronAddress != null && !newTronAddress.equals(walletAddress) && newTronAddress.startsWith("T")) {
                                        // UI 업데이트
                                        contractAddress.setCopyableValue(newTronAddress);
                                    } else {
                                        // 생성 실패 시 ETH 주소 표시
                                        contractAddress.setCopyableValue(walletAddress);
                                    }
                                }
                            });
                        } else {
                            // 지갑 정보가 없으면 ETH 주소 표시
                            addressToDisplay = walletAddress;
                        }
                    }
                }
            } else {
                // 일반 토큰의 경우 컨트랙트 주소 표시
                addressToDisplay = token.tokenInfo.address;
            }
            
            contractAddress = new TokenInfoView(getContext(), labelText);
            contractAddress.setCopyableValue(addressToDisplay);

            tokenInfoHeaderView = new TokenInfoHeaderView(getContext(), token, viewModel.getTokensService());
            tokenInfoHeaderLayout.addView(tokenInfoHeaderView);

            LinearLayout contractAddrView = view.findViewById(R.id.layout_contract_addr);
            contractAddrView.addView(contractAddress);

            /*tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Portfolio"));
            tokenInfoLayout.addView(portfolioBalance);
            tokenInfoLayout.addView(portfolioProfit24Hr);
            tokenInfoLayout.addView(portfolioProfitTotal);
            tokenInfoLayout.addView(portfolioShare);
            tokenInfoLayout.addView(portfolioAverageCost);
            tokenInfoLayout.addView(portfolioPaidFees);*/

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), getString(R.string.performance)));
            tokenInfoLayout.addView(performance1D);
            tokenInfoLayout.addView(performance1W);
            tokenInfoLayout.addView(performance1M);
            tokenInfoLayout.addView(performance1Y);

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Stats"));
            tokenInfoLayout.addView(statsMarketCap);
            tokenInfoLayout.addView(statsTradingVolume);
            tokenInfoLayout.addView(statsMaxVolume);
            tokenInfoLayout.addView(stats1YearLow);
            tokenInfoLayout.addView(stats1YearHigh);

            historyChart.fetchHistory(token, HistoryChart.Range.Day);
            populateStats(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleValues, e -> { /*TODO: Hide stats*/ })
                    .isDisposed();
        }
    }

    private void hideStats()
    {
        tokenInfoLayout.removeAllViews();
        tokenInfoLayout.invalidate();
    }

    private void handleValues(List<Double> values)
    {
        if (values.size() == 0)
        {
            hideStats();
            return;
        }

        int index = 0;

        performance1D.setCurrencyValue(values.get(index++));
        performance1W.setCurrencyValue(values.get(index++));
        performance1M.setCurrencyValue(values.get(index++));
        performance1Y.setCurrencyValue(values.get(index++));

        statsMarketCap.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        statsTradingVolume.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        statsMaxVolume.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        stats1YearLow.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        stats1YearHigh.setValue(TickerService.getFullCurrencyString(values.get(index++)));
    }

    private void initTabLayout(View view)
    {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("1D"));
        tabLayout.addTab(tabLayout.newTab().setText("1W"));
        tabLayout.addTab(tabLayout.newTab().setText("1M"));
        tabLayout.addTab(tabLayout.newTab().setText("3M"));
        tabLayout.addTab(tabLayout.newTab().setText("1Y"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                historyChart.fetchHistory(token, HistoryChart.Range.values()[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {
            }
        });

        TabUtils.setHighlightedTabColor(getContext(), tabLayout);
    }

    private void onPortfolioUpdated(TokenPortfolio tokenPortfolio)
    {
        portfolioBalance.setValue(tokenPortfolio.getBalance());
        portfolioProfit24Hr.setValue(tokenPortfolio.getProfit24Hrs());
        portfolioProfitTotal.setValue(tokenPortfolio.getProfitTotal());
        portfolioShare.setValue(tokenPortfolio.getShare());
        portfolioAverageCost.setValue(tokenPortfolio.getAverageCost());
        portfolioPaidFees.setValue(tokenPortfolio.getFees());
    }

    static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    private Single<List<Double>> populateStats(Token token)
    {
        List<Double> values = new ArrayList<>();
        if (!TickerService.validateCoinGeckoAPI(token)) return Single.fromCallable(() -> values);

        return Single.fromCallable(() -> {

            String coinGeckotokenId;
            if (token.isEthereum())
            {
                // 네이티브 토큰 (ETH, TRX 등)
                coinGeckotokenId = chainPairs.get(token.tokenInfo.chainId);
            }
            else
            {
                // ERC-20, TRC-20 등 컨트랙트 토큰
                String chainApiName = coinGeckoChainIdToAPIName.get(token.tokenInfo.chainId);
                if (chainApiName == null)
                {
                    // TRON 등 지원되지 않는 체인
                    return values;
                }
                coinGeckotokenId = chainApiName + "/contract/" + token.getAddress().toLowerCase();
            }
            
            if (coinGeckotokenId == null)
            {
                // CoinGecko에서 지원하지 않는 토큰
                return values;
            }

            Request request = new Request.Builder()
                    .url("https://api.coingecko.com/api/v3/coins/" + coinGeckotokenId + "/market_chart?vs_currency=" + TickerService.getCurrencySymbolTxt() + "&days=365")
                    .get()
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request)
                    .execute())
            {
                // Check if response is successful
                if (!response.isSuccessful() || response.body() == null)
                {
                    Timber.w("CoinGecko API request failed: %s", response.code());
                    return values;
                }

                String result = response.body().string();
                if (result == null || result.isEmpty())
                {
                    Timber.w("CoinGecko API returned empty response");
                    return values;
                }

                JSONObject jsonResponse = new JSONObject(result);
                
                // Check if "prices" key exists
                if (!jsonResponse.has("prices"))
                {
                    Timber.w("CoinGecko API response missing 'prices' key. Response: %s", result.length() > 200 ? result.substring(0, 200) : result);
                    return values;
                }

                //build mapping
                JSONArray prices = jsonResponse.getJSONArray("prices");
                if (prices == null || prices.length() == 0)
                {
                    Timber.w("CoinGecko API returned empty prices array");
                    return values;
                }

                JSONArray marketCaps = jsonResponse.has("market_caps") ? jsonResponse.getJSONArray("market_caps") : new JSONArray();
                JSONArray totalVolumes = jsonResponse.has("total_volumes") ? jsonResponse.getJSONArray("total_volumes") : new JSONArray();

                long yesterdayTime = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
                long oneWeekTime = System.currentTimeMillis() - 7 * DateUtils.DAY_IN_MILLIS;
                long oneMonthTime = System.currentTimeMillis() - 30 * DateUtils.DAY_IN_MILLIS;

                //Add performance stats. This is the variance
                double currentPrice = getDoubleValue(prices, prices.length() - 1);

                BigDecimal correctedBalance = token.getCorrectedBalance(Convert.Unit.ETHER.getFactor());

                BigDecimal yesterdayDiff = BigDecimal.valueOf(currentPrice - findValue(prices, getMidnightDateFromTimestamp(yesterdayTime))).multiply(correctedBalance);
                BigDecimal oneWeekDiff = BigDecimal.valueOf(currentPrice - findValue(prices, getMidnightDateFromTimestamp(oneWeekTime))).multiply(correctedBalance);
                BigDecimal oneMonthDiff = BigDecimal.valueOf(currentPrice - findValue(prices, getMidnightDateFromTimestamp(oneMonthTime))).multiply(correctedBalance);
                BigDecimal oneYearDiff = BigDecimal.valueOf(currentPrice - getDoubleValue(prices, 0)).multiply(correctedBalance);

                values.add(yesterdayDiff.doubleValue());
                values.add(oneWeekDiff.doubleValue());
                values.add(oneMonthDiff.doubleValue());
                values.add(oneYearDiff.doubleValue());

                //add market cap
                values.add(marketCaps.length() > 0 ? getDoubleValue(marketCaps, marketCaps.length() - 1) : 0.0);

                //add total volume
                values.add(totalVolumes.length() > 0 ? getDoubleValue(totalVolumes, totalVolumes.length() - 1) : 0.0);

                //get trading volume high
                Pair<Double, Double> minMax = totalVolumes.length() > 0 ? getMinMax(totalVolumes) : new Pair<>(0.0, 0.0);
                values.add(minMax.second);

                //get highs and lows
                minMax = getMinMax(prices);
                values.add(minMax.first);
                values.add(minMax.second);
            }
            catch (org.json.JSONException e)
            {
                Timber.e(e, "Failed to parse CoinGecko API response");
            }
            catch (Exception e)
            {
                Timber.e(e, "Error fetching price data from CoinGecko");
            }

            return values;
        });
    }

    private Pair<Double, Double> getMinMax(JSONArray valueArray)
    {
        double min = Double.MAX_VALUE;
        double max = 0.0;
        try
        {
            for (int i = 0; i < valueArray.length(); i++)
            {
                JSONArray valueElement = valueArray.getJSONArray(i);
                double value = valueElement.getDouble(1);
                if (value < min) min = value;
                if (value > max) max = value;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        if (min == Double.MAX_VALUE) min = 0.0;

        return new Pair<>(min, max);
    }

    private double findValue(JSONArray prices, Date targetDate)
    {
        try
        {
            long lastDate = System.currentTimeMillis();
            long targetTime = targetDate.getTime();
            for (int i = prices.length() - 2; i >= 0; i--)
            {
                JSONArray thisPrice = prices.getJSONArray(i);
                long timeStamp = thisPrice.getLong(0);
                if (lastDate > targetTime && targetTime >= timeStamp)
                {
                    //got it
                    return thisPrice.getDouble(1);
                }
                lastDate = timeStamp;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return 0.0;
    }

    private double getDoubleValue(JSONArray prices, int i)
    {
        try
        {
            JSONArray thisPrice = prices.getJSONArray(i);
            return thisPrice.getDouble(1);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return 0.0;
    }

    private Date getMidnightDateFromTimestamp(long timeStampInMillis) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(timeStampInMillis);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }

    @Override
    public void onDestroy()
    {
        if (tokenScriptView != null && tokenScriptView.getVisibility() == View.VISIBLE)
        {
            webWrapper.removeView(tokenScriptView);
            tokenScriptView.destroy();
            tokenScriptView = null;
        }
        super.onDestroy();
    }

    /***
     * TokenScript view handling
     */
    public void initTokenScript(final TokenDefinition td)
    {
        try
        {
            //restart if required
            if (tokenScriptView != null)
            {
                webWrapper.removeView(tokenScriptView);
                tokenScriptView.destroy();
                tokenScriptView = null;
            }

            tokenScriptView = new Web3TokenView(requireContext());
            tokenScriptView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tokenScriptView.clearCache(true);

            if (tokenScriptView.renderTokenScriptInfoView(token, new TicketRange(token.balance.toBigInteger()), viewModel.getAssetDefinitionService(), ViewType.VIEW, td))
            {
                webWrapper.setVisibility(View.VISIBLE);
                tokenScriptView.setChainId(token.tokenInfo.chainId);
                tokenScriptView.setWalletAddress(new Address(token.getWallet()));
                webWrapper.addView(tokenScriptView);
            }
        }
        catch (Exception e)
        {
            //fillEmpty();
        }
    }
}
