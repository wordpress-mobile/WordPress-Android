package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.TextUtils;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Created by nbradbury on 7/9/13.
 */
public class HtmlUtils {

    /*
     * removes html from the passed string - relies on Html.fromHtml which handles invalid HTML,
     * but it's very slow, so avoid using this where performance is important
     */
    public static String stripHtml(final String text) {
        if (TextUtils.isEmpty(text))
            return "";
        return Html.fromHtml(text).toString().trim();
    }

    /*
     * this is much faster than stripHtml() but should only be used when we know the html is valid
     * since the regex will be unpredictable with invalid html
     */
    public static String fastStripHtml(String text) {
        if (TextUtils.isEmpty(text))
            return text;

        // insert a line break before P tags unless the only one is at the start
        if (text.lastIndexOf("<p") > 0)
            text = text.replaceAll("<p(.|\n)*?>","\n<p>");

        // convert BR tags to line breaks
        if (text.contains("<br"))
            text = text.replaceAll("<br(.|\n)*?>","\n");

        // use regex to strip tags, then convert entities in the result
        return fastUnescapeHtml(text.replaceAll("<(.|\n)*?>", "").trim());
    }


    /*
     * convert html entities to actual Unicode characters - relies on commons apache lang
     */
    protected static String fastUnescapeHtml(final String text) {
        if (text==null || !text.contains("&"))
            return text;
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

}
