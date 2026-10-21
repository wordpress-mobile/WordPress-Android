package org.wordpress.android.widgets

import android.os.Build
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class ColorUnderlineSpan(var color: Int) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        // a custom LineBackgroundSpan ignores part of the string in some circumstances, so set the underline on the
        // paint instead; a colored underline is only public API from Q, older versions get a plain one
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tp.underlineColor = color
            tp.underlineThickness = UNDERLINE_THICKNESS
        } else {
            tp.isUnderlineText = true
        }
    }

    companion object {
        private const val UNDERLINE_THICKNESS = 5f
    }
}
