package org.wordpress.android.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.WPPrefUtils;

import java.lang.reflect.Field;

public class WPNumberPicker extends NumberPicker {
    private static final String DIVIDER_FIELD = "mSelectionDivider";
    private static final String INPUT_FIELD = "mInputText";
    private static final String INDICES_FIELD = "mSelectorIndices";
    private static final String CUR_OFFSET_FIELD = "mCurrentScrollOffset";
    private static final String MIDDLE_INDEX_FIELD = "SELECTOR_MIDDLE_ITEM_INDEX";
    private static final String STRING_CACHE_FIELD = "mSelectorIndexToStringCache";
    private static final String SELECTOR_HEIGHT_FIELD = "mSelectorElementHeight";

    private Field mOffsetField;
    private Field mSelectorHeight;
    private Field mSelectorIndices;
    private Field mStringCache;
    private int mMiddleIndex;
    private EditText mInputView;

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

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw normally, skipping the middle number
        if (mInputView != null) mInputView.setVisibility(View.VISIBLE);
        super.onDraw(canvas);

        // Draw the middle number with a different font
        String scrollSelectorValue = getDisplayString();
        float x = ((getRight() - getLeft()) / 2.0f) - (6.0f * getResources().getDisplayMetrics().scaledDensity * scrollSelectorValue.length());
        float y = getScrollOffset() + getSelectorElementHeight();
        Paint paint = mInputView.getPaint();
        paint.setColor(getResources().getColor(R.color.blue_medium));
        canvas.drawText(scrollSelectorValue, x, y, paint);
        if (mInputView != null) mInputView.setVisibility(View.INVISIBLE);
    }

    private void updateView(final View view) {
        if (view instanceof TextView) {
            WPPrefUtils.layoutAsNumberPickerPeek((TextView) view);
        }
    }

    private String getDisplayString() {
        if (mSelectorIndices == null || mStringCache == null) return "";

        try {
            int index = ((int []) mSelectorIndices.get(this))[mMiddleIndex];
            return ((SparseArray<String>) mStringCache.get(this)).get(index);
        } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    private int getScrollOffset() {
        if (mOffsetField != null) {
            try {
                return (Integer) mOffsetField.get(this);
            } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private int getSelectorElementHeight() {
        if (mSelectorHeight != null) {
            try {
                return (Integer) mSelectorHeight.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0;
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
        Field inputField = null;
        Field middleIndex = null;
        try {
            selectionDivider = numberPickerClass.getDeclaredField(DIVIDER_FIELD);
            inputField = numberPickerClass.getDeclaredField(INPUT_FIELD);
            middleIndex = numberPickerClass.getDeclaredField(MIDDLE_INDEX_FIELD);
            mSelectorIndices = numberPickerClass.getDeclaredField(INDICES_FIELD);
            mSelectorHeight = numberPickerClass.getDeclaredField(SELECTOR_HEIGHT_FIELD);
            mOffsetField = numberPickerClass.getDeclaredField(CUR_OFFSET_FIELD);
            mStringCache = numberPickerClass.getDeclaredField(STRING_CACHE_FIELD);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (middleIndex != null) {
            try {
                middleIndex.setAccessible(true);
                mMiddleIndex = (Integer) middleIndex.get(this);
            } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (selectionDivider != null) {
            try {
                selectionDivider.setAccessible(true);
                selectionDivider.set(this, null);
            } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (inputField != null) {
            try {
                inputField.setAccessible(true);
                mInputView = ((EditText) inputField.get(this));
            } catch (IllegalArgumentException | IllegalAccessException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (mStringCache != null) {
            try {
                mStringCache.setAccessible(true);
            } catch (IllegalArgumentException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (mSelectorIndices != null) {
            try {
                mSelectorIndices.setAccessible(true);
            } catch (IllegalArgumentException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (mOffsetField != null) {
            try {
                mOffsetField.setAccessible(true);
            } catch (IllegalArgumentException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
        if (mSelectorHeight != null) {
            try {
                mSelectorHeight.setAccessible(true);
            } catch (IllegalArgumentException | Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
