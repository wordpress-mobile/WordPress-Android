package org.wordpress.android.fluxc.model.jetpacksocial

import org.wordpress.android.fluxc.network.rest.wpcom.site.JetpackSocialResponse
import org.wordpress.android.fluxc.persistence.jetpacksocial.JetpackSocialDao.JetpackSocialEntity
import javax.inject.Inject

class JetpackSocialMapper @Inject constructor() {
    fun mapEntity(response: JetpackSocialResponse, siteLocalId: Int): JetpackSocialEntity =
        with(response) {
            JetpackSocialEntity(
                siteLocalId = siteLocalId,
                isShareLimitEnabled = isShareLimitEnabled ?: false,
                toBePublicizedCount = toBePublicizedCount ?: -1,
                shareLimit = shareLimit ?: -1,
                publicizedCount = publicizedCount ?: -1,
                sharedPostsCount = sharedPostsCount ?: -1,
                sharesRemaining = sharesRemaining ?: -1,
                isEnhancedPublishingEnabled = isEnhancedPublishingEnabled ?: false,
                isSocialImageGeneratorEnabled = isSocialImageGeneratorEnabled ?: false,
            )
        }

    fun mapDomain(entity: JetpackSocialEntity): JetpackSocial =
        with(entity) {
            JetpackSocial(
                isShareLimitEnabled = isShareLimitEnabled,
                toBePublicizedCount = toBePublicizedCount,
                shareLimit = shareLimit,
                publicizedCount = publicizedCount,
                sharedPostsCount = sharedPostsCount,
                sharesRemaining = sharesRemaining,
                isEnhancedPublishingEnabled = isEnhancedPublishingEnabled,
                isSocialImageGeneratorEnabled = isSocialImageGeneratorEnabled,
            )
        }
}
