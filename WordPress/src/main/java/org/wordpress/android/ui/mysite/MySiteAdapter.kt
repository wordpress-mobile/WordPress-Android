package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationViewHolder
import org.wordpress.android.ui.mysite.cards.post.PostCardViewHolder
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsViewHolder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardViewHolder
import org.wordpress.android.ui.mysite.cards.siteinfo.MySiteInfoViewHolder
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartDynamicCardViewHolder
import org.wordpress.android.ui.mysite.items.categoryheader.MySiteCategoryItemViewHolder
import org.wordpress.android.ui.mysite.items.listitem.MySiteListItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class MySiteAdapter(
    val imageManager: ImageManager,
    val uiHelpers: UiHelpers
) : Adapter<MySiteCardAndItemViewHolder<*>>() {
    private var items = listOf<MySiteCardAndItem>()
    private val quickStartViewPool = RecycledViewPool()
    private var nestedScrollStates = Bundle()

    fun loadData(result: List<MySiteCardAndItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MySiteAdapterDiffCallback(items, result)
        )
        items = result
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteCardAndItemViewHolder<*> {
        return when (viewType) {
            MySiteCardAndItem.Type.SITE_INFO_CARD.ordinal -> MySiteInfoViewHolder(parent, imageManager)
            MySiteCardAndItem.Type.QUICK_ACTIONS_CARD.ordinal -> QuickActionsViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD.ordinal -> DomainRegistrationViewHolder(parent)
            MySiteCardAndItem.Type.QUICK_START_CARD.ordinal -> QuickStartCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.QUICK_START_DYNAMIC_CARD.ordinal -> QuickStartDynamicCardViewHolder(
                    parent,
                    quickStartViewPool,
                    nestedScrollStates,
                    uiHelpers
            )
            MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM.ordinal -> MySiteCategoryItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.LIST_ITEM.ordinal -> MySiteListItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.POST_CARD_WITHOUT_POST_ITEMS.ordinal ->
                PostCardViewHolder.PostCardWithoutPostItemsViewHolder(parent, imageManager, uiHelpers)
            MySiteCardAndItem.Type.POST_CARD_WITH_POST_ITEMS.ordinal ->
                PostCardViewHolder.PostCardWithPostItemsViewHolder(parent, imageManager, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteCardAndItemViewHolder<*>, position: Int) {
        when (holder) {
            is MySiteInfoViewHolder -> holder.bind(items[position] as SiteInfoCard)
            is QuickActionsViewHolder -> holder.bind(items[position] as QuickActionsCard)
            is DomainRegistrationViewHolder -> holder.bind(items[position] as DomainRegistrationCard)
            is QuickStartCardViewHolder -> holder.bind(items[position] as QuickStartCard)
            is QuickStartDynamicCardViewHolder -> holder.bind(items[position] as QuickStartDynamicCard)
            is MySiteCategoryItemViewHolder -> holder.bind(items[position] as CategoryHeaderItem)
            is MySiteListItemViewHolder -> holder.bind(items[position] as ListItem)
            is PostCardViewHolder -> holder.bind(items[position] as PostCard)
        }
    }

    override fun onViewRecycled(holder: MySiteCardAndItemViewHolder<*>) {
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
