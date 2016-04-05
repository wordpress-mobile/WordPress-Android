package org.wordpress.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
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
    private static final String SELECTOR_HEIGHT_FIELD = "mSelectorElementHeight";
    private static final String INITIAL_OFFSET_FIELD = "mInitialScrollOffset";
    private static final String CURRENT_OFFSET_FIELD = "mCurrentScrollOffset";
    private static final String PAINT_FIELD = "mSelectorWheelPaint";

    private static final int DISPLAY_COUNT = 5;
    private static final int MIDDLE_INDEX = 2;

    private Field mOffsetField;
    private Field mSelectorHeight;
    private Field mSelectorIndices;
    private Field mInitialOffset;
    private Field mCurrentOffset;

    private EditText mInputView;
    private Formatter mFormatter;
    private Paint mPaint;
    private int[] mDisplayValues;

    public WPNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDisplayValues = new int[DISPLAY_COUNT];
        getFieldsViaReflection();
    }

    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof TextView) {
            WPPrefUtils.layoutAsNumberPickerPeek((TextView) child);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateIntitialOffset();
        setVerticalFadingEdgeEnabled(false);
        setHorizontalFadingEdgeEnabled(false);
        WPPrefUtils.layoutAsNumberPickerSelected(mInputView);
        mInputView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setValue(int value) {
        if (value < getMinValue()) value = getMinValue();
        if (value > getMaxValue()) value = getMaxValue();
        super.setValue(value);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int[] selectorIndices = getIndices();
        setIndices(new int[0]);
        setIndices(selectorIndices);

        // Draw the middle number with a different font
        setDisplayValues();
        float elementHeight = getSelectorElementHeight();
        float x = ((getRight() - getLeft()) / 2.0f);
        float y = getScrollOffset();
        Paint paint = mInputView.getPaint();
        paint.setTextAlign(Paint.Align.CENTER);
        //noinspection deprecation
        paint.setColor(getResources().getColor(R.color.blue_medium));
        int alpha = isEnabled() ? 255 : 96;
        paint.setAlpha(alpha);
        mPaint.setAlpha(alpha);

        int offset = getResources().getDimensionPixelSize(R.dimen.margin_medium);
        // Draw the visible values
        for (int i = 0; i < DISPLAY_COUNT; ++i) {
            String scrollSelectorValue;
            if (mFormatter != null) {
                scrollSelectorValue = mFormatter.format(mDisplayValues[i]);
            } else {
                scrollSelectorValue = String.valueOf(mDisplayValues[i]);
            }
            if (i == MIDDLE_INDEX) {
                canvas.drawText(scrollSelectorValue, x, y - ((paint.descent() + paint.ascent()) / 2) - offset, paint);
            } else {
                canvas.drawText(scrollSelectorValue, x, y - ((mPaint.descent() + mPaint.ascent()) / 2) - offset, mPaint);
            }
            y += elementHeight;
        }
    }

    @Override
    public void setFormatter(Formatter formatter) {
        super.setFormatter(formatter);
        mFormatter = formatter;
    }

    private void setDisplayValues() {
        int value = getValue();
        for (int i = 0; i < DISPLAY_COUNT; ++i) {
            mDisplayValues[i] = value - MIDDLE_INDEX + i;
            if (mDisplayValues[i] < getMinValue()) {
                mDisplayValues[i] = getMaxValue() + (mDisplayValues[i] + 1 - getMinValue());
            } else if (mDisplayValues[i] > getMaxValue()) {
                mDisplayValues[i] = getMinValue() + (mDisplayValues[i] - getMaxValue() - 1);
            }
        }
    }

    private void setIndices(int[] indices) {
        if (mSelectorIndices != null) {
            try {
                mSelectorIndices.set(this, indices);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private int[] getIndices() {
        if (mSelectorIndices != null) {
            try {
                return (int[]) mSelectorIndices.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private int getScrollOffset() {
        if (mOffsetField != null) {
            try {
                return (Integer) mOffsetField.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
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

    private void updateIntitialOffset() {
        if (mInitialOffset != null) {
            try {
                int offset = (Integer) mInitialOffset.get(this) - getSelectorElementHeight();
                mInitialOffset.set(this, offset);
                // Only do this once
                mInitialOffset = null;

                if (mCurrentOffset != null) {
                    mCurrentOffset.set(this, offset);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * From https://www.snip2code.com/Snippet/67740/NumberPicker-with-transparent-selection-
     */
    private void removeDividers(Class<?> clazz) {
        Field selectionDivider = getFieldAndSetAccessible(clazz, DIVIDER_FIELD);
        if (selectionDivider != null) {
            try {
                selectionDivider.set(this, null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void getTextPaint(Class<?> clazz) {
        Field paint = getFieldAndSetAccessible(clazz, PAINT_FIELD);
        if (paint != null) {
            try {
                mPaint = (Paint) paint.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void getInputField(Class<?> clazz) {
        Field inputField = getFieldAndSetAccessible(clazz, INPUT_FIELD);
        if (inputField != null) {
            try {
                mInputView = ((EditText) inputField.get(this));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a class field using reflection and makes it accessible.
     */
    private Field getFieldAndSetAccessible(Class<?> clazz, String fieldName) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return field;
    }

    private void getFieldsViaReflection() {
        Class<?> numberPickerClass = null;
        try {
            numberPickerClass = Class.forName(NumberPicker.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (numberPickerClass == null) return;

        mSelectorHeight = getFieldAndSetAccessible(numberPickerClass, SELECTOR_HEIGHT_FIELD);
        mOffsetField = getFieldAndSetAccessible(numberPickerClass, CUR_OFFSET_FIELD);
        mSelectorIndices = getFieldAndSetAccessible(numberPickerClass, INDICES_FIELD);
        mInitialOffset = getFieldAndSetAccessible(numberPickerClass, INITIAL_OFFSET_FIELD);
        mCurrentOffset = getFieldAndSetAccessible(numberPickerClass, CURRENT_OFFSET_FIELD);

        getTextPaint(numberPickerClass);
        getInputField(numberPickerClass);
        removeDividers(numberPickerClass);
        setIndices(new int[DISPLAY_COUNT]);
    }
}
