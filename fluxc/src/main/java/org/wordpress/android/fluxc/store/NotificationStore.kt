package org.wordpress.android.fluxc.store

import android.annotation.SuppressLint
import android.content.Context
import com.yarolegovich.wellsql.SelectQuery.ORDER_DESCENDING
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NoteIdSet
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStore @Inject
constructor(
    dispatcher: Dispatcher,
    private val context: Context,
    private val notificationRestClient: NotificationRestClient,
    private val notificationSqlUtils: NotificationSqlUtils
) : Store(dispatcher) {
    companion object {
        const val WPCOM_PUSH_DEVICE_UUID = "NOTIFICATIONS_UUID_PREF_KEY"
        const val WPCOM_PUSH_DEVICE_SERVER_ID = "NOTIFICATIONS_SERVER_ID_PREF_KEY"
    }

    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    class RegisterDevicePayload(
        val gcmToken: String,
        val appKey: NotificationAppKey,
        val site: SiteModel?
    ) : Payload<BaseNetworkError>()

    enum class NotificationAppKey(val value: String) {
        WORDPRESS("org.wordpress.android"),
        WOOCOMMERCE("com.woocommerce.android")
    }

    class RegisterDeviceResponsePayload(
        val deviceId: String? = null
    ) : Payload<DeviceRegistrationError>() {
        constructor(error: DeviceRegistrationError, deviceId: String? = null) : this(deviceId) { this.error = error }
    }

    class UnregisterDeviceResponsePayload() : Payload<DeviceUnregistrationError>() {
        constructor(error: DeviceUnregistrationError) : this() { this.error = error }
    }

    class DeviceRegistrationError(
        val type: DeviceRegistrationErrorType = DeviceRegistrationErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class DeviceRegistrationErrorType {
        INVALID_RESPONSE,
        MISSING_DEVICE_ID,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(DeviceRegistrationErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class DeviceUnregistrationError(
        val type: DeviceUnregistrationErrorType = DeviceUnregistrationErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class DeviceUnregistrationErrorType { GENERIC_ERROR; }

    class FetchNotificationsPayload : Payload<BaseNetworkError>()

    @Suppress("unused")
    class FetchNotificationsResponsePayload(
        val notifs: List<NotificationModel> = emptyList(),
        val lastSeen: Date? = null
    ) : Payload<NotificationError>() {
        constructor(error: NotificationError) : this() { this.error = error }
    }

    class FetchNotificationPayload(
        val remoteNoteId: Long
    ) : Payload<BaseNetworkError>()

    class FetchNotificationResponsePayload(
        val notification: NotificationModel? = null
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() { this.error = error }
    }

    class FetchNotificationHashesResponsePayload(
        val hashesMap: Map<Long, Long> = emptyMap()
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() { this.error = error }
    }

    class MarkNotificationsSeenPayload(
        val lastSeenTime: Long
    ) : Payload<BaseNetworkError>()

    class MarkNotificationSeenResponsePayload(
        val success: Boolean = false,
        val lastSeenTime: Long? = null
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() { this.error = error }
    }

    class MarkNotificationsReadPayload(
        val notifications: List<NotificationModel>
    ) : Payload<BaseNetworkError>()

    class MarkNotificationsReadResponsePayload(
        val notifications: List<NotificationModel>? = null,
        val success: Boolean = false
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() { this.error = error }
    }

    class NotificationError(val type: NotificationErrorType, val message: String = "") : OnChangedError

    enum class NotificationErrorType {
        BAD_REQUEST,
        NOT_FOUND,
        AUTHORIZATION_REQUIRED,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(NotificationErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    // OnChanged events
    @Suppress("unused")
    class OnDeviceRegistered(val deviceId: String?) : OnChanged<DeviceRegistrationError>()

    class OnDeviceUnregistered : OnChanged<DeviceUnregistrationError>()

    class OnNotificationChanged(var rowsAffected: Int) : OnChanged<NotificationError>() {
        var causeOfChange: NotificationAction? = null
        var lastSeenTime: Long? = null
        var success: Boolean = true
        val changedNotificationLocalIds = mutableListOf<Int>()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? NotificationAction ?: return
        when (actionType) {
            // remote actions
            NotificationAction.REGISTER_DEVICE -> registerDevice(action.payload as RegisterDevicePayload)
            NotificationAction.UNREGISTER_DEVICE -> unregisterDevice()
            NotificationAction.FETCH_NOTIFICATIONS -> synchronizeNotifications()
            NotificationAction.FETCH_NOTIFICATION -> fetchNotification(action.payload as FetchNotificationPayload)
            NotificationAction.MARK_NOTIFICATIONS_SEEN ->
                markNotificationSeen(action.payload as MarkNotificationsSeenPayload)
            NotificationAction.MARK_NOTIFICATIONS_READ ->
                markNotificationsRead(action.payload as MarkNotificationsReadPayload)

            // remote responses
            NotificationAction.REGISTERED_DEVICE ->
                handleRegisteredDevice(action.payload as RegisterDeviceResponsePayload)
            NotificationAction.UNREGISTERED_DEVICE ->
                handleUnregisteredDevice(action.payload as UnregisterDeviceResponsePayload)
            NotificationAction.FETCHED_NOTIFICATIONS ->
                handleFetchNotificationsCompleted(action.payload as FetchNotificationsResponsePayload)
            NotificationAction.FETCHED_NOTIFICATION_HASHES ->
                handleFetchNotificationHashesCompleted(action.payload as FetchNotificationHashesResponsePayload)
            NotificationAction.FETCHED_NOTIFICATION ->
                handleFetchNotificationCompleted(action.payload as FetchNotificationResponsePayload)
            NotificationAction.MARKED_NOTIFICATIONS_SEEN ->
                handleMarkedNotificationSeen(action.payload as MarkNotificationSeenResponsePayload)
            NotificationAction.MARKED_NOTIFICATIONS_READ ->
                handleMarkedNotificationsRead(action.payload as MarkNotificationsReadResponsePayload)

            // local actions
            NotificationAction.UPDATE_NOTIFICATION -> updateNotification(action.payload as NotificationModel)
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, NotificationStore::class.java.simpleName + " onRegister")
    }

    /**
     * Fetch all notifications from the database.
     *
     * Filtering. Filtering is done by fetching all records that match the strings in [filterByType] OR
     * [filterBySubtype].
     *
     * @param filterByType Optional. A list of notification type strings to filter by
     * @param filterBySubtype Optional. A list of notification subtype strings to filter by
     */
    @SuppressLint("WrongConstant")
    fun getNotifications(
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): List<NotificationModel> =
            notificationSqlUtils.getNotifications(ORDER_DESCENDING, filterByType, filterBySubtype)

    /**
     * Fetch all notifications for the given site.
     *
     * Filtering. Filtering is done by fetching all records that match the strings in [filterByType] OR
     * [filterBySubtype].
     *
     * @param site The [SiteModel] to fetch notifications for
     * @param filterByType Optional. A list of notification type strings to filter by
     * @param filterBySubtype Optional. A list of notification subtype strings to filter by
     */
    @SuppressLint("WrongConstant")
    fun getNotificationsForSite(
        site: SiteModel,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): List<NotificationModel> =
            notificationSqlUtils.getNotificationsForSite(site, ORDER_DESCENDING, filterByType, filterBySubtype)

    /**
     * Returns true if the given site has unread notifications
     *
     * @param site The [SiteModel] to check notifications for
     * @param filterByType Optional. A list of notification type strings to filter by
     * @param filterBySubtype Optional. A list of notification subtype strings to filter by
     */
    fun hasUnreadNotificationsForSite(
        site: SiteModel,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): Boolean =
            notificationSqlUtils.hasUnreadNotificationsForSite(site, filterByType, filterBySubtype)

    /**
     * Fetch the first notification matching the parameters specified in [NoteIdSet].
     *
     * @param idSet A [NoteIdSet] containing the localSiteId, remoteNoteId, and localNoteId
     */
    @Suppress("unused")
    fun getNotificationByIdSet(idSet: NoteIdSet) = notificationSqlUtils.getNotificationByIdSet(idSet)

    /**
     * Fetch a notification from the database by the remote notification ID.
     */
    @Suppress("unused")
    fun getNotificationByRemoteId(remoteNoteId: Long) =
            notificationSqlUtils.getNotificationByRemoteId(remoteNoteId)

    /**
     * Fetch a notification from the database by it's local notification id.
     */
    fun getNotificationByLocalId(noteId: Int) =
            notificationSqlUtils.getNotificationByIdSet(NoteIdSet(noteId, 0, 0))

    private fun registerDevice(payload: RegisterDevicePayload) {
        val uuid = preferences.getString(WPCOM_PUSH_DEVICE_UUID, null) ?: generateAndStoreUUID()

        with(payload) {
            notificationRestClient.registerDeviceForPushNotifications(gcmToken, appKey, uuid, site)
        }
    }

    private fun unregisterDevice() {
        val deviceId = requireNotNull(preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, ""), {
            "Because we are giving it a default value, preferences.getString shouldn't return null"
        })
        notificationRestClient.unregisterDeviceForPushNotifications(deviceId)
    }

    private fun handleRegisteredDevice(payload: RegisterDeviceResponsePayload) {
        val onDeviceRegistered = OnDeviceRegistered(payload.deviceId)

        with(payload) {
            if (isError || deviceId.isNullOrEmpty()) {
                when (error.type) {
                    DeviceRegistrationErrorType.MISSING_DEVICE_ID ->
                        AppLog.e(T.NOTIFS, "Server response missing device_id - registration skipped!")
                    DeviceRegistrationErrorType.GENERIC_ERROR ->
                        AppLog.e(T.NOTIFS, "Error trying to register device: ${error.type} - ${error.message}")
                    DeviceRegistrationErrorType.INVALID_RESPONSE ->
                        AppLog.e(T.NOTIFS, "Server response missing response object: ${error.type} - ${error.message}")
                }
                onDeviceRegistered.error = payload.error
            } else {
                preferences.edit().putString(WPCOM_PUSH_DEVICE_SERVER_ID, deviceId).apply()
                AppLog.i(T.NOTIFS, "Server response OK. Device ID: $deviceId")
            }
        }

        emitChange(onDeviceRegistered)
    }

    private fun handleUnregisteredDevice(payload: UnregisterDeviceResponsePayload) {
        val onDeviceUnregistered = OnDeviceUnregistered()

        preferences.edit().apply {
            remove(WPCOM_PUSH_DEVICE_SERVER_ID)
            remove(WPCOM_PUSH_DEVICE_UUID)
            apply()
        }

        if (payload.isError) {
            with(payload.error) {
                AppLog.e(T.NOTIFS, "Unregister device action failed: $type - $message")
            }
            onDeviceUnregistered.error = payload.error
        } else {
            AppLog.i(T.NOTIFS, "Unregister device action succeeded")
        }

        emitChange(onDeviceUnregistered)
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            preferences.edit().putString(WPCOM_PUSH_DEVICE_UUID, it).apply()
        }
    }

    /**
     * Determines the optimal route for fetching new notifications and synchronizing the local database.
     *
     * No cached notifications in the database: skip fetching hashes and just fetch full notifications
     * from the remote.
     *
     * Cached notifications exist: fetch only the notification id and note_hash from the remote API
     * and use the smaller, faster results to build a list of notifications to be fetched, and delete
     * notifications in the database that no longer exist.
     */
    private fun synchronizeNotifications() {
        val cachedCount = notificationSqlUtils.getNotificationsCount()

        if (cachedCount > 0) {
            // Fetch only the hashes to determine which notifications need to be fully fetched, and which
            // should be deleted
            notificationRestClient.fetchNotificationHashes()
        } else {
            // Fetch all notifications from the remote
            notificationRestClient.fetchNotifications()
        }
    }

    /**
     * Use the condensed map of newly fetched notification ids and hashes to determine which notifications are missing
     * from cache, require updates, or should be deleted.
     */
    private fun handleFetchNotificationHashesCompleted(payload: FetchNotificationHashesResponsePayload) {
        if (payload.isError) {
            // Unable to synchronize notifications with remote. Emit error event.
            val onNotificationChanged = OnNotificationChanged(0).also {
                it.error = payload.error
                it.causeOfChange = NotificationAction.FETCH_NOTIFICATIONS
            }
            emitChange(onNotificationChanged)
            return
        }

        // Create a mutable copy of freshly fetched notifications map
        val notifsToFetch = payload.hashesMap.toMutableMap()

        // Pull cached notifications from the database and build a map of remoteNoteId to noteHash
        val existingNotifsByRemoteIdMap = notificationSqlUtils
                .getNotifications().associateBy { it.remoteNoteId }.toMap()

        // Scrub the newly fetched list against the cached db records. Remove any entries for records that
        // do not require an update from the remote API
        existingNotifsByRemoteIdMap.entries.forEach { cached ->
            // Compare new note_hash values against cached values. Delete from db if
            // cached notification not present in new list
            notifsToFetch[cached.key]?.let { newNoteHash ->
                if (cached.value.noteHash == newNoteHash) {
                    // Notifications are identical. No update needed, remove from
                    // list of notifs to fetch
                    notifsToFetch.remove(cached.key)
                }
            } ?: notificationSqlUtils.deleteNotificationByRemoteId(cached.key) // Delete notification from the db
        }

        // Fetch new and updated notifications from the remote api
        notificationRestClient.fetchNotifications(notifsToFetch.keys.toList())
    }

    private fun handleFetchNotificationsCompleted(payload: FetchNotificationsResponsePayload) {
        val onNotificationChanged = if (payload.isError) {
            // Notification error
            OnNotificationChanged(0).also { it.error = payload.error }
        } else {
            // Save notifications to the database
            val rowsAffected = payload.notifs.sumBy { notificationSqlUtils.insertOrUpdateNotification(it) }

            OnNotificationChanged(rowsAffected)
        }.apply {
            causeOfChange = NotificationAction.FETCH_NOTIFICATIONS
        }

        emitChange(onNotificationChanged)
    }

    private fun fetchNotification(payload: FetchNotificationPayload) {
        notificationRestClient.fetchNotification(payload.remoteNoteId)
    }

    private fun handleFetchNotificationCompleted(payload: FetchNotificationResponsePayload) {
        val onNotificationChanged = if (payload.isError) {
            OnNotificationChanged(0).also { it.error = payload.error }
        } else {
            // Update the localSiteId and save to the db
            val rows = payload.notification?.let {
                notificationSqlUtils.insertOrUpdateNotification(it)
            } ?: 0
            // Fetch inserted/updated local notification id
            val dbNotification = payload.notification?.let {
                notificationSqlUtils.getNotificationByRemoteId(it.remoteNoteId)
            }
            OnNotificationChanged(rows).apply {
                dbNotification?.let { changedNotificationLocalIds.add(it.noteId) }
            }
        }.apply {
            causeOfChange = NotificationAction.FETCH_NOTIFICATION
        }
        emitChange(onNotificationChanged)
    }

    private fun markNotificationSeen(payload: MarkNotificationsSeenPayload) {
        notificationRestClient.markNotificationsSeen(payload.lastSeenTime)
    }

    private fun handleMarkedNotificationSeen(payload: MarkNotificationSeenResponsePayload) {
        val onNotificationChanged = if (payload.isError) {
            // Notification error
            OnNotificationChanged(0).apply {
                error = payload.error
                success = false
            }
        } else {
            OnNotificationChanged(0).apply {
                success = payload.success
                lastSeenTime = payload.lastSeenTime
            }
        }.apply {
            causeOfChange = NotificationAction.MARK_NOTIFICATIONS_SEEN
        }
        emitChange(onNotificationChanged)
    }

    private fun markNotificationsRead(payload: MarkNotificationsReadPayload) {
        notificationRestClient.markNotificationRead(payload.notifications)
    }

    private fun handleMarkedNotificationsRead(payload: MarkNotificationsReadResponsePayload) {
        // Update the notification in the database
        var rowsAffected = 0
        if (payload.success) {
            payload.notifications?.forEach {
                it.read = true // Just in case it wasn't set by the calling client
                rowsAffected += notificationSqlUtils.insertOrUpdateNotification(it)
            }
        }

        // Create and dispatch result
        val onNotificationChanged = if (payload.isError) {
            OnNotificationChanged(rowsAffected).apply {
                error = payload.error
                success = false
            }
        } else {
            OnNotificationChanged(rowsAffected).apply {
                success = true
            }
        }.apply {
            payload.notifications?.forEach {
                changedNotificationLocalIds.add(it.noteId)
            }
            causeOfChange = NotificationAction.MARK_NOTIFICATIONS_READ
        }
        emitChange(onNotificationChanged)
    }

    private fun updateNotification(payload: NotificationModel) {
        // save notification to the db
        val rowsAffected = notificationSqlUtils.insertOrUpdateNotification(payload)
        val onNotificationChanged = OnNotificationChanged(rowsAffected).apply {
            changedNotificationLocalIds.add(payload.noteId)
            causeOfChange = NotificationAction.UPDATE_NOTIFICATION
        }
        emitChange(onNotificationChanged)
    }
}
