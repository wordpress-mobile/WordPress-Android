package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

import org.wordpress.android.ui.suggestion.util.SuggestionTokenizer;

public class SuggestionAutoCompleteText extends MultiAutoCompleteTextView {
    AutoSaveTextHelper mAutoSaveTextHelper = new AutoSaveTextHelper();

    public SuggestionAutoCompleteText(Context context) {
        super(context, null);
        TypefaceCache.setCustomTypeface(context, this, null);
        this.setTokenizer(new SuggestionTokenizer());
        this.setThreshold(1);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypefaceCache.setCustomTypeface(context, this, attrs);
        this.setTokenizer(new SuggestionTokenizer());
        this.setThreshold(1);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypefaceCache.setCustomTypeface(context, this, attrs);
        this.setTokenizer(new SuggestionTokenizer());
        this.setThreshold(1);
    }

    public AutoSaveTextHelper getAutoSaveTextHelper() {
        return mAutoSaveTextHelper;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        String text = getAutoSaveTextHelper().loadString(this);
        setText(text);
        setSelection(text.length());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        getAutoSaveTextHelper().saveString(this, getText().toString());
    }
}
