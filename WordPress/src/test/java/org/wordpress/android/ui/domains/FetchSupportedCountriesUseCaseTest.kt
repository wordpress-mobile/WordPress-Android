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
import org.wordpress.android.ui.domains.usecases.FetchSupportedCountriesUseCase
import org.wordpress.android.ui.domains.usecases.SupportedCountriesResult
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SupportedCountries

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchSupportedCountriesUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: FetchSupportedCountriesUseCase

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = FetchSupportedCountriesUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun stubResponse(countries: SupportedCountries) {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(countries) as WpRequestResult<Any>)
    }

    @Test
    fun `given featured and all, when execute, the featured countries come first`() = test {
        stubResponse(
            SupportedCountries(
                featured = listOf(testSupportedCountry("CA", "Canada")),
                all = listOf(
                    testSupportedCountry("AF", "Afghanistan"),
                    testSupportedCountry("CA", "Canada"),
                ),
            )
        )

        val result = useCase.execute()

        assertThat(result).isInstanceOf(SupportedCountriesResult.Success::class.java)
        assertThat((result as SupportedCountriesResult.Success).countries.map { it.code })
            .containsExactly("CA", "AF", "CA")
    }

    @Test
    fun `given no featured countries, when execute, only the full list is returned`() = test {
        stubResponse(
            SupportedCountries(
                featured = emptyList(),
                all = listOf(testSupportedCountry("AF", "Afghanistan")),
            )
        )

        val result = useCase.execute()

        assertThat((result as SupportedCountriesResult.Success).countries.map { it.code })
            .containsExactly("AF")
    }

    @Test
    fun `given the request fails, when execute, returns error carrying no message`() = test {
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

        assertThat(result).isEqualTo(SupportedCountriesResult.Error(null))
    }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute()

        assertThat(result).isEqualTo(SupportedCountriesResult.Error(null))
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute()

        assertThat(result).isEqualTo(SupportedCountriesResult.Error(null))
    }
}
