package org.wordpress.android.util

import android.content.Context
import android.text.TextUtils
import org.wordpress.android.R
import java.text.Collator
import java.util.Locale
import java.util.regex.Pattern

/**
 * Helper class for working with localized strings
 */
object LocaleManager {
    /**
     * Pattern to split a language string (to parse the language and region values).
     */
    private val LANGUAGE_SPLITTER = Pattern.compile("_")

    private const val MIN_LANGUAGE_CODE_LENGTH = 2
    private const val MAX_LANGUAGE_CODE_LENGTH = 6

    /**
     * Convert the device language code (codes defined by ISO 639-1) to a Language ID.
     * Language IDs, used only by WordPress, are integer values that map to a language code.
     * http://bit.ly/2H7gksN
     */
    fun getLanguageWordPressId(context: Context): String {
        val deviceLanguageCode = LanguageUtils.getPatchedCurrentDeviceLanguage(context)

        val languageCodeToID = generateLanguageMap(context)
        var langID: String? = null
        if (languageCodeToID.containsKey(deviceLanguageCode)) {
            langID = languageCodeToID[deviceLanguageCode]
        } else {
            val pos = deviceLanguageCode.indexOf("_")
            if (pos > -1) {
                val newLang = deviceLanguageCode.substring(0, pos)
                if (languageCodeToID.containsKey(newLang)) {
                    langID = languageCodeToID[newLang]
                }
            }
        }

        return langID ?: deviceLanguageCode
    }

    /**
     * Gets a locale for the given language code.
     *
     * @param languageCode The language code (example "en" or "es-US"). If null or empty will return
     * the current default locale.
     */
    @JvmStatic
    fun languageLocale(languageCode: String?): Locale {
        if (languageCode == null || TextUtils.isEmpty(languageCode)) {
            return Locale.getDefault()
        }
        // Attempt to parse language and region codes.
        val opts = LANGUAGE_SPLITTER.split(languageCode, 0)
        return if (opts.size > 1) {
            Locale(opts[0], opts[1])
        } else {
            Locale(opts[0])
        }
    }

    /**
     * Creates a map from language codes to WordPress language IDs.
     */
    @JvmStatic
    fun generateLanguageMap(context: Context): Map<String, String> {
        val languageIds = context.resources.getStringArray(R.array.lang_ids)
        val languageCodes = context.resources.getStringArray(R.array.language_codes)

        val languageMap = hashMapOf<String, String>()
        var i = 0
        while (i < languageIds.size && i < languageCodes.size) {
            languageMap[languageCodes[i]] = languageIds[i]
            ++i
        }

        return languageMap
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    @JvmStatic
    fun createSortedLanguageDisplayStrings(
        languageCodes: Array<CharSequence>?,
        locale: Locale
    ): Triple<Array<String>, Array<String>, Array<String>>? {
        if (languageCodes.isNullOrEmpty()) {
            return null
        }

        val entryStrings = ArrayList<String>(languageCodes.size)
        for (i in languageCodes.indices) {
            // "__" is used to sort the language code with the display string so both arrays are sorted at the same time
            entryStrings.add(
                i, StringUtils.capitalize(
                    getLanguageString(languageCodes[i].toString(), locale)
                ) + "__" + languageCodes[i]
            )
        }

        entryStrings.sortWith(Collator.getInstance(locale))

        val sortedEntries = Array(languageCodes.size) { "" }
        val sortedValues = Array(languageCodes.size) { "" }
        val detailStrings = Array(languageCodes.size) { "" }

        for (i in entryStrings.indices) {
            // now, we can split the sorted array to extract the display string and the language code
            val split = entryStrings[i].split("__")
            sortedEntries[i] = split[0]
            sortedValues[i] = split[1]
            detailStrings[i] =
                StringUtils.capitalize(
                    getLanguageString(
                        sortedValues[i], languageLocale(
                            sortedValues[i]
                        )
                    )
                )
        }

        return Triple(sortedEntries, sortedValues, detailStrings)
    }


    /**
     * Return a non-null display string for a given language code.
     */
    @JvmStatic
    fun getLanguageString(languageCode: String?, displayLocale: Locale): String {
        if (languageCode == null
            || languageCode.length < MIN_LANGUAGE_CODE_LENGTH
            || languageCode.length > MAX_LANGUAGE_CODE_LENGTH
        ) {
            return ""
        }

        val languageLocale = languageLocale(languageCode)
        val displayLanguage =
            StringUtils.capitalize(languageLocale.getDisplayLanguage(displayLocale))
        val displayCountry = languageLocale.getDisplayCountry(displayLocale)

        return if (!TextUtils.isEmpty(displayCountry)) {
            "$displayLanguage ($displayCountry)"
        } else {
            displayLanguage
        }
    }
}
