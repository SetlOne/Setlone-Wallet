package com.setlone.app.ui;

import static com.setlone.app.C.Key.WALLET;
import static com.setlone.app.widget.AWalletAlertDialog.ERROR;
import static com.setlone.ethereum.EthereumNetworkBase.TRON_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.setlone.app.C;
import com.setlone.app.R;
import com.setlone.app.entity.StandardFunctionInterface;
import com.setlone.app.entity.Wallet;
import com.setlone.app.entity.tokens.Token;
import com.setlone.app.ui.widget.entity.AddressReadyCallback;
import com.setlone.app.ui.widget.entity.AmountReadyCallback;
import com.setlone.app.util.KeyboardUtils;
import com.setlone.app.util.TronUtils;
import com.setlone.app.util.Utils;
import com.setlone.app.viewmodel.SendViewModel;
import com.setlone.app.widget.AWalletAlertDialog;
import com.setlone.app.widget.FunctionButtonBar;
import com.setlone.app.widget.InputAddress;
import com.setlone.app.widget.InputAmount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * TRON 네트워크 전용 전송 화면
 * SendActivity와 유사하지만 TRON 네트워크에 특화된 기능 제공
 */
@AndroidEntryPoint
public class TronSendActivity extends BaseActivity implements AmountReadyCallback, StandardFunctionInterface, AddressReadyCallback
{
    private static final BigDecimal NEGATIVE = BigDecimal.ZERO.subtract(BigDecimal.ONE);
    SendViewModel viewModel;
    private Wallet wallet;
    private Token token;
    private final Handler handler = new Handler();
    private AWalletAlertDialog dialog;

    private InputAmount amountInput;
    private InputAddress addressInput;
    private String sendAddress;
    private BigDecimal sendAmount;
    private AWalletAlertDialog alertDialog;

    @Nullable
    private Disposable calcGasCost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        toolbar();

        viewModel = new ViewModelProvider(this)
                .get(SendViewModel.class);

        String contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        wallet = getIntent().getParcelableExtra(WALLET);
        // 항상 TRON 네트워크 사용
        token = viewModel.getToken(TRON_ID, getIntent().getStringExtra(C.EXTRA_ADDRESS));

        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);

        sendAddress = null;
        sendAmount = NEGATIVE;

        if (!checkTokenValidity(contractAddress))
        {
            return;
        }

        setTitle(getString(R.string.action_send_tkn, token.getShortName()));
        setupTokenContent();
    }

    private boolean checkTokenValidity(String contractAddress)
    {
        if (token == null || token.tokenInfo == null)
        {
            token = viewModel.getToken(TRON_ID, contractAddress);

            if (token == null)
            {
                finish();
            }
        }

        return (token != null);
    }

    private void onBack()
    {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void handleBackPressed()
    {
        onBack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBack();
        }
        else if (item.getItemId() == R.id.action_show_contract)
        {
            viewModel.showContractInfo(this, wallet, token);
        }

        return false;
    }

    @Override
    protected void onDestroy()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
        super.onDestroy();
        if (viewModel != null) viewModel.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (amountInput != null) amountInput.onDestroy();
    }

    private void setupTokenContent()
    {
        amountInput = findViewById(R.id.input_amount);
        amountInput.setupToken(token, viewModel.getTokenService(), this);
        addressInput = findViewById(R.id.input_address);
        addressInput.setAddressCallback(this);
        addressInput.setChainOverrideForWalletConnect(TRON_ID);
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.revealButtons();
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.action_next));
        functionBar.setupFunctions(this, functions);
    }

    @Override
    public void amountReady(BigDecimal value, BigDecimal gasPrice)
    {
        // TRON은 Gas 개념이 없으므로 gasPrice 무시
        if ((token.isEthereum() && token.balance.subtract(value).compareTo(BigDecimal.ZERO) > 0)
                || (token.getBalanceRaw().subtract(value).compareTo(BigDecimal.ZERO) >= 0))
        {
            sendAmount = value;
            calculateTransactionCost();
        }
        else
        {
            sendAmount = NEGATIVE;
            amountInput.showError(true, 0);
            addressInput.stopNameCheck();
        }
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (actionId == R.string.action_next)
        {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
            amountInput.getInputAmount();
            addressInput.getAddress();
        }
    }

    @Override
    public void addressReady(String address, String ensName)
    {
        sendAddress = address;
        // TRON 주소 검증
        if (!TronUtils.isValidTronAddress(address))
        {
            addressInput.setError(getString(R.string.error_invalid_address));
        }
        else
        {
            calculateTransactionCost();
        }
    }

    private void calculateTransactionCost()
    {
        if ((calcGasCost != null && !calcGasCost.isDisposed()) ||
                (dialog != null && dialog.isShowing())) return;

        if (sendAmount.compareTo(NEGATIVE) > 0 && TronUtils.isValidTronAddress(sendAddress))
        {
            final String txSendAddress = sendAddress;
            sendAddress = null;
            
            // TRON은 Gas 개념이 없으므로 바로 확인 다이얼로그 표시
            showTronConfirmDialog(txSendAddress);
        }
    }
    
    /**
     * TRON 전송 확인 다이얼로그 표시
     */
    private void showTronConfirmDialog(String toAddress)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(getString(R.string.confirm_transaction));
        String message = String.format("Send %s %s to %s?", sendAmount.toString(), token.getShortName(), toAddress);
        dialog.setMessage(message);
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
            sendTronTransaction(toAddress);
        });
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> dialog.dismiss());
        dialog.show();
    }
    
    /**
     * TRON 트랜잭션 전송
     */
    private void sendTronTransaction(String toAddress)
    {
        // 인증 후 전송
        viewModel.getMnemonic(this, wallet, new com.setlone.app.entity.CreateWalletCallbackInterface() {
            @Override
            public void HDKeyCreated(String address, android.content.Context ctx, com.setlone.app.service.KeyService.AuthenticationLevel level) {
                // 인증 성공
            }

            @Override
            public void keyFailure(String message) {
                displayErrorMessage(message);
            }

            @Override
            public void cancelAuthentication() {
                // 인증 취소
            }

            @Override
            public void fetchMnemonic(String mnemonic) {
                // 니모닉을 받아서 TRON 트랜잭션 전송
                if (calcGasCost != null && !calcGasCost.isDisposed()) {
                    calcGasCost.dispose();
                }
                calcGasCost = viewModel.sendTronTransaction(wallet, toAddress, sendAmount, mnemonic)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                txHash -> {
                                    Intent intent = new Intent();
                                    intent.putExtra(C.EXTRA_TXHASH, txHash);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                },
                                error -> {
                                    Timber.e(error, "Failed to send TRON transaction");
                                    displayErrorMessage(error.getMessage());
                                }
                        );
            }
        });
    }

    protected void displayErrorMessage(String message)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void txWritten(com.setlone.app.entity.TransactionReturn txData)
    {
        // Transaction written successfully
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_TXHASH, txData.hash);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void txError(com.setlone.app.entity.TransactionReturn txError)
    {
        Timber.d("txError: %s", txError.throwable.getMessage());
        displayErrorMessage(txError.throwable.getMessage());
    }
}
