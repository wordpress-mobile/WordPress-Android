package org.wordpress.android.ui.rs

import kotlinx.coroutines.Job

/**
 * Tracks the in-flight refresh of each tab of a list backed by an rs observable collection.
 *
 * A tab refreshes one at a time: `refresh()` fetches page 1 and replaces the stored metadata, so
 * two concurrent refreshes of the same collection would each overwrite the other's result.
 *
 * A request that arrives mid-refresh isn't dropped, though. The running refresh may have asked the
 * server before the change that prompted the new request landed, so the request is remembered and
 * replayed once the current one finishes.
 *
 * Lives outside the view models so it can be tested on its own, like [RsTabLoading] - the refresh
 * it coordinates is driven by an rs observable collection, which a unit test can neither create
 * nor fake.
 */
internal class RsTabRefreshJobs<T> {
    private val jobs = mutableMapOf<T, Job>()
    private val replays = mutableSetOf<T>()

    /**
     * Whether [tab] is already refreshing. When it is, the request is remembered so [onFinished]
     * can replay it, and the caller should do nothing else.
     */
    fun deferIfRunning(tab: T): Boolean {
        if (jobs[tab]?.isActive != true) return false
        replays.add(tab)
        return true
    }

    fun onStarted(tab: T, job: Job) {
        jobs[tab] = job
    }

    /**
     * Clears [tab]'s job and reports whether a request arrived while it was running, meaning the
     * caller should refresh once more.
     */
    fun onFinished(tab: T): Boolean {
        jobs.remove(tab)
        return replays.remove(tab)
    }

    /**
     * Forgets everything, for when the collections these jobs served are torn down. The jobs
     * themselves are cancelled with the scope that owns them.
     */
    fun clear() {
        jobs.clear()
        replays.clear()
    }
}
