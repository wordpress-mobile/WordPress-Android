package org.wordpress.android.ui.mlp

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Categories
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Layouts
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Subtitle
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.Title
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

/**
 * Renders the rows of the Modal Layout Picker
 */
class ModalLayoutPickerAdapter(context: Context) : Adapter<ModalLayoutPickerViewHolder>() {
    private var items: List<ModalLayoutPickerListItem> = listOf()
    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

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
            is LayoutsItemViewHolder -> holder.bind(items[position] as Layouts)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModalLayoutPickerViewHolder = when (viewType) {
        ViewType.TITLE.id -> TitleItemViewHolder(parent)
        ViewType.SUBTITLE.id -> SubtitleItemViewHolder(parent)
        ViewType.CATEGORIES.id -> CategoriesItemViewHolder(parent)
        ViewType.LAYOUTS.id -> LayoutsItemViewHolder(parent)
        else -> throw IllegalArgumentException("Unexpected view type in ModalLayoutPickerAdapter")
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(CategoriesItemDecoration())
    }

    override fun getItemViewType(position: Int): Int {
        if (position < 0) return ViewType.CATEGORIES.id
        return items[position].type.id
    }
}
