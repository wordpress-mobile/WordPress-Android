package org.wordpress.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Helper class for working with localized strings. Ensures updates to the users
 * selected language is properly saved and resources appropriately updated for the
 * android version.
 */
public class LocaleManager {
    /**
     * Key used for saving the language selection to shared preferences.
     */
    private static final String LANGUAGE_KEY = "language-pref";

    /**
     * Pattern to split a language string (to parse the language and region values).
     */
    private static Pattern languageSplitter = Pattern.compile("_");

    /**
     * Activate the locale associated with the provided context.
     *
     * @param context The current context.
     */
    public static Context setLocale(Context context) {
        return updateResources(context, getLanguage(context));
    }

    /**
     * Apply locale to the provided configuration.
     *
     * @param context current context used to access Shared Preferences.
     * @param configuration configuration that the locale should be applied to.
     */
    public static Configuration updatedConfigLocale(Context context, Configuration configuration) {
        Locale locale = languageLocale(getLanguage(context));
        Locale.setDefault(locale);

        // NOTE: Earlier versions of Android require both of these to be set, otherwise
        // RTL may not be implemented properly.
        configuration.setLocale(locale);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            configuration.locale = locale;
        }

        return configuration;
    }

    /**
     * Change the active locale to the language provided. Save the updated language
     * settings to sharedPreferences.
     *
     * @param context  The current context
     * @param language The 2-letter language code (example "en") to switch to
     */
    public static void setNewLocale(Context context, String language) {
        if (isSameLanguage(language)) {
            return;
        }
        saveLanguageToPref(context, language);
        updateResources(context, language);
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
     * If the user has selected a language other than the device default, return that
     * language code, else just return the device default language code.
     *
     * @return The 2-letter language code (example "en")
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(LANGUAGE_KEY, LanguageUtils.getCurrentDeviceLanguageCode());
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
     * Save the updated language to SharedPreferences.
     * Use commit() instead of apply() to ensure the language preference is saved instantly
     * as the app may be restarted immediately.
     *
     * @param context  The current context
     * @param language The 2-letter language code (example "en")
     */
    @SuppressLint("ApplySharedPref")
    private static void saveLanguageToPref(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(LANGUAGE_KEY, language).commit();
    }

    /**
     * Update resources for the current session.
     *
     * @param context  The current active context
     * @param language The 2-letter language code (example "en")
     * @return The modified context containing the updated localized resources
     */
    private static Context updateResources(Context context, String language) {
        Locale locale = languageLocale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        // NOTE: Earlier versions of Android require both of these to be set, otherwise
        // RTL may not be implemented properly.
        config.setLocale(locale);
        context = context.createConfigurationContext(config);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }

        return context;
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
            baseLocale = Build.VERSION.SDK_INT >= 24 ? config.getLocales().get(0) : config.locale;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            return languageLocale(baseLocale.getLanguage());
        } else {
            return baseLocale;
        }
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
        String[] languageIds = context.getResources().getStringArray(org.wordpress.android.R.array.lang_ids);
        String[] languageCodes = context.getResources().getStringArray(org.wordpress.android.R.array.language_codes);

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
    public static Pair<String[], String[]> createSortedLanguageDisplayStrings(CharSequence[] languageCodes,
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

        for (int i = 0; i < entryStrings.size(); ++i) {
            // now, we can split the sorted array to extract the display string and the language code
            String[] split = entryStrings.get(i).split("__");
            sortedEntries[i] = split[0];
            sortedValues[i] = split[1];
        }

        return new Pair<>(sortedEntries, sortedValues);
    }

    /**
     * Generates detail display strings in the currently selected locale. Used as detail text
     * in language preference dialog.
     */
    @Nullable
    public static String[] createLanguageDetailDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) {
            return null;
        }

        String[] detailStrings = new String[languageCodes.length];
        for (int i = 0; i < languageCodes.length; ++i) {
            detailStrings[i] = StringUtils.capitalize(getLanguageString(
                    languageCodes[i].toString(), languageLocale(languageCodes[i].toString())));
        }

        return detailStrings;
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
