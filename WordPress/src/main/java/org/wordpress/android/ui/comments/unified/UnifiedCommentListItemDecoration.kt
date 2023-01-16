package org.wordpress.android.ui.comments.unified

import android.R.attr
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import org.wordpress.android.R.dimen
import org.wordpress.android.util.RtlUtils
import kotlin.math.roundToInt

/**
 * This ItemDecoration adds margin to the start of the divider and skipp drawing divider for list sub-headers.
 * Based on DividerItemDecoration.
 */
class UnifiedCommentListItemDecoration(val context: Context) : ItemDecoration() {
    private val divider: Drawable?
    private val bounds = Rect()
    private var dividerStartOffset = 0

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        if (parent.layoutManager == null || divider == null) {
            return
        }
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingStart
            right = parent.width - parent.paddingEnd
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(child)
            if (viewHolder !is UnifiedCommentSubHeaderViewHolder && viewHolder !is LoadStateViewHolder) {
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val bottom = bounds.bottom + child.translationY.roundToInt()
                val top = bottom - divider.intrinsicHeight
                if (RtlUtils.isRtl(context)) {
                    divider.setBounds(left, top, right - dividerStartOffset, bottom)
                } else {
                    divider.setBounds(left + dividerStartOffset, top, right, bottom)
                }
                divider.draw(canvas)
            }
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: State
    ) {
        if (divider == null) {
            outRect[0, 0, 0] = 0
            return
        }
        val viewHolder = parent.getChildViewHolder(view)
        if (viewHolder is UnifiedCommentSubHeaderViewHolder) {
            outRect.setEmpty()
        } else {
            outRect[0, 0, 0] = divider.intrinsicHeight
        }
    }

    companion object {
        private val ATTRS = intArrayOf(attr.listDivider)
    }

    init {
        val attrs = context.obtainStyledAttributes(ATTRS)
        divider = attrs.getDrawable(0)
        attrs.recycle()
        dividerStartOffset = context.resources.getDimensionPixelOffset(dimen.comment_list_divider_start_offset)
    }
}
