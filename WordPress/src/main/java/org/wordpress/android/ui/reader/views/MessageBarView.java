package org.wordpress.android.ui.reader.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;

public class MessageBarView extends FrameLayout {

    private static final long DELAY_MILLIS = 1500;
    public static enum MessageBarType { INFO, ALERT }

    public MessageBarView(Context context) {
        super(context);
        init(context);
    }

    public MessageBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MessageBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.message_bar_view, this, true);
    }

    public void show(String message, MessageBarType messageType) {
        TextView textView = (TextView) findViewById(R.id.text_message_bar);
        textView.setText(message);

        int colorResId =
                (messageType == MessageBarType.ALERT ?
                        R.color.color_accent : R.color.color_primary_dark);
        setBackgroundColor(getContext().getResources().getColor(colorResId));

        if (getVisibility() != View.VISIBLE) {
            animate(true);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            }, DELAY_MILLIS);
        }
    }

    private void hide() {
        if (getVisibility() == View.VISIBLE) {
            animate(false);
        }
    }

    private void animate(boolean animateIn) {
        clearAnimation();

        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(getContext());

        ObjectAnimator anim;
        if (animateIn) {
            anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, displayHeight, 0f);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(View.VISIBLE);
                }
            });
            anim.setInterpolator(new DecelerateInterpolator());
        } else {
            anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, displayHeight);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                }
            });
            anim.setInterpolator(new AccelerateInterpolator());
        }
        anim.setDuration(duration);
        anim.start();
    }
}
