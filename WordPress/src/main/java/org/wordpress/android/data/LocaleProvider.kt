package org.wordpress.android.data

import org.wordpress.android.util.LanguageUtils
import org.wordpress.android.util.LocaleManager
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LocaleProvider @Inject constructor() : ReadOnlyProperty<Any, Locale> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = _appLocale

    private val _appLocale by lazy { getAppLocale() }

    fun getAppLocale(): Locale = LanguageUtils.getCurrentDeviceLanguage()

    fun getAppLanguageDisplayString(): String = LocaleManager.getLanguageString(_appLocale.toString(), _appLocale)

    fun createSortedLocalizedLanguageDisplayStrings(
        availableLocales: Array<String>,
        targetLocale: Locale
    ) = LocaleManager.createSortedLanguageDisplayStrings(availableLocales, targetLocale)
}
