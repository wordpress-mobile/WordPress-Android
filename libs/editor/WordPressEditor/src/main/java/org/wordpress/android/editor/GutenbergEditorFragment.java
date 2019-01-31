package org.wordpress.android.editor;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spanned;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;

import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentTimeout;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachQueryListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GutenbergEditorFragment extends EditorFragmentAbstract implements
        View.OnTouchListener,
        EditorMediaUploadListener,
        IHistoryListener {
    private static final String KEY_HTML_MODE_ENABLED = "KEY_HTML_MODE_ENABLED";
    private static final String ARG_IS_NEW_POST = "param_is_new_post";

    private static boolean mIsToolbarExpanded = false;

    private static final int CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101;

    private boolean mEditorWasPaused = false;
    private boolean mHideActionBarOnSoftKeyboardUp = false;
    private boolean mHtmlModeEnabled;

    private Handler mInvalidateOptionsHandler;
    private Runnable mInvalidateOptionsRunnable;

    private LiveTextWatcher mTextWatcher = new LiveTextWatcher();

    private WPAndroidGlueCode mWPAndroidGlueCode;

    private ConcurrentHashMap<String, Float> mUploadingMediaProgressMax = new ConcurrentHashMap<>();
    private Set<String> mFailedMediaIds = new HashSet<>();

    private boolean mIsNewPost;

    public GutenbergEditorFragment() {
        mWPAndroidGlueCode = new WPAndroidGlueCode();
    }

    public static GutenbergEditorFragment newInstance(String title,
                                                      String content,
                                                      boolean isExpanded,
                                                      boolean isNewPost) {
        mIsToolbarExpanded = isExpanded;
        GutenbergEditorFragment fragment = new GutenbergEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        args.putBoolean(ARG_IS_NEW_POST, isNewPost);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWPAndroidGlueCode.onCreate(getContext());

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");

        if (savedInstanceState != null) {
            mHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gutenberg_editor, container, false);

        if (getArguments() != null) {
            mIsNewPost = getArguments().getBoolean(ARG_IS_NEW_POST);
        }

        mWPAndroidGlueCode.onCreateView(
                view.findViewById(R.id.gutenberg),
                mHtmlModeEnabled,
                new OnMediaLibraryButtonListener() {
                    @Override public void onMediaLibraryButtonClicked() {
                        onToolbarMediaButtonClicked();
                    }

                    @Override
                    public void onUploadMediaButtonClicked() {
                        mEditorFragmentListener.onAddPhotoClicked();
                    }

                    @Override
                    public void onCapturePhotoButtonClicked() {
                        checkAndRequestCameraAndStoragePermissions();
                    }

                    @Override public void onRetryUploadForMediaClicked(int mediaId) {
                        showRetryMediaUploadDialog(mediaId);
                    }

                    @Override public void onCancelUploadForMediaClicked(int mediaId) {
                        showCancelMediaUploadDialog(mediaId);
                    }
                },
                new OnReattachQueryListener() {
                    @Override
                    public void onQueryCurrentProgressForUploadingMedia() {
                        updateMediaProgress();
                    }
                },
                getActivity().getApplication(),
                BuildConfig.DEBUG,
                BuildConfig.BUILD_GUTENBERG_FROM_SOURCE,
                mIsNewPost);

        // request dependency injection. Do this after setting min/max dimensions
        if (getActivity() instanceof EditorFragmentActivity) {
            ((EditorFragmentActivity) getActivity()).initializeEditorFragment();
        }

        setHasOptionsMenu(true);

        mInvalidateOptionsHandler = new Handler();
        mInvalidateOptionsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        };

        mEditorFragmentListener.onEditorFragmentInitialized();

        if (mIsNewPost) {
            showImplicitKeyboard();
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAPTURE_PHOTO_PERMISSION_REQUEST_CODE) {
            checkAndRequestCameraAndStoragePermissions();
        }
    }

    public void resetUploadingMediaToFailed(Set<Integer> failedMediaIds) {
        // get all media failed for this post, and represent it on tje UI
        if (failedMediaIds != null && !failedMediaIds.isEmpty()) {
            for (Integer mediaId : failedMediaIds) {
                mWPAndroidGlueCode.mediaFileUploadFailed(mediaId);
            }
        }
    }

    private void updateMediaProgress() {
        for (String mediaId : mUploadingMediaProgressMax.keySet()) {
            mWPAndroidGlueCode.mediaFileUploadProgress(Integer.valueOf(mediaId),
                    mUploadingMediaProgressMax.get(mediaId));
        }
    }
    
    private void checkAndRequestCameraAndStoragePermissions() {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this,
                CAPTURE_PHOTO_PERMISSION_REQUEST_CODE)) {
            mEditorFragmentListener.onCapturePhotoClicked();
        }
    }

    private void showCancelMediaUploadDialog(final int localMediaId) {
        // Display 'cancel upload' dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setTitle(getString(R.string.stop_upload_dialog_title));
        builder.setPositiveButton(R.string.stop_upload_dialog_button_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mUploadingMediaProgressMax.containsKey(String.valueOf(localMediaId))) {
                            mEditorFragmentListener.onMediaUploadCancelClicked(String.valueOf(localMediaId));
                            // remove from editor
                            mEditorFragmentListener.onMediaDeleted(String.valueOf(localMediaId));
                            mWPAndroidGlueCode.clearMediaFileURL(localMediaId);
                            mUploadingMediaProgressMax.remove(localMediaId);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.upload_finished_toast).show();
                        }
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(R.string.stop_upload_dialog_button_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showRetryMediaUploadDialog(final int mediaId) {
        // Display 'retry upload' dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert));
        builder.setTitle(getString(R.string.retry_failed_upload_title));
        builder.setPositiveButton(R.string.retry_failed_upload_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        boolean successfullyRetried = true;
                        if (mFailedMediaIds.contains(String.valueOf(mediaId))) {
                            successfullyRetried = mEditorFragmentListener.onMediaRetryClicked(String.valueOf(mediaId));
                        }
                        if (successfullyRetried) {
                            mFailedMediaIds.remove(String.valueOf(mediaId));
                            mUploadingMediaProgressMax.put(String.valueOf(mediaId), 0f);
                            mWPAndroidGlueCode.mediaFileUploadProgress(mediaId,
                                    mUploadingMediaProgressMax.get(String.valueOf(mediaId)));
                        }
                    }
                });

        builder.setNeutralButton(R.string.retry_failed_upload_retry_all, new OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mEditorFragmentListener.onMediaRetryAllClicked(mFailedMediaIds);
            }
        });

        builder.setNegativeButton(R.string.retry_failed_upload_remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                mEditorFragmentListener.onMediaDeleted(String.valueOf(mediaId));
                mWPAndroidGlueCode.clearMediaFileURL(mediaId);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        mEditorWasPaused = true;

        mWPAndroidGlueCode.onPause(getActivity());
    }

    private void showImplicitKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
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
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        }

        mWPAndroidGlueCode.onResume(this, getActivity());
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
    public void onDestroy() {
        super.onDestroy();
        mWPAndroidGlueCode.onDestroy(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_HTML_MODE_ENABLED, mHtmlModeEnabled);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_gutenberg, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            MenuItem debugMenuItem = menu.findItem(R.id.debugmenu);
            debugMenuItem.setVisible(BuildConfig.DEBUG);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.debugmenu) {
            mWPAndroidGlueCode.showDevOptionsDialog();
            return true;
        }

        return false;
    }

    @Override
    public void onRedoEnabled() {
        if (!isAdded()) {
            return;
        }

        mInvalidateOptionsHandler.removeCallbacks(mInvalidateOptionsRunnable);
        mInvalidateOptionsHandler.postDelayed(mInvalidateOptionsRunnable,
                                              getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    @Override
    public void onUndoEnabled() {
        if (!isAdded()) {
            return;
        }

        mInvalidateOptionsHandler.removeCallbacks(mInvalidateOptionsRunnable);
        mInvalidateOptionsHandler.postDelayed(mInvalidateOptionsRunnable,
                                              getResources().getInteger(android.R.integer.config_mediumAnimTime));
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

    @Override
    public void setTitle(CharSequence title) {
        if (title == null) {
            title = "";
        }

        if (!mWPAndroidGlueCode.hasReactRootView()) {
            return;
        }

        mWPAndroidGlueCode.setTitle(title.toString());
    }

    @Override
    public void setContent(CharSequence text) {
        if (text == null) {
            text = "";
        }

        if (!mWPAndroidGlueCode.hasReactRootView()) {
            return;
        }

        String postContent = removeVisualEditorProgressTag(text.toString());
        mWPAndroidGlueCode.setContent(postContent);
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

        // Don't switch to HTML mode if currently uploading media
        if (!mUploadingMediaProgressMax.isEmpty() || isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        mWPAndroidGlueCode.toggleEditorMode();
    }

    /*
    * TODO: REMOVE THIS ONCE AZTEC COMPLETELY REPLACES THE VISUAL EDITOR IN WPANDROID APP
     */
    private String removeVisualEditorProgressTag(String originalText) {
        // this regex picks any <progress> tags and any opening <span> tags for image containers
        // as produced by the Visual Editor. Note that we don't care about closing </span> tags
        // as the AztecParser takes care of that, and it would be very difficult to accomplish with a
        // regex (and using a proper XML crawler would be particularly overkill)
        if (originalText != null && originalText.contains("<progress")) {
            String regex = "<progress.*?><\\/progress>|<span id=\"img_container.*?"
                           + " class=\"img_container\" contenteditable=\"false\">";
            return originalText.replaceAll(regex, "");
        } else {
            return originalText;
        }
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
        return mWPAndroidGlueCode.getTitle(new OnGetContentTimeout() {
            @Override public void onGetContentTimeout(InterruptedException ie) {
                AppLog.e(T.EDITOR, ie);
                Thread.currentThread().interrupt();
            }
        });
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
    public CharSequence getContent(CharSequence originalContent) {
        return mWPAndroidGlueCode.getContent(originalContent, new OnGetContentTimeout() {
            @Override public void onGetContentTimeout(InterruptedException ie) {
                AppLog.e(T.EDITOR, ie);
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public LiveData<Editable> getTitleOrContentChanged() {
        return mTextWatcher.getAfterTextChanged();
    }

    @Override
    public void appendMediaFile(final MediaFile mediaFile, final String mediaUrl, ImageLoader imageLoader) {
        if (getActivity() == null) {
            // appendMediaFile may be called from a background thread (example: EditPostActivity.java#L2165) and
            // Activity may have already be gone.
            // Ticket: https://github.com/wordpress-mobile/WordPress-Android/issues/7386
            AppLog.d(T.MEDIA, "appendMediaFile() called but Activity is null! mediaUrl: " + mediaUrl);
            return;
        }

        if (URLUtil.isNetworkUrl(mediaUrl)) {
            mWPAndroidGlueCode.appendMediaFile(Integer.valueOf(mediaFile.getMediaId()), mediaUrl);
        } else {
            mWPAndroidGlueCode.appendUploadMediaFile(mediaFile.getId(), "file://" + mediaUrl);
            mUploadingMediaProgressMax.put(String.valueOf(mediaFile.getId()), 0f);
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
        return false;
    }

    @Override
    public boolean hasFailedMediaUploads() {
        return (mFailedMediaIds.size() > 0);
    }

    @Override
    public void removeAllFailedMediaUploads() {
    }

    @Override
    public void removeMedia(String mediaId) {
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
    public void onMediaUploadReattached(String localMediaId, float currentProgress) {
        mUploadingMediaProgressMax.put(localMediaId, currentProgress);
        mWPAndroidGlueCode.mediaFileUploadProgress(Integer.valueOf(localMediaId), currentProgress);
    }

    @Override
    public void onMediaUploadRetry(String localMediaId, MediaType mediaType) {
        if (mFailedMediaIds.contains(localMediaId)) {
            mFailedMediaIds.remove(localMediaId);
            mUploadingMediaProgressMax.put(localMediaId, 0f);
        }

        // TODO request to start the upload again from the UploadService
    }

    @Override
    public void onMediaUploadSucceeded(final String localMediaId, final MediaFile mediaFile) {
        mUploadingMediaProgressMax.remove(localMediaId);
        mWPAndroidGlueCode.mediaFileUploadSucceeded(Integer.valueOf(localMediaId), mediaFile.getFileURL(),
                Integer.valueOf(mediaFile.getMediaId()));
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
        mUploadingMediaProgressMax.put(localMediaId, progress);
        mWPAndroidGlueCode.mediaFileUploadProgress(Integer.valueOf(localMediaId), progress);
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final MediaType
            mediaType, final String errorMessage) {
        mWPAndroidGlueCode.mediaFileUploadFailed(Integer.valueOf(localMediaId));
        mFailedMediaIds.add(localMediaId);
        mUploadingMediaProgressMax.remove(localMediaId);
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Toggle action bar auto-hiding for the new orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                && !getResources().getBoolean(R.bool.is_large_tablet_landscape)) {
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        } else {
            mHideActionBarOnSoftKeyboardUp = false;
            showActionBarIfNeeded();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // In landscape mode, if the title or content view has received a touch event, the keyboard will be
        // displayed and the action bar should hide
        if (event.getAction() == MotionEvent.ACTION_UP
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mHideActionBarOnSoftKeyboardUp = true;
            hideActionBarIfNeeded();
        }
        return false;
    }

    /**
     * Hide the action bar if needed. Don't hide it if
     * - a hardware keyboard is connected.
     * - the soft keyboard is not visible.
     * - it's not visible.
     */
    private void hideActionBarIfNeeded() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        if (!isHardwareKeyboardPresent()
                && mHideActionBarOnSoftKeyboardUp
                && actionBar.isShowing()) {
            getActionBar().hide();
        }
    }

    /**
     * Show the action bar if needed.
     */
    private void showActionBarIfNeeded() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        if (!actionBar.isShowing()) {
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

    public boolean onToolbarMediaButtonClicked() {
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);

        if (isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
        }


        getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEditorFragmentListener.onAddMediaClicked();
                }
            });

        return true;
    }
}
