package org.wordpress.android.ui.mysite

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction

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

    data class QuickActionsCardBuilderParams(
        val siteModel: SiteModel,
        val onQuickActionStatsClick: () -> Unit,
        val onQuickActionPagesClick: () -> Unit,
        val onQuickActionPostsClick: () -> Unit,
        val onQuickActionMediaClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class QuickLinkRibbonBuilderParams(
        val siteModel: SiteModel,
        val onPagesClick: () -> Unit,
        val onPostsClick: () -> Unit,
        val onMediaClick: () -> Unit,
        val onStatsClick: () -> Unit,
        val activeTask: QuickStartTask?,
        val enableFocusPoints: Boolean = false
    ) : MySiteCardAndItemBuilderParams()

    data class DomainRegistrationCardBuilderParams(
        val isDomainCreditAvailable: Boolean,
        val domainRegistrationClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class QuickStartCardBuilderParams(
        val quickStartCategories: List<QuickStartCategory>,
        val onQuickStartBlockRemoveMenuItemClick: () -> Unit,
        val onQuickStartTaskTypeItemClick: (type: QuickStartTaskType) -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class DashboardCardsBuilderParams(
        val showErrorCard: Boolean = false,
        val onErrorRetryClick: () -> Unit,
        val todaysStatsCardBuilderParams: TodaysStatsCardBuilderParams,
        val postCardBuilderParams: PostCardBuilderParams,
        val bloggingPromptCardBuilderParams: BloggingPromptCardBuilderParams
    ) : MySiteCardAndItemBuilderParams()

    data class TodaysStatsCardBuilderParams(
        val todaysStatsCard: TodaysStatsCardModel?,
        val onTodaysStatsCardClick: () -> Unit,
        val onGetMoreViewsClick: () -> Unit,
        val onFooterLinkClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class PostCardBuilderParams(
        val posts: PostsCardModel?,
        val onPostItemClick: (params: PostItemClickParams) -> Unit,
        val onFooterLinkClick: (postCardType: PostCardType) -> Unit
    ) : MySiteCardAndItemBuilderParams() {
        data class PostItemClickParams(
            val postCardType: PostCardType,
            val postId: Int
        )
    }

    data class SiteItemsBuilderParams(
        val site: SiteModel,
        val activeTask: QuickStartTask? = null,
        val backupAvailable: Boolean = false,
        val scanAvailable: Boolean = false,
        val enableStatsFocusPoint: Boolean = false,
        val enablePagesFocusPoint: Boolean = false,
        val enableMediaFocusPoint: Boolean = false,
        val onClick: (ListItemAction) -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class BloggingPromptCardBuilderParams(
        val bloggingPrompt: BloggingPromptModel?,
        val showViewMoreAction: Boolean,
        val onShareClick: (message: String) -> Unit,
        val onAnswerClick: (promptId: Int) -> Unit,
        val onSkipClick: () -> Unit,
        val onViewMoreClick: () -> Unit,
        val onViewAnswersClick: (promptId: Int) -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class SingleActionCardParams(
        @StringRes val textResource: Int,
        @DrawableRes val imageResource: Int,
        val onActionClick: () -> Unit
    )
}
