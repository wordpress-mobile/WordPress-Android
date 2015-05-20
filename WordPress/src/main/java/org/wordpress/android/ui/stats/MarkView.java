package org.wordpress.android.ui.stats;

/**
 * This Widget is highly based on the MarkView project available here:
 * https://github.com/xiprox/MarkView
 *
 */


/*
 * Copyright (C) 2015 Ihsan Isik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.TypefaceCache;

/**
 * @author Ihsan Isik
 *         <p/>
 *         An android custom view that displays a circle with a colored arc given a mark.
 * @see #setMark(int)
 * <p/>
 */
public class MarkView extends View {
    private static final String STATE_STATE = "saved_instance";
    private static final String STATE_TEXT_COLOR = "text_color";
    private static final String STATE_TEXT_SIZE = "text_size";
    private static final String STATE_STROKE_COLORS = "stroke_colors";
    private static final String STATE_STROKE_WIDTH = "stroke_width";
    private static final String STATE_MARK = "mark";
    private static final String STATE_MAX = "max";

    private final int DEFAULT_STROKE_COLOR = Color.parseColor("#999999");
    private final float DEFAULT_STROKE_WIDTH;
    private final float DEFAULT_BG_RING_WIDTH;
    private final int DEFAULT_BG_RING_COLOR = Color.parseColor("#647d7d7d");
    private final int DEFAULT_TEXT_COLOR = Color.parseColor("#656565");
    private final int DEFAULT_MAX = 5;
    private final float DEFAULT_TEXT_SIZE;
    private final int MIN_SIZE;

    private Paint mPaint;
    private Paint mBgRingPaint;
    private Paint mInnerCirclePaint;
    private Paint mTextPaint;
    private Paint mSecondLineTextPaint;

    private float mTextSize;
    private int mTextColor;
    private float mSecondLineTextSize;
    private int mSecondLineTextColor;
    private int[] mStrokeColors;
    private float mStrokeWidth;
    private int mBgRingColor;
    private float mBgRingWidth;
    private int mMark = 0;
    private int mMax = DEFAULT_MAX;

    private RectF mOuterRect = new RectF();

    public MarkView(Context context) {
        this(context, null);
    }

