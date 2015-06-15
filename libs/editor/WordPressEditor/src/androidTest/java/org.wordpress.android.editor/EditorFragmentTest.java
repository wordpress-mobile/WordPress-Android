package org.wordpress.android.editor;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ToggleButton;

import org.wordpress.android.editor.R;

import java.util.HashMap;
import java.util.Map;

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

        long start = System.currentTimeMillis();
        while(!mFragment.mDomLoaded) {
            waitFor(10);
            if (System.currentTimeMillis() - start > 5000) {
                throw(new RuntimeException("Callback wait timed out"));
            }
        }

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
}