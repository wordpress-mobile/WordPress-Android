package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ActivityCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardPlansCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.Dynamic
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ErrorWithinCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackSwitchMenu
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PersonalizeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCampaignsCardViewHolder
import org.wordpress.android.ui.mysite.cards.blaze.PromoteWithBlazeCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloganuary.BloganuaryNudgeCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.error.ErrorCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.error.ErrorWithinCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardViewHolder
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationViewHolder
import org.wordpress.android.ui.mysite.cards.dynamiccard.DynamicDashboardCardViewHolder
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardViewHolder
import org.wordpress.android.ui.mysite.cards.jetpackfeature.SwitchToJetpackMenuCardViewHolder
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardViewHolder
import org.wordpress.android.ui.mysite.cards.nocards.NoCardsMessageViewHolder
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardViewHolder
import org.wordpress.android.ui.mysite.cards.quicklinksitem.QuickLinkRibbonViewHolder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardViewHolder
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardViewHolder
import org.wordpress.android.ui.mysite.cards.sotw2023.WpSotw2023NudgeCardViewHolder
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
    val learnMoreClicked: () -> Unit,
    val containerClicked: () -> Unit
) : ListAdapter<MySiteCardAndItem, MySiteCardAndItemViewHolder<*>>(MySiteAdapterDiffCallback) {
    private var nestedScrollStates = Bundle()

    @Suppress("ComplexMethod")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MySiteCardAndItemViewHolder<*> {
        return when (viewType) {
            MySiteCardAndItem.Type.SITE_INFO_CARD.ordinal -> SiteInfoHeaderCardViewHolder(parent, imageManager)
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

            MySiteCardAndItem.Type.ERROR_CARD.ordinal -> ErrorCardViewHolder(parent)
            MySiteCardAndItem.Type.TODAYS_STATS_CARD_ERROR.ordinal,
            MySiteCardAndItem.Type.POST_CARD_ERROR.ordinal -> ErrorWithinCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.TODAYS_STATS_CARD.ordinal -> TodaysStatsCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.POST_CARD_WITH_POST_ITEMS.ordinal ->
                PostCardViewHolder.PostCardWithPostItemsViewHolder(parent, imageManager, uiHelpers)
            MySiteCardAndItem.Type.BLOGGING_PROMPT_CARD.ordinal -> BloggingPromptCardViewHolder(
                parent,
                uiHelpers,
                bloggingPromptsCardAnalyticsTracker,
                htmlCompatWrapper,
                learnMoreClicked,
                containerClicked,
            )

            MySiteCardAndItem.Type.BLOGANUARY_NUDGE_CARD.ordinal -> BloganuaryNudgeCardViewHolder(parent)
            MySiteCardAndItem.Type.PROMOTE_WITH_BLAZE_CARD.ordinal -> PromoteWithBlazeCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.BLAZE_CAMPAIGNS_CARD.ordinal -> BlazeCampaignsCardViewHolder(parent)
            MySiteCardAndItem.Type.DASHBOARD_PLANS_CARD.ordinal -> PlansCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.PAGES_CARD.ordinal -> PagesCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.ACTIVITY_CARD.ordinal -> ActivityCardViewHolder(parent, uiHelpers)

            MySiteCardAndItem.Type.JETPACK_BADGE.ordinal -> MySiteJetpackBadgeViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.SINGLE_ACTION_CARD.ordinal -> SingleActionCardViewHolder(parent)
            MySiteCardAndItem.Type.JETPACK_FEATURE_CARD.ordinal -> JetpackFeatureCardViewHolder(parent, uiHelpers)
            MySiteCardAndItem.Type.JETPACK_SWITCH_CARD.ordinal -> SwitchToJetpackMenuCardViewHolder(parent)
            MySiteCardAndItem.Type.JETPACK_INSTALL_FULL_PLUGIN_CARD.ordinal -> JetpackInstallFullPluginCardViewHolder(
                parent
            )
            MySiteCardAndItem.Type.NO_CARDS_MESSAGE.ordinal -> NoCardsMessageViewHolder(parent)
            MySiteCardAndItem.Type.PERSONALIZE_CARD.ordinal -> PersonalizeCardViewHolder(parent)
            MySiteCardAndItem.Type.WP_SOTW_2023_NUDGE_CARD.ordinal -> WpSotw2023NudgeCardViewHolder(parent)
            MySiteCardAndItem.Type.DYNAMIC_DASHBOARD_CARD.ordinal -> DynamicDashboardCardViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    @Suppress("ComplexMethod")
    override fun onBindViewHolder(holder: MySiteCardAndItemViewHolder<*>, position: Int) {
        when (holder) {
            is SiteInfoHeaderCardViewHolder -> holder.bind(getItem(position) as SiteInfoHeaderCard)
            is QuickLinkRibbonViewHolder -> holder.bind(getItem(position) as QuickLinksItem)
            is DomainRegistrationViewHolder -> holder.bind(getItem(position) as DomainRegistrationCard)
            is QuickStartCardViewHolder -> holder.bind(getItem(position) as QuickStartCard)
            is MySiteInfoItemViewHolder -> holder.bind(getItem(position) as InfoItem)
            is MySiteCategoryItemViewHolder -> holder.bind(getItem(position) as CategoryHeaderItem)
            is MySiteCategoryItemEmptyViewHolder -> holder.bind(getItem(position) as CategoryEmptyHeaderItem)
            is MySiteListItemViewHolder -> holder.bind(getItem(position) as ListItem)
            is MySiteJetpackBadgeViewHolder -> holder.bind(getItem(position) as JetpackBadge)
            is ErrorCardViewHolder -> holder.bind(getItem(position)  as ErrorCard)
            is ErrorWithinCardViewHolder -> holder.bind(getItem(position)  as ErrorWithinCard)
            is TodaysStatsCardViewHolder -> holder.bind(getItem(position)  as TodaysStatsCardWithData)
            is PostCardViewHolder<*> -> holder.bind(getItem(position)  as PostCard)
            is BloggingPromptCardViewHolder -> holder.bind(getItem(position)  as BloggingPromptCardWithData)
            is BloganuaryNudgeCardViewHolder -> holder.bind(getItem(position)  as BloganuaryNudgeCardModel)
            is PromoteWithBlazeCardViewHolder -> holder.bind(getItem(position)  as PromoteWithBlazeCard)
            is BlazeCampaignsCardViewHolder -> holder.bind(getItem(position)  as BlazeCampaignsCardModel)
            is PlansCardViewHolder -> holder.bind(getItem(position)  as DashboardPlansCard)
            is PagesCardViewHolder -> holder.bind(getItem(position)  as PagesCard)
            is ActivityCardViewHolder -> holder.bind(getItem(position)  as ActivityCard)
            is SingleActionCardViewHolder -> holder.bind(getItem(position) as SingleActionCard)
            is JetpackFeatureCardViewHolder -> holder.bind(getItem(position) as JetpackFeatureCard)
            is SwitchToJetpackMenuCardViewHolder -> holder.bind(getItem(position) as JetpackSwitchMenu)
            is JetpackInstallFullPluginCardViewHolder -> holder.bind(getItem(position) as JetpackInstallFullPluginCard)
            is NoCardsMessageViewHolder -> holder.bind(getItem(position) as MySiteCardAndItem.Card.NoCardsMessage)
            is PersonalizeCardViewHolder -> holder.bind(getItem(position) as PersonalizeCardModel)
            is WpSotw2023NudgeCardViewHolder -> holder.bind(getItem(position) as WpSotw2023NudgeCardModel)
            is DynamicDashboardCardViewHolder -> holder.bind(getItem(position) as Dynamic)
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
