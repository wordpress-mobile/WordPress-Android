package org.wordpress.android.ui.posts

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView
import android.text.Layout

class PostListTitleExcerptTextView(context: Context, attrs: AttributeSet) : MaterialTextView(context, attrs) {
    private var targetTextView: MaterialTextView? = null

    enum class CallingTextView {
        TITLE, EXCERPT
    }

    fun setTargetTextView(targetTextView: MaterialTextView, callingTextView: CallingTextView) {
        this.targetTextView = targetTextView

        // Post a runnable to adjust maxLines after the layout is complete
        post {
            adjustMaxLines(callingTextView)
        }
    }

    private fun adjustMaxLines(callingTextView: CallingTextView) {
        // Adjust maxLines based on the target TextView's line count
        val targetLayout: Layout? = targetTextView?.layout
        targetLayout?.lineCount.let { lineCount ->
            maxLines = when (callingTextView) {
                CallingTextView.TITLE -> if (lineCount == 1) 1 else 2
                CallingTextView.EXCERPT -> if (lineCount == 1) 2 else 1
            }
        }
    }
}
