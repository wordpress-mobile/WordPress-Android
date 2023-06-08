package org.wordpress.android.data

import org.wordpress.android.util.LanguageUtils
import org.wordpress.android.util.LocaleManager
import java.util.Locale
import javax.inject.Inject

class LocaleProvider @Inject constructor() : Provider<Locale> {
    private val appLocale by lazy { getAppLocale() }

    override fun provide() = appLocale

    fun getAppLocale(): Locale = LanguageUtils.getCurrentDeviceLanguage()

    fun getAppLanguageDisplayString(): String = LocaleManager.getLanguageString(appLocale.toString(), appLocale)

    fun createSortedLocalizedLanguageDisplayStrings(
        availableLocales: Array<String>,
        targetLocale: Locale
    ) = LocaleManager.createSortedLanguageDisplayStrings(availableLocales, targetLocale)
}
