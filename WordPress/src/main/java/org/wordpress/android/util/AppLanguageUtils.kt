package org.wordpress.android.util

import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.ContextProvider
import java.util.Locale
import javax.inject.Inject

class AppLanguageUtils @Inject constructor(
    private val contextProvider: ContextProvider,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
) {
    fun changeAppLanguage(languageCode: String) {
        if (languageCode.isEmpty()) return
        if (LocaleManager.isSameLanguage(languageCode)) return

        LocaleManager.setNewLocale(WordPress.getContext(), languageCode)
        WordPress.updateContextLocale()
        contextProvider.refreshContext()

        trackLanguageChange()
    }

    private fun trackLanguageChange() {
        // Track language change on Analytics because we have both the device language and app selected language
        // data in Tracks metadata.
        val properties: MutableMap<String, Any?> = HashMap()
        properties["app_locale"] = Locale.getDefault()
        AnalyticsTracker.track(Stat.ACCOUNT_SETTINGS_LANGUAGE_CHANGED, properties)

        // Language is now part of metadata, so we need to refresh them
        AnalyticsUtils.refreshMetadata(accountStore, siteStore)
    }
}
