package com.setlone.app.widget;

/**
 * Created by James on 6/02/2018.
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.setlone.app.R;

public class ProgressView extends RelativeLayout {
    private ImageView progress;
    private TextView counter;
    private Context context;
    private AnimationDrawable loadingAnimation;

    public ProgressView(@NonNull Context context) {
        super(context);
    }

    public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_progress_view, this, false);
        addView(view);
        progress = view.findViewById(R.id.progress_v);
        counter = view.findViewById(R.id.textViewProgress);
        context = view.getContext();

        // loading_animation 설정
        loadingAnimation = (AnimationDrawable) ContextCompat.getDrawable(context, R.drawable.loading_animation);
        if (loadingAnimation != null) {
            progress.setImageDrawable(loadingAnimation);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            counter.setZ(1.0f);
            progress.setZ(0.99f);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 뷰가 윈도우에 연결되었을 때 애니메이션 시작 (표시 중인 경우)
        if (progress.getVisibility() == VISIBLE && loadingAnimation != null && !loadingAnimation.isRunning()) {
            loadingAnimation.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 뷰가 윈도우에서 분리될 때 애니메이션 중지
        if (loadingAnimation != null && loadingAnimation.isRunning()) {
            loadingAnimation.stop();
        }
    }

    public void updateProgress(Integer prog) {
        if (prog < 100) {
            counter.setText(prog + "%");
            progress.setVisibility(VISIBLE);
            counter.setVisibility(VISIBLE);
            setVisibility(VISIBLE);
            // 애니메이션 시작
            if (loadingAnimation != null && !loadingAnimation.isRunning()) {
                loadingAnimation.start();
            }
        } else {
            hide();
        }
    }

//    public void displayToast(String msg)
//    {
//        hide();
//        Toast.makeText(context, msg, Toast.LENGTH_SHORT);
//    }

    public void hide() {
        hideAllComponents();
        setVisibility(GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setZ(1.0f);
        }
    }

    private void hideAllComponents() {
        // 애니메이션 중지
        if (loadingAnimation != null && loadingAnimation.isRunning()) {
            loadingAnimation.stop();
        }
        progress.setVisibility(GONE);
        counter.setVisibility(GONE);
        setVisibility(VISIBLE);
    }

    public void showEmpty(View view) {
        hideAllComponents();
    }

    // ImageView로 변경되어 tint 관련 메서드는 더 이상 필요하지 않음
    // 필요시 ImageView에 colorFilter를 적용할 수 있음
}
