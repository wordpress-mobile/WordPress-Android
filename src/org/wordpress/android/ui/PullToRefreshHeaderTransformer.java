package org.wordpress.android.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.AbsDefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.R;
import uk.co.senab.actionbarpulltorefresh.library.sdk.Compat;

public class PullToRefreshHeaderTransformer extends AbsDefaultHeaderTransformer {
    private View mHeaderView;
    private ViewGroup mContentLayout;
    private long mAnimationDuration;
    private boolean mShowProgressBarOnly;
    private Animation mHeaderOutAnimation;
    private OnTopScrollChangedListener mOnTopScrollChangedListener;

    public interface OnTopScrollChangedListener {
        public void onTopScrollChanged(boolean scrolledOnTop);
    }

    public void setShowProgressBarOnly(boolean progressBarOnly) {
        mShowProgressBarOnly = progressBarOnly;
    }

    @Override
    public void onViewCreated(Activity activity, View headerView) {
        super.onViewCreated(activity, headerView);
        mHeaderView = headerView;
        mContentLayout = (ViewGroup) headerView.findViewById(R.id.ptr_content);

        mHeaderOutAnimation = AnimationUtils.loadAnimation(activity,
                uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.R.anim.fade_out);
        mAnimationDuration = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (mHeaderOutAnimation != null) {
            final AnimationCallback callback = new AnimationCallback();
            if (mHeaderOutAnimation != null) {
                mHeaderOutAnimation.setAnimationListener(callback);
            }
        }
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

    class AnimationCallback implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (animation == mHeaderOutAnimation) {
                mHeaderView.setVisibility(View.GONE);
                onReset();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