    public MarkView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        DEFAULT_TEXT_SIZE = DisplayUtils.spToPx(context, 16);
        MIN_SIZE = (int) DisplayUtils.dpToPx(context, 48);
        DEFAULT_STROKE_WIDTH = DisplayUtils.dpToPx(context, 2);
        DEFAULT_BG_RING_WIDTH = DisplayUtils.dpToPx(context, 2);

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MarkView, defStyleAttr, 0);
        initByAttributes(attributes);
        attributes.recycle();

        initPainters();
    }

    private void initByAttributes(TypedArray attributes) {
        mStrokeWidth = attributes.getDimension(R.styleable.MarkView_mv_strokeWidth, DEFAULT_STROKE_WIDTH);
        if (!isInEditMode()) {
            int id = attributes.getResourceId(R.styleable.MarkView_mv_strokeColors, R.array.stats_mark_colors);
            String[] colors = getResources().getStringArray(id);
            mStrokeColors = new int[colors.length];
            for (int i = 0; i < colors.length; i++) {
                mStrokeColors[i] = Color.parseColor(colors[i]);
            }
        }

        mBgRingWidth = attributes.getDimension(R.styleable.MarkView_mv_ringWidth,
                DEFAULT_BG_RING_WIDTH);
        mBgRingColor = attributes.getColor(R.styleable.MarkView_mv_ringColor,
                DEFAULT_BG_RING_COLOR);

        mTextColor = attributes.getColor(R.styleable.MarkView_mv_textColor, DEFAULT_TEXT_COLOR);
        mTextSize = attributes.getDimension(R.styleable.MarkView_mv_textSize, DEFAULT_TEXT_SIZE);

        mSecondLineTextColor = attributes.getColor(R.styleable.MarkView_mv_secondLine_textColor, DEFAULT_TEXT_COLOR);
        mSecondLineTextSize = attributes.getDimension(R.styleable.MarkView_mv_secondLine_textSize, DEFAULT_TEXT_SIZE);

        setMax(attributes.getInt(R.styleable.MarkView_mv_max, DEFAULT_MAX));
        setMark(attributes.getInt(R.styleable.MarkView_mv_mark, 0));
    }

    private void initPainters() {
        mTextPaint = new TextPaint();
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        // Use Open Sans
        mTextPaint.setTypeface(TypefaceCache.getTypeface(getContext()));

        mSecondLineTextPaint = new TextPaint();
        mSecondLineTextPaint.setColor(mSecondLineTextColor);
        mSecondLineTextPaint.setTextSize(mSecondLineTextSize);
        mSecondLineTextPaint.setAntiAlias(true);
        // Use Open Sans
        mSecondLineTextPaint.setTypeface(TypefaceCache.getTypeface(getContext()));


        mPaint = new Paint();
        mPaint.setColor(getStrokeColor());
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mStrokeWidth);

        mBgRingPaint = new Paint();
        mBgRingPaint.setColor(mBgRingColor);
        mBgRingPaint.setStyle(Paint.Style.STROKE);
        mBgRingPaint.setAntiAlias(true);
        mBgRingPaint.setStrokeWidth(mBgRingWidth);

        mInnerCirclePaint = new Paint();
        mInnerCirclePaint.setColor(Color.TRANSPARENT);
        mInnerCirclePaint.setAntiAlias(true);
    }

    @Override
    public void invalidate() {
        initPainters();
        super.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
    }

    private int measure(int measureSpec) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = MIN_SIZE;
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.min(result, size);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        String text = isMarkValid(mMark) ? String.valueOf(mMark) + "%" : "?";
        if (!TextUtils.isEmpty(text)) {
            float textHeight = mTextPaint.descent() + mTextPaint.ascent();
            float yPositionOfText = (getWidth() - textHeight) / 2.0f;
            canvas.drawText(text, (getWidth() - mTextPaint.measureText(text)) / 2.0f, yPositionOfText, mTextPaint);

            String viewsText = getResources().getString(R.string.stats_views);
            float paddingBetweenTextRows = -10f;
            //float secondRowtextHeight = mSecondLineTextPaint.descent() + mSecondLineTextPaint.ascent();
            canvas.drawText(viewsText, (getWidth() - mSecondLineTextPaint.measureText(viewsText)) / 2.0f,
                   yPositionOfText - (textHeight + paddingBetweenTextRows), mSecondLineTextPaint);
        }

        float delta = mStrokeWidth / 2f;

        // Move the canvas down so the bottom part of the circle is outside the view
        canvas.translate(0f, 2 * delta);
        mOuterRect.set(delta,
                delta,
                getWidth() - delta,
                getHeight() - delta);

        float innerCircleRadius = (getWidth() - mStrokeWidth) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, innerCircleRadius, mInnerCirclePaint);
        canvas.drawArc(mOuterRect, 150, 240f, false, mBgRingPaint); // the background arch
        canvas.drawArc(mOuterRect, 150, getMarkAngle(), false, mPaint); // the blue indicator
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_STATE, super.onSaveInstanceState());
        bundle.putInt(STATE_TEXT_COLOR, getTextColor());
        bundle.putFloat(STATE_TEXT_SIZE, getTextSize());
        bundle.putIntArray(STATE_STROKE_COLORS, getStrokeColors());
        bundle.putFloat(STATE_STROKE_WIDTH, getStrokeWidth());
        bundle.putInt(STATE_MAX, getMax());
        bundle.putInt(STATE_MARK, getMark());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            mTextColor = bundle.getInt(STATE_TEXT_COLOR);
            mTextSize = bundle.getFloat(STATE_TEXT_SIZE);
            mStrokeColors = bundle.getIntArray(STATE_STROKE_COLORS);
            mStrokeWidth = bundle.getFloat(STATE_STROKE_WIDTH);
            initPainters();
            setMax(bundle.getInt(STATE_MAX));
            setMark(bundle.getInt(STATE_MARK));
            super.onRestoreInstanceState(bundle.getParcelable(STATE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * Returns stroke width
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * Sets stroke width
     */
    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        invalidate();
    }

    private float getMarkAngle() {
        if (!isMarkValid(mMark)) return 240f;
        return getMark() / (float) mMax * 240f;
    }

    /**
     * Returns the current mark
     */
    public int getMark() {
        return mMark;
    }

    /**
     * Sets the mark
     */
    public void setMark(int mark) {
        mMark = mark;
        invalidate();
    }

    /**
     * Checks whether a given mark is valid according to the current configuration
     */
    public boolean isMarkValid(int mark) {
        return mark > 0 && mark <= mMax;
    }

    /**
     * Returns the highest mark this MarkView accepts
     */
    public int getMax() {
        return mMax;
    }

    /**
     * Sets the highest mark this MarkView accepts
     */
    public void setMax(int max) {
        if (max > 0) {
            mMax = max;
            invalidate();
        }
    }

    /**
     * Returns the current text size
     */
    public float getTextSize() {
        return mTextSize;
    }

    /**
     * Sets the text size
     */
    public void setTextSize(float textSize) {
        mTextSize = textSize;
        invalidate();
    }

    /**
     * Returns the current text color
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * Sets the text size to a given color
     */
    public void setTextColor(int textColor) {
        mTextColor = textColor;
        invalidate();
    }

    /**
     * Returns the current stroke colors array
     */
    public int[] getStrokeColors() {
        return mStrokeColors;
    }

    /**
     * Sets the stroke colors.
     * <p/>
     * See {@link #getStrokeColor()} to see how stroke colors are handled.
     */
    public void setStrokeColors(int... strokeColors) {
        mStrokeColors = strokeColors;
        invalidate();
    }

    /**
     * Returns the stroke color depending on the current mark.
     * <p/>
     * May return:
     * - DEFAULT_STROKE_COLOR if the mark is invalid or there are no colors in mStrokeColors.
     * - The color at the position of the mark in mStrokeColors if it contains the mark.
     * - The last color in mStrokeColors if it doesn't contain the mark.
     */
    public int getStrokeColor() {
        if (mStrokeColors == null || mStrokeColors.length == 0 || !isMarkValid(mMark)) {
            return DEFAULT_STROKE_COLOR;
        }
        if (mStrokeColors.length < mMark) {
            return mStrokeColors[mStrokeColors.length - 1];
        }
        return mStrokeColors[mMark - 1];
    }

    /**
     * Returns the width of the background ring
     */
    public float getRingWidth() {
        return mBgRingWidth;
    }

    /**
     * Sets the width of the background ring
     */
    public void setRingWidth(float width) {
        mBgRingWidth = width;
    }

    /**
     * Returns the color of the background ring
     */
    public int getRingColor() {
        return mBgRingColor;
    }

    /**
     * Sets the color of the background ring
     */
    public void setRingColor(int color) {
        mBgRingColor = color;
    }
}