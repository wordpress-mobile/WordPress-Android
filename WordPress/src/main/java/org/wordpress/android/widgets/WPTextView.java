package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * custom TextView used in layouts - enables keeping custom typeface handling in one place (so we
 * avoid having to set the typeface for every single TextView in every single activity)
 */
public class WPTextView extends TextView {
    public WPTextView(Context context) {
        super(context, null);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public WPTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public WPTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
