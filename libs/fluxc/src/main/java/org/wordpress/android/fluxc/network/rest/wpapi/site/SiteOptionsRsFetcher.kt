package org.wordpress.android.fluxc.network.rest.wpapi.site

import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SiteSettingsCommentStatus
import uniffi.wp_api.SparseSiteSettingsFieldWithEditContext
import uniffi.wp_api.SparseThemeFieldWithViewContext
import uniffi.wp_api.ThemeListParams
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeSupports
import uniffi.wp_api.ThemeSupportsData
import uniffi.wp_api.UniffiWpApiClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The site options a REST-root fetch can't see. Every field is null when this run couldn't determine
 * it -- the endpoint refused the request, the response didn't carry the field, or the site was
 * unreachable -- so callers must read null as "unchanged", never as "off" or "empty".
 */
data class SiteOptions(
    val defaultCommentStatus: String? = null,
    val timezone: String? = null,
    val isFeaturedImageSupported: Boolean? = null
)

/**
 * Reads, over wordpress-rs, the site options that [SiteWPAPIRestClient]'s REST-root fetch leaves out
 * but `wp.getOptions` used to provide.
 *
 * Both reads need a capability the signed-in user may not have -- `manage_options` for
 * `/wp/v2/settings`, and theme access for `/wp/v2/themes` -- and they're requested independently so
 * that one being refused doesn't cost us the other.
 *
 * Note what is *not* here: `software_version` has no REST equivalent at all, because WordPress core
 * deliberately doesn't publish its own version anywhere in the REST API.
 */
@Singleton
class SiteOptionsRsFetcher @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper
) {
    suspend fun fetch(site: SiteModel): SiteOptions {
        // Always the application-password client: getWpApiClient routes a WPCom-flagged row through
        // the WP.com REST proxy, which doesn't serve the site's own settings endpoint.
        val client = wpApiClientProvider.getApplicationPasswordClient(site)
        val settings = fetchSettings(client)
        return SiteOptions(
            defaultCommentStatus = settings?.defaultCommentStatus?.let { commentStatusSlug(it) },
            // The settings endpoint reports an Olson name ("Europe/Madrid"), which the REST root's
            // numeric gmt_offset can't express. SiteUtils.getNormalizedTimezone accepts both, and the
            // Olson name is the DST-correct one -- the numeric offset is what caused CMM-1095.
            timezone = settings?.timezone?.takeIf { it.isNotEmpty() },
            isFeaturedImageSupported = fetchPostThumbnailsSupport(client)
        )
    }

    private suspend fun fetchSettings(client: WpApiClient) = request(client, "site settings") {
        it.siteSettings().filterRetrieveWithEditContext(
            listOf(
                SparseSiteSettingsFieldWithEditContext.DEFAULT_COMMENT_STATUS,
                SparseSiteSettingsFieldWithEditContext.TIMEZONE
            )
        )
    }?.data

    private suspend fun fetchPostThumbnailsSupport(client: WpApiClient): Boolean? {
        val themes = request(client, "active theme supports") {
            it.themes().filterListWithViewContext(
                ThemeListParams(status = ThemeStatus.Active),
                listOf(SparseThemeFieldWithViewContext.THEME_SUPPORTS)
            )
        }?.data ?: return null
        return postThumbnailsSupport(themes.firstOrNull()?.themeSupports)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> request(
        client: WpApiClient,
        what: String,
        block: suspend (UniffiWpApiClient) -> T
    ): T? = try {
        when (val result = client.request(block)) {
            is WpRequestResult.Success -> result.response
            else -> {
                val message = (result as? WpRequestResult.WpError<*>)?.errorMessage
                appLogWrapper.d(AppLog.T.API, "$TAG: couldn't read $what: $message")
                null
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        appLogWrapper.d(AppLog.T.API, "$TAG: couldn't read $what: ${e.message}")
        null
    }

    companion object {
        private const val TAG = "SiteOptionsRsFetcher"
        private const val COMMENT_STATUS_OPEN = "open"
        private const val COMMENT_STATUS_CLOSED = "closed"

        /**
         * Whether the active theme supports featured images. Core sends `post-thumbnails` either as a
         * boolean or as the list of post types it applies to, so both shapes count. An absent key is
         * read as unknown rather than as unsupported: guessing "unsupported" would switch the editor's
         * featured-image controls off, which is the worse way to be wrong.
         */
        internal fun postThumbnailsSupport(
            themeSupports: Map<ThemeSupports, ThemeSupportsData>?
        ): Boolean? = when (val support = themeSupports?.get(ThemeSupports.PostThumbnails)) {
            is ThemeSupportsData.Bool -> support.v1
            is ThemeSupportsData.VecString -> support.v1.isNotEmpty()
            else -> null
        }

        internal fun commentStatusSlug(status: SiteSettingsCommentStatus): String? = when (status) {
            is SiteSettingsCommentStatus.Open -> COMMENT_STATUS_OPEN
            is SiteSettingsCommentStatus.Closed -> COMMENT_STATUS_CLOSED
            is SiteSettingsCommentStatus.Custom -> status.v1.takeIf { it.isNotEmpty() }
            else -> null
        }
    }
}
