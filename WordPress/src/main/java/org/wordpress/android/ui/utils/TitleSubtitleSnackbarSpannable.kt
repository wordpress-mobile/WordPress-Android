package org.wordpress.android.ui.utils

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import org.wordpress.android.R
import org.wordpress.android.util.extensions.getColorFromAttribute

object TitleSubtitleSnackbarSpannable {
    private const val SUBTITLE_ALPHA: Int = (0.6 * 0xFF).toInt()
    private const val LINE_BREAK = "\n"

    /**
     * Creates a Spannable with title and subtitle to be used inside Snackbar.
     * @param context needs to be the Activity context of the Activity where the snackbar is going to be shown, using an
     * Application Context might result in a crash, since some attributes and styles might not be applied properly.
     * @param title the [CharSequence] that will have the default snackbar text style, used as a title
     * @param subtitle the [CharSequence] that will be slightly more transparent text style, used as subtitle
     */
    fun create(context: Context, title: CharSequence, subtitle: CharSequence): Spannable {
        val color = context.getColorFromAttribute(R.attr.colorSurface) // color used for Snackbar text
        val subtitleColor = Color.argb(SUBTITLE_ALPHA, color.red, color.green, color.blue)
        val spannable = SpannableStringBuilder()

        spannable.append(title)
        spannable.append(LINE_BREAK)
        spannable.append(subtitle)
        spannable.setSpan(
            ForegroundColorSpan(subtitleColor),
            title.length + LINE_BREAK.length,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }
}
