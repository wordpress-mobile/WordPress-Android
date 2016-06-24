package org.wordpress.android.ui.posts;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostUtils {

    private static final HashSet<String> mShortcodeTable = new HashSet<>();

    /*
     * collapses shortcodes in the passed post content, stripping anything between the
     * shortcode name and the closing brace
     * ex: collapseShortcodes("[gallery ids="1206,1205,1191"]") -> "[gallery]"
     */
    public static String collapseShortcodes(final String postContent) {
        // speed things up by skipping regex if content doesn't contain a brace
        if (postContent == null || !postContent.contains("[")) {
            return postContent;
        }

        String shortCode;
        Pattern p = Pattern.compile("(\\[ *([^ ]+) [^\\[\\]]*\\])");
        Matcher m = p.matcher(postContent);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            shortCode = m.group(2);
            if (isKnownShortcode(shortCode)) {
                m.appendReplacement(sb, "[" + shortCode + "]");
            } else {
                AppLog.d(AppLog.T.POSTS, "unknown shortcode - " + shortCode);
            }
        }
        m.appendTail(sb);

        return sb.toString();
    }

    private static boolean isKnownShortcode(String shortCode) {
        if (shortCode == null) return false;

        // populate on first use
        if (mShortcodeTable.size() == 0) {
            // default shortcodes
            mShortcodeTable.add("audio");
            mShortcodeTable.add("caption");
            mShortcodeTable.add("embed");
            mShortcodeTable.add("gallery");
            mShortcodeTable.add("playlist");
            mShortcodeTable.add("video");
            mShortcodeTable.add("wp_caption");
            // audio/video
            mShortcodeTable.add("dailymotion");
            mShortcodeTable.add("flickr");
            mShortcodeTable.add("hulu");
            mShortcodeTable.add("kickstarter");
            mShortcodeTable.add("soundcloud");
            mShortcodeTable.add("vimeo");
            mShortcodeTable.add("vine");
            mShortcodeTable.add("wpvideo");
            mShortcodeTable.add("youtube");
            // images and documents
            mShortcodeTable.add("instagram");
            mShortcodeTable.add("scribd");
            mShortcodeTable.add("slideshare");
            mShortcodeTable.add("slideshow");
            mShortcodeTable.add("presentation");
            mShortcodeTable.add("googleapps");
            mShortcodeTable.add("office");
            // other
            mShortcodeTable.add("googlemaps");
            mShortcodeTable.add("polldaddy");
            mShortcodeTable.add("recipe");
            mShortcodeTable.add("sitemap");
            mShortcodeTable.add("twitter-timeline");
            mShortcodeTable.add("upcomingevents");
        }

        return mShortcodeTable.contains(shortCode);
    }

    public static void trackSavePostAnalytics(Post post) {
        PostStatus status = post.getStatusEnum();
        switch (status) {
            case PUBLISHED:
                if (!post.isLocalDraft()) {
                    AnalyticsUtils.trackWithBlogDetails(
                            AnalyticsTracker.Stat.EDITOR_UPDATED_POST,
                            WordPress.getBlog(post.getLocalTableBlogId())
                    );
                } else {
                    // Analytics for the event EDITOR_PUBLISHED_POST are tracked in PostUploadService
                }
                break;
            case SCHEDULED:
                if (!post.isLocalDraft()) {
                    AnalyticsUtils.trackWithBlogDetails(
                            AnalyticsTracker.Stat.EDITOR_UPDATED_POST,
                            WordPress.getBlog(post.getLocalTableBlogId())
                    );
                } else {
                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put("word_count", AnalyticsUtils.getWordCount(post.getContent()));
                    AnalyticsUtils.trackWithBlogDetails(
                            AnalyticsTracker.Stat.EDITOR_SCHEDULED_POST,
                            WordPress.getBlog(post.getLocalTableBlogId()),
                            properties
                    );
                }
                break;
            case DRAFT:
                AnalyticsUtils.trackWithBlogDetails(
                        AnalyticsTracker.Stat.EDITOR_SAVED_DRAFT,
                        WordPress.getBlog(post.getLocalTableBlogId())
                );
                break;
            default:
                // No-op
        }
    }
}
