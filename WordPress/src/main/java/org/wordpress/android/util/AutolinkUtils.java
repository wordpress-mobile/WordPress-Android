package org.wordpress.android.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutolinkUtils {
    private static final Set<Pattern> PROVIDERS;

    static {
        PROVIDERS = new HashSet<Pattern>();
        PROVIDERS.add(Pattern.compile("(http://(www\\.)?youtube\\.com/watch\\S+)"));
        PROVIDERS.add(Pattern.compile("(https://(www\\.)?youtube\\.com/watch\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://(www\\.)?youtube\\.com/playlist\\S+)"));
        PROVIDERS.add(Pattern.compile("(https://(www\\.)?youtube\\.com/playlist\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://youtu\\.be/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https://youtu\\.be/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://blip.tv/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?vimeo\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?dailymotion\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://dai.ly/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?flickr\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://flic\\.kr/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?smugmug\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?hulu\\.com/watch/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://revision3.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://i*.photobucket.com/albums/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://gi*.photobucket.com/groups/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?scribd\\.com/doc/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://wordpress.tv/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?polldaddy\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://poll\\.fm/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?funnyordie\\.com/videos/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/\\S+?/status(es)?/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?soundcloud\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(.+?\\.)?slideshare\\.net/\\S+)"));
        PROVIDERS.add(Pattern.compile("(http://instagr(\\.am|am\\.com)/p/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?rdio\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://rd\\.io/x/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(open|play)\\.spotify\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?imgur\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?meetu(\\.ps|p\\.com)/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?issuu\\.com/\\S+/docs/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?collegehumor\\.com/video/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?mixcloud\\.com/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.|embed\\.)?ted\\.com/talks/\\S+)"));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?(animoto|video214)\\.com/play/\\S+)"));
    }

    public static String autoCreateLinks(String text) {
        if (text == null) {
            return null;
        }
        Pattern urlPattern = Pattern.compile("(\\s+|^)((http|https|ftp|mailto):\\S+)");
        Matcher matcher = urlPattern.matcher(text);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String whitespaces = matcher.group(1);
            String url = matcher.group(2);
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
                matcher.appendReplacement(stringBuffer, whitespaces + "<a href=\"" + url + "\">" + url + "</a>");
            } else {
                matcher.appendReplacement(stringBuffer, whitespaces + url);
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
