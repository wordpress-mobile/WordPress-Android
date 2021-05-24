package org.wordpress.android.ui.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.style.LineBackgroundSpan
import kotlin.math.roundToInt

/**
 * Creates BackgroundColorSpan with padding
 * Credits: https://medium.com/@tokudu/android-adding-padding-to-backgroundcolorspan-179ab4fae187
 */
class PaddingBackgroundColorSpan(private val backgroundColor: Int, private val padding: Int) : LineBackgroundSpan {
    private val bgRect: Rect = Rect()
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        val textWidth = paint.measureText(text, start, end).roundToInt()
        val paintColor = paint.color
        bgRect.set(
            left - padding,
            top - if (lnum == 0) padding / 2 else -(padding / 2),
            left + textWidth + padding,
            bottom + padding / 2
        )
        paint.color = backgroundColor
        canvas.drawRect(bgRect, paint)
        paint.color = paintColor
    }
}
