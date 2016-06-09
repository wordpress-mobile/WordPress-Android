package org.wordpress.android.editor;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Spanned;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.HashMap;

public abstract class EditorFragmentAbstract extends Fragment {
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
    public abstract CharSequence getTitle();
    public abstract CharSequence getContent();
    public abstract void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader);
    public abstract void appendGallery(MediaGallery mediaGallery);
    public abstract void setUrlForVideoPressId(String videoPressId, String url, String posterUrl);
    public abstract boolean isUploadingMedia();
    public abstract boolean isActionInProgress();
    public abstract boolean hasFailedMediaUploads();
    public abstract void removeAllFailedMediaUploads();
    public abstract void setTitlePlaceholder(CharSequence text);
    public abstract void setContentPlaceholder(CharSequence text);

    // TODO: remove this as soon as we can (we'll need to drop the legacy editor or fix html2spanned translation)
    public abstract Spanned getSpannedContent();

    public enum MediaType {
        IMAGE, VIDEO;

        public static MediaType fromString(String value) {
            if (value != null) {
                for (MediaType mediaType : MediaType.values()) {
                    if (value.equalsIgnoreCase(mediaType.toString())) {
                        return mediaType;
                    }
                }
            }
            return null;
        }
    }

    private static final String FEATURED_IMAGE_SUPPORT_KEY = "featured-image-supported";
    private static final String FEATURED_IMAGE_WIDTH_KEY   = "featured-image-width";

    protected EditorFragmentListener mEditorFragmentListener;
    protected boolean mFeaturedImageSupported;
    protected long mFeaturedImageId;
    protected String mBlogSettingMaxImageWidth;
    protected ImageLoader mImageLoader;
    protected boolean mDebugModeEnabled;

    protected HashMap<String, String> mCustomHttpHeaders;

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

    public void setFeaturedImageId(long featuredImageId) {
        mFeaturedImageId = featuredImageId;
    }

    public void setCustomHttpHeader(String name, String value) {
        if (mCustomHttpHeaders == null) {
            mCustomHttpHeaders = new HashMap<>();
        }

        mCustomHttpHeaders.put(name, value);
    }

    public void setDebugModeEnabled(boolean debugModeEnabled) {
        mDebugModeEnabled = debugModeEnabled;
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
        void onEditorFragmentInitialized();
        void onSettingsClicked();
        void onAddMediaClicked();
        void onMediaRetryClicked(String mediaId);
        void onMediaUploadCancelClicked(String mediaId, boolean delete);
        void onFeaturedImageChanged(long mediaId);
        void onVideoPressInfoRequested(String videoId);
        String onAuthHeaderRequested(String url);
        // TODO: remove saveMediaFile, it's currently needed for the legacy editor
        void saveMediaFile(MediaFile mediaFile);
        void onTrackableEvent(TrackableEvent event);
    }

    public enum TrackableEvent {
        HTML_BUTTON_TAPPED,
        UNLINK_BUTTON_TAPPED,
        LINK_BUTTON_TAPPED,
        MEDIA_BUTTON_TAPPED,
        IMAGE_EDITED,
        BOLD_BUTTON_TAPPED,
        ITALIC_BUTTON_TAPPED,
        OL_BUTTON_TAPPED,
        UL_BUTTON_TAPPED,
        BLOCKQUOTE_BUTTON_TAPPED,
        STRIKETHROUGH_BUTTON_TAPPED,
        UNDERLINE_BUTTON_TAPPED,
        MORE_BUTTON_TAPPED
    }
}
