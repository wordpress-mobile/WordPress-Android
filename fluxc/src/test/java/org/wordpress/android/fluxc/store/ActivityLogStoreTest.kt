package org.wordpress.android.fluxc.store

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient

@RunWith(MockitoJUnitRunner::class)
class ActivityLogStoreTest {
    @Mock private lateinit var activityLogRestClient: ActivityLogRestClient
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var mActivityLogStore: ActivityLogStore

    @Before
    fun setUp() {
        mActivityLogStore = ActivityLogStore(activityLogRestClient, dispatcher)
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
}
