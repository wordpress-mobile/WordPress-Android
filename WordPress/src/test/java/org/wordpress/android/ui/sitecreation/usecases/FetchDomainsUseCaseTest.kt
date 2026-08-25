package org.wordpress.android.ui.sitecreation.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.FreeDomainSuggestion
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.WpErrorCode

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

            assertThat(result).isEqualTo(
                FetchDomainsResult.Error(type = "GENERIC_ERROR", message = null)
            )
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given invalid_query error, when fetchDomains, then return InvalidQuery`() =
        test {
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(wpError("invalid_query", "Domain searches must contain a word"))

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isEqualTo(FetchDomainsResult.InvalidQuery)
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given empty_results error, when fetchDomains, then return Success with no suggestions`() =
        test {
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(wpError("empty_results", "No available domains for that search."))

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isEqualTo(FetchDomainsResult.Success(emptyList()))
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given another api error, when fetchDomains, then return Error carrying its code`() =
        test {
            whenever(wpComApiClient.request<Any>(any()))
                .thenReturn(wpError("empty_query", "Query is empty"))

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isEqualTo(
                FetchDomainsResult.Error(type = "empty_query", message = "Query is empty")
            )
        }

    @Test
    fun `given a signed out account, when fetchDomains, then report the missing token`() =
        test {
            // What `AccountStore` returns when signed out, despite its nullable type.
            whenever(accountStore.accessToken).thenReturn("")

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isEqualTo(
                FetchDomainsResult.Error(type = "NO_ACCESS_TOKEN", message = null)
            )
            verifyNoInteractions(wpComApiClient)
        }

    @Test
    fun `given a null access token, when fetchDomains, then report it instead of crashing`() =
        test {
            whenever(accountStore.accessToken).thenReturn(null)

            val result = useCase.fetchDomains(
                SEARCH_QUERY, "vendor", onlyWordpressCom = false
            )

            assertThat(result).isEqualTo(
                FetchDomainsResult.Error(type = "NO_ACCESS_TOKEN", message = null)
            )
            verifyNoInteractions(wpComApiClient)
        }

    @Suppress("UNCHECKED_CAST")
    private fun wpError(code: String, message: String) = WpRequestResult.WpError<Any>(
        errorCode = WpErrorCode.CustomException(code),
        errorMessage = message,
        statusCode = 400.toUInt(),
        response = "",
        requestUrl = "",
        requestMethod = RequestMethod.GET,
    ) as WpRequestResult<Any>
}
