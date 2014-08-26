package org.wordpress.android.util.ptr;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;

import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.R;

import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.sdk.Compat;

public class PullToRefreshHeaderTransformer extends DefaultHeaderTransformer {
    private View mHeaderView;
    private ViewGroup mContentLayout;
    private TextView mTextView;
    private long mAnimationDuration;
    private boolean mShowProgressBarOnly;
    private Animation mHeaderOutAnimation;
    private OnTopScrollChangedListener mOnTopScrollChangedListener;
    private boolean mIsNetworkRefreshMode;
    private Context mContext;

    public interface OnTopScrollChangedListener {
        public void onTopScrollChanged(boolean scrolledOnTop);
    }

    public void setShowProgressBarOnly(boolean progressBarOnly) {
        mShowProgressBarOnly = progressBarOnly;
    }

    public boolean isNetworkRefreshMode() {
        return mIsNetworkRefreshMode;
    }

    public void setNetworkRefreshMode(boolean isNetworkRefresh) {
        mIsNetworkRefreshMode = isNetworkRefresh;
    }

    @Override
    public void onViewCreated(Activity activity, View headerView) {
        super.onViewCreated(activity, headerView);
        mContext = activity.getBaseContext();
        mHeaderView = headerView;
        mContentLayout = (ViewGroup) headerView.findViewById(R.id.ptr_content);
        mTextView = (TextView) headerView.findViewById(R.id.ptr_text);
        mAnimationDuration = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public boolean hideHeaderView() {
        mShowProgressBarOnly = false;
        return super.hideHeaderView();
    }

    @Override
    public boolean showHeaderView() {
        // Workaround to avoid this bug https://github.com/chrisbanes/ActionBar-PullToRefresh/issues/265
        // Note, that also remove the alpha animation
        resetContentLayoutAlpha();

        boolean changeVis = mHeaderView.getVisibility() != View.VISIBLE;
        mContentLayout.setVisibility(View.VISIBLE);
        if (changeVis) {
            if (isNetworkAvailableOrNotChecked()) {
                mTextView.setText(mContext.getText(R.string.pull_to_refresh_pull_label));
            } else {
                // Network mode enabled and network not available: show a different PTR label
                mTextView.setText(mContext.getText(R.string.pull_to_refresh_pull_no_network_label));
            }
            mHeaderView.setVisibility(View.VISIBLE);
            AnimatorSet animSet = new AnimatorSet();
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mHeaderView, "alpha", 0f, 1f);
            ObjectAnimator transAnim = ObjectAnimator.ofFloat(mContentLayout, "translationY",
                    -mContentLayout.getHeight(), 10f);
            animSet.playTogether(transAnim, alphaAnim);
            animSet.play(alphaAnim);
            animSet.setDuration(mAnimationDuration);
            animSet.start();
            if (mShowProgressBarOnly) {
                mContentLayout.setVisibility(View.INVISIBLE);
            }
        }
        return changeVis;
    }

    @Override
    public void onPulled(float percentagePulled) {
        super.onPulled(percentagePulled);
    }

    private void resetContentLayoutAlpha() {
        Compat.setAlpha(mContentLayout, 1f);
    }

    @Override
    public void onReset() {
        super.onReset();
        // Reset the Content Layout
        if (mContentLayout != null) {
            Compat.setAlpha(mContentLayout, 1f);
            mContentLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTopScrollChanged(boolean scrolledOnTop) {
        if (mOnTopScrollChangedListener != null) {
            mOnTopScrollChangedListener.onTopScrollChanged(scrolledOnTop);
        }
    }

    public void setOnTopScrollChangedListener(OnTopScrollChangedListener listener) {
        mOnTopScrollChangedListener = listener;
    }

    public boolean isNetworkAvailableOrNotChecked() {
        return !mIsNetworkRefreshMode || NetworkUtils.isNetworkAvailable(mContext);
    }
}
