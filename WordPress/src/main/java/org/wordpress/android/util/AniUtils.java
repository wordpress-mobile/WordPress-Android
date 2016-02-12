package org.wordpress.android.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

import org.wordpress.android.R;

public class AniUtils {

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

    public interface AnimationEndListener {
        void onAnimationEnd();
    }

    private AniUtils() {
        throw new AssertionError();
    }

    public static void startAnimation(View target, int aniResId) {
        startAnimation(target, aniResId, null);
    }

    public static void startAnimation(View target, int aniResId, int duration) {
        if (target == null) return;

        Animation animation = AnimationUtils.loadAnimation(target.getContext(), aniResId);
        if (animation != null) {
            animation.setDuration(duration);
            target.startAnimation(animation);
        }
    }

    public static void startAnimation(View target, int aniResId, AnimationListener listener) {
        if (target == null) return;

        Animation animation = AnimationUtils.loadAnimation(target.getContext(), aniResId);
        if (animation != null) {
            if (listener != null) {
                animation.setAnimationListener(listener);
            }
            target.startAnimation(animation);
        }
    }

    /*
     * in/out animation for floating action button
     */
    public static void showFab(final View view, final boolean show) {
        if (view == null) return;

        Context context = view.getContext();
        int fabHeight = context.getResources().getDimensionPixelSize(android.support.design.R.dimen.design_fab_size_normal);
        int fabMargin = context.getResources().getDimensionPixelSize(R.dimen.fab_margin);
        int max = (fabHeight + fabMargin) * 2;
        float fromY = (show ? max : 0f);
        float toY   = (show ? 0f : max);

        ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, fromY, toY);
        if (show) {
            anim.setInterpolator(new DecelerateInterpolator());
        } else {
            anim.setInterpolator(new AccelerateInterpolator());
        }
        anim.setDuration(show ? Duration.LONG.toMillis(context) : Duration.SHORT.toMillis(context));

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (view.getVisibility() != View.VISIBLE) {
                    view.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!show) {
                    view.setVisibility(View.GONE);
                }
            }
        });

        anim.start();
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

        view.clearAnimation();
        view.startAnimation(animation);
        view.setVisibility(newVisibility);
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
        if (target != null && duration != null) {
            getFadeInAnim(target, duration).start();
        }
    }

    public static void fadeOut(final View target, Duration duration) {
        if (target != null && duration != null) {
            getFadeOutAnim(target, duration).start();
        }
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

    public static void scaleOut(final View target, Duration duration) {
        scaleOut(target, View.GONE, duration, null);
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
}
