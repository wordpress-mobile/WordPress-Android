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

/**
 * Note: the success path isn't unit-tested here. Exercising it would require a real
 * [uniffi.wp_api.UserCapabilitiesMap] (its `hasCap` calls into native code, unavailable in JVM
 * tests) or mocking the wordpress-rs data classes (forbidden by the DoNotMockDataClass lint rule).
 * The capability-gating behavior that depends on the result is covered by the ViewModel tests,
 * which stub [SiteCapabilityChecker.canModerateComments] directly.
 */
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
    fun `a failed fetch is not cached and is retried on the next call`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        checker.canModerateComments(site)
        checker.canModerateComments(site)

        // A transient failure must not lock in a fail-closed result for the whole session.
        verify(wpApiClientProvider, times(2)).getWpApiClient(site)
    }
}
