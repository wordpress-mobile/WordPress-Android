package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ListView;

import org.wordpress.android.R;

public class ReaderAnim {

    public static interface AnimationEndListener {
        public void onAnimationEnd();
    }

    public static enum Duration {
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
                target.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                target.setVisibility(View.GONE);
            }
        });
        set.start();
    }

    /*
     * animation when user taps a like/follow/reblog button
     */
    private static enum ReaderButton { LIKE_ON, LIKE_OFF, REBLOG, FOLLOW }
    public static void animateLikeButton(final View target, boolean isAskingToLike) {
        animateButton(target, isAskingToLike ? ReaderButton.LIKE_ON : ReaderButton.LIKE_OFF);
    }
    public static void animateReblogButton(final View target) {
        animateButton(target, ReaderButton.REBLOG);
    }
    public static void animateFollowButton(final View target) {
        animateButton(target, ReaderButton.FOLLOW);
    }
    private static void animateButton(final View target, ReaderButton button) {
        if (target == null || button == null) {
            return;
        }

        // follow button uses reverse scaling
        float endScale = (button == ReaderButton.FOLLOW ? 0.75f : 1.75f);

        ObjectAnimator animX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, endScale);
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(1);

        ObjectAnimator animY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, endScale);
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
     * called when adding or removing an item from a listView
     */
    public static enum AnimateListItemStyle {
        ADD,
        REMOVE,
        SHRINK }
    public static void animateListItem(ListView listView,
                                       int positionAbsolute,
                                       AnimateListItemStyle style,
                                       Animation.AnimationListener listener) {
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

        final int animResId;
        switch (style) {
            case ADD:
                animResId = R.anim.reader_listitem_add;
                break;
            case REMOVE:
                animResId = R.anim.reader_listitem_remove;
                break;
            case SHRINK:
                animResId = R.anim.reader_listitem_shrink;
                break;
            default:
                return;
        }

        Animation animation = AnimationUtils.loadAnimation(listView.getContext(), animResId);

        if (listener != null) {
            animation.setAnimationListener(listener);
        }

        listItem.startAnimation(animation);
    }

}
