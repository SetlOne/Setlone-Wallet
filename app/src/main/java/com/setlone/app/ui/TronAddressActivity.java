package com.setlone.app.ui;

import static com.setlone.ethereum.EthereumNetworkBase.TRON_ID;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.setlone.app.C;
import com.setlone.app.R;
import com.setlone.app.entity.AddressMode;
import com.setlone.app.entity.CustomViewSettings;
import com.setlone.app.entity.EIP681Request;
import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.entity.Wallet;
import com.setlone.app.entity.tokens.Token;
import com.setlone.app.repository.EthereumNetworkBase;
import com.setlone.app.ui.QRScanning.DisplayUtils;
import com.setlone.app.ui.widget.entity.AmountReadyCallback;
import com.setlone.app.util.KeyboardUtils;
import com.setlone.app.util.QRUtils;
import com.setlone.app.viewmodel.MyAddressViewModel;
import com.setlone.app.widget.CopyTextView;
import com.setlone.app.widget.InputAmount;

import java.math.BigDecimal;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * TRON 네트워크 전용 주소 표시 화면
 * MyAddressActivity와 유사하지만 TRON 네트워크에 특화된 기능 제공
 */
@AndroidEntryPoint
public class TronAddressActivity extends BaseActivity implements AmountReadyCallback
{
    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_MODE = "mode";
    public static final String OVERRIDE_DEFAULT = "override";

    private MyAddressViewModel viewModel;

    private Wallet wallet;
    private String displayAddress;
    private String displayName;
    private Token token;
    private ImageView qrImageView;
    private LinearLayout layoutInputAmount;
    private NetworkInfo networkInfo;
    private AddressMode currentMode = AddressMode.MODE_ADDRESS;
    private long overrideNetwork;
    private int screenWidth;
    private CopyTextView copyAddress;
    private CopyTextView copyWalletName;
    private ProgressBar ensFetchProgressBar;

