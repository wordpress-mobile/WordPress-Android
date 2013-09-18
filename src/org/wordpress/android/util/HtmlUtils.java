package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.TextUtils;

/**
 * Created by nbradbury on 7/9/13.
 */
public class HtmlUtils {

    /*
     * removes html from the passed string - this relies on Html.fromHtml which is very slow, so
     * don't use this where performance is important
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

        // if text doesn't contain any html entities, use regex to strip tags and be done
        if (!text.contains("&"))
            return text.replaceAll("<(.|\n)*?>","").trim();

        // text contains entities, so use regex to strip tags and convert entities in the result
        return StringUtils.unescapeHTML(text.replaceAll("<(.|\n)*?>", "").trim());
    }

    /*
     * convert html entities to actual Unicode characters - relies on commons apache lang
     */
    /*public static String unescapeHtml(final String text) {
        if (text==null)
            return "";
        return StringEscapeUtils.unescapeHtml(text);
    }*/

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
     * adapted from Html.escapeHtml(), which was added in API Level 16
     */
    public static String escapeHtml(final String text) {
        if (text==null)
            return "";
        StringBuilder out = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < length && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }
}
