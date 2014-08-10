package org.wordpress.android.ui.reader.utils;

import android.net.Uri;

import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * used when a post doesn't have a featured image assigned, searches post's content
 * for an image that may be large enough to be suitable as a featured image
 * USAGE: new ReaderFeaturedImageFinder(content).getBestFeaturedImage()
 */
public class ReaderFeaturedImageFinder {
    private final String mContent;
    private static final int MIN_FEATURED_IMAGE_WIDTH = 500;

    // regex for matching img tags in html content
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img(\\s+.*?)(?:src\\s*=\\s*(?:'|\")(.*?)(?:'|\"))(.*?)/>",
            Pattern.DOTALL| Pattern.CASE_INSENSITIVE);

    // regex for matching width attributes in tags
    private static final Pattern WIDTH_ATTR_PATTERN = Pattern.compile(
            "width\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    // regex for matching src attributes in tags
    private static final Pattern SRC_ATTR_PATTERN = Pattern.compile(
            "src\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    public ReaderFeaturedImageFinder(final String contentOfPost) {
        mContent = contentOfPost;
    }

    /*
     * returns the url of the largest image based on the w= query param and/or the width
     * attribute, provided that the width is at least MIN_FEATURED_IMAGE_WIDTH
     */
    public String getBestFeaturedImage() {
        if (mContent == null || !mContent.contains("<img ")) {
            return null;
        }

        String currentImageUrl = null;
        int currentMaxWidth = MIN_FEATURED_IMAGE_WIDTH;

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imgTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
            String imageUrl = getSrcAttrValue(imgTag);

            int width = Math.max(getWidthAttrValue(imgTag), getIntQueryParam(imageUrl, "w"));
            if (width > currentMaxWidth) {
                currentImageUrl = imageUrl;
                currentMaxWidth = width;
            }
        }

        return currentImageUrl;
    }

    /*
     * returns the integer value from the width attribute in the passed html tag
     */
    private int getWidthAttrValue(final String tag) {
        if (tag == null) {
            return 0;
        }

        Matcher matcher = WIDTH_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "width=" and quotes from the result
            return StringUtils.stringToInt(tag.substring(matcher.start() + 7, matcher.end() - 1), 0);
        } else {
            return 0;
        }
    }

    /*
     * returns the value from the src attribute in the passed html tag
     */
    private String getSrcAttrValue(final String tag) {
        if (tag == null) {
            return null;
        }

        Matcher matcher = SRC_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "src=" and quotes from the result
            return tag.substring(matcher.start() + 5, matcher.end() - 1);
        } else {
            return null;
        }
    }

    /*
     * returns the integer value of the passed query param in the passed url - returns zero
     * if the url is invalid, or the param doesn't exist, or the param value could not be
     * converted to an int
     */
    private int getIntQueryParam(final String url, final String param) {
        if (url == null
                || param == null
                || !url.startsWith("http")
                || !url.contains(param + "=")) {
            return 0;
        }
        return StringUtils.stringToInt(Uri.parse(url).getQueryParameter(param));
    }

    /*
     * returns the actual image url from a Freshly Pressed featured image url - this is necessary because the
     * featured image returned by the API is often an ImagePress url that formats the actual image url for a
     * specific size, and we want to define the size in the app when the image is requested.
     * here's an example of an ImagePress featured image url from a freshly-pressed post:
     *   https://s1.wp.com/imgpress?crop=0px%2C0px%2C252px%2C160px
     *          &url=https%3A%2F%2Fs2.wp.com%2Fimgpress%3Fw%3D252%26url%3Dhttp%253A%252F%252Fmostlybrightideas.files.wordpress.com%252F2013%252F08%252Ftablet.png
     *          &unsharpmask=80,0.5,3
     */
    public static String getImageUrlFromFPFeaturedImageUrl(final String url) {
        if (url == null || !url.startsWith("http")) {
            return null;
        } else if (url.contains("/imgpress")) {
            return getImageUrlFromImagepressUrl(url);
        } else if (PhotonUtils.isMshotsUrl(url)) {
            // if this is an mshots image, return the actual url without the query string (?h=n&w=n),
            // and change it from https to http so it can be cached
            return UrlUtils.removeQuery(url).replaceFirst("https", "http");
        } else {
            return url;
        }
    }

    private static String getImageUrlFromImagepressUrl(final String url) {
        if (url == null) {
            return null;
        }

        String imageUrl = Uri.parse(url).getQueryParameter("url");
        if (imageUrl != null && imageUrl.contains("url=")) {
            // url still contains a url= param, process it again
            return getImageUrlFromImagepressUrl(imageUrl);
        } else {
            return UrlUtils.removeQuery(imageUrl);
        }
    }
}
