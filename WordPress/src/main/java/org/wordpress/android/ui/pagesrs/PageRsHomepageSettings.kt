package org.wordpress.android.ui.pagesrs

import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.SiteSettingsUpdateParams
import javax.inject.Inject

/**
 * Updates a site's homepage settings (page shown on front / page for posts) through the
 * wordpress-rs `/wp/v2/settings` endpoint, so it works on both WP.com and self-hosted
 * application-password sites — unlike FluxC's SiteOptionsStore, which is WP.com-only.
 *
 * Mirrors SiteOptionsStore's semantics: the update is rejected unless the site shows a
 * static page on front, and assigning a page to one slot clears it from the other (core's
 * settings endpoint does not auto-clear). Current values are read live from the server
 * rather than from [SiteModel], whose homepage fields aren't reliably populated for
 * self-hosted sites. On success the shared [SiteModel] is updated in place and
 * re-dispatched so the rest of the app (virtual rows, My Site, the legacy list) stays
 * in sync.
 */
internal class PageRsHomepageSettings @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val dispatcher: Dispatcher,
) {
    sealed interface Result {
        data object Success : Result

        /** The site's theme shows latest posts on front, so homepage pages can't be set. */
        data object StaticHomepageDisabled : Result

        data class Error(val message: String?) : Result
    }

    suspend fun setHomepage(site: SiteModel, pageId: Long): Result =
        update(site, HomepageTarget.PAGE_ON_FRONT, pageId)

    suspend fun setPostsPage(site: SiteModel, pageId: Long): Result =
        update(site, HomepageTarget.PAGE_FOR_POSTS, pageId)

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private suspend fun update(site: SiteModel, target: HomepageTarget, pageId: Long): Result {
        try {
            val client = wpApiClientProvider.getWpApiClient(site)

            val current = when (
                val response = client.request { it.siteSettings().retrieveWithEditContext() }
            ) {
                is WpRequestResult.Success -> response.response.data
                else -> return response.toError()
            }

            val params = computeHomepageUpdateParams(
                showOnFront = current.showOnFront,
                currentPageOnFront = current.pageOnFront.toLong(),
                currentPageForPosts = current.pageForPosts.toLong(),
                target = target,
                pageId = pageId
            ) ?: return Result.StaticHomepageDisabled

            val updated = when (
                val response = client.request { it.siteSettings().update(params) }
            ) {
                is WpRequestResult.Success -> response.response.data
                else -> return response.toError()
            }

            site.pageOnFront = updated.pageOnFront.toLong()
            site.pageForPosts = updated.pageForPosts.toLong()
            site.showOnFront = updated.showOnFront
            dispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site))
            return Result.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.Error(e.message)
        }
    }

    private fun WpRequestResult<*>.toError() = Result.Error(
        (this as? WpRequestResult.WpError<*>)?.errorMessage
    )
}

internal enum class HomepageTarget { PAGE_ON_FRONT, PAGE_FOR_POSTS }

/**
 * Builds the settings update for assigning [pageId] to [target], or returns null when the
 * site doesn't show a static page on front. Both homepage fields are always sent: the page
 * being assigned, and the other slot — cleared when it currently holds the same page.
 */
internal fun computeHomepageUpdateParams(
    showOnFront: String,
    currentPageOnFront: Long,
    currentPageForPosts: Long,
    target: HomepageTarget,
    pageId: Long
): SiteSettingsUpdateParams? {
    if (showOnFront != SHOW_ON_FRONT_PAGE) return null
    return when (target) {
        HomepageTarget.PAGE_ON_FRONT -> SiteSettingsUpdateParams(
            pageOnFront = pageId.toULong(),
            pageForPosts = (if (currentPageForPosts == pageId) 0L else currentPageForPosts).toULong()
        )
        HomepageTarget.PAGE_FOR_POSTS -> SiteSettingsUpdateParams(
            pageOnFront = (if (currentPageOnFront == pageId) 0L else currentPageOnFront).toULong(),
            pageForPosts = pageId.toULong()
        )
    }
}

private const val SHOW_ON_FRONT_PAGE = "page"
