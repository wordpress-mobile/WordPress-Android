package org.wordpress.android.ui.mysite

import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData

sealed class MySiteCardAndItemBuilderParams {
    data class DomainRegistrationCardBuilderParams(
        val isDomainCreditAvailable: Boolean,
        val domainRegistrationClick: () -> Unit
    ) : MySiteCardAndItemBuilderParams()

    data class PostCardBuilderParams(val mockedPostsData: MockedPostsData?) : MySiteCardAndItemBuilderParams()
}
