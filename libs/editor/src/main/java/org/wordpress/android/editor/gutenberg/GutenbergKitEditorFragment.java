package org.wordpress.android.editor.gutenberg;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

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
import org.wordpress.gutenberg.GutenbergRequestInterceptor;
import org.wordpress.gutenberg.GutenbergView;
import org.wordpress.gutenberg.GutenbergView.HistoryChangeListener;
import org.wordpress.gutenberg.GutenbergView.LogJsExceptionListener;
import org.wordpress.gutenberg.GutenbergView.OpenMediaLibraryListener;
import org.wordpress.gutenberg.GutenbergView.TitleAndContentCallback;
import org.wordpress.gutenberg.GutenbergWebViewPool;
import org.wordpress.gutenberg.EditorConfiguration;
import org.wordpress.gutenberg.WebViewGlobal;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.wordpress.gutenberg.Media.createMediaUsingMimeType;

public class GutenbergKitEditorFragment extends EditorFragmentAbstract implements
        EditorMediaUploadListener,
        IHistoryListener,
        EditorThemeUpdateListener,
        GutenbergDialogPositiveClickInterface,
        GutenbergDialogNegativeClickInterface,
        GutenbergNetworkConnectionListener,
        GutenbergRequestInterceptor {
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
    @Nullable private HistoryChangeListener mHistoryChangeListener = null;
    @Nullable private OpenMediaLibraryListener mOpenMediaLibraryListener = null;
    @Nullable private LogJsExceptionListener mOnLogJsExceptionListener = null;

    private boolean mEditorDidMount;
    @Nullable
    private View mRootView;

    @NonNull private static Map<String, Object> mSettings = Collections.emptyMap();
    private static boolean mIsPrivate = false;
    private static boolean mIsPrivateAtomic = false;
    @NonNull OkHttpClient mHttpClient = new OkHttpClient();

    public static GutenbergKitEditorFragment newInstance(Context context,
            boolean isNewPost,
            @Nullable GutenbergWebViewAuthorizationData webViewAuthorizationData,
            boolean jetpackFeaturesEnabled,
            @NonNull Map<String, Object> settings,
            boolean isPrivate,
            boolean isPrivateAtomic) {
        GutenbergKitEditorFragment fragment = new GutenbergKitEditorFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_NEW_POST, isNewPost);
        args.putBoolean(ARG_JETPACK_FEATURES_ENABLED, jetpackFeaturesEnabled);
        args.putSerializable(ARG_GUTENBERG_KIT_SETTINGS, (Serializable) settings);
        fragment.setArguments(args);
        SavedInstanceDatabase db = SavedInstanceDatabase.Companion.getDatabase(context);
        mSettings = settings;
        mIsPrivate = isPrivate;
        mIsPrivateAtomic = isPrivateAtomic;
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

        mEditorFragmentListener.onEditorFragmentInitialized();

        mRootView = inflater.inflate(R.layout.fragment_gutenberg_kit_editor, container, false);
        ViewGroup gutenbergViewContainer = mRootView.findViewById(R.id.gutenberg_view_container);

        mGutenbergView = GutenbergWebViewPool.getPreloadedWebView(requireContext());
        mGutenbergView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        gutenbergViewContainer.addView(mGutenbergView);

        setEditorProgressBarVisibility(true);

        mGutenbergView.setOnFileChooserRequestedListener((intent, requestCode) -> {
            startActivityForResult(intent, requestCode);
            return null;
        });
        mGutenbergView.setContentChangeListener(mTextWatcher::postTextChanged);
        mGutenbergView.setHistoryChangeListener(mHistoryChangeListener);
        mGutenbergView.setOpenMediaLibraryListener(mOpenMediaLibraryListener);
        mGutenbergView.setLogJsExceptionListener(mOnLogJsExceptionListener);
        mGutenbergView.setEditorDidBecomeAvailable(view -> {
            mEditorDidMount = true;
            mEditorFragmentListener.onEditorFragmentContentReady(new ArrayList<>(), false);
            setEditorProgressBarVisibility(false);
        });
        mGutenbergView.setRequestInterceptor(this);

        return mRootView;
    }

    @Override public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mGutenbergView != null) {
            mGutenbergView.invalidate();
        }
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
        setEditorProgressBarVisibility(!mEditorDidMount);
    }

    private void setEditorProgressBarVisibility(boolean shown) {
        if (isAdded() && mRootView != null) {
            mRootView.findViewById(R.id.editor_progress).setVisibility(shown ? View.VISIBLE : View.GONE);
        }
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
    public @NonNull Pair<CharSequence, CharSequence> getTitleAndContent(@NonNull CharSequence originalContent)
            throws EditorFragmentNotAddedException {
        return getTitleAndContent(originalContent, false);
    }

    public @NonNull Pair<CharSequence, CharSequence> getTitleAndContent(@NonNull CharSequence originalContent,
            boolean completeComposition) throws EditorFragmentNotAddedException {
        final Pair<CharSequence, CharSequence>[] result = new Pair[1];
        final CountDownLatch latch = new CountDownLatch(1);

        mGutenbergView.getTitleAndContent(originalContent, new TitleAndContentCallback() {
            @Override
            public void onResult(@NonNull CharSequence title, @NonNull CharSequence content) {
                result[0] = new Pair<>(title, content);
                latch.countDown();
            }
        }, completeComposition);

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

    public void startWithEditorSettings(@NonNull String editorSettings) {
        if (mGutenbergView == null) {
            return;
        }

        Integer postId = (Integer) mSettings.get("postId");
        if (postId != null && postId == 0) {
            postId = -1;
        }

        EditorConfiguration config = new EditorConfiguration.Builder()
                .setTitle((String) mSettings.get("postTitle"))
                .setContent((String) mSettings.get("postContent"))
                .setPostId(postId)
                .setPostType((String) mSettings.get("postType"))
                .setThemeStyles((Boolean) mSettings.get("themeStyles"))
                .setPlugins((Boolean) mSettings.get("plugins"))
                .setSiteApiRoot((String) mSettings.get("siteApiRoot"))
                .setSiteApiNamespace((String[]) mSettings.get("siteApiNamespace"))
                .setNamespaceExcludedPaths((String[]) mSettings.get("namespaceExcludedPaths"))
                .setAuthHeader((String) mSettings.get("authHeader"))
                .setWebViewGlobals((List<WebViewGlobal>) mSettings.get("webViewGlobals"))
                .setEditorSettings(editorSettings)
                .setLocale((String) mSettings.get("locale"))
                .build();

        mGutenbergView.start(config);
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

    @Override
    public boolean canIntercept(@NonNull WebResourceRequest request) {
        Uri url = request.getUrl();

        return mIsPrivate && isSiteHostedMediaFile(url.toString());
    }

    boolean isSiteHostedMediaFile(@NonNull String urlString) {
        String siteURL = (String) mSettings.get("siteURL");
        Set<String> mediaExtensions = new HashSet<>(Arrays.asList(
                "jpg", "jpeg", "png", "gif", "bmp", "webp",
                "mp4", "mov", "avi", "mkv",
                "mp3", "wav", "flac"
        ));

        try {
            URL url = new URL(urlString);
            URL siteUrlObj = new URL(siteURL);

            // Check if the URL is from the same host as the site URL
            if (!url.getHost().equalsIgnoreCase(siteUrlObj.getHost())) {
                return false;
            }

            // Extract the file name and extension
            String path = url.getPath();
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex == -1) {
                return false;
            }

            String extension = path.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);

            // Check if the extension is in the list of media extensions
            return mediaExtensions.contains(extension);
        } catch (MalformedURLException e) {
            // Handle invalid URLs
            return false;
        }
    }

    @Nullable @Override
    public WebResourceResponse handleRequest(@NonNull WebResourceRequest request) {
        Uri url = request.getUrl();

        String proxyUrl = url.toString();
        if (mIsPrivateAtomic) {
            proxyUrl = getPrivateResourceProxyUrl(url);
        }

        String authHeader = (String) mSettings.get("authHeader");
        if (authHeader == null) {
            return null;
        }

        try {
            Request okHttpRequest = new Builder()
                    .url(proxyUrl)
                    .headers(Headers.of(request.getRequestHeaders()))
                    .addHeader("Authorization", authHeader)
                    .build();

            Response response = mHttpClient.newCall(okHttpRequest).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }

            okhttp3.MediaType contentType = body.contentType();
            if (contentType == null) {
                return null;
            }

            return new WebResourceResponse(
                    contentType.toString(),
                    response.header("content-encoding"),
                    body.byteStream()
            );
        } catch (IOException e) {
            // We don't need to handle this ourselves, just tell the WebView that
            // we weren't able to fetch the resource
            return null;
        }
    }

    private static @NonNull String getPrivateResourceProxyUrl(@NonNull Uri url) {
        String path = url.getPath();
        if (path != null && path.startsWith("/")) {
            path = path.substring(1); // Remove leading '/'
        }

        Uri newUri = new Uri.Builder()
                .scheme("https")
                .authority("public-api.wordpress.com")
                .appendPath("wpcom")
                .appendPath("v2")
                .appendPath("sites")
                .appendPath(url.getAuthority())
                .appendPath("atomic-auth-proxy")
                .appendPath("file")
                .appendEncodedPath(path)
                .build();

        return newUri.toString();
    }
}
