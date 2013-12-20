package org.wordpress.android.util;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * Created by nbradbury on 11/12/13.
 */

/*
 * used by activities such as the reader tag editor and the comment list to animate a message in
 * from the bottom, then animate it back out after a brief delay - optional runnable will be
 * executed by hideMessageBar() once the bar disappears
 * note that the activity's layout must contain message_bar_include.xml for this to work
 */

public class MessageBarUtils {
    private static final long DELAY_MILLIS = 2000;
    public static enum MessageBarType { INFO, ALERT };

    public static void showMessageBar(final Activity activity, final String message) {
        showMessageBar(activity, message, MessageBarType.INFO, null);
    }
    public static void showMessageBar(final Activity activity,
                                      final String message,
                                      final MessageBarType messageBarType,
                                      final Runnable runnable) {
        if (activity==null)
            return;

        final TextView txtMessageBar = (TextView) activity.findViewById(R.id.text_message_bar);
        if (txtMessageBar==null)
            return;
        if (txtMessageBar.getVisibility()==View.VISIBLE)
            return;

        switch (messageBarType) {
            case INFO:
                txtMessageBar.setBackgroundResource(R.color.reader_message_bar_blue);
                break;
            case ALERT:
                txtMessageBar.setBackgroundResource(R.color.reader_message_bar_orange);
                break;
            default :
                return;
        }

        txtMessageBar.clearAnimation();
        txtMessageBar.setText(message);

        ReaderAniUtils.startAnimation(txtMessageBar, R.anim.reader_message_bar_in);
        txtMessageBar.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideMessageBar(activity, runnable, false);
            }
        }, DELAY_MILLIS);
    }

    public static void hideMessageBar(final Activity activity, final Runnable runnable, boolean hideImmediate) {
        if (activity==null)
            return;

        final TextView txtMessageBar = (TextView) activity.findViewById(R.id.text_message_bar);
        if (txtMessageBar==null)
            return;
        if (txtMessageBar.getVisibility()!=View.VISIBLE)
            return;

        txtMessageBar.clearAnimation();

        // hide w/o animation if caller requested it
        if (hideImmediate) {
            txtMessageBar.setVisibility(View.GONE);
            if (runnable != null)
                runnable.run();
            return;
        }

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                txtMessageBar.setVisibility(View.GONE);
                if (runnable != null)
                    runnable.run();
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        ReaderAniUtils.startAnimation(txtMessageBar, R.anim.reader_message_bar_out, listener);
    }
}
