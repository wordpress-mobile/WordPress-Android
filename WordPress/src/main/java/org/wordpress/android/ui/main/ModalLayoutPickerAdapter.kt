package org.wordpress.android.ui.main

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.Other
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.Subtitle
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.Title
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType
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
            is OtherItemViewHolder -> holder.bind(items[position] as Other)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModalLayoutPickerViewHolder = when (viewType) {
        ViewType.TITLE.id -> TitleItemViewHolder(parent)
        ViewType.SUBTITLE.id -> SubtitleItemViewHolder(parent)
        ViewType.OTHER.id -> OtherItemViewHolder(parent)
        else -> throw IllegalArgumentException("Unexpected view type in ModalLayoutPickerAdapter")
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.id
    }
}
