package org.wordpress.android.ui.reader.viewholders

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

class PostLikerItemDecorator(isRtl: Boolean, context: Context, @DimenRes leftOffsetResId: Int) : ItemDecoration() {
    private val isRtl = isRtl
    private val offset: Int = context.resources.getDimensionPixelSize(leftOffsetResId)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)

        if (position > 0) {
            if (isRtl) {
                outRect.set(outRect.left, outRect.top, outRect.right - offset, outRect.bottom)
            } else {
                outRect.set(outRect.left - offset, outRect.top, outRect.right, outRect.bottom)
            }
        }
    }
}
