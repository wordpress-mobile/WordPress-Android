package org.wordpress.android.ui.layoutpicker

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R

/**
 * Modal Layout Picker layouts view holder
 */
class LayoutsItemViewHolder(
    parent: ViewGroup,
    private val prefetchItemCount: Int = 4
) : RecyclerView.ViewHolder(
        LayoutInflater.from(
                parent.context
        ).inflate(R.layout.modal_layout_picker_layouts_row, parent, false)
) {
    private val title: TextView = itemView.findViewById(R.id.title)

    private val recycler: RecyclerView by lazy {
        itemView.findViewById<RecyclerView>(R.id.layouts_recycler_view).apply {
            layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.HORIZONTAL,
                    false
            ).apply { initialPrefetchItemCount = prefetchItemCount }
            setRecycledViewPool(RecyclerView.RecycledViewPool())
            adapter = LayoutsAdapter(parent.context)
        }
    }

    fun bind(category: LayoutCategoryUiState) {
        title.text = category.description
        (recycler.adapter as LayoutsAdapter).setData(category.layouts)
    }
}
