package org.wordpress.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import org.wordpress.android.WordPress;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Helper class for working with localized strings. Ensures updates to the users
 * selected language is properly saved and resources appropriately updated for the
 * android version.
 */
public abstract class LocaleManager {
    private static final String LANGUAGE_KEY = "language-pref";

    /**
     * Activate the locale associated with the provided context.
     * @param context The current context.
     */
    public static Context setLocale(Context context) {
        return updateResources(context, getLanguage(context));
    }

    /**
     * Change the active locale to the language provided. Save the updated language
     * settings to sharedPreferences.
     * @param context The current context
     * @param language The language to change to
     */
    public static Context setNewLocale(Context context, String language) {
        Locale newLocale = WPPrefUtils.languageLocale(language);

        if (Locale.getDefault().toString().equals(newLocale.toString())) {
            removePersistedLanguage(context);
        } else {
            saveLanguageToPref(context, language);
        }
        return updateResources(context, language);
    }

    /**
     * Compare the language for the current context with another language.
     * @param context The current context
     * @param language The language to compare
     * @return True if the languages are the same, else false
     */
    public static boolean isDifferentLanguage(Context context, String language) {
        Locale currentLocale = LanguageUtils.getCurrentDeviceLanguage(WordPress.getContext());
        Locale newLocale = WPPrefUtils.languageLocale(language);

        return !currentLocale.getLanguage().equals(newLocale.getLanguage());
    }

    /**
     * If the user has selected a language other than the device default, return that
     * language code, else just return the device default language code.
     * @return The 2-letter language code
     */
    private static String getLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(LANGUAGE_KEY)) {
            return prefs.getString(LANGUAGE_KEY, "");
        }
        return LanguageUtils.getCurrentDeviceLanguageCode(context);
    }

    /**
     * Save the updated language to SharedPreferences.
     * Use commit() instead of apply() to ensure the language preference is saved instantly
     * as the app may be restarted immediately.
     * @param context The current context
     * @param language The 2-letter language code (example "en")
     */
    @SuppressLint("ApplySharedPref")
    private static void saveLanguageToPref(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(LANGUAGE_KEY, language).commit();
    }

    /**
     * Remove any saved custom language selection from SharedPreferences.
     * Use commit() instead of apply() to ensure the language preference is saved instantly
     * as the app may be restarted immediately.
     * @param context The current context
     */
    @SuppressLint("ApplySharedPref")
    private static void removePersistedLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(LANGUAGE_KEY).commit();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
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
     *     String.format(LocaleManager.getSafeLocale(context), baseString, val)
     * </code>
     *
     * An example of a string that contains locale-specific grouping separators:
     * <code>
     *     <string name="test">%,d likes</string>
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
            return WPPrefUtils.languageLocale(baseLocale.getLanguage());
        } else {
            return baseLocale;
        }
    }
}
