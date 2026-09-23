package org.wordpress.android.ui.rs

import kotlinx.coroutines.Job

/**
 * The outstanding metric fetches of a list backed by an rs observable collection, cancelled only
 * at teardown.
 *
 * Deliberately not cancelled when the visible rows change: a cancelled fetch releases its in-flight
 * claim without filling the cache, and the next visible set would skip those ids as "already in
 * flight", stranding their rows on the loading skeleton with nothing left to resolve them. Volume
 * is bounded by [RsVisibleRows] instead.
 */
internal class RsMetricJobs {
    private val jobs = mutableSetOf<Job>()

    /** Keeps a job around so teardown can cancel it, and forgets it once it finishes. */
    fun track(job: Job) {
        jobs.add(job)
        job.invokeOnCompletion { jobs.remove(job) }
    }

    fun cancelAll() {
        // Snapshot first: each job's completion handler removes it from the set, and a job parked
        // on the view-count gate completes inline on Main.immediate during cancel(). Iterating the
        // live set would throw ConcurrentModificationException as soon as a non-last job did.
        jobs.toList().forEach { it.cancel() }
        jobs.clear()
    }
}
