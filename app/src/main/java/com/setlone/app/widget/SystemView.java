package com.setlone.app.widget;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.setlone.app.R;

public class SystemView extends FrameLayout implements View.OnClickListener {
	private ImageView progress;
	private AnimationDrawable loadingAnimation;
	private View errorBox;
	private TextView messageTxt;
	private View tryAgain;

	private OnClickListener onTryAgainClickListener;
	private FrameLayout emptyBox;

	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;

	public SystemView(@NonNull Context context) {
		super(context);
	}

	public SystemView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public SystemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_system_view, this, false);
		addView(view);
		progress = view.findViewById(R.id.progress);

		// loading_animation 설정
		loadingAnimation = (AnimationDrawable) ContextCompat.getDrawable(getContext(), R.drawable.loading_animation);
		if (loadingAnimation != null) {
			progress.setImageDrawable(loadingAnimation);
		}

		errorBox = view.findViewById(R.id.error_box);
		messageTxt = view.findViewById(R.id.message);
		tryAgain = view.findViewById(R.id.try_again);
		tryAgain.setOnClickListener(this);

		emptyBox = view.findViewById(R.id.empty_box);
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

	public void attachSwipeRefreshLayout(@Nullable SwipeRefreshLayout swipeRefreshLayout) {
		this.swipeRefreshLayout = swipeRefreshLayout;
	}

	public void attachRecyclerView(@Nullable RecyclerView recyclerView) {
		this.recyclerView = recyclerView;
	}

	public void hide() {
		hideAllComponents();
		setVisibility(GONE);
	}

	private void hideAllComponents() {
		if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
			swipeRefreshLayout.setRefreshing(false);
		}
		// 애니메이션 중지
		if (loadingAnimation != null && loadingAnimation.isRunning()) {
			loadingAnimation.stop();
		}
		emptyBox.setVisibility(GONE);
		errorBox.setVisibility(GONE);
		progress.setVisibility(GONE);
		setVisibility(VISIBLE);
	}

	// Shows the central progress spinner
	public void showCentralSpinner()
	{
		hideAllComponents();
		progress.setVisibility(VISIBLE);
		// 애니메이션 시작
		if (loadingAnimation != null && !loadingAnimation.isRunning()) {
			loadingAnimation.start();
		}
	}

	public void showProgress(boolean shouldShow) {
	    if (shouldShow && swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
	        return;
        }
		if (shouldShow) {
			if (swipeRefreshLayout != null
					&& recyclerView != null
					&& recyclerView.getAdapter() != null
					&& recyclerView.getAdapter().getItemCount() > 0) {
				hide();
				swipeRefreshLayout.setRefreshing(true);
			} else {
				hideAllComponents();
				progress.setVisibility(VISIBLE);
				// 애니메이션 시작
				if (loadingAnimation != null && !loadingAnimation.isRunning()) {
					loadingAnimation.start();
				}
			}
		} else {
			hide();
		}
	}

	public void showSwipe()
	{
		swipeRefreshLayout.setRefreshing(true);
	}

	public void showProgress()
	{
		progress.setVisibility(VISIBLE);
		// 애니메이션 시작
		if (loadingAnimation != null && !loadingAnimation.isRunning()) {
			loadingAnimation.start();
		}
	}

	public void showError(@Nullable String message, @Nullable OnClickListener onTryAgainClickListener) {
		if (recyclerView != null && recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {
			hide();
			Snackbar.make(this,
					TextUtils.isEmpty(message) ? getContext().getString(R.string.unknown_error) : message,
					Snackbar.LENGTH_LONG).show();
		} else {
			hideAllComponents();
			errorBox.setVisibility(VISIBLE);
			messageTxt.setText(message);

			this.onTryAgainClickListener = onTryAgainClickListener;

			messageTxt.setVisibility(TextUtils.isEmpty(message) ? GONE : VISIBLE);
			tryAgain.setVisibility(this.onTryAgainClickListener == null ? GONE : VISIBLE);
		}
	}

	public void showEmpty() {
		showEmpty("");
	}

	public void showEmpty(@NonNull String message) {
		showError(message, null);
	}

	public void showEmpty(@LayoutRes int emptyLayout) {
		showEmpty(LayoutInflater.from(getContext())
				.inflate(emptyLayout, emptyBox, false));
	}

	public void showEmpty(View view) {
		hideAllComponents();
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_VERTICAL;
		view.setLayoutParams(lp);
		emptyBox.setVisibility(VISIBLE);
		emptyBox.removeAllViews();
		emptyBox.addView(view);
	}

	@Override
	public void onClick(View v) {
		if (onTryAgainClickListener != null) {
			hide();
			onTryAgainClickListener.onClick(v);
		}
	}
}
