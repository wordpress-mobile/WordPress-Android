package org.wordpress.android.ui.mysite.items.categoryheader

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCategoryHeaderItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class MySiteCategoryItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySiteCategoryHeaderItemBinding>(
    parent.viewBinding(MySiteCategoryHeaderItemBinding::inflate)
) {
    fun bind(item: CategoryHeaderItem) = with(binding) {
        uiHelpers.setTextOrHide(category, item.title)
    }
}
