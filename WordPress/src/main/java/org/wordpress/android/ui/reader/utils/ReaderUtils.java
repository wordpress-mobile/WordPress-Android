package org.wordpress.android.ui.reader.utils;

import android.content.Context;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.PhotonUtils;
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
        if (isPrivate) {
            return getPrivateImageForDisplay(imageUrl, width, height);
        } else {
            return PhotonUtils.getPhotonImageUrl(imageUrl, width, height, quality);
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
                    return context.getString(R.string.reader_likes_you_and_multi, numLikes - 1);
            }
        } else {
            return (numLikes == 1 ?
                    context.getString(R.string.reader_likes_one) : context.getString(R.string.reader_likes_multi, numLikes));
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
}
