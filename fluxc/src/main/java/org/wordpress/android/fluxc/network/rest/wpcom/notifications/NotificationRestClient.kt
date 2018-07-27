package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationError
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDeviceResponsePayload
import javax.inject.Singleton

@Singleton
class NotificationRestClient constructor(
    appContext: Context?,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    fun registerDeviceForPushNotifications(params: Map<String, String>) {
        val url = WPCOMREST.devices.new_.urlV1
        val request = WPComGsonRequest.buildPostRequest(
                url, params, RegisterDeviceRestResponse::class.java,
                { response ->
                    response.id?.takeIf { it.isNotEmpty() }?.let {
                        val payload = RegisterDeviceResponsePayload(it)
                        dispatcher.dispatch(NotificationActionBuilder.newRegisteredDeviceAction(payload))
                    } ?: run {
                        val registrationError = DeviceRegistrationError(DeviceRegistrationErrorType.MISSING_DEVICE_ID)
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

    private fun networkErrorToRegistrationError(wpComError: WPComGsonNetworkError): DeviceRegistrationError {
        val orderErrorType = DeviceRegistrationErrorType.fromString(wpComError.apiError)
        return DeviceRegistrationError(orderErrorType, wpComError.message)
    }
}
