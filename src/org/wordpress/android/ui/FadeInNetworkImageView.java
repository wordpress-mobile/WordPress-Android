package org.wordpress.android.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.util.SysUtils;

/**
 * A custom NetworkImageView that does a fade in animation when the bitmap is set 
 * from: https://gist.github.com/benvd/5683818
 * nbradbury 10-Mar-2015 - replaced previous TransitionDrawable with faster alpha animation
 */

public class FadeInNetworkImageView extends NetworkImageView {
    
    private static final int FADE_IN_TIME_MS = 250;
 
    public FadeInNetworkImageView(Context context) {
        super(context);
    }
 
    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
 
    public FadeInNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @SuppressLint("NewApi")
    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        // use faster property animation if device supports it
        if (SysUtils.isGteAndroid4()) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.25f, 1f);
            alpha.setDuration(FADE_IN_TIME_MS);
            alpha.start();
        } else {
            AlphaAnimation animation = new AlphaAnimation(0.25f, 1f);
            animation.setDuration(FADE_IN_TIME_MS);
            this.startAnimation(animation);
        }
    }
}