package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class OpenSansButton extends Button {
    public OpenSansButton(Context context) {
        super(context, null);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public OpenSansButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public OpenSansButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
