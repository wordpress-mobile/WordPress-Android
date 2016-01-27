package org.wordpress.android.editor;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ToggleButton;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.wordpress.android.editor.TestingUtils.waitFor;

public class EditorFragmentTest extends ActivityInstrumentationTestCase2<MockEditorActivity> {
    private Activity mActivity;
    private EditorFragmentForTests mFragment;

    public EditorFragmentTest() {
        super(MockEditorActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mFragment = (EditorFragmentForTests) mActivity.getFragmentManager().findFragmentByTag("editorFragment");
    }

    public void testDomLoadedCallbackReceived() {
        // initJsEditor() should have been called on setup
        assertTrue(mFragment.mInitCalled);

        waitForOnDomLoaded();

        // The JS editor should have sent out a callback when the DOM loaded, triggering onDomLoaded()
        assertTrue(mFragment.mDomLoaded);
    }

    public void testFormatBarToggledOnSelectedFieldChanged() {
        Map<String, String> selectionArgs = new HashMap<>();

        selectionArgs.put("id", "zss_field_title");
        mFragment.onSelectionChanged(selectionArgs);

        waitFor(100);

        View view = mFragment.getView();

        if (view == null) {
            throw(new IllegalStateException("Fragment view is empty"));
        }

        // The formatting buttons should be disabled while the title field is selected
        ToggleButton mediaButton = (ToggleButton) view.findViewById(R.id.format_bar_button_media);
        ToggleButton boldButton = (ToggleButton) view.findViewById(R.id.format_bar_button_bold);
        ToggleButton italicButton = (ToggleButton) view.findViewById(R.id.format_bar_button_italic);
        ToggleButton quoteButton = (ToggleButton) view.findViewById(R.id.format_bar_button_quote);
        ToggleButton ulButton = (ToggleButton) view.findViewById(R.id.format_bar_button_ul);
        ToggleButton olButton = (ToggleButton) view.findViewById(R.id.format_bar_button_ol);
        ToggleButton linkButton = (ToggleButton) view.findViewById(R.id.format_bar_button_link);
        ToggleButton strikethroughButton = (ToggleButton) view.findViewById(R.id.format_bar_button_strikethrough);

        assertFalse(mediaButton.isEnabled());
        assertFalse(boldButton.isEnabled());
        assertFalse(italicButton.isEnabled());
        assertFalse(quoteButton.isEnabled());
        assertFalse(ulButton.isEnabled());
        assertFalse(olButton.isEnabled());
        assertFalse(linkButton.isEnabled());

        if (strikethroughButton != null) {
            assertFalse(strikethroughButton.isEnabled());
        }

        // The HTML button should always be enabled
        ToggleButton htmlButton = (ToggleButton) view.findViewById(R.id.format_bar_button_html);
        assertTrue(htmlButton.isEnabled());

        selectionArgs.clear();
        selectionArgs.put("id", "zss_field_content");
        mFragment.onSelectionChanged(selectionArgs);

        waitFor(100);

        // The formatting buttons should be enabled while the content field is selected
        assertTrue(mediaButton.isEnabled());
        assertTrue(boldButton.isEnabled());
        assertTrue(italicButton.isEnabled());
        assertTrue(quoteButton.isEnabled());
        assertTrue(ulButton.isEnabled());
        assertTrue(olButton.isEnabled());
        assertTrue(linkButton.isEnabled());

        if (strikethroughButton != null) {
            assertTrue(strikethroughButton.isEnabled());
        }

        // The HTML button should always be enabled
        assertTrue(htmlButton.isEnabled());
    }

    public void testHtmlModeToggleTextTransfer() throws InterruptedException {
        waitForOnDomLoaded();

        final View view = mFragment.getView();

        if (view == null) {
            throw (new IllegalStateException("Fragment view is empty"));
        }

        final ToggleButton htmlButton = (ToggleButton) view.findViewById(R.id.format_bar_button_html);

        String content = mFragment.getContent().toString();

        final SourceViewEditText titleText = (SourceViewEditText) view.findViewById(R.id.sourceview_title);
        final SourceViewEditText contentText = (SourceViewEditText) view.findViewById(R.id.sourceview_content);

        // -- Check that title and content text is properly loaded into the EditTexts when switching to HTML mode

        final CountDownLatch uiThreadLatch1 = new CountDownLatch(1);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                htmlButton.performClick(); // Turn on HTML mode

                uiThreadLatch1.countDown();
            }
        });

        uiThreadLatch1.await();

        // The HTML mode fields should be populated with the raw HTML loaded into the WebView on load
        // (see MockEditorActivity)
        assertEquals("A title", titleText.getText().toString());
        assertEquals(content, contentText.getText().toString());

        // -- Check that the title and content text is updated in the WebView when switching back from HTML mode

        final CountDownLatch uiThreadLatch2 = new CountDownLatch(1);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                titleText.setText("new title");
                contentText.setText("new <b>content</b>");

                // Check that getTitle() and getContent() return latest version even in HTML mode
                assertEquals("new title", mFragment.getTitle());
                assertEquals("new <b>content</b>", mFragment.getContent());

                htmlButton.performClick(); // Turn off HTML mode

                uiThreadLatch2.countDown();
            }
        });

        uiThreadLatch2.await();

        waitFor(300); // Wait for JS to update the title/content

        assertEquals("new title", mFragment.getTitle());
        assertEquals("new <b>content</b>", mFragment.getContent());
    }

    private void waitForOnDomLoaded() {
        long start = System.currentTimeMillis();
        while(!mFragment.mDomLoaded) {
            waitFor(10);
            if (System.currentTimeMillis() - start > 5000) {
                throw(new RuntimeException("Callback wait timed out"));
            }
        }
    }
}