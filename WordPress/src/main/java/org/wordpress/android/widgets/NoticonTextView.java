package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * TextView that uses noticon icon font
 */
public class NoticonTextView extends TextView {

    private String FONT_NAME = "Noticons-Regular.otf";

    public NoticonTextView(Context context) {
        super(context, null);
        this.setTypeface(TypefaceCache.getTypefaceForTypefaceName(context, FONT_NAME));
    }

    public NoticonTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(TypefaceCache.getTypefaceForTypefaceName(context, FONT_NAME));
    }

    public NoticonTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setTypeface(TypefaceCache.getTypefaceForTypefaceName(context, FONT_NAME));
    }
}