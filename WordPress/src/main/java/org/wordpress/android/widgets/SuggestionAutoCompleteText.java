package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

import org.wordpress.android.ui.suggestion.util.SuggestionTokenizer;
import org.wordpress.persistentedittext.AutoSaveTextHelper;

public class SuggestionAutoCompleteText extends MultiAutoCompleteTextView {
    AutoSaveTextHelper mAutoSaveTextHelper;

    public SuggestionAutoCompleteText(Context context) {
        super(context, null);
        init(context, null);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypefaceCache.setCustomTypeface(context, this, attrs);
        setTokenizer(new SuggestionTokenizer());
        setThreshold(1);
        mAutoSaveTextHelper = new AutoSaveTextHelper(context);
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
        getAutoSaveTextHelper().loadString(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        getAutoSaveTextHelper().saveString(this);
    }
}
