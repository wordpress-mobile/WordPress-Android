package org.wordpress.android.util;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import org.wordpress.android.R;

public class AniUtils {
    private AniUtils() {
        throw new AssertionError();
    }

    public static void fadeIn(View target) {
        startAnimation(target, android.R.anim.fade_in, null);
        if (target.getVisibility() != View.VISIBLE)
            target.setVisibility(View.VISIBLE);
    }

    public static void flyIn(View target) {
        Context context = target.getContext();
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.reader_flyin);
        if (animation==null)
            return;

        // add small overshoot for bounce effect
        animation.setInterpolator(new OvershootInterpolator(0.9f));
        long duration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        animation.setDuration((long)(duration * 1.5f));

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
        startAnimation(target, R.anim.reader_flyout, listener);
    }

    public static void startAnimation(View target, int aniResId) {
        startAnimation(target, aniResId, null);
    }
    public static void startAnimation(View target, int aniResId, AnimationListener listener) {
        if (target==null)
            return;
        Animation animation = AnimationUtils.loadAnimation(target.getContext(), aniResId);
        if (animation==null)
            return;
        if (listener!=null)
            animation.setAnimationListener(listener);

        target.startAnimation(animation);
    }
}