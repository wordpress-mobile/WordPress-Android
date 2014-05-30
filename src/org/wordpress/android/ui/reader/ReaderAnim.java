package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ListView;

public class ReaderAnim {

    public static enum Duration {
        SHORT,
        MEDIUM,
        LONG;

        private long toMillis(Context context) {
            switch (this) {
                case LONG:
                    return context.getResources().getInteger(android.R.integer.config_longAnimTime);
                case MEDIUM:
                    return context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
                default:
                    return context.getResources().getInteger(android.R.integer.config_shortAnimTime);
            }
        }
    }

    private static ObjectAnimator getFadeInAnim(final View target, Duration duration) {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(target, View.ALPHA, 0.0f, 1.0f);
        fadeIn.setDuration(duration.toMillis(target.getContext()));
        fadeIn.setInterpolator(new LinearInterpolator());
        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                target.setVisibility(View.VISIBLE);
            }
        });
        return fadeIn;
    }

    private static ObjectAnimator getFadeOutAnim(final View target, Duration duration) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(target, View.ALPHA, 1.0f, 0.0f);
        fadeOut.setDuration(duration.toMillis(target.getContext()));
        fadeOut.setInterpolator(new LinearInterpolator());
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                target.setVisibility(View.GONE);
            }
        });
        return fadeOut;
    }

    public static void fadeIn(final View target, Duration duration) {
        if (target == null || duration == null) {
            return;
        }
        getFadeInAnim(target, duration).start();
    }

    public static void fadeOut(final View target, Duration duration) {
        if (target == null || duration == null) {
            return;
        }
        getFadeOutAnim(target, duration).start();
    }

    public static void fadeInFadeOut(final View target, Duration duration) {
        if (target == null || duration == null) {
            return;
        }

        ObjectAnimator fadeIn = getFadeInAnim(target, duration);
        ObjectAnimator fadeOut = getFadeOutAnim(target, duration);

        // keep view visible for passed duration before fading it out
        fadeOut.setStartDelay(duration.toMillis(target.getContext()));

        AnimatorSet set = new AnimatorSet();
        set.play(fadeOut).after(fadeIn);
        set.start();
    }

    public static void scaleInScaleOut(final View target, Duration duration) {
        if (target == null || duration == null) {
            return;
        }

        ObjectAnimator animX = ObjectAnimator.ofFloat(target, View.SCALE_X, 0f, 1f);
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(1);
        ObjectAnimator animY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 0f, 1f);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        animY.setRepeatCount(1);

        AnimatorSet set = new AnimatorSet();
        set.play(animX).with(animY);
        set.setDuration(duration.toMillis(target.getContext()));
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                target.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                target.setVisibility(View.GONE);
            }
        });
        set.start();
    }

    /*
     * animation when user taps a like/follow/reblog button
     */
    public static void animateLikeButton(final View target) {
        animateButton(target, true);
    }
    public static void animateReblogButton(final View target) {
        animateButton(target, true);
    }
    public static void animateFollowButton(final View target) {
        animateButton(target, false);
    }
    private static void animateButton(final View target, boolean rotate) {
        if (target == null) {
            return;
        }

        ObjectAnimator animX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.5f);
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(1);

        ObjectAnimator animY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.5f);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        animY.setRepeatCount(1);

        AnimatorSet set = new AnimatorSet();

        if (rotate) {
            ObjectAnimator animRotate = ObjectAnimator.ofFloat(target, View.ROTATION, 0f, 60f);
            animRotate.setRepeatMode(ValueAnimator.REVERSE);
            animRotate.setRepeatCount(1);
            set.play(animX).with(animY).with(animRotate);
        } else {
            set.play(animX).with(animY);
        }

        long durationMillis = Duration.SHORT.toMillis(target.getContext());
        set.setDuration(durationMillis);
        set.setInterpolator(new AccelerateDecelerateInterpolator());

        set.start();
    }

    public static void animateListItem(ListView listView,
                                int positionAbsolute,
                                Animation.AnimationListener listener,
                                int animResId) {
        if (listView == null) {
            return;
        }

        // passed value is the absolute position of this item, convert to relative or else we'll
        // remove the wrong item if list is scrolled
        int firstVisible = listView.getFirstVisiblePosition();
        int positionRelative = positionAbsolute - firstVisible;

        View listItem = listView.getChildAt(positionRelative);
        if (listItem == null) {
            return;
        }

        Animation animation = AnimationUtils.loadAnimation(listView.getContext(), animResId);
        if (listener != null) {
            animation.setAnimationListener(listener);
        }

        listItem.startAnimation(animation);
    }

}
