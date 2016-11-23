package org.wordpress.android.ui.reader.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

public class ReaderUtils {

    public static String getResizedImageUrl(final String imageUrl, int width, int height, boolean isPrivate) {
        return getResizedImageUrl(imageUrl, width, height, isPrivate, PhotonUtils.Quality.MEDIUM);
    }
    public static String getResizedImageUrl(final String imageUrl,
                                            int width,
                                            int height,
                                            boolean isPrivate,
                                            PhotonUtils.Quality quality) {

        final String unescapedUrl = HtmlUtils.fastUnescapeHtml(imageUrl);
        if (isPrivate) {
            return getPrivateImageForDisplay(unescapedUrl, width, height);
        } else {
            return PhotonUtils.getPhotonImageUrl(unescapedUrl, width, height, quality);
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
                .replaceAll("&[^\\s]*;", "")            // remove html entities
                .replaceAll("[\\.\\s]+", "-")           // replace periods and whitespace with a dash
                .replaceAll("[^\\p{L}\\p{Nd}\\-]+", "") // remove remaining non-alphanum/non-dash chars (Unicode aware)
                .replaceAll("--", "-");                 // reduce double dashes potentially added above
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
                    return String.format(youAndMultiLikes, numLikes - 1);
            }
        } else {
            if (numLikes == 1) {
                return context.getString(R.string.reader_likes_one);
            } else {
                String likes = context.getString(R.string.reader_likes_multi);
                return String.format(likes, numLikes);
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
     * returns true if the reader should provide a "logged out" experience - no likes,
     * comments, or anything else that requires a wp.com account
     */
    public static boolean isLoggedOutReader() {
        return !AccountHelper.isSignedInWordPressDotCom();
    }

    /*
     * returns true if a ReaderPost and ReaderComment exist for the passed Ids
     */
    public static boolean postAndCommentExists(long blogId, long postId, long commentId) {
        return ReaderPostTable.postExists(blogId, postId) &&
                ReaderCommentTable.commentExists(blogId, postId, commentId);
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
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setBackgroundToRoundRipple(View view) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setBackgroundResource(R.drawable.ripple_oval);
        }
    }

    /*
     * returns a tag object from the passed tag name - first checks for it in the tag db
     * (so we can also get its title & endpoint), returns a new tag if that fails
     */
    public static ReaderTag getTagFromTagName(String tagName, ReaderTagType tagType) {
        ReaderTag tag = ReaderTagTable.getTag(tagName, tagType);
        if (tag != null) {
            return tag;
        } else {
            return createTagFromTagName(tagName, tagType);
        }
    }

    public static ReaderTag createTagFromTagName(String tagName, ReaderTagType tagType) {
        String tagSlug = sanitizeWithDashes(tagName).toLowerCase();
        String tagDisplayName = tagType == ReaderTagType.DEFAULT ? tagName : tagSlug;
        return new ReaderTag(tagSlug, tagDisplayName, tagName, null, tagType);
    }

    /*
     * returns the default tag, which is the one selected by default in the reader when
     * the user hasn't already chosen one
     */
    public static ReaderTag getDefaultTag() {
        return getTagFromTagName(ReaderTag.TAG_TITLE_DEFAULT, ReaderTagType.DEFAULT);
    }


}
