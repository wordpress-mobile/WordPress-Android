package org.wordpress.android.ui.mysite

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.CATEGORY_EMPTY_HEADER_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DASHBOARD_CARDS
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.INFO_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.JETPACK_BADGE
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.JETPACK_FEATURE_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_LINK_RIBBON
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.SINGLE_ACTION_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.SITE_INFO_CARD
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

typealias PromptID = Int

sealed class MySiteCardAndItem(open val type: Type, open val activeQuickStartItem: Boolean = false) {
    enum class Type {
        SITE_INFO_CARD,
        QUICK_LINK_RIBBON,
        DOMAIN_REGISTRATION_CARD,
        QUICK_START_CARD,
        INFO_ITEM,
        CATEGORY_HEADER_ITEM,
        CATEGORY_EMPTY_HEADER_ITEM,
        LIST_ITEM,
        DASHBOARD_CARDS,
        JETPACK_BADGE,
        SINGLE_ACTION_CARD,
        JETPACK_FEATURE_CARD,
        JETPACK_SWITCH_CARD,
        JETPACK_INSTALL_FULL_PLUGIN_CARD,
    }

    enum class DashboardCardType {
        ERROR_CARD,
        QUICK_START_CARD,
        TODAYS_STATS_CARD_ERROR,
        TODAYS_STATS_CARD,
        POST_CARD_ERROR,
        POST_CARD_WITH_POST_ITEMS,
        BLOGGING_PROMPT_CARD,
        PROMOTE_WITH_BLAZE_CARD,
        DASHBOARD_DOMAIN_TRANSFER_CARD,
        BLAZE_CAMPAIGNS_CARD,
        DASHBOARD_PLANS_CARD,
        PAGES_CARD_ERROR,
        PAGES_CARD,
        ACTIVITY_CARD,
    }

    data class SiteInfoHeaderCard(
        val title: String,
        val url: String,
        val iconState: IconState,
        val showTitleFocusPoint: Boolean,
        val showSubtitleFocusPoint: Boolean,
        val showIconFocusPoint: Boolean,
        val onTitleClick: ListItemInteraction? = null,
        val onIconClick: ListItemInteraction,
        val onUrlClick: ListItemInteraction,
        val onSwitchSiteClick: ListItemInteraction
    ) : MySiteCardAndItem(
        SITE_INFO_CARD,
        activeQuickStartItem = showTitleFocusPoint || showIconFocusPoint || showSubtitleFocusPoint
    ) {
        sealed class IconState {
            object Progress : IconState()
            data class Visible(val url: String? = null) : IconState()
        }
    }

