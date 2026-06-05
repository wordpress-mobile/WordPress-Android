package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.gutenberg.model.PostTypeDetails
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class GutenbergKitSettingsBuilderTest {
    @Mock
    lateinit var editorCapabilityResolver: EditorCapabilityResolver

    @Mock
    lateinit var perAppLocaleManager: PerAppLocaleManager

    private val builder by lazy {
        GutenbergKitSettingsBuilder(editorCapabilityResolver, perAppLocaleManager)
    }

    @Before
    fun setUp() {
        whenever(editorCapabilityResolver.resolveThemeStyles(any()))
            .thenReturn(EditorCapabilityState.Hidden)
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Hidden)
        whenever(perAppLocaleManager.getCurrentLocale()).thenReturn(Locale.ENGLISH)
    }

    // ===== Auth Header Tests =====

    @Test
    fun `WPCom site returns Bearer token header`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = true,
            accessToken = "my_token",
            username = null,
            password = null
        )

        assertThat(header).isEqualTo("Bearer my_token")
    }

    @Test
    fun `WPCom site with null token returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = true,
            accessToken = null,
            username = null,
            password = null
        )

        assertThat(header).isNull()
    }

    @Test
    fun `WPCom site with empty token returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = true,
            accessToken = "",
            username = null,
            password = null
        )

        assertThat(header).isNull()
    }

    @Test
    fun `self-hosted site returns Basic auth header`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "testuser",
            password = "testpass"
        )

        assertThat(header).isNotNull()
        assertThat(header).startsWith("Basic ")
    }

    @Test
    fun `Basic auth with null username returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = null,
            password = "password123"
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with empty username returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "",
            password = "password123"
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with null password returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "username",
            password = null
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with empty password returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "username",
            password = ""
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with both empty returns null`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "",
            password = ""
        )

        assertThat(header).isNull()
    }

    @Test
    fun `special characters in Basic auth are encoded`() {
        val header = builder.buildAuthHeader(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "user@example.com",
            password = "p@ss:word!123"
        )

        assertThat(header).isNotNull()
        assertThat(header).startsWith("Basic ")
    }

    // ===== Site API Namespace Tests =====

    @Test
    fun `namespace is empty for non-WPCom sites`() {
        val result = builder.buildSiteApiNamespace(
            shouldUseWPComRestApi = false,
            siteId = 123L,
            siteUrl = "https://example.com"
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `namespace includes site ID and host for WPCom sites`() {
        val result = builder.buildSiteApiNamespace(
            shouldUseWPComRestApi = true,
            siteId = 456L,
            siteUrl = "https://example.wordpress.com"
        )

        assertThat(result).containsExactly(
            "sites/456/",
            "sites/example.wordpress.com/"
        )
    }

    @Test
    fun `namespace includes only site ID when host extraction fails`() {
        val result = builder.buildSiteApiNamespace(
            shouldUseWPComRestApi = true,
            siteId = 789L,
            siteUrl = "not a valid url"
        )

        assertThat(result).containsExactly("sites/789/")
    }

    @Test
    fun `namespace uses host alias when URL is schemeless`() {
        val result = builder.buildSiteApiNamespace(
            shouldUseWPComRestApi = true,
            siteId = 456L,
            siteUrl = "example.wordpress.com"
        )

        assertThat(result).containsExactly(
            "sites/456/",
            "sites/example.wordpress.com/"
        )
    }

    // ===== Extract Host Tests =====

    @Test
    fun `extractHost returns host from valid URL`() {
        assertThat(
            builder.extractHost(
                "https://example.wordpress.com"
            )
        ).isEqualTo("example.wordpress.com")
    }

    @Test
    fun `extractHost returns null for blank input`() {
        assertThat(builder.extractHost("")).isNull()
        assertThat(builder.extractHost("   ")).isNull()
    }

    @Test
    fun `extractHost returns null for URL with whitespace`() {
        assertThat(
            builder.extractHost("not a url")
        ).isNull()
    }

    @Test
    fun `extractHost strips path from URL`() {
        assertThat(
            builder.extractHost(
                "https://example.com/blog/page"
            )
        ).isEqualTo("example.com")
    }

    @Test
    fun `extractHost handles schemeless host`() {
        assertThat(
            builder.extractHost("example.wordpress.com")
        ).isEqualTo("example.wordpress.com")
    }

    @Test
    fun `extractHost handles schemeless host with path`() {
        assertThat(
            builder.extractHost("example.com/blog")
        ).isEqualTo("example.com")
    }

    @Test
    fun `extractHost strips port from URL`() {
        assertThat(
            builder.extractHost("https://example.com:8080/foo")
        ).isEqualTo("example.com")
    }

    @Test
    fun `extractHost strips userinfo from URL`() {
        assertThat(
            builder.extractHost("https://user:pass@example.com/")
        ).isEqualTo("example.com")
    }

    // ===== buildPostConfiguration Tests =====

    // --- WPCom site configuration ---

    @Test
    fun `WPCom site uses WPCom API root`() {
        val config = buildWPComConfig()

        assertThat(config.siteApiRoot)
            .isEqualTo("https://public-api.wordpress.com/")
    }

    @Test
    fun `WPCom site sets Bearer auth header`() {
        val config = buildWPComConfig(accessToken = "wpcom_token")

        assertThat(config.authHeader)
            .isEqualTo("Bearer wpcom_token")
    }

    @Test
    fun `WPCom site sets site API namespace with ID and host`() {
        val config = buildWPComConfig(
            siteUrl = "https://mysite.wordpress.com",
            siteId = 42L
        )

        assertThat(config.siteApiNamespace).containsExactly(
            "sites/42/",
            "sites/mysite.wordpress.com/"
        )
    }

    @Test
    fun `WPCom site sets editor assets endpoint when plugins available`() {
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Available(userEnabled = true))

        val config = buildWPComConfig(siteId = 100L)

        assertThat(config.editorAssetsEndpoint).isEqualTo(
            "https://public-api.wordpress.com/" +
                "wpcom/v2/sites/100/editor-assets"
        )
    }

    @Test
    fun `editor assets endpoint is null when plugins unsupported`() {
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing))

        val config = buildWPComConfig(siteId = 100L)

        assertThat(config.editorAssetsEndpoint).isNull()
    }

    @Test
    fun `editor assets endpoint is null when plugins hidden`() {
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Hidden)

        val config = buildWPComConfig(siteId = 100L)

        assertThat(config.editorAssetsEndpoint).isNull()
    }

    @Test
    fun `WPCom site with missing token uses empty auth header`() {
        val config = buildWPComConfig(accessToken = null)

        assertThat(config.authHeader).isEmpty()
    }

    // --- Self-hosted site configuration ---

    @Test
    fun `self-hosted site uses wpApiRestUrl as API root`() {
        val config = buildSelfHostedConfig(
            wpApiRestUrl = "https://mysite.com/wp-json/"
        )

        assertThat(config.siteApiRoot)
            .isEqualTo("https://mysite.com/wp-json/")
    }

    @Test
    fun `self-hosted site falls back to siteUrl wp-json when no REST URL`() {
        val config = buildSelfHostedConfig(
            siteUrl = "https://mysite.com",
            wpApiRestUrl = null
        )

        assertThat(config.siteApiRoot)
            .isEqualTo("https://mysite.com/wp-json/")
    }

    @Test
    fun `self-hosted site sets Basic auth header`() {
        val config = buildSelfHostedConfig(
            applicationPassword = "app_pass",
            apiRestUsername = "admin"
        )

        assertThat(config.authHeader).startsWith("Basic ")
    }

    @Test
    fun `self-hosted site has empty namespace`() {
        val config = buildSelfHostedConfig()

        assertThat(config.siteApiNamespace).isEmpty()
    }

    @Test
    fun `self-hosted site builds editor assets endpoint from API root`() {
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Available(userEnabled = true))

        val config = buildSelfHostedConfig()

        assertThat(config.editorAssetsEndpoint).isEqualTo(
            "https://mysite.com/wp-json/wpcom/v2/editor-assets"
        )
    }

    // --- Application password overrides WPCom REST API ---

    @Test
    fun `app password forces non-WPCom API even if site uses WPCom REST`() {
        val site = SiteModel().apply {
            url = "https://mysite.com"
            siteId = 123L
            setIsWPCom(false)
            setIsJetpackConnected(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
            wpApiRestUrl = "https://mysite.com/wp-json/"
            apiRestPasswordPlain = "app_pass"
            apiRestUsernamePlain = "admin"
        }
        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "wpcom_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
        )

        assertThat(config.siteApiRoot)
            .isEqualTo("https://mysite.com/wp-json/")
        assertThat(config.authHeader).startsWith("Basic ")
        assertThat(config.siteApiNamespace).isEmpty()
    }

    // --- Post configuration ---

    @Test
    fun `post type is post by default`() {
        val config = buildWPComConfig()

        assertThat(config.postType).isEqualTo(PostTypeDetails.post)
    }

    @Test
    fun `null post title becomes empty string`() {
        val config = buildWPComConfig()

        assertThat(config.title).isEmpty()
    }

    @Test
    fun `null post content becomes empty string`() {
        val config = buildWPComConfig()

        assertThat(config.content).isEmpty()
    }

    @Test
    fun `null remote ID results in null post ID`() {
        val config = buildWPComConfig()

        assertThat(config.postId).isNull()
    }

    @Test
    fun `local draft post results in null post ID`() {
        val site = SiteModel().apply {
            url = "https://example.wordpress.com"
            siteId = 123L
            setIsWPCom(true)
            setIsJetpackConnected(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val post = PostModel().apply {
            setIsLocalDraft(true)
            setRemotePostId(99L)
        }
        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "test_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
            post = post,
        )

        assertThat(config.postId).isNull()
    }

    @Test
    fun `null post status defaults to draft`() {
        val config = buildWPComConfig()

        assertThat(config.postStatus).isEqualTo("draft")
    }

    // --- Asset caching ---

    @Test
    fun `asset caching is always enabled`() {
        val config = buildWPComConfig()

        assertThat(config.enableAssetCaching).isTrue()
    }

    @Test
    fun `cached hosts includes s0 wp com and site host`() {
        val config = buildWPComConfig(
            siteUrl = "https://mysite.wordpress.com"
        )

        assertThat(config.cachedAssetHosts).containsExactlyInAnyOrder(
            "s0.wp.com",
            "mysite.wordpress.com"
        )
    }

    @Test
    fun `cached hosts includes schemeless site host`() {
        val config = buildWPComConfig(siteUrl = "shieldeyesfromlight.wordpress.com")

        assertThat(config.cachedAssetHosts).containsExactlyInAnyOrder(
            "s0.wp.com",
            "shieldeyesfromlight.wordpress.com"
        )
    }

    // --- Namespace excluded paths ---

    @Test
    fun `namespace excluded paths are always set`() {
        val config = buildWPComConfig()

        assertThat(config.namespaceExcludedPaths).containsExactly(
            "/wpcom/v2/following/recommendations",
            "/wpcom/v2/following/mine"
        )
    }

    // --- Site URL passthrough ---

    @Test
    fun `site URL is passed through to configuration`() {
        val config = buildWPComConfig(
            siteUrl = "https://example.wordpress.com"
        )

        assertThat(config.siteURL)
            .isEqualTo("https://example.wordpress.com")
    }

    // ===== buildCachedHosts (via buildPostConfiguration) =====

    @Test
    fun `cached hosts includes site host for subdirectory URL`() {
        val config = buildWPComConfig(
            siteUrl = "https://example.com/blog"
        )

        assertThat(config.cachedAssetHosts).containsExactlyInAnyOrder(
            "s0.wp.com",
            "example.com"
        )
    }

    @Test
    fun `cached hosts only includes s0 wp com for empty URL`() {
        val config = buildWPComConfig(siteUrl = "")

        assertThat(config.cachedAssetHosts)
            .containsExactly("s0.wp.com")
    }

    // ===== buildSiteApiNamespace edge cases =====

    @Test
    fun `namespace with empty URL returns only site ID`() {
        val result = builder.buildSiteApiNamespace(
            shouldUseWPComRestApi = true,
            siteId = 321L,
            siteUrl = ""
        )

        assertThat(result).containsExactly("sites/321/")
    }

    // ===== Post type and ID edge cases =====

    @Test
    fun `page post results in page post type`() {
        val site = SiteModel().apply {
            url = "https://example.wordpress.com"
            siteId = 123L
            setIsWPCom(true)
            setIsJetpackConnected(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val post = PostModel().apply {
            setIsPage(true)
        }
        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "test_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
            post = post,
        )

        assertThat(config.postType).isEqualTo(PostTypeDetails.page)
    }

    @Test
    fun `published post sets remote post ID`() {
        val site = SiteModel().apply {
            url = "https://example.wordpress.com"
            siteId = 123L
            setIsWPCom(true)
            setIsJetpackConnected(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val post = PostModel().apply {
            setIsLocalDraft(false)
            setRemotePostId(42L)
        }
        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "test_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
            post = post,
        )

        assertThat(config.postId).isEqualTo(42u)
    }

    // ===== Per-call values propagate through =====

    @Test
    fun `current device locale passes through to configuration`() {
        whenever(perAppLocaleManager.getCurrentLocale())
            .thenReturn(Locale.forLanguageTag("pt-BR"))

        val config = buildWPComConfig()

        assertThat(config.locale).isEqualTo("pt-br")
    }

    @Test
    fun `cookies pass through to configuration`() {
        val cookies = mapOf("wp_session" to "abc123", "wordpress_logged_in" to "xyz")

        val config = buildWPComConfig(cookies = cookies)

        assertThat(config.cookies).isEqualTo(cookies)
    }

    @Test
    fun `network logging flag passes through to configuration`() {
        val config = buildWPComConfig(isNetworkLoggingEnabled = true)

        assertThat(config.enableNetworkLogging).isTrue()
    }

    // ===== Capability resolver integration =====

    @Test
    fun `themeStyles reflects resolver result`() {
        whenever(editorCapabilityResolver.resolveThemeStyles(any()))
            .thenReturn(EditorCapabilityState.Available(userEnabled = true))

        val config = buildWPComConfig()

        assertThat(config.themeStyles).isTrue()
    }

    @Test
    fun `themeStyles is false when resolver hides the capability`() {
        whenever(editorCapabilityResolver.resolveThemeStyles(any()))
            .thenReturn(EditorCapabilityState.Hidden)

        val config = buildWPComConfig()

        assertThat(config.themeStyles).isFalse()
    }

    @Test
    fun `plugins reflects resolver result`() {
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Available(userEnabled = true))

        val config = buildWPComConfig()

        assertThat(config.plugins).isTrue()
    }

    @Test
    fun `plugins is false when resolver hides the capability`() {
        whenever(editorCapabilityResolver.resolveThirdPartyBlocks(any()))
            .thenReturn(EditorCapabilityState.Hidden)

        val config = buildWPComConfig()

        assertThat(config.plugins).isFalse()
    }

    // ===== Helpers =====

    private fun buildWPComConfig(
        siteUrl: String = "https://example.wordpress.com",
        siteId: Long = 123L,
        accessToken: String? = "test_token",
        cookies: Map<String, String> = emptyMap(),
        isNetworkLoggingEnabled: Boolean = false,
    ): org.wordpress.gutenberg.model.EditorConfiguration {
        val site = SiteModel().apply {
            url = siteUrl
            this.siteId = siteId
            setIsWPCom(true)
            setIsJetpackConnected(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        return builder.buildPostConfiguration(
            site = site,
            accessToken = accessToken,
            cookies = cookies,
            isNetworkLoggingEnabled = isNetworkLoggingEnabled,
        )
    }

    private fun buildSelfHostedConfig(
        siteUrl: String = "https://mysite.com",
        wpApiRestUrl: String? = "https://mysite.com/wp-json/",
        applicationPassword: String? = "app_pass",
        apiRestUsername: String? = "admin"
    ): org.wordpress.gutenberg.model.EditorConfiguration {
        val site = SiteModel().apply {
            url = siteUrl
            siteId = 999L
            setIsWPCom(false)
            setIsJetpackConnected(false)
            this.wpApiRestUrl = wpApiRestUrl
            apiRestPasswordPlain = applicationPassword
            apiRestUsernamePlain = apiRestUsername
        }
        return builder.buildPostConfiguration(
            site = site,
            accessToken = null,
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
        )
    }
}
