package org.wordpress.android.fluxc.network.rest.wpcom.activity

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ActivityAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityRestClient.*
import org.wordpress.android.fluxc.store.ActivityErrorType
import org.wordpress.android.fluxc.store.FetchedActivitiesPayload

@RunWith(MockitoJUnitRunner::class)
class ActivityRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var restClient: BaseWPComRestClient
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var classCaptor: KArgumentCaptor<Class<ActivityRestClient.ActivitiesResponse>>
    private lateinit var successMethodCaptor: KArgumentCaptor<(ActivityRestClient.ActivitiesResponse) -> Unit>
    private lateinit var errorMethodCaptor: KArgumentCaptor<(BaseRequest.BaseNetworkError) -> Unit>
    private lateinit var activityActionCaptor: KArgumentCaptor<Action<FetchedActivitiesPayload>>
    private lateinit var activityRestClient: ActivityRestClient
    private val siteId: Long = 12
    private val number = 10
    private val offset = 0

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        classCaptor = argumentCaptor()
        successMethodCaptor = argumentCaptor()
        errorMethodCaptor = argumentCaptor()
        activityActionCaptor = argumentCaptor()
        activityRestClient = ActivityRestClient(dispatcher, restClient, wpComGsonRequestBuilder)
    }

    @Test
    fun passesCorrectParamToBuildRequest() {
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        assertEquals(urlCaptor.firstValue, "https://public-api.wordpress.com/wpcom/v2/sites/$siteId/activity/")
        with(paramsCaptor.firstValue) {
            assertEquals(this["page"], "1")
            assertEquals(this["number"], "$number")
        }
    }

    @Test
    fun dispatchesResponseOnSuccess() {
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", ACTIVITY_RESPONSE_PAGE)
        successMethodCaptor.firstValue.invoke(activitiesResponse)

        verify(dispatcher).dispatch(activityActionCaptor.capture())
        with(activityActionCaptor.firstValue) {
            assertEquals(this.type, ActivityAction.FETCHED_ACTIVITIES)
            assertEquals(this.payload.number, number)
            assertEquals(this.payload.offset, offset)
            assertEquals(this.payload.site, site)
            assertEquals(this.payload.activityModelRespons.size, 1)
            assertNull(this.payload.error)
            with(this.payload.activityModelRespons[0]) {
                assertEquals(this.activityID, ACTIVITY_RESPONSE.activity_id)
                assertEquals(this.gridicon, ACTIVITY_RESPONSE.gridicon)
                assertEquals(this.isDiscarded, ACTIVITY_RESPONSE.is_discarded)
                assertEquals(this.name, ACTIVITY_RESPONSE.name)
                assertEquals(this.published, ACTIVITY_RESPONSE.published)
                assertEquals(this.rewindID, ACTIVITY_RESPONSE.rewind_id)
                assertEquals(this.rewindable, ACTIVITY_RESPONSE.is_rewindable)
                assertEquals(this.text, ACTIVITY_RESPONSE.content?.text)
                assertEquals(this.actor?.avatarURL, ACTIVITY_RESPONSE.actor?.icon?.url)
                assertEquals(this.actor?.wpcomUserID, ACTIVITY_RESPONSE.actor?.wpcom_user_id)
            }
        }
    }

    @Test
    fun dispatchesErrorOnMissingActivityId() {
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(activity_id = null)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        successMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedError(ActivityErrorType.MISSING_ACTIVITY_ID)
    }

    @Test
    fun dispatchesErrorOnMissingSummary() {
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(summary = null)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        successMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedError(ActivityErrorType.MISSING_SUMMARY)
    }

    @Test
    fun dispatchesErrorOnMissingContentText() {
        val emptyContent = ActivitiesResponse.Content(null)
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(content = emptyContent)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        successMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedError(ActivityErrorType.MISSING_CONTENT_TEXT)
    }

    @Test
    fun dispatchesErrorOnMissingPublishedDate() {
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(published = null)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        successMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedError(ActivityErrorType.MISSING_PUBLISHED_DATE)
    }

    @Test
    fun dispatchesErrorOnFailure() {
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        errorMethodCaptor.firstValue(BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.NETWORK_ERROR))

        assertEmittedError(ActivityErrorType.GENERIC_ERROR)
    }

    private fun assertEmittedError(errorType: ActivityErrorType) {
        verify(dispatcher).dispatch(activityActionCaptor.capture())
        with(activityActionCaptor.firstValue) {
            assertEquals(this.type, ActivityAction.FETCHED_ACTIVITIES)
            assertEquals(this.payload.number, number)
            assertEquals(this.payload.offset, offset)
            assertEquals(this.payload.site, site)
            assertTrue(this.payload.isError)
            assertEquals(this.payload.error.type, errorType)
        }
    }

    private fun initFetchActivity(): WPComGsonRequest<ActivityRestClient.ActivitiesResponse> {
        val request = mock<WPComGsonRequest<ActivityRestClient.ActivitiesResponse>>()

        whenever(wpComGsonRequestBuilder.buildGetRequest(urlCaptor.capture(),
                paramsCaptor.capture(),
                classCaptor.capture(),
                successMethodCaptor.capture(),
                errorMethodCaptor.capture())).thenReturn(request)
        whenever(site.siteId).thenReturn(siteId)
        return request
    }
}
