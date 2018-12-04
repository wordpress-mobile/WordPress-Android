package org.wordpress.android.ui.posts;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.apache.commons.lang3.StringUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.text.BreakIterator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostUtils {
    private static final int MAX_EXCERPT_LEN = 150;

    private static final HashSet<String> SHORTCODE_TABLE = new HashSet<>();

    private static final String GUTENBERG_BLOCK_START = "<!-- wp:";

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
        if (shortCode == null) {
            return false;
        }

        // populate on first use
        if (SHORTCODE_TABLE.size() == 0) {
            // default shortcodes
            SHORTCODE_TABLE.add("audio");
            SHORTCODE_TABLE.add("caption");
            SHORTCODE_TABLE.add("embed");
            SHORTCODE_TABLE.add("gallery");
            SHORTCODE_TABLE.add("playlist");
            SHORTCODE_TABLE.add("video");
            SHORTCODE_TABLE.add("wp_caption");
            // audio/video
            SHORTCODE_TABLE.add("dailymotion");
            SHORTCODE_TABLE.add("flickr");
            SHORTCODE_TABLE.add("hulu");
            SHORTCODE_TABLE.add("kickstarter");
            SHORTCODE_TABLE.add("soundcloud");
            SHORTCODE_TABLE.add("vimeo");
            SHORTCODE_TABLE.add("vine");
            SHORTCODE_TABLE.add("wpvideo");
            SHORTCODE_TABLE.add("youtube");
            // images and documents
            SHORTCODE_TABLE.add("instagram");
            SHORTCODE_TABLE.add("scribd");
            SHORTCODE_TABLE.add("slideshare");
            SHORTCODE_TABLE.add("slideshow");
            SHORTCODE_TABLE.add("presentation");
            SHORTCODE_TABLE.add("googleapps");
            SHORTCODE_TABLE.add("office");
            // other
            SHORTCODE_TABLE.add("googlemaps");
            SHORTCODE_TABLE.add("polldaddy");
            SHORTCODE_TABLE.add("recipe");
            SHORTCODE_TABLE.add("sitemap");
            SHORTCODE_TABLE.add("twitter-timeline");
            SHORTCODE_TABLE.add("upcomingevents");
        }

        return SHORTCODE_TABLE.contains(shortCode);
    }

    public static void trackSavePostAnalytics(PostModel post, SiteModel site) {
        PostStatus status = PostStatus.fromPost(post);
        Map<String, Object> properties = new HashMap<>();
        switch (status) {
            case PUBLISHED:
                if (!post.isLocalDraft()) {
                    properties.put("post_id", post.getRemotePostId());
                    properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                            PostUtils.contentContainsGutenbergBlocks(post.getContent()));
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_UPDATED_POST, site, properties);
                } else {
                    // Analytics for the event EDITOR_PUBLISHED_POST are tracked in PostUploadHandler
                }
                break;
            case SCHEDULED:
                if (!post.isLocalDraft()) {
                    properties.put("post_id", post.getRemotePostId());
                    properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                            PostUtils.contentContainsGutenbergBlocks(post.getContent()));
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_UPDATED_POST, site, properties);
                } else {
                    properties.put("word_count", AnalyticsUtils.getWordCount(post.getContent()));
                    properties.put("editor_source",
                                shouldShowGutenbergEditor(post.isLocalDraft(), post) ? "gutenberg"
                                    : (AppPrefs.isAztecEditorEnabled() ? "aztec"
                                        : AppPrefs.isVisualEditorEnabled() ? "hybrid" : "legacy"));

                    properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                            PostUtils.contentContainsGutenbergBlocks(post.getContent()));
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_SCHEDULED_POST, site,
                                                        properties);
                }
                break;
            case DRAFT:
                properties.put("post_id", post.getRemotePostId());
                properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                        PostUtils.contentContainsGutenbergBlocks(post.getContent()));
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_SAVED_DRAFT, site, properties);
                break;
            default:
                // No-op
        }
    }

    public static void trackOpenPostAnalytics(PostModel post, SiteModel site) {
        Map<String, Object> properties = new HashMap<>();
        if (!post.isLocalDraft()) {
            properties.put("post_id", post.getRemotePostId());
        }
        properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                PostUtils.contentContainsGutenbergBlocks(post.getContent()));
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_OPENED, site,
                properties);
    }

    public static boolean isPublishable(PostModel post) {
        return post != null && !(post.getContent().trim().isEmpty()
                                 && post.getExcerpt().trim().isEmpty()
                                 && post.getTitle().trim().isEmpty());
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
                                            DateTimeUtils.timestampFromIso8601Millis(post.getDateCreated()),
                                            DateUtils.FORMAT_ABBREV_ALL);
        } else {
            return DateTimeUtils.javaDateToTimeSpan(DateTimeUtils.dateUTCFromIso8601(post.getDateCreated()),
                                                    WordPress.getContext());
        }
    }

    static boolean shouldPublishImmediately(PostModel postModel) {
        if (!shouldPublishImmediatelyOptionBeAvailable(postModel)) {
            return false;
        }
        Date pubDate = DateTimeUtils.dateFromIso8601(postModel.getDateCreated());
        Date now = new Date();
        // Publish immediately for posts that don't have any date set yet and drafts with publish dates in the past
        return pubDate == null || !pubDate.after(now);
    }

    // Only drafts should have the option to publish immediately to avoid user confusion
    static boolean shouldPublishImmediatelyOptionBeAvailable(PostModel postModel) {
        return PostStatus.fromPost(postModel) == PostStatus.DRAFT;
    }

    public static void updatePublishDateIfShouldBePublishedImmediately(PostModel postModel) {
        if (shouldPublishImmediately(postModel)) {
            postModel.setDateCreated(DateTimeUtils.iso8601FromDate(new Date()));
        }
    }

    static boolean updatePostTitleIfDifferent(PostModel post, String newTitle) {
        if (post.getTitle().compareTo(newTitle) != 0) {
            post.setTitle(newTitle);
            return true;
        }
        return false;
    }

    static boolean updatePostContentIfDifferent(PostModel post, String newContent) {
        if (post.getContent().compareTo(newContent) != 0) {
            post.setContent(newContent);
            return true;
        }
        return false;
    }

    public static boolean isFirstTimePublish(PostModel post) {
        return PostStatus.fromPost(post) == PostStatus.DRAFT
               || (PostStatus.fromPost(post) == PostStatus.PUBLISHED && post.isLocalDraft());
    }

    public static Set<PostModel> getPostsThatIncludeAnyOfTheseMedia(PostStore postStore,
                                                                    List<MediaModel> mediaModelList) {
        // if there' a Post to which the retried media belongs, clear their status
        HashSet<PostModel> postsThatContainListedMedia = new HashSet<>();
        for (MediaModel media : mediaModelList) {
            if (media.getLocalPostId() > 0) {
                PostModel post = postStore.getPostByLocalPostId(media.getLocalPostId());
                if (post != null) {
                    postsThatContainListedMedia.add(post);
                }
            }
        }

        return postsThatContainListedMedia;
    }

    /*
    Note the way we detect we're in presence of Gutenberg blocks logic is taken from
    https://github.com/WordPress/gutenberg/blob/5a6693589285363341bebad15bd56d9371cf8ecc/lib/register.php#L331-L345

    * Determine whether a content string contains blocks. This test optimizes for
    * performance rather than strict accuracy, detecting the pattern of a block
    * but not validating its structure. For strict accuracy, you should use the
    * block parser on post content.
    *
    * @since 1.6.0
    * @see gutenberg_parse_blocks()
    *
    * @param string $content Content to test.
    * @return bool Whether the content contains blocks.

    function gutenberg_content_has_blocks( $content ) {
        return false !== strpos( $content, '<!-- wp:' );
    }
    */
    public static boolean contentContainsGutenbergBlocks(String postContent) {
        return (postContent != null && postContent.contains(GUTENBERG_BLOCK_START));
    }

    public static void showGutenbergCompatibilityWarningDialog(Context ctx,
                                                               FragmentManager fragmentManager,
                                                               PostModel post,
                                                               SiteModel site) {
        GutenbergWarningFragmentDialog gutenbergCompatibilityDialog = new GutenbergWarningFragmentDialog();
        gutenbergCompatibilityDialog.initialize(post.getRemotePostId(), post.isPage());
        gutenbergCompatibilityDialog.show(fragmentManager, "tag_gutenberg_confirm_dialog");

        // track event
        trackGutenbergDialogEvent(AnalyticsTracker.Stat.GUTENBERG_WARNING_CONFIRM_DIALOG_SHOWN,
                post, site);
    }

    public static void trackGutenbergDialogEvent(AnalyticsTracker.Stat stat, PostModel post, SiteModel site) {
        // track event
        Map<String, Object> properties = new HashMap<>();
        if (!post.isLocalDraft()) {
            properties.put("post_id", post.getRemotePostId());
        }
        properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY, true);
        properties.put("is_page", post.isPage());
        AnalyticsUtils.trackWithSiteDetails(stat, site, properties);
    }

    public static boolean shouldShowGutenbergEditor(boolean isNewPost, PostModel post) {
        return AppPrefs.isGutenbergEditorEnabled()
               && (isNewPost || contentContainsGutenbergBlocks(post.getContent()));
    }
}
