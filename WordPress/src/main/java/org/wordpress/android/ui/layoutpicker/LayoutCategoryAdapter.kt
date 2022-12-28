package org.wordpress.android.ui.layoutpicker

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.layoutpicker.LayoutCategoryViewType.DEFAULT
import org.wordpress.android.ui.layoutpicker.LayoutCategoryViewType.FOOTER
import org.wordpress.android.ui.layoutpicker.LayoutCategoryViewType.RECOMMENDED

enum class LayoutCategoryViewType {
    DEFAULT,
    RECOMMENDED,
    FOOTER,
}

/**
 * Renders the layout categories
 */
class LayoutCategoryAdapter(
    private var nestedScrollStates: Bundle,
    private val thumbDimensionProvider: ThumbDimensionProvider,
    private val recommendedDimensionProvider: ThumbDimensionProvider? = null,
    private val showRowDividers: Boolean = true,
    private val useLargeCategoryHeading: Boolean = false,
    private val footerLayoutResId: Int? = null
) : Adapter<LayoutsRowViewHolder>() {
    private var items: List<LayoutCategoryUiState> = listOf()
    private val shouldShowFooter get() = footerLayoutResId != null && items.isNotEmpty()

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

    override fun getItemCount(): Int = items.size + if (shouldShowFooter) 1 else 0

    override fun onBindViewHolder(holder: LayoutsRowViewHolder, position: Int) {
        (holder as? LayoutsItemViewHolder)?.bind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            items.size <= position -> FOOTER.ordinal
            items[position].isRecommended -> RECOMMENDED.ordinal
            else -> DEFAULT.ordinal
        }
    }

    override fun onViewRecycled(holder: LayoutsRowViewHolder) {
        super.onViewRecycled(holder)
        (holder as? LayoutsItemViewHolder)?.onRecycled()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            FOOTER.ordinal -> LayoutsFooterViewHolder(parent, footerLayoutResId!!)
            else ->
                LayoutsItemViewHolder(
                    parent = parent,
                    nestedScrollStates = nestedScrollStates,
                    thumbDimensionProvider = thumbDimensionProvider,
                    recommendedDimensionProvider = recommendedDimensionProvider,
                    showRowDividers = showRowDividers,
                    useLargeCategoryHeading = useLargeCategoryHeading
                )
        }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        nestedScrollStates = savedInstanceState
    }

    fun onSaveInstanceState(): Bundle {
        return nestedScrollStates
    }
}
