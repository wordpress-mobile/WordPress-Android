package org.wordpress.android.ui.mysite.items.listitem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.UserCapabilitiesMap
import uniffi.wp_api.UserCapability
import uniffi.wp_api.UserWithEditContext
import uniffi.wp_api.UsersRequestRetrieveMeWithEditContextResponse

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
    fun `canModerateComments returns true when the user has the capability`() = test {
        stubSuccess(site, canModerate = true)

        assertThat(checker.canModerateComments(site)).isTrue
    }

    @Test
    fun `canModerateComments returns false when the user lacks the capability`() = test {
        stubSuccess(site, canModerate = false)

        assertThat(checker.canModerateComments(site)).isFalse
    }

    @Test
    fun `both capabilities come from a single fetch that is cached across calls`() = test {
        stubSuccess(site, canModerate = true, editThemeOptions = true)

        assertThat(checker.canModerateComments(site)).isTrue
        assertThat(checker.canModerateComments(site)).isTrue
        assertThat(checker.hasEditThemeOptionsCapability(site)).isTrue

        verify(wpApiClientProvider, times(1)).getWpApiClient(site)
    }

    @Test
    fun `canModerateComments fails closed when the capability cannot be fetched`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        assertThat(checker.canModerateComments(site)).isFalse
    }

    @Test
    fun `a failed fetch is not cached and is retried on the next call`() = test {
        whenever(wpApiClientProvider.getWpApiClient(site)).thenThrow(RuntimeException("boom"))

        checker.canModerateComments(site)
        checker.canModerateComments(site)

        // A transient failure must not lock in a fail-closed result for the whole session.
        verify(wpApiClientProvider, times(2)).getWpApiClient(site)
    }

    @Test
    fun `clearing the cache forces a refetch`() = test {
        stubSuccess(site, canModerate = true)

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
        stubSuccess(site, canModerate = true)
        stubSuccess(otherSite, canModerate = false)

        assertThat(checker.canModerateComments(site)).isTrue
        assertThat(checker.canModerateComments(otherSite)).isFalse
        // A repeat call for the first site reads its own cached value, not the other site's.
        assertThat(checker.canModerateComments(site)).isTrue

        verify(wpApiClientProvider, times(1)).getWpApiClient(site)
        verify(wpApiClientProvider, times(1)).getWpApiClient(otherSite)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun stubSuccess(
        site: SiteModel,
        canModerate: Boolean,
        editThemeOptions: Boolean = false
    ) {
        val caps = mock<UserCapabilitiesMap> {
            on { hasCap(UserCapability.ModerateComments) } doReturn canModerate
            on { hasCap(UserCapability.EditThemeOptions) } doReturn editThemeOptions
        }
        val user = mock<UserWithEditContext> { on { capabilities } doReturn caps }
        val response = mock<UsersRequestRetrieveMeWithEditContextResponse> { on { data } doReturn user }
        val client = mock<WpApiClient>()
        whenever(client.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(response) as WpRequestResult<Any>)
        whenever(wpApiClientProvider.getWpApiClient(site)).thenReturn(client)
    }
}
