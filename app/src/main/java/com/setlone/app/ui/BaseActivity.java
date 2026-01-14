package com.setlone.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.setlone.app.R;
import com.setlone.app.entity.AuthenticationCallback;
import com.setlone.app.entity.AuthenticationFailType;
import com.setlone.app.entity.Operation;
import com.setlone.app.viewmodel.BaseViewModel;
import com.setlone.app.widget.AWalletAlertDialog;
import com.setlone.app.widget.SignTransactionDialog;

public abstract class BaseActivity extends AppCompatActivity
{
    public static AuthenticationCallback authCallback;  // Note: This static is only for signing callbacks
                                                        // which won't occur between wallet sessions - do not repeat this pattern
                                                        // for other code

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Edge-to-edge 디스플레이: 시스템 바는 표시하되 콘텐츠가 가려지지 않도록 처리
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public void setContentView(int layoutResID)
    {
        super.setContentView(layoutResID);
        setupWindowInsets();
    }

    @Override
    public void setContentView(View view)
    {
        super.setContentView(view);
        setupWindowInsets();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params)
    {
        super.setContentView(view, params);
        setupWindowInsets();
    }

    /**
     * Edge-to-edge 디스플레이: WindowInsets를 적용하여 시스템 바 공간 확보
     */
    protected void setupWindowInsets()
    {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null)
        {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                
                // 상단 상태바 공간 확보
                int statusBarHeight = insets.top;
                // 하단 네비게이션 바 공간 확보
                int navigationBarHeight = insets.bottom;
                
                // 루트 뷰에 패딩 적용
                if (rootView != null && rootView.getParent() instanceof ViewGroup)
                {
                    ViewGroup parent = (ViewGroup) rootView.getParent();
                    parent.setPadding(
                        parent.getPaddingLeft(),
                        statusBarHeight,
                        parent.getPaddingRight(),
                        navigationBarHeight
                    );
                }
                
                return windowInsets;
            });
        }
    }

    protected Toolbar toolbar()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null)
        {
            setSupportActionBar(toolbar);
            toolbar.setTitle(R.string.empty);
        }
        enableDisplayHomeAsUp();
        return toolbar;
    }

    protected void setTitle(String title)
    {
        ActionBar actionBar = getSupportActionBar();
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null)
        {
            if (actionBar != null)
            {
                actionBar.setTitle(R.string.empty);
            }
            toolbarTitle.setText(title);
        }

        setDispatcher();
    }

    protected void setSubtitle(String subtitle)
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setSubtitle(subtitle);
        }
    }

    protected void enableDisplayHomeAsUp()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void enableDisplayHomeAsUp(@DrawableRes int resourceId)
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(resourceId);
        }
    }

    protected void enableDisplayHomeAsHome(boolean active)
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(active);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_browser_home);
        }
    }

    protected void disableDisplayHomeAsUp()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    protected void hideToolbar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }
    }

    protected void showToolbar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            handleBackPressed();
            finish();
        }
        return true;
    }

    public void displayToast(String message)
    {
        if (message != null)
        {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            BaseViewModel.onPushToast(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        //Interpret the return code; if it's within the range of values possible to return from PIN confirmation then separate out
        //the task code from the return value. We have to do it this way because there's no way to send a bundle across the PIN dialog
        //and out through the PIN dialog's return back to here
        if (authCallback == null)
        {
            return;
        }

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            Operation taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            if (resultCode == RESULT_OK)
            {
                authCallback.authenticatePass(taskCode);
            }
            else
            {
                authCallback.authenticateFail("", AuthenticationFailType.PIN_FAILED, taskCode);
            }

            authCallback = null;
        }
    }

    protected void displayErrorMessage(String message)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.title_dialog_error);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void setDispatcher()
    {
        // Create an OnBackPressedCallback
        final OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed()
            {
                handleBackPressed();
            }
        };

        this.getOnBackPressedDispatcher().addCallback(callback);
    }

    public void handleBackPressed()
    {
        finish();
    };
}
