package org.wordpress.android.fluxc.store

import javax.inject.Inject
import org.wordpress.android.fluxc.utils.PreferenceUtils

class GetDeviceRegistrationStatus @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper
) {
    operator fun invoke(): Status {
        val deviceId = prefsWrapper.getFluxCPreferences().getString(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        return if (deviceId.isNullOrEmpty()) {
            Status.UNREGISTERED
        } else {
            Status.REGISTERED
        }
    }

    enum class Status {
        REGISTERED, UNREGISTERED
    }
}