    sealed class Card(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false
    ) : MySiteCardAndItem(type, activeQuickStartItem) {
        data class QuickLinkRibbon(
            val quickLinkRibbonItems: List<QuickLinkRibbonItem>,
            val showPagesFocusPoint: Boolean = false,
            val showStatsFocusPoint: Boolean = false,
            val showMediaFocusPoint: Boolean = false
        ) : Card(
            QUICK_LINK_RIBBON,
            activeQuickStartItem = showPagesFocusPoint || showStatsFocusPoint || showMediaFocusPoint
        ) {
            data class QuickLinkRibbonItem(
                @StringRes val label: Int,
                @DrawableRes val icon: Int,
                val onClick: ListItemInteraction,
                val showFocusPoint: Boolean = false
            )
        }

        data class DomainRegistrationCard(val onClick: ListItemInteraction) : Card(DOMAIN_REGISTRATION_CARD)

        data class QuickStartCard(
            val title: UiString,
            val toolbarVisible: Boolean = true,
            val quickStartCardType: QuickStartCardType,
            val taskTypeItems: List<QuickStartTaskTypeItem>,
            val moreMenuOptions: MoreMenuOptions,
        ) : Card(QUICK_START_CARD) {
            data class QuickStartTaskTypeItem(
                val quickStartTaskType: QuickStartTaskType,
                val title: UiString,
                val titleEnabled: Boolean,
                val subtitle: UiString,
                val strikeThroughTitle: Boolean,
                @ColorRes val progressColor: Int,
                val progress: Int,
                val onClick: ListItemInteraction
            )

            data class MoreMenuOptions(
                val onMoreMenuClick: (type: QuickStartCardType) -> Unit,
                val onHideThisMenuItemClick: (type: QuickStartCardType) -> Unit
            )
        }

        data class JetpackFeatureCard(
            val content: UiString?,
            val onClick: ListItemInteraction,
            val onRemindMeLaterItemClick: ListItemInteraction,
            val onHideMenuItemClick: ListItemInteraction,
            val onLearnMoreClick: ListItemInteraction,
            val onMoreMenuClick: ListItemInteraction,
            val learnMoreUrl: String?,
        ) : Card(JETPACK_FEATURE_CARD)

        data class JetpackSwitchMenu(
            val onClick: ListItemInteraction,
            val onRemindMeLaterItemClick: ListItemInteraction,
            val onHideMenuItemClick: ListItemInteraction,
            val onMoreMenuClick: ListItemInteraction
        ) : Card(Type.JETPACK_SWITCH_CARD)

        data class JetpackInstallFullPluginCard(
            val siteName: String,
            val pluginNames: List<String>,
            val onLearnMoreClick: ListItemInteraction,
            val onHideMenuItemClick: ListItemInteraction,
        ) : Card(Type.JETPACK_INSTALL_FULL_PLUGIN_CARD)

        data class DashboardCards(
            val cards: List<DashboardCard>
        ) : MySiteCardAndItem(DASHBOARD_CARDS) {
            sealed class DashboardCard(
                open val dashboardCardType: DashboardCardType
            ) {
                data class ErrorCard(
                    val onRetryClick: ListItemInteraction
                ) : DashboardCard(dashboardCardType = DashboardCardType.ERROR_CARD)

                interface ErrorWithinCard {
                    val title: UiString
                }

                sealed class TodaysStatsCard(
                    override val dashboardCardType: DashboardCardType
                ) : DashboardCard(dashboardCardType) {
                    data class Error(
                        override val title: UiString
                    ) : TodaysStatsCard(dashboardCardType = DashboardCardType.TODAYS_STATS_CARD_ERROR), ErrorWithinCard

                    data class TodaysStatsCardWithData(
                        val title: UiString,
                        val views: UiString,
                        val visitors: UiString,
                        val likes: UiString,
                        val onCardClick: () -> Unit,
                        val message: TextWithLinks? = null,
                        val moreMenuOptions: MoreMenuOptions
                    ) : TodaysStatsCard(dashboardCardType = DashboardCardType.TODAYS_STATS_CARD)

                    data class TextWithLinks(
                        val text: UiString,
                        val links: List<Clickable>? = null
                    ) {
                        data class Clickable(val navigationAction: ListItemInteraction)
                    }

                    data class MoreMenuOptions(
                        val onMoreMenuClick: () -> Unit,
                        val onViewStatsMenuItemClick: () -> Unit,
                        val onHideThisMenuItemClick: () -> Unit
                    )
                }

                sealed class PagesCard(
                    override val dashboardCardType: DashboardCardType,
                ) : DashboardCard(dashboardCardType) {
                    data class Error(
                        override val title: UiString
                    ) : PagesCard(dashboardCardType = DashboardCardType.PAGES_CARD_ERROR), ErrorWithinCard

                    data class PagesCardWithData(
                        val title: UiString,
                        val pages: List<PageContentItem>,
                        val footerLink: CreateNewPageItem,
                        val moreMenuOptionsLink: MoreMenuOptions
                    ) : PagesCard(dashboardCardType = DashboardCardType.PAGES_CARD) {
                        data class PageContentItem(
                            val title: UiString,
                            @DrawableRes val statusIcon: Int?,
                            val status: UiString?,
                            val lastEditedOrScheduledTime: UiString,
                            val onClick: ListItemInteraction
                        )

                        data class CreateNewPageItem(
                            val label: UiString,
                            val description: UiString? = null,
                            @DrawableRes val imageRes: Int? = null,
                            val onClick: () -> Unit
                        )

                        data class MoreMenuOptions(
                            val onMoreClick: () -> Unit,
                            val allPagesMenuItemClick: () -> Unit,
                            val hideThisMenuItemClick: () -> Unit
                        )
                    }
                }

                sealed class PostCard(
                    override val dashboardCardType: DashboardCardType
                ) : DashboardCard(dashboardCardType) {
                    data class Error(
                        override val title: UiString
                    ) : PostCard(dashboardCardType = DashboardCardType.POST_CARD_ERROR), ErrorWithinCard

                    data class PostCardWithPostItems(
                        val postCardType: PostCardType,
                        val title: UiString,
                        val postItems: List<PostItem>,
                        @MenuRes val moreMenuResId: Int,
                        val moreMenuOptions: MoreMenuOptions
                    ) : PostCard(
                        dashboardCardType = DashboardCardType.POST_CARD_WITH_POST_ITEMS
                    ) {
                        data class PostItem(
                            val title: UiString,
                            val excerpt: UiString?,
                            val featuredImageUrl: String?,
                            val isTimeIconVisible: Boolean = false,
                            val onClick: ListItemInteraction
                        )
                        data class MoreMenuOptions(
                            val onMoreMenuClick: (postCardType: PostCardType) -> Unit,
                            val onViewPostsMenuItemClick: (postCardType: PostCardType) -> Unit,
                            val onHideThisMenuItemClick: (postCardType: PostCardType) -> Unit
                        )
                    }
                }

                sealed class ActivityCard(
                    override val dashboardCardType: DashboardCardType
                ) : DashboardCard(dashboardCardType) {
                    data class ActivityCardWithItems(
                        val title: UiString,
                        val activityItems: List<ActivityItem>,
                        val onAllActivityMenuItemClick: ListItemInteraction,
                        val onHideMenuItemClick: ListItemInteraction,
                        val onMoreMenuClick: ListItemInteraction
                    ) : ActivityCard(
                        dashboardCardType = DashboardCardType.ACTIVITY_CARD
                    ) {
                        data class ActivityItem(
                            val label: UiString,
                            val subLabel: String?,
                            val displayDate: String,
                            @DrawableRes val icon: Int,
                            @DrawableRes val iconBackgroundColor: Int,
                            val onClick: ListItemInteraction
                        )
                    }
                }

                sealed class BloggingPromptCard(
                    override val dashboardCardType: DashboardCardType
                ) : DashboardCard(dashboardCardType) {
                    data class BloggingPromptCardWithData(
                        val prompt: UiString,
                        val respondents: List<TrainOfAvatarsItem>,
                        val numberOfAnswers: Int,
                        val isAnswered: Boolean,
                        val promptId: Int,
                        val attribution: BloggingPromptAttribution,
                        val onShareClick: (String) -> Unit,
                        val onAnswerClick: (PromptID) -> Unit,
                        val onSkipClick: () -> Unit,
                        val onViewMoreClick: () -> Unit,
                        val onViewAnswersClick: ((PromptID) -> Unit)?,
                        val onRemoveClick: () -> Unit,
                    ) : BloggingPromptCard(dashboardCardType = DashboardCardType.BLOGGING_PROMPT_CARD)
                }
                data class DomainTransferCardModel(
                    @StringRes val title: Int,
                    @StringRes val subtitle: Int,
                    @StringRes val caption: Int,
                    @StringRes val cta: Int,
                    val onClick: ListItemInteraction,
                    val onHideMenuItemClick: ListItemInteraction,
                    val onMoreMenuClick: ListItemInteraction,
                ) : DashboardCard(dashboardCardType = DashboardCardType.DASHBOARD_DOMAIN_TRANSFER_CARD)
                sealed class BlazeCard(
                    override val dashboardCardType: DashboardCardType
                ) : DashboardCard(dashboardCardType) {
                    data class BlazeCampaignsCardModel(
                        val title: UiString,
                        val campaign: BlazeCampaignsCardItem,
                        val footer: BlazeCampaignsCardFooter,
                        val onClick: ListItemInteraction,
                        val moreMenuOptions: MoreMenuOptions
                        ) : BlazeCard(dashboardCardType = DashboardCardType.BLAZE_CAMPAIGNS_CARD) {
                        data class BlazeCampaignsCardItem(
                            val id: Int,
                            val title: UiString,
                            val status: CampaignStatus?,
                            val featuredImageUrl: String?,
                            val stats: BlazeCampaignStats?,
                            val onClick: (campaignId: Int) -> Unit,
                        ) {
                            data class BlazeCampaignStats(
                                val impressions: UiString,
                                val clicks: UiString,
                            )
                        }

                        data class BlazeCampaignsCardFooter(
                            val label: UiString,
                            val onClick: ListItemInteraction,
                        )

                        data class MoreMenuOptions(
                            val viewAllCampaignsItemClick: ListItemInteraction,
                            val learnMoreClick: ListItemInteraction,
                            val hideThisMenuItemClick: ListItemInteraction,
                            val onMoreClick: ListItemInteraction
                        )
                    }

                    data class PromoteWithBlazeCard(
                        val title: UiString?,
                        val subtitle: UiString?,
                        val onClick: ListItemInteraction,
                        val moreMenuOptions: MoreMenuOptions
                    ) : BlazeCard(dashboardCardType = DashboardCardType.PROMOTE_WITH_BLAZE_CARD) {
                        data class MoreMenuOptions(
                            val onMoreClick: ListItemInteraction,
                            val hideThisMenuItemClick: ListItemInteraction,
                            val learnMoreClick: ListItemInteraction
                        )
                    }
                }

                data class DashboardPlansCard(
                    val title: UiString?,
                    val subtitle: UiString?,
                    val onClick: ListItemInteraction,
                    val onHideMenuItemClick: ListItemInteraction,
                    val onMoreMenuClick: ListItemInteraction,
                ) : DashboardCard(dashboardCardType = DashboardCardType.DASHBOARD_PLANS_CARD)
            }
        }
    }

    sealed class Item(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false
    ) : MySiteCardAndItem(type, activeQuickStartItem) {
        data class InfoItem(val title: UiString) : Item(INFO_ITEM)

        data class SingleActionCard(
            @StringRes val textResource: Int,
            @DrawableRes val imageResource: Int,
            val onActionClick: () -> Unit
        ) : Item(SINGLE_ACTION_CARD)

        data class CategoryHeaderItem(val title: UiString) : Item(CATEGORY_HEADER_ITEM)

        data class CategoryEmptyHeaderItem(val title: UiString) : Item(CATEGORY_EMPTY_HEADER_ITEM)

        data class ListItem(
            @DrawableRes val primaryIcon: Int,
            val primaryText: UiString,
            @DrawableRes val secondaryIcon: Int? = null,
            val secondaryText: UiString? = null,
            val showFocusPoint: Boolean = false,
            val onClick: ListItemInteraction,
            val disablePrimaryIconTint: Boolean = false
        ) : Item(LIST_ITEM, activeQuickStartItem = showFocusPoint)
    }

    data class JetpackBadge(
        val text: UiString,
        val onClick: ListItemInteraction? = null,
    ) : MySiteCardAndItem(JETPACK_BADGE)
}
