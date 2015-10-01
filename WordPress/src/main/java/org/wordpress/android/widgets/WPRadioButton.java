package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

/**
 * A RadioButton that uses the default font from TypefaceCache
 */
public class WPRadioButton extends RadioButton {
    public WPRadioButton(Context context) {
        super(context);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public WPRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public WPRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
