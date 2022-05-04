package org.wordpress.android.util

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.InitializationRule
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.uploads.UploadStarter
import javax.inject.Inject

@HiltAndroidTest
class UploadWorkerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    var initializationRule = InitializationRule()

    @Inject lateinit var context: Context

    private val uploadStarter = mock<UploadStarter>()
    private val siteStore = mock<SiteStore>()

    @Before
    fun setUp() {
        hiltRule.inject()

        val config = Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                // Use a SynchronousExecutor here to make it easier to write tests
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(UploadWorker.Factory(uploadStarter, siteStore))
                .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun testOneTimeUploadWorker() {
        val testDriver = WorkManagerTestInitHelper.getTestDriver(mock())
        val workManager = WorkManager.getInstance(mock())

        // Define inputs
        val site = SiteModel()
        whenever(siteStore.getSiteByLocalId(any())).doReturn(site)

        // Enqueue
        val (request, operation) = enqueueUploadWorkRequestForSite(site)

        // Meet constraints
        testDriver!!.setAllConstraintsMet(request.id)

        // Wait for result
        operation.result.get()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // Check the work was successful and the method was called with the right argument
        verify(uploadStarter, times(1)).queueUploadFromSite(eq(site))
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
    }

    @Test
    fun testOneTimeUploadWorkerWithUnmetConstraints() {
        // Define inputs
        val site = SiteModel()
        whenever(siteStore.getSiteByLocalId(any())).doReturn(site)

        val workManager = WorkManager.getInstance(mock())

        // Enqueue
        val (request, operation) = enqueueUploadWorkRequestForSite(site)

        // Wait for result
        operation.result.get()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // We didn't call setAllConstraintsMet earlier, so the work won't be executed (can't be success or failure)
        verifyZeroInteractions(uploadStarter)
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun testPeriodicUploadWorkerWithMetConstraints() {
        // Define input data
        val testDriver = WorkManagerTestInitHelper.getTestDriver(mock())
        val workManager = WorkManager.getInstance(mock())

        // Enqueue
        val (request, operation) = enqueuePeriodicUploadWorkRequestForAllSites()

        // Meet constraints and delay
        testDriver!!.setAllConstraintsMet(request.id)
        testDriver.setPeriodDelayMet(request.id)

        // Wait for result
        operation.result.get()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // Periodic upload worker will stay enqueued after success/failure: ENQUEUED -> RUNNING -> ENQUEUED
        verify(uploadStarter, times(1)).queueUploadFromAllSites()
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun testPeriodicUploadWorkerWithMetConstraintsCalledTwice() {
        // Define input data
        val testDriver = WorkManagerTestInitHelper.getTestDriver(mock())
        val workManager = WorkManager.getInstance(mock())

        // Enqueue
        val (request, operation) = enqueuePeriodicUploadWorkRequestForAllSites()

        // ############### First round
        // Meet delay, constraints, and wait for result
        testDriver!!.setAllConstraintsMet(request.id)
        testDriver.setPeriodDelayMet(request.id)
        operation.result.get()

        // Periodic upload worker will stay queued after success/failure
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))

        // ############### Second round
        // Meet delay, constraints, and wait for result
        testDriver.setAllConstraintsMet(request.id)
        testDriver.setPeriodDelayMet(request.id)
        operation.result.get()

        // Check UploadStarter.queueUploadFromAllSites() was called twice
        verify(uploadStarter, times(2)).queueUploadFromAllSites()

        // WorkRequest should still be queued
        val workInfo2 = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo2.state, `is`(WorkInfo.State.ENQUEUED))
    }
}
