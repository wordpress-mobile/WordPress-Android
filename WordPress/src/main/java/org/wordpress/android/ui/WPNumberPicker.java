package org.wordpress.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.TypefaceCache;

public class WPNumberPicker extends NumberPicker {
    public WPNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWrapSelectorWheel(false);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        updateView(child);
    }

    @Override
    public void addView(View child, int index,
                        android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        updateView(child);
    }

    @Override
    public void addView(View child, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, params);
        updateView(child);
    }

    private void updateView(View view) {
        if (view instanceof TextView) {
            Typeface type = TypefaceCache.getTypeface(getContext(),
                    TypefaceCache.FAMILY_OPEN_SANS,
                    Typeface.NORMAL,
                    TypefaceCache.VARIATION_NORMAL);
            ((TextView) view).setTypeface(type);
            ((TextView) view).setTextSize(24);
            ((TextView) view).setTextColor(getResources().getColor(R.color.wp_blue));
        }
    }
}
