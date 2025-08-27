package org.wordpress.android.ui.posts

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.network.UserAgent

@RunWith(MockitoJUnitRunner::class)
@Suppress("LargeClass")
class GutenbergKitSettingsBuilderTest {
    // ===== Plugin Logic Tests =====
    @Mock
    lateinit var appContext: Context

    @Test
    fun `plugins disabled when feature flag is off regardless of site configuration`() {
        val testCases = listOf(
            // isWPCom, isJetpackConnected, applicationPassword
            Triple(true, false, null),        // WPCom site
            Triple(false, true, "password"),  // Jetpack with password
            Triple(false, false, null),       // Self-hosted
        )

        testCases.forEach { (isWPCom, isJetpack, password) ->
            val siteConfig = createSiteConfig(
                isWPCom = isWPCom,
                isJetpackConnected = isJetpack,
                apiRestPasswordPlain = password
            )

            val settings = GutenbergKitSettingsBuilder.buildSettings(
                siteConfig = siteConfig,
                postConfig = createPostConfig(),
                appConfig = createAppConfig(),

                featureConfig = createFeatureConfig(), // Both features disabled
            )

            assertThat(settings["plugins"])
                .withFailMessage("Expected plugins=false for WPCom=$isWPCom, Jetpack=$isJetpack, password=$password")
                .isEqualTo(false)
        }
    }

    @Test
    fun `plugins enabled for WPCom sites when feature flag is on`() {
        val siteConfig = createSiteConfig(isWPCom = true)

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),

