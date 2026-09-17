package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.PerAppLocaleManager
import java.util.Locale

/**
 * Covers how the editor picks its REST root and namespace. The two have to agree — a root that
 * already embeds `wp/v2/sites/<id>` paired with an empty namespace double-namespaces every request
 * and 404s with `rest_no_route` (CMM-2383) — so the combinations live together here rather than
 * among the post-configuration tests.
 */
@RunWith(MockitoJUnitRunner::class)
class GutenbergKitSettingsBuilderApiRoutingTest {
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

    /**
     * An Atomic site is WPCom-flagged and does carry an application password, so it takes the
     * direct-host branch. Its root has to be the site's own host: the namespace is empty here, so
     * a WP.com root would leave every path unnamespaced.
     */
    @Test
    fun `atomic site with app password uses its own host as the API root`() {
        val site = SiteModel().apply {
            url = "https://mysite.com"
            siteId = 123L
            setIsWPCom(true)
            setIsWPComAtomic(true)
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

        assertThat(config.siteApiRoot).isEqualTo("https://mysite.com/wp-json/")
        assertThat(config.siteApiNamespace).isEmpty()
        assertThat(config.authHeader).startsWith("Basic ")
    }

    /**
     * Installs predating the migration in `WellSqlConfig` still hold a synthesized WP.com proxy
     * root in `WP_API_REST_URL`. It already embeds `wp/v2/sites/<id>`, so using it with an empty
     * namespace double-namespaces every request — the `rest_no_route` failure in CMM-2383.
     */
    @Test
    fun `stored WPCom proxy root is rejected in favour of the site host`() {
        val site = SiteModel().apply {
            url = "https://mysite.com"
            siteId = 123L
            setIsWPCom(true)
            setIsWPComAtomic(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
            wpApiRestUrl = "https://public-api.wordpress.com/wp/v2/sites/123"
            apiRestPasswordPlain = "app_pass"
            apiRestUsernamePlain = "admin"
        }

        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "wpcom_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
        )

        assertThat(config.siteApiRoot).isEqualTo("https://mysite.com/wp-json/")
        assertThat(config.siteApiNamespace).isEmpty()
    }

    /**
     * `api-fetch`'s `createRootURLMiddleware` strips a path's leading slash before concatenating it
     * onto the root, and `buildEditorAssetsEndpoint` concatenates with no separator, so a root
     * stored without a trailing slash would produce `…/wp-jsonwp/v2/types`. `WpApiClientProvider`
     * builds exactly such slash-less roots, and they reach this column via app-password login.
     */
    @Test
    fun `stored root without a trailing slash gains one`() {
        val site = atomicSiteWithAppPassword(storedRoot = "https://mysite.com/wp-json")

        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "wpcom_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
        )

        assertThat(config.siteApiRoot).isEqualTo("https://mysite.com/wp-json/")
    }

    /**
     * A site without pretty permalinks advertises the `?rest_route=` form, which `api-fetch`
     * handles by switching the path's `?` to `&`. Appending a slash would corrupt it.
     */
    @Test
    fun `stored root with a query string is left alone`() {
        val site = atomicSiteWithAppPassword(
            storedRoot = "https://mysite.com/?rest_route=/"
        )

        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "wpcom_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
        )

        assertThat(config.siteApiRoot).isEqualTo("https://mysite.com/?rest_route=/")
    }

    private fun atomicSiteWithAppPassword(storedRoot: String?) = SiteModel().apply {
        url = "https://mysite.com"
        siteId = 123L
        setIsWPCom(true)
        setIsWPComAtomic(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
        wpApiRestUrl = storedRoot
        apiRestPasswordPlain = "app_pass"
        apiRestUsernamePlain = "admin"
    }

    @Test
    fun `simple site with no stored root still uses the WPCom proxy with a namespace`() {
        val site = SiteModel().apply {
            url = "https://example.wordpress.com"
            siteId = 123L
            setIsWPCom(true)
            setIsWPComAtomic(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }

        val config = builder.buildPostConfiguration(
            site = site,
            accessToken = "wpcom_token",
            cookies = emptyMap(),
            isNetworkLoggingEnabled = false,
        )

        assertThat(config.siteApiRoot).isEqualTo("https://public-api.wordpress.com/")
        assertThat(config.siteApiNamespace).contains("sites/123/")
        assertThat(config.authHeader).startsWith("Bearer ")
    }
}
