package org.wordpress.android.fluxc.network.rest.wpcom.activity

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
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
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.*
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivitiesPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType

@RunWith(MockitoJUnitRunner::class)
class ActivityLogRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var restClient: BaseWPComRestClient
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var activityResponseClassCaptor: KArgumentCaptor<Class<ActivityLogRestClient.ActivitiesResponse>>
    private lateinit var rewindStatusResponseClassCaptor: KArgumentCaptor<Class<ActivityLogRestClient.RewindStatusResponse>>
    private lateinit var activitySuccessMethodCaptor: KArgumentCaptor<(ActivityLogRestClient.ActivitiesResponse) -> Unit>
    private lateinit var rewindStatusSuccessMethodCaptor:
            KArgumentCaptor<(ActivityLogRestClient.RewindStatusResponse) -> Unit>
    private lateinit var errorMethodCaptor: KArgumentCaptor<(BaseRequest.BaseNetworkError) -> Unit>
    private lateinit var activityActionCaptor: KArgumentCaptor<Action<FetchedActivitiesPayload>>
    private lateinit var rewindStatusActionCaptor: KArgumentCaptor<Action<FetchedRewindStatePayload>>
    private lateinit var activityRestClient: ActivityLogRestClient
    private val siteId: Long = 12
    private val number = 10
    private val offset = 0

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        activityResponseClassCaptor = argumentCaptor()
        rewindStatusResponseClassCaptor = argumentCaptor()
        activitySuccessMethodCaptor = argumentCaptor()
        rewindStatusSuccessMethodCaptor = argumentCaptor()
        errorMethodCaptor = argumentCaptor()
        activityActionCaptor = argumentCaptor()
        rewindStatusActionCaptor = argumentCaptor()
        activityRestClient = ActivityLogRestClient(dispatcher, restClient, wpComGsonRequestBuilder)
    }

    @Test
    fun fetchActivity_passesCorrectParamToBuildRequest() {
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
    fun fetchActivity_dispatchesResponseOnSuccess() {
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", ACTIVITY_RESPONSE_PAGE)
        activitySuccessMethodCaptor.firstValue.invoke(activitiesResponse)

        verify(dispatcher).dispatch(activityActionCaptor.capture())
        with(activityActionCaptor.firstValue) {
            assertEquals(this.type, ActivityAction.FETCHED_ACTIVITIES)
            assertEquals(this.payload.number, number)
            assertEquals(this.payload.offset, offset)
            assertEquals(this.payload.site, site)
            assertEquals(this.payload.activityLogModels.size, 1)
            assertNull(this.payload.error)
            with(this.payload.activityLogModels[0]) {
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
    fun fetchActivity_dispatchesErrorOnMissingActivityId() {
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(activity_id = null)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        activitySuccessMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedActivityError(ActivityErrorType.MISSING_ACTIVITY_ID)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingSummary() {
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(summary = null)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        activitySuccessMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedActivityError(ActivityErrorType.MISSING_SUMMARY)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingContentText() {
        val emptyContent = ActivitiesResponse.Content(null)
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(content = emptyContent)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        activitySuccessMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedActivityError(ActivityErrorType.MISSING_CONTENT_TEXT)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingPublishedDate() {
        val failingPage = ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE.copy(published = null)))
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        activitySuccessMethodCaptor.firstValue.invoke(activitiesResponse)

        assertEmittedActivityError(ActivityErrorType.MISSING_PUBLISHED_DATE)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnFailure() {
        val request = initFetchActivity()

        activityRestClient.fetchActivity(site, number, offset)

        verify(restClient).add(request)

        errorMethodCaptor.firstValue(BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.NETWORK_ERROR))

        assertEmittedActivityError(ActivityErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityRewind_dispatchesResponseOnSuccess() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        val state = RewindStatusModel.State.ACTIVE
        val rewindResponse = REWIND_RESPONSE.copy(state = state.value)
        rewindStatusSuccessMethodCaptor.firstValue.invoke(rewindResponse)

        verify(dispatcher).dispatch(rewindStatusActionCaptor.capture())
        with(rewindStatusActionCaptor.firstValue) {
            assertEquals(this.type, ActivityAction.FETCHED_REWIND_STATE)
            assertEquals(this.payload.site, site)
            assertNull(this.payload.error)
            assertNotNull(this.payload.rewindStatusModelResponse)
            this.payload.rewindStatusModelResponse?.apply {
                assertEquals(this.reason, REWIND_RESPONSE.reason)
                assertEquals(this.state, state)
                assertNotNull(this.restore)
                this.restore?.apply {
                    assertEquals(this.message, RESTORE_RESPONSE.message)
                    assertEquals(this.status.value, RESTORE_RESPONSE.status)
                    assertEquals(this.progress, RESTORE_RESPONSE.progress)
                    assertEquals(this.id, RESTORE_RESPONSE.rewind_id)
                    assertEquals(this.errorCode, RESTORE_RESPONSE.error_code)
                    assertEquals(this.failureReason, RESTORE_RESPONSE.reason)
                }
            }
        }
    }

    @Test
    fun fetchActivityRewind_dispatchesGenericErrorOnFailure() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        errorMethodCaptor.firstValue(BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.NETWORK_ERROR))

        assertEmittedRewindStatusError(RewindStatusErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnMissingState() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        val rewindResponse = REWIND_RESPONSE.copy(state = null)
        rewindStatusSuccessMethodCaptor.firstValue.invoke(rewindResponse)

        assertEmittedRewindStatusError(RewindStatusErrorType.MISSING_STATE)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnWrongState() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        val rewindResponse = REWIND_RESPONSE.copy(state = "wrong")
        rewindStatusSuccessMethodCaptor.firstValue.invoke(rewindResponse)

        assertEmittedRewindStatusError(RewindStatusErrorType.INVALID_REWIND_STATE)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnMissingRestoreId() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        val rewindResponse = REWIND_RESPONSE.copy(restoreResponse = RESTORE_RESPONSE.copy(rewind_id = null))
        rewindStatusSuccessMethodCaptor.firstValue.invoke(rewindResponse)

        assertEmittedRewindStatusError(RewindStatusErrorType.MISSING_RESTORE_ID)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnMissingRestoreStatus() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        val rewindResponse = REWIND_RESPONSE.copy(restoreResponse = RESTORE_RESPONSE.copy(status = null))
        rewindStatusSuccessMethodCaptor.firstValue.invoke(rewindResponse)

        assertEmittedRewindStatusError(RewindStatusErrorType.MISSING_RESTORE_STATUS)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnWrongRestoreStatus() {
        val request = initFetchRewindStatus()

        activityRestClient.fetchActivityRewind(site)

        verify(restClient).add(request)

        val rewindResponse = REWIND_RESPONSE.copy(restoreResponse = RESTORE_RESPONSE.copy(status = "wrong"))
        rewindStatusSuccessMethodCaptor.firstValue.invoke(rewindResponse)

        assertEmittedRewindStatusError(RewindStatusErrorType.INVALID_RESTORE_STATUS)
    }

    private fun assertEmittedActivityError(errorType: ActivityErrorType) {
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

    private fun assertEmittedRewindStatusError(errorType: RewindStatusErrorType) {
        verify(dispatcher).dispatch(rewindStatusActionCaptor.capture())
        with(rewindStatusActionCaptor.firstValue) {
            assertEquals(this.type, ActivityAction.FETCHED_REWIND_STATE)
            assertEquals(this.payload.site, site)
            assertTrue(this.payload.isError)
            assertEquals(this.payload.error.type, errorType)
        }
    }

    private fun initFetchActivity(): WPComGsonRequest<ActivityLogRestClient.ActivitiesResponse> {
        val request = mock<WPComGsonRequest<ActivityLogRestClient.ActivitiesResponse>>()

        whenever(wpComGsonRequestBuilder.buildGetRequest(urlCaptor.capture(),
                paramsCaptor.capture(),
                activityResponseClassCaptor.capture(),
                activitySuccessMethodCaptor.capture(),
                errorMethodCaptor.capture())).thenReturn(request)
        whenever(site.siteId).thenReturn(siteId)
        return request
    }

    private fun initFetchRewindStatus(): WPComGsonRequest<ActivityLogRestClient.RewindStatusResponse> {
        val request = mock<WPComGsonRequest<ActivityLogRestClient.RewindStatusResponse>>()

        whenever(wpComGsonRequestBuilder.buildGetRequest(urlCaptor.capture(),
                paramsCaptor.capture(),
                rewindStatusResponseClassCaptor.capture(),
                rewindStatusSuccessMethodCaptor.capture(),
                errorMethodCaptor.capture())).thenReturn(request)
        whenever(site.siteId).thenReturn(siteId)
        return request
    }
}
