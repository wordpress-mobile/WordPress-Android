package org.wordpress.android.ui.mlp

import android.util.SparseIntArray
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Categories
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.LayoutCategory
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Subtitle
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Title
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType

/**
 * Renders the rows of the Modal Layout Picker
 */
class ModalLayoutPickerAdapter(
    private val layoutSelectionListener: LayoutSelectionListener
) : Adapter<ModalLayoutPickerViewHolder>() {
    private var items: List<ModalLayoutPickerListItem> = listOf()
    private val scrollStates = SparseIntArray()

    fun update(newItems: List<ModalLayoutPickerListItem>) {
        val diffResult = DiffUtil.calculateDiff(
                ModalLayoutPickerListDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ModalLayoutPickerViewHolder, position: Int) {
        when (holder) {
            is TitleItemViewHolder -> holder.bind(items[position] as Title)
            is SubtitleItemViewHolder -> holder.bind(items[position] as Subtitle)
            is CategoriesItemViewHolder -> holder.bind(items[position] as Categories)
            is LayoutsItemViewHolder -> holder.bind(items[position] as LayoutCategory)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModalLayoutPickerViewHolder = when (viewType) {
        ViewType.TITLE.id -> TitleItemViewHolder(parent)
        ViewType.SUBTITLE.id -> SubtitleItemViewHolder(parent)
        ViewType.CATEGORIES.id -> CategoriesItemViewHolder(parent)
        ViewType.LAYOUTS.id -> LayoutsItemViewHolder(parent, scrollStates, layoutSelectionListener)
        else -> throw IllegalArgumentException("Unexpected view type in ModalLayoutPickerAdapter")
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(CategoriesItemDecoration())
    }

    override fun getItemViewType(position: Int): Int {
        if (position < 0 || position >= items.size) return ViewType.TITLE.id
        return items[position].type.id
    }
}
