package org.wordpress.android.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import com.android.volley.toolbox.NetworkImageView;

/**
 * A custom NetworkImageView that does a fade in animation when the bitmap is set
 * from: https://gist.github.com/benvd/5683818
 * nbradbury 10-Mar-2015 - replaced previous TransitionDrawable with faster alpha animation
 */

public class FadeInNetworkImageView extends NetworkImageView {
    public FadeInNetworkImageView(Context context) {
        super(context);
    }

    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FadeInNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        if (getContext() == null)
            return;
        int duration = getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);

        // use faster property animation if device supports it
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.25f, 1f);
        alpha.setDuration(duration);
        alpha.start();
    }
}
