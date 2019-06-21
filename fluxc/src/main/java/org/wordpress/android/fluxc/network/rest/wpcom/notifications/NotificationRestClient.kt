package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import android.content.Context
import android.os.Build
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationError
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.DeviceUnregistrationError
import org.wordpress.android.fluxc.store.NotificationStore.DeviceUnregistrationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationHashesResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationSeenResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.NotificationAppKey
import org.wordpress.android.fluxc.store.NotificationStore.NotificationError
import org.wordpress.android.fluxc.store.NotificationStore.NotificationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDeviceResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.UnregisterDeviceResponsePayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.PackageUtils
import java.util.Date
import javax.inject.Singleton

@Singleton
class NotificationRestClient constructor(
    private val appContext: Context?,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    companion object {
        const val NOTIFICATION_DEFAULT_FIELDS = "id,type,read,body,subject,timestamp,meta,note_hash"
        const val NOTIFICATION_SYNC_FIELDS = "id,note_hash"
        const val NOTIFICATION_DEFAULT_NUMBER = 200
        const val NOTIFICATION_DEFAULT_NUM_NOTE_ITEMS = 20
    }

    // region Device Registration
    fun registerDeviceForPushNotifications(
        gcmToken: String,
        appKey: NotificationAppKey,
        uuid: String,
        site: SiteModel? = null
    ) {
        val deviceName = DeviceUtils.getInstance().getDeviceName(appContext)
        val params = listOfNotNull(
                "device_token" to gcmToken,
                "device_family" to "android",
                "app_secret_key" to appKey.value,
                "device_name" to deviceName,
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "app_version" to PackageUtils.getVersionName(appContext),
                "version_code" to PackageUtils.getVersionCode(appContext).toString(),
                "os_version" to Build.VERSION.RELEASE,
                "device_uuid" to uuid,
                ("selected_blog_id" to site?.siteId.toString()).takeIf { site != null }
        ).toMap()

        val url = WPCOMREST.devices.new_.urlV1
        val request = WPComGsonRequest.buildPostRequest(
                url, params, RegisterDeviceRestResponse::class.java,
                { response: RegisterDeviceRestResponse? ->
                    response?.let {
                        if (!it.id.isNullOrEmpty()) {
                            val payload = RegisterDeviceResponsePayload(it.id)
                            dispatcher.dispatch(NotificationActionBuilder.newRegisteredDeviceAction(payload))
                        } else {
                            val registrationError =
                                    DeviceRegistrationError(DeviceRegistrationErrorType.MISSING_DEVICE_ID)
                            val payload = RegisterDeviceResponsePayload(registrationError)
                            dispatcher.dispatch(NotificationActionBuilder.newRegisteredDeviceAction(payload))
                        }
                    } ?: run {
                        AppLog.e(T.API, "Response for url $url with param $params is null: $response")
                        val registrationError = DeviceRegistrationError(DeviceRegistrationErrorType.INVALID_RESPONSE)
                        val payload = RegisterDeviceResponsePayload(registrationError)
                        dispatcher.dispatch(NotificationActionBuilder.newRegisteredDeviceAction(payload))
                    }
                },
                { wpComError ->
                    val registrationError = networkErrorToRegistrationError(wpComError)
                    val payload = RegisterDeviceResponsePayload(registrationError)
                    dispatcher.dispatch(NotificationActionBuilder.newRegisteredDeviceAction(payload))
                })
        add(request)
    }

    fun unregisterDeviceForPushNotifications(deviceId: String) {
        val url = WPCOMREST.devices.deviceId(deviceId).delete.urlV1
        val request = WPComGsonRequest.buildPostRequest(
                url, null, Any::class.java,
                {
                    val payload = UnregisterDeviceResponsePayload()
                    dispatcher.dispatch(NotificationActionBuilder.newUnregisteredDeviceAction(payload))
                },
                { wpComError ->
                    val payload = UnregisterDeviceResponsePayload(DeviceUnregistrationError(
                            DeviceUnregistrationErrorType.GENERIC_ERROR, wpComError.message))
                    dispatcher.dispatch(NotificationActionBuilder.newUnregisteredDeviceAction(payload))
                })
        add(request)
    }
    // endregion

    /**
     * Requests a fresh batch of notifications from the api containing only the fields "id" and "note_hash".
     *
     * API endpoint:
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    fun fetchNotificationHashes() {
        val url = WPCOMREST.notifications.urlV1_1
        val params = mapOf(
                "number" to NOTIFICATION_DEFAULT_NUMBER.toString(),
                "num_note_items" to NOTIFICATION_DEFAULT_NUM_NOTE_ITEMS.toString(),
                "fields" to NOTIFICATION_SYNC_FIELDS)
        val request = WPComGsonRequest.buildGetRequest(url, params, NotificationHashesApiResponse::class.java,
                { response: NotificationHashesApiResponse? ->
                    // Create a map of remote id to note_hash
                    val hashesMap: Map<Long, Long> =
                            response?.notes?.map { it.id to it.note_hash }?.toMap() ?: emptyMap()
                    val payload = FetchNotificationHashesResponsePayload(hashesMap)
                    dispatcher.dispatch(NotificationActionBuilder.newFetchedNotificationHashesAction(payload))
                },
                { networkError ->
                    val payload = FetchNotificationHashesResponsePayload().apply {
                        error = NotificationError(
                                NotificationErrorType.fromString(networkError.apiError),
                                networkError.message)
                    }
                    dispatcher.dispatch(NotificationActionBuilder.newFetchedNotificationHashesAction(payload))
                })
        add(request)
    }

    /**
     * Fetch the latest list of notifications.
     *
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     *
     * @param siteStore A reference to [SiteStore] used for finding and populating the localSiteId property of
     * [NotificationModel]
     * @param remoteNoteIds Optional. A list of remote notification ids to be fetched from the remote api
     */
    fun fetchNotifications(siteStore: SiteStore, remoteNoteIds: List<Long>? = null) {
        val url = WPCOMREST.notifications.urlV1_1
        val params = mutableMapOf(
                "number" to NOTIFICATION_DEFAULT_NUMBER.toString(),
                "num_note_items" to NOTIFICATION_DEFAULT_NUM_NOTE_ITEMS.toString(),
                "fields" to NOTIFICATION_DEFAULT_FIELDS)

        remoteNoteIds?.let { if (it.isNotEmpty()) params["ids"] = it.joinToString() }

        val request = WPComGsonRequest.buildGetRequest(url, params, NotificationsApiResponse::class.java,
                { response: NotificationsApiResponse? ->
                    val lastSeenTime = response?.last_seen_time?.let {
                        Date(it)
                    }
                    val notifications = response?.notes?.map { it ->
                        NotificationApiResponse.notificationResponseToNotificationModel(it)
                    } ?: listOf()
                    val payload = FetchNotificationsResponsePayload(notifications, lastSeenTime)
                    dispatcher.dispatch(NotificationActionBuilder.newFetchedNotificationsAction(payload))
                },
                { networkError ->
                    val payload = FetchNotificationsResponsePayload().apply {
                        error = NotificationError(
                                NotificationErrorType.fromString(networkError.apiError),
                                networkError.message)
                    }
                    dispatcher.dispatch(NotificationActionBuilder.newFetchedNotificationsAction(payload))
                })
        add(request)
    }

    /**
     * Fetch a single notification by it's remote note_id.
     *
     * https://developer.wordpress.com/docs/api/1/get/notifications/%s
     */
    fun fetchNotification(remoteNoteId: Long) {
        val url = WPCOMREST.notifications.note(remoteNoteId).urlV1_1
        val params = mapOf(
                "fields" to NOTIFICATION_DEFAULT_FIELDS)
        val request = WPComGsonRequest.buildGetRequest(url, params, NotificationsApiResponse::class.java,
                { response ->
                    val notification = response?.notes?.firstOrNull()?.let {
                        NotificationApiResponse.notificationResponseToNotificationModel(it)
                    }
                    val payload = FetchNotificationResponsePayload(notification)
                    dispatcher.dispatch(NotificationActionBuilder.newFetchedNotificationAction(payload))
                },
                { networkError ->
                    val payload = FetchNotificationResponsePayload().apply {
                        error = NotificationError(
                                NotificationErrorType.fromString(networkError.apiError),
                                networkError.message)
                    }
                    dispatcher.dispatch(NotificationActionBuilder.newFetchedNotificationAction(payload))
                })
        add(request)
    }

    /**
     * Send the timestamp of the last notification seen to update the last set of notifications seen
     * on the server.
     *
     * https://developer.wordpress.com/docs/api/1/post/notifications/seen
     */
    fun markNotificationsSeen(timestamp: Long) {
        val url = WPCOMREST.notifications.seen.urlV1_1
        val params = mapOf("time" to timestamp.toString())
        val request = WPComGsonRequest.buildPostRequest(url, params, NotificationSeenApiResponse::class.java,
                { response ->
                    val payload = MarkNotificationSeenResponsePayload(response.success, response.last_seen_time)
                    dispatcher.dispatch(NotificationActionBuilder.newMarkedNotificationsSeenAction(payload))
                },
                { networkError ->
                    val payload = MarkNotificationSeenResponsePayload().apply {
                        error = NotificationError(
                                NotificationErrorType.fromString(networkError.apiError),
                                networkError.message
                        )
                    }
                    dispatcher.dispatch(NotificationActionBuilder.newMarkedNotificationsSeenAction(payload))
                })
        add(request)
    }

    /**
     * Mark a notification as read
     * Decrement the unread count for a notification. Key=note_ID, Value=decrement amount.
     *
     * https://developer.wordpress.com/docs/api/1/post/notifications/read/
     */
    fun markNotificationRead(notifications: List<NotificationModel>) {
        val url = WPCOMREST.notifications.read.urlV1_1
        // "9999" Ensures the "read" count of the notification is decremented enough so the notification
        // is marked read across all devices (just like WPAndroid)
        val params = mutableMapOf<String, String>()
        notifications.iterator().forEach { params["counts[${it.remoteNoteId}]"] = "9999" }
        val request = WPComGsonRequest.buildFormPostRequest(url, params, NotificationReadApiResponse::class.java,
                { response: NotificationReadApiResponse ->
                    val payload = MarkNotificationsReadResponsePayload(notifications, response.success)
                    dispatcher.dispatch(NotificationActionBuilder.newMarkedNotificationsReadAction(payload))
                },
                { networkError ->
                    val payload = MarkNotificationsReadResponsePayload().apply {
                        error = NotificationError(
                                NotificationErrorType.fromString(networkError.apiError),
                                networkError.message)
                    }
                    dispatcher.dispatch(NotificationActionBuilder.newMarkedNotificationsReadAction(payload))
                })
        add(request)
    }

    private fun networkErrorToRegistrationError(wpComError: WPComGsonNetworkError): DeviceRegistrationError {
        val orderErrorType = DeviceRegistrationErrorType.fromString(wpComError.apiError)
        return DeviceRegistrationError(orderErrorType, wpComError.message)
    }
}
