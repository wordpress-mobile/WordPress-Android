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

        /**
         * Adds animated quick start focus point targetedView to the top level parent,
         * and places it in the top-right corner of the specified targetedView.
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
                topLevelParent.getLocationOnScreen(topLevelParentViewLocation)

                val topLevelParentsHorizontalOffset = topLevelParentViewLocation[0]
                val topLevelParentsVerticalOffset = topLevelParentViewLocation[1]

                val focusPointTargetViewLocation = IntArray(2)
                targetedView.getLocationOnScreen(focusPointTargetViewLocation)

                val x = focusPointTargetViewLocation[0] - topLevelParentsHorizontalOffset + targetedView.width -
                focusPointSize - rightOffset
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