    private InputAmount amountInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        screenWidth = Math.min((int) ((float)DisplayUtils.getScreenResolution(this).x * 0.8f), 1900);
        super.onCreate(savedInstanceState);
        initViewModel();
        overrideNetwork = 0;
        getInfo();
        getPreviousMode();
    }

    private void getPreviousMode()
    {
        Intent intent = getIntent();
        if (token != null && token.isNonFungible())
        {
            showContract();
        }
        else if (intent != null)
        {
            AddressMode mode = AddressMode.values()[intent.getIntExtra(KEY_MODE, AddressMode.MODE_ADDRESS.ordinal())];
            if (mode == AddressMode.MODE_POS)
            {
                showPointOfSaleMode();
            }
            else
            {
                showAddress();
            }
        }
    }

    private void initViews() {
        toolbar();
        layoutInputAmount = findViewById(R.id.layout_define_request);
        qrImageView = findViewById(R.id.qr_image);
        qrImageView.setBackgroundResource(R.color.surface);
        ensFetchProgressBar = findViewById(R.id.ens_fetch_progress);

        if (viewModel == null) initViewModel();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(MyAddressViewModel.class);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        findViewById(R.id.layout_holder).setOnClickListener(view -> {
            if (getCurrentFocus() != null)
            {
                KeyboardUtils.hideKeyboard(getCurrentFocus());
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (amountInput != null) amountInput.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (CustomViewSettings.hideEIP681()) return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_receive, menu);

        switch (currentMode)
        {
            case MODE_ADDRESS:
                menu.findItem(R.id.action_my_address)
                        .setVisible(false);
                menu.findItem(R.id.action_networks)
                        .setVisible(false);
                break;
            case MODE_POS:
                menu.findItem(R.id.action_my_address)
                        .setVisible(false);
                menu.findItem(R.id.action_show_contract)
                        .setVisible(false);
                menu.findItem(R.id.action_networks)
                        .setVisible(false);
                break;
            case MODE_CONTRACT:
                menu.findItem(R.id.action_show_contract)
                        .setVisible(false);
                menu.findItem(R.id.action_networks)
                        .setVisible(false);
                break;
        }

        if (token == null || token.isEthereum())
        {
            menu.findItem(R.id.action_show_contract)
                    .setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_show_contract)
        {
            showContract();
        }
        else if (item.getItemId() == R.id.action_my_address)
        {
            showAddress();
        }
        else if (item.getItemId() == R.id.action_networks) {
            // TRON 전용 화면에서는 네트워크 선택 불필요
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPointOfSaleMode()
    {
        setContentView(R.layout.activity_eip681);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.GONE);
        setTitle("");
        
        // TRON 주소 가져오기
        String tronAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
        if (tronAddress == null || tronAddress.equals(wallet.address) || !tronAddress.startsWith("T")) {
            // TRON 주소가 없으면 생성 시도
            viewModel.generateTronAddressIfNeeded(wallet, this, () -> {
                String newTronAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
                if (newTronAddress != null && !newTronAddress.equals(wallet.address) && newTronAddress.startsWith("T")) {
                    displayAddress = newTronAddress;
                } else {
                    displayAddress = wallet.address;
                }
            });
            displayAddress = wallet.address; // 임시
        } else {
            displayAddress = tronAddress;
        }
        
        networkInfo = viewModel.getNetworkByChain(TRON_ID);
        currentMode = AddressMode.MODE_POS;
        layoutInputAmount.setVisibility(View.VISIBLE);

        amountInput = findViewById(R.id.input_amount);
        setupPOSMode(networkInfo);
    }

    private void setupPOSMode(NetworkInfo info)
    {
        String networkAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
        if (token == null) token = viewModel.getTokenService().getToken(TRON_ID, networkAddress);
        amountInput.setupToken(token, viewModel.getTokenService(), this);
        amountInput.setAmount("");
        updateCryptoAmount(BigDecimal.ZERO);
    }

    private void showAddress()
    {
        getInfo();
        setContentView(R.layout.activity_my_address);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);

        copyWalletName = findViewById(R.id.copy_wallet_name);
        copyAddress = findViewById(R.id.copy_address);

        if (amountInput != null)
        {
            amountInput.onDestroy();
            amountInput = null;
        }

        // TRON 주소 가져오기
        String tronAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
        
        Timber.d("=== TRON Address Debug ===");
        Timber.d("Original Wallet Address: %s", wallet.address);
        Timber.d("TRON Address (returned): %s", tronAddress);
        Timber.d("========================");
        
        // TRON 주소가 없으면 생성 시도
        if (tronAddress == null || tronAddress.equals(wallet.address) || !tronAddress.startsWith("T")) {
            viewModel.generateTronAddressIfNeeded(wallet, this, () -> {
                String newTronAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
                if (newTronAddress != null && !newTronAddress.equals(wallet.address) && newTronAddress.startsWith("T")) {
                    displayAddress = newTronAddress;
                    copyAddress.setFixedText(displayAddress);
                    qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, screenWidth));
                } else {
                    displayAddress = wallet.address;
                    copyAddress.setFixedText(displayAddress);
                    qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, screenWidth));
                }
            });
            displayAddress = wallet.address; // 임시로 기본 주소 표시
        } else {
            displayAddress = tronAddress;
        }
        
        setTitle(getString(R.string.my_wallet_address));
        copyAddress.setFixedText(displayAddress);
        currentMode = AddressMode.MODE_ADDRESS;
        if (getCurrentFocus() != null) {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
        }
        copyAddress.setVisibility(View.VISIBLE);
        onWindowFocusChanged(true);
        updateAddressWithENS(null); // TRON은 ENS 없음
    }

    private void showContract()
    {
        getInfo();
        setContentView(R.layout.activity_contract_address);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);
        copyAddress = findViewById(R.id.copy_address);
        copyAddress.setVisibility(View.VISIBLE);

        currentMode = AddressMode.MODE_CONTRACT;
        
        // TRON 네이티브 토큰인 경우 지갑의 TRON 주소를 표시
        if (token != null && token.isEthereum()) {
            String tronAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
            if (tronAddress != null && !tronAddress.equals(wallet.address) && tronAddress.startsWith("T")) {
                displayAddress = tronAddress;
                setTitle(getString(R.string.my_wallet_address));
            } else {
                viewModel.generateTronAddressIfNeeded(wallet, this, () -> {
                    String newTronAddress = viewModel.getAddressForNetwork(wallet.address, TRON_ID);
                    if (newTronAddress != null && !newTronAddress.equals(wallet.address) && newTronAddress.startsWith("T")) {
                        displayAddress = newTronAddress;
                        copyAddress.setText(displayAddress);
                    } else {
                        displayAddress = wallet.address;
                        copyAddress.setText(displayAddress);
                    }
                });
                displayAddress = wallet.address;
            }
        } else {
            // 일반 토큰의 경우 컨트랙트 주소 표시
            displayAddress = token != null ? token.getAddress() : wallet.address;
            setTitle(getString(R.string.contract_address));
        }
        
        copyAddress.setText(displayAddress);
        onWindowFocusChanged(true);
    }

    private void updateAddressWithENS(String ensName)
    {
        if (ensFetchProgressBar != null) {
            ensFetchProgressBar.setVisibility(View.GONE);
        }

        // TRON은 ENS 없음
        if (copyWalletName != null) {
            copyWalletName.setVisibility(View.GONE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            if (amountInput == null)
            {
                getInfo();
                qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, screenWidth));
                qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            }
            else
            {
                amountInput.setupToken(token, viewModel.getTokenService(), this);
            }
        }
    }

    private void getInfo()
    {
        if (viewModel == null) initViewModel();
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, TRON_ID);
        token = viewModel.getTokenService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        overrideNetwork = TRON_ID; // 항상 TRON 네트워크

        if (wallet == null)
        {
            finish();
        }
    }

    @Override
    public void amountReady(BigDecimal value, BigDecimal gasFee)
    {
        // unimplemented
    }

    @Override
    public void updateCryptoAmount(BigDecimal weiAmount)
    {
        if (token != null)
        {
            // TRON은 EIP681 형식이 아닐 수 있으므로 간단한 주소만 QR 코드로 표시
            qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, screenWidth));
        }
    }
}
