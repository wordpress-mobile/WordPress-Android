package org.wordpress.android.ui.mlp

import android.os.Bundle
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R

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
 * Modal Layout Picker categories view holder
 */
class CategoriesItemViewHolder(parent: ViewGroup, private val layoutSelectionListener: LayoutSelectionListener) :
        ModalLayoutPickerViewHolder(
                parent,
                R.layout.modal_layout_picker_categories_row
        ) {
    private val recycler: RecyclerView = itemView.findViewById(R.id.categories_recycler_view)
    fun bind(item: ModalLayoutPickerListItem.Categories) {
        val childLayoutManager = LinearLayoutManager(recycler.context, RecyclerView.HORIZONTAL, false)
        childLayoutManager.initialPrefetchItemCount = 4
        val viewPool = RecyclerView.RecycledViewPool()

        recycler.apply {
            layoutManager = childLayoutManager
            adapter = CategoriesAdapter(recycler.context, item.categories, layoutSelectionListener)
            setRecycledViewPool(viewPool)
        }
    }
}

/**
 * Modal Layout Picker layouts view holder
 */
class LayoutsItemViewHolder(
    parent: ViewGroup,
    private val scrollStates: SparseIntArray,
    private val layoutSelectionListener: LayoutSelectionListener
) :
        ModalLayoutPickerViewHolder(
                parent,
                R.layout.modal_layout_picker_layouts_row
        ) {
    private val title: TextView = itemView.findViewById(R.id.title)

    val recycler: RecyclerView by lazy {
        itemView.findViewById<RecyclerView>(R.id.layouts_recycler_view).apply {
            layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.HORIZONTAL,
                    false
            ).apply { initialPrefetchItemCount = 4 }
            adapter = LayoutsAdapter(context, layoutSelectionListener)
            setRecycledViewPool(RecyclerView.RecycledViewPool())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val firstVisibleItem = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    scrollStates.put(adapterPosition, firstVisibleItem)
                }
            })
        }
    }

    fun bind(item: ModalLayoutPickerListItem.LayoutCategory) {
        title.text = item.description
        (recycler.adapter as LayoutsAdapter).setData(item.layouts)
        val lastSeenFirstPosition = scrollStates.get(layoutPosition, 0)
        if (lastSeenFirstPosition >= 0) {
            recycler.scrollToPosition(lastSeenFirstPosition)
        }
    }
}
