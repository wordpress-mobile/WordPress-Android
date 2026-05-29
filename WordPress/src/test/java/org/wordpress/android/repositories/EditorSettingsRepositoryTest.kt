package org.wordpress.android.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import uniffi.wp_api.ThemeAuthor
import uniffi.wp_api.ThemeAuthorUri
import uniffi.wp_api.ThemeDescription
import uniffi.wp_api.ThemeName
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeStylesheet
import uniffi.wp_api.ThemeTags
import uniffi.wp_api.ThemeUri
import uniffi.wp_api.ThemeWithEditContext
import java.io.IOException

@ExperimentalCoroutinesApi
class EditorSettingsRepositoryTest : BaseUnitTest() {
    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var themeRepository: ThemeRepository

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var okHttpClient: OkHttpClient

    private lateinit var repository: EditorSettingsRepository

    private val wpComSite = SiteModel().apply {
        id = 1
        siteId = 42L
        url = "https://example.wordpress.com"
        setIsWPCom(true)
        setIsJetpackConnected(false)
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    private val atomicSite = SiteModel().apply {
        id = 5
        siteId = 777L
        url = "https://atomic.example.com"
        setIsWPCom(true)
        setIsWPComAtomic(true)
        setIsJetpackConnected(false)
        origin = SiteModel.ORIGIN_WPCOM_REST
        wpApiRestUrl = "https://atomic.example.com/wp-json/"
    }

    private val selfHostedSite = SiteModel().apply {
        id = 2
        siteId = 0L
        url = "https://mysite.com"
        setIsWPCom(false)
        setIsJetpackConnected(false)
        wpApiRestUrl = "https://mysite.com/wp-json/"
        apiRestUsernamePlain = "admin"
        apiRestPasswordPlain = "app_pass"
    }

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("wpcom_token")
        repository = EditorSettingsRepository(
            themeRepository = themeRepository,
            appPrefsWrapper = appPrefsWrapper,
            accountStore = accountStore,
            editorAuthHeaderBuilder = EditorAuthHeaderBuilder(),
            okHttpClient = okHttpClient,
            ioDispatcher = testDispatcher(),
        )
    }

    // ===== Probe outcomes =====

