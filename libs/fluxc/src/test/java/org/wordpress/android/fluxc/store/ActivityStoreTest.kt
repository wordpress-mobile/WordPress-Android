package org.wordpress.android.fluxc.store

import com.nhaarman.mockito_kotlin.verify
import com.yarolegovich.wellsql.SelectQuery
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityRestClient
import org.wordpress.android.fluxc.persistence.ActivitySqlUtils

@RunWith(MockitoJUnitRunner::class)
class ActivityStoreTest {
    @Mock private lateinit var activityRestClient: ActivityRestClient
    @Mock private lateinit var activitySqlUtils: ActivitySqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var activityStore: ActivityStore

    @Before
    fun setUp() {
        activityStore = ActivityStore(activityRestClient, activitySqlUtils, dispatcher)
    }

    @Test
    fun onFetchActivitiesActionCallRestClient() {
        val number = 10
        val offset = 0

        activityStore.onAction(ActivityActionBuilder.newFetchActivitiesAction(FetchActivitiesPayload(siteModel, number, offset)))

        verify(activityRestClient).fetchActivity(siteModel, number, offset)
    }

    @Test
    fun onFetchRewindStatusActionCallRestClient() {
        val number = 10
        val offset = 0

        activityStore.onAction(ActivityActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(siteModel, number, offset)))

        verify(activityRestClient).fetchActivityRewind(siteModel, number, offset)
    }

    @Test
    fun getActivitiesBySiteLoadsActivitiesFromDb() {
        activityStore.getActivitiesForSite(siteModel, true)

        verify(activitySqlUtils).getActivitiesForSite(siteModel, SelectQuery.ORDER_ASCENDING)
    }
}
