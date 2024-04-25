package org.wordpress.android.ui.notifications.blocks

import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.widget.TextView

/**
 * Allows links to be highlighted when tapped on note blocks.
 * See: http://stackoverflow.com/a/20905824/309558
 */
class NoteBlockLinkMovementMethod : LinkMovementMethod() {
    private var mPressedSpan: NoteBlockClickableSpan? = null
    override fun onTouchEvent(textView: TextView, spannable: Spannable, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressedSpan = getPressedSpan(textView, spannable, event)
                if (mPressedSpan != null) {
                    mPressedSpan?.setPressed(true)
                    Selection.setSelection(
                        spannable, spannable.getSpanStart(mPressedSpan),
                        spannable.getSpanEnd(mPressedSpan)
                    )
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val touchedSpan = getPressedSpan(textView, spannable, event)
                if (mPressedSpan != null && touchedSpan !== mPressedSpan) {
                    mPressedSpan?.setPressed(false)
                    mPressedSpan = null
                    Selection.removeSelection(spannable)
                }
            }

            else -> {
                if (mPressedSpan != null) {
                    mPressedSpan?.setPressed(false)
                    super.onTouchEvent(textView, spannable, event)
                }
                mPressedSpan = null
                Selection.removeSelection(spannable)
            }
        }
        return true
    }

    private fun getPressedSpan(textView: TextView, spannable: Spannable, event: MotionEvent): NoteBlockClickableSpan? {
        val x = event.x + textView.scrollX - textView.totalPaddingLeft
        val y = event.y + textView.scrollY - textView.totalPaddingTop
        val line = textView.layout.getLineForVertical(y.toInt())
        val off = textView.layout.getOffsetForHorizontal(line, x)
        val link = spannable.getSpans(off, off, NoteBlockClickableSpan::class.java)
        if (link.isNotEmpty()) {
            return link[0]
        }
        return null
    }
}
