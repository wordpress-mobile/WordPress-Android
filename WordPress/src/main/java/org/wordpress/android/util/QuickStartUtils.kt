package org.wordpress.android.util

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.Html
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
            val spanTagOpen = context.resources.getString(R.string.quick_start_span_start)
            val spanTagEnd = context.resources.getString(R.string.quick_start_span_end)
            val formattedMessage = context.resources.getString(messageId, spanTagOpen, spanTagEnd)

            val spannedMessage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(formattedMessage, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(formattedMessage)
            }

            val highlightColor = ContextCompat.getColor(context, R.color.blue_light)

            val mutableSpannedMessage = SpannableStringBuilder(spannedMessage)
            val foregroundColorSpan = mutableSpannedMessage
                    .getSpans(0, spannedMessage.length, ForegroundColorSpan::class.java).firstOrNull()

            // nothing to highlight
            if (foregroundColorSpan != null) {
                val startOfHighlight = mutableSpannedMessage.getSpanStart(foregroundColorSpan)
                val endOfHighlight = mutableSpannedMessage.getSpanEnd(foregroundColorSpan)

                mutableSpannedMessage.removeSpan(foregroundColorSpan)
                mutableSpannedMessage.setSpan(ForegroundColorSpan(highlightColor),
                        startOfHighlight, endOfHighlight, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                val icon: Drawable? = try {
                    ContextCompat.getDrawable(context, iconId)
                } catch (e: Resources.NotFoundException) {
                    null
                }

                if (icon != null) {
                    val iconSize = context.resources.getDimensionPixelOffset(R.dimen.dialog_snackbar_max_icons_size)
                    icon.setBounds(0, 0, iconSize, iconSize)

                    DrawableCompat.setTint(icon, highlightColor)
                    mutableSpannedMessage.setSpan(ImageSpan(icon), startOfHighlight - 1, startOfHighlight,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            return mutableSpannedMessage
        }
    }
}
