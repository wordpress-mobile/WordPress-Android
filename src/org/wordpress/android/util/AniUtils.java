package org.wordpress.android.util;

/*
 *  Reader animation utilities - these are backwards-compatible to Android 2.2
 */

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ListView;

import org.wordpress.android.R;


public class AniUtils {
    public static enum Duration { DEFAULT, SHORT, MEDIUM, LONG }

    private AniUtils() {
        throw new AssertionError();
    }

    public static void fadeIn(View target) {
        fadeIn(target, null);
    }
    public static void fadeIn(View target, AnimationListener listener) {
        startAnimation(target, android.R.anim.fade_in, listener, Duration.DEFAULT);
        if (target.getVisibility() != View.VISIBLE)
            target.setVisibility(View.VISIBLE);
    }

    public static void fadeOut(View target) {
        fadeOut(target, null);
    }
    public static void fadeOut(View target, AnimationListener listener) {
        startAnimation(target, android.R.anim.fade_out, listener, Duration.DEFAULT);
        if (target.getVisibility() != View.GONE)
            target.setVisibility(View.GONE);
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
        startAnimation(target, R.anim.reader_flyout, listener, Duration.DEFAULT);
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
        startAnimation(target, aniResId, null, Duration.DEFAULT);
    }
    public static void startAnimation(View target, int aniResId, Duration duration) {
        startAnimation(target, aniResId, null, Duration.DEFAULT);
    }
    public static void startAnimation(View target, int aniResId, AnimationListener listener) {
        startAnimation(target, aniResId, listener, Duration.DEFAULT);
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

        // set duration if we're not using the default (default = duration defined in animation resource)
        if (duration != Duration.DEFAULT) {
            Context context = target.getContext();
            final long durationMillis;
            switch (duration) {
                case LONG:
                    durationMillis = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
                    break;
                case MEDIUM:
                    durationMillis = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
                    break;
                default :
                    durationMillis = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
                    break;
            }
            animation.setDuration(durationMillis);
        }

        target.startAnimation(animation);
    }
}