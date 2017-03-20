package org.wordpress.android.widgets;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * TextView that uses noticon icon font
 */
public class NoticonTextView extends AppCompatTextView {
    private static final String NOTICON_FONT_NAME = "Noticons.ttf";

    public NoticonTextView(Context context) {
        super(context, null);
        this.setTypeface(TypefaceCache.getTypefaceForTypefaceName(context, NOTICON_FONT_NAME));
    }

    public NoticonTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(TypefaceCache.getTypefaceForTypefaceName(context, NOTICON_FONT_NAME));
    }

    public NoticonTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setTypeface(TypefaceCache.getTypefaceForTypefaceName(context, NOTICON_FONT_NAME));
    }
}
