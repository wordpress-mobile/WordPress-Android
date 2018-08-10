package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import android.content.Context
import android.os.Build
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationError
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.DeviceUnregistrationError
import org.wordpress.android.fluxc.store.NotificationStore.DeviceUnregistrationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDeviceResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.UnregisterDeviceResponsePayload
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.PackageUtils
import javax.inject.Singleton

@Singleton
class NotificationRestClient constructor(
    private val appContext: Context?,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    fun registerDeviceForPushNotifications(gcmToken: String, appKey: String, uuid: String, site: SiteModel? = null) {
        val deviceName = DeviceUtils.getInstance().getDeviceName(appContext)
        val params = mapOf(
                "device_token" to gcmToken,
                "device_family" to "android",
                "app_secret_key" to appKey,
                "device_name" to deviceName,
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "app_version" to PackageUtils.getVersionName(appContext),
                "version_code" to PackageUtils.getVersionCode(appContext).toString(),
                "os_version" to Build.VERSION.RELEASE,
                "device_uuid" to uuid,
                "selected_blog_id" to site?.siteId.toString().takeIf { site != null }
        )

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

    private fun networkErrorToRegistrationError(wpComError: WPComGsonNetworkError): DeviceRegistrationError {
        val orderErrorType = DeviceRegistrationErrorType.fromString(wpComError.apiError)
        return DeviceRegistrationError(orderErrorType, wpComError.message)
    }
}
