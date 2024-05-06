package org.wordpress.android.ui.mysite

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardContentType
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardType
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.utils.UiString

sealed class MySiteCardAndItemBuilderParams {
    data class SiteInfoCardBuilderParams(
        val site: SiteModel,
        val showSiteIconProgressBar: Boolean,
        val titleClick: () -> Unit,
        val iconClick: () -> Unit,
        val urlClick: () -> Unit,
        val switchSiteClick: () -> Unit,
        val activeTask: QuickStartTask?
    ) : MySiteCardAndItemBuilderParams()

    data class InfoItemBuilderParams(
        val isStaleMessagePresent: Boolean
    ) : MySiteCardAndItemBuilderParams()

    data class QuickStartCardBuilderParams(
        val quickStartCategories: List<QuickStartCategory>,
        val onQuickStartTaskTypeItemClick: (type: QuickStartTaskType) -> Unit,
        val moreMenuClickParams: MoreMenuParams
    ) : MySiteCardAndItemBuilderParams() {
        data class MoreMenuParams(
            val onMoreMenuClick: (type: QuickStartCardType) -> Unit,
            val onHideThisMenuItemClick: (type: QuickStartCardType) -> Unit,
        )
    }

    data class TodaysStatsCardBuilderParams(
        val todaysStatsCard: TodaysStatsCardModel?,
        val onTodaysStatsCardClick: () -> Unit,
        val onGetMoreViewsClick: () -> Unit,
        val moreMenuClickParams: MoreMenuParams
    ) : MySiteCardAndItemBuilderParams() {
        data class MoreMenuParams(
            val onMoreMenuClick: () -> Unit,
            val onHideThisMenuItemClick: () -> Unit,
            val onViewStatsMenuItemClick: () -> Unit
        )
    }

    data class PostCardBuilderParams(
        val posts: PostsCardModel?,
        val onPostItemClick: (params: PostItemClickParams) -> Unit,
        val moreMenuClickParams: MoreMenuParams
    ) : MySiteCardAndItemBuilderParams() {
        data class PostItemClickParams(
            val postCardType: PostCardType,
            val postId: Int
        )

        data class MoreMenuParams(
            val onMoreMenuClick: (postCardType: PostCardType) -> Unit,
            val onHideThisMenuItemClick: (postCardType: PostCardType) -> Unit,
            val onViewPostsMenuItemClick: (postCardType: PostCardType) -> Unit
        )
    }

    data class PagesCardBuilderParams(
        val pageCard: PagesCardModel?,
        val onPagesItemClick: (params: PagesItemClickParams) -> Unit,
        val onFooterLinkClick: () -> Unit,
        val moreMenuClickParams: MoreMenuParams
    ) : MySiteCardAndItemBuilderParams() {
        data class PagesItemClickParams(
            val pagesCardType: PagesCardContentType,
            val pageId: Int
        )

        data class MoreMenuParams(
            val onMoreMenuClick: () -> Unit,
            val onHideThisCardItemClick: () -> Unit,
            val onAllPagesItemClick: () -> Unit
        )
    }

    data class ActivityCardBuilderParams(
        val activityCardModel: CardModel.ActivityCardModel?,
        val onActivityItemClick: (activityCardItemClickParams: ActivityCardItemClickParams) -> Unit,
        val onAllActivityMenuItemClick: () -> Unit,
        val onHideMenuItemClick: () -> Unit,
        val onMoreMenuClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams() {
        data class ActivityCardItemClickParams(
            val activityId: String,
            val isRewindable: Boolean
        )
    }

    data class SiteItemsBuilderParams(
        val site: SiteModel,
        val activeTask: QuickStartTask? = null,
        val backupAvailable: Boolean = false,
        val scanAvailable: Boolean = false,
        val enableFocusPoints: Boolean = false,
        val onClick: (ListItemAction) -> Unit,
        val isBlazeEligible: Boolean = false
    ) : MySiteCardAndItemBuilderParams()

    data class BloggingPromptCardBuilderParams(
        val bloggingPrompt: BloggingPromptModel?,
        val onShareClick: (message: String) -> Unit,
        val onAnswerClick: (promptId: Int) -> Unit,
        val onSkipClick: () -> Unit,
        val onViewMoreClick: () -> Unit,
        val onViewAnswersClick: (tagUrl: String) -> Unit,
        val onRemoveClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class BloganuaryNudgeCardBuilderParams(
        val title: UiString,
        val text: UiString,
        val isEligible: Boolean,
        val onLearnMoreClick: () -> Unit,
        val onMoreMenuClick: () -> Unit,
        val onHideMenuItemClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class DynamicCardsBuilderParams(
        val dynamicCards: DynamicCardsModel?,
        val onCtaClick: (params: ClickParams) -> Unit,
        val onCardClick: (params: ClickParams) -> Unit,
        val onHideMenuItemClick: (cardId: String) -> Unit
    ) : MySiteCardAndItemBuilderParams() {
        data class ClickParams(val id: String, val actionUrl: String)
    }

    sealed class BlazeCardBuilderParams : MySiteCardAndItemBuilderParams() {
        data class PromoteWithBlazeCardBuilderParams(
            val onClick: () -> Unit,
            val moreMenuParams: MoreMenuParams
        ) : BlazeCardBuilderParams() {
            data class MoreMenuParams(
                val onMoreMenuClick: () -> Unit,
                val onHideThisCardItemClick: () -> Unit,
                val onLearnMoreClick: () -> Unit
            )
        }

        data class CampaignWithBlazeCardBuilderParams(
            val campaign: BlazeCampaignModel,
            val onCreateCampaignClick: () -> Unit,
            val onCampaignClick: (campaignId: String) -> Unit,
            val onCardClick: () -> Unit,
            val moreMenuParams: MoreMenuParams
        ) : BlazeCardBuilderParams() {
            data class MoreMenuParams(
                val viewAllCampaignsItemClick: () -> Unit,
                val onLearnMoreClick: () -> Unit,
                val onHideThisCardItemClick: () -> Unit,
                val onMoreMenuClick: () -> Unit
            )
        }
    }

    data class DashboardCardPlansBuilderParams(
        val isEligible: Boolean = false,
        val onClick: () -> Unit,
        val onHideMenuItemClick: () -> Unit,
        val onMoreMenuClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class SingleActionCardParams(
        @StringRes val textResource: Int,
        @DrawableRes val imageResource: Int,
        val onActionClick: () -> Unit
    )

    data class PersonalizeCardBuilderParams(
        val onClick: () -> Unit
    )
}
