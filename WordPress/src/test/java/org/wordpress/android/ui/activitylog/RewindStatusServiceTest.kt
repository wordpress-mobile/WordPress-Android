package org.wordpress.android.ui.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.action.ActivityLogAction.REWIND
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FAILED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.INACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewind
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.activitylog.RewindStatusService.RewindProgress
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class RewindStatusServiceTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    private val rewindStatusCaptor = argumentCaptor<FetchRewindStatePayload>()
    private val rewindCaptor = argumentCaptor<RewindPayload>()

    @Mock private lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var rewindProgressChecker: RewindProgressChecker
    @Mock private lateinit var site: SiteModel

    private lateinit var rewindStatusService: RewindStatusService
    private var rewindAvailable: Boolean? = null
    private var rewindProgress: RewindProgress? = null
    private var rewindError: RewindError? = null
    private var rewindStatusFetchError: RewindStatusError? = null

    private val rewindId = "10"
    private val activityID = "activityId"
    private val published = Date()
    private val activityLogModel = ActivityLogModel(
            activityID,
            "summary",
            FormattableContent(text = "text"),
            null,
            null,
            null,
            null,
            null,
            rewindId,
            published
    )

    private val activeRewindStatusModel = RewindStatusModel(
            state = ACTIVE,
            lastUpdated = Date(),
            rewind = null,
            credentials = null,
            reason = null,
            canAutoconfigure = null
    )

    private val progress = 23
    private val rewindInProgress = Rewind(
            rewindId = rewindId,
            restoreId = 10,
            status = RUNNING,
            progress = progress,
            reason = null
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        rewindStatusService = RewindStatusService(activityLogStore, rewindProgressChecker, TEST_SCOPE)
        rewindAvailable = null
        rewindStatusService.rewindAvailable.observeForever { rewindAvailable = it }
        rewindStatusService.rewindProgress.observeForever { rewindProgress = it }
        rewindStatusService.rewindError.observeForever { rewindError = it }
        rewindStatusService.rewindStatusFetchError.observeForever { rewindStatusFetchError = it }
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(null)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))
        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind(rewindId, null, REWIND))

        whenever(activityLogStore.getActivityLogItemByRewindId(rewindId)).thenReturn(activityLogModel)
        whenever(site.origin).thenReturn(SiteModel.ORIGIN_WPCOM_REST)
        whenever(rewindProgressChecker.startNow(any(), any())).thenReturn(null)
    }

    @After
    fun tearDown() {
        rewindStatusService.stop()
    }

    @Test
    fun emitsAvailableRewindStatusOnStartWhenActiveStatusPresent() {
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(activeRewindStatusModel)

        rewindStatusService.start(site)

        assertEquals(rewindAvailable, true)
    }

    @Test
    fun emitsUnavailableRewindStatusOnStartWhenNonActiveStatusPresent() {
        val inactiveRewindStatusModel = activeRewindStatusModel.copy(state = INACTIVE)
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(inactiveRewindStatusModel)

        rewindStatusService.start(site)

        assertEquals(rewindAvailable, false)
    }

    @Test
    fun emitsUnavailableRewindStatusOnStartWhenRewindInProgress() = runBlocking {
        val inactiveRewindStatusModel = activeRewindStatusModel.copy(rewind = rewindInProgress)
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(inactiveRewindStatusModel, null)

        rewindStatusService.start(site)

        assertEquals(rewindAvailable, false)
    }

    @Test
    fun triggersFetchWhenRewindStatusNotAvailable() = runBlocking {
        rewindStatusService.start(site)

        assertFetchRewindStatusAction()
    }

    @Test
    fun updatesRewindStatusAndRestartsCheckerWhenRewindNotAlreadyRunning() = runBlocking<Unit> {
        rewindStatusService.start(site)
        val rewindStatusInProgress = activeRewindStatusModel.copy(rewind = rewindInProgress)
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(rewindStatusInProgress, null)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))
        reset(rewindProgressChecker)

        rewindStatusService.requestStatusUpdate()

        verify(rewindProgressChecker).startNow(site, rewindInProgress.restoreId)
    }

    @Test
    fun triggersRewindAndMakesActionUnavailable() = runBlocking {
        val rewindId = "10"

        rewindStatusService.rewind(rewindId, site)

        assertRewindAction(rewindId)
        assertEquals(false, rewindAvailable)
        assertEquals(rewindProgress, RewindProgress(activityLogModel, 0, published, RUNNING))
    }

    @Test
    fun cancelsWorkerOnFetchErrorRewindStateAndEmitsError() = runBlocking {
        rewindStatusService.start(site)
        val error = RewindStatusError(INVALID_RESPONSE, null)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(error, REWIND))

        rewindStatusService.requestStatusUpdate()

        assertEquals(error, rewindStatusFetchError)
    }

    @Test
    fun onRewindStateInProgressUpdateState() = runBlocking {
        rewindStatusService.start(site)

        val rewindStatusInProgress = activeRewindStatusModel.copy(rewind = rewindInProgress)
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(rewindStatusInProgress, null)

        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))

        rewindStatusService.requestStatusUpdate()

        assertEquals(rewindAvailable, false)
        assertEquals(rewindProgress?.status, Status.RUNNING)
        assertEquals(rewindProgress, RewindProgress(activityLogModel, progress, published, RUNNING))
    }

    @Test
    fun onRewindStateFinishedUpdateState() = runBlocking {
        rewindStatusService.start(site)

        val rewindFinished = rewindInProgress.copy(status = FINISHED, progress = 100)
        val rewindStatusInProgress = activeRewindStatusModel.copy(rewind = rewindFinished)
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(rewindStatusInProgress, null)

        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))

        rewindStatusService.requestStatusUpdate()

        assertEquals(rewindAvailable, true)
        assertEquals(rewindProgress?.status, Status.FINISHED)
    }

    @Test
    fun onRewindErrorCancelWorkerAndReenableRewindStatus() = runBlocking {
        rewindStatusService.start(site)

        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(activeRewindStatusModel)

        val error = RewindError(RewindErrorType.INVALID_RESPONSE, null)
        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind(rewindId, error, REWIND))

        rewindAvailable = null

        rewindStatusService.rewind(rewindId, site)

        assertEquals(rewindAvailable, true)
        assertEquals(error, rewindError)
        val progress = RewindProgress(activityLogModel, 0, published, FAILED, INVALID_RESPONSE.toString())
        assertEquals(rewindProgress, progress)
    }

    @Test
    fun onRewindFetchStatusAndStartWorker() = runBlocking<Unit> {
        rewindStatusService.start(site)
        reset(rewindProgressChecker)

        whenever(activityLogStore.rewind(any())).thenReturn(OnRewind("5", 10, REWIND))

        rewindStatusService.rewind(rewindId, site)

        verify(rewindProgressChecker).start(site, 10)
    }

    private suspend fun assertFetchRewindStatusAction() {
        verify(activityLogStore).fetchActivitiesRewind(rewindStatusCaptor.capture())
        rewindStatusCaptor.firstValue.apply {
            assertEquals(this.site, site)
        }
    }

    private suspend fun assertRewindAction(rewindId: String) {
        verify(activityLogStore).rewind(rewindCaptor.capture())
        rewindCaptor.firstValue.apply {
            assertEquals(this.site, site)
            assertEquals(this.rewindId, rewindId)
        }
    }
}
