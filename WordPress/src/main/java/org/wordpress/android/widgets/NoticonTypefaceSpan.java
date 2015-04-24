package org.wordpress.android.widgets;


import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import org.wordpress.android.R;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;

public class NoticonTypefaceSpan extends MetricAffectingSpan {
    private final Typeface mTypeface;
    private int mColor;

    public NoticonTypefaceSpan(Context context) {
        mTypeface = TypefaceCache.getTypefaceForTypefaceName(context, NotificationsUtils.NOTICON_FONT_NAME);
        mColor = context.getResources().getColor(R.color.grey);
    }

    @Override
    public void updateDrawState(final TextPaint drawState) {
        apply(drawState);
    }

    @Override
    public void updateMeasureState(final TextPaint paint) {
        apply(paint);
    }

    private void apply(final Paint paint) {
        paint.setTypeface(mTypeface);
        paint.setColor(mColor);
    }
}
