package org.wordpress.android.fluxc.store

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yarolegovich.wellsql.SelectQuery
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils

@RunWith(MockitoJUnitRunner::class)
class ActivityLogStoreTest {
    @Mock private lateinit var activityLogRestClient: ActivityLogRestClient
    @Mock private lateinit var activityLogSqlUtils: ActivityLogSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var activityLogStore: ActivityLogStore

    @Before
    fun setUp() {
        activityLogStore = ActivityLogStore(activityLogRestClient, activityLogSqlUtils, dispatcher)
    }

    @Test
    fun onFetchActivitiesActionCallRestClient() {
        val number = 10
        val offset = 0

        val payload = ActivityLogStore.FetchActivityLogPayload(siteModel, number, offset)
        val action = ActivityLogActionBuilder.newFetchActivitiesAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivity(siteModel, number, offset)
    }

    @Test
    fun onFetchRewindStatusActionCallRestClient() {
        val payload = ActivityLogStore.FetchRewindStatePayload(siteModel)
        val action = ActivityLogActionBuilder.newFetchRewindStateAction(payload)
        activityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivityRewind(siteModel)
    }

    @Test
    fun storeFetchedActivityLogToDb() {
        val activityModels = listOf<ActivityLogModel>(mock())
        val payload = ActivityLogStore.FetchedActivityLogPayload(activityModels, siteModel, 10, 0)
        val action = ActivityLogActionBuilder.newFetchedActivitiesAction(payload)
        val rowsAffected = 1
        whenever(activityLogSqlUtils.insertOrUpdateActivities(any(), any())).thenReturn(rowsAffected)

        activityLogStore.onAction(action)

        verify(activityLogSqlUtils).insertOrUpdateActivities(siteModel, activityModels)
        val expectedChangeEvent = ActivityLogStore.OnActivityLogFetched(rowsAffected,
                activityModels,
                ActivityLogAction.FETCHED_ACTIVITIES)
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
    fun storeFetchedRewindStatusToDb() {
        val rewindStatusModel = mock<RewindStatusModel>()
        val payload = ActivityLogStore.FetchedRewindStatePayload(rewindStatusModel, siteModel)
        val action = ActivityActionBuilder.newFetchedRewindStateAction(payload)

        activityLogStore.onAction(action)

        verify(activityLogSqlUtils).insertOrUpdateRewindStatus(siteModel, rewindStatusModel)
        val expectedChangeEvent = ActivityLogStore.OnRewindStatusFetched(ActivityAction.FETCHED_REWIND_STATE)
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
}
