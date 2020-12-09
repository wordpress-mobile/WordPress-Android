package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.Type.CATEGORY_HEADER
import org.wordpress.android.ui.mysite.MySiteItem.Type.DOMAIN_REGISTRATION_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class MySiteAdapter(val imageManager: ImageManager, val uiHelpers: UiHelpers) : Adapter<MySiteItemViewHolder>() {
    private var items = listOf<MySiteItem>()
    fun loadData(result: List<MySiteItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MySiteAdapterDiffCallback(items, result)
        )
        items = result
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteItemViewHolder {
        return when (viewType) {
            SITE_INFO_BLOCK.ordinal -> MySiteInfoViewHolder(parent, imageManager)
            QUICK_ACTIONS_BLOCK.ordinal -> QuickActionsViewHolder(parent)
            DOMAIN_REGISTRATION_BLOCK.ordinal -> DomainRegistrationViewHolder(parent)
            CATEGORY_HEADER.ordinal -> MySiteCategoryViewHolder(parent, uiHelpers)
            LIST_ITEM.ordinal -> MySiteListItemViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteItemViewHolder, position: Int) {
        when (holder) {
            is MySiteInfoViewHolder -> holder.bind(items[position] as SiteInfoBlock)
            is QuickActionsViewHolder -> holder.bind(items[position] as QuickActionsBlock)
            is DomainRegistrationViewHolder -> holder.bind(items[position] as DomainRegistrationBlock)
            is MySiteCategoryViewHolder -> holder.bind(items[position] as CategoryHeader)
            is MySiteListItemViewHolder -> holder.bind(items[position] as ListItem)
        }
    }

    override fun getItemViewType(position: Int) = items[position].type.ordinal

    override fun getItemCount(): Int = items.size
}
