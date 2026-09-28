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

    /** The ids each tab's running lookup is fetching. */
    private val pendingIds = mutableMapOf<TAB, Set<Long>>()

    /** Author ids the users endpoint didn't return, so they aren't asked for again until a refresh. */
    private val unresolvable = mutableSetOf<Long>()

    /** Kept for the screen's lifetime once answered; a failed lookup is asked again after a refresh. */
    private var publishedAuthors: Deferred<List<Long>?>? = null
    private var hasPublishedAuthorsFailed = false

    /**
     * Looks up the names [items] still lack. Ids that already failed wait for a refresh, and a lookup
     * already running for [tab] is only replaced when it doesn't cover them all.
     */
    fun resolve(tab: TAB, site: SiteModel, items: List<ContentItemUiModel<*>>) {
        val unresolvedIds = items
            .filter { it.authorId != 0L && it.authorDisplayName == null }
            .map { it.authorId }
            .filter { it !in unresolvable }
            .distinct()
        if (unresolvedIds.isEmpty()) return
        if (jobs[tab]?.isActive == true && pendingIds[tab].orEmpty().containsAll(unresolvedIds)) return
        val loadedAuthorIds = items.map { it.authorId }.filter { it != 0L }.toSet()

        jobs[tab]?.cancel()
        pendingIds[tab] = unresolvedIds.toSet()
        jobs[tab] = scope.launch {
            if (!isMultiAuthor(site, loadedAuthorIds)) return@launch
            val names = withContext(ioDispatcher) {
                restClient.fetchUserDisplayNames(site, unresolvedIds)
            }
            unresolvable.addAll(unresolvedIds.filterNot(names::containsKey))
            if (names.isNotEmpty()) onNamesResolved(tab, names)
        }
    }

    /**
     * WP.com sites know whether they're single-user. Elsewhere the rows on screen are combined with the
     * site's published authors, so a draft by someone who has never published still counts as a second
     * author.
     */
    private suspend fun isMultiAuthor(site: SiteModel, loadedAuthorIds: Set<Long>): Boolean {
        if (loadedAuthorIds.size > 1) return true
        site.isSingleUserSite?.let { return !it }
        val published = publishedAuthorIds(site) ?: return false
        return (loadedAuthorIds + published).size > 1
    }

    private suspend fun publishedAuthorIds(site: SiteModel): List<Long>? {
        val lookup = publishedAuthors ?: scope.async {
            withContext(ioDispatcher) { restClient.fetchPublishedAuthorIds(site, postType) }
        }.also { publishedAuthors = it }
        return lookup.await().also { if (it == null) hasPublishedAuthorsFailed = true }
    }

    /** Forgets the failures, so a refresh asks for them again. */
    fun invalidateUnresolved() {
        unresolvable.clear()
        if (hasPublishedAuthorsFailed) {
            publishedAuthors = null
            hasPublishedAuthorsFailed = false
        }
    }

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        pendingIds.clear()
        unresolvable.clear()
    }
}
