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

        /**
         * Adds animated quick start focus point targetedView to the top level parent,
         * and places it in the top-right cornere of the specified targetedView.
         *
         * @param topLevelParent Parent where quick start focus targetedView will be added.
         * Usually Relative or Frame layout
         * @param targetedView View in top-right corner of which the quick start focus view will be placed. Child of
         * topLevelParent
         * @param rightOffset specifies in px how much should we move view to the left from the right
         * @param topOffset specifies in px how much should we move view to the bottom from the top
         */
        @JvmStatic
        fun addQuickStartFocusPointAboveTheView(
            topLevelParent: ViewGroup,
            targetedView: View,
            rightOffset: Int,
            topOffset: Int
        ) {
            topLevelParent.post {
                val quickStartFocusPointView = LayoutInflater.from(topLevelParent.context)
                        .inflate(R.layout.quick_start_focus_point_view, topLevelParent, false)
                val focusPointSize =
                        topLevelParent.context.resources.getDimensionPixelOffset(R.dimen.quick_start_focus_point_size)

                val topLevelParentViewLocation = IntArray(2)
                topLevelParent.getLocationInWindow(topLevelParentViewLocation)

                val topLevelParentsVerticalOffset = topLevelParentViewLocation[1]

                val focusPointTargetViewLocation = IntArray(2)
                targetedView.getLocationOnScreen(focusPointTargetViewLocation)

                val x = focusPointTargetViewLocation[0] + targetedView.width - focusPointSize - rightOffset
                val y = focusPointTargetViewLocation[1] - topLevelParentsVerticalOffset + topOffset

                val params = quickStartFocusPointView.layoutParams as MarginLayoutParams
                params.leftMargin = x
                params.topMargin = y
                topLevelParent.addView(quickStartFocusPointView)

                quickStartFocusPointView.post {
                    quickStartFocusPointView.layoutParams = params
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
    }
}
