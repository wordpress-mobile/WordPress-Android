package org.wordpress.android.fluxc.model.jetpacksocial

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.site.JetpackSocialResponse
import org.wordpress.android.fluxc.persistence.jetpacksocial.JetpackSocialDao.JetpackSocialEntity

class JetpackSocialMapperTest {
    private val siteLocalId = 123
    private val classToTest = JetpackSocialMapper()

    @Test
    fun `Should map entity to default values correctly if response properties are null`() {
        val actual = classToTest.mapEntity(
            siteLocalId = siteLocalId,
            response = JetpackSocialResponse(
                isShareLimitEnabled = null,
                toBePublicizedCount = null,
                shareLimit = null,
                publicizedCount = null,
                sharedPostsCount = null,
                sharesRemaining = null,
                isEnhancedPublishingEnabled = null,
                isSocialImageGeneratorEnabled = null,
            ),
        )
        val expected = JetpackSocialEntity(
            siteLocalId = siteLocalId,
            isShareLimitEnabled = false,
            toBePublicizedCount = -1,
            shareLimit = -1,
            publicizedCount = -1,
            sharedPostsCount = -1,
            sharesRemaining = -1,
            isEnhancedPublishingEnabled = false,
            isSocialImageGeneratorEnabled = false,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map entity correctly if response properties are present`() {
        val actual = classToTest.mapEntity(
            siteLocalId = siteLocalId,
            response = JetpackSocialResponse(
                isShareLimitEnabled = true,
                toBePublicizedCount = 10,
                shareLimit = 11,
                publicizedCount = 12,
                sharedPostsCount = 13,
                sharesRemaining = 14,
                isEnhancedPublishingEnabled = true,
                isSocialImageGeneratorEnabled = true,
            ),
        )
        val expected = JetpackSocialEntity(
            siteLocalId = siteLocalId,
            isShareLimitEnabled = true,
            toBePublicizedCount = 10,
            shareLimit = 11,
            publicizedCount = 12,
            sharedPostsCount = 13,
            sharesRemaining = 14,
            isEnhancedPublishingEnabled = true,
            isSocialImageGeneratorEnabled = true,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map domain correctly`() {
        val actual = classToTest.mapDomain(
            JetpackSocialEntity(
                siteLocalId = siteLocalId,
                isShareLimitEnabled = true,
                toBePublicizedCount = 10,
                shareLimit = 11,
                publicizedCount = 12,
                sharedPostsCount = 13,
                sharesRemaining = 14,
                isEnhancedPublishingEnabled = true,
                isSocialImageGeneratorEnabled = true,
            )
        )
        val expected = JetpackSocial(
            isShareLimitEnabled = true,
            toBePublicizedCount = 10,
            shareLimit = 11,
            publicizedCount = 12,
            sharedPostsCount = 13,
            sharesRemaining = 14,
            isEnhancedPublishingEnabled = true,
            isSocialImageGeneratorEnabled = true,
        )
        assertThat(actual).isEqualTo(expected)
    }
}
