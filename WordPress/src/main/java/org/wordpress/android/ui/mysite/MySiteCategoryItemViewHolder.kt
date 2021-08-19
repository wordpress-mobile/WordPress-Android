package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCategoryHeaderItemBinding
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeaderItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class MySiteCategoryItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteItemViewHolder<MySiteCategoryHeaderItemBinding>(
        parent.viewBinding(MySiteCategoryHeaderItemBinding::inflate)
) {
    fun bind(item: CategoryHeaderItem) = with(binding) {
        uiHelpers.setTextOrHide(category, item.title)
    }
}
