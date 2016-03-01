package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

/**
 * A CheckBox that uses the default font from TypefaceCache
 */
public class WPCheckBox extends CheckBox {
    public WPCheckBox(Context context) {
        super(context);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public WPCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public WPCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
