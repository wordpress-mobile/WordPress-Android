package org.wordpress.android.ui.mysite.items.categoryheader

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteCategoryEmptyHeaderItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class MySiteCategoryItemEmptyViewHolder (
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<MySiteCategoryEmptyHeaderItemBinding>(
    parent.viewBinding(MySiteCategoryEmptyHeaderItemBinding::inflate)
) {
    fun bind(item: CategoryEmptyHeaderItem) = with(binding) {
        uiHelpers.setTextOrHide(emptyCategory, item.title)
    }
}
