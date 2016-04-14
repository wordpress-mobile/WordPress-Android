package org.wordpress.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class SlidingRelativeLayout extends RelativeLayout {

    public SlidingRelativeLayout(Context context) {
        super(context);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public float getXFraction() {
        int width = getWidth();
        return (width == 0) ? 0 : getX() / (float) width;
    }

    public void setXFraction(float xFraction) {
        int width = getWidth();
        setX((width > 0) ? (xFraction * width) : 0);
    }
}
