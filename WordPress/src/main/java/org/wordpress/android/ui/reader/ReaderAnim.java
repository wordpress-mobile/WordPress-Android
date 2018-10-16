package org.wordpress.android.ui.reader;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.wordpress.android.util.AniUtils;

public class ReaderAnim {
    /*
     * animation when user taps a like button
     */
    private enum ReaderButton {
        LIKE_ON, LIKE_OFF
    }

    public static void animateLikeButton(final View target, boolean isAskingToLike) {
        animateButton(target, isAskingToLike ? ReaderButton.LIKE_ON : ReaderButton.LIKE_OFF);
    }

    private static void animateButton(final View target, ReaderButton button) {
        if (target == null || button == null) {
            return;
        }

        ObjectAnimator animX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.2f);
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(1);

        ObjectAnimator animY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.2f);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        animY.setRepeatCount(1);

        AnimatorSet set = new AnimatorSet();

        switch (button) {
            case LIKE_ON:
            case LIKE_OFF:
                // rotate like button +/- 72 degrees (72 = 360/5, 5 is the number of points in the star)
                float endRotate = (button == ReaderButton.LIKE_ON ? 72f : -72f);
                ObjectAnimator animRotate = ObjectAnimator.ofFloat(target, View.ROTATION, 0f, endRotate);
                animRotate.setRepeatMode(ValueAnimator.REVERSE);
                animRotate.setRepeatCount(1);
                set.play(animX).with(animY).with(animRotate);
                break;
            default:
                set.play(animX).with(animY);
                break;
        }

        long durationMillis = AniUtils.Duration.SHORT.toMillis(target.getContext());
        set.setDuration(durationMillis);
        set.setInterpolator(new AccelerateDecelerateInterpolator());

        set.start();
    }
}
