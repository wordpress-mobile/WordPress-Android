package org.wordpress.android.networking.restapi

import org.wordpress.android.util.PerAppLocaleManager
import rs.wordpress.api.kotlin.fromLocale
import uniffi.wp_api.WpComLanguage
import uniffi.wp_api.WpComLanguageProvider
import javax.inject.Inject

/**
 * Reports the language that WordPress.com should localize its responses to.
 *
 * Reads the app's own language preference, falling back to the device language when
 * the user hasn't chosen one. The locale is read on every request, so a language
 * change part-way through a session takes effect without rebuilding the client.
 * Returns `null` when the language has no WordPress.com equivalent, which sends no
 * locale and leaves the choice to the server.
 */
class AppWpComLanguageProvider @Inject constructor(
    private val perAppLocaleManager: PerAppLocaleManager
) : WpComLanguageProvider {
    override fun currentLanguage(): WpComLanguage? =
        WpComLanguage.fromLocale(perAppLocaleManager.getCurrentLocale())
}
