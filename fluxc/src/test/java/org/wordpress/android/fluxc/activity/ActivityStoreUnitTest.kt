package org.wordpress.android.fluxc.activity

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ActivityActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.ActivityStore

@RunWith(MockitoJUnitRunner::class)
class ActivityStoreUnitTest {
    @Mock private lateinit var activityRestClient: ActivityRestClient
    @Mock private lateinit var dispatcher: Dispatcher
    private lateinit var activityStore: ActivityStore

    @Before
    fun setUp() {
        activityStore = ActivityStore(activityRestClient, dispatcher)
    }

    @Test
    fun onFetchActionCallRestClient() {
        val siteModel = mock<SiteModel>()
        val number = 10
        val offset = 0

        activityStore.onAction(ActivityActionBuilder.newFetchActivitiesAction(ActivityStore.FetchActivitiesPayload(siteModel, number, offset)))

        verify(activityRestClient).fetchActivity(siteModel, number, offset)
    }
}
