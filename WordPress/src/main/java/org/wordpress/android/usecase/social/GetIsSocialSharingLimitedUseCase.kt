package org.wordpress.android.usecase.social

import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
import javax.inject.Inject

class GetIsSocialSharingLimitedUseCase @Inject constructor(
    private val jetpackSocialFeatureConfig: JetpackSocialFeatureConfig,
    private val siteStore: SiteStore,
) {
    fun execute(siteId: Long): Boolean =
        jetpackSocialFeatureConfig.isEnabled() && siteStore.getSiteBySiteId(siteId)?.run {
            !isHostedAtWPCom && !hasSocialShares1000Active
        } ?: false
}
