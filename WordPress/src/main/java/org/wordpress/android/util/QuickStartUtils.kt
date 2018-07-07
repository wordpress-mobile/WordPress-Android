package org.wordpress.android.util

import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import org.wordpress.android.R

class QuickStartUtils {
    companion object {
        const val BOTTOM_NAV_READER_BUTTON_TAG = "quick_start_reader_button"
        const val BOTTOM_NAV_NEW_POST_BUTTON_TAG = "quick_start_new_post_button"

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

        @JvmStatic
        fun addQuickStartFocusPointToCoordinates(parent: ViewGroup, x: Int, y: Int) {
            val quickStartFocusPointView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.quick_start_focus_point_standalone, parent, false)

            val params = quickStartFocusPointView.layoutParams as MarginLayoutParams
            params.leftMargin = x
            params.topMargin = y
            parent.addView(quickStartFocusPointView)

            quickStartFocusPointView.post { quickStartFocusPointView.layoutParams = params }
        }

        @JvmStatic
        fun removeQuickStartFocusPoint(parent: ViewGroup) {
            val focusPointView = parent.findViewById<View>(R.id.quick_start_focus_point)
            if (focusPointView != null) {
                parent.removeView(focusPointView)
            }
        }
    }
}
