package org.wordpress.android.editor;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.editor.MetadataUtils.AttributesWithClass;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.AztecAttributes;
import org.wordpress.aztec.AztecText;
import org.wordpress.aztec.AztecText.OnImageTappedListener;
import org.wordpress.aztec.HistoryListener;
import org.wordpress.aztec.Html;
import org.wordpress.aztec.source.SourceViewEditText;
import org.wordpress.aztec.toolbar.AztecToolbar;
import org.wordpress.aztec.toolbar.AztecToolbarClickListener;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AztecEditorFragment extends EditorFragmentAbstract implements
        OnImeBackListener,
        EditorMediaUploadListener,
        OnImageTappedListener,
        AztecToolbarClickListener,
        HistoryListener {

    private static final String ATTR_ALIGN_DASH = "align-";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_ID_WP = "data-wpid";
    private static final String ATTR_IMAGE_WP_DASH = "wp-image-";
    private static final String ATTR_SIZE = "size";
    private static final String ATTR_SIZE_DASH = "size-";
    private static final String TEMP_IMAGE_ID = "data-temp-aztec-id";

    private static final int MIN_BITMAP_DIMENSION_DP = 48;

    public static final int MAX_ACTION_TIME_MS = 2000;

    private static final MediaFile DEFAULT_MEDIA = new MediaFile();
    private static final int DEFAULT_MEDIA_HEIGHT = DEFAULT_MEDIA.getHeight();
    private static final int DEFAULT_MEDIA_WIDTH = DEFAULT_MEDIA.getWidth();

    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_TEXT = Arrays.asList(ClipDescription
            .MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML);
    private static final List<String> DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE = Arrays.asList("image/jpeg", "image/png");

    private boolean mIsKeyboardOpen = false;
    private boolean mEditorWasPaused = false;
    private boolean mHideActionBarOnSoftKeyboardUp = false;

    private AztecText title;
    private AztecText content;
    private SourceViewEditText source;
    private AztecToolbar formattingToolbar;
    private Html.ImageGetter imageLoader;

    private Handler invalidateOptionsHandler;
    private Runnable invalidateOptionsRunnable;

    private Map<String, MediaType> mUploadingMedia;
    private Set<String> mFailedMediaIds;

    private long mActionStartedAt = -1;

    private ImagePredicate mTappedImagePredicate;

    public static AztecEditorFragment newInstance(String title, String content) {
        AztecEditorFragment fragment = new AztecEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aztec_editor, container, false);

        mUploadingMedia = new HashMap<>();
        mFailedMediaIds = new HashSet<>();

        title = (AztecText) view.findViewById(R.id.title);
        content = (AztecText)view.findViewById(R.id.aztec);
        source = (SourceViewEditText) view.findViewById(R.id.source);

        source.setHint("<p>" + getString(R.string.edit_hint) + "</p>");

        formattingToolbar = (AztecToolbar) view.findViewById(R.id.formatting_toolbar);
        formattingToolbar.setEditor(content, source);
        formattingToolbar.setToolbarListener(this);

        title.setOnFocusChangeListener(
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    formattingToolbar.enableFormatButtons(!hasFocus);
                }
            }
        );

        // initialize the text & HTML
        source.setHistory(content.getHistory());
        content.setImageGetter(imageLoader);

        content.getHistory().setHistoryListener(this);

        content.setOnImageTappedListener(this);

        mEditorFragmentListener.onEditorFragmentInitialized();

        content.setOnDragListener(mOnDragListener);
        source.setOnDragListener(mOnDragListener);

        setHasOptionsMenu(true);

        invalidateOptionsHandler = new Handler();
        invalidateOptionsRunnable = new Runnable() {
            @Override
            public void run() {
                getActivity().invalidateOptionsMenu();
            }
        };

        return view;
    }

    public void setImageLoader(Html.ImageGetter imageLoader) {
        this.imageLoader = imageLoader;
    }

    @Override
    public void onPause() {
        super.onPause();
        mEditorWasPaused = true;
        mIsKeyboardOpen = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the editor was previously paused and the current orientation is landscape,
        // hide the actionbar because the keyboard is going to appear (even if it was hidden
        // prior to being paused).
        if (mEditorWasPaused
                && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mIsKeyboardOpen = true;
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mEditorDragAndDropListener = (EditorDragAndDropListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorDragAndDropListener");
        }
    }

    @Override
    public void onDetach() {
        // Soft cancel (delete flag off) all media uploads currently in progress
        for (String mediaId : mUploadingMedia.keySet()) {
            mEditorFragmentListener.onMediaUploadCancelClicked(mediaId, false);
        }
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(ATTR_TITLE, getTitle());
        outState.putCharSequence(ATTR_CONTENT, getContent());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_aztec, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // TODO: disable undo/redo in media mode
        boolean canRedo = content.history.redoValid();
        boolean canUndo = content.history.undoValid();

        if (menu != null && menu.findItem(R.id.redo) != null) {
            menu.findItem(R.id.redo).setEnabled(canRedo);
        }

        if (menu != null && menu.findItem(R.id.undo) != null) {
            menu.findItem(R.id.undo).setEnabled(canUndo);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.undo) {
            if (content.getVisibility() == View.VISIBLE) {
                content.undo();
            } else {
                source.undo();
            }
            return true;
        } else if (item.getItemId() == R.id.redo) {
            if (content.getVisibility() == View.VISIBLE) {
                content.redo();
            } else {
                source.redo();
            }
            return true;
        }

        return false;
    }

    @Override
    public void onRedoEnabled() {
        invalidateOptionsHandler.removeCallbacks(invalidateOptionsRunnable);
        invalidateOptionsHandler.postDelayed(invalidateOptionsRunnable, getResources().getInteger(android.R.integer.config_mediumAnimTime) );
    }

    @Override
    public void onUndoEnabled() {
        invalidateOptionsHandler.removeCallbacks(invalidateOptionsRunnable);
        invalidateOptionsHandler.postDelayed(invalidateOptionsRunnable, getResources().getInteger(android.R.integer.config_mediumAnimTime) );
    }

    private ActionBar getActionBar() {
        if (!isAdded()) {
            return null;
        }

        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            return null;
        }
    }

    /**
     * Intercept back button press while soft keyboard is visible.
     */
    @Override
    public void onImeBack() {
        mIsKeyboardOpen = false;
        showActionBarIfNeeded();
    }

    @Override
    public void setTitle(CharSequence text) {
        title.setText(text);
    }

    @Override
    public void setContent(CharSequence text) {
        content.fromHtml(text.toString());
        updateFailedMediaList();
        overlayFailedMedia();
    }

    /**
     * Returns the contents of the title field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getTitle() {
        if (!isAdded()) {
            return "";
        }

        // TODO: Aztec returns a ZeroWidthJoiner when empty so, strip it. Aztec needs fixing to return empty string.
        return StringUtils.notNullStr(title.getText().toString().replaceAll("&nbsp;$", "").replaceAll("\u200B", ""));
    }

    @Override
    public void onToolbarHtmlModeClicked() {
        if (!isAdded()) {
            return;
        }

        checkForFailedUploadAndSwitchToHtmlMode();
    }

    private void checkForFailedUploadAndSwitchToHtmlMode() {
        // Show an Alert Dialog asking the user if he wants to remove all failed media before upload
        if (hasFailedMediaUploads()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.editor_failed_uploads_switch_html)
                    .setPositiveButton(R.string.editor_remove_failed_uploads, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Clear failed uploads and switch to HTML mode
                            removeAllFailedMediaUploads();
                            toggleHtmlMode();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // nothing special to do
                        }
                    })
                    .create()
                    .show();
        } else {
            toggleHtmlMode();
        }
    }

    private void toggleHtmlMode() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED);

        // Don't switch to HTML mode if currently uploading media
        if (!mUploadingMedia.isEmpty() || isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        formattingToolbar.toggleEditorMode();

        if (source.getVisibility() == View.VISIBLE) {
            updateFailedMediaList();
        }
    }

    public void enableMediaMode(boolean enable) {
        formattingToolbar.enableMediaMode(enable);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean isActionInProgress() {
        return System.currentTimeMillis() - mActionStartedAt < MAX_ACTION_TIME_MS;
    }

    private void updateFailedMediaList() {
        AztecText.AttributePredicate failedPredicate = new AztecText.AttributePredicate() {
            @Override
            public boolean matches(@NonNull Attributes attrs) {
                AttributesWithClass attributesWithClass = new AttributesWithClass(attrs);
                return attributesWithClass.hasClass(ATTR_STATUS_FAILED);
            }
        };

        mFailedMediaIds.clear();

        for (Attributes attrs : content.getAllElementAttributes(failedPredicate)) {
            mFailedMediaIds.add(attrs.getValue(ATTR_ID_WP));
        }
    }

    private void overlayFailedMedia() {
        for (String localMediaId : mFailedMediaIds) {
            Attributes attributes = content.getElementAttributes(ImagePredicate.getLocalMediaIdPredicate(localMediaId));
            overlayFailedMedia(localMediaId, attributes);
        }
    }

    private void overlayFailedMedia(String localMediaId, Attributes attributes) {
        // set intermediate shade overlay
        AztecText.AttributePredicate localMediaIdPredicate = ImagePredicate.getLocalMediaIdPredicate(localMediaId);
        content.setOverlay(localMediaIdPredicate, 0,
                new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_error_color)),
                Gravity.FILL);

        Drawable alertDrawable = getResources().getDrawable(R.drawable.media_retry_image);
        content.setOverlay(localMediaIdPredicate, 1, alertDrawable, Gravity.CENTER);
        content.updateElementAttributes(localMediaIdPredicate, new AztecAttributes(attributes));
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Override
    public CharSequence getContent() {
        if (!isAdded()) {
            return "";
        }

        if (content.getVisibility() == View.VISIBLE) {
            return content.toHtml(false);
        } else {
            return source.getPureHtml(false);
        }
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        final String safeMediaUrl = Utils.escapeQuotes(mediaUrl);

        if (URLUtil.isNetworkUrl(mediaUrl)) {
            if (mediaFile.isVideo()) {
                // TODO: insert video
                ToastUtils.showToast(getActivity(), R.string.media_insert_unimplemented);
            } else {
                imageLoader.get(mediaUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Show failed placeholder.
                        ToastUtils.showToast(getActivity(), R.string.error_media_load);
                        Drawable drawable = getResources().getDrawable(R.drawable.ic_image_failed_grey_a_40_48dp);
                        AztecAttributes attributes = new AztecAttributes();
                        attributes.setValue(ATTR_SRC, mediaUrl);
                        setAttributeValuesIfNotDefault(attributes, mediaFile);
                        content.insertImage(drawable, attributes);
                    }

                    @Override
                    public void onResponse(ImageLoader.ImageContainer container, boolean isImmediate) {
                        Bitmap downloadedBitmap = container.getBitmap();

                        if (downloadedBitmap == null) {
                            // No bitmap downloaded from server.
                            return;
                        }

                        AztecAttributes attributes = new AztecAttributes();
                        attributes.setValue(ATTR_SRC, mediaUrl);
                        setAttributeValuesIfNotDefault(attributes, mediaFile);

                        int minimumDimension = DisplayUtils.dpToPx(getActivity(), MIN_BITMAP_DIMENSION_DP);

                        if (downloadedBitmap.getHeight() < minimumDimension || downloadedBitmap.getWidth() < minimumDimension) {
                            // Bitmap is too small.  Show image placeholder.
                            ToastUtils.showToast(getActivity(), R.string.error_media_small);
                            Drawable drawable = getResources().getDrawable(R.drawable.ic_image_loading_grey_a_40_48dp);
                            content.insertImage(drawable, attributes);
                            return;
                        }

                        Bitmap resizedBitmap = ImageUtils.getScaledBitmapAtLongestSide(downloadedBitmap, DisplayUtils.getDisplayPixelWidth(getActivity()));
                        content.insertImage(new BitmapDrawable(getResources(), resizedBitmap), attributes);
                    }
                }, 0, 0);
            }

            mActionStartedAt = System.currentTimeMillis();
        } else {
            String localMediaId = String.valueOf(mediaFile.getId());

            if (mediaFile.isVideo()) {
                // TODO: insert local video
                ToastUtils.showToast(getActivity(), R.string.media_insert_unimplemented);
            } else {
                AztecAttributes attrs = new AztecAttributes();
                attrs.setValue(ATTR_ID_WP, localMediaId);
                attrs.setValue(ATTR_SRC, safeMediaUrl);
                attrs.setValue(ATTR_CLASS, ATTR_STATUS_UPLOADING);

                // load a scaled version of the image to prevent OOM exception
                int maxWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
                Bitmap bitmapToShow = ImageUtils.getWPImageSpanThumbnailFromFilePath(getActivity(), safeMediaUrl, maxWidth);

                if (bitmapToShow != null) {
                    content.insertImage(new BitmapDrawable(getResources(), bitmapToShow), attrs);
                } else {
                    // Failed to retrieve bitmap.  Show failed placeholder.
                    ToastUtils.showToast(getActivity(), R.string.error_media_load);
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_image_failed_grey_a_40_48dp);
                    drawable.setBounds(0, 0, maxWidth, maxWidth);
                    content.insertImage(drawable, attrs);
                }

                // set intermediate shade overlay
                AztecText.AttributePredicate localMediaIdPredicate = ImagePredicate.getLocalMediaIdPredicate(localMediaId);
                content.setOverlay(localMediaIdPredicate, 0,
                        new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_color)),
                        Gravity.FILL);

                Drawable progressDrawable = getResources().getDrawable(android.R.drawable.progress_horizontal);
                // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
                progressDrawable.setBounds(0, 0, 0, 4);

                content.setOverlay(localMediaIdPredicate, 1, progressDrawable,
                        Gravity.FILL_HORIZONTAL | Gravity.TOP);

                content.updateElementAttributes(localMediaIdPredicate, attrs);

                content.refreshText();

                mUploadingMedia.put(localMediaId, MediaType.IMAGE);
            }
        }
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
        ToastUtils.showToast(getActivity(), R.string.media_insert_unimplemented);
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
    }

    @Override
    public boolean isUploadingMedia() {
        return (mUploadingMedia.size() > 0);
    }

    @Override
    public boolean hasFailedMediaUploads() {
        return (mFailedMediaIds.size() > 0);
    }

    @Override
    public void removeAllFailedMediaUploads() {
        content.removeMedia(new AztecText.AttributePredicate() {
            @Override
            public boolean matches(@NotNull Attributes attrs) {
                return new AttributesWithClass(attrs).hasClass(ATTR_STATUS_FAILED);
            }
        });
    }

    @Override
    public Spanned getSpannedContent() {
        return null;
    }

    @Override
    public void setTitlePlaceholder(CharSequence placeholderText) {
    }

    @Override
    public void setContentPlaceholder(CharSequence placeholderText) {
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
        if(!isAdded()) {
            return;
        }
        final MediaType mediaType = mUploadingMedia.get(localMediaId);
        if (mediaType != null) {
            String remoteUrl = Utils.escapeQuotes(mediaFile.getFileURL());
            if (mediaType.equals(MediaType.IMAGE)) {

                AztecAttributes attrs = new AztecAttributes();
                attrs.setValue(ATTR_SRC, remoteUrl);

                // clear overlay
                ImagePredicate predicate = ImagePredicate.getLocalMediaIdPredicate(localMediaId);
                content.clearOverlays(predicate);
                content.updateElementAttributes(predicate, attrs);
                content.refreshText();

                mUploadingMedia.remove(localMediaId);
            } else if (mediaType.equals(MediaType.VIDEO)) {
                // TODO: update video element
            }
        }
    }

    private static class ImagePredicate implements AztecText.AttributePredicate {
        private final String mId;
        private final String mAttributeName;

        static ImagePredicate getLocalMediaIdPredicate(String id) {
            return new ImagePredicate(id, ATTR_ID_WP);
        }

        ImagePredicate(String id, String attributeName) {
            mId = id;
            mAttributeName = attributeName;
        }

        @Override
        public boolean matches(@NotNull Attributes attrs) {
            return attrs.getIndex(mAttributeName) > -1 && attrs.getValue(mAttributeName).equals(mId);
        }
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
        if(!isAdded()) {
            return;
        }
        final MediaType mediaType = mUploadingMedia.get(localMediaId);
        if (mediaType != null) {
            AztecText.AttributePredicate localMediaIdPredicate = ImagePredicate.getLocalMediaIdPredicate(localMediaId);
            content.setOverlayLevel(localMediaIdPredicate, 1, (int)(progress * 10000));
            content.refreshText();
        }
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final String errorMessage) {
        if(!isAdded()) {
            return;
        }
        MediaType mediaType = mUploadingMedia.get(localMediaId);
        if (mediaType != null) {
            switch (mediaType) {
                case IMAGE:
                    AttributesWithClass attributesWithClass = new AttributesWithClass(
                            content.getElementAttributes(ImagePredicate.getLocalMediaIdPredicate(localMediaId)));

                    attributesWithClass.removeClass(ATTR_STATUS_UPLOADING);
                    attributesWithClass.addClass(ATTR_STATUS_FAILED);

                    overlayFailedMedia(localMediaId, attributesWithClass.getAttributes());
                    content.refreshText();
                    break;
                case VIDEO:
                    // TODO: mark media as upload-failed
            }
            mFailedMediaIds.add(localMediaId);
            mUploadingMedia.remove(localMediaId);
        }
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
    }

    /**
     * Hide the action bar if needed.
     */
    private void hideActionBarIfNeeded() {

        ActionBar actionBar = getActionBar();
        if (actionBar != null
                && !isHardwareKeyboardPresent()
                && mHideActionBarOnSoftKeyboardUp
                && mIsKeyboardOpen
                && actionBar.isShowing()) {
            getActionBar().hide();
        }
    }

    /**
     * Show the action bar if needed.
     */
    private void showActionBarIfNeeded() {

        ActionBar actionBar = getActionBar();
        if (actionBar != null && !actionBar.isShowing()) {
            actionBar.show();
        }
    }

    /**
     * Returns true if a hardware keyboard is detected, otherwise false.
     */
    private boolean isHardwareKeyboardPresent() {
        Configuration config = getResources().getConfiguration();
        boolean returnValue = false;
        if (config.keyboard != Configuration.KEYBOARD_NOKEYS) {
            returnValue = true;
        }
        return returnValue;
    }

    private final View.OnDragListener mOnDragListener = new View.OnDragListener() {
        private boolean isSupported(ClipDescription clipDescription, List<String> mimeTypesToCheck) {
            if (clipDescription == null) {
                return false;
            }

            for (String supportedMimeType : mimeTypesToCheck) {
                if (clipDescription.hasMimeType(supportedMimeType)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_TEXT) ||
                            isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE);
                case DragEvent.ACTION_DRAG_ENTERED:
                    // would be nice to start marking the place the item will drop
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    int x = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getX());
                    int y = DisplayUtils.pxToDp(getActivity(), (int) dragEvent.getY());

                    content.setSelection(content.getOffsetForPosition(x, y));
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // clear any drop marking maybe
                    break;
                case DragEvent.ACTION_DROP:
                    if (source.getVisibility() == View.VISIBLE) {
                        if (isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE)) {
                            // don't allow dropping images into the HTML source
                            ToastUtils.showToast(getActivity(), R.string.editor_dropped_html_images_not_allowed,
                                    ToastUtils.Duration.LONG);
                            return true;
                        } else {
                            // let the system handle the text drop
                            return false;
                        }
                    }

                    if (isSupported(dragEvent.getClipDescription(), DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE) &&
                            isTitleFocused()) {
                        // don't allow dropping images into the title field
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_title_images_not_allowed,
                                ToastUtils.Duration.LONG);
                        return true;
                    }

                    if (isAdded()) {
                        mEditorDragAndDropListener.onRequestDragAndDropPermissions(dragEvent);
                    }

                    ClipDescription clipDescription = dragEvent.getClipDescription();
                    if (clipDescription.getMimeTypeCount() < 1) {
                        break;
                    }

                    ContentResolver contentResolver = getActivity().getContentResolver();
                    ArrayList<Uri> uris = new ArrayList<>();
                    boolean unsupportedDropsFound = false;

                    for (int i = 0; i < dragEvent.getClipData().getItemCount(); i++) {
                        ClipData.Item item = dragEvent.getClipData().getItemAt(i);
                        Uri uri = item.getUri();

                        final String uriType = uri != null ? contentResolver.getType(uri) : null;
                        if (uriType != null && DRAGNDROP_SUPPORTED_MIMETYPES_IMAGE.contains(uriType)) {
                            uris.add(uri);
                            continue;
                        } else if (item.getText() != null) {
                            insertTextToEditor(item.getText().toString());
                            continue;
                        } else if (item.getHtmlText() != null) {
                            insertTextToEditor(item.getHtmlText());
                            continue;
                        }

                        // any other drop types are not supported, including web URLs. We cannot proactively
                        // determine their mime type for filtering
                        unsupportedDropsFound = true;
                    }

                    if (unsupportedDropsFound) {
                        ToastUtils.showToast(getActivity(), R.string.editor_dropped_unsupported_files, ToastUtils
                                .Duration.LONG);
                    }

                    if (uris.size() > 0) {
                        mEditorDragAndDropListener.onMediaDropped(uris);
                    }

                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // clear any drop marking maybe
                default:
                    break;
            }
            return true;
        }

        private void insertTextToEditor(String text) {
            if (text != null) {
                content.getText().insert(content.getSelectionStart(), reformatVisually(Utils.escapeHtml(text)));
            } else {
                ToastUtils.showToast(getActivity(), R.string.editor_dropped_text_error, ToastUtils.Duration.SHORT);
                AppLog.d(AppLog.T.EDITOR, "Dropped text was null!");
            }
        }

        private String reformatVisually(String text) {
            // TODO: implement wp.loadText (see wpload.js)
            return text;
        }

        private boolean isTitleFocused() {
            return title.isFocused();
        }
    };

    /**
     * Save post content from source HTML.
     */
    public void saveContentFromSource() {
        if (content != null && source != null && source.getVisibility() == View.VISIBLE) {
            content.fromHtml(source.getPureHtml(false));
        }
    }

    @Override
    public void onToolbarAddMediaClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);

        if (isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        if (source.isFocused()) {
            ToastUtils.showToast(getActivity(), R.string.alert_insert_image_html_mode, ToastUtils.Duration.LONG);
        } else {
            mEditorFragmentListener.onAddMediaClicked();
        }
    }

    @Override
    public void onImageTapped(@NotNull AztecAttributes attrs, int naturalWidth, int naturalHeight) {
        Set<String> classes = MetadataUtils.getClassAttribute(attrs);

        String idName;
        String uploadStatus = "";
        JSONObject meta = MetadataUtils.getMetadata(new AttributesWithClass(attrs), naturalWidth, naturalHeight);
        if (classes.contains(ATTR_STATUS_UPLOADING)) {
            uploadStatus = ATTR_STATUS_UPLOADING;
            idName = ATTR_ID_WP;
        } else if (classes.contains(ATTR_STATUS_FAILED)) {
            uploadStatus = ATTR_STATUS_FAILED;
            idName = ATTR_ID_WP;
        } else {
            idName = ATTR_ID;
        }

        String id = attrs.getValue(idName);

        // generate the element ID if ATTR_ID or ATTR_ID_WP are missing
        if (!attrs.hasAttribute(idName) || TextUtils.isEmpty(attrs.getValue(idName))) {
            idName = TEMP_IMAGE_ID;
            id = UUID.randomUUID().toString();
        }

        attrs.setValue(idName, id);
        mTappedImagePredicate = new ImagePredicate(id, idName);

        onMediaTapped(id, MediaType.IMAGE, meta, uploadStatus);
    }

    public void onMediaTapped(final String localMediaId, final MediaType mediaType, final JSONObject meta, String uploadStatus) {
        if (mediaType == null || !isAdded()) {
            return;
        }

        switch (uploadStatus) {
            case ATTR_STATUS_UPLOADING:
                // Display 'cancel upload' dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.stop_upload_dialog_title));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (mUploadingMedia.containsKey(localMediaId)) {
                            mEditorFragmentListener.onMediaUploadCancelClicked(localMediaId, true);

                            switch (mediaType) {
                                case IMAGE:
                                    content.removeMedia(mTappedImagePredicate);
                                    break;
                                case VIDEO:
                                    // TODO: remove video
                            }
                            mUploadingMedia.remove(localMediaId);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.upload_finished_toast).show();
                        }

                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case ATTR_STATUS_FAILED:
                // Retry media upload
                if (mFailedMediaIds.contains(localMediaId)) {
                    mEditorFragmentListener.onMediaRetryClicked(localMediaId);
                }
                switch (mediaType) {
                    case IMAGE:
                        AttributesWithClass attributesWithClass = new AttributesWithClass(
                                content.getElementAttributes(mTappedImagePredicate));
                        attributesWithClass.removeClass(ATTR_STATUS_FAILED);

                        // set intermediate shade overlay
                        content.setOverlay(mTappedImagePredicate, 0,
                                new ColorDrawable(getResources().getColor(R.color.media_shade_overlay_color)), Gravity.FILL);

                        Drawable progressDrawable = getResources().getDrawable(android.R.drawable.progress_horizontal);
                        // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
                        progressDrawable.setBounds(0, 0, 0, 4);

                        content.setOverlay(mTappedImagePredicate, 1, progressDrawable, Gravity.FILL_HORIZONTAL | Gravity.TOP);
                        content.updateElementAttributes(mTappedImagePredicate, attributesWithClass.getAttributes());

                        content.refreshText();
                        break;
                    case VIDEO:
                        // TODO: unmark video failed
                }
                mFailedMediaIds.remove(localMediaId);
                mUploadingMedia.put(localMediaId, mediaType);
                break;
            default:
                if (!mediaType.equals(MediaType.IMAGE)) {
                    return;
                }

                // Only show image options fragment for image taps
                FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager.findFragmentByTag(ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG) != null) {
                    return;
                }
                mEditorFragmentListener.onTrackableEvent(TrackableEvent.IMAGE_EDITED);
                ImageSettingsDialogFragment imageSettingsDialogFragment = new ImageSettingsDialogFragment();
                imageSettingsDialogFragment.setImageLoader(mImageLoader);
                imageSettingsDialogFragment.setTargetFragment(this,
                        ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_REQUEST_CODE);

                Bundle dialogBundle = new Bundle();

                dialogBundle.putString(EXTRA_MAX_WIDTH, mBlogSettingMaxImageWidth);
                dialogBundle.putBoolean(EXTRA_IMAGE_FEATURED, mFeaturedImageSupported);
                dialogBundle.putBoolean(EXTRA_ENABLED_AZTEC, true);

                try {
                    // Use https:// when requesting the auth header, in case the image is incorrectly using http://
                    // If an auth header is returned, force https:// for the actual HTTP request
                    final String imageSrc = meta.getString(ATTR_SRC);
                    String authHeader = mEditorFragmentListener.onAuthHeaderRequested(UrlUtils.makeHttps(imageSrc));
                    if (authHeader.length() > 0) {
                        meta.put(ATTR_SRC, UrlUtils.makeHttps(imageSrc));
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.EDITOR, "Could not retrieve image url from JSON metadata");
                }

                dialogBundle.putString(EXTRA_IMAGE_META, meta.toString());

                String imageId = JSONUtils.getString(meta, ATTR_ID_ATTACHMENT);
                if (!imageId.isEmpty()) {
                    dialogBundle.putBoolean(EXTRA_FEATURED, mFeaturedImageId == Integer.parseInt(imageId));
                }

                imageSettingsDialogFragment.setArguments(dialogBundle);

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                fragmentTransaction.add(android.R.id.content, imageSettingsDialogFragment,
                        ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG)
                        .addToBackStack(null)
                        .commit();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_REQUEST_CODE) {
            if (mTappedImagePredicate != null) {
                AztecAttributes attributes = content.getElementAttributes(mTappedImagePredicate);
                attributes.removeAttribute(TEMP_IMAGE_ID);

                content.updateElementAttributes(mTappedImagePredicate, attributes);

                if (data == null || data.getExtras() == null) {
                    return;
                }

                Bundle extras = data.getExtras();
                JSONObject meta;

                try {
                    meta = new JSONObject(StringUtils.notNullStr(extras.getString(EXTRA_IMAGE_META)));
                } catch (JSONException e) {
                    return;
                }

                attributes.setValue(ATTR_SRC, JSONUtils.getString(meta, ATTR_SRC));

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_TITLE))) {
                    attributes.setValue(ATTR_TITLE, JSONUtils.getString(meta, ATTR_TITLE));
                }

                attributes.setValue(ATTR_DIMEN_WIDTH, JSONUtils.getString(meta, ATTR_DIMEN_WIDTH));
                attributes.setValue(ATTR_DIMEN_HEIGHT, JSONUtils.getString(meta, ATTR_DIMEN_HEIGHT));

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_ALT))) {
                    attributes.setValue(ATTR_ALT, JSONUtils.getString(meta, ATTR_ALT));
                }

                AttributesWithClass attributesWithClass = new AttributesWithClass(attributes);

                // remove previously set class attributes to add updated values
                attributesWithClass.removeClassStartingWith(ATTR_ALIGN_DASH);
                attributesWithClass.removeClassStartingWith(ATTR_SIZE_DASH);
                attributesWithClass.removeClassStartingWith(ATTR_IMAGE_WP_DASH);

                // only add align attribute if there is no caption since alignment is sent with shortcode
                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_ALIGN)) &&
                        TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_CAPTION))) {
                    attributesWithClass.addClass(ATTR_ALIGN_DASH + JSONUtils.getString(meta, ATTR_ALIGN));
                }

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_SIZE))) {
                    attributesWithClass.addClass(ATTR_SIZE_DASH + JSONUtils.getString(meta, ATTR_SIZE));
                }

                if (!TextUtils.isEmpty(JSONUtils.getString(meta, ATTR_ID_ATTACHMENT))) {
                    attributesWithClass.addClass(ATTR_IMAGE_WP_DASH + JSONUtils.getString(meta, ATTR_ID_ATTACHMENT));
                }

