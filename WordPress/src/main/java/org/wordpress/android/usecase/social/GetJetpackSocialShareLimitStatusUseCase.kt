package org.wordpress.android.usecase.social

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchedJetpackSocialResult
import org.wordpress.android.util.extensions.doesNotContain
import javax.inject.Inject

class GetJetpackSocialShareLimitStatusUseCase @Inject constructor(
    private val siteStore: SiteStore,
) {
    suspend fun execute(siteModel: SiteModel): ShareLimit {
        val isShareLimitEnabled =
            !siteModel.isHostedAtWPCom
                    && (siteModel.planActiveFeatures?.split(",")?.doesNotContain(FEATURE_SOCIAL_SHARES_1000) != false)
        val result = siteStore.fetchJetpackSocial(siteModel)
        return if (isShareLimitEnabled && result is FetchedJetpackSocialResult.Success) {
            with(result.jetpackSocial) {
                ShareLimit.Enabled(
                    shareLimit = shareLimit,
                    publicizedCount = publicizedCount,
                    sharedPostsCount = sharedPostsCount,
                    sharesRemaining = sharesRemaining,
                )
            }
        } else {
            ShareLimit.Disabled
        }
    }
}

sealed interface ShareLimit {
    data class Enabled(
        val shareLimit: Int,
        val publicizedCount: Int,
        val sharedPostsCount: Int,
        val sharesRemaining: Int,
    ) : ShareLimit

    object Disabled : ShareLimit
}

private const val FEATURE_SOCIAL_SHARES_1000 = "social-shares-1000"
