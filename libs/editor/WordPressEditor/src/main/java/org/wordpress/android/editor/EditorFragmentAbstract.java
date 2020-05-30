package org.wordpress.android.editor;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.DragEvent;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class EditorFragmentAbstract extends Fragment {
    public class EditorFragmentNotAddedException extends Exception {
    }

    public abstract @NonNull String getEditorName();
    public abstract void setTitle(CharSequence text);
    public abstract void setContent(CharSequence text);
    public abstract CharSequence getTitle() throws EditorFragmentNotAddedException;
    public abstract CharSequence getContent(CharSequence originalContent) throws EditorFragmentNotAddedException;
    public abstract LiveData<Editable> getTitleOrContentChanged();
    public abstract void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader);
    public abstract void appendMediaFiles(Map<String, MediaFile> mediaList);
    public abstract void appendGallery(MediaGallery mediaGallery);
    public abstract void setUrlForVideoPressId(String videoPressId, String url, String posterUrl);
    public abstract boolean isUploadingMedia();
    public abstract boolean isActionInProgress();
    public abstract boolean hasFailedMediaUploads();
    // Check whether we need to reload the content of the post from the editor or not.
    // This was required since Aztec Visual->HTML can slightly change the content of the HTML. See #692 for details.
    public abstract void removeAllFailedMediaUploads();
    public abstract void removeMedia(String mediaId);
    public abstract boolean showSavingProgressDialogIfNeeded();
    public abstract boolean hideSavingProgressDialog();
    // Called from EditPostActivity to let the block editor know when a media selection is cancelled
    public abstract void mediaSelectionCancelled();


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

    protected static final String ARG_PARAM_TITLE = "param_title";
    protected static final String ARG_PARAM_CONTENT = "param_content";
    protected static final String ATTR_ALIGN = "align";
    protected static final String ATTR_ALT = "alt";
    protected static final String ATTR_CAPTION = "caption";
    protected static final String ATTR_DIMEN_HEIGHT = "height";
    protected static final String ATTR_DIMEN_WIDTH = "width";
    protected static final String ATTR_ID = "id";
    protected static final String ATTR_ID_ATTACHMENT = "attachment_id";
    protected static final String ATTR_ID_IMAGE_REMOTE = "imageRemoteId";
    protected static final String ATTR_SRC = "src";
    protected static final String ATTR_STATUS_FAILED = "failed";
    protected static final String ATTR_STATUS_UPLOADING = "uploading";
    protected static final String ATTR_TITLE = "title";
    protected static final String ATTR_URL_LINK = "linkUrl";
    protected static final String EXTRA_ENABLED_AZTEC = "isAztecEnabled";
    protected static final String EXTRA_FEATURED = "isFeatured";
    protected static final String EXTRA_IMAGE_FEATURED = "featuredImageSupported";
    protected static final String EXTRA_IMAGE_META = "imageMeta";
    protected static final String EXTRA_MAX_WIDTH = "maxWidth";

    private static final String FEATURED_IMAGE_SUPPORT_KEY = "featured-image-supported";
    private static final String FEATURED_IMAGE_WIDTH_KEY = "featured-image-width";

    protected EditorFragmentListener mEditorFragmentListener;
    protected EditorDragAndDropListener mEditorDragAndDropListener;
    protected EditorImagePreviewListener mEditorImagePreviewListener;
    protected EditorEditMediaListener mEditorEditMediaListener;
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

    public static MediaType getEditorMimeType(MediaFile mediaFile) {
        if (mediaFile == null) {
            // default to image
            return MediaType.IMAGE;
        }
        return mediaFile.isVideo() ? MediaType.VIDEO
                : MediaType.IMAGE;
    }

    /**
     * Callbacks used to communicate with the parent Activity
     */
    public interface EditorFragmentListener {
        void onEditorFragmentInitialized();
        void onEditorFragmentContentReady(ArrayList<Object> unsupportedBlocks);
        void onAddMediaClicked();
        void onAddMediaImageClicked(boolean allowMultipleSelection);
        void onAddMediaVideoClicked(boolean allowMultipleSelection);
        void onAddLibraryMediaClicked(boolean allowMultipleSelection);
        void onAddPhotoClicked(boolean allowMultipleSelection);
        void onCapturePhotoClicked();
        void onAddVideoClicked(boolean allowMultipleSelection);
        void onAddDeviceMediaClicked(boolean allowMultipleSelection);
        void onCaptureVideoClicked();
        boolean onMediaRetryClicked(String mediaId);
        void onMediaRetryAllClicked(Set<String> mediaIdSet);
        void onMediaUploadCancelClicked(String mediaId);
        void onMediaDeleted(String mediaId);
        void onUndoMediaCheck(String undoedContent);
        void onVideoPressInfoRequested(String videoId);
        Map<String, String> onAuthHeaderRequested(String url);
        void onTrackableEvent(TrackableEvent event);
        void onHtmlModeToggledInToolbar();
        void onAddStockMediaClicked(boolean allowMultipleSelection);
        void onPerformFetch(String path, Consumer<String> onResult, Consumer<Bundle> onError);
        void onGutenbergEditorSessionTemplateApplyTracked(String template);
        void onGutenbergEditorSessionTemplatePreviewTracked(String template);
        void getMention(Consumer<String> onResult);
        void onGutenbergEditorSetStarterPageTemplatesTooltipShown(boolean tooltipShown);
        boolean onGutenbergEditorRequestStarterPageTemplatesTooltipShown();
    }

    /**
     * Callbacks for drag and drop support
     */
    public interface EditorDragAndDropListener {
        void onMediaDropped(ArrayList<Uri> mediaUri);
        void onRequestDragAndDropPermissions(DragEvent dragEvent);
    }

    public enum TrackableEvent {
        BLOCKQUOTE_BUTTON_TAPPED,
        BOLD_BUTTON_TAPPED,
        ELLIPSIS_COLLAPSE_BUTTON_TAPPED,
        ELLIPSIS_EXPAND_BUTTON_TAPPED,
        HEADING_BUTTON_TAPPED,
        HEADING_1_BUTTON_TAPPED,
        HEADING_2_BUTTON_TAPPED,
        HEADING_3_BUTTON_TAPPED,
        HEADING_4_BUTTON_TAPPED,
        HEADING_5_BUTTON_TAPPED,
        HEADING_6_BUTTON_TAPPED,
        HORIZONTAL_RULE_BUTTON_TAPPED,
        FORMAT_ALIGN_LEFT_BUTTON_TAPPED,
        FORMAT_ALIGN_CENTER_BUTTON_TAPPED,
        FORMAT_ALIGN_RIGHT_BUTTON_TAPPED,
        HTML_BUTTON_TAPPED,
        IMAGE_EDITED,
        ITALIC_BUTTON_TAPPED,
        LINK_ADDED_BUTTON_TAPPED,
        LIST_BUTTON_TAPPED,
        LIST_ORDERED_BUTTON_TAPPED,
        LIST_UNORDERED_BUTTON_TAPPED,
        MEDIA_BUTTON_TAPPED,
        NEXT_PAGE_BUTTON_TAPPED,
        PARAGRAPH_BUTTON_TAPPED,
        PREFORMAT_BUTTON_TAPPED,
        READ_MORE_BUTTON_TAPPED,
        STRIKETHROUGH_BUTTON_TAPPED,
        UNDERLINE_BUTTON_TAPPED,
        REDO_TAPPED,
        UNDO_TAPPED
    }
}
