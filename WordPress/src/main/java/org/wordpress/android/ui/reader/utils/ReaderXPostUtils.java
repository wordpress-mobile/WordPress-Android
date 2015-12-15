package org.wordpress.android.ui.reader.utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;

import org.wordpress.android.models.ReaderPost;

/**
 * Reader cross-post utility routines
 */

public class ReaderXPostUtils {

    // note that these strings don't need to be localized due to the intended audience
    private static final String UNKNOWN_SITE = "(unknown)";
    private static final String FMT_SITE_XPOST = "%1$s cross-posted from %2$s to %3$s";
    private static final String FMT_COMMENT_XPOST = "%1$s left a comment on %2$s, cross-posted to %3$s";

    /*
     * returns the title to display for this xpost, which is simply the post's title
     * without the "X-post: " prefix
     */
    public static String getXPostTitle(@NonNull ReaderPost post) {
        if (post.getTitle().startsWith("X-post: ")) {
            return post.getTitle().substring(8);
        } else {
            return post.getTitle();
        }
    }

    /*
     * returns the subtitle to display for this xpost
     * ex: "Nick cross-posted from +blog1 to +blog2"
     * ex: "Nick left a comment on +blog1, cross-posted to +blog2"
     */
    public static Spanned getXPostSubtitleHtml(@NonNull ReaderPost post) {
        boolean isCommentXPost = post.getExcerpt().startsWith("X-comment");

        // origin site name can be extracted from the excerpt,
        // ex: "<p>X-post from +blog2: I have a request..."
        String fromSiteName;
        String excerpt = post.getExcerpt();
        int plusPos = excerpt.indexOf("+");
        int colonPos = excerpt.indexOf(":", plusPos);
        if (plusPos > 0 && colonPos > 0) {
            fromSiteName = excerpt.substring(plusPos, colonPos);
        } else {
            fromSiteName = UNKNOWN_SITE;
        }

        // destination site name is the subdomain of the blog url
        String toSiteName = "+" + getSubdomain(post.getBlogUrl());

        String subtitle = String.format(
                isCommentXPost ? FMT_COMMENT_XPOST : FMT_SITE_XPOST,
                makeBold(post.getAuthorName()),
                fromSiteName,
                toSiteName);
        return Html.fromHtml(subtitle);
    }

    private static String makeBold(String s) {
        return "<strong>" + s + "</strong>";
    }

    private static String getSubdomain(String url) {
        Uri uri = Uri.parse(url);
        String domain = uri.getHost();
        if (domain == null || !domain.contains(".")) {
            return UNKNOWN_SITE;
        }

        return domain.substring(0, domain.indexOf("."));
    }
}
