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
import org.wordpress.android.ui.main.WPMainActivity

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
        fun addQuickStartFocusPointAboveTheView(topLevelParent: ViewGroup, view: View, horizontalOffset: Int,
            verticalOffset: Int) {

            topLevelParent.post {
                val quickStartFocusPointView = LayoutInflater.from(topLevelParent.context)
                        .inflate(R.layout.quick_start_focus_point_view, topLevelParent, false)

                val parentViewLocation = IntArray(2)
                topLevelParent.getLocationInWindow(parentViewLocation)

                val parentsVerticalOffset = parentViewLocation[1]
                val focusPointSize = topLevelParent.context.resources.getDimensionPixelOffset(R.dimen.quick_start_focus_point_size)

                view.post {
                    val focusPointContainerLocation = IntArray(2)
                    view.getLocationOnScreen(focusPointContainerLocation)

                    val x = focusPointContainerLocation[0] + view.width - focusPointSize - horizontalOffset
                    val y = focusPointContainerLocation[1] - parentsVerticalOffset + verticalOffset

                    val params = quickStartFocusPointView.layoutParams as MarginLayoutParams
                    params.leftMargin = x
                    params.topMargin = y
                    topLevelParent.addView(quickStartFocusPointView)

                    quickStartFocusPointView.post {
                        quickStartFocusPointView.layoutParams = params
                    }
                }
            }
        }

        @JvmStatic
        fun removeQuickStartFocusPoint(topLevelParent: ViewGroup) {
            val focusPointView = topLevelParent.findViewById<View>(R.id.quick_start_focus_point)
            if (focusPointView != null) {
                val directParent = focusPointView.parent
                if (directParent is ViewGroup) {
                    directParent.removeView(focusPointView)
                }
            }
        }

        @JvmStatic
        fun addQuickStartFocusPointToBottomNavReaderButton(topLevelParent: WPMainActivity) {
            val focusPointView = topLevelParent.findViewById<View>(R.id.quick_start_focus_point)
            if (focusPointView != null) {
                val directParent = focusPointView.parent
                if (directParent is ViewGroup) {
                    directParent.removeView(focusPointView)
                }
            }
        }
    }
}
