package org.wordpress.android.usecase.social

import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.extensions.doesNotContain
import javax.inject.Inject

class GetIsSocialSharingLimitedUseCase @Inject constructor(
    private val siteStore: SiteStore,
) {
    fun execute(siteId: Long): Boolean =
        siteStore.getSiteBySiteId(siteId)?.run {
            !isHostedAtWPCom && (planActiveFeatures?.split(",")?.doesNotContain(FEATURE_SOCIAL_SHARES_1000) ?: true)
        } ?: false
}

private const val FEATURE_SOCIAL_SHARES_1000 = "social-shares-1000"
