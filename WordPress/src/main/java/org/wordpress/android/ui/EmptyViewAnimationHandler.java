package org.wordpress.android.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.util.AniUtils;

/**
 * Animates transitions between <em>no content</em> screens (image and caption) and
 * <em>loading</em> screens (caption only).
 */
public class EmptyViewAnimationHandler implements ObjectAnimator.AnimatorListener {
    /**
     * Interface used to update the calling activity on the animation's progress.
     */
    public interface OnAnimationProgressListener {
        public void onSequenceStarted(EmptyViewMessageType emptyViewMessageType);
        public void onNewTextFadingIn();
    }

    public enum AnimationStage {
        PRE_ANIMATION,
        FADE_OUT_IMAGE, SLIDE_TEXT_UP, FADE_OUT_NO_CONTENT_TEXT, FADE_IN_LOADING_TEXT,
        IN_BETWEEN,
        FADE_OUT_LOADING_TEXT, FADE_IN_NO_CONTENT_TEXT, SLIDE_TEXT_DOWN, FADE_IN_IMAGE,
        FINISHED;

        public AnimationStage next() {
            return AnimationStage.values()[ordinal() + 1];
        }

        public boolean isLowerThan(AnimationStage animationStage) {
            return (ordinal() < animationStage.ordinal());
        }

        public int stagesRemaining() {
            return (IN_BETWEEN.ordinal() - ordinal());
        }
    }

    private static final int ANIMATION_DURATION = 150;
    private static final int MINIMUM_LOADING_DURATION = 500;

    private final View mEmptyViewImage;
    private final TextView mEmptyViewTitle;
    private final OnAnimationProgressListener mListener;

    private AnimationStage mAnimationStage = AnimationStage.PRE_ANIMATION;
    private boolean mHasDisplayedLoadingSequence;

    private ObjectAnimator mObjectAnimator;

    private final int mSlideDistance;

    /**
     * Create a new <code>EmptyViewAnimationHandler</code> with the given UI elements to animate.
     *
     * @param emptyViewTitle the caption <code>TextView</code> shared between the
     *                       <em>no content</em> and <em>loading</em> screens
     * @param emptyViewImage the <em>no content</em> image <code>View</code>
     * @param listener       the listener for UI change callbacks
     */
    public EmptyViewAnimationHandler(TextView emptyViewTitle, View emptyViewImage,
                                     OnAnimationProgressListener listener) {
        mListener = listener;

        mEmptyViewImage = emptyViewImage;
        mEmptyViewTitle = emptyViewTitle;

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mEmptyViewImage.getLayoutParams();
        mSlideDistance = -((layoutParams.height + layoutParams.bottomMargin + layoutParams.topMargin) / 2);
    }

    public boolean isShowingLoadingAnimation() {
        return (mAnimationStage != AnimationStage.PRE_ANIMATION &&
                mAnimationStage.isLowerThan(AnimationStage.IN_BETWEEN));
    }

    public boolean isBetweenSequences() {
        return (mAnimationStage == AnimationStage.IN_BETWEEN);
    }

    public void clear() {
        mAnimationStage = AnimationStage.PRE_ANIMATION;
        mHasDisplayedLoadingSequence = false;
    }

    void startAnimation(Object target, String propertyName, float fromValue, float toValue) {
        if (mObjectAnimator != null) {
            mObjectAnimator.removeAllListeners();
        }

        mObjectAnimator = ObjectAnimator.ofFloat(target, propertyName, fromValue, toValue);
        mObjectAnimator.setDuration(ANIMATION_DURATION);
        mObjectAnimator.addListener(this);

        mObjectAnimator.start();
    }

    public void showLoadingSequence() {
        mAnimationStage = AnimationStage.FADE_OUT_IMAGE;
        mListener.onSequenceStarted(EmptyViewMessageType.LOADING);
        startAnimation(mEmptyViewImage, "alpha", 1f, 0f);
    }

    public void showNoContentSequence() {
        // If the data was auto-refreshed, the NO_CONTENT > LOADING sequence was not shown before this one, and
        // some special handling will be needed
        mHasDisplayedLoadingSequence = (isShowingLoadingAnimation() || isBetweenSequences());

        if (isShowingLoadingAnimation()) {
            // Delay by enough time to complete the remaining stages of the currently on-going animation,
            // additionally allowing the loading message to display for MINIMUM_LOADING_DURATION
            int delayTime = (mAnimationStage.stagesRemaining() * ANIMATION_DURATION) + MINIMUM_LOADING_DURATION;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAnimationStage = AnimationStage.FADE_OUT_LOADING_TEXT;
                    mListener.onSequenceStarted(EmptyViewMessageType.NO_CONTENT);
                    startAnimation(mEmptyViewTitle, "alpha", 1f, 0.1f);
                }
            }, delayTime);
        } else {
            mAnimationStage = AnimationStage.FADE_OUT_LOADING_TEXT;
            mListener.onSequenceStarted(EmptyViewMessageType.NO_CONTENT);
            startAnimation(mEmptyViewTitle, "alpha", 1f, 0.1f);
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mAnimationStage = mAnimationStage.next();

        // A step in the animation has completed. Set up the next animation in the chain
        switch (mAnimationStage) {
            // NO_CONTENT > LOADING section
            case SLIDE_TEXT_UP:
                startAnimation(mEmptyViewTitle, "translationY", 0, mSlideDistance);
                break;
            case FADE_OUT_NO_CONTENT_TEXT:
                startAnimation(mEmptyViewTitle, "alpha", 1f, 0.1f);
                break;
            case FADE_IN_LOADING_TEXT:
                mListener.onNewTextFadingIn();

                startAnimation(mEmptyViewTitle, "alpha", 0.1f, 1f);
                break;

            // LOADING > NO_CONTENT section
            case FADE_IN_NO_CONTENT_TEXT:
                mListener.onNewTextFadingIn();

                startAnimation(mEmptyViewTitle, "alpha", 0.1f, 1f);
                break;
            case SLIDE_TEXT_DOWN:
                startAnimation(mEmptyViewTitle, "translationY", mSlideDistance, 0);

                if (!mHasDisplayedLoadingSequence) {
                    // Force mEmptyViewImage to take up space in the layout, so that mEmptyViewTitle lands
                    // where it should at the end of its slide animation
                    mEmptyViewImage.setVisibility(View.INVISIBLE);
                }
                break;
            case FADE_IN_IMAGE:
                if (!mHasDisplayedLoadingSequence) {
                    // Uses AlphaAnimation instead of an ObjectAnimator to address a display glitch
                    // in the auto-refresh case
                    AniUtils.startAnimation(mEmptyViewImage, android.R.anim.fade_in, ANIMATION_DURATION);
                    mEmptyViewImage.setVisibility(View.VISIBLE);
                } else {
                    startAnimation(mEmptyViewImage, "alpha", 0f, 1f);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        mAnimationStage = AnimationStage.PRE_ANIMATION;
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
