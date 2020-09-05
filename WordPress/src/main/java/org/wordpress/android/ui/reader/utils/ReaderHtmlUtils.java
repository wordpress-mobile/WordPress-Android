package org.wordpress.android.ui.reader.utils;

import android.net.Uri;

import androidx.annotation.Nullable;

import org.wordpress.android.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderHtmlUtils {
    public interface HtmlScannerListener {
        void onTagFound(String tag, String src);
    }

    // regex for matching oriwidth attributes in tags
    private static final Pattern ORIGINAL_WIDTH_ATTR_PATTERN = Pattern.compile(
            "data-orig-size\\s*=\\s*['\"](.*?),.*?['\"]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern ORIGINAL_HEIGHT_ATTR_PATTERN = Pattern.compile(
            "data-orig-size\\s*=\\s*['\"].*?,(.*?)['\"]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // regex for matching width attributes in tags
    private static final Pattern WIDTH_ATTR_PATTERN = Pattern.compile(
            "width\\s*=\\s*['\"](.*?)['\"]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // regex for matching height attributes in tags
    private static final Pattern HEIGHT_ATTR_PATTERN = Pattern.compile(
            "height\\s*=\\s*['\"](.*?)['\"]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // regex for matching class attributes in tags
    private static final Pattern CLASS_ATTR_PATTERN = Pattern.compile(
            "class\\s*=\\s*['\"](.*?)['\"]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static final Pattern SRCSET_ATTR_PATTERN = Pattern.compile(
            "srcset\\s*=\\s*['\"](.*?)['\"]",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // Matches pairs of URLs and widths inside a srcset tag, e.g.:
    // <URL1> 600w, <URL2> 800w -> (<URL1>, 600) and (<URL2>, 800)
    public static final Pattern SRCSET_INNER_PATTERN = Pattern.compile(
            "(\\S*?)\\s+(\\d*)w,?\\s*?",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /*
    * returns the integer value from the data-orig-size attribute in the passed html tag
    */
    public static int getOriginalWidthAttrValue(final String tag) {
        if (tag == null) {
            return 0;
        }

        Matcher matcher = ORIGINAL_WIDTH_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            return StringUtils.stringToInt(matcher.group(1), 0);
        } else {
            return 0;
        }
    }

    public static int getOriginalHeightAttrValue(final String tag) {
        if (tag == null) {
            return 0;
        }

        Matcher matcher = ORIGINAL_HEIGHT_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            return StringUtils.stringToInt(matcher.group(1), 0);
        } else {
            return 0;
        }
    }

    /*
    * returns the integer value from the width attribute in the passed html tag
    */
    public static int getWidthAttrValue(final String tag) {
        if (tag == null) {
            return 0;
        }

        Matcher matcher = WIDTH_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            return StringUtils.stringToInt(matcher.group(1), 0);
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
            return StringUtils.stringToInt(matcher.group(1), 0);
        } else {
            return 0;
        }
    }

    /*
     * returns the value from class src attribute in the passed html tag
     */
    public static String getClassAttrValue(final String tag) {
        if (tag == null) {
            return null;
        }

        Matcher matcher = CLASS_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    /*
     * returns the integer value of the passed query param in the passed url - returns zero
     * if the url is invalid, or the param doesn't exist, or the param value could not be
     * converted to an int
     */
    public static int getIntQueryParam(final String url,
                                       @SuppressWarnings("SameParameterValue") final String param) {
        if (url == null
            || param == null
            || !url.startsWith("http")
            || !url.contains(param + "=")) {
            return 0;
        }
        return StringUtils.stringToInt(Uri.parse(url).getQueryParameter(param));
    }

    /*
     * Extracts the srcset attribute from the given [tag], and returns the smallest image
     * larger than [minWidth].
     * Returns null if the srcset attribute is not present.
     */
    @Nullable public static String getSrcsetImageForTag(final String tag, final int minWidth) {
        if (tag == null) {
            return null;
        }

        Matcher matcher = SRCSET_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            String srcsetBody = matcher.group(1);
            Matcher innerMatcher = SRCSET_INNER_PATTERN.matcher(srcsetBody);
            int bestWidth = 0;
            String bestImageUrl = null;
            while (innerMatcher.find()) {
                int currentWidth = StringUtils.stringToInt(innerMatcher.group(2));
                // Only interested in images wider than minWidth
                if (currentWidth > minWidth) {
                    // Only keep currentWidth if it's closer to minWidth than the current bestWidth
                    if (bestWidth == 0 || currentWidth < bestWidth) {
                        bestWidth = currentWidth;
                        bestImageUrl = innerMatcher.group(1);
                    }
                }
            }
            return bestImageUrl;
        } else {
            return null;
        }
    }
}
