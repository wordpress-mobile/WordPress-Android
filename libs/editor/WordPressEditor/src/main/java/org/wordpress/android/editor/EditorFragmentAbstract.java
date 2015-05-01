package org.wordpress.android.editor;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import android.text.Spanned;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

public abstract class EditorFragmentAbstract extends Fragment {
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
    public abstract CharSequence getTitle();
    public abstract CharSequence getContent();
    public abstract void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader);
    public abstract void appendGallery(MediaGallery mediaGallery);

    // TODO: remove this as soon as we can (we'll need to drop the legacy editor or fix html2spanned translation)
    public abstract Spanned getSpannedContent();

    private static final String FEATURED_IMAGE_SUPPORT_KEY = "featured-image-supported";
    private static final String FEATURED_IMAGE_WIDTH_KEY   = "featured-image-width";

    protected EditorFragmentListener mEditorFragmentListener;
    protected boolean mFeaturedImageSupported;
    protected String mBlogSettingMaxImageWidth;
    protected ImageLoader mImageLoader;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEditorFragmentListener = (EditorFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorFragmentListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(FEATURED_IMAGE_SUPPORT_KEY, mFeaturedImageSupported);
        outState.putString(FEATURED_IMAGE_WIDTH_KEY, mBlogSettingMaxImageWidth);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(FEATURED_IMAGE_SUPPORT_KEY)) {
                mFeaturedImageSupported = savedInstanceState.getBoolean(FEATURED_IMAGE_SUPPORT_KEY);
            }
            if (savedInstanceState.containsKey(FEATURED_IMAGE_WIDTH_KEY)) {
                mBlogSettingMaxImageWidth = savedInstanceState.getString(FEATURED_IMAGE_WIDTH_KEY);
            }
        }
    }

    public void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public void setFeaturedImageSupported(boolean featuredImageSupported) {
        mFeaturedImageSupported = featuredImageSupported;
    }

    public void setBlogSettingMaxImageWidth(String blogSettingMaxImageWidth) {
        mBlogSettingMaxImageWidth = blogSettingMaxImageWidth;
    }

    /**
     * Called by the activity when back button is pressed.
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * The editor may need to differentiate local draft and published articles
     *
     * @param isLocalDraft edited post is a local draft
     */
    public void setLocalDraft(boolean isLocalDraft) {
        // Not unused in the new editor
    }

    /**
     * Callbacks used to communicate with the parent Activity
     */
    public interface EditorFragmentListener {
        public void onEditorFragmentInitialized();
        public void onSettingsClicked();
        public void onAddMediaClicked();
        // TODO: remove saveMediaFile, it's currently needed for the legacy editor - we should have something like
        // "EditorFragmentAbstract.getFeaturedImage()" returning the remote id
        public void saveMediaFile(MediaFile mediaFile);
    }
}
