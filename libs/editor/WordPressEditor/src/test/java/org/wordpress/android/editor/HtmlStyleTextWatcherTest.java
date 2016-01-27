package org.wordpress.android.editor;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class HtmlStyleTextWatcherTest {

    private HtmlStyleTextWatcherForTests mWatcher;
    private Editable mContent;
    private boolean mUpdateSpansWasCalled;
    private HtmlStyleTextWatcher.SpanRange mSpanRange;

    @Before
    public void setUp() {
        mWatcher = new HtmlStyleTextWatcherForTests();
        mUpdateSpansWasCalled = false;
    }

    @Test
    public void testTypingNormalText() {
        // -- Test typing in normal text (non-HTML) in an empty document
        mContent = new SpannableStringBuilder("a");

        mWatcher.onTextChanged(mContent, 0, 0, 1); // Typed "a"
        mWatcher.afterTextChanged(mContent);

        assertEquals(false, mUpdateSpansWasCalled);

        mContent = new SpannableStringBuilder("ab");

        mWatcher.onTextChanged(mContent, 1, 0, 1); // Typed "b"
        mWatcher.afterTextChanged(mContent);

        assertEquals(false, mUpdateSpansWasCalled);


        // -- Test typing in normal text after exiting tags
        mContent = new SpannableStringBuilder("text <b>bold</b> a");

        mWatcher.onTextChanged(mContent, 17, 0, 1); // Typed "a"
        mWatcher.afterTextChanged(mContent);

        assertEquals(false, mUpdateSpansWasCalled);


        // -- Test typing in normal text before exiting tags
        mContent = new SpannableStringBuilder("text a <b>bold</b>");

        mWatcher.onTextChanged(mContent, 5, 0, 1); // Typed "a"
        mWatcher.afterTextChanged(mContent);

        assertEquals(false, mUpdateSpansWasCalled);
    }

    @Test
    public void testTypingInOpeningTag() {
        // Test with several different cases of pre-existing text
        String[] previousTextCases = new String[]{"", "plain text", "<i>",
                "<blockquote>some existing content</blockquote> "};
        for (String initialText : previousTextCases) {
            int offset = initialText.length();
            mUpdateSpansWasCalled = false;

            // -- Test typing in an opening tag symbol
            mContent = new SpannableStringBuilder(initialText + "<");

            mWatcher.onTextChanged(mContent, offset, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);


            // -- Test typing in the tag name
            mContent = new SpannableStringBuilder(initialText + "<b");

            mWatcher.onTextChanged(mContent, offset + 1, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);


            // -- Test typing in a closing tag symbol
            mContent = new SpannableStringBuilder(initialText + "<b>");

            mWatcher.onTextChanged(mContent, offset + 2, 0, 1);
            mWatcher.afterTextChanged(mContent);

            assertEquals(offset, mSpanRange.getOpeningTagLoc());
            assertEquals(offset + 3, mSpanRange.getClosingTagLoc());
        }
    }

    @Test
    public void testTypingInClosingTag() {
        // Test with several different cases of pre-existing text
        String[] previousTextCases = new String[]{"<b>stuff", "plain text <b>stuff", "<i><b>stuff",
                "<blockquote>some existing content</blockquote> <b>stuff"};

        for (String initialText : previousTextCases) {
            int offset = initialText.length();
            mUpdateSpansWasCalled = false;

            // -- Test typing in an opening tag symbol
            mContent = new SpannableStringBuilder(initialText + "<");

            mWatcher.onTextChanged(mContent, offset, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);


            // -- Test typing in the closing tag slash
            mContent = new SpannableStringBuilder(initialText + "</");

            mWatcher.onTextChanged(mContent, offset + 1, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);

            // -- Test typing in the tag name
            mContent = new SpannableStringBuilder(initialText + "</b");

            mWatcher.onTextChanged(mContent, offset + 2, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);


            // -- Test typing in a closing tag symbol
            mContent = new SpannableStringBuilder(initialText + "</b>");

            mWatcher.onTextChanged(mContent, offset + 3, 0, 1);
            mWatcher.afterTextChanged(mContent);

            assertEquals(offset, mSpanRange.getOpeningTagLoc());
            assertEquals(offset + 4, mSpanRange.getClosingTagLoc());
        }
    }

    @Test
    public void testTypingInTagWithSurroundingTags() {
        // Spans in this case will be applied until the end of the next tag
        // This fixes a pasting bug and might be refined later
        // -- Test typing in the opening tag symbol
        mContent = new SpannableStringBuilder("some <del>text</del> < <b>bold text</b>");

        mWatcher.onTextChanged(mContent, 21, 0, 1); // Added lone "<"
        mWatcher.afterTextChanged(mContent);

        assertEquals(21, mSpanRange.getOpeningTagLoc());
        assertEquals(26, mSpanRange.getClosingTagLoc());


        // -- Test typing in the tag name
        mContent = new SpannableStringBuilder("some <del>text</del> <i <b>bold text</b>");

        mWatcher.onTextChanged(mContent, 22, 0, 1);
        mWatcher.afterTextChanged(mContent);

        assertEquals(21, mSpanRange.getOpeningTagLoc());
        assertEquals(27, mSpanRange.getClosingTagLoc());


        // -- Test typing in the closing tag symbol
        mContent = new SpannableStringBuilder("some <del>text</del> <i> <b>bold text</b>");

        mWatcher.onTextChanged(mContent, 23, 0, 1);
        mWatcher.afterTextChanged(mContent);

        assertEquals(21, mSpanRange.getOpeningTagLoc());
        assertEquals(28, mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testTypingInLoneClosingSymbol() {
        // -- Test typing in an isolated closing tag symbol
        mContent = new SpannableStringBuilder("some text >");

        mWatcher.onTextChanged(mContent, 10, 0, 1);
        mWatcher.afterTextChanged(mContent);

        // No formatting should be applied/removed
        assertEquals(false, mUpdateSpansWasCalled);


        // -- Test typing in an isolated closing tag symbol with surrounding tags
        mContent = new SpannableStringBuilder("some <b>tex>t</b>");

        mWatcher.onTextChanged(mContent, 11, 0, 1); // Added lone ">"
        mWatcher.afterTextChanged(mContent);

        // The span in this case will be applied from the start of the previous tag to the end of the next tag
        assertEquals(5, mSpanRange.getOpeningTagLoc());
        assertEquals(17, mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testTypingInEntity() {
        // Test with several different cases of pre-existing text
        String[] previousTextCases = new String[]{"", "plain text", "&rho;",
                "<blockquote>some existing content &dagger;</blockquote> "};
        for (String initialText : previousTextCases) {
            int offset = initialText.length();
            mUpdateSpansWasCalled = false;

            // -- Test typing in the entity's opening '&'
            mContent = new SpannableStringBuilder(initialText + "&");

            mWatcher.onTextChanged(mContent, offset, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);


            // -- Test typing in the entity's main text
            mContent = new SpannableStringBuilder(initialText + "&amp");

            mWatcher.onTextChanged(mContent, offset + 3, 0, 1);
            mWatcher.afterTextChanged(mContent);

            // No formatting should be applied/removed
            assertEquals(false, mUpdateSpansWasCalled);


            // -- Test typing in the entity's closing ';'
            mContent = new SpannableStringBuilder(initialText + "&amp;");

            mWatcher.onTextChanged(mContent, offset + 4, 0, 1);
            mWatcher.afterTextChanged(mContent);

            assertEquals(offset, mSpanRange.getOpeningTagLoc());
            assertEquals(offset + 5, mSpanRange.getClosingTagLoc());
        }
    }

    @Test
    public void testAddingTagFromFormatBar() {
        // -- Test adding a tag to an empty document
        mContent = new SpannableStringBuilder("<b>");

        mWatcher.onTextChanged(mContent, 0, 0, 3);
        mWatcher.afterTextChanged(mContent);

        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(3, mSpanRange.getClosingTagLoc());


        // -- Test adding a tag at the end of a document with text
        mContent = new SpannableStringBuilder("stuff<b>");

        mWatcher.onTextChanged(mContent, 5, 0, 3);
        mWatcher.afterTextChanged(mContent);

        assertEquals(5, mSpanRange.getOpeningTagLoc());
        assertEquals(8, mSpanRange.getClosingTagLoc());


        // -- Test adding a tag at the end of a document containing other html
        mContent = new SpannableStringBuilder("some text <i>italics</i> <b>");

        mWatcher.onTextChanged(mContent, 25, 0, 3); // Added "<b>"
        mWatcher.afterTextChanged(mContent);

        assertEquals(25, mSpanRange.getOpeningTagLoc());
        assertEquals(28, mSpanRange.getClosingTagLoc());


        // -- Test adding a tag at the start of a document with text
        mContent = new SpannableStringBuilder("<b>some text");

        mWatcher.onTextChanged(mContent, 0, 0, 3);
        mWatcher.afterTextChanged(mContent);

        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(3, mSpanRange.getClosingTagLoc());


        // -- Test adding a tag at the start of a document containing other html
        mContent = new SpannableStringBuilder("<b>some text <i>italics</i>");

        mWatcher.onTextChanged(mContent, 0, 0, 3);
        mWatcher.afterTextChanged(mContent);

        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(3, mSpanRange.getClosingTagLoc());


        // -- Test adding a tag within another tag pair
        mContent = new SpannableStringBuilder("<b>some <i>text</b>");

        mWatcher.onTextChanged(mContent, 8, 0, 3); // Added <i>
        mWatcher.afterTextChanged(mContent);

        assertEquals(8, mSpanRange.getOpeningTagLoc());
        assertEquals(11, mSpanRange.getClosingTagLoc());


        // -- Test adding a closing tag within another tag pair
        mContent = new SpannableStringBuilder("<b>some <i>text</i></b>");

        mWatcher.onTextChanged(mContent, 15, 0, 4); // Added "</i>"
        mWatcher.afterTextChanged(mContent);

        assertEquals(15, mSpanRange.getOpeningTagLoc());
        assertEquals(19, mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testAddingListTagsFromFormatBar() {
        // -- Test adding a list tag to an empty document
        mContent = new SpannableStringBuilder("<ul>\n\t<li>");

        mWatcher.onTextChanged(mContent, 0, 0, 10);
        mWatcher.afterTextChanged(mContent);

        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(10, mSpanRange.getClosingTagLoc());


        // -- Test adding a closing list tag
        mContent = new SpannableStringBuilder("<ul>\n" + //5
                "\t<li>list item</li>\n" + //20
                "\t<li>another list item</li>\n" + //22
                "</ul>");

        mWatcher.onTextChanged(mContent, 47, 0, 11); // Added "</li>\n</ul>"
        mWatcher.afterTextChanged(mContent);

        assertEquals(47, mSpanRange.getOpeningTagLoc());
        assertEquals(58, mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testDeletingPartsOfTag() {
        // -- Test deleting different characters within a tag
        mContent = new SpannableStringBuilder("<b>stuff</b>");

        int deletedChar = 0;
        mWatcher.beforeTextChanged(mContent, deletedChar, 1, 0);
        // Deleted characters are removed from the string between beforeTextChanged() and onTextChanged()
        mContent.delete(deletedChar, deletedChar + 1);
        mWatcher.onTextChanged(mContent, deletedChar, 1, 0);
        mWatcher.afterTextChanged(mContent);

        // "b>" should be re-styled
        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(2, mSpanRange.getClosingTagLoc());

        for (int i = 8; i < 12; i++) {
            mContent = new SpannableStringBuilder("<b>stuff</b>");

            mWatcher.beforeTextChanged(mContent, i, 1, 0);
            mContent.delete(i, i + 1);
            mWatcher.afterTextChanged(mContent);

            // Style should be updated starting from the end of 'stuff'
            assertEquals(8, mSpanRange.getOpeningTagLoc());
            assertEquals(mContent.length(), mSpanRange.getClosingTagLoc());
        }
    }

    @Test
    public void testPasteTagPair() {
        // -- Test pasting in a set of opening and closing tags at the end of the document
        mContent = new SpannableStringBuilder("text <b></b>");

        mWatcher.onTextChanged(mContent, 5, 0, 7);
        mWatcher.afterTextChanged(mContent);

        assertEquals(5, mSpanRange.getOpeningTagLoc());
        assertEquals(12, mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testCutAndPasteTagPart() {
        // -- Test cutting a tag and part of another tag from the document
        mContent = new SpannableStringBuilder("test <b></b> <i>italics</i>");

        mWatcher.beforeTextChanged(mContent, 5, 4, 0); // Deleted "<b><"
        mContent.delete(5, 9);
        mWatcher.onTextChanged(mContent, 5, 4, 0);
        mWatcher.afterTextChanged(mContent);

        assertEquals(5, mSpanRange.getOpeningTagLoc());
        assertEquals(8, mSpanRange.getClosingTagLoc());


        // -- Test pasting the cut text back in
        mContent = new SpannableStringBuilder("test <b></b> <i>italics</i>");
        mWatcher.onTextChanged(mContent, 5, 0, 4); // Pasted "<b><" back in
        mWatcher.afterTextChanged(mContent);

        assertEquals(5, mSpanRange.getOpeningTagLoc());
        assertEquals(12, mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testCutAndPasteTagPartReplacingText() {
        // -- Test pasting cut text while text is selected
        // Pasted "<b><", replacing "st " of "test "
        mContent = new SpannableStringBuilder("test /b> <i>italics</i>");
        mWatcher.beforeTextChanged(mContent, 2, 3, 4);
        mContent = new SpannableStringBuilder("te<b></b> <i>italics</i>");
        mWatcher.onTextChanged(mContent, 2, 3, 4);
        mWatcher.afterTextChanged(mContent);

        // Should re-style whole document
        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(mContent.length(), mSpanRange.getClosingTagLoc());


        // -- Test pasting cut text while text is selected, case 2
        // Pasted "i>", replacing "test "
        mContent = new SpannableStringBuilder("<test italics</i>");
        mWatcher.beforeTextChanged(mContent, 1, 5, 2);
        mContent = new SpannableStringBuilder("<i>italics</i>");
        mWatcher.onTextChanged(mContent, 1, 5, 2);
        mWatcher.afterTextChanged(mContent);

        // Should re-style whole document
        assertEquals(0, mSpanRange.getOpeningTagLoc());
        assertEquals(mContent.length(), mSpanRange.getClosingTagLoc());
    }

    @Test
    public void testNoChange() {

        mWatcher.beforeTextChanged("sample", 0, 0, 0);
        mWatcher.onTextChanged("sample", 0, 0, 0);
        mWatcher.afterTextChanged(null);

        // No formatting should be applied/removed
        assertEquals(false, mUpdateSpansWasCalled);
    }

    @Test
    public void testUpdateSpans() {
        // -- Test tag styling
        HtmlStyleTextWatcher watcher = new HtmlStyleTextWatcher();
        Spannable content = new SpannableStringBuilder("<b>stuff</b>");
        watcher.updateSpans(content, new HtmlStyleTextWatcher.SpanRange(0, 3));

        assertEquals(1, content.getSpans(0, 3, ForegroundColorSpan.class).length);

        // -- Test entity styling
        content = new SpannableStringBuilder("text &amp; more text");
        watcher.updateSpans(content, new HtmlStyleTextWatcher.SpanRange(5, 10));

        assertEquals(1, content.getSpans(5, 10, ForegroundColorSpan.class).length);
        assertEquals(1, content.getSpans(5, 10, StyleSpan.class).length);
        assertEquals(1, content.getSpans(5, 10, RelativeSizeSpan.class).length);

        // -- Test comment styling
        content = new SpannableStringBuilder("text <!--comment--> more text");
        watcher.updateSpans(content, new HtmlStyleTextWatcher.SpanRange(5, 19));

        assertEquals(1, content.getSpans(5, 19, ForegroundColorSpan.class).length);
        assertEquals(1, content.getSpans(5, 19, StyleSpan.class).length);
        assertEquals(1, content.getSpans(5, 19, RelativeSizeSpan.class).length);

        content = new SpannableStringBuilder("<b>stuff</b>");
        watcher.updateSpans(content, new HtmlStyleTextWatcher.SpanRange(0, 3));

        watcher.updateSpans(content, new HtmlStyleTextWatcher.SpanRange(0, 42));
        assertEquals(1, content.getSpans(0, 3, ForegroundColorSpan.class).length);

    }

    private class HtmlStyleTextWatcherForTests extends HtmlStyleTextWatcher {
        @Override
        protected void updateSpans(Spannable s, SpanRange spanRange) {
            mSpanRange = spanRange;
            mUpdateSpansWasCalled = true;
        }
    }
}
