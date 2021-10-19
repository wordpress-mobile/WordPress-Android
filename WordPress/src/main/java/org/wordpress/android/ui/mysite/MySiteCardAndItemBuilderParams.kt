package org.wordpress.android.ui.mysite

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData

sealed class MySiteCardAndItemBuilderParams {
    data class DomainRegistrationCardBuilderParams(
        val isDomainCreditAvailable: Boolean,
        val domainRegistrationClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class PostCardBuilderParams(val mockedPostsData: MockedPostsData?) : MySiteCardAndItemBuilderParams()

    data class QuickActionsCardBuilderParams(
        val siteModel: SiteModel,
        val activeTask: QuickStartTask?,
        val onQuickActionStatsClick: () -> Unit,
        val onQuickActionPagesClick: () -> Unit,
        val onQuickActionPostsClick: () -> Unit,
        val onQuickActionMediaClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()
}
