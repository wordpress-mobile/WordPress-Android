package org.wordpress.android.editor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.android.volley.toolbox.ImageLoader;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.AztecText;
import org.wordpress.aztec.Html;
import org.wordpress.aztec.source.SourceViewEditText;
import org.wordpress.aztec.toolbar.AztecToolbar;
import org.wordpress.aztec.toolbar.AztecToolbarClickListener;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AztecEditorFragment extends EditorFragmentAbstract implements OnImeBackListener, EditorMediaUploadListener,
        AztecToolbarClickListener {

    private static final String ARG_PARAM_TITLE = "param_title";
    private static final String ARG_PARAM_CONTENT = "param_content";

    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";

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

    private Map<String, MediaType> mUploadingMedia;
    private Set<String> mFailedMediaIds;

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

        // It seems that hardware accel makes the progressbar in MediaSpan to not show that it updates.
        //  Instead, software rendering works. See: https://github.com/koral--/android-gif-drawable/issues/234#issuecomment-165938445
        content.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        formattingToolbar = (AztecToolbar) view.findViewById(R.id.formatting_toolbar);
        formattingToolbar.setEditor(content, source);
        formattingToolbar.setToolbarListener(this);

        // initialize the text & HTML
        source.history = content.history;
        content.setImageGetter(imageLoader);

        mEditorFragmentListener.onEditorFragmentInitialized();

        content.setOnDragListener(mOnDragListener);
        source.setOnDragListener(mOnDragListener);

        setHasOptionsMenu(true);

        registerForContextMenu(formattingToolbar);

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
        outState.putCharSequence(KEY_TITLE, getTitle());
        outState.putCharSequence(KEY_CONTENT, getContent());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_aztec, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.undo) {
            if (content.getVisibility() == View.VISIBLE) {
                content.undo();
            } else {
                source.undo();
            }

        } else if (item.getItemId() == R.id.redo) {
            if (content.getVisibility() == View.VISIBLE) {
                content.redo();
            } else {
                source.redo();
            }

        }

        return true;
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

        return StringUtils.notNullStr(title.getText().toString().replaceAll("&nbsp;$", ""));
    }

    @Override
    public boolean isActionInProgress() {
        return false;
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

        return content.toHtml(false);
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        final String safeMediaUrl = Utils.escapeQuotes(mediaUrl);

        if (URLUtil.isNetworkUrl(mediaUrl)) {
            String mediaId = mediaFile.getMediaId();
            if (mediaFile.isVideo()) {
                // TODO: insert video
            } else {
                // TODO: insert image
            }
//            mActionStartedAt = System.currentTimeMillis();
        } else {
            String id = mediaFile.getMediaId();
            if (mediaFile.isVideo()) {
                // TODO: insert local video
//                mUploadingMedia.put(id, MediaType.VIDEO);
            } else {
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "data-wpid", "data-wpid", "string", id);
                attrs.addAttribute("", "src", "src", "string", safeMediaUrl);

                Bitmap bitmap = BitmapFactory.decodeFile(safeMediaUrl);

                Drawable progressDrawable = getResources().getDrawable(android.R.drawable.progress_horizontal);
                // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
                progressDrawable.setBounds(0, 0, 0, 2);

                content.insertMedia(new BitmapDrawable(getResources(), bitmap), progressDrawable,
                        Gravity.FILL_HORIZONTAL | Gravity.TOP, attrs);
                mUploadingMedia.put(id, MediaType.IMAGE);
            }
        }
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
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
        final MediaType mediaType = mUploadingMedia.get(localMediaId);
        if (mediaType != null) {
            String remoteUrl = Utils.escapeQuotes(mediaFile.getFileURL());
            if (mediaType.equals(MediaType.IMAGE)) {
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "src", "src", "string", remoteUrl);

                // clear overlay
                content.setOverlay(new LocalImagePredicate(localMediaId), null, 0, attrs);
            } else if (mediaType.equals(MediaType.VIDEO)) {
                // TODO: update video element
            }
        }
    }

    private class LocalImagePredicate implements AztecText.AttributePredicate {
        private String mDataWpid;

        LocalImagePredicate(String dataWpid) {
            mDataWpid = dataWpid;
        }

        @Override
        public boolean matches(@NotNull Attributes attrs) {
            return attrs.getIndex("data-wpid") > -1 && attrs.getValue("data-wpid").equals(mDataWpid);
        }
    };

    @Override
    public void onMediaUploadProgress(final String mediaId, final float progress) {
        final MediaType mediaType = mUploadingMedia.get(mediaId);
        if (mediaType != null) {
            content.setOverlayLevel(new LocalImagePredicate(mediaId), (int)(progress * 10000));
        }
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final String errorMessage) {
        MediaType mediaType = mUploadingMedia.get(localMediaId);
        if (mediaType != null) {
            switch (mediaType) {
                case IMAGE:
                    Drawable alertDrawable = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
                    content.setOverlay(new LocalImagePredicate(localMediaId), alertDrawable, Gravity.CENTER, null);
                    break;
                case VIDEO:
                    // TODO: mark media as upload-failed
            }
            mFailedMediaIds.add(localMediaId);
            mUploadingMedia.remove(localMediaId);
        }
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, String remoteMediaId, int remaining) {
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
            getActivity().openContextMenu(formattingToolbar);
        }
    }
}
