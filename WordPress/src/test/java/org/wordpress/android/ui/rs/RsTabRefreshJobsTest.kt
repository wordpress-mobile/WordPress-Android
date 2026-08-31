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
        assertThat(jobs.deferIfRunning(TAB)).isFalse()
    }

    @Test
    fun `a tab with a job running defers`() {
        jobs.onStarted(TAB, Job())

        assertThat(jobs.deferIfRunning(TAB)).isTrue()
    }

    @Test
    fun `a different tab is unaffected by a running job`() {
        jobs.onStarted(TAB, Job())

        assertThat(jobs.deferIfRunning(OTHER_TAB)).isFalse()
    }

    @Test
    fun `a cancelled job no longer defers`() {
        val job = Job()
        jobs.onStarted(TAB, job)
        job.cancel()

        assertThat(jobs.deferIfRunning(TAB)).isFalse()
    }

    @Test
    fun `a completed job no longer defers`() {
        val job = Job()
        jobs.onStarted(TAB, job)
        job.complete()

        assertThat(jobs.deferIfRunning(TAB)).isFalse()
    }

    @Test
    fun `onFinished asks for a replay only when a request was deferred`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB)

        assertThat(jobs.onFinished(TAB)).isTrue()
    }

    @Test
    fun `onFinished asks for nothing when no request was deferred`() {
        jobs.onStarted(TAB, Job())

        assertThat(jobs.onFinished(TAB)).isFalse()
    }

    @Test
    fun `several deferred requests are replayed once`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB)
        jobs.deferIfRunning(TAB)
        jobs.deferIfRunning(TAB)

        assertThat(jobs.onFinished(TAB)).isTrue()
        assertThat(jobs.onFinished(TAB)).isFalse()
    }

    @Test
    fun `onFinished releases the tab for the next refresh`() {
        jobs.onStarted(TAB, Job())
        jobs.onFinished(TAB)

        assertThat(jobs.deferIfRunning(TAB)).isFalse()
    }

    @Test
    fun `clear forgets running jobs and deferred requests`() {
        jobs.onStarted(TAB, Job())
        jobs.deferIfRunning(TAB)

        jobs.clear()

        assertThat(jobs.deferIfRunning(TAB)).isFalse()
        assertThat(jobs.onFinished(TAB)).isFalse()
    }

    companion object {
        private const val TAB = "published"
        private const val OTHER_TAB = "drafts"
    }
}
