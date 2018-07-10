package org.wordpress.android.util

import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan

class QuickStartUtils {
    companion object {
        /**
         * Formats the string, to highlight text between %1$s and %2$s with specified color, and add an icon
         * in front of it if necessary
         *
         * @param string String to process
         * @param color Color to highlight the specific area of the string and tint the icon with
         * @param icon Icon that goes before the highlighted area
         */
        @JvmStatic
        @JvmOverloads
        fun stylizeQuickStartPrompt(string: String, color: Int, icon: Drawable? = null): Spannable {
            val startOfHighlight = string.indexOf("%1${'$'}s")

            var clearString = string.replace("%1${'$'}s", if (icon != null) " " else "")

            val endOfHighlight = clearString.indexOf("%2${'$'}s")
            clearString = clearString.replace("%2${'$'}s", "")

            val spannedString = SpannableStringBuilder(clearString)
            spannedString.setSpan(ForegroundColorSpan(color),
                    startOfHighlight, endOfHighlight, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (icon != null) {
                DrawableCompat.setTint(icon, color)
                spannedString.setSpan(ImageSpan(icon), startOfHighlight, startOfHighlight + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return spannedString
        }
    }
}

