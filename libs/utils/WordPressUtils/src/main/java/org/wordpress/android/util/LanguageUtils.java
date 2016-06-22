package org.wordpress.android.util;

import android.content.Context;

import java.util.Locale;

/**
 * Methods for dealing with i18n messages
 */
public class LanguageUtils {

    public static Locale getCurrentDeviceLanguage(Context context) {
        //better use getConfiguration as it has the latest locale configuration change.
        //Otherwise Locale.getDefault().getLanguage() gets
        //the config upon application launch.
        Locale deviceLocale = context != null ? context.getResources().getConfiguration().locale : Locale.getDefault();
        return deviceLocale;
    }

    public static String getCurrentDeviceLanguageCode(Context context) {
        String deviceLanguageCode = getCurrentDeviceLanguage(context).toString();
        return deviceLanguageCode;
    }

    public static String getPatchedCurrentDeviceLanguage(Context context) {
        return patchDeviceLanguageCode(getCurrentDeviceLanguageCode(context));
    }

    /**
     * Patches a deviceLanguageCode if any of deprecated values iw, id, or yi
     */
    public static String patchDeviceLanguageCode(String deviceLanguageCode){
        String patchedCode = deviceLanguageCode;
        /*
         <p>Note that Java uses several deprecated two-letter codes. The Hebrew ("he") language
         * code is rewritten as "iw", Indonesian ("id") as "in", and Yiddish ("yi") as "ji". This
         * rewriting happens even if you construct your own {@code Locale} object, not just for
         * instances returned by the various lookup methods.
         */
        if (deviceLanguageCode != null) {
            if (deviceLanguageCode.startsWith("iw"))
                patchedCode = deviceLanguageCode.replace("iw", "he");
            else if (deviceLanguageCode.startsWith("in"))
                patchedCode = deviceLanguageCode.replace("in", "id");
            else if (deviceLanguageCode.startsWith("ji"))
                patchedCode = deviceLanguageCode.replace("ji", "yi");
        }

        return patchedCode;
    }

}
