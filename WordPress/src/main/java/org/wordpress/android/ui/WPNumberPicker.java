package org.wordpress.android.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.TypefaceCache;

import java.lang.reflect.Field;

public class WPNumberPicker extends NumberPicker {
    private static final String DIVIDER_FIELD = "mSelectionDivider";

    public WPNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWrapSelectorWheel(false);

        Class<?> numberPickerClass = null;
        try {
            numberPickerClass = Class.forName("android.widget.NumberPicker");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Field selectionDivider = null;
        try {
            selectionDivider = numberPickerClass.getDeclaredField(DIVIDER_FIELD);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        try {
            selectionDivider.setAccessible(true);
            selectionDivider.set(this, null);
        } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
            e.printStackTrace();
        }
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
