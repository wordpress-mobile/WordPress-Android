package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class InvalidateDeviceRegistration @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper
) {
    operator fun invoke() {
        prefsWrapper.getFluxCPreferences()
            .edit()
            .remove(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID)
            .apply()
    }
}
