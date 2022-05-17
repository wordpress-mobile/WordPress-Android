package org.wordpress.android.ui.layoutpicker

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import org.wordpress.android.R
import org.wordpress.android.util.extensions.setVisible

sealed class LayoutsRowViewHolder(view: View) : RecyclerView.ViewHolder(view)

class LayoutsFooterViewHolder(parent: ViewGroup, footerLayoutResId: Int) :
        LayoutsRowViewHolder(
                LayoutInflater.from(parent.context).inflate(footerLayoutResId, parent, false)
        )

/**
 * Modal Layout Picker layouts view holder
 */
class LayoutsItemViewHolder(
    parent: ViewGroup,
    private val prefetchItemCount: Int = 4,
    private val showRowDividers: Boolean,
    private var nestedScrollStates: Bundle,
    private val thumbDimensionProvider: ThumbDimensionProvider,
    private val recommendedDimensionProvider: ThumbDimensionProvider?
) : LayoutsRowViewHolder(
        LayoutInflater.from(
                parent.context
        ).inflate(R.layout.modal_layout_picker_layouts_row, parent, false)
) {
    private val title: TextView = itemView.findViewById(R.id.title)
    private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
    private var currentItem: LayoutCategoryUiState? = null

    private val recycler: RecyclerView by lazy {
        val dimensionProvider = if (currentItem?.isRecommended == true && recommendedDimensionProvider != null) {
            recommendedDimensionProvider
        } else {
            thumbDimensionProvider
        }
        itemView.updateLayoutParams {
            height = dimensionProvider.rowHeight
        }
        itemView.findViewById<View>(R.id.layouts_row_separator_line).isVisible = showRowDividers
        itemView.findViewById<RecyclerView>(R.id.layouts_recycler_view).apply {
            layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.HORIZONTAL,
                    false
            ).apply { initialPrefetchItemCount = prefetchItemCount }
            setRecycledViewPool(RecyclerView.RecycledViewPool())
            adapter = LayoutsAdapter(parent.context, dimensionProvider)

            addOnScrollListener(object : OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        currentItem?.let { saveScrollState(recyclerView, it.title) }
                    }
                }
            })
        }
    }

    fun bind(category: LayoutCategoryUiState) {
        currentItem = category

        title.text = category.description
        subtitle.setVisible(category.isRecommended)
        (recycler.adapter as LayoutsAdapter).setData(category.layouts)
        restoreScrollState(recycler, category.title)
    }

    fun onRecycled() {
        currentItem?.let { saveScrollState(recycler, it.title) }
        currentItem = null
    }

    private fun saveScrollState(recyclerView: RecyclerView, key: String) {
        recyclerView.layoutManager?.onSaveInstanceState()?.let { nestedScrollStates.putParcelable(key, it) }
    }

    private fun restoreScrollState(recyclerView: RecyclerView, key: String) {
        recyclerView.layoutManager?.apply {
            val scrollState = nestedScrollStates.getParcelable<Parcelable>(key)
            if (scrollState != null) {
                onRestoreInstanceState(scrollState)
            } else {
                scrollToPosition(0)
            }
        }
    }
}
