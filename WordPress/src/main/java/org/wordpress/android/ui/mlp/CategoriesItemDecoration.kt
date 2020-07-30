package org.wordpress.android.ui.mlp

import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType
import kotlin.math.max

/**
 * Implements the Categories 'sticky' row behavior
 */
class CategoriesItemDecoration : RecyclerView.ItemDecoration() {
    private lateinit var stickyView: View
    private lateinit var scrollingView: View

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (ViewType.CATEGORIES.id == parent.adapter?.getItemViewType(parent.getChildAdapterPosition(view))) {
            scrollingView = view
        }
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)
        if (scrollingView.y > 0 || !::scrollingView.isInitialized) return
        if (!::stickyView.isInitialized) {
            stickyView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.modal_layout_picker_categories_row, parent, false)
        }
        canvas.save()
        canvas.translate(0f, max(0f, stickyView.top.toFloat() - stickyView.height.toFloat()))
        scrollingView.draw(canvas)
        canvas.restore()
    }
}
