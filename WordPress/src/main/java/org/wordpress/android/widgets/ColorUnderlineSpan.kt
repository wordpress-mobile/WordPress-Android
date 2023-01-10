package org.wordpress.android.widgets

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class ColorUnderlineSpan(var color: Int) : CharacterStyle(), UpdateAppearance {
    @Suppress("SwallowedException")
    override fun updateDrawState(tp: TextPaint) {
        try {
            // we have to resort to reflection instead of custom span, because of the bug in LineBackgroundSpan
            // that ignores part of the string in some circumstances
            TextPaint::class.java.getMethod(
                "setUnderlineText",
                Integer.TYPE,
                java.lang.Float.TYPE
            ).invoke(tp, color, 5f)
        } catch (e: Exception) {
            // fallback to normal underline
            tp.isUnderlineText = true
        }
    }
}
