package org.wordpress.android.widgets;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;

public class WPSwitch extends SwitchCompat {
    public WPSwitch(Context context) {
        super(context);
    }

    public WPSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public WPSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
