package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class InterceptingFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    // NOTE: Intercepting touch event to avoid inner views (first use case was with an inner RecyclerView in
    // reader_post_likers_faces_list.xml)
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }
}
