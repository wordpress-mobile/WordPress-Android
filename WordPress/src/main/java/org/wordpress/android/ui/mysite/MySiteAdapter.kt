package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteItem.Type.CATEGORY_HEADER_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.DOMAIN_REGISTRATION_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_ACTIONS_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.QUICK_START_DYNAMIC_CARD
import org.wordpress.android.ui.mysite.MySiteItem.Type.SITE_INFO_CARD
import org.wordpress.android.ui.mysite.quickactions.QuickActionsViewHolder
import org.wordpress.android.ui.mysite.quickstart.QuickStartCardViewHolder
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
            SITE_INFO_CARD.ordinal -> MySiteInfoViewHolder(parent, imageManager)
            QUICK_ACTIONS_CARD.ordinal -> QuickActionsViewHolder(parent, uiHelpers)
            DOMAIN_REGISTRATION_CARD.ordinal -> DomainRegistrationViewHolder(parent)
            QUICK_START_CARD.ordinal -> QuickStartCardViewHolder(parent, uiHelpers)
            QUICK_START_DYNAMIC_CARD.ordinal -> QuickStartDynamicCardViewHolder(
                    parent,
                    quickStartViewPool,
                    nestedScrollStates,
                    uiHelpers
            )
            CATEGORY_HEADER_ITEM.ordinal -> MySiteCategoryItemViewHolder(parent, uiHelpers)
            LIST_ITEM.ordinal -> MySiteListItemViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteItemViewHolder<*>, position: Int) {
        when (holder) {
            is MySiteInfoViewHolder -> holder.bind(items[position] as SiteInfoCard)
            is QuickActionsViewHolder -> holder.bind(items[position] as QuickActionsCard)
            is DomainRegistrationViewHolder -> holder.bind(items[position] as DomainRegistrationCard)
            is QuickStartCardViewHolder -> holder.bind(items[position] as QuickStartCard)
            is QuickStartDynamicCardViewHolder -> holder.bind(items[position] as QuickStartDynamicCard)
            is MySiteCategoryItemViewHolder -> holder.bind(items[position] as CategoryHeaderItem)
            is MySiteListItemViewHolder -> holder.bind(items[position] as ListItem)
        }
    }

    override fun onViewRecycled(holder: MySiteItemViewHolder<*>) {
        super.onViewRecycled(holder)
        if (holder is QuickStartDynamicCardViewHolder) {
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
