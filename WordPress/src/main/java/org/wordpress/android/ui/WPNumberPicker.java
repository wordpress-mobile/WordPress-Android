package org.wordpress.android.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.wordpress.android.util.WPPrefUtils;

import java.lang.reflect.Field;

public class WPNumberPicker extends NumberPicker {
    private static final String DIVIDER_FIELD = "mSelectionDivider";

    public WPNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        removeDividers();
    }

    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        updateView(child);
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        EditText view = (EditText) getChildAt(0);
        WPPrefUtils.layoutAsNumberPickerSelected(view);
    }

    private void updateView(final View view) {
        if (view instanceof TextView) {
            WPPrefUtils.layoutAsNumberPickerPeek((TextView) view);
        }
    }

    /**
     * From https://www.snip2code.com/Snippet/67740/NumberPicker-with-transparent-selection-
     */
    private void removeDividers() {
        Class<?> numberPickerClass = null;
        try {
            numberPickerClass = Class.forName(NumberPicker.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (numberPickerClass == null) return;

        Field selectionDivider = null;
        try {
            selectionDivider = numberPickerClass.getDeclaredField(DIVIDER_FIELD);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (selectionDivider == null) return;

        try {
            selectionDivider.setAccessible(true);
            selectionDivider.set(this, null);
        } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }
}
