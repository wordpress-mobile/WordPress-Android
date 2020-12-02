package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.utils.UiHelpers

class MySiteCategoryViewHolder(parent: ViewGroup, private val uiHelpers: UiHelpers) : MySiteItemViewHolder(
        parent,
        R.layout.my_site_category_header_block
) {
    private val category = itemView.findViewById<TextView>(R.id.category)
    fun bind(item: CategoryHeader) {
        uiHelpers.setTextOrHide(category, item.title)
    }
}
