package org.wordpress.android.ui.reader.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.android.material.button.MaterialButton;

import org.wordpress.android.R;

/**
 * Follow button used in reader detail
 */
public class ReaderFollowButton extends MaterialButton {
    private boolean mIsFollowed;
    private boolean mShowCaption;

    public ReaderFollowButton(Context context) {
        super(context);
        initView(context, null);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.followButtonStyle);
        initView(context, attrs);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        // default to showing caption, then read the value from passed attributes
        mShowCaption = true;
        if (attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ReaderFollowButton, 0, 0);
            if (array != null) {
                mShowCaption = array.getBoolean(R.styleable.ReaderFollowButton_wpShowFollowButtonCaption, true);
            }
        }

        if (!mShowCaption) {
            hideCaptionAndEnlargeIcon(context);
        }
    }

    private void hideCaptionAndEnlargeIcon(Context context) {
        setText(null);
        int iconSz = context.getResources().getDimensionPixelSize(R.dimen.reader_follow_icon_no_caption);
        setIconSize(iconSz);
    }

    private void updateFollowTextAndIcon() {
        if (mShowCaption) {
            setText(mIsFollowed ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        }
        setSelected(mIsFollowed);

        int drawableId;
        if (mIsFollowed) {
            drawableId = R.drawable.ic_reader_following_white_24dp;
        } else {
            drawableId = R.drawable.ic_reader_follow_white_24dp;
        }
        Drawable drawable = getContext().getResources().getDrawable(drawableId, getContext().getTheme());
        setIcon(drawable);
    }

    public void setIsFollowed(boolean isFollowed) {
        setIsFollowed(isFollowed, false);
    }

    public void setIsFollowedAnimated(boolean isFollowed) {
        setIsFollowed(isFollowed, true);
    }

    private void setIsFollowed(boolean isFollowed, boolean animateChanges) {
        if (isFollowed == mIsFollowed && isSelected() == isFollowed) {
            return;
        }

        mIsFollowed = isFollowed;

        if (animateChanges) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 0f);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(1);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationRepeat(Animator animation) {
                    updateFollowTextAndIcon();
                }
            });

            long duration = getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
            AnimatorSet set = new AnimatorSet();
            set.play(anim);
            set.setDuration(duration);
            set.setInterpolator(new AccelerateDecelerateInterpolator());

            set.start();
        } else {
            updateFollowTextAndIcon();
        }
    }
}
