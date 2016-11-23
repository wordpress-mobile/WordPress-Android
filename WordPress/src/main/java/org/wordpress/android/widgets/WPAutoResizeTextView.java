package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;

import org.wordpress.android.util.widgets.AutoResizeTextView;

/**
 * custom AutoTextView used in layouts - enables keeping custom typeface handling in one place (so we
 * avoid having to set the typeface for every single AutoResizeTextView in every single activity)
 */
public class WPAutoResizeTextView extends AutoResizeTextView {
    public WPAutoResizeTextView(Context context) {
        super(context);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public WPAutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public WPAutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
