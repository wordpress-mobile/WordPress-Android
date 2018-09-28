package org.wordpress.android.widgets

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spannable
import android.text.style.LineBackgroundSpan
import android.text.style.UpdateAppearance

class ColoredUnderlineSpan(color: Int) : LineBackgroundSpan, UpdateAppearance {
    private val paint: Paint = Paint()
    private val underlineHeight = 5f

    init {
        paint.color = color
        paint.strokeWidth = underlineHeight
        paint.style = Paint.Style.FILL_AND_STROKE
    }

    override fun drawBackground(
        c: Canvas,
        p: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        val spanStart = (text as Spannable).getSpanStart(this)
        val spanEnd = text.getSpanEnd(this)

        if (spanStart < start) return
        if (spanEnd > end) return

        var offsetX = 0F
        if (spanStart > start) {
            offsetX = p.measureText(text.subSequence(start, spanStart).toString())
        }

        val length = p.measureText(
                text.subSequence(
                        Math.max(start, spanStart),
                        Math.min(end, spanEnd)
                ).toString()
        )
        c.drawLine(
                offsetX,
                bottom.toFloat() - underlineHeight,
                length + offsetX,
                bottom.toFloat() - underlineHeight,
                paint
        )
    }
}
