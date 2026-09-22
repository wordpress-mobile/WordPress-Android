package org.wordpress.android.ui.rs

import androidx.annotation.MainThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.wordpress.android.ui.newstats.datasource.PostViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.util.AppLog

/**
 * All-time view counts for the rows of a list backed by an rs observable collection.
 *
 * Driven by scroll position rather than by the page load because the stats API answers for one
 * post at a time. Volume is held down by debouncing on the caller's side, a single [gate] shared
 * across all calls, and re-checking the tab's visible rows once a permit is granted - not by
 * cancelling earlier batches, which would strand rows on their skeletons.
 *
 * Whether a tab expects counts at all is the caller's business: this knows how to fetch them, not
 * when it is worth doing. The posts list layers comment counts on top of the same [visibleRows] and
 * [jobs], which is why those are collaborators rather than private state.
 */
internal class RsViewCounts<TAB>(
    private val scope: CoroutineScope,
    private val statsDataSource: StatsDataSource,
    private val visibleRows: RsVisibleRows<TAB>,
    private val jobs: RsMetricJobs,
    private val logTag: AppLog.T,
    /** Pushes whatever the cache now holds for these ids onto the tab's rows. */
    private val onCountsChanged: (TAB, List<Long>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Caps concurrent view-count requests across the whole screen.
     *
     * Shared rather than created per call: the caller reports on every visible-set change, so a
     * per-call semaphore would cap each emission separately and a fling could still put a request
     * in flight for every row it passed.
     */
    private val gate = Semaphore(MAX_CONCURRENT_VIEW_FETCHES)

    /**
     * View counts keyed by remote id, so scrolling back to a row does not refetch it and a cache
     * reload does not blank the number out. Only touched from the main dispatcher.
     *
     * A present key means "fetched"; a null value means the fetch came back with nothing usable.
     * Rows distinguish the two so a failure clears the loading skeleton rather than pinning it.
     */
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

        jobs.track(
            scope.launch {
                wanted.forEach { id ->
                    launch {
                        gate.withPermit {
                            // Re-checked after waiting for a permit rather than before queuing: by
                            // the time a slot frees up the user may have scrolled well past this
                            // row, and fetching it would spend a request on something off screen.
                            if (id in visibleRows.visible(tab)) fetchOne(tab, siteId, id)
                        }
                    }
                }
            }
        )
    }

    /**
     * Drops the counts that resolved to nothing, so a refresh asks for them again. Rows that got a
     * number keep it rather than blanking out while the new one is on its way.
     */
    fun invalidateUnresolved() {
        cache.entries.removeAll { it.value == null }
    }

    fun clear() {
        cache.clear()
        inFlight.clear()
    }

    /**
     * Fetches one row's view count.
     *
     * Metrics decorate the rows; the list is perfectly usable without them, so nothing in this path
     * is allowed to take the screen down.
     */
    private suspend fun fetchOne(tab: TAB, siteId: Long, id: Long) {
        // Re-checked here, not just when the batch was queued: ids waiting on [gate] are not yet
        // recorded as in flight, so the same row can be queued twice and the first fetch can land
        // before the second gets its permit.
        if (cache.containsKey(id)) return
        if (!inFlight.add(id)) return
        try {
            // A null either way: the fetch failed, or it answered with nothing usable. Both mean
            // the row has no number to show and should stop waiting for one.
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
        /**
         * The stats API answers for one post at a time, so a screenful of rows is a screenful of
         * requests. Enough to fill visible rows promptly without monopolising the connection.
         */
        private const val MAX_CONCURRENT_VIEW_FETCHES = 4
    }
}
