package org.wordpress.android.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Build;
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
    private Animation mHeaderInAnimation, mHeaderOutAnimation;

    public void setShowProgressBarOnly(boolean progressBarOnly) {
        mShowProgressBarOnly = progressBarOnly;
    }

    @Override
    public void onViewCreated(Activity activity, View headerView) {
        super.onViewCreated(activity, headerView);
        mHeaderView = headerView;
        mContentLayout = (ViewGroup) headerView.findViewById(R.id.ptr_content);

        mHeaderInAnimation = AnimationUtils.loadAnimation(activity,
                uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.R.anim.fade_in);
        mHeaderOutAnimation = AnimationUtils.loadAnimation(activity,
                uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.R.anim.fade_out);
        mAnimationDuration = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);
        if (mHeaderOutAnimation != null || mHeaderInAnimation != null) {
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

    private boolean showHeaderViewICSAndPostICS() {
        boolean changeVis = mHeaderView.getVisibility() != View.VISIBLE;
        mContentLayout.setVisibility(View.VISIBLE);
        if (changeVis) {
            mHeaderView.setVisibility(View.VISIBLE);
            AnimatorSet animSet = new AnimatorSet();
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mHeaderView, "alpha", 0f, 1f);
            ObjectAnimator transAnim = ObjectAnimator.ofFloat(mContentLayout, "translationY",
                    -mContentLayout.getHeight(), 0f);
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

    private boolean showHeaderViewPreICS() {
        boolean changeVis = mHeaderView.getVisibility() != View.VISIBLE;
        mContentLayout.setVisibility(View.VISIBLE);
        if (changeVis) {
            if (mHeaderInAnimation != null) {
                mHeaderView.startAnimation(mHeaderInAnimation);
            }
            mHeaderView.setVisibility(View.VISIBLE);
            if (mShowProgressBarOnly) {
                mContentLayout.setVisibility(View.INVISIBLE);
            }
        }
        return changeVis;
    }

    @Override
    public boolean showHeaderView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return showHeaderViewICSAndPostICS();
        }
        return showHeaderViewPreICS();
    }

    @Override
    public void onPulled(float percentagePulled) {
        Compat.setAlpha(mContentLayout, 1f);
        super.onPulled(percentagePulled);
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
