package org.wordpress.android.widgets;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

/**
 * A CheckBox that uses the default font from TypefaceCache
 */
public class WPCheckBox extends AppCompatCheckBox {
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
