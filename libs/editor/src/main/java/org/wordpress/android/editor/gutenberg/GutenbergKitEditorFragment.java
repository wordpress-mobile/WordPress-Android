package org.wordpress.android.editor.gutenberg;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.android.volley.toolbox.ImageLoader;
import com.google.gson.Gson;

import org.wordpress.android.editor.BuildConfig;
import org.wordpress.android.editor.EditorEditMediaListener;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentActivity;
import org.wordpress.android.editor.EditorImagePreviewListener;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorThemeUpdateListener;
import org.wordpress.android.editor.LiveTextWatcher;
import org.wordpress.android.editor.R;
import org.wordpress.android.editor.gutenberg.GutenbergDialogFragment.GutenbergDialogNegativeClickInterface;
import org.wordpress.android.editor.gutenberg.GutenbergDialogFragment.GutenbergDialogPositiveClickInterface;
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.gutenberg.GutenbergView;
import org.wordpress.gutenberg.GutenbergView.ContentChangeListener;
import org.wordpress.gutenberg.GutenbergView.HistoryChangeListener;
import org.wordpress.gutenberg.GutenbergView.LogJsExceptionListener;
import org.wordpress.gutenberg.GutenbergView.OpenMediaLibraryListener;
import org.wordpress.gutenberg.GutenbergView.TitleAndContentCallback;
import org.wordpress.gutenberg.GutenbergWebViewPool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.wordpress.gutenberg.Media.createMediaUsingMimeType;

