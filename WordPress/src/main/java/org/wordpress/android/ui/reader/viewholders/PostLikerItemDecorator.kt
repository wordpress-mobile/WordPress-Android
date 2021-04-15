package org.wordpress.android.ui.reader.viewholders

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

class PostLikerItemDecorator(context: Context, @DimenRes leftOffsetResId: Int) : ItemDecoration() {
    private val leftOffset: Int = context.resources.getDimensionPixelSize(leftOffsetResId)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)

        if (position > 0) {
            outRect.set((outRect.left - leftOffset), outRect.top, outRect.right, outRect.bottom)
        }
    }
}
