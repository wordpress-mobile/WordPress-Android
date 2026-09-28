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

    /** Kept for the screen's lifetime once answered; a failed check is asked again on the next load. */
    private var isMultiAuthor: Deferred<Boolean?>? = null

    fun resolve(tab: TAB, site: SiteModel, items: List<ContentItemUiModel<*>>) {
        val unresolvedIds = items
            .filter { it.authorId != 0L && it.authorDisplayName == null }
            .map { it.authorId }
            .distinct()
        if (unresolvedIds.isEmpty()) return
        // Rows by two authors settle it locally - and catch authors who haven't published yet, whom the
        // site's count leaves out, as on a Drafts tab.
        val hasSeveralAuthors = items.map { it.authorId }.filter { it != 0L }.distinct().size > 1

        jobs[tab]?.cancel()
        jobs[tab] = scope.launch {
            if (!hasSeveralAuthors && isMultiAuthor(site) != true) return@launch
            val names = withContext(ioDispatcher) {
                restClient.fetchUserDisplayNames(site, unresolvedIds)
            }
            if (names.isNotEmpty()) onNamesResolved(tab, names)
        }
    }

    // isSingleUserSite is only set for WP.com sites; app-password sites have to ask.
    private suspend fun isMultiAuthor(site: SiteModel): Boolean? {
        val check = isMultiAuthor ?: scope.async {
            site.isSingleUserSite?.let { !it }
                ?: withContext(ioDispatcher) { restClient.hasMultipleAuthors(site, postType) }
        }.also { isMultiAuthor = it }
        return check.await().also { if (it == null && isMultiAuthor === check) isMultiAuthor = null }
    }

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
