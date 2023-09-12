package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackSwitchMenu
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PersonalizeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.cards.dashboard.CardsViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationViewHolder
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardViewHolder
import org.wordpress.android.ui.mysite.cards.jetpackfeature.SwitchToJetpackMenuCardViewHolder
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardViewHolder
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardViewHolder
import org.wordpress.android.ui.mysite.cards.quicklinksribbon.QuickLinkRibbonViewHolder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardViewHolder
import org.wordpress.android.ui.mysite.items.categoryheader.MySiteCategoryItemEmptyViewHolder
import org.wordpress.android.ui.mysite.items.categoryheader.MySiteCategoryItemViewHolder
import org.wordpress.android.ui.mysite.items.infoitem.MySiteInfoItemViewHolder
import org.wordpress.android.ui.mysite.items.listitem.MySiteListItemViewHolder
import org.wordpress.android.ui.mysite.items.singleactioncard.SingleActionCardViewHolder
import org.wordpress.android.ui.mysite.jetpackbadge.MySiteJetpackBadgeViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.image.ImageManager

@Suppress("LongParameterList")
class MySiteAdapter(
    val imageManager: ImageManager,
    val uiHelpers: UiHelpers,
    val accountStore: AccountStore,
    val gravatarLoader: MeGravatarLoader,
    val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    val htmlCompatWrapper: HtmlCompatWrapper,
    val learnMoreClicked: () -> Unit
) : ListAdapter<MySiteCardAndItem, MySiteCardAndItemViewHolder<*>>(MySiteAdapterDiffCallback) {
    private var nestedScrollStates = Bundle()

    @Suppress("ComplexMethod")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteCardAndItemViewHolder<*> {
        return when (viewType) {
            MySiteCardAndItem.Type.QUICK_LINK_RIBBON.ordinal -> QuickLinkRibbonViewHolder(parent)
            MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD.ordinal -> DomainRegistrationViewHolder(parent)
            MySiteCardAndItem.Type.QUICK_START_CARD.ordinal -> QuickStartCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.INFO_ITEM.ordinal -> MySiteInfoItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM.ordinal -> MySiteCategoryItemViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.CATEGORY_EMPTY_HEADER_ITEM.ordinal -> {
                MySiteCategoryItemEmptyViewHolder(parent, uiHelpers)
            }
            MySiteCardAndItem.Type.LIST_ITEM.ordinal -> MySiteListItemViewHolder(
                parent,
                uiHelpers,
                accountStore,
                gravatarLoader
            )
            MySiteCardAndItem.Type.DASHBOARD_CARDS.ordinal -> CardsViewHolder(
                parent,
                imageManager,
                uiHelpers,
                bloggingPromptsCardAnalyticsTracker,
                htmlCompatWrapper,
                learnMoreClicked
            )
            MySiteCardAndItem.Type.JETPACK_BADGE.ordinal -> MySiteJetpackBadgeViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.SINGLE_ACTION_CARD.ordinal -> SingleActionCardViewHolder(parent)
            MySiteCardAndItem.Type.JETPACK_FEATURE_CARD.ordinal -> JetpackFeatureCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.JETPACK_SWITCH_CARD.ordinal -> SwitchToJetpackMenuCardViewHolder(parent)
            MySiteCardAndItem.Type.JETPACK_INSTALL_FULL_PLUGIN_CARD.ordinal -> JetpackInstallFullPluginCardViewHolder(
                parent
            )
            MySiteCardAndItem.Type.PERSONALIZE_CARD.ordinal -> PersonalizeCardViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    @Suppress("ComplexMethod")
    override fun onBindViewHolder(holder: MySiteCardAndItemViewHolder<*>, position: Int) {
        when (holder) {
            is QuickLinkRibbonViewHolder -> holder.bind(getItem(position) as QuickLinkRibbon)
            is DomainRegistrationViewHolder -> holder.bind(getItem(position) as DomainRegistrationCard)
            is QuickStartCardViewHolder -> holder.bind(getItem(position) as QuickStartCard)
            is MySiteInfoItemViewHolder -> holder.bind(getItem(position) as InfoItem)
            is MySiteCategoryItemViewHolder -> holder.bind(getItem(position) as CategoryHeaderItem)
            is MySiteCategoryItemEmptyViewHolder -> holder.bind(getItem(position) as CategoryEmptyHeaderItem)
            is MySiteListItemViewHolder -> holder.bind(getItem(position) as ListItem)
            is MySiteJetpackBadgeViewHolder -> holder.bind(getItem(position) as JetpackBadge)
            is CardsViewHolder -> holder.bind(getItem(position) as DashboardCards)
            is SingleActionCardViewHolder -> holder.bind(getItem(position) as SingleActionCard)
            is JetpackFeatureCardViewHolder -> holder.bind(getItem(position) as JetpackFeatureCard)
            is SwitchToJetpackMenuCardViewHolder -> holder.bind(getItem(position) as JetpackSwitchMenu)
            is JetpackInstallFullPluginCardViewHolder -> holder.bind(getItem(position) as JetpackInstallFullPluginCard)
            is PersonalizeCardViewHolder -> holder.bind(getItem(position) as PersonalizeCardModel)
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