//                TODO: Add shortcode support to allow captions.
//                https://github.com/wordpress-mobile/AztecEditor-Android/issues/17
//                String caption = JSONUtils.getString(meta, ATTR_CAPTION);

//                TODO: Fix issue with image inside link.
//                https://github.com/wordpress-mobile/AztecEditor-Android/issues/196
//                String link = JSONUtils.getString(meta, ATTR_URL_LINK);

                final int imageRemoteId = extras.getInt(ATTR_ID_IMAGE_REMOTE);
                final boolean isFeaturedImage = extras.getBoolean(EXTRA_FEATURED);

                if (imageRemoteId != 0) {
                    if (isFeaturedImage) {
                        mFeaturedImageId = imageRemoteId;
                        mEditorFragmentListener.onFeaturedImageChanged(mFeaturedImageId);
                    } else {
                        // if this image was unset as featured, clear the featured image id
                        if (mFeaturedImageId == imageRemoteId) {
                            mFeaturedImageId = 0;
                            mEditorFragmentListener.onFeaturedImageChanged(mFeaturedImageId);
                        }
                    }
                }

                mTappedImagePredicate = null;
            }
        }
    }

    protected void setAttributeValuesIfNotDefault(AztecAttributes attributes, MediaFile mediaFile) {
        if (mediaFile.getWidth() != DEFAULT_MEDIA_WIDTH) {
            attributes.setValue(ATTR_DIMEN_WIDTH, String.valueOf(mediaFile.getWidth()));
        }

        if (mediaFile.getHeight() != DEFAULT_MEDIA_HEIGHT) {
            attributes.setValue(ATTR_DIMEN_HEIGHT, String.valueOf(mediaFile.getHeight()));
        }
    }
}
