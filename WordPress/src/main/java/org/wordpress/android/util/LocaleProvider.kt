package org.wordpress.android.util

import java.util.Locale
import javax.inject.Inject

class LocaleProvider @Inject constructor() {
    fun getAppLocale(): Locale {
        return LanguageUtils.getCurrentDeviceLanguage()
    }

    fun getAppLanguageDisplayString(): String {
        return LocaleManager.getLanguageString(getAppLocale().toString(), getAppLocale())
    }

    fun createSortedLocalizedLanguageDisplayStrings(
        availableLocales: Array<String>,
        targetLocale: Locale
    ): Triple<Array<String>, Array<String>, Array<String>>? {
        return LocaleManager.createSortedLanguageDisplayStrings(availableLocales, targetLocale)
    }
}
