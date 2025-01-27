package org.wordpress.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.util.analytics.AnalyticsUtils
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

/**
 * Helper class to manage AndroidX per-app language preferences
 * https://developer.android.com/guide/topics/resources/app-languages
 */
class PerAppLocaleManager @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val appLogWrapper: AppLogWrapper,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
) {
    private fun getCurrentLocale(): Locale {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty || locales == LocaleListCompat.getEmptyLocaleList()) {
            Locale.getDefault()
        } else {
            locales[0] ?: Locale.getDefault()
        }
    }

    fun getCurrentLocaleDisplayName(): String = getCurrentLocale().displayName

    fun getCurrentLocaleLanguageCode(): String = getCurrentLocale().language

    private fun setCurrentLocaleByLanguageCode(languageCode: String) {
        // We shouldn't have to replace "_" with "-" but this is in order to work with our existing language picker
        // on pre-Android 13 devices
        val appLocale = LocaleListCompat.forLanguageTags(languageCode.replace("_", "-"))
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Previously the app locale was stored in SharedPreferences, so here we migrate to AndroidX per-app language prefs.
     * This was added in our Jan 2025 release and can be removed after a few subsequent releases.
     */
    fun performMigrationIfNecessary() {
        val previousLanguage = appPrefsWrapper.getPrefString(OLD_LOCALE_PREF_KEY_STRING, "")
        if (previousLanguage?.isNotEmpty() == true) {
            appLogWrapper.d(
                AppLog.T.SETTINGS,
                "PerAppLocaleManager: performing migration to AndroidX per-app language prefs"
            )
            setCurrentLocaleByLanguageCode(previousLanguage)
            appPrefsWrapper.removePref(OLD_LOCALE_PREF_KEY_STRING)
        }
    }

    /**
     * Open the app settings dialog so the user can change the app language.
     * Note that the per-app language setting is only available in API 33+
     * and it's up to the caller to check the version.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun openAppLanguageSettings(context: Context) {
        Intent().also { intent ->
            intent.setAction(Settings.ACTION_APP_LOCALE_SETTINGS)
            intent.setData(Uri.parse("package:" + context.packageName))
            context.startActivity(intent)
        }
    }

    @Suppress("ForbiddenComment")
    /**
     * Called when the device language is changed from our in-app language picker
     * TODO: Detect when language changed from system app settings dialog
     */
    fun onLanguageChanged(languageCode: String) {
        if (languageCode.isEmpty()) {
            return
        }

        // Only update if the language is different
        if (languageCode != getCurrentLocaleLanguageCode()) {
            setCurrentLocaleByLanguageCode(languageCode)
        }

        // Track language change on Analytics because we have both the device language and app selected language
        // data in Tracks metadata.
        val properties: MutableMap<String, Any?> = HashMap()
        properties["app_locale"] = languageCode
        AnalyticsTracker.track(AnalyticsTracker.Stat.ACCOUNT_SETTINGS_LANGUAGE_CHANGED, properties)

        // Language is now part of metadata, so we need to refresh them
        AnalyticsUtils.refreshMetadata(accountStore, siteStore)

        // update Reader tags as they need be localized
        ReaderUpdateServiceStarter.startService(getContext(), EnumSet.of(UpdateTask.TAGS))
    }

    /**
     * This routine can be helpful during development to reset the app locale
     */
    @Suppress("unused")
    fun resetApplicationLocale() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    companion object {
        // Key previously used for saving the language selection to shared preferences
        private const val OLD_LOCALE_PREF_KEY_STRING: String = "language-pref"
    }
}
