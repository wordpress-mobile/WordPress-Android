package org.wordpress.android.editor;

import android.app.Activity;
import android.text.Spanned;

import com.android.volley.toolbox.ImageLoader;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class EditorFragmentAbstractTest {
    @Test
    public void testActivityMustImplementEditorFragmentListener() {
        // Host Activity must implement EditorFragmentListener, exception expected if not
        boolean didPassTest = false;
        Activity hostActivity = Robolectric.buildActivity(Activity.class).create().get();
        EditorFragmentAbstract testFragment = new DefaultEditorFragment();

        try {
            testFragment.onAttach(hostActivity);
        } catch (ClassCastException classCastException) {
            didPassTest = true;
        }

        Assert.assertTrue(didPassTest);
    }

    @Test
    public void testOnBackPressReturnsFalseByDefault() {
        // The default behavior of onBackPressed should return false
        Assert.assertFalse(new DefaultEditorFragment().onBackPressed());
    }

    /**
     * Used to test default behavior of non-abstract methods.
     */
    public static class DefaultEditorFragment extends EditorFragmentAbstract {
        @Override
        public void setTitle(CharSequence text) {
        }

        @Override
        public void setContent(CharSequence text) {
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public CharSequence getContent() {
            return null;
        }

        @Override
        public void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader) {
        }

        @Override
        public void appendGallery(MediaGallery mediaGallery) {
        }

        @Override
        public void setUrlForVideoPressId(String videoPressId, String url, String posterUrl) {

        }

        @Override
        public boolean isUploadingMedia() {
            return false;
        }

        @Override
        public boolean isActionInProgress() {
            return false;
        }

        @Override
        public boolean hasFailedMediaUploads() {
            return false;
        }

        @Override
        public void removeAllFailedMediaUploads() {

        }

        @Override
        public void setTitlePlaceholder(CharSequence text) {

        }

        @Override
        public void setContentPlaceholder(CharSequence text) {

        }

        @Override
        public Spanned getSpannedContent() {
            return null;
        }
    }
}
