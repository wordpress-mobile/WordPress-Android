package org.wordpress.android.fluxc.store

import android.content.Context
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStore @Inject
constructor(
    dispatcher: Dispatcher,
    private val context: Context,
    private val notificationRestClient: NotificationRestClient
) : Store(dispatcher) {
    companion object {
        const val WPCOM_PUSH_DEVICE_UUID = "NOTIFICATIONS_UUID_PREF_KEY"
        const val WPCOM_PUSH_DEVICE_SERVER_ID = "NOTIFICATIONS_SERVER_ID_PREF_KEY"
    }

    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    class RegisterDevicePayload(
        val gcmToken: String,
        val appKey: String,
        val site: SiteModel?
    ) : Payload<BaseNetworkError>()

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
        MISSING_DEVICE_ID,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = DeviceRegistrationErrorType.values().associateBy(DeviceRegistrationErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class DeviceUnregistrationError(
        val type: DeviceUnregistrationErrorType = DeviceUnregistrationErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class DeviceUnregistrationErrorType { GENERIC_ERROR; }

    // OnChanged events
    class OnDeviceRegistered(val deviceId: String?) : OnChanged<DeviceRegistrationError>()

    class OnDeviceUnregistered : OnChanged<DeviceUnregistrationError>()

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? NotificationAction ?: return
        when (actionType) {
            NotificationAction.REGISTER_DEVICE -> registerDevice(action.payload as RegisterDevicePayload)
            NotificationAction.UNREGISTER_DEVICE -> unregisterDevice()
            NotificationAction.REGISTERED_DEVICE ->
                handleRegisteredDevice(action.payload as RegisterDeviceResponsePayload)
            NotificationAction.UNREGISTERED_DEVICE ->
                handleUnregisteredDevice(action.payload as UnregisterDeviceResponsePayload)
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, NotificationStore::class.java.simpleName + " onRegister")
    }

    private fun registerDevice(payload: RegisterDevicePayload) {
        val uuid = preferences.getString(WPCOM_PUSH_DEVICE_UUID, null) ?: generateAndStoreUUID()

        with(payload) {
            notificationRestClient.registerDeviceForPushNotifications(gcmToken, appKey, uuid, site)
        }
    }

    private fun unregisterDevice() {
        val deviceId = preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, "")
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
}
