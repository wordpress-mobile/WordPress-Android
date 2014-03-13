package org.wordpress.android.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

public class OnTopMessage {
    private LinearLayout mLayout;
    private TextView mTextView;
    private AlphaAnimation mHideAnimation;
    private AlphaAnimation mShowAnimation;

    public OnTopMessage(Activity activity) {
        ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
        init(activity, root);
    }

    public OnTopMessage(Activity activity, ViewGroup root) {
        init(activity, root);
    }

    private void init(Activity activity, ViewGroup root) {
        View v = activity.getLayoutInflater().inflate(R.layout.on_top_message, root);
        mLayout = (LinearLayout) v.findViewById(R.id.otm_layout);
        mLayout.setClickable(true);
        mLayout.setVisibility(View.GONE);
        mTextView = (TextView) v.findViewById(R.id.otm_message);
        mHideAnimation = new AlphaAnimation(1.0f, 0.0f);
        mHideAnimation.setInterpolator(new AccelerateInterpolator());
        mHideAnimation.setDuration(activity.getResources().getInteger(android.R.integer.config_mediumAnimTime));
        mHideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mShowAnimation = new AlphaAnimation(0.0f, 1.0f);
        mShowAnimation.setInterpolator(new AccelerateInterpolator());
        mShowAnimation.setDuration(activity.getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    public void setMessage(String message) {
        mTextView.setText(message);
    }

    public void show() {
        mLayout.setVisibility(View.VISIBLE);
    }

    public void showAnimated() {
        mLayout.setVisibility(View.VISIBLE);
        mLayout.startAnimation(mShowAnimation);
    }

    public void hideAnimated() {
        mLayout.startAnimation(mHideAnimation);
    }

    public void setTopMargin(int topMargin) {
        LayoutParams layoutParams = (LayoutParams) mLayout.getLayoutParams();
        layoutParams.setMargins(layoutParams.leftMargin, topMargin, layoutParams.rightMargin,
                layoutParams.bottomMargin);
        mLayout.setLayoutParams(layoutParams);
    }
}
