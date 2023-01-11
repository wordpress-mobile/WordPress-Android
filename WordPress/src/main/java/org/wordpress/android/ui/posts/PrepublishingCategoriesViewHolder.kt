package org.wordpress.android.ui.posts

import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.databinding.PrepublishingCategoriesRowBinding
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel.PrepublishingCategoriesListItemUiState
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

sealed class PrepublishingCategoriesViewHolder<T : ViewBinding>(
    internal val binding: T
) : RecyclerView.ViewHolder(binding.root) {
    abstract fun onBind(uiState: PrepublishingCategoriesListItemUiState, position: Int)

    class PrepublishingCategoriesListItemViewHolder(
        parentView: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : PrepublishingCategoriesViewHolder<PrepublishingCategoriesRowBinding>(
        parentView.viewBinding(PrepublishingCategoriesRowBinding::inflate)
    ) {
        override fun onBind(uiState: PrepublishingCategoriesListItemUiState, position: Int) = with(binding) {
            prepublishingCategoryRowLayout.setOnClickListener {
                uiState.onItemTapped(position)
            }
            prepublishingCategoryText.text = StringEscapeUtils.unescapeHtml4(uiState.categoryNode.name)
            prepublishingCategoryCheck.isChecked = uiState.checked
            prepublishingCategoryCheck.setOnClickListener {
                uiState.onItemTapped(position)
            }
            val verticalPadding: Int = uiHelpers.getPxOfUiDimen(
                prepublishingCategoryText.context,
                UIDimenRes(uiState.verticalPaddingResId)
            )
            val horizontalPadding: Int = uiHelpers.getPxOfUiDimen(
                prepublishingCategoryText.context,
                UIDimenRes(uiState.horizontalPaddingResId)
            )
            ViewCompat.setPaddingRelative(
                prepublishingCategoryText,
                horizontalPadding * uiState.categoryNode.level,
                verticalPadding,
                horizontalPadding,
                verticalPadding
            )
        }
    }
}
