package org.wordpress.android.editor.gutenberg;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

class GutenbergUtils {
    public static Boolean isDarkMode(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Returns the gutenberg-mobile specific translations
     *
     * @return Bundle a map of "english string" => [ "current locale string" ]
     */
    public static Bundle getTranslations(Activity activity) {
        Bundle translations = new Bundle();
        Locale defaultLocale = new Locale("en");
        Resources currentResources = activity.getResources();
        Context localizedContextCurrent = activity
                .createConfigurationContext(currentResources.getConfiguration());
        // if the current locale of the app is english stop here and return an empty map
        Configuration currentConfiguration = localizedContextCurrent.getResources().getConfiguration();
        if (currentConfiguration.locale.equals(defaultLocale)) {
            return translations;
        }

        // Let's create a Resources object for the default locale (english) to get the original values for our strings
        Configuration defaultLocaleConfiguration = new Configuration(currentConfiguration);
        defaultLocaleConfiguration.setLocale(defaultLocale);
        Context localizedContextDefault = activity
                .createConfigurationContext(defaultLocaleConfiguration);
        Resources englishResources = localizedContextDefault.getResources();

        // Strings are only being translated in the WordPress package
        // thus we need to get a reference of the R class for this package
        // Here we assume the Application class is at the same level as the R class
        // It will not work if this lib is used outside of WordPress-Android,
        // in this case let's just return an empty map
        Class<?> rString;
        Package mainPackage = activity.getApplication().getClass().getPackage();

        if (mainPackage == null) {
            return translations;
        }

        try {
            rString = activity.getApplication().getClassLoader().loadClass(mainPackage.getName() + ".R$string");
        } catch (ClassNotFoundException ex) {
            return translations;
        }

        for (Field stringField : rString.getDeclaredFields()) {
            int resourceId;
            try {
                resourceId = stringField.getInt(rString);
            } catch (IllegalArgumentException | IllegalAccessException iae) {
                AppLog.e(T.EDITOR, iae);
                continue;
            }

            String fieldName = stringField.getName();
            // Filter out all strings that are not prefixed with `gutenberg_native_`
            if (!fieldName.startsWith("gutenberg_native_")) {
                continue;
            }

            try {
                // Add the mapping english => [ translated ] to the bundle if both string are not empty
                String currentResourceString = currentResources.getString(resourceId);
                String englishResourceString = englishResources.getString(resourceId);
                if (currentResourceString.length() > 0 && englishResourceString.length() > 0) {
                    translations.putStringArrayList(
                            englishResourceString,
                            new ArrayList<>(Arrays.asList(currentResourceString))
                    );
                }
            } catch (Resources.NotFoundException rnfe) {
                AppLog.w(T.EDITOR, rnfe.getMessage());
            }
        }

        return translations;
    }
}
