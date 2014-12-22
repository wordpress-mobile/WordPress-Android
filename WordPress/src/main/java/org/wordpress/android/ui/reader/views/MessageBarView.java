package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AniUtils;

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
                        R.color.reader_message_bar_alert : R.color.reader_message_bar_info);
        setBackgroundColor(getContext().getResources().getColor(colorResId));

        if (getVisibility() != View.VISIBLE) {
            clearAnimation();
            AniUtils.startAnimation(this, R.anim.reader_message_bar_in);
            setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            }, DELAY_MILLIS);
        }
    }

    private void hide() {
        if (getVisibility() != View.VISIBLE) {
            return;
        }
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        clearAnimation();
        AniUtils.startAnimation(this, R.anim.reader_message_bar_out, listener);
    }
}
