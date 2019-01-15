package org.wordpress.android.ui.activitylog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_REWIND_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.FINISHED
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnRewindStatusFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType.GENERIC_ERROR
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class RewindProgressCheckerTest {
    @Mock lateinit var activityLogStore: ActivityLogStore
    @Mock lateinit var site: SiteModel
    private val restoreId = 1L
    private lateinit var rewindProgressChecker: RewindProgressChecker
    @Before
    fun setUp() {
        rewindProgressChecker = RewindProgressChecker(activityLogStore, TEST_SCOPE)
    }

    private val finishedRewind = Rewind("rewindId", restoreId, FINISHED, 100, "finished")

    private val rewindStatusModel = RewindStatusModel(ACTIVE, "reason", Date(), true, null, null)
    private val finishedRewindStatus = rewindStatusModel.copy(rewind = finishedRewind)

    @Test
    fun `on start checks current value and finishes if rewind is done`() = runBlocking {
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(finishedRewindStatus)

        val onRewindStatusFetched = rewindProgressChecker.start(site, restoreId, checkDelay = -1)

        assertEquals(OnRewindStatusFetched(FETCH_REWIND_STATE), onRewindStatusFetched)
    }

    @Test
    fun `on start triggers fetch if rewind not in progress`() = runBlocking {
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(rewindStatusModel, finishedRewindStatus)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(OnRewindStatusFetched(FETCH_REWIND_STATE))

        val onRewindStatusFetched = rewindProgressChecker.start(site, restoreId, checkDelay = -1)

        with(inOrder(activityLogStore)) {
            verify(activityLogStore).getRewindStatusForSite(site)
            verify(activityLogStore).fetchActivitiesRewind(any())
            verify(activityLogStore).getRewindStatusForSite(site)
        }

        assertEquals(OnRewindStatusFetched(FETCH_REWIND_STATE), onRewindStatusFetched)
    }

    @Test
    fun `on fetch fail return error`() = runBlocking {
        whenever(activityLogStore.getRewindStatusForSite(site)).thenReturn(rewindStatusModel)
        val errorStatus = OnRewindStatusFetched(RewindStatusError(GENERIC_ERROR, "generic error"), FETCH_REWIND_STATE)
        whenever(activityLogStore.fetchActivitiesRewind(any())).thenReturn(errorStatus)

        val onRewindStatusFetched = rewindProgressChecker.start(site, restoreId, checkDelay = -1)

        with(inOrder(activityLogStore)) {
            verify(activityLogStore).getRewindStatusForSite(site)
            verify(activityLogStore).fetchActivitiesRewind(any())
            verifyNoMoreInteractions()
        }

        assertEquals(errorStatus, onRewindStatusFetched)
    }

    @Test
    fun `on cancel stops a job`() = runBlocking {
        val onRewindStatusFetched = launch {
            rewindProgressChecker.start(site, restoreId)
        }

        onRewindStatusFetched.cancel()

        verifyZeroInteractions(activityLogStore)
    }
}
