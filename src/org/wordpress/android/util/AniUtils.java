package org.wordpress.android.util;

/*
 *  Reader animation utilities - these are backwards-compatible to Android 2.2
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ListView;

import org.wordpress.android.R;


public class AniUtils {
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

    private AniUtils() {
        throw new AssertionError();
    }

    public static void fadeIn(View target) {
        fadeIn(target, null);
    }
    public static void fadeIn(View target, AnimationListener listener) {
        startAnimation(target, android.R.anim.fade_in, listener, null);
        if (target.getVisibility() != View.VISIBLE)
            target.setVisibility(View.VISIBLE);
    }

    public static void fadeOut(View target) {
        fadeOut(target, null);
    }
    public static void fadeOut(View target, AnimationListener listener) {
        startAnimation(target, android.R.anim.fade_out, listener, null);
        if (target.getVisibility() != View.GONE)
            target.setVisibility(View.GONE);
    }

    /*
     * fades in the passed view then fades it out
     */
    public static void fadeInFadeOut(final View target, Duration duration) {
        if (target == null || duration == null) {
            return;
        }

        long durationMillis = duration.toMillis(target.getContext());

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(target, View.ALPHA, 0.0f, 1.0f);
        fadeIn.setDuration(durationMillis);
        fadeIn.setInterpolator(new LinearInterpolator());

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(target, View.ALPHA, 1.0f, 0.0f);
        fadeOut.setDuration(durationMillis);
        fadeOut.setInterpolator(new LinearInterpolator());
        fadeOut.setStartDelay(durationMillis / 2);

        AnimatorSet set = new AnimatorSet();
        set.play(fadeOut).after(fadeIn);
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
     * called when user clicks an action view (like button, follow button, etc.)
     */
    public static void zoomAction(final View target) {
        startAnimation(target, R.anim.reader_zoom_action);
    }

    public static void flyIn(View target) {
        flyIn(target, null);
    }
    public static void flyIn(View target, AnimationListener listener) {
        Context context = target.getContext();
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.reader_flyin);
        if (animation==null)
            return;

        // add small overshoot for bounce effect
        animation.setInterpolator(new OvershootInterpolator(0.9f));
        long duration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        animation.setDuration((long)(duration * 1.5f));

        if (listener!=null)
            animation.setAnimationListener(listener);

        target.startAnimation(animation);
        target.setVisibility(View.VISIBLE);
    }

    public static void flyOut(final View target) {
        AnimationListener listener = new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                target.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        startAnimation(target, R.anim.reader_flyout, listener, null);
    }

    /*
     * animate the removal of a listview item
     */
    public static void removeListItem(ListView listView, int positionAbsolute, AnimationListener listener, int animResId) {
        if (listView==null)
            return;

        // passed value is the absolute position of this item, convert to relative or else we'll remove the wrong item if list is scrolled
        int firstVisible = listView.getFirstVisiblePosition();
        int positionRelative = positionAbsolute - firstVisible;

        View listItem = listView.getChildAt(positionRelative);
        if (listItem==null)
            return;

        Animation animation = AnimationUtils.loadAnimation(listView.getContext(), animResId);
        if (listener!=null)
            animation.setAnimationListener(listener);

        listItem.startAnimation(animation);
    }

    public static void startAnimation(View target, int aniResId) {
        startAnimation(target, aniResId, null, null);
    }
    public static void startAnimation(View target, int aniResId, Duration duration) {
        startAnimation(target, aniResId, null, null);
    }
    public static void startAnimation(View target, int aniResId, AnimationListener listener) {
        startAnimation(target, aniResId, listener, null);
    }
    public static void startAnimation(View target,
                                      int aniResId,
                                      AnimationListener listener,
                                      Duration duration) {
        if (target==null)
            return;
        Animation animation = AnimationUtils.loadAnimation(target.getContext(), aniResId);
        if (animation==null)
            return;
        if (listener!=null)
            animation.setAnimationListener(listener);

        // if duration is null we'll use the duration defined in animation resource
        if (duration != null) {
            animation.setDuration(duration.toMillis(target.getContext()));
        }

        target.startAnimation(animation);
    }
}