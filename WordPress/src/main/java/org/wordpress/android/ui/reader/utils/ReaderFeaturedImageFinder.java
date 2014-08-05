package org.wordpress.android.ui.reader.utils;

import android.net.Uri;

import org.wordpress.android.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * used when a post doesn't have a featured image assigned, searches post's content for an
 * image that may still be suitable as a featured image - only works with posts from wp
 * blogs since it checks for the existence of specific wp classes or attributes
 */
public class ReaderFeaturedImageFinder {
    private final String mContent;
    private static final int MIN_FEATURED_IMAGE_WIDTH = 500;

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

    public ReaderFeaturedImageFinder(final String contentOfPost) {
        mContent = contentOfPost;
    }

    public String getFeaturedImage() {
        if (mContent == null || !mContent.contains("<img ")) {
            return null;
        }

        // pick the class name we want to match, starting with the largest wp img class - if no
        // match is found we'll fall back to check the w= query param
        final String classToFind;
        if (mContent.contains("size-full")) {
            classToFind = "size-full";
        } else if (mContent.contains("size-large")) {
            classToFind = "size-large";
        } else if (mContent.contains("size-medium")) {
            classToFind = "size-medium";
        } else {
            classToFind = null;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imgTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
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
    private String getClassAttrValue(final String tag) {
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
}
