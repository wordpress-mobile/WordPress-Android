package org.wordpress.android.ui.rs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.rs.contentlist.ContentItemUiModel
import org.wordpress.android.ui.rs.data.RsSiteRestClient

/**
 * Author display names for the rows of an rs list, looked up only on sites where more than one user
 * has published [postType] - on a single-author site every row would carry the same name. The caller
 * applies the result, because pages wrap their models in tree items and posts don't.
 */
internal class RsAuthorNames<TAB>(
    private val scope: CoroutineScope,
    private val restClient: RsSiteRestClient,
    /** "post" or "page", the post type whose published authors are counted. */
    private val postType: String,
    /** Called on [scope] with whatever resolved. */
    private val onNamesResolved: (TAB, Map<Long, String>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val jobs = mutableMapOf<TAB, Job>()

    /** Asked once and kept for the screen's lifetime; a refresh doesn't change the answer. */
    private var isMultiAuthor: Deferred<Boolean>? = null

    /** Looks up the names [items] still lack, replacing any lookup already running for [tab]. */
    fun resolve(tab: TAB, site: SiteModel, items: List<ContentItemUiModel<*>>) {
        val unresolvedIds = items
            .filter { it.authorId != 0L && it.authorDisplayName == null }
            .map { it.authorId }
            .distinct()
        if (unresolvedIds.isEmpty()) return

        jobs[tab]?.cancel()
        jobs[tab] = scope.launch {
            if (!isMultiAuthor(site).await()) return@launch
            val names = withContext(ioDispatcher) {
                restClient.fetchUserDisplayNames(site, unresolvedIds)
            }
            if (names.isNotEmpty()) onNamesResolved(tab, names)
        }
    }

    /**
     * WP.com sites already know whether they're single-user. Everything else - app-password sites,
     * where that flag is never set - asks the site how many users have published [postType].
     */
    private fun isMultiAuthor(site: SiteModel): Deferred<Boolean> =
        isMultiAuthor ?: scope.async {
            site.isSingleUserSite?.let { !it }
                ?: withContext(ioDispatcher) { restClient.hasMultipleAuthors(site, postType) }
        }.also { isMultiAuthor = it }

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
