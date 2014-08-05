package org.wordpress.android.ui.reader.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderHtmlUtils {

    // regex for matching img tags in html content
    private static final Pattern IMG_PATTERN = Pattern.compile(
            "<img(\\s+.*?)(?:src\\s*=\\s*(?:'|\")(.*?)(?:'|\"))(.*?)/>",
            Pattern.DOTALL| Pattern.CASE_INSENSITIVE);

    // regex for matching class attributes in tags
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "class\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    // regex for matching src attributes in tags
    private static final Pattern SRC_PATTERN = Pattern.compile(
            "src\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    /*
     * called when a post doesn't have a featured image, searches post's content for an image that
     * may still be suitable as a featured image - only works with posts from wp blogs since it
     * checks for the existence of specific wp img classes
     */
    public static String findFeaturedImage(final String text) {
        if (text == null
                || !text.contains("<img ")
                || !text.contains("size-")) {
            return null;
        }

        // pick the class name we want to match, starting with the largest wp img class
        final String classToFind;
        if (text.contains("size-full")) {
            classToFind = "size-full";
        } else if (text.contains("size-large")) {
            classToFind = "size-large";
        } else if (text.contains("size-medium")) {
            classToFind = "size-medium";
        } else {
            return null;
        }

        Matcher imgMatcher = IMG_PATTERN.matcher(text);
        while (imgMatcher.find()) {
            String imgTag = text.substring(imgMatcher.start(), imgMatcher.end());
            String classAttr = getClassAttrValue(imgTag);
            if (classAttr != null && classAttr.contains(classToFind)) {
                return getSrcAttrValue(imgTag);
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

        Matcher matcher = CLASS_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "class=" and quotes from the result
            String attr = tag.substring(matcher.start(), matcher.end());
            return attr.substring(7, attr.length() - 1);
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

        Matcher matcher = SRC_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "src=" and quotes from the result
            String attr = tag.substring(matcher.start(), matcher.end());
            return attr.substring(5, attr.length() - 1);
        } else {
            return null;
        }
    }

}
