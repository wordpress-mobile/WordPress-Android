package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.QuoteSpan;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.util.helpers.WPHtmlTagHandler;
import org.wordpress.android.util.helpers.WPQuoteSpan;

import static org.wordpress.android.util.AppLog.T.UTILS;

public class HtmlUtils {
    /**
     * Removes html from the passed string - relies on Html.fromHtml which handles invalid HTML,
     * but it's very slow, so avoid using this where performance is important
     * @param text String containing html
     * @return String without HTML
     */
    public static String stripHtml(final String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        return Html.fromHtml(text).toString().trim();
    }

    /**
     * This is much faster than stripHtml() but should only be used when we know the html is valid
     * since the regex will be unpredictable with invalid html
     * @param str String containing only valid html
     * @return String without HTML
     */
    public static String fastStripHtml(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        // insert a line break before P tags unless the only one is at the start
        if (str.lastIndexOf("<p") > 0) {
            str = str.replaceAll("<p(.|\n)*?>", "\n<p>");
        }

        // convert BR tags to line breaks
        if (str.contains("<br")) {
            str = str.replaceAll("<br(.|\n)*?>", "\n");
        }

        // use regex to strip tags, then convert entities in the result
        return trimStart(StringEscapeUtils.unescapeHtml4(str.replaceAll("<(.|\n)*?>", "")));
    }

    /*
     * Same as apache.commons.lang.StringUtils.stripStart() but also removes non-breaking
     * space (160) chars
     */
    private static String trimStart(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return "";
        }
        int start = 0;
        while (start != strLen && (Character.isWhitespace(str.charAt(start)) || str.charAt(start) == 160)) {
            start++;
        }
        return str.substring(start);
    }

    /**
     * Converts an R.color.xxx resource to an HTML hex color
     * @param context Android Context
     * @param resId Android R.color.xxx
     * @return A String HTML hex color code
     */
    public static String colorResToHtmlColor(Context context, int resId) {
        try {
            return String.format("#%06X", 0xFFFFFF & context.getResources().getColor(resId));
        } catch (Resources.NotFoundException e) {
            return "#000000";
        }
    }

    /**
     * Remove {@code <script>..</script>} blocks from the passed string - added to project after noticing
     * comments on posts that use the "Sociable" plugin ( http://wordpress.org/plugins/sociable/ )
     * may have a script block which contains {@code <!--//-->} followed by a CDATA section followed by {@code <!]]>,}
     * all of which will show up if we don't strip it here.
     * @see <a href="http://wordpress.org/plugins/sociable/">Wordpress Sociable Plugin</a>
     * @return String without {@code <script>..</script>}, {@code <!--//-->} blocks followed by a CDATA section
     * followed by {@code <!]]>,}
     * @param text String containing script tags
     */
    public static String stripScript(final String text) {
        if (text == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(text);
        int start = sb.indexOf("<script");

        while (start > -1) {
            int end = sb.indexOf("</script>", start);
            if (end == -1) {
                return sb.toString();
            }
            sb.delete(start, end + 9);
            start = sb.indexOf("<script", start);
        }

        return sb.toString();
    }

    /**
     * An alternative to Html.fromHtml() supporting {@code <ul>}, {@code <ol>}, {@code <blockquote>}
     * tags and replacing EmoticonsUtils with Emojis
     * @param source
     * @param imageGetter
     */
    public static SpannableStringBuilder fromHtml(String source, ImageGetter imageGetter) {
        source = replaceListTagsWithCustomTags(source);
        SpannableStringBuilder html;
        try {
            html = (SpannableStringBuilder) Html.fromHtml(source, imageGetter, new WPHtmlTagHandler());
        } catch (RuntimeException runtimeException) {
            // In case our tag handler fails
            try {
                html = (SpannableStringBuilder) Html.fromHtml(source, imageGetter, null);
            } catch (IllegalArgumentException illegalArgumentException) {
                // In case the html is missing a required parameter (for example: "src" missing from img)
                html = new SpannableStringBuilder("");
                AppLog.w(UTILS, "Could not parse html");
            }
        }

        EmoticonsUtils.replaceEmoticonsWithEmoji(html);
        QuoteSpan[] spans = html.getSpans(0, html.length(), QuoteSpan.class);
        for (QuoteSpan span : spans) {
            html.setSpan(new WPQuoteSpan(), html.getSpanStart(span), html.getSpanEnd(span), html.getSpanFlags(span));
            html.setSpan(new ForegroundColorSpan(0xFF666666), html.getSpanStart(span), html.getSpanEnd(span),
                         html.getSpanFlags(span));
            html.removeSpan(span);
        }
        return html;
    }

    private static String replaceListTagsWithCustomTags(String source) {
        return source.replace("<ul", "<WPUL")
                     .replace("</ul>", "</WPUL>")
                     .replace("<ol", "<WPOL")
                     .replace("</ol>", "</WPOL>")
                     .replace("<li", "<WPLI")
                     .replace("</li>", "</WPLI>");
    }

    public static Spanned fromHtml(String source) {
        return fromHtml(source, null);
    }
}
