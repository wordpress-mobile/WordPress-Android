package org.wordpress.android.fluxc.network.rest.wpcom.activity

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Credentials
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityError
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityTypesError
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityTypesErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadResultPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadStatusErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.DismissBackupDownloadError
import org.wordpress.android.fluxc.store.ActivityLogStore.DismissBackupDownloadErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.DismissBackupDownloadResultPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityTypesResultPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedBackupDownloadStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedRewindStatePayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType.API_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindResultPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusError
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindStatusErrorType
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.utils.NetworkErrorMapper
import org.wordpress.android.fluxc.utils.TimeZoneProvider
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ActivityLogRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val timeZoneProvider: TimeZoneProvider,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchActivity(payload: FetchActivityLogPayload, number: Int, offset: Int): FetchedActivityLogPayload {
        val url = WPCOMV2.sites.site(payload.site.siteId).activity.url
        val params = buildParams(offset, number, payload)
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, ActivitiesResponse::class.java)
        return when (response) {
            is Success -> {
                val activities = response.data.current?.orderedItems ?: listOf()
                val totalItems = response.data.totalItems ?: 0
                buildActivityPayload(activities, payload.site, totalItems, number, offset)
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                        response.error,
                        GENERIC_ERROR,
                        INVALID_RESPONSE,
                        AUTHORIZATION_REQUIRED
                )
                val error = ActivityError(errorType, response.error.message)
                FetchedActivityLogPayload(error, payload.site, number = number, offset = offset)
            }
        }
    }

    suspend fun fetchActivityRewind(site: SiteModel): FetchedRewindStatePayload {
        val url = WPCOMV2.sites.site(site.siteId).rewind.url
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), RewindStatusResponse::class.java)
        return when (response) {
            is Success -> {
                buildRewindStatusPayload(response.data, site)
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                        response.error,
                        RewindStatusErrorType.GENERIC_ERROR,
                        RewindStatusErrorType.INVALID_RESPONSE,
                        RewindStatusErrorType.AUTHORIZATION_REQUIRED
                )
                val error = RewindStatusError(errorType, response.error.message)
                FetchedRewindStatePayload(error, site)
            }
        }
    }

    suspend fun rewind(site: SiteModel, rewindId: String, types: RewindRequestTypes? = null): RewindResultPayload {
        val url = WPCOMREST.activity_log.site(site.siteId).rewind.to.rewind(rewindId).urlV1
        val typesBody = if (types != null) {
            mapOf("types" to types)
        } else {
            mapOf()
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(this, url, null, typesBody, RewindResponse::class.java)
        return when (response) {
            is Success -> {
                if (response.data.ok != true && (response.data.error != null && response.data.error.isNotEmpty())) {
                    RewindResultPayload(RewindError(API_ERROR, response.data.error), rewindId, site)
                } else {
                    RewindResultPayload(rewindId, response.data.restore_id, site)
                }
            }
            is Error -> {
                val error = RewindError(
                        NetworkErrorMapper.map(
                                response.error,
                                RewindErrorType.GENERIC_ERROR,
                                RewindErrorType.INVALID_RESPONSE,
                                RewindErrorType.AUTHORIZATION_REQUIRED
                        ), response.error.message
                )
                RewindResultPayload(error, rewindId, site)
            }
        }
    }

    suspend fun backupDownload(
        site: SiteModel,
        rewindId: String,
        types: BackupDownloadRequestTypes
    ): BackupDownloadResultPayload {
        val url = WPCOMV2.sites.site(site.siteId).rewind.downloads.url
        val request = mapOf("rewindId" to rewindId, "types" to types)
        val response =
                wpComGsonRequestBuilder.syncPostRequest(this, url, null, request, BackupDownloadResponse::class.java)
        return when (response) {
            is Success -> {
                BackupDownloadResultPayload(
                        response.data.rewindId,
                        response.data.downloadId,
                        response.data.backupPoint,
                        response.data.startedAt,
                        response.data.progress,
                        site
                )
            }
            is Error -> {
                val error = BackupDownloadError(
                        NetworkErrorMapper.map(
                                response.error,
                                BackupDownloadErrorType.GENERIC_ERROR,
                                BackupDownloadErrorType.INVALID_RESPONSE,
                                BackupDownloadErrorType.AUTHORIZATION_REQUIRED
                        ), response.error.message
                )
                BackupDownloadResultPayload(error, rewindId, site)
            }
        }
    }

    suspend fun fetchBackupDownloadState(site: SiteModel): FetchedBackupDownloadStatePayload {
        val url = WPCOMV2.sites.site(site.siteId).rewind.downloads.url
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                mapOf(),
                Array<BackupDownloadStatusResponse>::class.java
        )
        return when (response) {
            is Success -> {
                if (response.data.isNotEmpty()) {
                    buildBackupDownloadStatusPayload(response.data[0], site)
                } else {
                    FetchedBackupDownloadStatePayload(null, site)
                }
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                        response.error,
                        BackupDownloadStatusErrorType.GENERIC_ERROR,
                        BackupDownloadStatusErrorType.INVALID_RESPONSE,
                        BackupDownloadStatusErrorType.AUTHORIZATION_REQUIRED
                )
                val error = BackupDownloadStatusError(errorType, response.error.message)
                FetchedBackupDownloadStatePayload(error, site)
            }
        }
    }

    suspend fun fetchActivityTypes(remoteSiteId: Long, after: Date?, before: Date?): FetchedActivityTypesResultPayload {
        val url = WPCOMV2.sites.site(remoteSiteId).activity.count.group.url
        val params = mutableMapOf<String, String>()
        addDateRangeParams(params, after, before)

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                ActivityTypesResponse::class.java
        )
        return when (response) {
            is Success -> buildActivityTypesPayload(response.data, remoteSiteId)
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                        response.error,
                        ActivityTypesErrorType.GENERIC_ERROR,
                        ActivityTypesErrorType.INVALID_RESPONSE,
                        ActivityTypesErrorType.AUTHORIZATION_REQUIRED
                )
                val error = ActivityTypesError(errorType, response.error.message)
                FetchedActivityTypesResultPayload(error, remoteSiteId)
            }
        }
    }

    suspend fun dismissBackupDownload(
        site: SiteModel,
        downloadId: Long
    ): DismissBackupDownloadResultPayload {
        val url = WPCOMV2.sites.site(site.siteId).rewind.downloads.download(downloadId).url
        val request = mapOf("dismissed" to true.toString())
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                DismissBackupDownloadResponse::class.java
        )
        return when (response) {
            is Success -> DismissBackupDownloadResultPayload(
                    site.siteId,
                    response.data.downloadId,
                    response.data.isDismissed
            )
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                        response.error,
                        DismissBackupDownloadErrorType.GENERIC_ERROR,
                        DismissBackupDownloadErrorType.INVALID_RESPONSE,
                        DismissBackupDownloadErrorType.AUTHORIZATION_REQUIRED
                )
                val error = DismissBackupDownloadError(errorType, response.error.message)
                DismissBackupDownloadResultPayload(error, site.siteId, downloadId)
            }
        }
    }

    private fun buildParams(
        offset: Int,
        number: Int,
        payload: FetchActivityLogPayload
    ): MutableMap<String, String> {
        val pageNumber = offset / number + 1
        val params = mutableMapOf(
                "page" to pageNumber.toString(),
                "number" to number.toString()
        )

        addDateRangeParams(params, payload.after, payload.before)
        payload.groups.forEachIndexed { index, value ->
            params["group[$index]"] = value
        }
        return params
    }

    private fun addDateRangeParams(
        params: MutableMap<String, String>,
        after: Date? = null,
        before: Date? = null
    ) {
        after?.let {
            val offset = timeZoneProvider.getDefaultTimeZone().getOffset(it.time)
            params["after"] = DateTimeUtils.iso8601UTCFromDate(Date(it.time - offset))
        }
        before?.let {
            val offset = timeZoneProvider.getDefaultTimeZone().getOffset(it.time)
            params["before"] = DateTimeUtils.iso8601UTCFromDate(Date(it.time - offset))
        }
    }

    private fun buildActivityPayload(
        activityResponses: List<ActivitiesResponse.ActivityResponse>,
        site: SiteModel,
        totalItems: Int,
        number: Int,
        offset: Int
    ): FetchedActivityLogPayload {
        var error: ActivityLogErrorType? = null

        val activities = activityResponses.mapNotNull {
            when {
                it.activity_id == null -> {
                    error = ActivityLogErrorType.MISSING_ACTIVITY_ID
                    null
                }
                it.summary == null -> {
                    error = ActivityLogErrorType.MISSING_SUMMARY
                    null
                }
                it.content?.text == null -> {
                    error = ActivityLogErrorType.MISSING_CONTENT_TEXT
                    null
                }
                it.published == null -> {
                    error = ActivityLogErrorType.MISSING_PUBLISHED_DATE
                    null
                }
                else -> {
                    ActivityLogModel(
                            activityID = it.activity_id,
                            summary = it.summary,
                            content = it.content,
                            name = it.name,
                            type = it.type,
                            gridicon = it.gridicon,
                            status = it.status,
                            rewindable = it.is_rewindable,
                            rewindID = it.rewind_id,
                            published = it.published,
                            actor = it.actor?.let {
                                ActivityLogModel.ActivityActor(
                                        it.name,
                                        it.type,
                                        it.wpcom_user_id,
                                        it.icon?.url,
                                        it.role
                                )
                            }
                    )
                }
            }
        }
        error?.let {
            return FetchedActivityLogPayload(ActivityError(it), site, totalItems, number, offset)
        }
        return FetchedActivityLogPayload(activities, site, totalItems, number, offset)
    }

    @Suppress("ReturnCount")
    private fun buildRewindStatusPayload(response: RewindStatusResponse, site: SiteModel):
            FetchedRewindStatePayload {
        val state = RewindStatusModel.State.fromValue(response.state)
                ?: return buildErrorPayload(site, RewindStatusErrorType.INVALID_RESPONSE)
        val reason = RewindStatusModel.Reason.fromValue(response.reason)
        val rewindModel = response.rewind?.let {
            val rewindId = it.rewind_id
                    ?: return buildErrorPayload(site, RewindStatusErrorType.MISSING_REWIND_ID)
            val restoreId = it.restore_id
                    ?: return buildErrorPayload(site, RewindStatusErrorType.MISSING_RESTORE_ID)
            val restoreStatusValue = it.status
            val restoreStatus = RewindStatusModel.Rewind.Status.fromValue(restoreStatusValue)
                    ?: return buildErrorPayload(site, RewindStatusErrorType.INVALID_REWIND_STATE)
            RewindStatusModel.Rewind(
                    rewindId = rewindId,
                    restoreId = restoreId,
                    status = restoreStatus,
                    progress = it.progress,
                    reason = it.reason,
                    message = it.message,
                    currentEntry = it.currentEntry
            )
        }

        val rewindStatusModel = RewindStatusModel(
                state = state,
                reason = reason,
                lastUpdated = response.last_updated,
                canAutoconfigure = response.can_autoconfigure,
                credentials = response.credentials?.map {
                    Credentials(it.type, it.role, it.host, it.port, it.still_valid)
                },
                rewind = rewindModel
        )
        return FetchedRewindStatePayload(rewindStatusModel, site)
    }

    private fun buildErrorPayload(site: SiteModel, errorType: RewindStatusErrorType) =
            FetchedRewindStatePayload(RewindStatusError(errorType), site)

    private fun buildBackupDownloadStatusPayload(response: BackupDownloadStatusResponse, site: SiteModel):
            FetchedBackupDownloadStatePayload {
        val statusModel = BackupDownloadStatusModel(
                rewindId = response.rewindId,
                downloadId = response.downloadId,
                backupPoint = response.backupPoint,
                startedAt = response.startedAt,
                progress = response.progress,
                downloadCount = response.downloadCount,
                validUntil = response.validUntil,
                url = response.url
        )
        return FetchedBackupDownloadStatePayload(statusModel, site)
    }

    private fun buildActivityTypesPayload(
        response: ActivityTypesResponse,
        remoteSiteId: Long
    ): FetchedActivityTypesResultPayload {
        val activityTypes = response.groups?.activityTypes
                ?.filter { it.key != null && it.name != null }
                ?.map { ActivityTypeModel(requireNotNull(it.key), requireNotNull(it.name), it.count ?: 0) }
                ?: listOf()

        check(!BuildConfig.DEBUG || (response.groups?.activityTypes?.size ?: 0) == activityTypes.size) {
            "ActivityTypes parsing failed - one or more items were ignored."
        }

        return FetchedActivityTypesResultPayload(
                remoteSiteId,
                activityTypes,
                response.totalItems ?: 0
        )
    }

    @Suppress("ConstructorParameterNaming")
    class ActivitiesResponse(
        val totalItems: Int?,
        val summary: String?,
        val current: Page?,
        // This class is reused in CardsRestClient, the error field is not used for activity log
        val error: String? = null
    ) {
        class Page(val orderedItems: List<ActivityResponse>)

        data class ActivityResponse(
            val summary: String?,
            val content: FormattableContent?,
            val name: String?,
            val actor: Actor?,
            val type: String?,
            val published: Date?,
            val generator: Generator?,
            val is_rewindable: Boolean?,
            val rewind_id: String?,
            val gridicon: String?,
            val status: String?,
            val activity_id: String?
        )

        class Actor(
            val type: String?,
            val name: String?,
            val external_user_id: Long?,
            val wpcom_user_id: Long?,
            val icon: Icon?,
            val role: String?
        )

        class Icon(val type: String?, val url: String?, val width: Int?, val height: Int?)

        class Generator(val jetpack_version: Float?, val blog_id: Long?)
    }

    @Suppress("ConstructorParameterNaming")
    data class RewindStatusResponse(
        val state: String,
        val reason: String?,
        val last_updated: Date,
        val can_autoconfigure: Boolean?,
        val credentials: List<Credentials>?,
        val rewind: Rewind?,
        val message: String?,
        val currentEntry: String?
    ) {
        data class Credentials(
            val type: String,
            val role: String,
            val host: String?,
            val port: Int?,
            val still_valid: Boolean
        )

        data class Rewind(
            val site_id: String?,
            val status: String?,
            val restore_id: Long?,
            val rewind_id: String?,
            val progress: Int?,
            val reason: String?,
            val message: String?,
            val currentEntry: String?
        )
    }

    @Suppress("ConstructorParameterNaming")
    class RewindResponse(
        val restore_id: Long,
        val ok: Boolean?,
        val error: String?
    )

    class BackupDownloadResponse(
        val downloadId: Long,
        val rewindId: String,
        val backupPoint: String,
        val startedAt: String,
        val progress: Int
    )

    data class BackupDownloadStatusResponse(
        val downloadId: Long,
        val rewindId: String,
        val backupPoint: Date,
        val startedAt: Date,
        val progress: Int?,
        val downloadCount: Int?,
        val validUntil: Date?,
        val url: String?
    )

    data class ActivityTypesResponse(
        @JsonAdapter(ActivityTypesDeserializer::class) val groups: Groups?,
        val totalItems: Int?
    ) : Response {
        data class Groups(
            val activityTypes: List<ActivityType>
        )

        data class ActivityType(
            val key: String?,
            val name: String?,
            val count: Int?
        )
    }

    class DismissBackupDownloadResponse(
        val downloadId: Long,
        val isDismissed: Boolean
    )
}
