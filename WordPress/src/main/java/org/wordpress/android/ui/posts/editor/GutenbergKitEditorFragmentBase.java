package org.wordpress.android.ui.posts.editor;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.android.volley.toolbox.ImageLoader;
import com.automattic.android.tracks.crashlogging.JsException;
import com.automattic.android.tracks.crashlogging.JsExceptionCallback;

import org.wordpress.android.editor.EditorEditMediaListener;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorImagePreviewListener;
import org.wordpress.android.editor.gutenberg.DialogVisibilityProvider;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class GutenbergKitEditorFragmentBase extends Fragment {
    public class EditorFragmentNotAddedException extends Exception {
    }

    public abstract @NonNull String getEditorName();
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
    public abstract CharSequence getContent(CharSequence originalContent)
            throws EditorFragmentAbstract.EditorFragmentNotAddedException;
    public abstract Pair<CharSequence, CharSequence> getTitleAndContent(CharSequence originalContent) throws
            EditorFragmentAbstract.EditorFragmentNotAddedException;
    public abstract LiveData<Editable> getTitleOrContentChanged();
    public abstract void appendMediaFiles(Map<String, MediaFile> mediaList);

    public abstract void onUndoPressed();

    public abstract void onRedoPressed();

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
    private static final String FEATURED_IMAGE_WIDTH_KEY = "featured-image-width";

    protected EditorFragmentListener mEditorFragmentListener;
    protected EditorImagePreviewListener mEditorImagePreviewListener;
    protected EditorEditMediaListener mEditorEditMediaListener;
    protected boolean mFeaturedImageSupported;
    protected long mFeaturedImageId;
    protected String mBlogSettingMaxImageWidth;
    protected ImageLoader mImageLoader;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
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

    public void setFeaturedImageId(long featuredImageId) {
        mFeaturedImageId = featuredImageId;
    }

    public void setCustomHttpHeader(String name, String value) {
        if (mCustomHttpHeaders == null) {
            mCustomHttpHeaders = new HashMap<>();
        }

        mCustomHttpHeaders.put(name, value);
    }

    /**
     * Called by the activity when back button is pressed.
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * Callbacks used to communicate with the parent Activity
     */
    public interface EditorFragmentListener extends DialogVisibilityProvider {
        void onEditorFragmentInitialized();
        void onEditorFragmentContentReady(ArrayList<Object> unsupportedBlocks, boolean replaceBlockActionWaiting);
        void onCapturePhotoClicked();
        void onCaptureVideoClicked();
        Map<String, String> onAuthHeaderRequested(String url);
        void onTrackableEvent(org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent event);
        void onHtmlModeToggledInToolbar();
        void showUserSuggestions(Consumer<String> onResult);
        void showXpostSuggestions(Consumer<String> onResult);
        boolean showPreview();
        void onToggleUndo(boolean isDisabled);
        void onToggleRedo(boolean isDisabled);
        void onLogJsException(JsException jsException, JsExceptionCallback onSendJsException);
        void onFeaturedImageIdChanged(long mediaID, boolean isGutenbergEditor);
        void onOpenMediaLibraryRequested(org.wordpress.gutenberg.GutenbergView.OpenMediaLibraryConfig config);
    }
}
