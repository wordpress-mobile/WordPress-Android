package org.wordpress.android.ui.sitecreation.usecases

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
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.FreeDomainSuggestion
import uniffi.wp_api.RequestMethod

private const val SEARCH_QUERY = "test"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchDomainsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: FetchDomainsUseCase

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = FetchDomainsUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given successful response, when fetchDomains, then return Success with suggestions`() =
        test {
            val suggestions = listOf(
                DomainSuggestion.Free(
                    FreeDomainSuggestion(
                        domainName = "$SEARCH_QUERY.wordpress.com",
                        cost = "Free",
                        isFree = true,
                    )
                )
            )
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.Success(suggestions)
                        as WpRequestResult<Any>
                )

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isInstanceOf(FetchDomainsResult.Success::class.java)
            val success = result as FetchDomainsResult.Success
            assertThat(success.suggestions).hasSize(1)
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given error response, when fetchDomains, then return Error`() =
        test {
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(
                    WpRequestResult.UnknownError<Any>(
                        500.toUInt(),
                        "Internal Server Error",
                        "",
                        RequestMethod.GET,
                    )
                )

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isEqualTo(FetchDomainsResult.Error)
        }
}
