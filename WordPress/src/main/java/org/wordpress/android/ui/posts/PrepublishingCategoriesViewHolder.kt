package org.wordpress.android.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.prepublishing_categories_row.view.*
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel.PrepublishingCategoriesListItemUiState
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiHelpers

sealed class PrepublishingCategoriesViewHolder(
    internal val parent: ViewGroup,
    @LayoutRes layout: Int
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: PrepublishingCategoriesListItemUiState)

    class PrepublishingCategoriesListItemViewHolder(
        parentView: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : PrepublishingCategoriesViewHolder(
            parentView,
            R.layout.prepublishing_categories_row
    ) {
        private val container = itemView.prepublishing_category_row_layout
        private val categoryName = itemView.prepublishing_category_text
        private val categoryCheckBox = itemView.prepublishing_category_check
        private var onCategorySelected: ((Int) -> Unit)? = null

        init {
            container.setOnClickListener {
                onCategorySelected?.invoke(adapterPosition)
            }
            categoryCheckBox.setOnClickListener {
                onCategorySelected?.invoke(adapterPosition)
            }
        }

        override fun onBind(uiState: PrepublishingCategoriesListItemUiState) {
            onCategorySelected = requireNotNull(uiState.onItemTapped) { "OnItemTapped is required" }
            categoryName.text = StringEscapeUtils.unescapeHtml4(uiState.categoryNode.name)
            categoryCheckBox.isChecked = uiState.checked
            val verticalPadding: Int = uiHelpers.getPxOfUiDimen(
                    categoryName.context,
                    UIDimenRes(uiState.verticalPaddingResId)
            )
            val horizontalPadding: Int = uiHelpers.getPxOfUiDimen(
                    categoryName.context,
                    UIDimenRes(uiState.horizontalPaddingResId)
            )
            ViewCompat.setPaddingRelative(
                    categoryName,
                    horizontalPadding * uiState.categoryNode.level,
                    verticalPadding,
                    horizontalPadding,
                    verticalPadding
            )
        }
    }
}
