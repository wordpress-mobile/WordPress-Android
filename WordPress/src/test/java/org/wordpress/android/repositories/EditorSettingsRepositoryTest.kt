package org.wordpress.android.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpLoginClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ApiRootRequestGetResponse
import uniffi.wp_api.ApiUrlResolver
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.ParseUrlException
import uniffi.wp_api.ThemeAuthor
import uniffi.wp_api.ThemeAuthorUri
import uniffi.wp_api.ThemeDescription
import uniffi.wp_api.ThemeName
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeStylesheet
import uniffi.wp_api.ThemeTags
import uniffi.wp_api.ThemeUri
import uniffi.wp_api.ThemeWithEditContext
import uniffi.wp_api.WpApiDetails
import uniffi.wp_api.WpNetworkHeaderMap

@ExperimentalCoroutinesApi
class EditorSettingsRepositoryTest : BaseUnitTest() {
    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var wpLoginClient: WpLoginClient

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var themeRepository: ThemeRepository

    @Mock
    lateinit var wpApiClient: WpApiClient

    @Mock
    lateinit var appPasswordClient: WpApiClient

    @Mock
    lateinit var apiUrlResolver: ApiUrlResolver

    @Mock
    lateinit var directHostResolver: ApiUrlResolver

    private lateinit var repository: EditorSettingsRepository

    private val testSite = SiteModel().apply {
        id = 1
        url = "https://test.wordpress.com"
    }

