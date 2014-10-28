package org.wordpress.android.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutolinkUtils {
    private static final Set<Pattern> PROVIDERS;

    static {
        PROVIDERS = new HashSet<Pattern>();
        PROVIDERS.add(Pattern.compile("(http://(www\\.)?youtube\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https://(www\\.)?youtube\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://youtu\\.be/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https://youtu\\.be/\\S+)"));
    }

    public static String autoCreateLinks(String text) {
        if (text == null) {
            return null;
        }
        Pattern urlPattern = Pattern.compile("((http|https|ftp|mailto):\\S+)");
        Matcher matcher = urlPattern.matcher(text);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            boolean blacklisted = false;
            // Check if the URL is blacklisted
            for (Pattern providerPattern : PROVIDERS) {
                Matcher providerMatcher = providerPattern.matcher(url);
                if (providerMatcher.matches()) {
                    blacklisted = true;
                }
            }
            // Create a <a href> HTML tag for the link
            if (!blacklisted) {
                matcher.appendReplacement(stringBuffer,  "<a href=\"" + url + "\">" + url + "</a>");
            } else {
                matcher.appendReplacement(stringBuffer, url);
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
