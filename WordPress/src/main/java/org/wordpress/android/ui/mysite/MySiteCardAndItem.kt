package org.wordpress.android.ui.mysite

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.CATEGORY_HEADER_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DASHBOARD_CARDS
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.INFO_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.JETPACK_BADGE
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.JETPACK_FEATURE_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.LIST_ITEM
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_ACTIONS_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_LINK_RIBBON
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.QUICK_START_DYNAMIC_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.SINGLE_ACTION_CARD
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type.SITE_INFO_CARD
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

typealias PromptID = Int

sealed class MySiteCardAndItem(open val type: Type, open val activeQuickStartItem: Boolean = false) {
    enum class Type {
        SITE_INFO_CARD,
        QUICK_ACTIONS_CARD,
        QUICK_LINK_RIBBON,
        DOMAIN_REGISTRATION_CARD,
        QUICK_START_CARD,
        QUICK_START_DYNAMIC_CARD,
        INFO_ITEM,
        CATEGORY_HEADER_ITEM,
        LIST_ITEM,
        DASHBOARD_CARDS,
        JETPACK_BADGE,
        SINGLE_ACTION_CARD,
        JETPACK_FEATURE_CARD,
        JETPACK_SWITCH_CARD
    }

    enum class DashboardCardType {
        ERROR_CARD,
        QUICK_START_CARD,
        TODAYS_STATS_CARD_ERROR,
        TODAYS_STATS_CARD,
        POST_CARD_ERROR,
        POST_CARD_WITHOUT_POST_ITEMS,
        POST_CARD_WITH_POST_ITEMS,
        BLOGGING_PROMPT_CARD
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
        data class QuickActionsCard(
            val title: UiString,
            val onStatsClick: ListItemInteraction,
            val onPagesClick: ListItemInteraction,
            val onPostsClick: ListItemInteraction,
            val onMediaClick: ListItemInteraction,
            val showPages: Boolean = true
        ) : Card(QUICK_ACTIONS_CARD)

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
            val moreMenuVisible: Boolean = true,
            val onRemoveMenuItemClick: ListItemInteraction,
            val taskTypeItems: List<QuickStartTaskTypeItem>
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
        }

        data class JetpackFeatureCard(
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
            val onMoreMenuClick: ListItemInteraction
        ) : Card(Type.JETPACK_SWITCH_CARD)

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
                        val views: UiString,
                        val visitors: UiString,
                        val likes: UiString,
                        val onCardClick: () -> Unit,
                        val message: TextWithLinks? = null,
                        val footerLink: FooterLink
                    ) : TodaysStatsCard(dashboardCardType = DashboardCardType.TODAYS_STATS_CARD)

                    data class FooterLink(
                        val label: UiString,
                        val onClick: () -> Unit
                    )

                    data class TextWithLinks(
                        val text: UiString,
                        val links: List<Clickable>? = null
                    ) {
                        data class Clickable(val navigationAction: ListItemInteraction)
                    }
                }

                sealed class PostCard(
                    override val dashboardCardType: DashboardCardType,
                    open val footerLink: FooterLink? = null
                ) : DashboardCard(dashboardCardType) {
                    data class Error(
                        override val title: UiString
                    ) : PostCard(dashboardCardType = DashboardCardType.POST_CARD_ERROR), ErrorWithinCard

                    data class PostCardWithoutPostItems(
                        val postCardType: PostCardType,
                        val title: UiString,
                        val excerpt: UiString,
                        @DrawableRes val imageRes: Int,
                        override val footerLink: FooterLink,
                        val onClick: ListItemInteraction
                    ) : PostCard(
                        dashboardCardType = DashboardCardType.POST_CARD_WITHOUT_POST_ITEMS,
                        footerLink = footerLink
                    )

                    data class PostCardWithPostItems(
                        val postCardType: PostCardType,
                        val title: UiString,
                        val postItems: List<PostItem>,
                        override val footerLink: FooterLink
                    ) : PostCard(
                        dashboardCardType = DashboardCardType.POST_CARD_WITH_POST_ITEMS,
                        footerLink = footerLink
                    ) {
                        data class PostItem(
                            val title: UiString,
                            val excerpt: UiString?,
                            val featuredImageUrl: String?,
                            val isTimeIconVisible: Boolean = false,
                            val onClick: ListItemInteraction
                        )
                    }

                    data class FooterLink(
                        val label: UiString,
                        val onClick: (postCardType: PostCardType) -> Unit
                    )
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
                        val showViewMoreAction: Boolean,
                        val onShareClick: (String) -> Unit,
                        val onAnswerClick: (PromptID) -> Unit,
                        val onSkipClick: () -> Unit,
                        val onViewMoreClick: () -> Unit,
                        val onViewAnswersClick: ((PromptID) -> Unit)?,
                    ) : BloggingPromptCard(dashboardCardType = DashboardCardType.BLOGGING_PROMPT_CARD)
                }
            }
        }
    }

    sealed class DynamicCard(
        override val type: Type,
        override val activeQuickStartItem: Boolean = false,
        open val dynamicCardType: DynamicCardType,
        open val onMoreClick: ListItemInteraction
    ) : MySiteCardAndItem(
        type,
        activeQuickStartItem
    ) {
        data class QuickStartDynamicCard(
            val id: DynamicCardType,
            val title: UiString,
            val taskCards: List<QuickStartTaskCard>,
            @ColorRes val accentColor: Int,
            val progress: Int,
            override val onMoreClick: ListItemInteraction
        ) : DynamicCard(QUICK_START_DYNAMIC_CARD, dynamicCardType = id, onMoreClick = onMoreClick) {
            data class QuickStartTaskCard(
                val quickStartTask: QuickStartTask,
                val title: UiString,
                val description: UiString,
                @DrawableRes val illustration: Int,
                @ColorRes val accentColor: Int,
                val done: Boolean = false,
                val onClick: ListItemInteraction
            )
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

        data class ListItem(
            @DrawableRes val primaryIcon: Int,
            val primaryText: UiString,
            @DrawableRes val secondaryIcon: Int? = null,
            val secondaryText: UiString? = null,
            val showFocusPoint: Boolean = false,
            val onClick: ListItemInteraction
        ) : Item(LIST_ITEM, activeQuickStartItem = showFocusPoint)
    }

    data class JetpackBadge(
        val text: UiString,
        val onClick: ListItemInteraction? = null,
    ) : MySiteCardAndItem(JETPACK_BADGE)
}
