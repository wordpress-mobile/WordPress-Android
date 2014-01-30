package org.wordpress.android.ui.notifications;

import android.text.SpannableStringBuilder;

import junit.framework.TestCase;

import org.wordpress.android.models.Note;

public class NotesParseTest extends TestCase {
    public void testParagraphInListItem1() {
        String text = "<li><p>Paragraph in li</p></li>";
        SpannableStringBuilder html = Note.prepareHtml(text);
        // if this didn't throw a RuntimeException we're ok
        assertNotNull(html);
    }
}