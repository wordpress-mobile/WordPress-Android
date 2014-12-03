package org.wordpress.android.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.MultiAutoCompleteTextView;

import org.wordpress.android.ui.suggestion.util.SuggestionTokenizer;

public class SuggestionAutoCompleteText extends MultiAutoCompleteTextView {
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

    private String getViewPathId(View view) {
        StringBuilder sb = new StringBuilder();
        for (View currentView = view; currentView != null && currentView.getParent() != null
                && currentView.getParent() instanceof View; currentView = (View) currentView.getParent()) {
            sb.append(currentView.getId());
        }
        return sb.toString();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("TODO", Context.MODE_PRIVATE);
        String text = sharedPreferences.getString(getViewPathId(this), "");
        setText(text);
        setSelection(text.length());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("TODO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getViewPathId(this), getText().toString());
        editor.apply();
    }
}
