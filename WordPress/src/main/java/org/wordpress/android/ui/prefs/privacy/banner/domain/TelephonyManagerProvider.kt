package org.wordpress.android.ui.prefs.privacy.banner.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.util.extensions.telephonyManager
import javax.inject.Inject

class TelephonyManagerProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    /**
     * Gets the country code string via telephony if available or empty string if not.
     */
    fun getCountryCode(): String = appContext.telephonyManager?.networkCountryIso.orEmpty()
}
