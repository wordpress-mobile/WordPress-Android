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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.management.testDomainItem
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AllDomainsResponse
import uniffi.wp_api.DomainSubtypeId
import uniffi.wp_api.RequestMethod

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchAllDomainsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: FetchAllDomainsUseCase

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = FetchAllDomainsUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given all-domains returns results, when execute, returns success`() =
        test {
            val domains = listOf(
                testDomainItem(domain = "test.domain.one"),
                testDomainItem(domain = "test.domain.two"),
            )
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.Success(
                        AllDomainsResponse(domains)
                    ) as WpRequestResult<Any>
                )

            val result = useCase.execute()

            assertThat(result).isInstanceOf(AllDomains.Success::class.java)
            with(result as AllDomains.Success) {
                assertThat(this.domains).hasSize(2)
                assertThat(this.domains[0].domain)
                    .isEqualTo("test.domain.one")
                assertThat(this.domains[1].domain)
                    .isEqualTo("test.domain.two")
            }
        }

    @Test
    fun `given all-domains returns error, when execute, returns error`() =
        test {
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.UnknownError<Any>(
                        500.toUInt(),
                        "Internal Server Error",
                        "",
                        RequestMethod.GET
                    )
                )

            val result = useCase.execute()

            assertThat(result).isInstanceOf(AllDomains.Error::class.java)
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given all-domains returns empty, when execute, returns empty`() =
        test {
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.Success(
                        AllDomainsResponse(emptyList())
                    ) as WpRequestResult<Any>
                )

            val result = useCase.execute()

            assertThat(result).isInstanceOf(AllDomains.Empty::class.java)
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given free site addresses, when execute, they are excluded`() =
        test {
            val domains = listOf(
                testDomainItem(
                    domain = "free.wordpress.com",
                    subtypeId = DomainSubtypeId.DefaultAddress,
                ),
                testDomainItem(
                    domain = "registered.domain",
                    subtypeId = DomainSubtypeId.DomainRegistration,
                ),
            )
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.Success(
                        AllDomainsResponse(domains)
                    ) as WpRequestResult<Any>
                )

            val result = useCase.execute()

            assertThat(result).isInstanceOf(AllDomains.Success::class.java)
            with(result as AllDomains.Success) {
                assertThat(this.domains).hasSize(1)
                assertThat(this.domains[0].domain)
                    .isEqualTo("registered.domain")
            }
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given only free site addresses, when execute, returns empty`() =
        test {
            val domains = listOf(
                testDomainItem(
                    domain = "free.wordpress.com",
                    subtypeId = DomainSubtypeId.DefaultAddress,
                ),
                testDomainItem(
                    domain = "staging.wpcomstaging.com",
                    subtypeId = DomainSubtypeId.DefaultAddress,
                ),
            )
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.Success(
                        AllDomainsResponse(domains)
                    ) as WpRequestResult<Any>
                )

            val result = useCase.execute()

            assertThat(result).isInstanceOf(AllDomains.Empty::class.java)
        }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute()

        assertThat(result).isInstanceOf(AllDomains.Error::class.java)
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute()

        assertThat(result).isInstanceOf(AllDomains.Error::class.java)
    }
}
