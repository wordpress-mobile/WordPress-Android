package org.wordpress.android.util

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import org.wordpress.android.R

class QuickStartUtils {
    companion object {
        /**
         * Formats the string, to highlight text between %1$s and %2$s with specified color, and add an icon
         * in front of it if necessary
         *
         * @param context Context used to access resources
         * @param messageId resources id of the message to display
         * @param iconId resource if of the icon that goes before the highlighted area
         */
        @JvmStatic
        fun stylizeQuickStartPrompt(context: Context, messageId: Int, iconId: Int): Spannable {
            val message = context.resources.getString(messageId)
            val icon = ContextCompat.getDrawable(context, iconId)

            val highlightColor = ContextCompat.getColor(context, R.color.blue_light)

            val startOfHighlight = message.indexOf("%1${'$'}s")
            var clearString = message.replace("%1${'$'}s", if (icon != null) " " else "")
            val endOfHighlight = clearString.indexOf("%2${'$'}s")
            clearString = clearString.replace("%2${'$'}s", "")

            val spannedString = SpannableStringBuilder(clearString)
            spannedString.setSpan(ForegroundColorSpan(highlightColor),
                    startOfHighlight, endOfHighlight, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (icon != null) {
                val iconSize = context.resources.getDimensionPixelOffset(R.dimen.dialog_snackbar_max_icons_size)
                icon.setBounds(0, 0, iconSize, iconSize)

                DrawableCompat.setTint(icon, highlightColor)
                spannedString.setSpan(ImageSpan(icon), startOfHighlight, startOfHighlight + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return spannedString
        }
    }
}
