package org.wordpress.android.ui.mysite.items.listitem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper

@ExperimentalCoroutinesApi
class SiteCapabilityCheckerTest : BaseUnitTest() {
    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    private lateinit var checker: SiteCapabilityChecker

    private val site = SiteModel().apply {
        id = 1
        siteId = 0L // self-hosted application-password sites report a WP.com blog id of 0
    }

    @Before
    fun setUp() {
        checker = SiteCapabilityChecker(wpApiClientProvider, appLogWrapper)
    }

    @Test
    fun `canModerateComments fails closed when the capability cannot be fetched`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        assertThat(checker.canModerateComments(site)).isFalse
    }

    @Test
    fun `hasEditThemeOptionsCapability fails closed when the capability cannot be fetched`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        assertThat(checker.hasEditThemeOptionsCapability(site)).isFalse
    }

    @Test
    fun `capabilities are fetched once and cached across calls`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        checker.canModerateComments(site)
        checker.canModerateComments(site)
        // A different capability reads from the same cached fetch rather than triggering another.
        checker.hasEditThemeOptionsCapability(site)

        verify(wpApiClientProvider, times(1)).getWpApiClient(site)
    }

    @Test
    fun `clearing the cache forces a refetch`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        checker.canModerateComments(site)
        checker.clearCacheForSite(site)
        checker.canModerateComments(site)

        verify(wpApiClientProvider, times(2)).getWpApiClient(site)
    }

    @Test
    fun `capabilities are cached per local site id, not the WP-com blog id`() = test {
        // Two distinct self-hosted sites both report siteId 0; keying by the local id keeps their
        // capabilities from colliding.
        val otherSite = SiteModel().apply {
            id = 2
            siteId = 0L
        }
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))
        whenever(wpApiClientProvider.getWpApiClient(otherSite)).thenThrow(RuntimeException("boom"))

        checker.canModerateComments(site)
        checker.canModerateComments(otherSite)

        verify(wpApiClientProvider, times(1)).getWpApiClient(site)
        verify(wpApiClientProvider, times(1)).getWpApiClient(otherSite)
    }
}
