package org.wordpress.android.ui.reader.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;

/**
 * Follow button used in reader detail
 */
public class ReaderFollowButton extends LinearLayout {
    private TextView mTextFollow;
    private ImageView mImageFollow;
    private boolean mIsFollowed;
    private boolean mShowCaption;

    public ReaderFollowButton(Context context) {
        super(context);
        initView(context, null);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        inflate(context, R.layout.reader_follow_button, this);
        mTextFollow = (TextView) findViewById(R.id.text_follow_button);
        mImageFollow = (ImageView) findViewById(R.id.image_follow_button);

        // default to showing caption, then read the value from passed attributes
        mShowCaption = true;
        if (attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ReaderFollowButton, 0, 0);
            if (array != null) {
                mShowCaption = array.getBoolean(R.styleable.ReaderFollowButton_wpShowFollowButtonCaption, true);
            }
        }

        // hide follow text and enlarge the follow icon if there's no caption
        if (!mShowCaption) {
            mTextFollow.setText(null);
            mTextFollow.setVisibility(View.GONE);
            int iconSz = context.getResources().getDimensionPixelSize(R.dimen.reader_follow_icon_no_caption);
            mImageFollow.getLayoutParams().width = iconSz;
            mImageFollow.getLayoutParams().height = iconSz;
        }
    }

    private void updateFollowText() {
        if (mShowCaption) {
            mTextFollow.setText(mIsFollowed ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
        }
        mTextFollow.setSelected(mIsFollowed);

        // show green icon if site is followed, gray icon if not followed and there's a caption,
        // blue icon if not followed and there is no caption
        int drawableId;
        int colorId;
        if (mIsFollowed) {
            drawableId = R.drawable.ic_reader_following_white_24dp;
            colorId = ContextExtensionsKt.getColorResIdFromAttribute(getContext(), R.attr.wpColorSuccess);
        } else {
            drawableId = R.drawable.ic_reader_follow_white_24dp;
            colorId = ContextExtensionsKt.getColorResIdFromAttribute(getContext(), R.attr.colorPrimary);
        }
        ColorUtils.INSTANCE.setImageResourceWithTint(mImageFollow, drawableId, colorId);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTextFollow.setEnabled(enabled);
        mImageFollow.setEnabled(enabled);
    }

    public void setIsFollowed(boolean isFollowed) {
        setIsFollowed(isFollowed, false);
    }

    public void setIsFollowedAnimated(boolean isFollowed) {
        setIsFollowed(isFollowed, true);
    }

    private void setIsFollowed(boolean isFollowed, boolean animateChanges) {
        if (isFollowed == mIsFollowed && mTextFollow.isSelected() == isFollowed) {
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
                    updateFollowText();
                }
            });

            long duration = getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
            AnimatorSet set = new AnimatorSet();
            set.play(anim);
            set.setDuration(duration);
            set.setInterpolator(new AccelerateDecelerateInterpolator());

            set.start();
        } else {
            updateFollowText();
        }
    }
}
