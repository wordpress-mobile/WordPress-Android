package org.wordpress.android.ui.notifications;

import android.text.Html;
import android.text.SpannableStringBuilder;

import org.wordpress.android.util.WPHtmlTagHandler;

import junit.framework.TestCase;

public class NotesParseTest extends TestCase {

    public void testParagraphInListItem() {
        String text = "<li><p>Paragraph in li</p></li>";
        SpannableStringBuilder html = (SpannableStringBuilder) Html.fromHtml(text, null, new WPHtmlTagHandler());
        // if this didn't throw a RuntimeException we're ok
        assert(true);
    }

}