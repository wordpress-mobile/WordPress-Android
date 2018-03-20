package org.wordpress.android.fluxc.store

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ActivityAction
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils

@RunWith(MockitoJUnitRunner::class)
class ActivityLogStoreTest {
    @Mock private lateinit var activityLogRestClient: ActivityLogRestClient
    @Mock private lateinit var activityLogSqlUtils: ActivityLogSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var mActivityLogStore: ActivityLogStore

    @Before
    fun setUp() {
        mActivityLogStore = ActivityLogStore(activityLogRestClient, activityLogSqlUtils, dispatcher)
    }

    @Test
    fun onFetchActivitiesActionCallRestClient() {
        val number = 10
        val offset = 0

        val payload = ActivityLogStore.FetchActivitiesPayload(siteModel, number, offset)
        val action = ActivityActionBuilder.newFetchActivitiesAction(payload)
        mActivityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivity(siteModel, number, offset)
    }

    @Test
    fun onFetchRewindStatusActionCallRestClient() {
        val payload = ActivityLogStore.FetchRewindStatePayload(siteModel)
        val action = ActivityActionBuilder.newFetchRewindStateAction(payload)
        mActivityLogStore.onAction(action)

        verify(activityLogRestClient).fetchActivityRewind(siteModel)
    }

    @Test
    fun storeFetchedActivityLogToDb() {
        val activityModels = listOf<ActivityLogModel>(mock())
        val payload = ActivityLogStore.FetchedActivitiesPayload(activityModels, siteModel, 10, 0)
        val action = ActivityActionBuilder.newFetchedActivitiesAction(payload)
        val rowsAffected = 1
        whenever(activityLogSqlUtils.insertOrUpdateActivities(any(), any())).thenReturn(rowsAffected)

        mActivityLogStore.onAction(action)

        verify(activityLogSqlUtils).insertOrUpdateActivities(siteModel, activityModels)
        val expectedChangeEvent = ActivityLogStore.OnActivitiesFetched(rowsAffected, ActivityAction.FETCHED_ACTIVITIES)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }
}
