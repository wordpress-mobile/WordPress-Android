package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStore @Inject
constructor(dispatcher: Dispatcher, private val notificationRestClient: NotificationRestClient) : Store(dispatcher) {
    class RegisterDevicePayload(
        val params: Map<String, String>
    ) : Payload<BaseNetworkError>()

    class RegisterDeviceResponsePayload(
        val deviceId: String? = null
    ) : Payload<DeviceRegistrationError>() {
        constructor(error: DeviceRegistrationError, deviceId: String? = null) : this(deviceId) { this.error = error }
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

    // OnChanged events
    class OnDeviceRegistered(val deviceId: String?) : OnChanged<DeviceRegistrationError>()

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? NotificationAction ?: return
        when (actionType) {
            NotificationAction.REGISTER_DEVICE -> registerDevice(action.payload as RegisterDevicePayload)
            NotificationAction.REGISTERED_DEVICE ->
                handleRegisteredDevice(action.payload as RegisterDeviceResponsePayload)
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, NotificationStore::class.java.simpleName + " onRegister")
    }

    private fun registerDevice(payload: RegisterDevicePayload) {
        notificationRestClient.registerDeviceForPushNotifications(payload.params)
    }

    private fun handleRegisteredDevice(payload: RegisterDeviceResponsePayload) {
        val onDeviceRegistered = OnDeviceRegistered(payload.deviceId)

        if (payload.isError) {
            onDeviceRegistered.error = payload.error
        }

        emitChange(onDeviceRegistered)
    }
}
