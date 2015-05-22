package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

import java.lang.ref.WeakReference;

public class ReaderAnim {

    public interface AnimationEndListener {
        void onAnimationEnd();
    }

    public enum Duration {
        SHORT,
        MEDIUM,
        LONG;

        public long toMillis(Context context) {
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

    public static void scaleIn(final View target, Duration duration) {
        if (target == null || duration == null) {
            return;
        }

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY);
        animator.setDuration(duration.toMillis(target.getContext()));
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                target.setVisibility(View.VISIBLE);
            }
        });

        animator.start();
    }

    public static void scaleOut(final View target,
                                final int endVisibility,
                                Duration duration,
                                final AnimationEndListener endListener) {
        if (target == null || duration == null) {
            return;
        }

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY);
        animator.setDuration(duration.toMillis(target.getContext()));
        animator.setInterpolator(new AccelerateInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                target.setVisibility(endVisibility);
                if (endListener != null) {
                    endListener.onAnimationEnd();
                }
            }
        });

        animator.start();
    }

    /*
     * animation when user taps a like/reblog button
     */
    private enum ReaderButton { LIKE_ON, LIKE_OFF, REBLOG}
    public static void animateLikeButton(final View target, boolean isAskingToLike) {
        animateButton(target, isAskingToLike ? ReaderButton.LIKE_ON : ReaderButton.LIKE_OFF);
    }
    public static void animateReblogButton(final View target) {
        animateButton(target, ReaderButton.REBLOG);
    }
    private static void animateButton(final View target, ReaderButton button) {
        if (target == null || button == null) {
            return;
        }

        ObjectAnimator animX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.75f);
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(1);

        ObjectAnimator animY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.75f);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        animY.setRepeatCount(1);

        AnimatorSet set = new AnimatorSet();

        switch (button) {
            case LIKE_ON: case LIKE_OFF:
                // rotate like button +/- 72 degrees (72 = 360/5, 5 is the number of points in the star)
                float endRotate = (button == ReaderButton.LIKE_ON ? 72f : -72f);
                ObjectAnimator animRotate = ObjectAnimator.ofFloat(target, View.ROTATION, 0f, endRotate);
                animRotate.setRepeatMode(ValueAnimator.REVERSE);
                animRotate.setRepeatCount(1);
                set.play(animX).with(animY).with(animRotate);
                // on Android 4.4.3 the rotation animation may cause the drawable to fade out unless
                // we set the layer type - https://code.google.com/p/android/issues/detail?id=70914
                target.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                break;
            default:
                set.play(animX).with(animY);
                break;
        }

        long durationMillis = Duration.SHORT.toMillis(target.getContext());
        set.setDuration(durationMillis);
        set.setInterpolator(new AccelerateDecelerateInterpolator());

        set.start();
    }

    /*
     * used when animating a toolbar in/out
     */
    public static void animateTopBar(View view, boolean show) {
        animateBar(view, show, true);
    }
    public static void animateBottomBar(View view, boolean show) {
        animateBar(view, show, false);
    }
    private static void animateBar(final View view,
                                   final boolean show,
                                   final boolean isTopBar) {
        int newVisibility = (show ? View.VISIBLE : View.GONE);
        if (view == null || view.getVisibility() == newVisibility) {
            return;
        }

        float fromY;
        float toY;
        if (isTopBar) {
            fromY = (show ? -1f : 0f);
            toY   = (show ? 0f : -1f);
        } else {
            fromY = (show ? 1f : 0f);
            toY   = (show ? 0f : 1f);
        }
        Animation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, fromY,
                Animation.RELATIVE_TO_SELF, toY);

        long durationMillis = Duration.SHORT.toMillis(view.getContext());
        animation.setDuration(durationMillis);

        if (show) {
            animation.setInterpolator(new DecelerateInterpolator());
        } else {
            animation.setInterpolator(new AccelerateInterpolator());
        }

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (show) {
                    view.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                if (!show) {
                    view.setVisibility(View.GONE);
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
                // noop
            }
        });

        view.clearAnimation();
        view.startAnimation(animation);
    }

    /*
     * in/out animation for floating action button
     */
    public static void showFab(View fabView, boolean show) {
        animateBar(fabView, show, false);
    }
    public static void showFabDelayed(View fabView, final boolean show, long delayMs) {
        int newVisibility = (show ? View.VISIBLE : View.GONE);
        if (fabView == null || fabView.getVisibility() == newVisibility) {
            return;
        }

        // use a weak reference to the view so it won't be retained if the
        // activity/fragment is destroyed before the animation is started
        final WeakReference<View> weakView = new WeakReference<View>(fabView);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                View view = weakView.get();
                if (view != null) {
                    showFab(view, show);
                }
            }
        }, delayMs);
    }
}
