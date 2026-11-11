package org.wordpress.android.ui.rs

import kotlinx.coroutines.Job
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class RsTabRefreshJobsTest {
    private lateinit var jobs: RsTabRefreshJobs<String>

    @Before
    fun setUp() {
        jobs = RsTabRefreshJobs()
    }

    @Test
    fun `a tab with no job running does not defer`() {
        assertThat(jobs.deferIfRunning(TAB, isUserRefresh = false)).isFalse()
    }

    @Test
    fun `a tab with a job running defers`() {
        jobs.onStarted(TAB, Job())

        assertThat(jobs.deferIfRunning(TAB, isUserRefresh = false)).isTrue()
    }

    @Test
    fun `a different tab is unaffected by a running job`() {
        jobs.onStarted(TAB, Job())

        assertThat(jobs.deferIfRunning(OTHER_TAB, isUserRefresh = false)).isFalse()
    }

    @Test
    fun `a cancelled job no longer defers`() {
        val job = Job()
        jobs.onStarted(TAB, job)
        job.cancel()

        assertThat(jobs.deferIfRunning(TAB, isUserRefresh = false)).isFalse()
    }

    @Test
    fun `a completed job no longer defers`() {
        val job = Job()
        jobs.onStarted(TAB, job)
        job.complete()

        assertThat(jobs.deferIfRunning(TAB, isUserRefresh = false)).isFalse()
    }

    @Test
    fun `a deferred background refresh is replayed as a background refresh`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB, isUserRefresh = false)

        assertThat(jobs.onFinished(TAB)).isFalse()
    }

    @Test
    fun `a deferred user refresh is replayed as the user's`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB, isUserRefresh = true)

        assertThat(jobs.onFinished(TAB)).isTrue()
    }

    @Test
    fun `one user request among several deferred makes the replay the user's`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB, isUserRefresh = false)
        jobs.deferIfRunning(TAB, isUserRefresh = true)
        jobs.deferIfRunning(TAB, isUserRefresh = false)

        assertThat(jobs.onFinished(TAB)).isTrue()
    }

    @Test
    fun `onFinished asks for nothing when no request was deferred`() {
        jobs.onStarted(TAB, Job())

        assertThat(jobs.onFinished(TAB)).isNull()
    }

    @Test
    fun `several deferred requests are replayed once`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB, isUserRefresh = false)
        jobs.deferIfRunning(TAB, isUserRefresh = false)
        jobs.deferIfRunning(TAB, isUserRefresh = false)

        assertThat(jobs.onFinished(TAB)).isNotNull()
        assertThat(jobs.onFinished(TAB)).isNull()
    }

    @Test
    fun `onFinished releases the tab for the next refresh`() {
        jobs.onStarted(TAB, Job())
        jobs.onFinished(TAB)

        assertThat(jobs.deferIfRunning(TAB, isUserRefresh = false)).isFalse()
    }

    @Test
    fun `clear forgets running jobs and deferred requests`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB, isUserRefresh = false)

        jobs.clear()

        assertThat(jobs.deferIfRunning(TAB, isUserRefresh = false)).isFalse()
        assertThat(jobs.onFinished(TAB)).isNull()
    }

    companion object {
        private const val TAB = "published"
        private const val OTHER_TAB = "drafts"
    }
}
