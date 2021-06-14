package org.wordpress.android.util

import java.util.Locale
import javax.inject.Inject

class LocaleProvider @Inject constructor() {
    fun getAppLocale(): Locale {
        return LanguageUtils.getCurrentDeviceLanguage()
    }

    fun getLanguageDisplayString(languageCode: String, displayLocale: Locale): String {
        return LocaleManager.getLanguageString(languageCode, displayLocale)
    }

    fun createSortedLocalizedLanguageDisplayStrings(
        availableLocales: Array<String>,
        targetLocale: Locale
    ): Triple<Array<String>, Array<String>, Array<String>>? {
        return LocaleManager.createSortedLanguageDisplayStrings(availableLocales, targetLocale)
    }
}
