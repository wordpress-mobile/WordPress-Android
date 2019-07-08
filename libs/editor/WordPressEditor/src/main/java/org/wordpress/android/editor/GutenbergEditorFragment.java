package org.wordpress.android.editor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;

import com.android.volley.toolbox.ImageLoader;
import com.facebook.react.bridge.ReadableArray;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnAuthHeaderRequestedListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorMountListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentTimeout;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachQueryListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GutenbergEditorFragment extends EditorFragmentAbstract implements
        EditorMediaUploadListener,
        IHistoryListener {
    private static final String KEY_HTML_MODE_ENABLED = "KEY_HTML_MODE_ENABLED";
    private static final String KEY_EDITOR_DID_MOUNT = "KEY_EDITOR_DID_MOUNT";
    private static final String ARG_IS_NEW_POST = "param_is_new_post";
    private static final String ARG_LOCALE_SLUG = "param_locale_slug";

    private static final int CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101;
    private static final int CAPTURE_VIDEO_PERMISSION_REQUEST_CODE = 102;

    private boolean mHtmlModeEnabled;

    private Handler mInvalidateOptionsHandler;
    private Runnable mInvalidateOptionsRunnable;

    private LiveTextWatcher mTextWatcher = new LiveTextWatcher();

    // pointer (to the Gutenberg container fragment) that outlives this fragment's Android lifecycle. The retained
    //  fragment can be alive and accessible even before it gets attached to an activity.
    //  See discussion at https://github.com/wordpress-mobile/WordPress-Android/pull/9030#issuecomment-459447537 and on.
    GutenbergContainerFragment mRetainedGutenbergContainerFragment;

    private ConcurrentHashMap<String, Float> mUploadingMediaProgressMax = new ConcurrentHashMap<>();
    private Set<String> mFailedMediaIds = new HashSet<>();

    private boolean mIsNewPost;

    private boolean mEditorDidMount;

    private ProgressDialog mSavingContentProgressDialog;

    public static GutenbergEditorFragment newInstance(String title,
                                                      String content,
                                                      boolean isNewPost,
                                                      String localeSlug) {
        GutenbergEditorFragment fragment = new GutenbergEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        args.putBoolean(ARG_IS_NEW_POST, isNewPost);
        args.putString(ARG_LOCALE_SLUG, localeSlug);
        fragment.setArguments(args);
        return fragment;
    }

    private GutenbergContainerFragment getGutenbergContainerFragment() {
        if (mRetainedGutenbergContainerFragment == null) {
            mRetainedGutenbergContainerFragment = (GutenbergContainerFragment) getChildFragmentManager()
                    .findFragmentByTag(GutenbergContainerFragment.TAG);
        } else {
            // Noop. Just use the cached reference. The container fragment might not be attached yet so, getting it from
            // the fragment manager is not reliable. No need either; it's retained and outlives this EditorFragment.
        }

        return mRetainedGutenbergContainerFragment;
    }

    /**
     * Returns the gutenberg-mobile specific translations
     *
     * @return Bundle a map of "english string" => [ "current locale string" ]
     */
    public Bundle getTranslations() {
        Bundle translations = new Bundle();
        Locale defaultLocale = new Locale("en");
        Resources currentResources = getActivity().getApplicationContext().getResources();
        Configuration currentConfiguration = currentResources.getConfiguration();
        // if the current locale of the app is english stop here and return an empty map
        if (currentConfiguration.locale.equals(defaultLocale)) {
            return translations;
        }

        // Let's create a Resources object for the default locale (english) to get the original values for our strings
        DisplayMetrics metrics = new DisplayMetrics();
        Configuration defaultLocaleConfiguration = new Configuration(currentConfiguration);
        defaultLocaleConfiguration.setLocale(defaultLocale);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Resources defaultResources = new Resources(getActivity().getAssets(), metrics, defaultLocaleConfiguration);

        // Strings are only being translated in the WordPress package
        // thus we need to get a reference of the R class for this package
        // Here we assume the Application class is at the same level as the R class
        // It will not work if this lib is used outside of WordPress-Android,
        // in this case let's just return an empty map
        Class<?> rString;
        Package mainPackage = getActivity().getApplication().getClass().getPackage();

        if (mainPackage == null) {
            return translations;
        }

        try {
            rString = getActivity().getApplication().getClassLoader().loadClass(mainPackage.getName() + ".R$string");
        } catch (ClassNotFoundException ex) {
            return translations;
        }

        for (Field stringField : rString.getDeclaredFields()) {
            int resourceId;
            try {
                resourceId = stringField.getInt(rString);
            } catch (IllegalArgumentException | IllegalAccessException iae) {
                AppLog.e(T.EDITOR, iae);
                continue;
            }

            String fieldName = stringField.getName();
            // Filter out all strings that are not prefixed with `gutenberg_mobile_`
            if (!fieldName.startsWith("gutenberg_mobile_")) {
                continue;
            }

            // Add the mapping english => [ translated ] to the bundle if both string are not empty
            String currentResourceString = currentResources.getString(resourceId);
            String defaultResourceString = defaultResources.getString(resourceId);
            if (currentResourceString.length() > 0 && defaultResourceString.length() > 0) {
                translations.putStringArrayList(
                        defaultResourceString,
                        new ArrayList<>(Arrays.asList(currentResourceString))
                );
            }
        }
        return translations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getGutenbergContainerFragment() == null) {
            boolean isNewPost = getArguments().getBoolean(ARG_IS_NEW_POST);
            String localeSlug = getArguments().getString(ARG_LOCALE_SLUG);

            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            GutenbergContainerFragment gutenbergContainerFragment =
                    GutenbergContainerFragment.newInstance(isNewPost, localeSlug, this.getTranslations());
            gutenbergContainerFragment.setRetainInstance(true);
            fragmentTransaction.add(gutenbergContainerFragment, GutenbergContainerFragment.TAG);
            fragmentTransaction.commitNow();
        }

        ProfilingUtils.start("Visual Editor Startup");
        ProfilingUtils.split("EditorFragment.onCreate");

        if (savedInstanceState != null) {
            mHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED);
            mEditorDidMount = savedInstanceState.getBoolean(KEY_EDITOR_DID_MOUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gutenberg_editor, container, false);

        if (getArguments() != null) {
            mIsNewPost = getArguments().getBoolean(ARG_IS_NEW_POST);
        }

        ViewGroup gutenbergContainer = view.findViewById(R.id.gutenberg_container);
        getGutenbergContainerFragment().attachToContainer(gutenbergContainer,
                new OnMediaLibraryButtonListener() {
                    @Override public void onMediaLibraryImageButtonClicked() {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddMediaImageClicked();
                    }

                    @Override
                    public void onMediaLibraryVideoButtonClicked() {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddMediaVideoClicked();
                    }

                    @Override
                    public void onUploadPhotoButtonClicked() {
                        mEditorFragmentListener.onAddPhotoClicked();
                    }

                    @Override
                    public void onUploadVideoButtonClicked() {
                        mEditorFragmentListener.onAddVideoClicked();
                    }

                    @Override
                    public void onCaptureVideoButtonClicked() {
                        checkAndRequestCameraAndStoragePermissions(CAPTURE_VIDEO_PERMISSION_REQUEST_CODE);
                    }

                    @Override
                    public void onCapturePhotoButtonClicked() {
                        checkAndRequestCameraAndStoragePermissions(CAPTURE_PHOTO_PERMISSION_REQUEST_CODE);
                    }

                    @Override public void onRetryUploadForMediaClicked(int mediaId) {
                        showRetryMediaUploadDialog(mediaId);
                    }

                    @Override public void onCancelUploadForMediaClicked(int mediaId) {
                        showCancelMediaUploadDialog(mediaId);
                    }

                    @Override public void onCancelUploadForMediaDueToDeletedBlock(int mediaId) {
                        cancelMediaUploadForDeletedBlock(mediaId);
                    }
                },
                new OnReattachQueryListener() {
                    @Override
                    public void onQueryCurrentProgressForUploadingMedia() {
                        updateFailedMediaState();
                        updateMediaProgress();
                    }
                },
                new OnEditorMountListener() {
                    @Override
                    public void onEditorDidMount(ReadableArray unsupportedBlocks) {
                        mEditorDidMount = true;
                        mEditorFragmentListener.onEditorFragmentContentReady(unsupportedBlocks.size() > 0);

                        // Hide the progress bar when editor is ready
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                setEditorProgressBarVisibility(!mEditorDidMount);
                            }
                        });
                    }
                }, new OnAuthHeaderRequestedListener() {
                    @Override public String onAuthHeaderRequested(String url) {
                        return mEditorFragmentListener.onAuthHeaderRequested(url);
                    }
                });

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

        if (!getGutenbergContainerFragment().hasReceivedAnyContent()) {
            // container is empty, which means it's a fresh instance so, signal to complete its init
            mEditorFragmentListener.onEditorFragmentInitialized();
        }

        if (mIsNewPost) {
            showImplicitKeyboard();
        }

        return view;
    }

    @Override public void onResume() {
        super.onResume();

        setEditorProgressBarVisibility(!mEditorDidMount);
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

    private void setEditorProgressBarVisibility(boolean shown) {
        if (isAdded() && getView() != null) {
            getView().findViewById(R.id.editor_progress).setVisibility(shown ? View.VISIBLE : View.GONE);
        }
    }

    public void resetUploadingMediaToFailed(Set<Integer> failedMediaIds) {
        // get all media failed for this post, and represent it on tje UI
        if (failedMediaIds != null && !failedMediaIds.isEmpty()) {
            for (Integer mediaId : failedMediaIds) {
                // and keep track of failed ids around
                mFailedMediaIds.add(String.valueOf(mediaId));
            }
        }
    }

    private void updateFailedMediaState() {
        for (String mediaId : mFailedMediaIds) {
            getGutenbergContainerFragment().mediaFileUploadFailed(Integer.valueOf(mediaId));
        }
    }

    private void updateMediaProgress() {
        for (String mediaId : mUploadingMediaProgressMax.keySet()) {
            getGutenbergContainerFragment().mediaFileUploadProgress(Integer.valueOf(mediaId),
                    mUploadingMediaProgressMax.get(mediaId));
        }
    }

    private void checkAndRequestCameraAndStoragePermissions(int permissionRequestCode) {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this,
                permissionRequestCode)) {
            if (permissionRequestCode == CAPTURE_PHOTO_PERMISSION_REQUEST_CODE) {
                mEditorFragmentListener.onCapturePhotoClicked();
            } else if (permissionRequestCode == CAPTURE_VIDEO_PERMISSION_REQUEST_CODE) {
                mEditorFragmentListener.onCaptureVideoClicked();
            }
        }
    }

    private void cancelMediaUploadForDeletedBlock(int localMediaId) {
        if (mUploadingMediaProgressMax.containsKey(String.valueOf(localMediaId))) {
            // first make sure to signal deletion
            mEditorFragmentListener.onMediaDeleted(String.valueOf(localMediaId));
            // second also perform a media upload cancel action, through the onMediaUploadCancelClicked interface
            mEditorFragmentListener.onMediaUploadCancelClicked(String.valueOf(localMediaId));
            mUploadingMediaProgressMax.remove(localMediaId);
        } else {
            // upload has already finished by the time the user deleted the block, so no op
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
                            getGutenbergContainerFragment().clearMediaFileURL(localMediaId);
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
                            getGutenbergContainerFragment().mediaFileUploadProgress(mediaId,
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
                getGutenbergContainerFragment().clearMediaFileURL(mediaId);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showImplicitKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_HTML_MODE_ENABLED, mHtmlModeEnabled);
        outState.putBoolean(KEY_EDITOR_DID_MOUNT, mEditorDidMount);
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
            getGutenbergContainerFragment().showDevOptionsDialog();
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

        getGutenbergContainerFragment().setTitle(title.toString());
    }

    @Override
    public void setContent(CharSequence text) {
        if (text == null) {
            text = "";
        }

        String postContent = removeVisualEditorProgressTag(text.toString());
        getGutenbergContainerFragment().setContent(postContent);
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

        getGutenbergContainerFragment().toggleHtmlMode();
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
        return getGutenbergContainerFragment().getTitle(new OnGetContentTimeout() {
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
        return getGutenbergContainerFragment().getContent(originalContent, new OnGetContentTimeout() {
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
            getGutenbergContainerFragment().appendMediaFile(
                    Integer.valueOf(mediaFile.getMediaId()),
                    mediaUrl,
                    mediaFile.isVideo());
        } else {
            getGutenbergContainerFragment().appendUploadMediaFile(
                    mediaFile.getId(),
                    "file://" + mediaUrl,
                    mediaFile.isVideo());
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

    // Getting the content from the HTML editor can take time and the UI seems to be unresponsive.
    // Show a progress dialog for now. Ref: https://github.com/wordpress-mobile/gutenberg-mobile/issues/713
    @Override
    public boolean showSavingProgressDialogIfNeeded() {
        if (!isAdded()) {
            return false;
        }

        if (!mHtmlModeEnabled) return false;

        if (mSavingContentProgressDialog != null && mSavingContentProgressDialog.isShowing()) {
            // Already on the screen? no need to show it again.
            return true;
        }

        if (mSavingContentProgressDialog == null) {
            mSavingContentProgressDialog = new ProgressDialog(getActivity());
            mSavingContentProgressDialog.setCancelable(false);
            mSavingContentProgressDialog.setIndeterminate(true);
            mSavingContentProgressDialog.setMessage(getActivity().getString(R.string.long_post_dlg_saving));
        }
        mSavingContentProgressDialog.show();
       return true;
    }

    @Override
    public boolean hideSavingProgressDialog() {
        if (mSavingContentProgressDialog != null && mSavingContentProgressDialog.isShowing()) {
            mSavingContentProgressDialog.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void onMediaUploadReattached(String localMediaId, float currentProgress) {
        mUploadingMediaProgressMax.put(localMediaId, currentProgress);
        getGutenbergContainerFragment().mediaFileUploadProgress(Integer.valueOf(localMediaId), currentProgress);
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
        getGutenbergContainerFragment().mediaFileUploadSucceeded(Integer.valueOf(localMediaId), mediaFile.getFileURL(),
                Integer.valueOf(mediaFile.getMediaId()));
    }

    @Override
    public void onMediaUploadProgress(final String localMediaId, final float progress) {
        mUploadingMediaProgressMax.put(localMediaId, progress);
        getGutenbergContainerFragment().mediaFileUploadProgress(Integer.valueOf(localMediaId), progress);
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId, final MediaType
            mediaType, final String errorMessage) {
        getGutenbergContainerFragment().mediaFileUploadFailed(Integer.valueOf(localMediaId));
        mFailedMediaIds.add(localMediaId);
        mUploadingMediaProgressMax.remove(localMediaId);
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
    }
}
