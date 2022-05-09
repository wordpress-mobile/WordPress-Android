package org.wordpress.android.ui.layoutpicker

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.layoutpicker.LayoutCategoryViewType.DEFAULT
import org.wordpress.android.ui.layoutpicker.LayoutCategoryViewType.RECOMMENDED

enum class LayoutCategoryViewType {
    DEFAULT,
    RECOMMENDED,
}

/**
 * Renders the layout categories
 */
class LayoutCategoryAdapter(
    private var nestedScrollStates: Bundle,
    private val thumbDimensionProvider: ThumbDimensionProvider,
    private val recommendedDimensionProvider: ThumbDimensionProvider? = null,
    private val showRowDividers: Boolean = true
) : Adapter<LayoutsItemViewHolder>() {
    private var items: List<LayoutCategoryUiState> = listOf()

    fun update(newItems: List<LayoutCategoryUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                LayoutCategoryDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LayoutsItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isRecommended) RECOMMENDED.ordinal else DEFAULT.ordinal
    }

    override fun onViewRecycled(holder: LayoutsItemViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            LayoutsItemViewHolder(
                    parent = parent,
                    nestedScrollStates = nestedScrollStates,
                    thumbDimensionProvider = thumbDimensionProvider,
                    recommendedDimensionProvider = recommendedDimensionProvider,
                    showRowDividers = showRowDividers
            )

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        nestedScrollStates = savedInstanceState
    }

    fun onSaveInstanceState(): Bundle {
        return nestedScrollStates
    }
}
