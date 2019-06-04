package org.wordpress.android.util;

import android.content.Context;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Methods for dealing with i18n messages
 */
public class LanguageUtils {
    /**
     * @deprecated Use {@link #getCurrentDeviceLanguage()}. As of API 25, setting the locale by updating the
     * configuration on the resources object was deprecated, so this method stopped working for newer versions
     * of Android. The current active locale should always be set in {@link Locale#getDefault()}. When manually
     * setting the active locale, the developer should set it in {@link Locale#setDefault(Locale)}.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static Locale getCurrentDeviceLanguage(@Nullable Context context) {
        return getCurrentDeviceLanguage();
    }

    @SuppressWarnings("WeakerAccess")
    public static Locale getCurrentDeviceLanguage() {
        return Locale.getDefault();
    }

    /**
     * @deprecated Use {@link #getCurrentDeviceLanguageCode()}.
     */
    @SuppressWarnings("WeakerAccess,DeprecatedIsStillUsed")
    @Deprecated
    public static String getCurrentDeviceLanguageCode(@Nullable Context context) {
        return getCurrentDeviceLanguageCode();
    }

    @SuppressWarnings("WeakerAccess")
    public static String getCurrentDeviceLanguageCode() {
        return getCurrentDeviceLanguage().toString();
    }

    public static String getPatchedCurrentDeviceLanguage(Context context) {
        return patchDeviceLanguageCode(getCurrentDeviceLanguageCode(context));
    }

    /**
     * Patches a deviceLanguageCode if any of deprecated values iw, id, or yi
     */
    @SuppressWarnings("WeakerAccess")
    public static String patchDeviceLanguageCode(String deviceLanguageCode) {
        String patchedCode = deviceLanguageCode;
        /*
         <p>Note that Java uses several deprecated two-letter codes. The Hebrew ("he") language
         * code is rewritten as "iw", Indonesian ("id") as "in", and Yiddish ("yi") as "ji". This
         * rewriting happens even if you construct your own {@code Locale} object, not just for
         * instances returned by the various lookup methods.
         */
        if (deviceLanguageCode != null) {
            if (deviceLanguageCode.startsWith("iw")) {
                patchedCode = deviceLanguageCode.replace("iw", "he");
            } else if (deviceLanguageCode.startsWith("in")) {
                patchedCode = deviceLanguageCode.replace("in", "id");
            } else if (deviceLanguageCode.startsWith("ji")) {
                patchedCode = deviceLanguageCode.replace("ji", "yi");
            }
        }

        return patchedCode;
    }
}
