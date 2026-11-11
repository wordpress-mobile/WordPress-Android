package org.wordpress.android.ui.rs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Pages an rs observable collection through to the end.
 *
 * A list that is rendered as a tree can only nest a child once its parent has loaded, and a
 * collection sorted by title delivers parents and children in unrelated pages. Legacy solved
 * this by fetching until the server was exhausted; this is the same loop for the rs lists.
 *
 * Lives outside the view models so it can be tested on its own, like [RsTabLoading] - the
 * collection it pages is an rs observable collection, which a unit test can neither create nor
 * fake. The loop only sees `hasMorePages` answers and a `loadNextPage` lambda; whether those
 * answers come from the server's page count or from the size of the page is the caller's call.
 */
internal object RsCollectionPrefetch {
    sealed interface Outcome {
        /** The last page has been loaded. */
        data object Complete : Outcome

        /** [loadRemainingPages]'s `maxPages` was reached with more still reported. */
        data object Capped : Outcome
    }

    /**
     * Loads pages until none are left, or [maxPages] have been loaded.
     *
     * [hasMorePages] seeds the loop with the answer from the refresh that preceded it, and
     * [loadNextPage] returns the answer from each page it fetches. A page that throws is retried
     * up to [maxAttemptsPerPage] times, waiting on [backoff] in between, unless [shouldRetry]
     * rules the error out - a rejected credential, for instance, is not going to pass on the
     * third try. Once retries are spent, the error propagates, so the caller reports a fill that
     * fails the same way as the refresh that started it.
     *
     * Cancellation propagates too: a caller that tears the collection down cancels the loop with
     * it.
     */
    @Suppress("LongParameterList")
    suspend fun loadRemainingPages(
        hasMorePages: Boolean,
        maxPages: Int,
        maxAttemptsPerPage: Int = DEFAULT_MAX_ATTEMPTS_PER_PAGE,
        shouldRetry: (Exception) -> Boolean = { true },
        backoff: suspend (attempt: Int) -> Unit = { delay(BASE_BACKOFF_MS shl (it - 1)) },
        loadNextPage: suspend () -> Boolean
    ): Outcome {
        var pagesLoaded = 0
        var outcome = stopReason(hasMorePages, pagesLoaded, maxPages)
        while (outcome == null) {
            val more = loadPage(maxAttemptsPerPage, shouldRetry, backoff, loadNextPage)
            pagesLoaded++
            outcome = stopReason(more, pagesLoaded, maxPages)
        }
        return outcome
    }

    /** Why the loop should stop given the latest `hasMorePages`, or null to keep going. */
    private fun stopReason(hasMorePages: Boolean, pagesLoaded: Int, maxPages: Int): Outcome? = when {
        !hasMorePages -> Outcome.Complete
        pagesLoaded >= maxPages -> Outcome.Capped
        else -> null
    }

    /** Loads one page under the retry policy, rethrowing the error that ended the retries. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadPage(
        maxAttempts: Int,
        shouldRetry: (Exception) -> Boolean,
        backoff: suspend (attempt: Int) -> Unit,
        loadNextPage: suspend () -> Boolean
    ): Boolean {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return loadNextPage()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!shouldRetry(e) || attempt >= maxAttempts) throw e
                backoff(attempt)
            }
        }
    }

    private const val DEFAULT_MAX_ATTEMPTS_PER_PAGE = 3
    private const val BASE_BACKOFF_MS = 1_000L
}
