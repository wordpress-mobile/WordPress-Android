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
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityRestClient

@RunWith(MockitoJUnitRunner::class)
class ActivityStoreTest {
    @Mock private lateinit var activityRestClient: ActivityRestClient
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var activityStore: ActivityStore

    @Before
    fun setUp() {
        activityStore = ActivityStore(activityRestClient, dispatcher)
    }

    @Test
    fun onFetchActivitiesActionCallRestClient() {
        val number = 10
        val offset = 0

        val action = ActivityActionBuilder.newFetchActivitiesAction(FetchActivitiesPayload(siteModel, number, offset))
        activityStore.onAction(action)

        verify(activityRestClient).fetchActivity(siteModel, number, offset)
    }

    @Test
    fun onFetchRewindStatusActionCallRestClient() {
        val number = 10
        val offset = 0

        val action = ActivityActionBuilder.newFetchRewindStateAction(FetchRewindStatePayload(siteModel, number, offset))
        activityStore.onAction(action)

        verify(activityRestClient).fetchActivityRewind(siteModel, number, offset)
    }
}
