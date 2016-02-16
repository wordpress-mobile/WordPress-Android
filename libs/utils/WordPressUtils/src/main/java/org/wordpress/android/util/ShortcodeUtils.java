package org.wordpress.android.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortcodeUtils {
    public static String getVideoPressShortcodeFromId(String videoPressId) {
        if (videoPressId == null || videoPressId.isEmpty()) {
            return "";
        }

        return "[wpvideo " + videoPressId + "]";
    }

    public static String getVideoPressIdFromShortCode(String shortcode) {
        String videoPressId = "";

        if (shortcode != null) {
            String videoPressShortcodeRegex = "^\\[wpvideo (.*)]$";

            Pattern pattern = Pattern.compile(videoPressShortcodeRegex);
            Matcher matcher = pattern.matcher(shortcode);

            if (matcher.find()) {
                videoPressId = matcher.group(1);
            }
        }

        return videoPressId;
    }
}
