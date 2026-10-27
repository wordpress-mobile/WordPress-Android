package org.wordpress.android.ui.rs

import androidx.annotation.MainThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.wordpress.android.ui.newstats.datasource.PostViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.util.AppLog

/**
 * All-time view counts for the visible rows of an rs list. The stats API answers one post at a
 * time, so fetches are capped by [gate] and skipped for rows scrolled off screen while queued -
 * never cancelled, which would strand rows on their skeletons.
 */
internal class RsViewCounts<TAB>(
    private val scope: RsCollectionScope,
    private val statsDataSource: StatsDataSource,
    private val visibleRows: RsVisibleRows<TAB>,
    private val logTag: AppLog.T,
    /** Pushes whatever the cache now holds for these ids onto the tab's rows. */
    private val onCountsChanged: (TAB, List<Long>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** One cap for the whole screen; a per-call cap would let a fling fetch every row it passed. */
    private val gate = Semaphore(MAX_CONCURRENT_VIEW_FETCHES)

    /** Keyed by remote id; a null value means "fetched, nothing to show". Main thread only. */
    private val cache = mutableMapOf<Long, Long?>()
    private val inFlight = mutableSetOf<Long>()

    /** The count to show against [id], or null when there is none to show. */
    fun countFor(id: Long): Long? = cache[id]

    /** Whether a count is still expected for [id] but has not arrived, so the row shows a skeleton. */
    fun isOutstanding(id: Long): Boolean = !cache.containsKey(id)

    @MainThread
    fun fetch(tab: TAB, siteId: Long, ids: List<Long>) {
        val wanted = ids.filter { !cache.containsKey(it) && it !in inFlight }
        if (wanted.isEmpty()) return

        scope.launch {
            wanted.forEach { id ->
                launch {
                    gate.withPermit {
                        // The user may have scrolled past this row while it waited for a permit.
                        if (id in visibleRows.visible(tab)) fetchOne(tab, siteId, id)
                    }
                }
            }
        }
    }

    /** Drops the counts that resolved to nothing, so a refresh asks for them again. */
    fun invalidateUnresolved() {
        cache.entries.removeAll { it.value == null }
    }

    fun clear() {
        cache.clear()
        inFlight.clear()
    }

    /** Never throws: metrics decorate the rows, so a failure must not take the screen down. */
    private suspend fun fetchOne(tab: TAB, siteId: Long, id: Long) {
        // Queued ids aren't in flight yet, so the same row can be queued twice.
        if (cache.containsKey(id)) return
        if (!inFlight.add(id)) return
        try {
            @Suppress("TooGenericExceptionCaught")
            val views = try {
                val result = withContext(ioDispatcher) {
                    statsDataSource.fetchPostViews(siteId = siteId, postId = id)
                }
                (result as? PostViewsDataResult.Success)?.data?.totalViews
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(logTag, "Failed to fetch view count for $id", e)
                null
            }
            cache[id] = views
            onCountsChanged(tab, listOf(id))
        } finally {
            inFlight.remove(id)
        }
    }

    companion object {
        private const val MAX_CONCURRENT_VIEW_FETCHES = 4
    }
}
