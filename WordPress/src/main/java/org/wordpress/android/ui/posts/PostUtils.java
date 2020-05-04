package org.wordpress.android.ui.posts;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType;
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessor;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.uploads.PostEvents;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.utils.UiString.UiStringText;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private static final int SRC_ATTRIBUTE_LENGTH_PLUS_ONE = 5;
    private static final String GB_IMG_BLOCK_HEADER_PLACEHOLDER = "<!-- wp:image {\"id\":%s";
    private static final String GB_IMG_BLOCK_CLASS_PLACEHOLDER = "class=\"wp-image-%s\"";
    private static final String GB_MEDIA_TEXT_BLOCK_HEADER_PLACEHOLDER = "<!-- wp:media-text {\"mediaId\":%s";

    public static Map<String, Object> addPostTypeToAnalyticsProperties(PostImmutableModel post,
                                                                       Map<String, Object> properties) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put("post_type", post.isPage() ? "page" : "post");
        return properties;
    }

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

    public static void trackSavePostAnalytics(PostImmutableModel post, SiteModel site) {
        PostStatus status = PostStatus.fromPost(post);
        Map<String, Object> properties = new HashMap<>();
        PostUtils.addPostTypeToAnalyticsProperties(post, properties);
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
                            shouldShowGutenbergEditor(post.isLocalDraft(), post.getContent(), site)
                                    ? SiteUtils.GB_EDITOR_NAME : SiteUtils.AZTEC_EDITOR_NAME);
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

    public static void trackOpenEditorAnalytics(PostImmutableModel post, SiteModel site) {
        Map<String, Object> properties = new HashMap<>();
        PostUtils.addPostTypeToAnalyticsProperties(post, properties);
        if (!post.isLocalDraft()) {
            properties.put("post_id", post.getRemotePostId());
        }
        properties.put(AnalyticsUtils.EDITOR_HAS_HW_ACCELERATION_DISABLED_KEY, AppPrefs.isPostWithHWAccelerationOff(
                site.getId(), post.getId()) ? "1" : "0");
        properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                PostUtils.contentContainsGutenbergBlocks(post.getContent()));
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_OPENED, site,
                properties);
    }

    public static boolean isPublishable(PostImmutableModel post) {
        return post != null && !(post.getContent().trim().isEmpty()
                                 && post.getExcerpt().trim().isEmpty()
                                 && post.getTitle().trim().isEmpty());
    }

    public static boolean hasEmptyContentFields(PostImmutableModel post) {
        return TextUtils.isEmpty(post.getTitle()) && TextUtils.isEmpty(post.getContent());
    }

    /**
     * Checks if two posts have differing data
     */
    public static boolean postHasEdits(@Nullable PostImmutableModel oldPost, PostImmutableModel newPost) {
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
                                    && StringUtils.equals(oldPost.getSlug(), newPost.getSlug())
                                    && oldPost.getFeaturedImageId() == newPost.getFeaturedImageId()
                                    && oldPost.getTagNameList().containsAll(newPost.getTagNameList())
                                    && newPost.getTagNameList().containsAll(oldPost.getTagNameList())
                                    && oldPost.getCategoryIdList().containsAll(newPost.getCategoryIdList())
                                    && newPost.getCategoryIdList().containsAll(oldPost.getCategoryIdList())
                                    && PostLocation.Companion.equals(oldPost.getLocation(), newPost.getLocation())
                                    && oldPost.getChangesConfirmedContentHashcode() == newPost
                .getChangesConfirmedContentHashcode()
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
        if (StringUtils.isEmpty(description)) {
            return null;
        }

        String s = HtmlUtils.fastStripHtml(removeWPGallery(description));
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

    /**
     * Removes the wp-gallery tag and its internals from the given string.
     *
     * See https://github.com/wordpress-mobile/WordPress-Android/issues/11063
     */
    public static String removeWPGallery(String str) {
        return str.replaceAll("(?s)<!--\\swp:gallery?(.*?)wp:gallery\\s-->", "");
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
        if (!shouldPublishImmediatelyOptionBeAvailable(PostStatus.fromPost(postModel))) {
            return false;
        }
        Date pubDate = DateTimeUtils.dateFromIso8601(postModel.getDateCreated());
        Date now = new Date();
        // Publish immediately for posts that don't have any date set yet and drafts with publish dates in the past
        return pubDate == null || !pubDate.after(now);
    }

    static boolean shouldPublishImmediately(PostStatus postStatus, String dateCreated) {
        if (!shouldPublishImmediatelyOptionBeAvailable(postStatus)) {
            return false;
        }
        Date pubDate = DateTimeUtils.dateFromIso8601(dateCreated);
        Date now = new Date();
        // Publish immediately for posts that don't have any date set yet and drafts with publish dates in the past
        return pubDate == null || !pubDate.after(now);
    }

    public static boolean isPublishDateInTheFuture(String dateCreated) {
        Date pubDate = DateTimeUtils.dateFromIso8601(dateCreated);
        Date now = new Date();
        return pubDate != null && pubDate.after(now);
    }

    public static boolean isPublishDateInTheFuture(String dateCreated, Date now) {
        Date pubDate = DateTimeUtils.dateFromIso8601(dateCreated);
        return pubDate != null && pubDate.after(now);
    }

    public static boolean isPublishDateInThePast(String dateCreated) {
        Date pubDate = DateTimeUtils.dateFromIso8601(dateCreated);

        // just use half an hour before now as a threshold to make sure this is backdated, to avoid false positives
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -30);
        Date halfHourBack = cal.getTime();
        return pubDate != null && pubDate.before(halfHourBack);
    }

    // Only drafts should have the option to publish immediately to avoid user confusion
    static boolean shouldPublishImmediatelyOptionBeAvailable(PostModel postModel) {
        return PostStatus.fromPost(postModel) == PostStatus.DRAFT;
    }

    public static boolean shouldPublishImmediatelyOptionBeAvailable(PostStatus postStatus) {
        return postStatus == PostStatus.DRAFT;
    }

    static boolean shouldPublishImmediatelyOptionBeAvailable(String postStatus) {
        return postStatus.equals(PostStatus.DRAFT.toString());
    }

    public static void updatePublishDateIfShouldBePublishedImmediately(PostModel postModel) {
        if (shouldPublishImmediately(postModel)) {
            postModel.setDateCreated(DateTimeUtils.iso8601FromDate(new Date()));
        }
    }

    public static boolean isFirstTimePublish(PostImmutableModel post) {
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

    public static boolean shouldShowGutenbergEditor(boolean isNewPost, String postContent, SiteModel site) {
        // Default to Gutenberg

        if (isNewPost || TextUtils.isEmpty(postContent)) {
            // For a new post, use Gutenberg if the "use for new posts" switch is set
            return SiteUtils.isBlockEditorDefaultForNewPost(site);
        } else {
            // for already existing (and non-empty) posts, open Gutenberg only if the post contains blocks
            return contentContainsGutenbergBlocks(postContent);
        }
    }

    public static String replaceMediaFileWithUrlInGutenbergPost(@NonNull String postContent,
                                                 String localMediaId, MediaFile mediaFile, String siteUrl) {
        if (mediaFile != null && contentContainsGutenbergBlocks(postContent)) {
            String remoteUrl = org.wordpress.android.util.StringUtils
                    .notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
            MediaUploadCompletionProcessor processor = new MediaUploadCompletionProcessor(localMediaId, mediaFile,
                    siteUrl);
            postContent = processor.processPost(postContent);
        }
        return postContent;
    }

    public static boolean isMediaInGutenbergPostBody(@NonNull String postContent,
                                            String localMediaId) {
        List<String> patterns = new ArrayList<>();
        // Regex for Image and Video blocks
        patterns.add("<!-- wp:(?:image|video){1} \\{[^\\}]*\"id\":%s([^\\d\\}][^\\}]*)*\\} -->");
        // Regex for Media&Text block
        patterns.add("<!-- wp:media-text \\{[^\\}]*\"mediaId\":%s([^\\d\\}][^\\}]*)*\\} -->");
        // Regex for Gallery block
        patterns.add("<!-- wp:gallery \\{[^\\}]*\"ids\":\\[(?:\\d*,)*%s(?:,\\d*)*\\][^\\}]*\\} -->");

        StringBuilder sb = new StringBuilder();
        // Merge the patterns into one so we don't need to go over the post content multiple times
        for (int i = 0; i < patterns.size(); i++) {
            sb.append("(?:")
              // insert the media id
              .append(String.format(patterns.get(i), localMediaId))
              .append(")");
            boolean notLast = i != patterns.size() - 1;
            if (notLast) {
                sb.append("|");
            }
        }

        Matcher matcher = Pattern.compile(sb.toString()).matcher(postContent);
        return matcher.find();
    }

    public static boolean isPostInConflictWithRemote(PostImmutableModel post) {
        // at this point we know there's a potential version conflict (the post has been modified
        // both locally and on the remote)
        return !post.getLastModified().equals(post.getRemoteLastModified()) && post.isLocallyChanged();
    }

    public static boolean hasAutoSave(PostModel post) {
        // TODO: would be great to check if title, content and excerpt are different,
        // but we currently don't have them when we fetch the post list

        // Ignore auto-saves in case the post is locally changed.
        // This might be changed in the future to show a better conflict UX.
        return !post.isLocallyChanged()
               // has auto-save
               && post.hasUnpublishedRevision();
    }

    public static String getConflictedPostCustomStringForDialog(PostModel post) {
        Context context = WordPress.getContext();
        String firstPart = context.getString(R.string.dialog_confirm_load_remote_post_body);
        String lastModified =
                TextUtils.isEmpty(post.getDateLocallyChanged()) ? post.getLastModified() : post.getDateLocallyChanged();
        String secondPart =
                String.format(context.getString(R.string.dialog_confirm_load_remote_post_body_2),
                        getFormattedDateForLastModified(
                                context, DateTimeUtils.timestampFromIso8601Millis(lastModified)),
                        getFormattedDateForLastModified(
                                context, DateTimeUtils.timestampFromIso8601Millis(post.getRemoteLastModified())));
        return firstPart + secondPart;
    }

    public static UiStringText getCustomStringForAutosaveRevisionDialog(PostModel post) {
        Context context = WordPress.getContext();
        String firstPart = post.isPage() ? context.getString(R.string.dialog_confirm_autosave_body_first_part_for_page)
                : context.getString(R.string.dialog_confirm_autosave_body_first_part);

        String lastModified =
                TextUtils.isEmpty(post.getDateLocallyChanged()) ? post.getLastModified() : post.getDateLocallyChanged();
        String secondPart =
                String.format(context.getString(R.string.dialog_confirm_autosave_body_second_part),
                        getFormattedDateForLastModified(
                                context, DateTimeUtils.timestampFromIso8601Millis(lastModified)),
                        getFormattedDateForLastModified(
                                context, DateTimeUtils.timestampFromIso8601Millis(post.getAutoSaveModified())));
        return new UiStringText(firstPart + secondPart);
    }

    /**
     * E.g. Jul 2, 2013 @ 21:57
     */
    public static String getFormattedDateForLastModified(Context context, long timeSinceLastModified) {
        Date date = new Date(timeSinceLastModified);

        DateFormat dateFormat = DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                LocaleManager.getSafeLocale(context));
        DateFormat timeFormat = DateFormat.getTimeInstance(
                DateFormat.SHORT,
                LocaleManager.getSafeLocale(context));

        dateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
        timeFormat.setTimeZone(Calendar.getInstance().getTimeZone());

        return dateFormat.format(date) + " @ " + timeFormat.format(date);
    }

    public static String getPreviewUrlForPost(RemotePreviewType remotePreviewType, PostImmutableModel post) {
        String previewUrl;

        switch (remotePreviewType) {
            case NOT_A_REMOTE_PREVIEW:
            case REMOTE_PREVIEW:
                // always add the preview parameter to avoid bumping stats when viewing posts
                previewUrl = UrlUtils.appendUrlParameter(post.getLink(), "preview", "true");
                break;
            case REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE:
                previewUrl = post.getAutoSavePreviewUrl();
                break;
            default:
                throw new IllegalArgumentException(
                        "Cannot get a Preview URL for " + remotePreviewType + " Preview type."
                );
        }

        return previewUrl;
    }

    public static void preparePostForPublish(PostModel post, SiteModel site) {
        PostUtils.updatePublishDateIfShouldBePublishedImmediately(post);
        post.setDateLocallyChanged(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));

        // We need to update the post status and mark the post as locally changed. If we didn't mark it as locally
        // changed the UploadStarter wouldn't upload the post if the only change the user did was clicking on Publish
        // button.
        if (UploadUtils.userCanPublish(site)) {
            if (PostStatus.fromPost(post) != PostStatus.PRIVATE) {
                post.setStatus(PostStatus.PUBLISHED.toString());
            }
        } else {
            post.setStatus(PostStatus.PENDING.toString());
        }
        if (!post.isLocalDraft()) {
            post.setIsLocallyChanged(true);
        }
        AppLog.d(T.POSTS, "User explicitly confirmed changes. Post title: " + post.getTitle());
        // the changes were explicitly confirmed by the user
        post.setChangesConfirmedContentHashcode(post.contentHashcode());
    }

    public static boolean isPostCurrentlyBeingEdited(PostImmutableModel post) {
        PostEvents.PostOpenedInEditor flag = EventBus.getDefault().getStickyEvent(PostEvents.PostOpenedInEditor.class);
        return flag != null && post != null
               && post.getLocalSiteId() == flag.localSiteId
               && post.getId() == flag.postId;
    }
}
