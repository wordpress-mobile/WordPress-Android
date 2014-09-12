package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class OpenSansEditText extends EditText {
    public OpenSansEditText(Context context) {
        super(context, null);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public OpenSansEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public OpenSansEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