    @Before
    fun setUp() {
        whenever(wpApiClientProvider.getWpApiClient(testSite))
            .thenReturn(wpApiClient)
        whenever(wpApiClientProvider.getApiUrlResolver(testSite))
            .thenReturn(apiUrlResolver)

        repository = EditorSettingsRepository(
            wpApiClientProvider = wpApiClientProvider,
            wpLoginClient = wpLoginClient,
            appPrefsWrapper = appPrefsWrapper,
            themeRepository = themeRepository,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `fetch persists true when routes are present`() =
        runTest {
            mockApiRootResponse(
                hasEditorSettings = true,
                hasEditorAssets = true
            )
            mockThemeResponse(isBlockTheme = false)

            val result =
                repository.fetchEditorCapabilitiesForSite(
                    testSite
                )

            assertThat(result).isTrue()
            verify(appPrefsWrapper)
                .setSiteSupportsEditorSettings(testSite, true)
            verify(appPrefsWrapper)
                .setSiteSupportsEditorAssets(testSite, true)
        }

    @Test
    fun `fetch does not persist routes on API error`() = runTest {
        mockApiRootError()
        mockThemeResponse(isBlockTheme = false)

        repository.fetchEditorCapabilitiesForSite(testSite)

        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorSettings(any(), any())
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorAssets(any(), any())
    }

    @Test
    fun `fetch does not persist block theme when theme is null`() =
        runTest {
            mockApiRootResponse(
                hasEditorSettings = true,
                hasEditorAssets = true
            )
            whenever(
                themeRepository.fetchCurrentTheme(testSite)
            ).thenReturn(null)

            repository.fetchEditorCapabilitiesForSite(
                testSite
            )

            verify(appPrefsWrapper, never())
                .setSiteThemeIsBlockTheme(any(), any())
        }

    @Test
    fun `route failure does not prevent theme update`() =
        runTest {
            whenever(wpApiClient.request<Any>(any()))
                .thenThrow(RuntimeException("network error"))
            mockThemeResponse(isBlockTheme = true)

            val result =
                repository.fetchEditorCapabilitiesForSite(
                    testSite
                )

            assertThat(result).isFalse()
            verify(appPrefsWrapper)
                .setSiteThemeIsBlockTheme(testSite, true)
        }

    @Test
    fun `theme failure does not prevent route update`() =
        runTest {
            mockApiRootResponse(
                hasEditorSettings = true,
                hasEditorAssets = true
            )
            whenever(
                themeRepository.fetchCurrentTheme(testSite)
            ).thenThrow(RuntimeException("network error"))

            val result =
                repository.fetchEditorCapabilitiesForSite(
                    testSite
                )

            assertThat(result).isFalse()
            verify(appPrefsWrapper)
                .setSiteSupportsEditorSettings(testSite, true)
            verify(appPrefsWrapper)
                .setSiteSupportsEditorAssets(testSite, true)
        }

    @Test
    fun `both failures returns false without writing prefs`() =
        runTest {
            whenever(wpApiClient.request<Any>(any()))
                .thenThrow(RuntimeException("network error"))
            whenever(
                themeRepository.fetchCurrentTheme(testSite)
            ).thenThrow(RuntimeException("network error"))

            val result =
                repository.fetchEditorCapabilitiesForSite(
                    testSite
                )

            assertThat(result).isFalse()
            verify(appPrefsWrapper, never())
                .setSiteSupportsEditorSettings(any(), any())
            verify(appPrefsWrapper, never())
                .setSiteSupportsEditorAssets(any(), any())
            verify(appPrefsWrapper, never())
                .setSiteThemeIsBlockTheme(any(), any())
        }

    @Test
    fun `atomic site probes via api discovery`() =
        runTest {
            val atomicSite = SiteModel().apply {
                id = 2
                url = "https://atomic.example.com"
                setIsWPCom(true)
                setIsWPComAtomic(true)
            }
            mockDiscoverySuccess(
                siteUrl = atomicSite.url,
                hasEditorSettings = false,
                hasEditorAssets = false
            )
            whenever(themeRepository.fetchCurrentTheme(atomicSite))
                .thenReturn(buildTheme(isBlockTheme = false))

            val result =
                repository.fetchEditorCapabilitiesForSite(atomicSite)

            assertThat(result).isTrue()
            verify(appPrefsWrapper)
                .setSiteSupportsEditorSettings(atomicSite, false)
            verify(appPrefsWrapper)
                .setSiteSupportsEditorAssets(atomicSite, false)
            verify(wpApiClientProvider, never()).getWpApiClient(atomicSite)
        }

    @Test
    fun `atomic site returns false when discovery fails`() =
        runTest {
            val atomicSite = SiteModel().apply {
                id = 4
                url = "https://atomic.example.com"
                setIsWPCom(true)
                setIsWPComAtomic(true)
            }
            whenever(wpLoginClient.apiDiscovery(atomicSite.url))
                .thenReturn(
                    ApiDiscoveryResult.FailureParseSiteUrl(
                        ParseUrlException.Generic("")
                    )
                )
            whenever(themeRepository.fetchCurrentTheme(atomicSite))
                .thenReturn(buildTheme(isBlockTheme = false))

            val result =
                repository.fetchEditorCapabilitiesForSite(atomicSite)

            assertThat(result).isFalse()
            verify(appPrefsWrapper, never())
                .setSiteSupportsEditorSettings(any(), any())
            verify(appPrefsWrapper, never())
                .setSiteSupportsEditorAssets(any(), any())
            verify(wpApiClientProvider, never()).getWpApiClient(atomicSite)
        }

    @Test
    fun `atomic site with app password also probes via api discovery`() =
        runTest {
            val atomicSite = SiteModel().apply {
                id = 3
                url = "https://atomic.example.com"
                setIsWPCom(true)
                setIsWPComAtomic(true)
                apiRestUsernamePlain = "user"
                apiRestPasswordPlain = "secret"
            }
            mockDiscoverySuccess(
                siteUrl = atomicSite.url,
                hasEditorSettings = true,
                hasEditorAssets = true
            )
            whenever(themeRepository.fetchCurrentTheme(atomicSite))
                .thenReturn(buildTheme(isBlockTheme = false))

            val result =
                repository.fetchEditorCapabilitiesForSite(atomicSite)

            assertThat(result).isTrue()
            verify(appPrefsWrapper)
                .setSiteSupportsEditorSettings(atomicSite, true)
            verify(appPrefsWrapper)
                .setSiteSupportsEditorAssets(atomicSite, true)
            verify(wpApiClientProvider, never()).getWpApiClient(atomicSite)
        }

    @Test
    fun `atomic site falls back to authenticated probe when discovery fails`() =
        runTest {
            val atomicSite = SiteModel().apply {
                id = 5
                url = "https://atomic.example.com"
                setIsWPCom(true)
                setIsWPComAtomic(true)
                apiRestUsernamePlain = "user"
                apiRestPasswordPlain = "secret"
            }
            mockDiscoveryFailure(atomicSite.url)
            whenever(
                wpApiClientProvider.getApplicationPasswordClient(atomicSite)
            ).thenReturn(appPasswordClient)
            whenever(
                wpApiClientProvider.getDirectHostApiUrlResolver(atomicSite)
            ).thenReturn(directHostResolver)
            mockApiRootResponseFor(
                client = appPasswordClient,
                resolver = directHostResolver,
                hasEditorSettings = true,
                hasEditorAssets = true,
            )
            whenever(themeRepository.fetchCurrentTheme(atomicSite))
                .thenReturn(buildTheme(isBlockTheme = false))

            val result =
                repository.fetchEditorCapabilitiesForSite(atomicSite)

            assertThat(result).isTrue()
            verify(appPrefsWrapper)
                .setSiteSupportsEditorSettings(atomicSite, true)
            verify(appPrefsWrapper)
                .setSiteSupportsEditorAssets(atomicSite, true)
        }

    @Test
    fun `atomic site returns false when authenticated probe also fails`() =
        runTest {
            val atomicSite = SiteModel().apply {
                id = 6
                url = "https://atomic.example.com"
                setIsWPCom(true)
                setIsWPComAtomic(true)
                apiRestUsernamePlain = "user"
                apiRestPasswordPlain = "secret"
            }
            mockDiscoveryFailure(atomicSite.url)
            whenever(
                wpApiClientProvider.getApplicationPasswordClient(atomicSite)
            ).thenReturn(appPasswordClient)
            whenever(
                wpApiClientProvider.getDirectHostApiUrlResolver(atomicSite)
            ).thenReturn(directHostResolver)
            mockApiRootErrorFor(appPasswordClient)
            whenever(themeRepository.fetchCurrentTheme(atomicSite))
                .thenReturn(buildTheme(isBlockTheme = false))

            val result =
                repository.fetchEditorCapabilitiesForSite(atomicSite)

            assertThat(result).isFalse()
            verify(appPrefsWrapper, never())
                .setSiteSupportsEditorSettings(any(), any())
            verify(appPrefsWrapper, never())
                .setSiteSupportsEditorAssets(any(), any())
        }

    private suspend fun mockDiscoveryFailure(siteUrl: String) {
        whenever(wpLoginClient.apiDiscovery(siteUrl))
            .thenReturn(
                ApiDiscoveryResult.FailureParseSiteUrl(
                    ParseUrlException.Generic("")
                )
            )
    }

    private suspend fun mockApiRootResponse(
        hasEditorSettings: Boolean,
        hasEditorAssets: Boolean
    ) = mockApiRootResponseFor(
        client = wpApiClient,
        resolver = apiUrlResolver,
        hasEditorSettings = hasEditorSettings,
        hasEditorAssets = hasEditorAssets,
    )

    private suspend fun mockDiscoverySuccess(
        siteUrl: String,
        hasEditorSettings: Boolean,
        hasEditorAssets: Boolean,
    ) {
        val apiDetails = mock<WpApiDetails>()
        whenever(
            apiDetails.hasRouteForEndpoint(
                directHostResolver,
                "/wp-block-editor/v1",
                "settings"
            )
        ).thenReturn(hasEditorSettings)
        whenever(
            apiDetails.hasRouteForEndpoint(
                directHostResolver,
                "/wpcom/v2",
                "editor-assets"
            )
        ).thenReturn(hasEditorAssets)
        val apiRootUrl = mock<uniffi.wp_api.ParsedUrl>()
        whenever(wpApiClientProvider.urlResolverFor(apiRootUrl))
            .thenReturn(directHostResolver)
        val success = AutoDiscoveryAttemptSuccess(
            parsedSiteUrl = mock(),
            apiRootUrl = apiRootUrl,
            apiDetails = apiDetails,
            authentication = DiscoveredAuthenticationMechanism
                .ApplicationPasswords(mock()),
        )
        whenever(wpLoginClient.apiDiscovery(siteUrl))
            .thenReturn(ApiDiscoveryResult.Success(success))
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mockApiRootResponseFor(
        client: WpApiClient,
        resolver: ApiUrlResolver,
        hasEditorSettings: Boolean,
        hasEditorAssets: Boolean
    ) {
        val apiDetails = mock<WpApiDetails>()
        whenever(
            apiDetails.hasRouteForEndpoint(
                resolver,
                "/wp-block-editor/v1",
                "settings"
            )
        ).thenReturn(hasEditorSettings)
        whenever(
            apiDetails.hasRouteForEndpoint(
                resolver,
                "/wpcom/v2",
                "editor-assets"
            )
        ).thenReturn(hasEditorAssets)

        val response = ApiRootRequestGetResponse(
            data = apiDetails,
            headerMap = mock<WpNetworkHeaderMap>()
        )
        whenever(client.request<Any>(any()))
            .thenReturn(
                WpRequestResult.Success(response)
                    as WpRequestResult<Any>
            )
    }

    private suspend fun mockApiRootError() = mockApiRootErrorFor(wpApiClient)

    @Suppress("UNCHECKED_CAST")
    private suspend fun mockApiRootErrorFor(client: WpApiClient) {
        val error = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "https://test.wordpress.com/wp-json",
            requestMethod = uniffi.wp_api.RequestMethod.GET
        )
        whenever(client.request<Any>(any()))
            .thenReturn(error)
    }

    private suspend fun mockThemeResponse(
        isBlockTheme: Boolean
    ) {
        whenever(
            themeRepository.fetchCurrentTheme(testSite)
        ).thenReturn(buildTheme(isBlockTheme = isBlockTheme))
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
        description = ThemeDescription(raw = "", rendered = ""),
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
