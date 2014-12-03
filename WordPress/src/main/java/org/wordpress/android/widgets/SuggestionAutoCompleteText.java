package org.wordpress.android.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.MultiAutoCompleteTextView;

import org.wordpress.android.ui.suggestion.util.SuggestionTokenizer;

public class SuggestionAutoCompleteText extends MultiAutoCompleteTextView {
    String mUniqueId;

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

    public void setUniqueId(String uniqueId) {
        mUniqueId = uniqueId;
    }

    private String getViewPathId(View view) {
        StringBuilder sb = new StringBuilder();
        for (View currentView = view; currentView != null && currentView.getParent() != null
                && currentView.getParent() instanceof View; currentView = (View) currentView.getParent()) {
            sb.append(currentView.getId());
        }
        return sb.toString();
    }

    public void clearSavedText() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("TODO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(getViewPathId(this) + mUniqueId);
        editor.apply();
    }

    private void loadText() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("TODO", Context.MODE_PRIVATE);
        String text = sharedPreferences.getString(getViewPathId(this) + mUniqueId, "");
        setText(text);
        setSelection(text.length());
    }

    private void saveText() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("TODO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getViewPathId(this) + mUniqueId, getText().toString());
        editor.apply();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mUniqueId == null) {
            return;
        }
        loadText();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mUniqueId == null) {
            return;
        }
        saveText();
    }
}
