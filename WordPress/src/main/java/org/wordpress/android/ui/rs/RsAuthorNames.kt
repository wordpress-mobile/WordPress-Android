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
 * Author names for rs list rows, looked up only on multi-author sites. The caller applies them, since
 * pages wrap their models in tree items.
 */
internal class RsAuthorNames<TAB>(
    private val scope: CoroutineScope,
    private val restClient: RsSiteRestClient,
    private val postType: String,
    private val onNamesResolved: (TAB, Map<Long, String>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val jobs = mutableMapOf<TAB, Job>()

    /** Asked once and kept for the screen's lifetime; a refresh doesn't change the answer. */
    private var isMultiAuthor: Deferred<Boolean>? = null

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

    // isSingleUserSite is only set for WP.com sites; app-password sites have to ask.
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
