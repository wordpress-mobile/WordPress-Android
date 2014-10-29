package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

public class SuggestionAutoCompleteText extends MultiAutoCompleteTextView {
    public SuggestionAutoCompleteText(Context context) {
        super(context, null);
        TypefaceCache.setCustomTypeface(context, this, null);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
    }
}
