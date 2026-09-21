package org.wordpress.android.ui.rs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.rs.RsCollectionPrefetch.Outcome
import java.io.IOException

@ExperimentalCoroutinesApi
class RsCollectionPrefetchTest {
    private val backoffs = mutableListOf<Int>()
    private val noBackoff: suspend (Int) -> Unit = { backoffs.add(it) }

    @Test
    fun `stops once the server reports no more pages`() = runTest {
        val answers = ArrayDeque(listOf(true, false))
        var calls = 0

        val outcome = RsCollectionPrefetch.loadRemainingPages(
            hasMorePages = true,
            maxPages = MAX_PAGES,
            backoff = noBackoff
        ) {
            calls++
            answers.removeFirst()
        }

        assertThat(outcome).isEqualTo(Outcome.Complete)
        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun `does nothing when the refresh already reported no more pages`() = runTest {
        var calls = 0

        val outcome = RsCollectionPrefetch.loadRemainingPages(
            hasMorePages = false,
            maxPages = MAX_PAGES,
            backoff = noBackoff
        ) {
            calls++
            false
        }

        assertThat(outcome).isEqualTo(Outcome.Complete)
        assertThat(calls).isZero()
    }

    @Test
    fun `retries a failed page after backing off and carries on`() = runTest {
        val answers = ArrayDeque<() -> Boolean>(
            listOf(
                { throw IOException("500") },
                { throw IOException("500") },
                { true },
                { false }
            )
        )
        var calls = 0

        val outcome = RsCollectionPrefetch.loadRemainingPages(
            hasMorePages = true,
            maxPages = MAX_PAGES,
            backoff = noBackoff
        ) {
            calls++
            answers.removeFirst()()
        }

        assertThat(outcome).isEqualTo(Outcome.Complete)
        assertThat(calls).isEqualTo(4)
        assertThat(backoffs).containsExactly(1, 2)
    }

    @Test
    fun `rethrows the error of a page that fails every attempt`() = runTest {
        val cause = IOException("500")
        val answers = ArrayDeque<() -> Boolean>(
            listOf(
                { true },
                { throw cause },
                { throw cause },
                { throw cause },
                { false }
            )
        )
        var calls = 0

        val thrown = runCatching {
            RsCollectionPrefetch.loadRemainingPages(
                hasMorePages = true,
                maxPages = MAX_PAGES,
                maxAttemptsPerPage = 3,
                backoff = noBackoff
            ) {
                calls++
                answers.removeFirst()()
            }
        }.exceptionOrNull()

        assertThat(thrown).isSameAs(cause)
        assertThat(calls).isEqualTo(4)
        assertThat(backoffs).containsExactly(1, 2)
    }

    @Test
    fun `rethrows at once an error not worth retrying`() = runTest {
        val cause = IOException("401")
        var calls = 0

        val thrown = runCatching {
            RsCollectionPrefetch.loadRemainingPages(
                hasMorePages = true,
                maxPages = MAX_PAGES,
                shouldRetry = { false },
                backoff = noBackoff
            ) {
                calls++
                throw cause
            }
        }.exceptionOrNull()

        assertThat(thrown).isSameAs(cause)
        assertThat(calls).isEqualTo(1)
        assertThat(backoffs).isEmpty()
    }

    @Test
    fun `stops at the page cap while the server still reports more`() = runTest {
        var calls = 0

        val outcome = RsCollectionPrefetch.loadRemainingPages(
            hasMorePages = true,
            maxPages = 3,
            backoff = noBackoff
        ) {
            calls++
            true
        }

        assertThat(outcome).isEqualTo(Outcome.Capped)
        assertThat(calls).isEqualTo(3)
    }

    @Test
    fun `cancellation while a page is in flight stops the loop`() = runTest {
        val inFlight = CompletableDeferred<Boolean>()
        var calls = 0
        val job = launch {
            RsCollectionPrefetch.loadRemainingPages(
                hasMorePages = true,
                maxPages = MAX_PAGES,
                backoff = noBackoff
            ) {
                calls++
                inFlight.await()
            }
        }
        advanceUntilIdle()

        job.cancel()
        inFlight.complete(true)
        advanceUntilIdle()

        assertThat(job.isCancelled).isTrue()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `cancellation during backoff stops the loop`() = runTest {
        var calls = 0
        val job = launch {
            RsCollectionPrefetch.loadRemainingPages(
                hasMorePages = true,
                maxPages = MAX_PAGES,
                backoff = { delay(BACKOFF_MS) }
            ) {
                calls++
                throw IOException("500")
            }
        }
        // advanceUntilIdle would run the delay to completion and retry, so only run what is due
        // now: the first attempt, which parks the loop inside the backoff. Cancel it there.
        runCurrent()
        job.cancel()
        advanceUntilIdle()

        assertThat(job.isCancelled).isTrue()
        assertThat(calls).isEqualTo(1)
    }

    companion object {
        private const val MAX_PAGES = 50
        private const val BACKOFF_MS = 1_000L
    }
}
