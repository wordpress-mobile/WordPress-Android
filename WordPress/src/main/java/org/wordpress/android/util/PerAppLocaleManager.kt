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
import org.wordpress.android.ui.prefs.AppPrefs
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
        return if (isApplicationLocaleEmpty()) {
            Locale.getDefault()
        } else {
            getApplicationLocaleList()[0] ?: Locale.getDefault()
        }
    }

    fun getCurrentLocaleDisplayName(): String = getCurrentLocale().displayName

    private fun getCurrentLocaleLanguageCode(): String = getCurrentLocale().language

    /**
     * Important: this should only be called after Activity.onCreate()
     * https://developer.android.com/reference/androidx/appcompat/app/AppCompatDelegate#getApplicationLocales()
     */
    private fun getApplicationLocaleList() = AppCompatDelegate.getApplicationLocales()

    private fun isApplicationLocaleEmpty(): Boolean {
        val locales = getApplicationLocaleList()
        return (locales.isEmpty || locales == LocaleListCompat.getEmptyLocaleList())
    }

    /**
     * This can be helpful during development to reset the app locale back to the default
     */
    @Suppress("unused")
    fun resetApplicationLocale() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    private fun setCurrentLocaleByLanguageCode(languageCode: String) {
        // We shouldn't have to replace "_" with "-" but this is in order to work with our existing language picker
        // on pre-Android 13 devices
        val appLocale = LocaleListCompat.forLanguageTags(languageCode.replace("_", "-"))
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Check the old language pref to see if the user previously changed the language, and if so make sure
     * that the per-app language is set to the same language.
     */
    fun performMigrationIfNecessary() {
        if (isApplicationLocaleEmpty()) {
            val previousLanguage = appPrefsWrapper.getPrefString(OLD_LANGUAGE_PREF_KEY, "")
            if (previousLanguage?.isNotEmpty() == true) {
                appLogWrapper.d(
                    AppLog.T.SETTINGS,
                    "PerAppLocaleManager: performing migration to AndroidX per-app language prefs"
                )
                setCurrentLocaleByLanguageCode(previousLanguage)
                appPrefsWrapper.removePref(OLD_LANGUAGE_PREF_KEY)
            } else {
                appLogWrapper.d(
                    AppLog.T.SETTINGS,
                    "PerAppLocaleManager: setting default locale"
                )
                setCurrentLocaleByLanguageCode(Locale.getDefault().language)
            }
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

    /**
     * Called when the device language is changed from our in-app language picker
     * TODO Detect when language changed from app settings dialog
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


        // When language changed we need to reset the shared prefs reader tag since if we have it stored
        // it's fields can be in a different language and we can get odd behaviors since we will generally fail
        // to get the ReaderTag.equals method recognize the equality based on the ReaderTag.getLabel method.
        AppPrefs.setReaderTag(null)

        // update Reader tags as they need be localized
        ReaderUpdateServiceStarter.startService(getContext(), EnumSet.of(UpdateTask.TAGS))
    }

    companion object {
         // Key previously used for saving the language selection to shared preferences.
        private const val OLD_LANGUAGE_PREF_KEY: String = "language-pref"
    }
}
