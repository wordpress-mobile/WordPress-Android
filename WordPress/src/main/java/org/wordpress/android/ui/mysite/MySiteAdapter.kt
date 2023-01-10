package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.cards.dashboard.CardsViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationViewHolder
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardViewHolder
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsViewHolder
import org.wordpress.android.ui.mysite.cards.quicklinksribbon.QuickLinkRibbonViewHolder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardViewHolder
import org.wordpress.android.ui.mysite.dynamiccards.quickstart.QuickStartDynamicCardViewHolder
import org.wordpress.android.ui.mysite.items.categoryheader.MySiteCategoryItemViewHolder
import org.wordpress.android.ui.mysite.items.infoitem.MySiteInfoItemViewHolder
import org.wordpress.android.ui.mysite.items.listitem.MySiteListItemViewHolder
import org.wordpress.android.ui.mysite.items.singleactioncard.SingleActionCardViewHolder
import org.wordpress.android.ui.mysite.jetpackbadge.MySiteJetpackBadgeViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.image.ImageManager

class MySiteAdapter(
    val imageManager: ImageManager,
    val uiHelpers: UiHelpers,
    val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    val htmlCompatWrapper: HtmlCompatWrapper,
    val learnMoreClicked: () -> Unit
) : ListAdapter<MySiteCardAndItem, MySiteCardAndItemViewHolder<*>>(MySiteAdapterDiffCallback) {
    private val quickStartViewPool = RecycledViewPool()
    private var nestedScrollStates = Bundle()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteCardAndItemViewHolder<*> {
        return when (viewType) {
            MySiteCardAndItem.Type.QUICK_ACTIONS_CARD.ordinal -> QuickActionsViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.QUICK_LINK_RIBBON.ordinal -> QuickLinkRibbonViewHolder(parent)
            MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD.ordinal -> DomainRegistrationViewHolder(parent)
            MySiteCardAndItem.Type.QUICK_START_CARD.ordinal -> QuickStartCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.QUICK_START_DYNAMIC_CARD.ordinal -> QuickStartDynamicCardViewHolder(
                parent,
                quickStartViewPool,
                nestedScrollStates,
                uiHelpers
            )
            MySiteCardAndItem.Type.INFO_ITEM.ordinal -> MySiteInfoItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM.ordinal -> MySiteCategoryItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.LIST_ITEM.ordinal -> MySiteListItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.DASHBOARD_CARDS.ordinal -> CardsViewHolder(
                parent,
                imageManager,
                uiHelpers,
                bloggingPromptsCardAnalyticsTracker,
                htmlCompatWrapper,
                learnMoreClicked
            )
            MySiteCardAndItem.Type.JETPACK_BADGE.ordinal -> MySiteJetpackBadgeViewHolder(parent)
            MySiteCardAndItem.Type.SINGLE_ACTION_CARD.ordinal -> SingleActionCardViewHolder(parent)
            MySiteCardAndItem.Type.JETPACK_FEATURE_CARD.ordinal -> JetpackFeatureCardViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: MySiteCardAndItemViewHolder<*>, position: Int) {
        when (holder) {
            is QuickActionsViewHolder -> holder.bind(getItem(position) as QuickActionsCard)
            is QuickLinkRibbonViewHolder -> holder.bind(getItem(position) as QuickLinkRibbon)
            is DomainRegistrationViewHolder -> holder.bind(getItem(position) as DomainRegistrationCard)
            is QuickStartCardViewHolder -> holder.bind(getItem(position) as QuickStartCard)
            is QuickStartDynamicCardViewHolder -> holder.bind(getItem(position) as QuickStartDynamicCard)
            is MySiteInfoItemViewHolder -> holder.bind(getItem(position) as InfoItem)
            is MySiteCategoryItemViewHolder -> holder.bind(getItem(position) as CategoryHeaderItem)
            is MySiteListItemViewHolder -> holder.bind(getItem(position) as ListItem)
            is MySiteJetpackBadgeViewHolder -> holder.bind(getItem(position) as JetpackBadge)
            is CardsViewHolder -> holder.bind(getItem(position) as DashboardCards)
            is SingleActionCardViewHolder -> holder.bind(getItem(position) as SingleActionCard)
            is JetpackFeatureCardViewHolder -> holder.bind(getItem(position) as JetpackFeatureCard)
        }
    }

    override fun onViewRecycled(holder: MySiteCardAndItemViewHolder<*>) {
        super.onViewRecycled(holder)
        if (holder is QuickStartDynamicCardViewHolder) {
            holder.onRecycled()
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).type.ordinal

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        nestedScrollStates = savedInstanceState
    }

    fun onSaveInstanceState(): Bundle {
        return nestedScrollStates
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}
