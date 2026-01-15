package com.setlone.app.ui;

import static java.util.Collections.singletonList;

import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;

import androidx.lifecycle.ViewModelProvider;

import com.setlone.app.R;
import com.setlone.app.analytics.Analytics;
import com.setlone.app.entity.NetworkInfo;
import com.setlone.app.entity.StandardFunctionInterface;
import com.setlone.app.util.TronUtils;
import com.setlone.app.viewmodel.CustomNetworkViewModel;
import com.setlone.app.widget.FunctionButtonBar;
import com.setlone.app.widget.InputView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddCustomRPCNetworkActivity extends BaseActivity implements StandardFunctionInterface
{

    public static final String CHAIN_ID = "chain_id";
    private final Handler handler = new Handler();
    private CustomNetworkViewModel viewModel;
    private InputView nameInputView;
    private InputView rpcUrlInputView;
    private InputView chainIdInputView;
    private InputView symbolInputView;
    private InputView blockExplorerUrlInputView;
    private InputView blockExplorerApiUrl;
    private MaterialCheckBox testNetCheckBox;
    private long chainId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_custom_rpc_network);

        toolbar();
        setTitle(getString(R.string.title_activity_add_custom_rpcnetwork));

        nameInputView = findViewById(R.id.input_network_name);
        nameInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        nameInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        rpcUrlInputView = findViewById(R.id.input_network_rpc_url);
        rpcUrlInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        rpcUrlInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        chainIdInputView = findViewById(R.id.input_network_chain_id);
        chainIdInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        chainIdInputView.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        symbolInputView = findViewById(R.id.input_network_symbol);
        symbolInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        symbolInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        blockExplorerUrlInputView = findViewById(R.id.input_network_block_explorer_url);
        blockExplorerUrlInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        blockExplorerUrlInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        blockExplorerUrlInputView.getEditText().setHint("https://etherscan.com/tx/");

        blockExplorerApiUrl = findViewById(R.id.input_network_explorer_api);
        blockExplorerApiUrl.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);
        blockExplorerApiUrl.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        blockExplorerApiUrl.getEditText().setHint("https://api.etherscan.io/api?");

        testNetCheckBox = findViewById(R.id.checkbox_testnet);

        initViewModel();

        chainId = getIntent().getLongExtra(CHAIN_ID, -1);
        isEditMode = chainId >= 0;

        if (isEditMode)
        {
            setTitle(getString(R.string.title_network_info));
            // get network info and fill ui
            NetworkInfo network = viewModel.getNetworkInfo(chainId);

            renderNetwork(network);

            List<Integer> buttons = new ArrayList<>();
            buttons.add(R.string.action_update_network);

            if (!network.isCustom)
            {
                chainIdInputView.getEditText().setEnabled(false);
                buttons.add(R.string.action_reset_network);
            }
            addFunctionBar(buttons);
        }
        else
        {
            addFunctionBar(singletonList(R.string.action_add_network));
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.track(Analytics.Navigation.ADD_CUSTOM_NETWORK);
    }

    private void addFunctionBar(List<Integer> functionResources)
    {
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, functionResources);
        functionBar.revealButtons();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(CustomNetworkViewModel.class);
    }

    private boolean validateInputs()
    {
        if (TextUtils.isEmpty(nameInputView.getText()))
        {
            nameInputView.setError(getString(R.string.error_field_required));
            return false;
        }

        if (TextUtils.isEmpty(rpcUrlInputView.getText()))
        {
            rpcUrlInputView.setError(getString(R.string.error_field_required));
            return false;
        }
        else if (!URLUtil.isValidUrl(rpcUrlInputView.getText().toString()))
        {
            rpcUrlInputView.setError(getString(R.string.error_invalid_url));
            return false;
        }

        if (TextUtils.isEmpty(chainIdInputView.getText()))
        {
            chainIdInputView.setError(getString(R.string.error_field_required));
            return false;
        }
        else
        {
            try
            {
                Long.parseLong(chainIdInputView.getText().toString());
            }
            catch (NumberFormatException ex)
            {
                chainIdInputView.setError(getString(R.string.error_must_numeric));
                return false;
            }
        }

        long newChainId = Long.parseLong(chainIdInputView.getText().toString());
        
        // TRON 네트워크는 EVM 호환이 아니므로 별도 처리 필요
        // TRON은 RPC URL이 아닌 HTTP API를 사용하므로 경고 표시
        if (com.setlone.app.util.TronUtils.isTronChain(newChainId))
        {
            // TRON은 이미 built-in 네트워크로 추가되어 있으므로
            // 사용자가 수동으로 추가하려는 경우 경고 표시
            // TODO: TRON 네트워크 추가 시 별도 UI 또는 안내 메시지 표시
        }
        
        long chainId = getIntent().getLongExtra(CHAIN_ID, -1);
        if (newChainId != chainId)
        {
            NetworkInfo network = viewModel.getNetworkInfo(newChainId);
            if (network != null)
            {
                chainIdInputView.setError(getString(R.string.error_chainid_already_taken));
                return false;
            }
        }

        if (TextUtils.isEmpty(symbolInputView.getText()))
        {
            symbolInputView.setError(getString(R.string.error_field_required));
            return false;
        }

        //Allow blank for these
        /*if (TextUtils.isEmpty(blockExplorerUrlInputView.getText())) {
            blockExplorerUrlInputView.setError(getString(R.string.error_field_required));
            return false;
        } else*/
        if (!TextUtils.isEmpty(blockExplorerUrlInputView.getText().toString()) && !URLUtil.isValidUrl(blockExplorerUrlInputView.getText().toString()))
        {
            blockExplorerUrlInputView.setError(getString(R.string.error_invalid_url));
            return false;
        }

        /*if (TextUtils.isEmpty(blockExplorerApiUrl.getText())) {
            blockExplorerApiUrl.setError(getString(R.string.error_field_required));
            return false;
        } else*/
        if (!TextUtils.isEmpty(blockExplorerApiUrl.getText().toString()) && !URLUtil.isValidUrl(blockExplorerApiUrl.getText().toString()))
        {
            blockExplorerApiUrl.setError(getString(R.string.error_invalid_url));
            return false;
        }

        return true;
    }

    private void resetValidateErrors()
    {
        nameInputView.setError(null);
        rpcUrlInputView.setError(null);
        chainIdInputView.setError(null);
        symbolInputView.setError(null);
        blockExplorerUrlInputView.setError(null);
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (actionId == R.string.action_reset_network)
        {
            resetDefault();
            return;
        }

        if (validateInputs())
        {
            viewModel.saveNetwork(
                    isEditMode,
                    nameInputView.getText().toString(),
                    rpcUrlInputView.getText().toString(),
                    Long.parseLong(chainIdInputView.getText().toString()),
                    symbolInputView.getText().toString(),
                    blockExplorerUrlInputView.getText().toString(),
                    blockExplorerApiUrl.getText().toString(), testNetCheckBox.isChecked(), chainId != -1L ? chainId : null);
            finish();
        }
        else
        {
            handler.postDelayed(this::resetValidateErrors, 2000);
        }
    }

    private void resetDefault()
    {
        NetworkInfo network = viewModel.getBuiltInNetwork(chainId);
        renderNetwork(network);
    }

    private void renderNetwork(NetworkInfo network)
    {
        nameInputView.setText(network.name);
        rpcUrlInputView.setText(network.rpcServerUrl.replaceAll("(/)([0-9a-fA-F]{32})", "/********************************"));
        chainIdInputView.setText(String.valueOf(network.chainId));
        symbolInputView.setText(network.symbol);
        blockExplorerUrlInputView.setText(network.etherscanUrl);
        blockExplorerApiUrl.setText(network.etherscanAPI);
        testNetCheckBox.setChecked(viewModel.isTestNetwork(network));
    }
}
