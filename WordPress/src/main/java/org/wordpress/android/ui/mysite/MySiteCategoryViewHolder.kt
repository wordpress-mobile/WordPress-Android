package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCategoryHeaderBlockBinding
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class MySiteCategoryViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteItemViewHolder<MySiteCategoryHeaderBlockBinding>(
        parent.viewBinding(MySiteCategoryHeaderBlockBinding::inflate)
) {
    fun bind(item: CategoryHeader) = with(binding) {
        uiHelpers.setTextOrHide(category, item.title)
    }
}
