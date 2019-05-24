package org.wordpress.android.util

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.WordPressTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.DaggerMockedUploadAppComponent
import org.wordpress.android.modules.FakeLocalDraftUploadStarter
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class UploadWorkRequestTest {
    @Inject lateinit var mockedLocalDraftUploadStarter: LocalDraftUploadStarter
    lateinit var fakeLocalDraftUploadStarter: FakeLocalDraftUploadStarter

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as WordPressTest
        val appComponent = DaggerMockedUploadAppComponent.builder()
                .application(app)
                .build()
        app.setAppComponent(appComponent)
        appComponent.inject(this)
        fakeLocalDraftUploadStarter = mockedLocalDraftUploadStarter as FakeLocalDraftUploadStarter

        val context = InstrumentationRegistry.getTargetContext()
        val config = Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                // Use a SynchronousExecutor here to make it easier to write tests
                .setExecutor(SynchronousExecutor())
                .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun testOneTimeUploadWorker() {
        val testDriver = WorkManagerTestInitHelper.getTestDriver()
        val workManager = WorkManager.getInstance()

        // Define inputs
        val site = SiteModel()

        // Enqueue
        val (request, operation) = enqueueUploadWorkRequestForSite(site)

        // Meet constraints
        testDriver.setAllConstraintsMet(request.id)

        // Wait for result
        operation.result.get()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // Check the work was successful and the method was called with the right argument
        assertThat(fakeLocalDraftUploadStarter.counter.getValue(fakeLocalDraftUploadStarter::queueUploadFromSite),
                `is`(1))
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
    }

    @Test
    fun testOneTimeUploadWorkerWithUnmetConstraints() {
        // Define inputs
        val site = SiteModel()

        val workManager = WorkManager.getInstance()

        // Enqueue
        val (request, operation) = enqueueUploadWorkRequestForSite(site)

        // Wait for result
        operation.result.get()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // We didn't call setAllConstraintsMet earlier, so the work won't be executed (can't be success or failure)
        assertThat(fakeLocalDraftUploadStarter.counter.getValue(fakeLocalDraftUploadStarter::queueUploadFromSite),
                `is`(0))
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun testPeriodicUploadWorkerWithMetConstraints() {
        // Define input data
        val testDriver = WorkManagerTestInitHelper.getTestDriver()
        val workManager = WorkManager.getInstance()

        // Enqueue
        val (request, operation) = enqueuePeriodicUploadWorkRequestForAllSites()

        // Meet constraints and delay
        testDriver.setAllConstraintsMet(request.id)
        testDriver.setPeriodDelayMet(request.id)

        // Wait for result
        operation.result.get()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // Periodic upload worker will stay enqueued after success/failure: ENQUEUED -> RUNNING -> ENQUEUED
        assertThat(fakeLocalDraftUploadStarter.counter.getValue(fakeLocalDraftUploadStarter::queueUploadFromAllSites),
                `is`(1))
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun testPeriodicUploadWorkerWithMetConstraintsCalledTwice() {
        // Define input data
        val testDriver = WorkManagerTestInitHelper.getTestDriver()
        val workManager = WorkManager.getInstance()

        // Enqueue
        val (request, operation) = enqueuePeriodicUploadWorkRequestForAllSites()

        // ############### First round
        // Meet delay, constraints, and wait for result
        testDriver.setAllConstraintsMet(request.id)
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

        // Check LocalDraftUploadStarter.queueUploadFromAllSites() was called twice
        assertThat(fakeLocalDraftUploadStarter.counter.getValue(fakeLocalDraftUploadStarter::queueUploadFromAllSites),
                `is`(2))

        // WorkRequest should still be queued
        val workInfo2 = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo2.state, `is`(WorkInfo.State.ENQUEUED))
    }
}
