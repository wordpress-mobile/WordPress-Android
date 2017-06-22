package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.widgets.WPAlertDialogFragment;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostUtils {
    private static final int MAX_EXCERPT_LEN = 150;

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

    public static void trackSavePostAnalytics(PostModel post, SiteModel site) {
        PostStatus status = PostStatus.fromPost(post);
        Map<String, Object> properties = new HashMap<>();
        switch (status) {
            case PUBLISHED:
                if (!post.isLocalDraft()) {
                    properties.put("post_id", post.getRemotePostId());
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_UPDATED_POST, site, properties);
                } else {
                    // Analytics for the event EDITOR_PUBLISHED_POST are tracked in PostUploadService
                }
                break;
            case SCHEDULED:
                if (!post.isLocalDraft()) {
                    properties.put("post_id", post.getRemotePostId());
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_UPDATED_POST, site, properties);
                } else {
                    properties.put("word_count", AnalyticsUtils.getWordCount(post.getContent()));
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_SCHEDULED_POST, site,
                            properties);
                }
                break;
            case DRAFT:
                properties.put("post_id", post.getRemotePostId());
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_SAVED_DRAFT, site);
                break;
            default:
                // No-op
        }
    }

    public static void showCustomDialog(Activity activity, String title, String message,
                                        String positiveButton, String negativeButton, String tag) {
        FragmentManager fm = activity.getFragmentManager();
        WPAlertDialogFragment saveDialog = (WPAlertDialogFragment) fm.findFragmentByTag(tag);
        if (saveDialog == null) {

            saveDialog = WPAlertDialogFragment.newCustomDialog(title, message, positiveButton, negativeButton);
        }
        if (!saveDialog.isAdded()) {
            saveDialog.show(fm, tag);
        }
    }

    public static boolean isPublishable(PostModel post) {
        return !(post.getContent().isEmpty() && post.getExcerpt().isEmpty() && post.getTitle().isEmpty());
    }

    public static boolean hasEmptyContentFields(PostModel post) {
        return TextUtils.isEmpty(post.getTitle()) && TextUtils.isEmpty(post.getContent());
    }

    /**
     * Checks if two posts have differing data
     */
    public static boolean postHasEdits(PostModel oldPost, PostModel newPost) {
        if (oldPost == null) {
            return newPost != null;
        }

        return newPost == null || !(StringUtils.equals(oldPost.getTitle(), newPost.getTitle())
                && StringUtils.equals(oldPost.getContent(), newPost.getContent())
                && StringUtils.equals(oldPost.getExcerpt(), newPost.getExcerpt())
                && StringUtils.equals(oldPost.getStatus(), newPost.getStatus())
                && StringUtils.equals(oldPost.getPassword(), newPost.getPassword())
                && StringUtils.equals(oldPost.getPostFormat(), newPost.getPostFormat())
                && StringUtils.equals(oldPost.getDateCreated(), newPost.getDateCreated())
                && oldPost.getFeaturedImageId() == newPost.getFeaturedImageId()
                && oldPost.getTagNameList().containsAll(newPost.getTagNameList())
                && newPost.getTagNameList().containsAll(oldPost.getTagNameList())
                && oldPost.getCategoryIdList().containsAll(newPost.getCategoryIdList())
                && newPost.getCategoryIdList().containsAll(oldPost.getCategoryIdList())
                && PostLocation.equals(oldPost.getLocation(), newPost.getLocation())
        );
    }

    public static String getPostListExcerptFromPost(PostModel post) {
        if (StringUtils.isEmpty(post.getExcerpt())) {
            return makeExcerpt(post.getContent());
        } else {
            return makeExcerpt(post.getExcerpt());
        }
    }


    /*
     * Java's string.trim() doesn't handle non-breaking space chars (#160), which may appear at the
     * end of post content - work around this by converting them to standard spaces before trimming
     */
    private static final String NBSP = String.valueOf((char) 160);

    private static String trimEx(final String s) {
        return s.replace(NBSP, " ").trim();
    }

    private static String makeExcerpt(String description) {
        if (TextUtils.isEmpty(description)) {
            return null;
        }

        String s = HtmlUtils.fastStripHtml(description);
        if (s.length() < MAX_EXCERPT_LEN) {
            return trimEx(s);
        }

        StringBuilder result = new StringBuilder();
        BreakIterator wordIterator = BreakIterator.getWordInstance();
        wordIterator.setText(s);
        int start = wordIterator.first();
        int end = wordIterator.next();
        int totalLen = 0;
        while (end != BreakIterator.DONE) {
            String word = s.substring(start, end);
            result.append(word);
            totalLen += word.length();
            if (totalLen >= MAX_EXCERPT_LEN) {
                break;
            }
            start = end;
            end = wordIterator.next();
        }

        if (totalLen == 0) {
            return null;
        }
        return trimEx(result.toString()) + "...";
    }

    public static String getFormattedDate(PostModel post) {
        if (PostStatus.fromPost(post) == PostStatus.SCHEDULED) {
            return DateUtils.formatDateTime(WordPress.getContext(),
                    DateTimeUtils.timestampFromIso8601Millis(post.getDateCreated()), DateUtils.FORMAT_ABBREV_ALL);
        } else {
            return DateTimeUtils.javaDateToTimeSpan(DateTimeUtils.dateUTCFromIso8601(post.getDateCreated()),
                    WordPress.getContext());
        }
    }

    public static boolean postListsAreEqual(List<PostModel> lhs, List<PostModel> rhs) {
        if (lhs == null || rhs == null || lhs.size() != rhs.size()) {
            return false;
        }

        for (int i = 0; i < rhs.size(); i++) {
            PostModel newPost = rhs.get(i);
            PostModel currentPost = lhs.get(i);

            if (!newPost.equals(currentPost)) {
                return false;
            }
        }
        return true;
    }

    public static int indexOfPostInList(final PostModel post, final List<PostModel> posts) {
        if (post == null) {
            return -1;
        }
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId() == post.getId() &&
                    posts.get(i).getLocalSiteId() == post.getLocalSiteId()) {
                return i;
            }
        }
        return -1;
    }

    public static @NotNull List<Integer> indexesOfFeaturedMediaIdInList(final long mediaId, List<PostModel> posts) {
        List<Integer> list = new ArrayList<>();
        if (mediaId == 0) {
            return list;
        }
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getFeaturedImageId() == mediaId) {
                list.add(i);
            }
        }
        return list;
    }

    static boolean shouldPublishImmediately(PostModel postModel) {
        if (!shouldPublishImmediatelyOptionBeAvailable(postModel)) {
            return false;
        }
        Date pubDate = DateTimeUtils.dateFromIso8601(postModel.getDateCreated());
        Date now = new Date();
        // For drafts with publish dates in the past, we should publish immediately
        return !pubDate.after(now);
    }

    // Only drafts should have the option to publish immediately to avoid user confusion
    static boolean shouldPublishImmediatelyOptionBeAvailable(PostModel postModel) {
        return PostStatus.fromPost(postModel) == PostStatus.DRAFT;
    }

    static void updatePublishDateIfShouldBePublishedImmediately(PostModel postModel) {
        if (shouldPublishImmediately(postModel)) {
            postModel.setDateCreated(DateTimeUtils.iso8601FromDate(new Date()));
        }
    }
}
