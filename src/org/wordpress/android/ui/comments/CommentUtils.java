package org.wordpress.android.ui.comments;

import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.widget.TextView;

import org.wordpress.android.util.Emoticons;
import org.wordpress.android.util.WPImageGetter;

public class CommentUtils {
    /*
     * displays comment text as html, including retrieving images
     */
    public static void displayHtmlComment(TextView textView, String content, int maxImageSize) {
        if (textView == null)
            return;

        if (content == null) {
            textView.setText(null);
            return;
        }

        // skip performance hit of html conversion if content doesn't contain html
        if (!content.contains("<") && !content.contains("&")) {
            textView.setText(content.trim());
            // make sure unnamed links are clickable
            if (content.contains("://"))
                Linkify.addLinks(textView, Linkify.WEB_URLS);
            return;
        }

        // convert emoticons first (otherwise they'll be downloaded)
        content = Emoticons.replaceEmoticonsWithEmoji(content);

        // now convert to HTML with an image getter that enforces a max image size
        final Spanned html;
        if (maxImageSize > 0 && content.contains("<img")) {
            html = Html.fromHtml(content, new WPImageGetter(textView, maxImageSize), null);
        } else {
            html = Html.fromHtml(content);
        }

        // remove extra \n\n added by Html.convert()
        CharSequence source = html;
        int start = 0;
        int end = source.length();
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }

        textView.setText(source.subSequence(start, end));
    }
}
