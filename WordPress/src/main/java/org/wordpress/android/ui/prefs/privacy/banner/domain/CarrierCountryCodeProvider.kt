package org.wordpress.android.ui.prefs.privacy.banner.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.util.extensions.telephonyManager
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Gets the country code string via telephony if available or null
 */
class CarrierCountryCodeProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ReadOnlyProperty<Any, String?> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = appContext.telephonyManager?.networkCountryIso
}
