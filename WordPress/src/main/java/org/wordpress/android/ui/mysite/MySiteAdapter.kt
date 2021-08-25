package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.Type.CATEGORY_HEADER
import org.wordpress.android.ui.mysite.MySiteItem.Type.DOMAIN_REGISTRATION_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_BLOCK
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_DYNAMIC_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_BLOCK
import org.wordpress.android.ui.mysite.quickactions.QuickActionsViewHolder
import org.wordpress.android.ui.mysite.quickstart.QuickStartBlockViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class MySiteAdapter(val imageManager: ImageManager, val uiHelpers: UiHelpers) : Adapter<MySiteItemViewHolder<*>>() {
    private var items = listOf<MySiteItem>()
    private val quickStartViewPool = RecycledViewPool()
    private var nestedScrollStates = Bundle()

    fun loadData(result: List<MySiteItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MySiteAdapterDiffCallback(items, result)
        )
        items = result
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteItemViewHolder<*> {
        return when (viewType) {
            SITE_INFO_BLOCK.ordinal -> MySiteInfoViewHolder(parent, imageManager)
            QUICK_ACTIONS_BLOCK.ordinal -> QuickActionsViewHolder(parent, uiHelpers)
            DOMAIN_REGISTRATION_BLOCK.ordinal -> DomainRegistrationViewHolder(parent)
            QUICK_START_BLOCK.ordinal -> QuickStartBlockViewHolder(parent, uiHelpers)
            QUICK_START_DYNAMIC_CARD.ordinal -> QuickStartCardViewHolder(
                    parent,
                    quickStartViewPool,
                    nestedScrollStates,
                    uiHelpers
            )
            CATEGORY_HEADER.ordinal -> MySiteCategoryViewHolder(parent, uiHelpers)
            LIST_ITEM.ordinal -> MySiteListItemViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteItemViewHolder<*>, position: Int) {
        when (holder) {
            is MySiteInfoViewHolder -> holder.bind(items[position] as SiteInfoBlock)
            is QuickActionsViewHolder -> holder.bind(items[position] as QuickActionsBlock)
            is DomainRegistrationViewHolder -> holder.bind(items[position] as DomainRegistrationBlock)
            is QuickStartBlockViewHolder -> holder.bind(items[position] as QuickStartBlock)
            is QuickStartCardViewHolder -> holder.bind(items[position] as QuickStartCard)
            is MySiteCategoryViewHolder -> holder.bind(items[position] as CategoryHeader)
            is MySiteListItemViewHolder -> holder.bind(items[position] as ListItem)
        }
    }

    override fun onViewRecycled(holder: MySiteItemViewHolder<*>) {
        super.onViewRecycled(holder)
        if (holder is QuickStartCardViewHolder) {
            holder.onRecycled()
        }
    }

    override fun getItemViewType(position: Int) = items[position].type.ordinal

    override fun getItemCount(): Int = items.size

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        nestedScrollStates = savedInstanceState
    }

    fun onSaveInstanceState(): Bundle {
        return nestedScrollStates
    }
}
