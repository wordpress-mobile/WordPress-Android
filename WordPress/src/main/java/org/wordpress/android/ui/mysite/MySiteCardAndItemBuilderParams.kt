package org.wordpress.android.ui.mysite

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.cards.post.PostCardType
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction

sealed class MySiteCardAndItemBuilderParams {
    data class DomainRegistrationCardBuilderParams(
        val isDomainCreditAvailable: Boolean,
        val domainRegistrationClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class PostCardBuilderParams(
        val mockedPostsData: MockedPostsData?,
        val onPostItemClick: (postId: Int) -> Unit,
        val onFooterLinkClick: (postCardType: PostCardType) -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class QuickActionsCardBuilderParams(
        val siteModel: SiteModel,
        val activeTask: QuickStartTask?,
        val onQuickActionStatsClick: () -> Unit,
        val onQuickActionPagesClick: () -> Unit,
        val onQuickActionPostsClick: () -> Unit,
        val onQuickActionMediaClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class QuickStartCardBuilderParams(
        val quickStartCategories: List<QuickStartCategory>,
        val onQuickStartBlockRemoveMenuItemClick: () -> Unit,
        val onQuickStartTaskTypeItemClick: (type: QuickStartTaskType) -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class SiteInfoCardBuilderParams(
        val site: SiteModel,
        val showSiteIconProgressBar: Boolean,
        val titleClick: () -> Unit,
        val iconClick: () -> Unit,
        val urlClick: () -> Unit,
        val switchSiteClick: () -> Unit,
        val activeTask: QuickStartTask?
    ) : MySiteCardAndItemBuilderParams()

    data class SiteItemsBuilderParams(
        val site: SiteModel,
        val activeTask: QuickStartTask? = null,
        val backupAvailable: Boolean = false,
        val scanAvailable: Boolean = false,
        val onClick: (ListItemAction) -> Unit
    ) : MySiteCardAndItemBuilderParams()
}
