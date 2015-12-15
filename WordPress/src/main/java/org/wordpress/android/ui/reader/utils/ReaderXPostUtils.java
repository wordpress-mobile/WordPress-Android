package org.wordpress.android.ui.reader.utils;

import android.text.Html;
import android.text.Spanned;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.UrlUtils;

/**
 * Reader cross-post utility routines
 */
public class ReaderXPostUtils {
// TODO: need to handle comment xposts
    /*
     * returns the title to display for this xpost, which is simply the post's title
     * without the "X-post: " prefix
     */
    public static String getXPostTitle(ReaderPost post) {
        String title = post.getTitle();
        if (title.startsWith("X-post: ")) {
            return title.substring(8);
        } else {
            return title;
        }
    }

    /*
     * returns the subtitle to display for this xpost - note that this doesn't
     * need to be localized due to the intended audience
     * ex: "Nick cross-posted from +blog1 to +blog2"
     */
    public static Spanned getXPostSubtitleHtml(ReaderPost post) {
        // origin site name can be extracted from the excerpt,
        // ex: "<p>X-post from +blog2: I have a request..."
        String fromSiteName;
        String excerpt = post.getExcerpt();
        int plusPos = excerpt.indexOf("+");
        int colonPos = excerpt.indexOf(":", plusPos);
        if (plusPos > 0 && colonPos > 0) {
            fromSiteName = excerpt.substring(plusPos, colonPos);
        } else {
            fromSiteName = "(unknown)";
        }

        // destination site name is the subdomain of the blog url
        String toSiteName = "+" + UrlUtils.getDomainFromUrl(post.getBlogUrl());
        if (toSiteName.contains(".")) {
            toSiteName = toSiteName.substring(0, toSiteName.indexOf("."));
        }

        String subtitle = String.format(
                "%1$s cross-posted from %2$s to %3$s",
                makeBold(post.getAuthorName()),
                fromSiteName,
                toSiteName);
        return Html.fromHtml(subtitle);
    }

    private static String makeBold(String s) {
        return "<strong>" + s + "</strong>";
    }
}
