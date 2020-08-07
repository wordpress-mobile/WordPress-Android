package org.wordpress.android.ui.reader.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReaderUtils {
    public static String getResizedImageUrl(final String imageUrl, int width, int height, boolean isPrivate,
                                            boolean isPrivateAtomic) {
        return getResizedImageUrl(imageUrl, width, height, isPrivate, isPrivateAtomic, PhotonUtils.Quality.MEDIUM);
    }

    public static String getResizedImageUrl(final String imageUrl,
                                            int width,
                                            int height,
                                            boolean isPrivate,
                                            boolean isPrivateAtomic,
                                            PhotonUtils.Quality quality) {
        final String unescapedUrl = StringEscapeUtils.unescapeHtml4(imageUrl);


        if (isPrivate && !isPrivateAtomic) {
            return getPrivateImageForDisplay(unescapedUrl, width, height);
        } else {
            return PhotonUtils.getPhotonImageUrl(unescapedUrl, width, height, quality, isPrivateAtomic);
        }
    }

    /*
     * use this to request a reduced size image from a private post - images in private posts can't
     * use photon but these are usually wp images so they support the h= and w= query params
     */
    private static String getPrivateImageForDisplay(final String imageUrl, int width, int height) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        final String query;
        if (width > 0 && height > 0) {
            query = "?w=" + width + "&h=" + height;
        } else if (width > 0) {
            query = "?w=" + width;
        } else if (height > 0) {
            query = "?h=" + height;
        } else {
            query = "";
        }
        // remove the existing query string, add the new one, and make sure the url is https:
        return UrlUtils.removeQuery(UrlUtils.makeHttps(imageUrl)) + query;
    }

    /*
     * returns the passed string formatted for use with our API - see sanitize_title_with_dashes
     * https://github.com/WordPress/WordPress/blob/master/wp-includes/formatting.php#L1258
     * http://stackoverflow.com/a/1612015/1673548
     */
    public static String sanitizeWithDashes(final String title) {
        if (title == null) {
            return "";
        }

        return title.trim()
                    .replaceAll("&[^\\s]*;", "") // remove html entities
                    .replaceAll("[\\.\\s]+", "-") // replace periods and whitespace with a dash
                    .replaceAll("[^\\p{L}\\p{Nd}\\-]+",
                            "") // remove remaining non-alphanum/non-dash chars (Unicode aware)
                    .replaceAll("--", "-"); // reduce double dashes potentially added above
    }

    /*
     * returns the long text to use for a like label ("Liked by 3 people", etc.)
     */
    public static String getLongLikeLabelText(Context context, int numLikes, boolean isLikedByCurrentUser) {
        if (isLikedByCurrentUser) {
            switch (numLikes) {
                case 1:
                    return context.getString(R.string.reader_likes_only_you);
                case 2:
                    return context.getString(R.string.reader_likes_you_and_one);
                default:
                    String youAndMultiLikes = context.getString(R.string.reader_likes_you_and_multi);
                    return String.format(
                            LocaleManager.getSafeLocale(context), youAndMultiLikes, numLikes - 1);
            }
        } else {
            if (numLikes == 1) {
                return context.getString(R.string.reader_likes_one);
            } else {
                String likes = context.getString(R.string.reader_likes_multi);
                return String.format(LocaleManager.getSafeLocale(context), likes, numLikes);
            }
        }
    }

    /*
     * short like text ("1 like," "5 likes," etc.)
     */
    public static String getShortLikeLabelText(Context context, int numLikes) {
        switch (numLikes) {
            case 0:
                return context.getString(R.string.reader_short_like_count_none);
            case 1:
                return context.getString(R.string.reader_short_like_count_one);
            default:
                String count = FormatUtils.formatInt(numLikes);
                return String.format(context.getString(R.string.reader_short_like_count_multi), count);
        }
    }

    public static String getShortCommentLabelText(Context context, int numComments) {
        switch (numComments) {
            case 1:
                return context.getString(R.string.reader_short_comment_count_one);
            default:
                String count = FormatUtils.formatInt(numComments);
                return String.format(context.getString(R.string.reader_short_comment_count_multi), count);
        }
    }

    /*
     * returns true if a ReaderPost and ReaderComment exist for the passed Ids
     */
    public static boolean postAndCommentExists(long blogId, long postId, long commentId) {
        return ReaderPostTable.postExists(blogId, postId)
               && ReaderCommentTable.commentExists(blogId, postId, commentId);
    }

    /*
     * used by Discover site picks to add a "Visit [BlogName]" link which shows the
     * native blog preview for that blog
     */
    public static String makeBlogPreviewUrl(long blogId) {
        return "wordpress://blogpreview?blogId=" + Long.toString(blogId);
    }

    public static boolean isBlogPreviewUrl(String url) {
        return (url != null && url.startsWith("wordpress://blogpreview"));
    }

    public static long getBlogIdFromBlogPreviewUrl(String url) {
        if (isBlogPreviewUrl(url)) {
            String strBlogId = Uri.parse(url).getQueryParameter("blogId");
            return StringUtils.stringToLong(strBlogId);
        } else {
            return 0;
        }
    }

    /*
     * returns the passed string prefixed with a "#" if it's non-empty and isn't already
     * prefixed with a "#"
     */
    public static String makeHashTag(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return "";
        } else if (tagName.startsWith("#")) {
            return tagName;
        } else {
            return "#" + tagName;
        }
    }

    /*
     * set the background of the passed view to the round ripple drawable - only works on
     * Lollipop or later, does nothing on earlier Android versions
     */
    public static void setBackgroundToRoundRipple(View view) {
        if (view != null) {
            view.setBackgroundResource(R.drawable.ripple_oval);
        }
    }

    /*
     * returns a tag object from the passed endpoint if tag is in database, otherwise null
     */
    public static ReaderTag getTagFromEndpoint(String endpoint) {
        return ReaderTagTable.getTagFromEndpoint(endpoint);
    }

    /*
     * returns a tag object from the passed tag name - first checks for it in the tag db
     * (so we can also get its title & endpoint), returns a new tag if that fails
     */
    public static ReaderTag getTagFromTagName(String tagName, ReaderTagType tagType) {
        return getTagFromTagName(tagName, tagType, false);
    }

    public static ReaderTag getTagFromTagName(String tagName, ReaderTagType tagType, boolean markDefaultIfInMemory) {
        ReaderTag tag = ReaderTagTable.getTag(tagName, tagType);
        if (tag != null) {
            return tag;
        } else {
            return createTagFromTagName(tagName, tagType, markDefaultIfInMemory);
        }
    }

    public static ReaderTag createTagFromTagName(String tagName, ReaderTagType tagType) {
        return createTagFromTagName(tagName, tagType, false);
    }

    public static ReaderTag createTagFromTagName(String tagName, ReaderTagType tagType, boolean isDefaultInMemoryTag) {
        String tagSlug = sanitizeWithDashes(tagName).toLowerCase(Locale.ROOT);
        String tagDisplayName = tagType == ReaderTagType.DEFAULT ? tagName : tagSlug;
        return new ReaderTag(
                tagSlug,
                tagDisplayName,
                tagName,
                null,
                tagType,
                isDefaultInMemoryTag
        );
    }

    /*
     * returns the default tag, which is the one selected by default in the reader when
     * the user hasn't already chosen one
     */
    public static ReaderTag getDefaultTag() {
        ReaderTag defaultTag = getTagFromEndpoint(ReaderTag.TAG_ENDPOINT_DEFAULT);
        if (defaultTag == null) {
            defaultTag = getTagFromTagName(ReaderTag.TAG_TITLE_DEFAULT, ReaderTagType.DEFAULT, true);
        }
        return defaultTag;
    }

    public static @NonNull ReaderTag getDefaultTagFromDbOrCreateInMemory(
            @NonNull Context context,
            TagUpdateClientUtilsProvider clientUtilsProvider
    ) {
        // getDefaultTag() tries to get the default tag from reader db by tag endpoint or tag name.
        // In case it cannot get the default tag from db, it creates it in memory with createTagFromTagName
        ReaderTag tag = getDefaultTag();

        if (tag.isDefaultInMemoryTag()) {
            // if the tag was created in memory from createTagFromTagName
            // we need to set some fields as below before to use it
            tag.setTagTitle(context.getString(R.string.reader_following_display_name));
            tag.setTagDisplayName(context.getString(R.string.reader_following_display_name));

            String baseUrl = clientUtilsProvider.getTagUpdateEndpointURL();

            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            tag.setEndpoint(baseUrl + ReaderTag.FOLLOWING_PATH);
        }

        return tag;
    }

    /*
     * used when storing search results in the reader post table
     */
    public static ReaderTag getTagForSearchQuery(@NonNull String query) {
        String trimQuery = query != null ? query.trim() : "";
        String slug = ReaderUtils.sanitizeWithDashes(trimQuery);
        return new ReaderTag(slug, trimQuery, trimQuery, null, ReaderTagType.SEARCH);
    }

    public static Map<String, TagInfo> getDefaultTagInfo() {
        // Note that the following is the desired order in the tabs
        // (see usage in prependDefaults)
        Map<String, TagInfo> defaultTagInfo = new LinkedHashMap<>();

        defaultTagInfo.put(ReaderConstants.KEY_FOLLOWING, new TagInfo(ReaderTagType.DEFAULT, ReaderTag.FOLLOWING_PATH));
        defaultTagInfo.put(ReaderConstants.KEY_DISCOVER, new TagInfo(ReaderTagType.DEFAULT, ReaderTag.DISCOVER_PATH));
        defaultTagInfo.put(ReaderConstants.KEY_LIKES, new TagInfo(ReaderTagType.DEFAULT, ReaderTag.LIKED_PATH));
        defaultTagInfo.put(ReaderConstants.KEY_SAVED, new TagInfo(ReaderTagType.BOOKMARKED, ""));

        return defaultTagInfo;
    }

    private static boolean putIfAbsentDone(Map<String, ReaderTag> defaultTags, String key, ReaderTag tag) {
        boolean insertionDone = false;

        if (defaultTags.get(key) == null) {
            defaultTags.put(key, tag);
            insertionDone = true;
        }

        return insertionDone;
    }

    private static void prependDefaults(
            Map<String, ReaderTag> defaultTags,
            ReaderTagList orderedTagList,
            Map<String, TagInfo> defaultTagInfo
    ) {
        if (defaultTags.isEmpty()) return;

        List<String> reverseOrderedKeys = new ArrayList<>(defaultTagInfo.keySet());
        Collections.reverse(reverseOrderedKeys);

        for (String key : reverseOrderedKeys) {
            if (defaultTags.containsKey(key)) {
                ReaderTag tag = defaultTags.get(key);

                orderedTagList.add(0, tag);
            }
        }
    }

    private static boolean defaultTagFoundAndAdded(
            Map<String, TagInfo> defaultTagInfos,
            ReaderTag tag,
            Map<String, ReaderTag> defaultTags
    ) {
        boolean foundAndAdded = false;

        for (String key : defaultTagInfos.keySet()) {
            if (defaultTagInfos.get(key).isDesiredTag(tag)) {
                if (putIfAbsentDone(defaultTags, key, tag)) {
                    foundAndAdded = true;
                }
                break;
            }
        }

        return foundAndAdded;
    }

    public static ReaderTagList getOrderedTagsList(ReaderTagList tagList, Map<String, TagInfo> defaultTagInfos) {
        ReaderTagList orderedTagList = new ReaderTagList();
        Map<String, ReaderTag> defaultTags = new HashMap<>();

        for (ReaderTag tag : tagList) {
            if (defaultTagFoundAndAdded(defaultTagInfos, tag, defaultTags)) continue;

            orderedTagList.add(tag);
        }
        prependDefaults(defaultTags, orderedTagList, defaultTagInfos);

        return orderedTagList;
    }

    public static boolean isTagManagedInFollowingTab(
            ReaderTag tag,
            boolean isTopLevelReader,
            FilteredRecyclerView recyclerView
    ) {
        if (isTopLevelReader) {
            if (ReaderUtils.isDefaultInMemoryTag(tag)) {
                return true;
            }

            boolean isSpecialTag = tag != null
                                   &&
                                   (tag.isDiscover() || tag.isPostsILike() || tag.isBookmarked());

            boolean tabsInitializingNow = recyclerView != null && recyclerView.getCurrentFilter() == null;

            boolean tagIsFollowedSitesOrAFollowedTag = tag != null
                                                       && (
                                                               tag.isFollowedSites()
                                                               || tag.tagType == ReaderTagType.FOLLOWED
                                                       );

            if (isSpecialTag) {
                return false;
            } else if (tabsInitializingNow) {
                return tagIsFollowedSitesOrAFollowedTag;
            } else if (recyclerView != null && recyclerView.getCurrentFilter() instanceof ReaderTag) {
                if (recyclerView.isValidFilter(tag)) {
                    return tag.isFollowedSites();
                } else {
                    // If we reach here it means we are setting a followed tag or site in the Following tab
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return tag != null && tag.isFollowedSites();
        }
    }

    public static @NonNull ReaderTag getValidTagForSharedPrefs(
            @NonNull ReaderTag tag,
            boolean isTopLevelReader,
            FilteredRecyclerView recyclerView,
            @NonNull ReaderTag defaultTag
    ) {
        if (!isTopLevelReader) {
            return tag;
        }

        boolean isValidFilter = (recyclerView != null && recyclerView.isValidFilter(tag));
        boolean isSpecialTag = tag.isDiscover() || tag.isPostsILike() || tag.isBookmarked();
        if (!isSpecialTag && !isValidFilter && isTagManagedInFollowingTab(tag, isTopLevelReader, recyclerView)) {
            return defaultTag;
        }

        return tag;
    }

    public static boolean isDefaultInMemoryTag(ReaderTag tag) {
        return tag != null && tag.isDefaultInMemoryTag();
    }

    public static String getCommaSeparatedTagSlugs(ReaderTagList tags) {
        StringBuilder slugs = new StringBuilder();
        for (ReaderTag tag : tags) {
            if (slugs.length() > 0) {
                slugs.append(",");
            }
            final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagSlug());
            slugs.append(tagNameForApi);
        }
        return slugs.toString();
    }
}
