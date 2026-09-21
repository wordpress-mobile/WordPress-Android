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
 * fake. The loop only sees `hasMorePages` values and a `loadNextPage` lambda.
 *
 * Two `hasMorePages` exist and they disagree about unknown totals. The `ListInfo` extension answers
 * **true** when the server reported no total, which is what keeps the scroll-driven load-more
 * offering another page; `SyncResult.hasMorePages`, which this loop consumes, answers **null**. The
 * loop stops on null so an unknown total can never spin it, and the scroll-driven path remains as
 * the fallback.
 */
internal object RsCollectionPrefetch {
    sealed interface Outcome {
        /** The server reported no more pages. */
        data object Complete : Outcome

        /** The server did not report a total, so there is no way to know when to stop. */
        data object Unknown : Outcome

        /** [loadRemainingPages]'s `maxPages` was reached with more still reported. */
        data object Capped : Outcome

        /** A page failed every attempt, or with an error not worth retrying. */
        data class GaveUp(val cause: Exception, val pagesLoaded: Int) : Outcome
    }

    /**
     * Loads pages until the server reports none left, or one of the other [Outcome]s applies.
     *
     * [hasMorePages] seeds the loop with the value from the refresh that preceded it, and
     * [loadNextPage] returns the value from each page it fetches. A page that throws is retried
     * up to [maxAttemptsPerPage] times, waiting on [backoff] in between, unless [shouldRetry]
     * rules the error out - a rejected credential, for instance, is not going to pass on the
     * third try.
     *
     * Cancellation propagates: a caller that tears the collection down cancels the loop with it.
     */
    @Suppress("TooGenericExceptionCaught", "LongParameterList")
    suspend fun loadRemainingPages(
        hasMorePages: Boolean?,
        maxPages: Int,
        maxAttemptsPerPage: Int = DEFAULT_MAX_ATTEMPTS_PER_PAGE,
        shouldRetry: (Exception) -> Boolean = { true },
        backoff: suspend (attempt: Int) -> Unit = { delay(BASE_BACKOFF_MS shl (it - 1)) },
        loadNextPage: suspend () -> Boolean?
    ): Outcome {
        var more = hasMorePages
        var pagesLoaded = 0
        while (true) {
            when (more) {
                false -> return Outcome.Complete
                null -> return Outcome.Unknown
                true -> if (pagesLoaded >= maxPages) return Outcome.Capped
            }
            var attempt = 0
            while (true) {
                attempt++
                try {
                    more = loadNextPage()
                    pagesLoaded++
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!shouldRetry(e) || attempt >= maxAttemptsPerPage) {
                        return Outcome.GaveUp(e, pagesLoaded)
                    }
                    backoff(attempt)
                }
            }
        }
    }

    private const val DEFAULT_MAX_ATTEMPTS_PER_PAGE = 3
    private const val BASE_BACKOFF_MS = 1_000L
}
