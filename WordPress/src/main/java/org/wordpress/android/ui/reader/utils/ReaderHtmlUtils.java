package org.wordpress.android.ui.reader.utils;

import android.net.Uri;

import org.wordpress.android.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderHtmlUtils {

    public static interface HtmlScannerListener {
        public void onTagFound(String tag, String src, int start, int end);
        public void onScanCompleted();
    }

    // regex for matching width attributes in tags
    private static final Pattern WIDTH_ATTR_PATTERN = Pattern.compile(
            "width\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    // regex for matching height attributes in tags
    private static final Pattern HEIGHT_ATTR_PATTERN = Pattern.compile(
            "height\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    // regex for matching src attributes in tags
    private static final Pattern SRC_ATTR_PATTERN = Pattern.compile(
            "src\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);


    /*
    * returns the integer value from the width attribute in the passed html tag
    */
    public static int getWidthAttrValue(final String tag) {
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

    public static int getHeightAttrValue(final String tag) {
        if (tag == null) {
            return 0;
        }
        Matcher matcher = HEIGHT_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            return StringUtils.stringToInt(tag.substring(matcher.start() + 8, matcher.end() - 1), 0);
        } else {
            return 0;
        }
    }

    /*
     * returns the value from the src attribute in the passed html tag
     */
    public static String getSrcAttrValue(final String tag) {
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
    public static int getIntQueryParam(final String url, final String param) {
        if (url == null
                || param == null
                || !url.startsWith("http")
                || !url.contains(param + "=")) {
            return 0;
        }
        return StringUtils.stringToInt(Uri.parse(url).getQueryParameter(param));
    }
}
