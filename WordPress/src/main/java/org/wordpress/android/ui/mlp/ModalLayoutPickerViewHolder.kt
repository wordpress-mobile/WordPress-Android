package org.wordpress.android.ui.mlp

import android.os.Bundle
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Categories
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.LayoutCategory

/**
 * Modal Layout Picker abstract list view holder
 */
sealed class ModalLayoutPickerViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(
        LayoutInflater.from(
                parent.context
        ).inflate(layout, parent, false)
) {
    open fun updateChanges(bundle: Bundle) {}
}

/**
 * Modal Layout Picker Title Item view holder
 */
class TitleItemViewHolder(parent: ViewGroup) : ModalLayoutPickerViewHolder(
        parent,
        R.layout.modal_layout_picker_title_row
) {
    private val title: TextView = itemView.findViewById(R.id.title)

    fun bind(item: ModalLayoutPickerListItem.Title) {
        title.setText(item.labelRes)
        title.visibility = if (item.visible) View.VISIBLE else View.INVISIBLE
    }
}

/**
 * Modal Layout Picker Subtitle Item view holder
 */
class SubtitleItemViewHolder(parent: ViewGroup) : ModalLayoutPickerViewHolder(
        parent,
        R.layout.modal_layout_picker_subtitle_row
) {
    private val subtitle: TextView = itemView.findViewById(R.id.subtitle)

    fun bind(item: ModalLayoutPickerListItem.Subtitle) {
        subtitle.setText(item.labelRes)
    }
}

/**
 * Modal Layout Picker abstract horizontal RecyclerView view holder
 */
abstract class HorizontalRecyclerViewHolder(
    parent: ViewGroup,
    private val scrollStates: SparseIntArray,
    private val recyclerAdapter: RecyclerView.Adapter<*>,
    @LayoutRes private val layoutResId: Int,
    @IdRes private val recyclerResId: Int,
    private val prefetchItemCount: Int = 4
) : ModalLayoutPickerViewHolder(parent, layoutResId) {
    protected val recycler: RecyclerView by lazy {
        itemView.findViewById<RecyclerView>(recyclerResId).apply {
            layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.HORIZONTAL,
                    false
            ).apply { initialPrefetchItemCount = prefetchItemCount }
            setRecycledViewPool(RecyclerView.RecycledViewPool())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val firstVisibleItem = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    scrollStates.put(adapterPosition, firstVisibleItem)
                }
            })
            adapter = recyclerAdapter
        }
    }

    open fun bind(item: ModalLayoutPickerListItem) {
        val lastSeenFirstPosition = scrollStates.get(layoutPosition, 0)
        if (lastSeenFirstPosition >= 0) {
            recycler.scrollToPosition(lastSeenFirstPosition)
        }
    }
}

/**
 * Modal Layout Picker categories view holder
 */
class CategoriesItemViewHolder(
    parent: ViewGroup,
    scrollStates: SparseIntArray,
    layoutSelectionListener: LayoutSelectionListener
) : HorizontalRecyclerViewHolder(
        parent,
        scrollStates,
        CategoriesAdapter(parent.context, layoutSelectionListener),
        R.layout.modal_layout_picker_categories_row,
        R.id.categories_recycler_view
) {
    override fun bind(item: ModalLayoutPickerListItem) {
        (recycler.adapter as CategoriesAdapter).setData((item as Categories).categories)
        super.bind(item)
    }
}

/**
 * Modal Layout Picker layouts view holder
 */
class LayoutsItemViewHolder(
    parent: ViewGroup,
    private val scrollStates: SparseIntArray,
    private val layoutSelectionListener: LayoutSelectionListener
) : HorizontalRecyclerViewHolder(
        parent,
        scrollStates,
        LayoutsAdapter(parent.context, layoutSelectionListener),
        R.layout.modal_layout_picker_layouts_row,
        R.id.layouts_recycler_view
) {
    private val title: TextView = itemView.findViewById(R.id.title)

    override fun bind(item: ModalLayoutPickerListItem) {
        val category = item as LayoutCategory
        title.text = category.description
        (recycler.adapter as LayoutsAdapter).setData(category.layouts)
        super.bind(item)
    }
}
