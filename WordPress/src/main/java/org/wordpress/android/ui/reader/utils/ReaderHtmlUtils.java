package org.wordpress.android.ui.reader.utils;

import android.net.Uri;
import android.text.TextUtils;

import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * html-related routines specific to the Reader
 **/

public class ReaderHtmlUtils {

    // regex for matching img tags in html content
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img(\\s+.*?)(?:src\\s*=\\s*(?:'|\")(.*?)(?:'|\"))(.*?)/>",
            Pattern.DOTALL| Pattern.CASE_INSENSITIVE);

    // regex for matching class attributes in tags
    private static final Pattern CLASS_ATTR_PATTERN = Pattern.compile(
            "class\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    // regex for matching src attributes in tags
    private static final Pattern SRC_ATTR_PATTERN = Pattern.compile(
            "src\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    /*
     * called when a post doesn't have a featured image, searches post's content for an image that
     * may still be suitable as a featured image - only works with posts from wp blogs since it
     * checks for the existence of specific wp classes or attributes
     */
    private static final int MIN_FEATURED_IMAGE_WIDTH = 500;
    public static String findFeaturedImage(final String text) {
        if (text == null || !text.contains("<img ")) {
            return null;
        }

        // pick the class name we want to match, starting with the largest wp img class - if no
        // match is found we'll fall back to check the w= query param
        final String classToFind;
        if (text.contains("size-full")) {
            classToFind = "size-full";
        } else if (text.contains("size-large")) {
            classToFind = "size-large";
        } else if (text.contains("size-medium")) {
            classToFind = "size-medium";
        } else {
            classToFind = null;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(text);
        while (imgMatcher.find()) {
            String imgTag = text.substring(imgMatcher.start(), imgMatcher.end());
            if (classToFind != null) {
                String classAttr = getClassAttrValue(imgTag);
                if (classAttr != null && classAttr.contains(classToFind)) {
                    return getSrcAttrValue(imgTag);
                }
            } else {
                String imageUrl = getSrcAttrValue(imgTag);
                if (getIntQueryParam(imageUrl, "w") >= MIN_FEATURED_IMAGE_WIDTH) {
                    return imageUrl;
                }
            }
        }

        return null;
    }

    /*
     * returns the value from the class attribute in the passed html tag
     */
    private static String getClassAttrValue(final String tag) {
        if (tag == null) {
            return null;
        }

        Matcher matcher = CLASS_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "class=" and quotes from the result
            return tag.substring(matcher.start() + 7, matcher.end() - 1);
        } else {
            return null;
        }
    }

    /*
     * returns the value from the src attribute in the passed html tag
     */
    private static String getSrcAttrValue(final String tag) {
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
    private static int getIntQueryParam(final String url, final String param) {
        if (url == null
                || param == null
                || !url.startsWith("http")
                || !url.contains(param + "=")) {
            return 0;
        }
        return StringUtils.stringToInt(Uri.parse(url).getQueryParameter(param));
    }

    /*
     *  returns the actual image url from a Freshly Pressed featured image url - this is necessary
     *  because the featured image returned by the API is often an ImagePress url that formats the
     *  actual image url for a specific size, and we want to define the size in the app when the
     *  image is requested. example of an ImagePress featured image url from a freshly-pressed post:
     *  https://s1.wp.com/imgpress?crop=0px%2C0px%2C252px%2C160px&url=https%3A%2F%2Fs2.wp.com%2Fimgpress%3Fw%3D252%26url%3Dhttp%253A%252F%252Fmostlybrightideas.files.wordpress.com%252F2013%252F08%252Ftablet.png&unsharpmask=80,0.5,3
     */
    public static String getImageUrlFromFeaturedImageUrl(final String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            return null;
        }

        // if this is an mshots image, return the actual url without the query string (?h=n&w=n),
        // and change it from https: to http: so it can be cached (it's only https because it's
        // being returned by an authenticated REST endpoint - these images are found only in
        // FP posts so they don't require https)
        if (PhotonUtils.isMshotsUrl(imageUrl)) {
            return UrlUtils.removeQuery(imageUrl).replaceFirst("https", "http");
        }

        if (imageUrl.contains("imgpress")) {
            // parse the url parameter
            String actualImageUrl = Uri.parse(imageUrl).getQueryParameter("url");
            if (actualImageUrl == null) {
                return imageUrl;
            }

            // at this point the imageUrl may still be an ImagePress url, so check the url param again (see above example)
            if (actualImageUrl.contains("url=")) {
                return Uri.parse(actualImageUrl).getQueryParameter("url");
            } else {
                return actualImageUrl;
            }
        }

        // for all other featured images, return the passed url w/o the query string (since the query string
        // often contains Photon sizing params that we don't want here)
        int pos = imageUrl.lastIndexOf("?");
        if (pos == -1) {
            return imageUrl;
        }

        return imageUrl.substring(0, pos);
    }

}
