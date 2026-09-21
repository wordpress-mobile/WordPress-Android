package org.wordpress.android.ui.rs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.datasource.PostViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.util.AppLog

@ExperimentalCoroutinesApi
class RsViewCountsTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var statsDataSource: StatsDataSource

    private lateinit var visibleRows: RsVisibleRows<String>
    private lateinit var jobs: RsMetricJobs
    private val applied = mutableListOf<Pair<String, List<Long>>>()

    @Before
    fun setUp() {
        visibleRows = RsVisibleRows()
        jobs = RsMetricJobs()
        applied.clear()
    }

    private fun createViewCounts() = RsViewCounts(
        scope = testScope(),
        statsDataSource = statsDataSource,
        visibleRows = visibleRows,
        jobs = jobs,
        logTag = AppLog.T.POSTS,
        onCountsChanged = { tab, ids -> applied.add(tab to ids) },
        ioDispatcher = testDispatcher(),
    )

    private fun views(id: Long, total: Long) = PostViewsDataResult.Success(
        PostViewsData(
            postId = id,
            totalViews = total,
            dailyViews = emptyList(),
            weeks = emptyList(),
            years = emptyList(),
            averages = emptyList(),
            post = null,
        )
    )

    @Test
    fun `a visible row's count is fetched, cached and pushed to the rows`() = test {
        whenever(statsDataSource.fetchPostViews(any(), any())).doSuspendableAnswer { views(ONE, 7L) }
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE))

        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()

        assertThat(viewCounts.countFor(ONE)).isEqualTo(7L)
        assertThat(viewCounts.isOutstanding(ONE)).isFalse
        assertThat(applied).containsExactly(TAB to listOf(ONE))
    }

    @Test
    fun `a failed fetch caches nothing to show rather than leaving the row waiting`() = test {
        whenever(statsDataSource.fetchPostViews(any(), any()))
            .doSuspendableAnswer { PostViewsDataResult.Error(StatsErrorType.NOT_AVAILABLE) }
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE))

        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()

        assertThat(viewCounts.countFor(ONE)).isNull()
        // The row has no number, but it is no longer waiting for one - otherwise it shimmers for good.
        assertThat(viewCounts.isOutstanding(ONE)).isFalse
    }

    @Test
    fun `a cached row is not fetched again`() = test {
        whenever(statsDataSource.fetchPostViews(any(), any())).doSuspendableAnswer { views(ONE, 7L) }
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE))
        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()

        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()

        verify(statsDataSource).fetchPostViews(SITE_ID, ONE)
    }

    /**
     * The regression guard for the double-queue race.
     *
     * Ids waiting on the gate are not yet recorded as in flight, so the same row can be queued
     * twice. Saturating the gate first is what makes the two queued copies land either side of the
     * first fetch completing - which is the only ordering the in-flight set alone cannot catch.
     */
    @Test
    fun `a row queued twice is only fetched once`() = test {
        val parked = (1..GATE_SIZE).map { CompletableDeferred<Unit>() }
        whenever(statsDataSource.fetchPostViews(any(), any())).doSuspendableAnswer { invocation ->
            val id = invocation.getArgument<Long>(1)
            if (id != ONE) parked[(id - FILLER_FIRST_ID).toInt()].await()
            views(id, 7L)
        }
        val filler = (FILLER_FIRST_ID until FILLER_FIRST_ID + GATE_SIZE).toList()
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, filler + ONE)

        // Every permit is taken and parked, so both batches below queue behind the gate.
        viewCounts.fetch(TAB, SITE_ID, TOKEN, filler)
        advanceUntilIdle()
        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()
        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()

        // One permit frees: the first copy fetches, caches and releases its in-flight claim.
        parked[0].complete(Unit)
        advanceUntilIdle()
        // A second frees: the other copy gets a permit and must find the cached count.
        parked[1].complete(Unit)
        advanceUntilIdle()
        parked.drop(2).forEach { it.complete(Unit) }
        advanceUntilIdle()

        verify(statsDataSource).fetchPostViews(SITE_ID, ONE)
    }

    @Test
    fun `a row scrolled off screen while it waited for a permit is not fetched`() = test {
        // No stubbing: the point of the test is that the data source is never reached.
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE))

        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        visibleRows.record(TAB, listOf(TWO))
        advanceUntilIdle()

        verify(statsDataSource, never()).fetchPostViews(any(), eq(ONE))
    }

    /**
     * The other half of the per-tab visible-row fix: work queued for one tab must not be dropped
     * because the neighbouring tab reported its own rows in the meantime.
     */
    @Test
    fun `a neighbouring tab's rows do not cancel this tab's queued fetches`() = test {
        whenever(statsDataSource.fetchPostViews(any(), any())).doSuspendableAnswer { views(ONE, 7L) }
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE))

        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        visibleRows.record(OTHER_TAB, listOf(TWO))
        advanceUntilIdle()

        verify(statsDataSource).fetchPostViews(SITE_ID, ONE)
    }

    @Test
    fun `invalidateUnresolved drops the counts that resolved to nothing and keeps the rest`() = test {
        whenever(statsDataSource.fetchPostViews(any(), eq(ONE))).doSuspendableAnswer { views(ONE, 7L) }
        whenever(statsDataSource.fetchPostViews(any(), eq(TWO)))
            .doSuspendableAnswer { PostViewsDataResult.Error(StatsErrorType.NOT_AVAILABLE) }
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE, TWO))
        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE, TWO))
        advanceUntilIdle()

        viewCounts.invalidateUnresolved()

        // A row that got a number keeps it rather than blanking out while the new one is on its way.
        assertThat(viewCounts.countFor(ONE)).isEqualTo(7L)
        assertThat(viewCounts.isOutstanding(ONE)).isFalse
        assertThat(viewCounts.isOutstanding(TWO)).isTrue
    }

    @Test
    fun `clear empties the cache`() = test {
        whenever(statsDataSource.fetchPostViews(any(), any())).doSuspendableAnswer { views(ONE, 7L) }
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, listOf(ONE))
        viewCounts.fetch(TAB, SITE_ID, TOKEN, listOf(ONE))
        advanceUntilIdle()

        viewCounts.clear()

        assertThat(viewCounts.countFor(ONE)).isNull()
        assertThat(viewCounts.isOutstanding(ONE)).isTrue
    }

    @Test
    fun `no more than four rows are fetched at once`() = test {
        val release = CompletableDeferred<Unit>()
        var concurrent = 0
        var peak = 0
        whenever(statsDataSource.fetchPostViews(any(), any())).doSuspendableAnswer {
            concurrent++
            peak = maxOf(peak, concurrent)
            release.await()
            concurrent--
            views(ONE, 1L)
        }
        val ids = (1L..8L).toList()
        val viewCounts = createViewCounts()
        visibleRows.record(TAB, ids)

        viewCounts.fetch(TAB, SITE_ID, TOKEN, ids)
        advanceUntilIdle()

        assertThat(peak).isEqualTo(4)
        release.complete(Unit)
        advanceUntilIdle()
        assertThat(peak).isEqualTo(4)
    }

    companion object {
        private const val TAB = "published"
        private const val OTHER_TAB = "drafts"
        private const val SITE_ID = 12L
        private const val TOKEN = "token"
        private const val ONE = 1L
        private const val GATE_SIZE = 4
        private const val FILLER_FIRST_ID = 100L
        private const val TWO = 2L
    }
}
