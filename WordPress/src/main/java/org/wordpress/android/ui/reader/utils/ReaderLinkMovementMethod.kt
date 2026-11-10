package org.wordpress.android.ui.reader.utils

import android.content.ActivityNotFoundException
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.view.MotionEvent
import android.widget.TextView
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption
import org.wordpress.android.util.AppLog
import java.util.EnumSet

/*
 * custom LinkMovementMethod which shows photo viewer when an image span is tapped
 */
class ReaderLinkMovementMethod private constructor(private val isPrivate: Boolean) : LinkMovementMethod() {
    override fun onTouchEvent(textView: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt() - textView.totalPaddingLeft + textView.scrollX
            val y = event.y.toInt() - textView.totalPaddingTop + textView.scrollY
            val layout = textView.layout
            val offset = layout.getOffsetForHorizontal(layout.getLineForVertical(y), x.toFloat())

            val image = buffer.getSpans(offset, offset, ImageSpan::class.java).firstOrNull()
            if (image != null) {
                val options = EnumSet.noneOf(PhotoViewerOption::class.java)
                if (isPrivate) {
                    options.add(PhotoViewerOption.IS_PRIVATE_IMAGE)
                }
                ReaderActivityLauncher.showReaderPhotoViewer(
                    textView.context,
                    image.source.orEmpty(),
                    null,
                    textView,
                    options,
                    event.x.toInt(),
                    event.y.toInt()
                )
                return true
            }
        }

        return try {
            super.onTouchEvent(textView, buffer, event)
        } catch (e: ActivityNotFoundException) {
            AppLog.e(AppLog.T.UTILS, e)
            false
        }
    }

    companion object {
        // separate instances depending on whether we're showing content from a private blog
        private val publicInstance by lazy { ReaderLinkMovementMethod(false) }
        private val privateInstance by lazy { ReaderLinkMovementMethod(true) }

        fun getInstance(isPrivate: Boolean): ReaderLinkMovementMethod =
            if (isPrivate) privateInstance else publicInstance
    }
}
