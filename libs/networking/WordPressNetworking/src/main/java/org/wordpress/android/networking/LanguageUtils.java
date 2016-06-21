package org.wordpress.android.networking;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Methods for dealing with i18n messages
 */
public class LanguageUtils {

    public static String getCurrentDeviceLanguage(Context context) {
        //better use getConfiguration as it has the latest locale configuration change.
        //Otherwise Locale.getDefault().getLanguage() gets
        //the config upon application launch.
        String deviceLanguageCode = context != null ? context.getResources().getConfiguration().locale.toString() : Locale.getDefault().getLanguage();
        return deviceLanguageCode;
    }

    public static String getPatchedCurrentDeviceLanguage(Context context) {
        return patchDeviceLanguageCode(getCurrentDeviceLanguage(context));
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

    /**
     * Returns locale parameter used in REST calls which require the response to be localized
     */
    public static HashMap<String, String> getRestLocaleParams(Context context) {
        HashMap<String, String> params = new HashMap<>();
        String deviceLanguageCode = getCurrentDeviceLanguage(context);
        if (!TextUtils.isEmpty(deviceLanguageCode)) {
            //patch locale if it's any of the deprecated codes as can be read in Locale.java source code:
            deviceLanguageCode = patchDeviceLanguageCode(deviceLanguageCode);
            params.put("locale", deviceLanguageCode);
        }
        return params;
    }

}
