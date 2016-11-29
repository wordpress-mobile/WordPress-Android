package org.wordpress.android.editor;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class HtmlStyleUtilsTest {

    @Test
    public void testBulkStyling() {
        // -- Test bulk styling
        Spannable content = new SpannableStringBuilder("text <b>bold</b> &amp; <!--a comment--> <a href=\"website\">link</a>");
        HtmlStyleUtils.styleHtmlForDisplay(content);

        assertEquals(0, content.getSpans(0, 5, CharacterStyle.class).length); // 'text '

        assertEquals(1, content.getSpans(5, 8, ForegroundColorSpan.class).length); // '<b>'

        assertEquals(1, content.getSpans(12, 16, ForegroundColorSpan.class).length); // '</b>'

        assertEquals(1, content.getSpans(17, 22, ForegroundColorSpan.class).length); // '&amp;'
        assertEquals(1, content.getSpans(17, 22, StyleSpan.class).length); // '&amp;'
        assertEquals(1, content.getSpans(17, 22, RelativeSizeSpan.class).length); // '&amp;'

        assertEquals(1, content.getSpans(23, 39, ForegroundColorSpan.class).length); // '<!--a comment-->'
        assertEquals(1, content.getSpans(23, 39, StyleSpan.class).length); // '<!--a comment-->'
        assertEquals(1, content.getSpans(23, 39, RelativeSizeSpan.class).length); // '<!--a comment-->'

        assertEquals(2, content.getSpans(40, 58, ForegroundColorSpan.class).length); // '<a href="website">'
        assertEquals(1, content.getSpans(40, 48, ForegroundColorSpan.class).length); // '<a href='
        // Attribute span is applied on top of tag span, so there should be 2 ForegroundColorSpans present
        assertEquals(2, content.getSpans(48, 57, ForegroundColorSpan.class).length); // '"website"'
        assertEquals(1, content.getSpans(57, 58, ForegroundColorSpan.class).length); // '>'

        assertEquals(0, content.getSpans(58, 62, CharacterStyle.class).length); // 'link'

        assertEquals(1, content.getSpans(62, 66, ForegroundColorSpan.class).length); // '</a>'
    }

    @Test
    public void testClearSpans() {
        Spannable content = new SpannableStringBuilder("<b>text &amp;");

        HtmlStyleUtils.styleHtmlForDisplay(content);

        assertEquals(1, content.getSpans(0, 3, ForegroundColorSpan.class).length); // '<b>'

        assertEquals(1, content.getSpans(9, 14, ForegroundColorSpan.class).length); // '&amp;'
        assertEquals(1, content.getSpans(9, 14, StyleSpan.class).length); // '&amp;'
        assertEquals(1, content.getSpans(9, 14, RelativeSizeSpan.class).length); // '&amp;'

        HtmlStyleUtils.clearSpans(content, 9, 14);

        assertEquals(1, content.getSpans(0, 3, ForegroundColorSpan.class).length);

        assertEquals(0, content.getSpans(9, 14, ForegroundColorSpan.class).length);
        assertEquals(0, content.getSpans(9, 14, StyleSpan.class).length);
        assertEquals(0, content.getSpans(9, 14, RelativeSizeSpan.class).length);

        HtmlStyleUtils.clearSpans(content, 0, 3);

        assertEquals(0, content.getSpans(0, 3, ForegroundColorSpan.class).length);


    }

    @Test
    public void testClearSpansShouldIgnoreUnderline() {
        // clearSpans() should ignore UnderlineSpan as it's used by the system for spelling suggestions
        Spannable content = new SpannableStringBuilder("test");

        content.setSpan(new UnderlineSpan(), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        HtmlStyleUtils.clearSpans(content, 0, 4);

        assertEquals(1, content.getSpans(0, 4, UnderlineSpan.class).length);
    }
}
