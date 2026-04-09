package org.wordpress.android.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ThemeAuthor
import uniffi.wp_api.ThemeAuthorUri
import uniffi.wp_api.ThemeDescription
import uniffi.wp_api.ThemeName
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeStylesheet
import uniffi.wp_api.ThemeTags
import uniffi.wp_api.ThemeUri
import uniffi.wp_api.ThemeWithEditContext
import uniffi.wp_api.ThemesRequestListWithEditContextResponse
import uniffi.wp_api.WpNetworkHeaderMap

@ExperimentalCoroutinesApi
class ThemeRepositoryTest : BaseUnitTest() {
    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var wpApiClient: WpApiClient

    private lateinit var repository: ThemeRepository

    private val testSite = SiteModel().apply {
        id = 1
        url = "https://test.wordpress.com"
    }

    @Before
    fun setUp() {
        whenever(wpApiClientProvider.getWpApiClient(testSite))
            .thenReturn(wpApiClient)

        repository = ThemeRepository(
            wpApiClientProvider = wpApiClientProvider,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `returns theme when API succeeds with non-empty list`() =
        runTest {
            val theme = buildTheme(stylesheet = "twentytwentyfive")
            mockSuccessResponse(listOf(theme))

            val result = repository.fetchCurrentTheme(testSite)

            assertThat(result).isEqualTo(theme)
        }

    @Test
    fun `returns null when API succeeds with empty list`() =
        runTest {
            mockSuccessResponse(emptyList())

            val result = repository.fetchCurrentTheme(testSite)

            assertThat(result).isNull()
        }

    @Test
    fun `returns first theme when API returns multiple`() =
        runTest {
            val first = buildTheme(stylesheet = "first")
            val second = buildTheme(stylesheet = "second")
            mockSuccessResponse(listOf(first, second))

            val result = repository.fetchCurrentTheme(testSite)

            assertThat(result).isEqualTo(first)
        }

    @Test
    fun `returns null on API error`() = runTest {
        val error = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "https://test.wordpress.com/wp-json",
            requestMethod = uniffi.wp_api.RequestMethod.GET
        )
        whenever(wpApiClient.request<Any>(any()))
            .thenReturn(error)

        val result = repository.fetchCurrentTheme(testSite)

        assertThat(result).isNull()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mockSuccessResponse(
        themes: List<ThemeWithEditContext>
    ) {
        val response = ThemesRequestListWithEditContextResponse(
            data = themes,
            headerMap = mock<WpNetworkHeaderMap>()
        )
        val success = WpRequestResult.Success(response)
        whenever(wpApiClient.request<Any>(any()))
            .thenReturn(
                success
                    as WpRequestResult<Any>
            )
    }

    private fun buildTheme(
        stylesheet: String = "test-theme",
        isBlockTheme: Boolean = false
    ) = ThemeWithEditContext(
        stylesheet = ThemeStylesheet(stylesheet),
        template = stylesheet,
        requiresPhp = "",
        requiresWp = "",
        textdomain = stylesheet,
        version = "1.0",
        screenshot = "",
        author = ThemeAuthor(raw = "", rendered = ""),
        authorUri = ThemeAuthorUri(raw = "", rendered = ""),
        description = ThemeDescription(
            raw = "", rendered = ""
        ),
        name = ThemeName(raw = stylesheet, rendered = stylesheet),
        tags = ThemeTags(raw = emptyList(), rendered = ""),
        themeUri = ThemeUri(raw = "", rendered = ""),
        status = ThemeStatus.Active,
        isBlockTheme = isBlockTheme,
        stylesheetUri = "",
        templateUri = "",
        themeSupports = null,
        defaultTemplateTypes = emptyList()
    )
}