            featureConfig = createFeatureConfig(isPluginsFeatureEnabled = true),

        )

        assertThat(settings["plugins"]).isEqualTo(true)
    }

    @Test
    fun `plugins enabled for Jetpack sites with application password when feature flag is on`() {
        val siteConfig = createSiteConfig(
            isWPCom = false,
            isJetpackConnected = true,
            apiRestPasswordPlain = "validPassword123"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),

            featureConfig = createFeatureConfig(isPluginsFeatureEnabled = true),

        )

        assertThat(settings["plugins"]).isEqualTo(true)
    }

    @Test
    fun `plugins disabled for Jetpack sites without application password`() {
        val passwordVariants = listOf(null, "")

        passwordVariants.forEach { password ->
            val siteConfig = createSiteConfig(
                isWPCom = false,
                isJetpackConnected = true,
                apiRestPasswordPlain = password
            )

            val settings = GutenbergKitSettingsBuilder.buildSettings(
                siteConfig = siteConfig,
                postConfig = createPostConfig(),
                appConfig = createAppConfig(),

                featureConfig = createFeatureConfig(isPluginsFeatureEnabled = true),

            )

            assertThat(settings["plugins"])
                .withFailMessage("Expected plugins=false for password=$password")
                .isEqualTo(false)
        }
    }

    @Test
    fun `plugins disabled for self-hosted sites without Jetpack`() {
        val siteConfig = createSiteConfig(
            isWPCom = false,
            isJetpackConnected = false,
            apiRestPasswordPlain = "password" // Has password but no Jetpack
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),

            featureConfig = createFeatureConfig(isPluginsFeatureEnabled = true),

        )

        assertThat(settings["plugins"]).isEqualTo(false)
    }

    // ===== Authentication Flow Tests =====

    @Test
    fun `WPCom site uses Bearer token and public API`() {
        val siteConfig = createSiteConfig(
            url = "https://example.wordpress.com",
            siteId = 123,
            isWPCom = true,
            isUsingWpComRestApi = true
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(accessToken = "test_bearer_token"),

            featureConfig = createFeatureConfig(),

        )

        assertThat(settings["authHeader"]).isEqualTo("Bearer test_bearer_token")
        assertThat(settings["siteApiRoot"]).isEqualTo("https://public-api.wordpress.com/")
        assertThat(settings["siteApiNamespace"] as Array<*>)
            .containsExactly("sites/123/", "sites/example.wordpress.com/")
    }

    @Test
    fun `Jetpack site with application password uses Basic auth and site API`() {
        val siteConfig = createSiteConfig(
            url = "https://mysite.com",
            siteId = 789,
            isJetpackConnected = true,
            wpApiRestUrl = "https://mysite.com/wp-json/",
            apiRestUsernamePlain = "testuser",
            apiRestPasswordPlain = "testpass123"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(accessToken = "unused_token"),

            featureConfig = createFeatureConfig(),

        )

        assertThat(settings["authHeader"] as String).startsWith("Basic ")
        assertThat(settings["siteApiRoot"]).isEqualTo("https://mysite.com/wp-json/")
        assertThat(settings["siteApiNamespace"] as Array<*>).isEmpty()
    }

    @Test
    fun `Jetpack site without password falls back to Bearer when WPCom REST available`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            isUsingWpComRestApi = true,
            apiRestPasswordPlain = null
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(accessToken = "fallback_token"),

            featureConfig = createFeatureConfig(),

        )

        assertThat(settings["authHeader"]).isEqualTo("Bearer fallback_token")
        assertThat(settings["siteApiRoot"]).isEqualTo("https://public-api.wordpress.com/")
    }

    // ===== Authentication Edge Cases Tests =====

    @Test
    fun `WPCom site with null access token returns null auth header`() {
        val siteConfig = createSiteConfig(
            isWPCom = true,
            isUsingWpComRestApi = true
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(accessToken = null),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `WPCom site with empty access token returns null auth header`() {
        val siteConfig = createSiteConfig(
            isWPCom = true,
            isUsingWpComRestApi = true
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(accessToken = ""),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `Basic auth with null username returns null auth header`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = null,
            apiRestPasswordPlain = "password123"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `Basic auth with empty username returns null auth header`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = "",
            apiRestPasswordPlain = "password123"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `Basic auth with null password returns null auth header`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = "username",
            apiRestPasswordPlain = null
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `Basic auth with empty password returns null auth header`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = "username",
            apiRestPasswordPlain = ""
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `Basic auth with both username and password empty returns null auth header`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = "",
            apiRestPasswordPlain = ""
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isNull()
    }

    @Test
    fun `Valid WPCom authentication returns proper Bearer header`() {
        val siteConfig = createSiteConfig(
            isWPCom = true,
            isUsingWpComRestApi = true
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(accessToken = "valid_token_123"),
            featureConfig = createFeatureConfig()
        )

        assertThat(settings["authHeader"]).isEqualTo("Bearer valid_token_123")
    }

    @Test
    fun `Valid Basic auth returns proper Basic header`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = "testuser",
            apiRestPasswordPlain = "testpass"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        val authHeader = settings["authHeader"] as String?
        assertThat(authHeader).isNotNull()
        assertThat(authHeader).startsWith("Basic ")
        // Verify it's a valid Base64 encoded string
        val encodedPart = authHeader?.removePrefix("Basic ")
        assertThat(encodedPart).isNotEmpty()
    }

    @Test
    fun `Special characters in Basic auth credentials are handled correctly`() {
        val siteConfig = createSiteConfig(
            isJetpackConnected = true,
            apiRestUsernamePlain = "user@example.com",
            apiRestPasswordPlain = "p@ss:word!123"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),
            featureConfig = createFeatureConfig()
        )

        val authHeader = settings["authHeader"] as String?
        assertThat(authHeader).isNotNull()
        assertThat(authHeader).startsWith("Basic ")
    }

    // ===== Complete Scenario Tests =====

    @Test
    fun `complete settings for WPCom simple site with all features enabled`() {
        val siteConfig = GutenbergKitSettingsBuilder.SiteConfig(
            url = "https://example.wordpress.com",
            siteId = 123,
            isWPCom = true,
            isWPComAtomic = false,
            isJetpackConnected = false,
            isUsingWpComRestApi = true,
            wpApiRestUrl = null,
            apiRestUsernamePlain = null,
            apiRestPasswordPlain = null,
            selfHostedSiteId = 0,
            webEditor = "gutenberg",
            apiRestUsernameProcessed = null,
            apiRestPasswordProcessed = null
        )

        val postConfig = GutenbergKitSettingsBuilder.PostConfig(
            remotePostId = 456L,
            isPage = false,
            title = "Test Post",
            content = "Test Content"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = postConfig,
            appConfig = createAppConfig(
                accessToken = "test_token",
                cookies = "test_cookies"
            ),
            featureConfig = createFeatureConfig(
                isPluginsFeatureEnabled = true,
                isThemeStylesFeatureEnabled = true
            )
        )

        // Verify all settings are correctly configured
        assertThat(settings["postId"]).isEqualTo(456)
        assertThat(settings["postType"]).isEqualTo("post")
        assertThat(settings["postTitle"]).isEqualTo("Test Post")
        assertThat(settings["postContent"]).isEqualTo("Test Content")
        assertThat(settings["siteURL"]).isEqualTo("https://example.wordpress.com")
        assertThat(settings["authHeader"]).isEqualTo("Bearer test_token")
        assertThat(settings["siteApiRoot"]).isEqualTo("https://public-api.wordpress.com/")
        assertThat(settings["plugins"]).isEqualTo(true) // WPCom with feature enabled
        assertThat(settings["themeStyles"]).isEqualTo(true)
        assertThat(settings["locale"]).isEqualTo("en-us")
        assertThat(settings["cookies"]).isEqualTo("test_cookies")
    }

    @Test
    fun `complete settings for Jetpack site with application password`() {
        val siteConfig = GutenbergKitSettingsBuilder.SiteConfig(
            url = "https://jetpack-site.com",
            siteId = 999,
            isWPCom = false,
            isWPComAtomic = false,
            isJetpackConnected = true,
            isUsingWpComRestApi = false,
            wpApiRestUrl = "https://jetpack-site.com/wp-json/",
            apiRestUsernamePlain = "admin",
            apiRestPasswordPlain = "securepass",
            selfHostedSiteId = 999,
            webEditor = "gutenberg",
            apiRestUsernameProcessed = "admin",
            apiRestPasswordProcessed = "securepass"
        )

        val postConfig = GutenbergKitSettingsBuilder.PostConfig(
            remotePostId = 100L,
            isPage = true,
            title = "Test Page",
            content = "Page Content"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = postConfig,
            appConfig = createAppConfig(
                accessToken = "unused",
                locale = "fr_FR"
            ),
            featureConfig = createFeatureConfig(isPluginsFeatureEnabled = true)
        )

        assertThat(settings["postType"]).isEqualTo("page")
        assertThat(settings["authHeader"] as String).startsWith("Basic ")
        assertThat(settings["siteApiRoot"]).isEqualTo("https://jetpack-site.com/wp-json/")
        assertThat(settings["siteApiNamespace"] as Array<*>).isEmpty()
        assertThat(settings["plugins"]).isEqualTo(true) // Jetpack with password and feature enabled
        assertThat(settings["locale"]).isEqualTo("fr-fr")
    }

    @Test
    fun `locale transformation handles underscores correctly`() {
        val testCases = mapOf(
            "en_US" to "en-us",
            "fr_FR" to "fr-fr",
            "de_DE" to "de-de",
            "es_ES" to "es-es",
            "pt_BR" to "pt-br"
        )

        testCases.forEach { (input, expected) ->
            val settings = GutenbergKitSettingsBuilder.buildSettings(
                siteConfig = createSiteConfig(),
                postConfig = createPostConfig(),
                appConfig = createAppConfig(locale = input),
                featureConfig = createFeatureConfig()
            )

            assertThat(settings["locale"])
                .withFailMessage("Expected $input to transform to $expected")
                .isEqualTo(expected)
        }
    }

    @Test
    fun `feature flags control themeStyles and plugins independently`() {
        val siteConfig = createSiteConfig(isWPCom = true)

        // Test all combinations
        val flagCombinations = listOf(
            Triple(false, false, Pair(false, false)),
            Triple(false, true, Pair(false, true)),
            Triple(true, false, Pair(true, false)),
            Triple(true, true, Pair(true, true))
        )

        flagCombinations.forEach { (plugins, themes, expected) ->
            val settings = GutenbergKitSettingsBuilder.buildSettings(
                siteConfig = siteConfig,
                postConfig = createPostConfig(),
                appConfig = createAppConfig(),

                featureConfig = createFeatureConfig(
                    isPluginsFeatureEnabled = plugins,
                    isThemeStylesFeatureEnabled = themes
                ),
            )

            assertThat(settings["plugins"]).isEqualTo(expected.first)
            assertThat(settings["themeStyles"]).isEqualTo(expected.second)
        }
    }

    @Test
    fun `self-hosted site uses correct API endpoint when wpApiRestUrl is null`() {
        val siteConfig = createSiteConfig(
            url = "https://selfhosted.org",
            wpApiRestUrl = null,
            apiRestPasswordPlain = "password"
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),

            featureConfig = createFeatureConfig(),

        )

        assertThat(settings["siteApiRoot"]).isEqualTo("https://selfhosted.org/wp-json/")
    }

    @Test
    fun `namespaceExcludedPaths is always included`() {
        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = createSiteConfig(),
            postConfig = createPostConfig(),
            appConfig = createAppConfig(),

            featureConfig = createFeatureConfig(),

        )

        val excludedPaths = settings["namespaceExcludedPaths"] as Array<*>
        assertThat(excludedPaths).containsExactly(
            "/wpcom/v2/following/recommendations",
            "/wpcom/v2/following/mine"
        )
    }

    @Test
    fun `null post data is handled correctly`() {
        val postConfig = GutenbergKitSettingsBuilder.PostConfig(
            remotePostId = null,
            isPage = false,
            title = null,
            content = null
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = createSiteConfig(),
            postConfig = postConfig,
            appConfig = createAppConfig(),

            featureConfig = createFeatureConfig(),

        )

        assertThat(settings["postId"]).isNull()
        assertThat(settings["postTitle"]).isNull()
        assertThat(settings["postContent"]).isNull()
        assertThat(settings["postType"]).isEqualTo("post") // Still defaults to post
    }

    // ===== Helper Methods =====

    private fun createFeatureConfig(
        isPluginsFeatureEnabled: Boolean = false,
        isThemeStylesFeatureEnabled: Boolean = false
    ) = GutenbergKitSettingsBuilder.FeatureConfig(
        isPluginsFeatureEnabled = isPluginsFeatureEnabled,
        isThemeStylesFeatureEnabled = isThemeStylesFeatureEnabled
    )

    private fun createAppConfig(
        accessToken: String? = "token",
        locale: String = "en_US",
        cookies: Any? = null
    ) = GutenbergKitSettingsBuilder.AppConfig(
        accessToken = accessToken,
        locale = locale,
        cookies = cookies,
        accountUserId = 123L,
        accountUserName = "testuser",
        userAgent = UserAgent(appContext = appContext, appName = "foo"),
        isJetpackSsoEnabled = false
    )

    private fun createSiteConfig(
        url: String = "https://test.com",
        siteId: Long = 1,
        isWPCom: Boolean = false,
        isWPComAtomic: Boolean = false,
        isJetpackConnected: Boolean = false,
        isUsingWpComRestApi: Boolean = false,
        wpApiRestUrl: String? = null,
        apiRestUsernamePlain: String? = null,
        apiRestPasswordPlain: String? = null
    ) = GutenbergKitSettingsBuilder.SiteConfig(
        url = url,
        siteId = siteId,
        isWPCom = isWPCom,
        isWPComAtomic = isWPComAtomic,
        isJetpackConnected = isJetpackConnected,
        isUsingWpComRestApi = isUsingWpComRestApi,
        wpApiRestUrl = wpApiRestUrl,
        apiRestUsernamePlain = apiRestUsernamePlain,
        apiRestPasswordPlain = apiRestPasswordPlain,
        selfHostedSiteId = siteId,
        webEditor = "gutenberg",
        apiRestUsernameProcessed = apiRestUsernamePlain,
        apiRestPasswordProcessed = apiRestPasswordPlain
    )

    private fun createPostConfig(
        remotePostId: Long? = 1L,
        isPage: Boolean = false,
        title: String? = "Test",
        content: String? = "Content"
    ) = GutenbergKitSettingsBuilder.PostConfig(
        remotePostId = remotePostId,
        isPage = isPage,
        title = title,
        content = content
    )
}
