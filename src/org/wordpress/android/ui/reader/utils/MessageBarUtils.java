package org.wordpress.android.ui.reader.utils;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AniUtils;

/*
 * used by activities to animate a message in from the bottom then animate it back out after
 * a brief delay - note that the activity's layout must contain message_bar_include.xml for
 * this to work
 */
public class MessageBarUtils {
    private static final long DELAY_MILLIS = 1500;
    public static enum MessageBarType { INFO, ALERT }

    public static void showMessageBar(final Activity activity,
                                      final String message,
                                      final MessageBarType messageBarType) {
        if (activity == null) {
            return;
        }

        final TextView txtMessageBar = (TextView) activity.findViewById(R.id.text_message_bar);
        if (txtMessageBar == null || txtMessageBar.getVisibility() == View.VISIBLE) {
            return;
        }

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

        AniUtils.startAnimation(txtMessageBar, R.anim.reader_message_bar_in);
        txtMessageBar.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideMessageBar(activity);
            }
        }, DELAY_MILLIS);
    }

    private static void hideMessageBar(final Activity activity) {
        if (activity == null) {
            return;
        }

        final TextView txtMessageBar = (TextView) activity.findViewById(R.id.text_message_bar);
        if (txtMessageBar == null || txtMessageBar.getVisibility() != View.VISIBLE) {
            return;
        }

        txtMessageBar.clearAnimation();

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                txtMessageBar.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        AniUtils.startAnimation(txtMessageBar, R.anim.reader_message_bar_out, listener);
    }
}
