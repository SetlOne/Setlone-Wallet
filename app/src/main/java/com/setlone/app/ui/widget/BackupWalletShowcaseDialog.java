package com.setlone.app.ui.widget;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.setlone.app.R;

/**
 * Backup wallet showcase dialog to replace TutoShowcase library.
 * Displays a full-screen overlay with custom layout and handles user interactions.
 */
public class BackupWalletShowcaseDialog extends DialogFragment
{
    public interface OnShowcaseDismissedListener
    {
        void onShowcaseDismissed();
        void onSettingsClicked();
    }

    private OnShowcaseDismissedListener listener;
    private int originalStatusBarColor;

    public static BackupWalletShowcaseDialog newInstance()
    {
        return new BackupWalletShowcaseDialog();
    }

    public void setOnShowcaseDismissedListener(OnShowcaseDismissedListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        Window window = dialog.getWindow();
        if (window != null)
        {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                          WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            
            // Save original status bar color
            originalStatusBarColor = window.getStatusBarColor();
        }
        
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.showcase_backup_wallet, container, false);
        
        int backgroundColor = ContextCompat.getColor(requireContext(), R.color.translucent_dark);
        rootView.setBackgroundColor(backgroundColor);
        
        // Set status bar color
        Window window = getDialog().getWindow();
        if (window != null)
        {
            window.setStatusBarColor(backgroundColor);
        }
        
        // Close button
        Button btnClose = rootView.findViewById(R.id.btn_close);
        if (btnClose != null)
        {
            btnClose.setOnClickListener(v -> dismissShowcase());
        }
        
        // Click on layout to dismiss
        View showcaseLayout = rootView.findViewById(R.id.showcase_layout);
        if (showcaseLayout != null)
        {
            showcaseLayout.setOnClickListener(v -> dismissShowcase());
        }
        
        return rootView;
    }

    @Override
    public void onDestroyView()
    {
        // Restore original status bar color
        Window window = getDialog().getWindow();
        if (window != null)
        {
            window.setStatusBarColor(originalStatusBarColor);
        }
        super.onDestroyView();
    }

    private void dismissShowcase()
    {
        if (listener != null)
        {
            listener.onShowcaseDismissed();
        }
        dismiss();
    }

    public void onSettingsClicked()
    {
        if (listener != null)
        {
            listener.onSettingsClicked();
        }
        dismiss();
    }
}
