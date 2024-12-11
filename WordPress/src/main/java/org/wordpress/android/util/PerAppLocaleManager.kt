package org.wordpress.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import java.util.Locale
import javax.inject.Inject
import java.util.EnumSet

/**
 * Helper class to manage AndroidX per-app language preferences
 * https://developer.android.com/guide/topics/resources/app-languages
 */
class PerAppLocaleManager @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val appLogWrapper: AppLogWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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

    fun getCurrentLocaleLanguageCode(): String = getCurrentLocale().language

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
     * We want to make sure the language pref for the in-app locale (old implementation) is set
     * to the same locale as the AndroidX per-app locale. This way LocaleManager.getLanguage -
     * which is used throughout the app - returns the correct language code. We can remove
     * this once the per-app language pref is no longer experimental.
     */
    fun checkAndUpdateOldLanguagePrefKey() {
        val prefKey = LocaleManager.getLocalePrefKeyString()
        val inAppLanguage = appPrefsWrapper.getPrefString(prefKey, "")
        val perAppLanguage = getCurrentLocale().language
        if (perAppLanguage.isNotEmpty() && inAppLanguage.equals(perAppLanguage).not()) {
            appPrefsWrapper.setPrefString(prefKey, perAppLanguage)
            appLogWrapper.d(
                AppLog.T.SETTINGS,
                "PerAppLocaleManager: changed inAppLanguage from $inAppLanguage to $perAppLanguage"
            )
        }
    }

    fun resetApplicationLocale() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    private fun setCurrentLocaleByLanguageCode(languageCode: String) {
        // We shouldn't have to replace "_" with "-" but this is in order to work with our existing language picker
        // on pre-Android 13 devices
        val appLocale = LocaleListCompat.forLanguageTags(languageCode.replace("_", "-"))
        AppCompatDelegate.setApplicationLocales(appLocale)
        checkAndUpdateOldLanguagePrefKey()
    }

    /**
     * Previously the app locale was stored in SharedPreferences, so here we migrate to AndroidX per-app language prefs
     */
    fun performMigrationIfNecessary() {
        if (isApplicationLocaleEmpty()) {
            val prefKey = LocaleManager.getLocalePrefKeyString()
            val previousLanguage = appPrefsWrapper.getPrefString(prefKey, "")
            if (previousLanguage?.isNotEmpty() == true) {
                appLogWrapper.d(
                    AppLog.T.SETTINGS,
                    "PerAppLocaleManager: performing migration to AndroidX per-app language prefs"
                )
                setCurrentLocaleByLanguageCode(previousLanguage)
            } else {
                appLogWrapper.d(
                    AppLog.T.SETTINGS,
                    "PerAppLocaleManager: setting default locale"
                )
                setCurrentLocaleByLanguageCode(Locale.getDefault().language)
            }
        } else {
            checkAndUpdateOldLanguagePrefKey()
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
     * Called when the user chooses a new language from LocalePickerBottomSheet
     * TODO: detect when language is changed from app settings dialog
     */
    fun onLanguageChanged(context: Context, languageCode: String) {
        setCurrentLocaleByLanguageCode(languageCode)

        // Track language change on Analytics because we have both the device language
        // and app selected language data in Tracks metadata.
        val properties: MutableMap<String, Any?> = HashMap()
        properties["app_locale"] = languageCode
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.ACCOUNT_SETTINGS_LANGUAGE_CHANGED,
            properties
        )

        // Language is now part of metadata, so we need to refresh them
        AnalyticsUtils.refreshMetadata(accountStore, siteStore)

        // update Reader tags as they need be localized
        ReaderUpdateServiceStarter.startService(
            context,
            EnumSet.of(ReaderUpdateLogic.UpdateTask.TAGS)
        )
    }
}
