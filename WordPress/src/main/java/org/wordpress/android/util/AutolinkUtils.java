package org.wordpress.android.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutolinkUtils {
    private static final Set<Pattern> PROVIDERS;

    static {
        PROVIDERS = new HashSet<>();
        PROVIDERS.add(Pattern.compile("(https?://((m|www)\\.)?youtube\\.com/watch\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://((m|www)\\.)?youtube\\.com/playlist\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://youtu\\.be/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?vimeo\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?dailymotion\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://dai\\.ly/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?flickr\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://flic\\.kr/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?smugmug\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?hulu\\.com/watch/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(http://i*.photobucket.com/albums/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(http://gi*.photobucket.com/groups/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?scribd\\.com/doc/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://wordpress\\.tv/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?polldaddy\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://poll\\.fm/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?funnyordie\\.com/videos/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/\\S+/status(es)?/\\S+)",
                                      Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/\\S+$)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/\\S+/likes$)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/\\S+/lists/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/\\S+/timelines/\\S+)",
                                      Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?twitter\\.com/i/moments/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://vine\\.co/v/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?soundcloud\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(.+?\\.)?slideshare\\.net/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?instagr(\\.am|am\\.com)/p/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(open|play)\\.spotify\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(.+\\.)?imgur\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?meetu(\\.ps|p\\.com)/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?issuu\\.com/.+/docs/.+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?collegehumor\\.com/video/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?mixcloud\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.|embed\\.)?ted\\.com/talks/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?(animoto|video214)\\.com/play/\\S+)",
                                      Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(.+)\\.tumblr\\.com/post/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?kickstarter\\.com/projects/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://kck\\.st/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://cloudup\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?reverbnation\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://videopress\\.com/v/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?reddit\\.com/r/[^/]+/comments/\\S+)",
                                      Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://(www\\.)?speakerdeck\\.com/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/\\S+/posts/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/\\S+/activity/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/\\S+/photos/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS
                .add(Pattern.compile("(https?://www\\.facebook\\.com/photo(s/|\\.php)\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/permalink\\.php\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/media/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/questions/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/notes/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/\\S+/videos/\\S+)", Pattern.CASE_INSENSITIVE));
        PROVIDERS.add(Pattern.compile("(https?://www\\.facebook\\.com/video\\.php\\S+)", Pattern.CASE_INSENSITIVE));
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
