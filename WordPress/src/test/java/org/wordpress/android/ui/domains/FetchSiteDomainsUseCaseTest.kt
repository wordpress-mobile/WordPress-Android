package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.usecases.FetchSiteDomainsUseCase
import org.wordpress.android.ui.domains.usecases.SiteDomainsResult
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SiteDomainsResponse

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchSiteDomainsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: FetchSiteDomainsUseCase

    private val site = SiteModel().apply { siteId = 1234L }

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = FetchSiteDomainsUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given site domains are returned, when execute, returns success`() = test {
        val domains = listOf(
            testSiteDomain(domain = "test.domain.one"),
            testSiteDomain(domain = "test.domain.two"),
        )
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.Success(
                    SiteDomainsResponse(domains)
                ) as WpRequestResult<Any>
            )

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SiteDomainsResult.Success::class.java)
        with(result as SiteDomainsResult.Success) {
            assertThat(this.domains).hasSize(2)
            assertThat(this.domains[0].domain).isEqualTo("test.domain.one")
            assertThat(this.domains[1].domain).isEqualTo("test.domain.two")
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given the site has no domains, when execute, returns an empty success`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.Success(
                    SiteDomainsResponse(emptyList())
                ) as WpRequestResult<Any>
            )

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SiteDomainsResult.Success::class.java)
        assertThat((result as SiteDomainsResult.Success).domains).isEmpty()
    }

    @Test
    fun `given site domains returns error, when execute, returns error`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.GET
                )
            )

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SiteDomainsResult.Error::class.java)
    }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SiteDomainsResult.Error::class.java)
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SiteDomainsResult.Error::class.java)
    }
}
