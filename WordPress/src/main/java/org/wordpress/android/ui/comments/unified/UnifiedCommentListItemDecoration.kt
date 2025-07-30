package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import org.wordpress.android.R
import org.wordpress.android.util.RtlUtils
import kotlin.math.roundToInt
import android.R as AndroidR
import androidx.core.graphics.withSave
import androidx.core.content.withStyledAttributes

/**
 * This ItemDecoration adds margin to the start of the divider and skip drawing divider for list sub-headers.
 * Based on DividerItemDecoration.
 */
class UnifiedCommentListItemDecoration(val context: Context) : ItemDecoration() {
    private var divider: Drawable? = null
    private val bounds = Rect()
    private var dividerStartOffset = 0

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        parent.layoutManager?.let {
            divider?.let { divider ->
                canvas.withSave {
                    val left: Int
                    val right: Int
                    if (parent.clipToPadding) {
                        left = parent.paddingStart
                        right = parent.width - parent.paddingEnd
                        clipRect(
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
                            divider.draw(this)
                        }
                    }
                }
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: State
    ) {
        divider?.run {
            val viewHolder = parent.getChildViewHolder(view)
            if (viewHolder is UnifiedCommentSubHeaderViewHolder) {
                outRect.setEmpty()
            } else {
                outRect[0, 0, 0] = intrinsicHeight
            }
        } ?: run {
            outRect[0, 0, 0] = 0
        }
    }

    companion object {
        private val ATTRS = intArrayOf(AndroidR.attr.listDivider)
    }

    init {
        context.withStyledAttributes(null, ATTRS) {
            divider = getDrawable(0)
            divider
        }
        dividerStartOffset = context.resources.getDimensionPixelOffset(R.dimen.comment_list_divider_start_offset)
    }
}
