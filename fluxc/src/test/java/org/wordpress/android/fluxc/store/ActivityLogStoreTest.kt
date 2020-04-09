package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.SelectQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindResultPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class ActivityLogStoreTest {
    @Mock private lateinit var activityLogRestClient: ActivityLogRestClient
    @Mock private lateinit var activityLogSqlUtils: ActivityLogSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var activityLogStore: ActivityLogStore

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        activityLogStore = ActivityLogStore(activityLogRestClient, activityLogSqlUtils,
                initCoroutineEngine(), dispatcher)
    }

    @Test
    fun onFetchActivityLogFirstPageActionCleanupDbAndCallRestClient() = test {
        val number = 10
        val offset = 0

        whenever(activityLogRestClient.fetchActivity(eq(siteModel), any(), any())).thenReturn(
                FetchedActivityLogPayload(
                        listOf(),
                        siteModel,
                        0,
                        0,
                        0
                )
        )

        val payload = FetchActivityLogPayload(siteModel)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivity(siteModel, number, offset)
    }

    @Test
    fun onFetchActivityLogNextActionReadCurrentDataAndCallRestClient() = test {
        val number = 10

        whenever(activityLogRestClient.fetchActivity(eq(siteModel), any(), any())).thenReturn(
                FetchedActivityLogPayload(
                        listOf(),
                        siteModel,
                        0,
                        0,
                        0
                )
        )

        val existingActivities = listOf<ActivityLogModel>(mock())
        whenever(activityLogSqlUtils.getActivitiesForSite(siteModel, SelectQuery.ORDER_ASCENDING))
                .thenReturn(existingActivities)

        val payload = FetchActivityLogPayload(siteModel, true)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivity(siteModel, number, existingActivities.size)
    }

    @Test
    fun onFetchRewindStatusActionCallRestClient() = test {
        val payload = FetchRewindStatePayload(siteModel)
        whenever(activityLogRestClient.fetchActivityRewind(siteModel)).thenReturn(
                FetchedRewindStatePayload(
                        null,
                        siteModel
                )
        )
        val action = ActivityLogActionBuilder.newFetchRewindStateAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivityRewind(siteModel)
    }

    @Test
    fun onRewindActionCallRestClient() = test {
        whenever(activityLogRestClient.rewind(eq(siteModel), any())).thenReturn(
                RewindResultPayload(
                        "rewindId",
                        null,
                        siteModel
                )
        )

        val rewindId = "rewindId"
        val payload = RewindPayload(siteModel, rewindId)
        val action = ActivityLogActionBuilder.newRewindAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).rewind(siteModel, rewindId)
    }

    @Test
    fun storeFetchedActivityLogToDbAndSetsLoadMoreToFalse() = test {
        val rowsAffected = 1
        val activityModels = listOf<ActivityLogModel>(mock())

        val action = initRestClient(activityModels, rowsAffected)

        activityLogStore.onAction(action)

        verify(activityLogSqlUtils).insertOrUpdateActivities(siteModel, activityModels)
        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(rowsAffected,
                false,
                ActivityLogAction.FETCH_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
        verify(activityLogSqlUtils).deleteActivityLog()
    }

    @Test
    fun cannotLoadMoreWhenResponseEmpty() = test {
        val rowsAffected = 0
        val activityModels = listOf<ActivityLogModel>(mock())

        val action = initRestClient(activityModels, rowsAffected)

        activityLogStore.onAction(action)

        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(0,
                false,
                ActivityLogAction.FETCH_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun setsLoadMoreToTrueOnMoreItems() = test {
        val rowsAffected = 1
        val activityModels = listOf<ActivityLogModel>(mock())

        val action = initRestClient(activityModels, rowsAffected, totalItems = 100)
        whenever(activityLogSqlUtils.insertOrUpdateActivities(any(), any())).thenReturn(rowsAffected)

        activityLogStore.onAction(action)

        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(rowsAffected,
                true,
                ActivityLogAction.FETCH_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnActivitiesFromDb() {
        val activityModels = listOf<ActivityLogModel>(mock())
        whenever(activityLogSqlUtils.getActivitiesForSite(siteModel, SelectQuery.ORDER_DESCENDING))
                .thenReturn(activityModels)

        val activityModelsFromDb = activityLogStore.getActivityLogForSite(siteModel, ascending = false)

        verify(activityLogSqlUtils).getActivitiesForSite(siteModel, SelectQuery.ORDER_DESCENDING)
        assertEquals(activityModels, activityModelsFromDb)
    }

    @Test
    fun storeFetchedRewindStatusToDb() = test {
        val rewindStatusModel = mock<RewindStatusModel>()
        val payload = FetchedRewindStatePayload(rewindStatusModel, siteModel)
        whenever(activityLogRestClient.fetchActivityRewind(siteModel)).thenReturn(payload)

        val fetchAction = ActivityLogActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(siteModel))
        activityLogStore.onAction(fetchAction)

        verify(activityLogSqlUtils).replaceRewindStatus(siteModel, rewindStatusModel)
        val expectedChangeEvent = ActivityLogStore.OnRewindStatusFetched(ActivityLogAction.FETCH_REWIND_STATE)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnRewindStatusFromDb() {
        val rewindStatusModel = mock<RewindStatusModel>()
        whenever(activityLogSqlUtils.getRewindStatusForSite(siteModel))
                .thenReturn(rewindStatusModel)

        val rewindStatusFromDb = activityLogStore.getRewindStatusForSite(siteModel)

        verify(activityLogSqlUtils).getRewindStatusForSite(siteModel)
        assertEquals(rewindStatusModel, rewindStatusFromDb)
    }

    @Test
    fun emitsRewindResult() = test {
        val rewindId = "rewindId"
        val restoreId = 10L

        val payload = ActivityLogStore.RewindResultPayload(rewindId, restoreId, siteModel)
        whenever(activityLogRestClient.rewind(siteModel, rewindId)).thenReturn(payload)

        activityLogStore.onAction(ActivityLogActionBuilder.newRewindAction(RewindPayload(siteModel, rewindId)))

        val expectedChangeEvent = ActivityLogStore.OnRewind(rewindId, restoreId, ActivityLogAction.REWIND)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun returnsActivityLogItemFromDbByRewindId() {
        val rewindId = "rewindId"
        val activityLogModel = mock<ActivityLogModel>()
        whenever(activityLogSqlUtils.getActivityByRewindId(rewindId)).thenReturn(activityLogModel)

        val returnedItem = activityLogStore.getActivityLogItemByRewindId(rewindId)

        assertEquals(activityLogModel, returnedItem)
        verify(activityLogSqlUtils).getActivityByRewindId(rewindId)
    }

    @Test
    fun returnsActivityLogItemFromDbByActivityId() {
        val rewindId = "activityId"
        val activityLogModel = mock<ActivityLogModel>()
        whenever(activityLogSqlUtils.getActivityByActivityId(rewindId)).thenReturn(activityLogModel)

        val returnedItem = activityLogStore.getActivityLogItemByActivityId(rewindId)

        assertEquals(activityLogModel, returnedItem)
        verify(activityLogSqlUtils).getActivityByActivityId(rewindId)
    }

    private suspend fun initRestClient(
        activityModels: List<ActivityLogModel>,
        rowsAffected: Int,
        offset: Int = 0,
        number: Int = 10,
        totalItems: Int = 10
    ): Action<*> {
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(FetchActivityLogPayload(siteModel))

        val payload = FetchedActivityLogPayload(activityModels, siteModel, totalItems, number, offset)
        whenever(activityLogRestClient.fetchActivity(siteModel, number, offset)).thenReturn(payload)
        whenever(activityLogSqlUtils.insertOrUpdateActivities(any(), any())).thenReturn(rowsAffected)
        return action
    }
}