    @Test
    fun `2xx on both endpoints persists true and returns true`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isTrue()
        verify(appPrefsWrapper)
            .setSiteSupportsEditorSettings(wpComSite, true)
        verify(appPrefsWrapper)
            .setSiteSupportsEditorAssets(wpComSite, true)
    }

    @Test
    fun `404 persists false but still returns true (definitive answer)`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 404,
            "wpcom/v2" to 404,
        )
        mockTheme(isBlockTheme = false)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isTrue()
        verify(appPrefsWrapper)
            .setSiteSupportsEditorSettings(wpComSite, false)
        verify(appPrefsWrapper)
            .setSiteSupportsEditorAssets(wpComSite, false)
    }

    @Test
    fun `mixed 200 and 404 persists per-endpoint`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 404,
        )
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isTrue()
        verify(appPrefsWrapper)
            .setSiteSupportsEditorSettings(wpComSite, true)
        verify(appPrefsWrapper)
            .setSiteSupportsEditorAssets(wpComSite, false)
    }

    @Test
    fun `5xx response leaves pref untouched and returns false`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 500,
            "wpcom/v2" to 500,
        )
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isFalse()
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorSettings(any(), any())
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorAssets(any(), any())
    }

    @Test
    fun `network IOException leaves pref untouched and returns false`() = runTest {
        whenever(okHttpClient.newCall(any())).thenAnswer {
            mock<Call>().also { call ->
                whenever(call.enqueue(any())).thenAnswer { callInvocation ->
                    val callback = callInvocation.arguments[0] as Callback
                    callback.onFailure(call, IOException("offline"))
                }
            }
        }
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isFalse()
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorSettings(any(), any())
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorAssets(any(), any())
    }

    @Test
    fun `missing access token on WPCom site marks probe inconclusive`() = runTest {
        whenever(accountStore.accessToken).thenReturn(null)
        mockTheme(isBlockTheme = false)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isFalse()
        verify(okHttpClient, never()).newCall(any())
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorSettings(any(), any())
        verify(appPrefsWrapper, never())
            .setSiteSupportsEditorAssets(any(), any())
    }

    // ===== Probe URL and headers =====

    @Test
    fun `WPCom site probes through proxy with sites prefix and Bearer auth`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(wpComSite)

        val requests = captureRequests()
        val urls = requests.map { it.url.toString() }
        assertThat(urls).contains(
            "https://public-api.wordpress.com/wp-block-editor/v1/sites/42/settings",
            "https://public-api.wordpress.com/wpcom/v2/sites/42/editor-assets",
        )
        requests.forEach {
            assertThat(it.header("Authorization"))
                .isEqualTo("Bearer wpcom_token")
        }
    }

    @Test
    fun `self-hosted site probes wpApiRestUrl directly with Basic auth`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(selfHostedSite)

        val requests = captureRequests()
        val urls = requests.map { it.url.toString() }
        assertThat(urls).contains(
            "https://mysite.com/wp-json/wp-block-editor/v1/settings",
            "https://mysite.com/wp-json/wpcom/v2/editor-assets",
        )
        requests.forEach {
            assertThat(it.header("Authorization"))
                .startsWith("Basic ")
        }
    }

    @Test
    fun `self-hosted site without wpApiRestUrl falls back to siteUrl wp-json`() = runTest {
        val site = SiteModel().apply {
            id = 3
            url = "https://fallback.example"
            setIsWPCom(false)
            setIsJetpackConnected(false)
            apiRestUsernamePlain = "admin"
            apiRestPasswordPlain = "app_pass"
            // no wpApiRestUrl set
        }
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(site)

        val urls = captureRequests().map { it.url.toString() }
        assertThat(urls).contains(
            "https://fallback.example/wp-json/wp-block-editor/v1/settings",
            "https://fallback.example/wp-json/wpcom/v2/editor-assets",
        )
    }

    @Test
    fun `app password on WPCom-routed site forces direct probe with Basic auth`() = runTest {
        val site = SiteModel().apply {
            id = 4
            siteId = 99L
            url = "https://atomic.example"
            setIsWPCom(false)
            setIsJetpackConnected(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
            wpApiRestUrl = "https://atomic.example/wp-json/"
            apiRestUsernamePlain = "admin"
            apiRestPasswordPlain = "app_pass"
        }
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(site)

        val requests = captureRequests()
        val urls = requests.map { it.url.toString() }
        assertThat(urls).contains(
            "https://atomic.example/wp-json/wp-block-editor/v1/settings",
            "https://atomic.example/wp-json/wpcom/v2/editor-assets",
        )
        requests.forEach {
            assertThat(it.header("Authorization"))
                .startsWith("Basic ")
        }
    }

    // ===== Atomic direct-host routing (MUST NOT REMOVE — see #22879/#22883) =====

    /**
     * Atomic sites: GutenbergKit fetches `wp-block-editor/v1/settings` from
     * the DIRECT host, bypassing the configured `siteApiRoot`. Capability
     * detection for this endpoint MUST hit the same host. Probing the WP.com
     * proxy here would false-positive on Atomic sites where the proxy
     * advertises a route the direct host doesn't actually serve — the editor
     * would then 404 trying to fetch from the direct host.
     *
     * If this test fails, the production fix from #22883 has regressed.
     * Restore the direct-host routing rather than weakening the assertion.
     */
    @Test
    fun `MUST_NOT_REMOVE - atomic settings probe goes to the direct host, not the proxy`() = runTest {
        stubResponses(
            "atomic.example.com" to 200,
            "public-api.wordpress.com" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(atomicSite)

        val settingsRequests = captureRequests().filter {
            it.url.toString().endsWith("wp-block-editor/v1/settings") ||
                it.url.toString().endsWith("/settings")
        }
        assertThat(settingsRequests).isNotEmpty
        settingsRequests.forEach {
            assertThat(it.url.toString())
                .`as`("Atomic settings probe must hit the direct host")
                .isEqualTo("https://atomic.example.com/wp-json/wp-block-editor/v1/settings")
            assertThat(it.url.host)
                .`as`("Atomic settings probe must NOT go through the WP.com proxy")
                .isNotEqualTo("public-api.wordpress.com")
        }
    }

    @Test
    fun `atomic editor-assets probe still goes through the proxy (only settings is special)`() = runTest {
        stubResponses(
            "atomic.example.com" to 200,
            "public-api.wordpress.com" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(atomicSite)

        val assetsRequests = captureRequests().filter {
            it.url.toString().contains("editor-assets")
        }
        assertThat(assetsRequests).isNotEmpty
        assetsRequests.forEach {
            assertThat(it.url.toString())
                .isEqualTo("https://public-api.wordpress.com/wpcom/v2/sites/777/editor-assets")
        }
    }

    @Test
    fun `non-atomic WPCom site keeps proxy routing for settings`() = runTest {
        // Regression check: only Atomic gets the direct-host override.
        stubResponses(
            "public-api.wordpress.com" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(wpComSite)

        val urls = captureRequests().map { it.url.toString() }
        assertThat(urls).contains(
            "https://public-api.wordpress.com/wp-block-editor/v1/sites/42/settings",
        )
    }

    // ===== 401/403 handling (route exists, auth rejected) =====

    @Test
    fun `401 response is treated as Supported (route exists, auth required)`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 401,
            "wpcom/v2" to 401,
        )
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isTrue()
        verify(appPrefsWrapper)
            .setSiteSupportsEditorSettings(wpComSite, true)
        verify(appPrefsWrapper)
            .setSiteSupportsEditorAssets(wpComSite, true)
    }

    @Test
    fun `403 response is treated as Supported (route exists, lacks permission)`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 403,
            "wpcom/v2" to 403,
        )
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isTrue()
        verify(appPrefsWrapper)
            .setSiteSupportsEditorSettings(wpComSite, true)
        verify(appPrefsWrapper)
            .setSiteSupportsEditorAssets(wpComSite, true)
    }

    // ===== Theme block-style support =====

    @Test
    fun `theme is fetched and persisted independently of probes`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        mockTheme(isBlockTheme = true)

        repository.fetchEditorCapabilitiesForSite(wpComSite)

        verify(appPrefsWrapper)
            .setSiteThemeIsBlockTheme(wpComSite, true)
    }

    @Test
    fun `null theme is not persisted`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        whenever(themeRepository.fetchCurrentTheme(wpComSite))
            .thenReturn(null)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isFalse()
        verify(appPrefsWrapper, never())
            .setSiteThemeIsBlockTheme(any(), any())
    }

    @Test
    fun `theme failure does not block probe persistence`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 200,
            "wpcom/v2" to 200,
        )
        whenever(themeRepository.fetchCurrentTheme(wpComSite))
            .thenThrow(RuntimeException("network error"))

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isFalse()
        verify(appPrefsWrapper)
            .setSiteSupportsEditorSettings(wpComSite, true)
        verify(appPrefsWrapper)
            .setSiteSupportsEditorAssets(wpComSite, true)
    }

    @Test
    fun `probe failure does not block theme persistence`() = runTest {
        stubResponses(
            "wp-block-editor/v1" to 500,
            "wpcom/v2" to 500,
        )
        mockTheme(isBlockTheme = true)

        val ok = repository.fetchEditorCapabilitiesForSite(wpComSite)

        assertThat(ok).isFalse()
        verify(appPrefsWrapper)
            .setSiteThemeIsBlockTheme(wpComSite, true)
    }

    // ===== Cancellation =====

    @Test
    fun `cancelling the probe coroutine cancels in-flight OkHttp calls`() = runTest {
        val capturedCalls = mutableListOf<Call>()
        whenever(okHttpClient.newCall(any())).thenAnswer {
            mock<Call>().also { call ->
                // enqueue() never invokes the callback — the call sits "in flight"
                // until something cancels it.
                whenever(call.enqueue(any())).then { /* no-op */ }
                capturedCalls.add(call)
            }
        }
        mockTheme(isBlockTheme = true)

        val job = launch {
            repository.fetchEditorCapabilitiesForSite(wpComSite)
        }
        advanceUntilIdle()
        job.cancelAndJoin()

        // Cancellation propagated to OkHttp.
        assertThat(capturedCalls).hasSize(2)
        capturedCalls.forEach { verify(it).cancel() }

        // Coroutine actually ended in a cancelled state — not still suspended
        // (a buggy invokeOnCancellation could leak the continuation) and not
        // completed normally (the suspension must have observed cancellation).
        assertThat(job.isCancelled).isTrue
        assertThat(job.isCompleted).isTrue
    }

    // ===== getSupportsEditorSettingsForSite gating =====

    @Test
    fun `getSupports honours probe result of true once probe has run`() {
        whenever(appPrefsWrapper.hasSiteEditorCapabilities(wpComSite))
            .thenReturn(true)
        whenever(appPrefsWrapper.getSiteSupportsEditorSettings(wpComSite))
            .thenReturn(true)

        assertThat(repository.getSupportsEditorSettingsForSite(wpComSite))
            .isTrue()
    }

    @Test
    fun `getSupports honours probe result of false once probe has run`() {
        whenever(appPrefsWrapper.hasSiteEditorCapabilities(wpComSite))
            .thenReturn(true)
        whenever(appPrefsWrapper.getSiteSupportsEditorSettings(wpComSite))
            .thenReturn(false)

        // Probe is authoritative — even if a legacy SQL row would otherwise
        // suggest supported, the probe's `false` wins. This is the fix for
        // sites where the endpoint was once available but no longer is.
        assertThat(repository.getSupportsEditorSettingsForSite(wpComSite))
            .isFalse()
    }

    // ===== Helpers =====

    private fun stubResponses(vararg matches: Pair<String, Int>) {
        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.arguments[0] as Request
            val url = request.url.toString()
            val code = matches.firstOrNull { url.contains(it.first) }?.second
                ?: error("No stubbed response for URL: $url")
            mock<Call>().also { call ->
                whenever(call.enqueue(any())).thenAnswer { callInvocation ->
                    val callback = callInvocation.arguments[0] as Callback
                    callback.onResponse(call, buildResponse(request, code))
                }
            }
        }
    }

    private fun buildResponse(request: Request, code: Int): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body("".toResponseBody(null))
            .build()

    private fun captureRequests(): List<Request> {
        val captor = argumentCaptor<Request>()
        verify(okHttpClient, org.mockito.kotlin.atLeastOnce())
            .newCall(captor.capture())
        return captor.allValues
    }

    private suspend fun mockTheme(isBlockTheme: Boolean) {
        whenever(themeRepository.fetchCurrentTheme(any()))
            .thenReturn(buildTheme(isBlockTheme = isBlockTheme))
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
