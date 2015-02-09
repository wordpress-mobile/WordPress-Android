package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.QuoteSpan;

import org.apache.commons.lang.StringEscapeUtils;
import org.wordpress.android.util.helpers.WPHtmlTagHandler;
import org.wordpress.android.util.helpers.WPImageGetter;
import org.wordpress.android.util.helpers.WPQuoteSpan;

public class HtmlUtils {
    /*
     * removes html from the passed string - relies on Html.fromHtml which handles invalid HTML,
     * but it's very slow, so avoid using this where performance is important
     */
    public static String stripHtml(final String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        return Html.fromHtml(text).toString().trim();
    }

    /*
     * this is much faster than stripHtml() but should only be used when we know the html is valid
     * since the regex will be unpredictable with invalid html
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
        return trimStart(fastUnescapeHtml(str.replaceAll("<(.|\n)*?>", "")));
    }

    /*
     * same as apache.commons.lang.StringUtils.stripStart() but also removes non-breaking
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

    /*
     * convert html entities to actual Unicode characters - relies on commons apache lang
     */
    public static String fastUnescapeHtml(final String text) {
        if (text == null || !text.contains("&")) {
            return text;
        }
        return StringEscapeUtils.unescapeHtml(text);
    }

    /*
     * converts an R.color.xxx resource to an HTML hex color
     */
    public static String colorResToHtmlColor(Context context, int resId) {
        try {
            return String.format("#%06X", 0xFFFFFF & context.getResources().getColor(resId));
        } catch (Resources.NotFoundException e) {
            return "#000000";
        }
    }

    /*
     * remove <script>..</script> blocks from the passed string - added to project after noticing
     * comments on posts that use the "Sociable" plugin ( http://wordpress.org/plugins/sociable/ )
     * may have a script block which contains <!--//--> followed by a CDATA section followed by <!]]>,
     * all of which will show up if we don't strip it here (example: http://cl.ly/image/0J0N3z3h1i04 )
     * first seen at http://houseofgeekery.com/2013/11/03/13-terrible-x-men-we-wont-see-in-the-movies/
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
     * an alternative to Html.fromHtml() supporting <ul>, <ol>, <blockquote> tags and replacing EmoticonsUtils with Emojis
     */
    public static SpannableStringBuilder fromHtml(String source, WPImageGetter wpImageGetter) {
        SpannableStringBuilder html;
        try {
            html = (SpannableStringBuilder) Html.fromHtml(source, wpImageGetter, new WPHtmlTagHandler());
        } catch (RuntimeException runtimeException) {
            // In case our tag handler fails
            html = (SpannableStringBuilder) Html.fromHtml(source, wpImageGetter, null);
        }
        EmoticonsUtils.replaceEmoticonsWithEmoji(html);
        QuoteSpan spans[] = html.getSpans(0, html.length(), QuoteSpan.class);
        for (QuoteSpan span : spans) {
            html.setSpan(new WPQuoteSpan(), html.getSpanStart(span), html.getSpanEnd(span), html.getSpanFlags(span));
            html.setSpan(new ForegroundColorSpan(0xFF666666), html.getSpanStart(span), html.getSpanEnd(span),
                    html.getSpanFlags(span));
            html.removeSpan(span);
        }
        return html;
    }

    public static Spanned fromHtml(String source) {
        return fromHtml(source, null);
    }
}
