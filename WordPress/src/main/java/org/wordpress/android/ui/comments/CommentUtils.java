package org.wordpress.android.ui.comments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.text.util.Linkify;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.Emoticons;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.WPImageGetter;

public class CommentUtils {
    /*
     * displays comment text as html, including retrieving images
     */
    public static CharSequence displayHtmlComment(TextView textView, String content, int maxImageSize) {
        if (textView == null)
            return null;

        if (content == null) {
            textView.setText(null);
            return null;
        }

        // skip performance hit of html conversion if content doesn't contain html
        if (!content.contains("<") && !content.contains("&")) {
            content = content.trim();
            textView.setText(content);
            // make sure unnamed links are clickable
            if (content.contains("://")) {
                Linkify.addLinks(textView, Linkify.WEB_URLS);
            }

            return content;
        }

        // convert emoticons first (otherwise they'll be downloaded)
        content = Emoticons.replaceEmoticonsWithEmoji(content);

        // now convert to HTML with an image getter that enforces a max image size
        final Spanned html;
        if (maxImageSize > 0 && content.contains("<img")) {
            Drawable loading = textView.getContext().getResources().getDrawable(R.drawable.remote_image);
            Drawable failed = textView.getContext().getResources().getDrawable(R.drawable.remote_failed);
            html = HtmlUtils.fromHtml(content, new WPImageGetter(textView, maxImageSize, WordPress.imageLoader,
                    loading, failed));
        } else {
            html = HtmlUtils.fromHtml(content);
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

        textView.setText(source);

        return source;
    }

    public static void wrapTextAroundAvatar(TextView textView, CharSequence commentText, int textOffsetX, int textOffsetY) {
        if (textOffsetX > 0) {
            float textLineHeight = textView.getPaint().getTextSize();

            // Set the span according to the number of lines and width of the image
            int lines = Math.round(textOffsetY / textLineHeight);
            //For an html text you can use this line: SpannableStringBuilder ss = (SpannableStringBuilder)Html.fromHtml(text);
            SpannableString ss = new SpannableString(commentText.toString().trim());
            ss.setSpan(new TextWrappingLeadingMarginSpan(lines, textOffsetX), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(ss);
        } else {
            textView.setText(commentText);
        }
    }

    private static class TextWrappingLeadingMarginSpan implements LeadingMarginSpan.LeadingMarginSpan2 {
        private int margin;
        private int lines;

        public TextWrappingLeadingMarginSpan(int lines, int margin) {
            this.margin = margin;
            this.lines = lines;
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return first ? margin : 0;
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {

        }

        @Override
        public int getLeadingMarginLineCount() {
            return lines;
        }
    }
}
