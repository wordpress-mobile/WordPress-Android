package org.wordpress.android.fluxc.network.rest.wpcom.activity

import com.android.volley.RequestQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Reason
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivitiesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivitiesResponse.Page
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivityTypesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivityTypesResponse.ActivityType
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivityTypesResponse.Groups
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.BackupDownloadResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.BackupDownloadStatusResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.DismissBackupDownloadResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.RewindResponse
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.RewindStatusResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityTypesErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.utils.TimeZoneProvider
import java.util.Date
import java.util.TimeZone

private const val DATE_1_IN_MILLIS = 1578614400000L // 2020-01-10T00:00:00+00:00
private const val DATE_2_IN_MILLIS = 1578787200000L // 2020-01-12T00:00:00+00:00

@RunWith(MockitoJUnitRunner::class)
class ActivityLogRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestPayload: FetchActivityLogPayload
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var timeZoneProvider: TimeZoneProvider
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var activityRestClient: ActivityLogRestClient
    private val siteId: Long = 12
    private val number = 10
    private val offset = 0

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        activityRestClient = ActivityLogRestClient(
                wpComGsonRequestBuilder,
                timeZoneProvider,
                dispatcher,
                null,
                requestQueue,
                accessToken,
                userAgent)
        whenever(requestPayload.site).thenReturn(site)
        whenever(timeZoneProvider.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"))
    }

    @Test
    fun fetchActivity_passesCorrectParamToBuildRequest() = test {
        initFetchActivity()
        val payload = FetchActivityLogPayload(
                site,
                false,
                Date(DATE_1_IN_MILLIS),
                Date(DATE_2_IN_MILLIS),
                listOf("post", "attachment")
        )

        activityRestClient.fetchActivity(payload, number, offset)

        assertEquals(urlCaptor.firstValue, "https://public-api.wordpress.com/wpcom/v2/sites/$siteId/activity/")
        with(paramsCaptor.firstValue) {
            assertEquals("1", this["page"])
            assertEquals("$number", this["number"])
            assertNotNull(this["after"])
            assertNotNull(this["before"])
            assertEquals("post", this["group[0]"])
            assertEquals("attachment", this["group[1]"])
        }
    }

    @Test
    fun fetchActivity_passesOnlyNonEmptyParamsToBuildRequest() = test {
        initFetchActivity()
        val payload = FetchActivityLogPayload(
                site,
                false,
                after = null,
                before = null,
                groups = listOf()
        )
        activityRestClient.fetchActivity(payload, number, offset)

        assertEquals(urlCaptor.firstValue, "https://public-api.wordpress.com/wpcom/v2/sites/$siteId/activity/")
        with(paramsCaptor.firstValue) {
            assertEquals("1", this["page"])
            assertEquals("$number", this["number"])
            assertEquals(2, size)
        }
    }

    @Test
    fun fetchActivity_adjustsDateRangeBasedOnTimezoneGMT3() = test {
        val timezoneOffset = 3
        whenever(timeZoneProvider.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("GMT+$timezoneOffset"))
        initFetchActivity()
        val payload = FetchActivityLogPayload(
                site,
                false,
                // 2020-01-10T00:00:00+00:00
                Date(DATE_1_IN_MILLIS),
                // 2020-01-12T00:00:00+00:00
                Date(DATE_2_IN_MILLIS)
        )

        activityRestClient.fetchActivity(payload, number, offset)

        with(paramsCaptor.firstValue) {
            assertEquals("2020-01-09T21:00:00+00:00", this["after"])
            assertEquals("2020-01-11T21:00:00+00:00", this["before"])
        }
    }

    @Test
    fun fetchActivity_adjustsDateRangeBasedOnTimezoneGMTMinus7() = test {
        val timezoneOffset = -7
        whenever(timeZoneProvider.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("GMT$timezoneOffset"))
        initFetchActivity()
        val payload = FetchActivityLogPayload(
                site,
                false,
                // 2020-01-10T00:00:00+00:00
                Date(DATE_1_IN_MILLIS),
                // 2020-01-12T00:00:00+00:00
                Date(DATE_2_IN_MILLIS)
        )

        activityRestClient.fetchActivity(payload, number, offset)

        with(paramsCaptor.firstValue) {
            assertEquals("2020-01-10T07:00:00+00:00", this["after"])
            assertEquals("2020-01-12T07:00:00+00:00", this["before"])
        }
    }

    @Test
    fun fetchActivity_dispatchesResponseOnSuccess() = test {
        val response = ActivitiesResponse(1, "response", ACTIVITY_RESPONSE_PAGE)
        initFetchActivity(response)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        with(payload) {
            assertEquals(this@ActivityLogRestClientTest.number, number)
            assertEquals(this@ActivityLogRestClientTest.offset, offset)
            assertEquals(totalItems, 1)
            assertEquals(this@ActivityLogRestClientTest.site, site)
            assertEquals(activityLogModels.size, 1)
            assertNull(error)
            with(activityLogModels[0]) {
                assertEquals(activityID, ACTIVITY_RESPONSE.activity_id)
                assertEquals(gridicon, ACTIVITY_RESPONSE.gridicon)
                assertEquals(name, ACTIVITY_RESPONSE.name)
                assertEquals(published, ACTIVITY_RESPONSE.published)
                assertEquals(rewindID, ACTIVITY_RESPONSE.rewind_id)
                assertEquals(rewindable, ACTIVITY_RESPONSE.is_rewindable)
                assertEquals(content, ACTIVITY_RESPONSE.content)
                assertEquals(actor?.avatarURL, ACTIVITY_RESPONSE.actor?.icon?.url)
                assertEquals(actor?.wpcomUserID, ACTIVITY_RESPONSE.actor?.wpcom_user_id)
            }
        }
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingActivityId() = test {
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(activity_id = null)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_ACTIVITY_ID)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingSummary() = test {
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(summary = null)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_SUMMARY)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingContentText() = test {
        val emptyContent = FormattableContent(null)
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(content = emptyContent)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_CONTENT_TEXT)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnMissingPublishedDate() = test {
        val failingPage = Page(listOf(ACTIVITY_RESPONSE.copy(published = null)))
        val activitiesResponse = ActivitiesResponse(1, "response", failingPage)
        initFetchActivity(activitiesResponse)

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.MISSING_PUBLISHED_DATE)
    }

    @Test
    fun fetchActivity_dispatchesErrorOnFailure() = test {
        initFetchActivity(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = activityRestClient.fetchActivity(requestPayload, number, offset)

        assertEmittedActivityError(payload, ActivityLogErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityRewind_dispatchesResponseOnSuccess() = test {
        val state = State.ACTIVE
        val rewindResponse = REWIND_STATUS_RESPONSE.copy(state = state.value)
        initFetchRewindStatus(rewindResponse)

        val payload = activityRestClient.fetchActivityRewind(site)

        with(payload) {
            assertEquals(this@ActivityLogRestClientTest.site, site)
            assertNull(error)
            assertNotNull(rewindStatusModelResponse)
            rewindStatusModelResponse?.apply {
                assertEquals(reason, Reason.UNKNOWN)
                assertEquals(state, state)
                assertNotNull(rewind)
                rewind?.apply {
                    assertEquals(status.value, REWIND_RESPONSE.status)
                }
            }
        }
    }

    @Test
    fun fetchActivityRewind_dispatchesGenericErrorOnFailure() = test {
        initFetchRewindStatus(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnWrongState() = test {
        initFetchRewindStatus(REWIND_STATUS_RESPONSE.copy(state = "wrong"))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.INVALID_RESPONSE)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnMissingRestoreId() = test {
        initFetchRewindStatus(REWIND_STATUS_RESPONSE.copy(rewind = REWIND_RESPONSE.copy(rewind_id = null)))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.MISSING_REWIND_ID)
    }

    @Test
    fun fetchActivityRewind_dispatchesErrorOnWrongRestoreStatus() = test {
        initFetchRewindStatus(REWIND_STATUS_RESPONSE.copy(rewind = REWIND_RESPONSE.copy(status = "wrong")))

        val payload = activityRestClient.fetchActivityRewind(site)

        assertEmittedRewindStatusError(payload, RewindStatusErrorType.INVALID_REWIND_STATE)
    }

    @Test
    fun postRewindOperation() = test {
        val restoreId = 10L
        val response = RewindResponse(restoreId, true, null)
        initPostRewind(response)

        val payload = activityRestClient.rewind(site, "rewindId")

        assertEquals(restoreId, payload.restoreId)
    }

    @Test
    fun postRewindOperationError() = test {
        initPostRewind(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = activityRestClient.rewind(site, "rewindId")

        assertTrue(payload.isError)
    }

    @Test
    fun postRewindApiError() = test {
        val restoreId = 10L
        initPostRewind(RewindResponse(restoreId, false, "error"))

        val payload = activityRestClient.rewind(site, "rewindId")

        assertTrue(payload.isError)
    }

    @Test
    fun postRewindOperationWithTypes() = test {
        val restoreId = 10L
        val response = RewindResponse(restoreId, true, null)
        val types = RewindRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        initPostRewindWithTypes(data = response, requestTypes = types)

        val payload = activityRestClient.rewind(site, "rewindId", types)

        assertEquals(restoreId, payload.restoreId)
    }

    @Test
    fun postRewindOperationErrorWithTypes() = test {
        val types = RewindRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        initPostRewindWithTypes(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)), requestTypes = types)

        val payload = activityRestClient.rewind(site, "rewindId", types)

        assertTrue(payload.isError)
    }

    @Test
    fun postRewindApiErrorWithTypes() = test {
        val restoreId = 10L
        val types = RewindRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        initPostRewindWithTypes(data = RewindResponse(restoreId, false, "error"), requestTypes = types)

        val payload = activityRestClient.rewind(site, "rewindId", types)

        assertTrue(payload.isError)
    }

    @Test
    fun postBackupDownloadOperation() = test {
        val downloadId = 10L
        val rewindId = "rewind_id"
        val backupPoint = "backup_point"
        val startedAt = "started_at"
        val progress = 0
        val response = BackupDownloadResponse(downloadId, rewindId, backupPoint, startedAt, progress)
        val types = BackupDownloadRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        initPostBackupDownload(rewindId = rewindId, data = response, requestTypes = types)

        val payload = activityRestClient.backupDownload(site, rewindId, types)

        assertEquals(downloadId, payload.downloadId)
    }

    @Test
    fun postBackupDownloadOperationError() = test {
        val rewindId = "rewind_id"
        val types = BackupDownloadRequestTypes(themes = true,
                plugins = true,
                uploads = true,
                sqls = true,
                roots = true,
                contents = true)
        initPostBackupDownload(rewindId = rewindId, error =
            WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)), requestTypes = types)

        val payload = activityRestClient.backupDownload(site, rewindId, types)

        assertTrue(payload.isError)
    }

    @Test
    fun fetchActivityDownload_dispatchesGenericErrorOnFailure() = test {
        initFetchBackupDownloadStatus(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR
                        )
                )
        )

        val payload = activityRestClient.fetchBackupDownloadState(site)

        assertEmittedDownloadStatusError(payload, BackupDownloadStatusErrorType.GENERIC_ERROR)
    }

    @Test
    fun fetchActivityBackupDownload_dispatchesResponseOnSuccess() = test {
        val progress = 55
        val downloadResponse = BACKUP_DOWNLOAD_STATUS_RESPONSE.copy(progress = progress)
        initFetchBackupDownloadStatus(arrayOf(downloadResponse))

        val payload = activityRestClient.fetchBackupDownloadState(site)

        with(payload) {
            assertEquals(this@ActivityLogRestClientTest.site, site)
            assertNull(error)
            assertNotNull(backupDownloadStatusModelResponse)
            backupDownloadStatusModelResponse?.apply {
                assertEquals(downloadId, BACKUP_DOWNLOAD_STATUS_RESPONSE.downloadId)
                assertEquals(rewindId, BACKUP_DOWNLOAD_STATUS_RESPONSE.rewindId)
                assertEquals(backupPoint, BACKUP_DOWNLOAD_STATUS_RESPONSE.backupPoint)
                assertEquals(startedAt, BACKUP_DOWNLOAD_STATUS_RESPONSE.startedAt)
                assertEquals(downloadCount, BACKUP_DOWNLOAD_STATUS_RESPONSE.downloadCount)
                assertEquals(validUntil, BACKUP_DOWNLOAD_STATUS_RESPONSE.validUntil)
                assertEquals(url, BACKUP_DOWNLOAD_STATUS_RESPONSE.url)
                assertEquals(progress, progress)
            }
        }
    }

    @Test
    fun fetchEmptyActivityBackupDownload_dispatchesResponseOnSuccess() = test {
        initFetchBackupDownloadStatus(arrayOf())

        val payload = activityRestClient.fetchBackupDownloadState(site)

        with(payload) {
            assertEquals(site, this@ActivityLogRestClientTest.site)
            assertNull(error)
            assertNull(backupDownloadStatusModelResponse)
        }
    }

    @Test
    fun fetchActivityTypes_dispatchesSuccessResponseOnSuccess() = test {
        initFetchActivityTypes()
        val siteId = 90L

        val payload = activityRestClient.fetchActivityTypes(siteId, null, null)

        with(payload) {
            assertEquals(siteId, remoteSiteId)
            assertEquals(false, isError)
        }
    }

    @Test
    fun fetchActivityTypes_dispatchesGenericErrorOnFailure() = test {
        initFetchActivityTypes(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))
        val siteId = 90L

        val payload = activityRestClient.fetchActivityTypes(siteId, null, null)

        with(payload) {
            assertEquals(siteId, remoteSiteId)
            assertEquals(true, isError)
            assertEquals(error.type, ActivityTypesErrorType.GENERIC_ERROR)
        }
    }

    @Test
    fun fetchActivityTypes_mapsResponseModelsToDomainModels() = test {
        val activityType = ActivityType("key1", "name1", 10)
        initFetchActivityTypes(
                data = ActivityTypesResponse(
                        groups = Groups(
                                activityTypes = listOf(activityType)
                        ),
                        15
                )
        )
        val siteId = site.siteId

        val payload = activityRestClient.fetchActivityTypes(siteId, null, null)

        assertEquals(
                payload.activityTypeModels[0],
                ActivityTypeModel(activityType.key!!, activityType.name!!, activityType.count!!)
        )
    }

    @Test
    fun fetchActivityTypes_passesCorrectParams() = test {
        initFetchActivityTypes()
        val siteId = site.siteId
        val afterMillis = 234124242145
        val beforeMillis = 234124242999
        val after = Date(afterMillis)
        val before = Date(beforeMillis)

        activityRestClient.fetchActivityTypes(siteId, after, before)

        with(paramsCaptor.firstValue) {
            assertNotNull(this["after"])
            assertNotNull(this["before"])
        }
    }

    @Test
    fun postDismissBackupDownloadOperation() = test {
        val downloadId = 10L
        val response = DismissBackupDownloadResponse(downloadId, true)

        initPostDismissBackupDownload(data = response)

        val payload = activityRestClient.dismissBackupDownload(site, downloadId)

        assertEquals(downloadId, payload.downloadId)
    }

    @Test
    fun postDismissBackupDownloadOperationError() = test {
        val downloadId = 10L
        initPostDismissBackupDownload(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val payload = activityRestClient.dismissBackupDownload(site, downloadId)

        assertTrue(payload.isError)
    }

    private suspend fun initFetchActivity(
        data: ActivitiesResponse = mock(),
        error: WPComGsonNetworkError? = null
    ): Response<ActivitiesResponse> {
        val response = if (error != null) Response.Error<ActivitiesResponse>(error) else Success(data)
        whenever(wpComGsonRequestBuilder.syncGetRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(ActivitiesResponse::class.java),
                eq(false),
                any(),
                eq(false))
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initFetchRewindStatus(
        data: RewindStatusResponse = mock(),
        error: WPComGsonNetworkError? = null
    ):
            Response<RewindStatusResponse> {
        val response = if (error != null) Response.Error<RewindStatusResponse>(error) else Success(data)
        whenever(wpComGsonRequestBuilder.syncGetRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(RewindStatusResponse::class.java),
                eq(false),
                any(),
                eq(false))).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initPostRewind(
        data: RewindResponse = mock(),
        error: WPComGsonNetworkError? = null
    ): Response<RewindResponse> {
        val response = if (error != null) Response.Error<RewindResponse>(error) else Success(data)

        whenever(wpComGsonRequestBuilder.syncPostRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                eq(null),
                eq(mapOf()),
                eq(RewindResponse::class.java),
                isNull(),
                anyOrNull(),
        )).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initPostRewindWithTypes(
        data: RewindResponse = mock(),
        error: WPComGsonNetworkError? = null,
        requestTypes: RewindRequestTypes
    ): Response<RewindResponse> {
        val response = if (error != null) Response.Error<RewindResponse>(error) else Success(data)

        whenever(wpComGsonRequestBuilder.syncPostRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                eq(null),
                eq(mapOf("types" to requestTypes)),
                eq(RewindResponse::class.java),
                isNull(),
                anyOrNull(),
        )).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initPostBackupDownload(
        data: BackupDownloadResponse = mock(),
        error: WPComGsonNetworkError? = null,
        requestTypes: BackupDownloadRequestTypes,
        rewindId: String
    ): Response<BackupDownloadResponse> {
        val response = if (error != null) Response.Error<BackupDownloadResponse>(error) else Success(data)

        whenever(wpComGsonRequestBuilder.syncPostRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                eq(null),
                eq(mapOf("rewindId" to rewindId,
                        "types" to requestTypes)),
                eq(BackupDownloadResponse::class.java),
                isNull(),
                anyOrNull(),
        )).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initFetchBackupDownloadStatus(
        data: Array<BackupDownloadStatusResponse>? = null,
        error: WPComGsonNetworkError? = null
    ): Response<Array<BackupDownloadStatusResponse>> {
        val defaultError = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR))
        val response = when {
            error != null -> { Response.Error(error) }
            data != null -> { Success(data) }
            else -> { Response.Error(defaultError) }
        }

        whenever(wpComGsonRequestBuilder.syncGetRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(Array<BackupDownloadStatusResponse>::class.java),
                eq(false),
                any(),
                eq(false))).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private suspend fun initFetchActivityTypes(
        data: ActivityTypesResponse = mock(),
        error: WPComGsonNetworkError? = null
    ): Response<ActivityTypesResponse> {
        val response = if (error != null) Response.Error<ActivityTypesResponse>(error) else Success(data)
        whenever(wpComGsonRequestBuilder.syncGetRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(ActivityTypesResponse::class.java),
                eq(false),
                any(),
                eq(false))
        ).thenReturn(response)
        return response
    }

    private suspend fun initPostDismissBackupDownload(
        data: DismissBackupDownloadResponse = mock(),
        error: WPComGsonNetworkError? = null
    ): Response<DismissBackupDownloadResponse> {
        val response = if (error != null) Response.Error(error) else Success(data)

        whenever(wpComGsonRequestBuilder.syncPostRequest(
                eq(activityRestClient),
                urlCaptor.capture(),
                anyOrNull(),
                eq(mapOf("dismissed" to true.toString())),
                eq(DismissBackupDownloadResponse::class.java),
                isNull(),
                anyOrNull(),
        )).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    private fun assertEmittedActivityError(payload: FetchedActivityLogPayload, errorType: ActivityLogErrorType) {
        with(payload) {
            assertEquals(this@ActivityLogRestClientTest.number, number)
            assertEquals(this@ActivityLogRestClientTest.offset, offset)
            assertEquals(this@ActivityLogRestClientTest.site, site)
            assertTrue(isError)
            assertEquals(error.type, errorType)
        }
    }

    private fun assertEmittedRewindStatusError(payload: FetchedRewindStatePayload, errorType: RewindStatusErrorType) {
        with(payload) {
            assertEquals(this@ActivityLogRestClientTest.site, site)
            assertTrue(isError)
            assertEquals(errorType, error.type)
        }
    }

    private fun assertEmittedDownloadStatusError(
        payload: FetchedBackupDownloadStatePayload,
        errorType: BackupDownloadStatusErrorType
    ) {
        with(payload) {
            assertEquals(this@ActivityLogRestClientTest.site, site)
            assertTrue(isError)
            assertEquals(errorType, error.type)
        }
    }
}
