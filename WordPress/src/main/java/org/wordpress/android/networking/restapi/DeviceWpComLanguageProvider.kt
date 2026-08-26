package org.wordpress.android.networking.restapi

import org.wordpress.android.util.LocaleManagerWrapper
import rs.wordpress.api.kotlin.fromLocale
import uniffi.wp_api.WpComLanguage
import uniffi.wp_api.WpComLanguageProvider
import javax.inject.Inject

/**
 * Reports the device language that WordPress.com should localize its responses to.
 *
 * The locale is read on every request rather than captured once, so a language
 * change part-way through a session is picked up without rebuilding the client.
 * Returns `null` when the device language has no WordPress.com equivalent, which
 * sends no locale and leaves the choice to the server.
 */
class DeviceWpComLanguageProvider @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper
) : WpComLanguageProvider {
    override fun currentLanguage(): WpComLanguage? =
        WpComLanguage.fromLocale(localeManagerWrapper.getLocale())
}