public class GutenbergKitEditorFragment extends EditorFragmentAbstract implements
        EditorMediaUploadListener,
        IHistoryListener,
        EditorThemeUpdateListener,
        GutenbergDialogPositiveClickInterface,
        GutenbergDialogNegativeClickInterface,
        GutenbergNetworkConnectionListener {
    @Nullable private GutenbergView mGutenbergView;
    private static final String GUTENBERG_EDITOR_NAME = "gutenberg";
    private static final String KEY_HTML_MODE_ENABLED = "KEY_HTML_MODE_ENABLED";
    private static final String KEY_EDITOR_DID_MOUNT = "KEY_EDITOR_DID_MOUNT";
    private static final String ARG_IS_NEW_POST = "param_is_new_post";
    private static final String ARG_GUTENBERG_WEB_VIEW_AUTH_DATA = "param_gutenberg_web_view_auth_data";
    public static final String ARG_FEATURED_IMAGE_ID = "featured_image_id";
    public static final String ARG_JETPACK_FEATURES_ENABLED = "jetpack_features_enabled";
    public static final String ARG_GUTENBERG_KIT_SETTINGS = "gutenberg_kit_settings";

    private static final int CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101;
    private static final int CAPTURE_VIDEO_PERMISSION_REQUEST_CODE = 102;

    private boolean mHtmlModeEnabled;

    private final LiveTextWatcher mTextWatcher = new LiveTextWatcher();
    @Nullable private ContentChangeListener mContentChangeListener = null;
    @Nullable private HistoryChangeListener mHistoryChangeListener = null;
    @Nullable private OpenMediaLibraryListener mOpenMediaLibraryListener = null;
    @Nullable private LogJsExceptionListener mOnLogJsExceptionListener = null;

    private boolean mEditorDidMount;

    @Nullable private static Map<String, Object> mSettings;

    public static GutenbergKitEditorFragment newInstance(Context context,
                                                         boolean isNewPost,
                                                         GutenbergWebViewAuthorizationData webViewAuthorizationData,
                                                         boolean jetpackFeaturesEnabled,
                                                         @Nullable Map<String, Object> settings) {
        GutenbergKitEditorFragment fragment = new GutenbergKitEditorFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_NEW_POST, isNewPost);
        args.putBoolean(ARG_JETPACK_FEATURES_ENABLED, jetpackFeaturesEnabled);
        args.putSerializable(ARG_GUTENBERG_KIT_SETTINGS, (Serializable) settings);
        fragment.setArguments(args);
        SavedInstanceDatabase db = SavedInstanceDatabase.Companion.getDatabase(context);
        mSettings = settings;
        if (db != null) {
            db.addParcel(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA, webViewAuthorizationData);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");

        if (savedInstanceState != null) {
            mHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED);
            mEditorDidMount = savedInstanceState.getBoolean(KEY_EDITOR_DID_MOUNT);
            mFeaturedImageId = savedInstanceState.getLong(ARG_FEATURED_IMAGE_ID);
        }
    }

    @SuppressWarnings("MethodLength")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            mSettings = (Map<String, Object>) getArguments().getSerializable(ARG_GUTENBERG_KIT_SETTINGS);
        }

        // request dependency injection. Do this after setting min/max dimensions
        if (getActivity() instanceof EditorFragmentActivity) {
            ((EditorFragmentActivity) getActivity()).initializeEditorFragment();
        }

        mGutenbergView = GutenbergWebViewPool.getPreloadedWebView(requireContext());
        mGutenbergView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        mGutenbergView.setOnFileChooserRequestedListener((intent, requestCode) -> {
            startActivityForResult(intent, requestCode);
            return null;
        });
        mGutenbergView.setContentChangeListener(mContentChangeListener);
        mGutenbergView.setHistoryChangeListener(mHistoryChangeListener);
        mGutenbergView.setOpenMediaLibraryListener(mOpenMediaLibraryListener);
        mGutenbergView.setLogJsExceptionListener(mOnLogJsExceptionListener);
        mGutenbergView.setEditorDidBecomeAvailable(view -> {
            mEditorFragmentListener.onEditorFragmentContentReady(new ArrayList<>(), false);
        });

        Integer postId = (Integer) mSettings.get("postId");
        if (postId != null && postId == 0) {
            postId = -1;
        }
        mGutenbergView.start(
                (String) mSettings.get("siteApiRoot"),
                (String) mSettings.get("siteApiNamespace"),
                (String) mSettings.get("authHeader"),
                (Boolean) mSettings.get("themeStyles"),
                postId,
                (String) mSettings.get("postType"),
                (String) mSettings.get("postTitle"),
                (String) mSettings.get("postContent")
        );

        return mGutenbergView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == mGutenbergView.getPickImageRequestCode()) {
            ValueCallback<Uri[]> filePathCallback = mGutenbergView.getFilePathCallback();

            if (filePathCallback != null) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        Uri[] uris = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            uris[i] = clipData.getItemAt(i).getUri();
                        }
                        filePathCallback.onReceiveValue(uris);
                    } else if (data.getData() != null) {
                        Uri uri = data.getData();
                        filePathCallback.onReceiveValue(new Uri[]{uri});
                    } else {
                        filePathCallback.onReceiveValue(null);
                    }
                } else {
                    filePathCallback.onReceiveValue(null);
                }
                mGutenbergView.resetFilePathCallback();
            }
        }
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (PermissionUtils.checkCameraAndStoragePermissions(this.getActivity())) {
            if (requestCode == CAPTURE_PHOTO_PERMISSION_REQUEST_CODE) {
                mEditorFragmentListener.onCapturePhotoClicked();
            } else if (requestCode == CAPTURE_VIDEO_PERMISSION_REQUEST_CODE) {
                mEditorFragmentListener.onCaptureVideoClicked();
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mEditorDragAndDropListener = (EditorDragAndDropListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement EditorDragAndDropListener");
        }

        try {
            mEditorImagePreviewListener = (EditorImagePreviewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement EditorImagePreviewListener");
        }

        try {
            mEditorEditMediaListener = (EditorEditMediaListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement EditorEditMediaListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_HTML_MODE_ENABLED, mHtmlModeEnabled);
        outState.putBoolean(KEY_EDITOR_DID_MOUNT, mEditorDidMount);
        outState.putLong(ARG_FEATURED_IMAGE_ID, mFeaturedImageId);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_gutenberg, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem debugMenuItem = menu.findItem(R.id.debugmenu);
        debugMenuItem.setVisible(BuildConfig.DEBUG);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onRedoEnabled() {
        // Currently unsupported
    }

    @Override
    public void onUndoEnabled() {
        // Currently unsupported
    }

    @Override
    public void onUndo() {
        // Analytics tracking is not available in GB mobile
    }

    @Override
    public void onRedo() {
        // Analytics tracking is not available in GB mobile
    }

    @Override
    public void setTitle(CharSequence title) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void setContent(CharSequence text) {
        if (text == null) {
            text = "";
        }

        mGutenbergView.setContent((String) text);
    }

    @Override
    public void updateContent(@Nullable CharSequence text) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    public void onToggleHtmlMode() {
        if (!isAdded()) {
            return;
        }

        toggleHtmlMode();
    }

    private void toggleHtmlMode() {
        mHtmlModeEnabled = !mHtmlModeEnabled;
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED);
        mEditorFragmentListener.onHtmlModeToggledInToolbar();
        mGutenbergView.setTextEditorEnabled(mHtmlModeEnabled);
    }

    @Override
    public Pair<CharSequence, CharSequence> getTitleAndContent(CharSequence originalContent) throws
            EditorFragmentNotAddedException {
        final Pair<CharSequence, CharSequence>[] result = new Pair[1];
        final CountDownLatch latch = new CountDownLatch(1);

        mGutenbergView.getTitleAndContent(new TitleAndContentCallback() {
            @Override
            public void onResult(@Nullable String title, @NonNull String content) {
                result[0] = new Pair<>(title, content);
                latch.countDown();
            }
        }, true);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Pair<>("", "");
        }

        return result[0] != null ? result[0] : new Pair<>("", "");
    }

    @NonNull
    @Override
    public String getEditorName() {
        return GUTENBERG_EDITOR_NAME;
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
    public CharSequence getContent(CharSequence originalContent) throws EditorFragmentNotAddedException {
        if (!isAdded()) {
            throw new EditorFragmentNotAddedException();
        }

        return "";
    }

    @Override
    public void showContentInfo() throws EditorFragmentNotAddedException {
        if (!isAdded()) {
            throw new EditorFragmentNotAddedException();
        }
    }

    public void onEditorContentChanged(@NonNull ContentChangeListener listener) {
        mContentChangeListener = listener;
    }

    public void onEditorHistoryChanged(@NonNull HistoryChangeListener listener) {
        mHistoryChangeListener = listener;
    }

    public void onOpenMediaLibrary(@NonNull OpenMediaLibraryListener listener) {
        mOpenMediaLibraryListener = listener;
    }

    public void onLogJsException(@NonNull LogJsExceptionListener listener) {
        mOnLogJsExceptionListener = listener;
    }

    @Override
    public LiveData<Editable> getTitleOrContentChanged() {
        return mTextWatcher.getAfterTextChanged();
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        // noop implementation for shared interface with Aztec
    }

    @Override
    public void appendMediaFiles(Map<String, MediaFile> mediaList) {
        if (getActivity() == null) {
            // appendMediaFile may be called from a background thread (example: EditPostActivity.java#L2165) and
            // Activity may have already be gone.
            // Ticket: https://github.com/wordpress-mobile/WordPress-Android/issues/7386
            AppLog.d(T.MEDIA, "appendMediaFiles() called but Activity is null!");
            return;
        }

        // Get media URL of first of media first to check if it is network or local one.
        String mediaUrl = "";
        Object[] mediaUrls = mediaList.keySet().toArray();
        if (mediaUrls.length > 0) {
            mediaUrl = (String) mediaUrls[0];
        }

        boolean isNetworkUrl = URLUtil.isNetworkUrl(mediaUrl);

        // Disable upload handling until supported--e.g., media shared to the app
        if (mGutenbergView == null || !isNetworkUrl) {
            return;
        }

        ArrayList<org.wordpress.gutenberg.Media> processedMediaList = new ArrayList<>();

        for (Map.Entry<String, MediaFile> mediaEntry : mediaList.entrySet()) {
            int mediaId = Integer.parseInt(mediaEntry.getValue().getMediaId());
            String url = mediaEntry.getKey();
            MediaFile mediaFile = mediaEntry.getValue();
            Bundle metadata = new Bundle();
            String videoPressGuid = mediaFile.getVideoPressGuid();
            if (videoPressGuid != null) {
                metadata.putString("videopressGUID", videoPressGuid);
            }
            processedMediaList.add(createMediaUsingMimeType(mediaId,
                    url,
                    mediaFile.getMimeType(),
                    mediaFile.getCaption(),
                    mediaFile.getTitle(),
                    mediaFile.getAlt()));
        }

        String mediaString = new Gson().toJson(processedMediaList);
        mGutenbergView.setMediaUploadAttachment(mediaString);
    }

    @Override
    public void appendGallery(MediaGallery mediaGallery) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void setUrlForVideoPressId(final String videoId, final String videoUrl, final String posterUrl) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public boolean isUploadingMedia() {
        // Unused, no-op retained for the shared interface with Gutenberg
        return false;
    }

    @Override
    public boolean hasFailedMediaUploads() {
        // Unused, no-op retained for the shared interface with Gutenberg
        return false;
    }

    @Override
    public void removeAllFailedMediaUploads() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void removeMedia(String mediaId) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onDestroy() {
        if (mGutenbergView != null) {
            GutenbergWebViewPool.recycleWebView(mGutenbergView);
            mContentChangeListener = null;
            mHistoryChangeListener = null;
        }
        super.onDestroy();
    }

    @Override public void mediaSelectionCancelled() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onMediaUploadReattached(String localMediaId, float currentProgress) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onMediaUploadRetry(String localMediaId, MediaType mediaType) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onMediaUploadPaused(final String localMediaId) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onEditorThemeUpdated(Bundle editorTheme) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void showNotice(String message) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void showEditorHelp() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override public void onUndoPressed() {
        mGutenbergView.undo();
    }

    @Override public void onRedoPressed() {
        mGutenbergView.redo();
    }

    @Override
    public void onGutenbergDialogPositiveClicked(@NonNull String instanceTag, int mediaId) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onGutenbergDialogNegativeClicked(@NonNull String instanceTag) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    @Override
    public void onConnectionStatusChange(boolean isConnected) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }
}
