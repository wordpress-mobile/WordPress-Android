package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import kotlin.Triple;

/**
 * Helper class for working with localized strings. Ensures updates to the users
 * selected language is properly saved and resources appropriately updated for the
 * android version.
 */
public class LocaleManager {
    /**
     * Pattern to split a language string (to parse the language and region values).
     */
    private static Pattern languageSplitter = Pattern.compile("_");

    /**
     * Previously the app stored the language code in shared preferences, but now
     * we use per-app language preferences
     */
    public static String getLanguage() {
        return PerAppLocaleManager.Companion.getLanguageCode();
    }

    /**
     * Compare the language for the current context with another language.
     *
     * @param language The language to compare
     * @return True if the languages are the same, else false
     */
    public static boolean isSameLanguage(@NonNull String language) {
        Locale newLocale = languageLocale(language);
        return Locale.getDefault().toString().equals(newLocale.toString());
    }

    /**
     * Convert the device language code (codes defined by ISO 639-1) to a Language ID.
     * Language IDs, used only by WordPress, are integer values that map to a language code.
     * http://bit.ly/2H7gksN
     **/
    public static @NonNull String getLanguageWordPressId(Context context) {
        final String deviceLanguageCode = LanguageUtils.getPatchedCurrentDeviceLanguage(context);

        Map<String, String> languageCodeToID = LocaleManager.generateLanguageMap(context);
        String langID = null;
        if (languageCodeToID.containsKey(deviceLanguageCode)) {
            langID = languageCodeToID.get(deviceLanguageCode);
        } else {
            int pos = deviceLanguageCode.indexOf("_");
            if (pos > -1) {
                String newLang = deviceLanguageCode.substring(0, pos);
                if (languageCodeToID.containsKey(newLang)) {
                    langID = languageCodeToID.get(newLang);
                }
            }
        }

        if (langID == null) {
            // fallback to device language code if there is no match
            langID = deviceLanguageCode;
        }
        return langID;
    }

    /**
     * Method gets around a bug in the java.util.Formatter for API 7.x as detailed here
     * [https://bugs.openjdk.java.net/browse/JDK-8167567]. Any strings that contain
     * locale-specific grouping separators should use:
     * <code>
     * String.format(LocaleManager.getSafeLocale(context), baseString, val)
     * </code>
     * <p>
     * An example of a string that contains locale-specific grouping separators:
     * <code>
     * <string name="test">%,d likes</string>
     * </code>
     */
    public static Locale getSafeLocale(@Nullable Context context) {
        Locale baseLocale;
        if (context == null) {
            baseLocale = Locale.getDefault();
        } else {
            Configuration config = context.getResources().getConfiguration();
            baseLocale = config.getLocales().get(0);
        }

        return languageLocale(baseLocale.getLanguage());
    }

    /**
     * Gets a locale for the given language code.
     *
     * @param languageCode The language code (example "en" or "es-US"). If null or empty will return
     *                     the current default locale.
     */
    public static Locale languageLocale(@Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            return Locale.getDefault();
        }
        // Attempt to parse language and region codes.
        String[] opts = languageSplitter.split(languageCode, 0);
        if (opts.length > 1) {
            return new Locale(opts[0], opts[1]);
        } else {
            return new Locale(opts[0]);
        }
    }

    /**
     * Creates a map from language codes to WordPress language IDs.
     */
    public static Map<String, String> generateLanguageMap(Context context) {
        String[] languageIds = context.getResources().getStringArray(R.array.lang_ids);
        String[] languageCodes = context.getResources().getStringArray(R.array.language_codes);

        Map<String, String> languageMap = new HashMap<>();
        for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
            languageMap.put(languageCodes[i], languageIds[i]);
        }

        return languageMap;
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    @Nullable
    public static Triple<String[], String[], String[]> createSortedLanguageDisplayStrings(CharSequence[] languageCodes,
                                                                                Locale locale) {
        if (languageCodes == null || languageCodes.length < 1) {
            return null;
        }

        ArrayList<String> entryStrings = new ArrayList<>(languageCodes.length);
        for (int i = 0; i < languageCodes.length; ++i) {
            // "__" is used to sort the language code with the display string so both arrays are sorted at the same time
            entryStrings.add(i, StringUtils.capitalize(
                    getLanguageString(languageCodes[i].toString(), locale)) + "__" + languageCodes[i]);
        }

        Collections.sort(entryStrings, Collator.getInstance(locale));

        String[] sortedEntries = new String[languageCodes.length];
        String[] sortedValues = new String[languageCodes.length];
        String[] detailStrings = new String[languageCodes.length];

        for (int i = 0; i < entryStrings.size(); ++i) {
            // now, we can split the sorted array to extract the display string and the language code
            String[] split = entryStrings.get(i).split("__");
            sortedEntries[i] = split[0];
            sortedValues[i] = split[1];
            detailStrings[i] =
                    StringUtils.capitalize(getLanguageString(sortedValues[i], languageLocale(sortedValues[i])));
        }

        return new Triple<>(sortedEntries, sortedValues, detailStrings);
    }

    /**
     * Return a non-null display string for a given language code.
     */
    public static String getLanguageString(String languageCode, Locale displayLocale) {
        if (languageCode == null || languageCode.length() < 2 || languageCode.length() > 6) {
            return "";
        }

        Locale languageLocale = languageLocale(languageCode);
        String displayLanguage = StringUtils.capitalize(languageLocale.getDisplayLanguage(displayLocale));
        String displayCountry = languageLocale.getDisplayCountry(displayLocale);

        if (!TextUtils.isEmpty(displayCountry)) {
            return displayLanguage + " (" + displayCountry + ")";
        }
        return displayLanguage;
    }
}
