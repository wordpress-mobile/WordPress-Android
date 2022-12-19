/*
 * Copyright (C) 2012 The Android Open Source Project
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
 *
 * Imported and modified from:
 * https://android.googlesource.com/platform/packages/apps/Camera/+/master/src/com/android/camera/drawable
 * /TextDrawable.java
 *
 */
package org.wordpress.android.editor;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.ColorInt;


public class TextDrawable extends Drawable {
    private static final int DEFAULT_COLOR = Color.WHITE;
    private TextPaint mPaint;
    private CharSequence mText;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private int mTranslateX;
    private int mTranslateY;
    private StaticLayout mTextLayout;

    public TextDrawable(Resources res, CharSequence text, float textSize) {
        mText = text;
        mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setTextSize(textSize);
        mIntrinsicWidth = (int) (mPaint.measureText(mText, 0, mText.length()) + .5);
        mIntrinsicHeight = mPaint.getFontMetricsInt(null);
        mTextLayout = new StaticLayout(mText, mPaint, mIntrinsicWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        canvas.translate(bounds.centerX() + mTranslateX, bounds.centerY() + mTranslateY);
        mTextLayout.draw(canvas);
    }

    @Override
    public int getOpacity() {
        return mPaint.getAlpha();
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        mPaint.setColorFilter(filter);
    }

    public void setColor(@ColorInt int color) {
        mPaint.setColor(color);
    }

    /**
     * Shift the text on the x axis by @param x pixels.
     */
    public void setTranslateX(int x) {
        mTranslateX = x;
    }
    /**
     * Shift the text on the y axis by @param y pixels.
     */
    public void setTranslateY(int y) {
        mTranslateY = y;
    }
}
