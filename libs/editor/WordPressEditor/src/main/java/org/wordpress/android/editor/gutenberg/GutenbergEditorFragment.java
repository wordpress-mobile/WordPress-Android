package org.wordpress.android.editor.gutenberg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.editor.BuildConfig;
import org.wordpress.android.editor.EditorEditMediaListener;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentActivity;
import org.wordpress.android.editor.EditorImagePreviewListener;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorThemeUpdateListener;
import org.wordpress.android.editor.LiveTextWatcher;
import org.wordpress.android.editor.R;
import org.wordpress.android.editor.WPGutenbergWebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.aztec.IHistoryListener;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergBridgeJS2Parent.GutenbergUserEvent;
import org.wordpress.mobile.WPAndroidGlue.ShowSuggestionsUtil;
import org.wordpress.mobile.WPAndroidGlue.Media;
import org.wordpress.mobile.WPAndroidGlue.MediaOption;
import org.wordpress.mobile.WPAndroidGlue.UnsupportedBlock;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnContentInfoReceivedListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorMountListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentTimeout;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidRequestUnsupportedBlockFallbackListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidSendButtonPressedActionListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnLogGutenbergUserEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachMediaSavingQueryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachMediaUploadQueryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaFilesCollectionBasedBlockEditorListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.wordpress.mobile.WPAndroidGlue.Media.createRNMediaUsingMimeType;

public class GutenbergEditorFragment extends EditorFragmentAbstract implements
        EditorMediaUploadListener,
        IHistoryListener,
        EditorThemeUpdateListener,
        StorySaveMediaListener {
    private static final String GUTENBERG_EDITOR_NAME = "gutenberg";
    private static final String KEY_HTML_MODE_ENABLED = "KEY_HTML_MODE_ENABLED";
    private static final String KEY_EDITOR_DID_MOUNT = "KEY_EDITOR_DID_MOUNT";
    private static final String ARG_IS_NEW_POST = "param_is_new_post";
    private static final String ARG_GUTENBERG_WEB_VIEW_AUTH_DATA = "param_gutenberg_web_view_auth_data";
    private static final String ARG_TENOR_ENABLED = "param_tenor_enabled";
    private static final String ARG_GUTENBERG_PROPS_BUILDER = "param_gutenberg_props_builder";
    private static final String ARG_STORY_EDITOR_REQUEST_CODE = "param_sory_editor_request_code";
    public static final String ARG_STORY_BLOCK_ID = "story_block_id";
    public static final String ARG_STORY_BLOCK_UPDATED_CONTENT = "story_block_updated_content";

    private static final int CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101;
    private static final int CAPTURE_VIDEO_PERMISSION_REQUEST_CODE = 102;

    private static final String MEDIA_SOURCE_FILE = "MEDIA_SOURCE_FILE";
    private static final String MEDIA_SOURCE_AUDIO_FILE = "MEDIA_SOURCE_AUDIO_FILE";
    private static final String MEDIA_SOURCE_STOCK_MEDIA = "MEDIA_SOURCE_STOCK_MEDIA";
    private static final String GIF_MEDIA = "GIF_MEDIA";

    private static final String USER_EVENT_KEY_TEMPLATE = "template";

    private static final int UNSUPPORTED_BLOCK_REQUEST_CODE = 1001;

    private boolean mHtmlModeEnabled;

    private Handler mInvalidateOptionsHandler;
    private Runnable mInvalidateOptionsRunnable;

    private LiveTextWatcher mTextWatcher = new LiveTextWatcher();
    private int mStoryBlockEditRequestCode;

    // pointer (to the Gutenberg container fragment) that outlives this fragment's Android lifecycle. The retained
    //  fragment can be alive and accessible even before it gets attached to an activity.
    //  See discussion at https://github.com/wordpress-mobile/WordPress-Android/pull/9030#issuecomment-459447537 and on.
    GutenbergContainerFragment mRetainedGutenbergContainerFragment;

    private ConcurrentHashMap<String, Float> mUploadingMediaProgressMax = new ConcurrentHashMap<>();
    private Set<String> mFailedMediaIds = new HashSet<>();
    private ConcurrentHashMap<String, Date> mCancelledMediaIds = new ConcurrentHashMap<>();

    private boolean mIsNewPost;
    private boolean mIsJetpackSsoEnabled;

    private boolean mEditorDidMount;
    private GutenbergPropsBuilder mCurrentGutenbergPropsBuilder;
    private boolean mUpdateCapabilitiesOnCreate = false;

    private ProgressDialog mSavingContentProgressDialog;

    public static GutenbergEditorFragment newInstance(String title,
                                                      String content,
                                                      boolean isNewPost,
                                                      GutenbergWebViewAuthorizationData webViewAuthorizationData,
                                                      boolean tenorEnabled,
                                                      GutenbergPropsBuilder gutenbergPropsBuilder,
                                                      int storyBlockEditRequestCode) {
        GutenbergEditorFragment fragment = new GutenbergEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        args.putString(ARG_PARAM_CONTENT, content);
        args.putBoolean(ARG_IS_NEW_POST, isNewPost);
        args.putParcelable(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA, webViewAuthorizationData);
        args.putBoolean(ARG_TENOR_ENABLED, tenorEnabled);
        args.putParcelable(ARG_GUTENBERG_PROPS_BUILDER, gutenbergPropsBuilder);
        args.putInt(ARG_STORY_EDITOR_REQUEST_CODE, storyBlockEditRequestCode);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getGutenbergContainerFragment() == null) {
            GutenbergPropsBuilder gutenbergPropsBuilder = getArguments().getParcelable(ARG_GUTENBERG_PROPS_BUILDER);
            mCurrentGutenbergPropsBuilder = gutenbergPropsBuilder;

            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            GutenbergContainerFragment fragment = GutenbergContainerFragment.newInstance(gutenbergPropsBuilder);
            fragment.setRetainInstance(true);
            fragmentTransaction.add(fragment, GutenbergContainerFragment.TAG);
            fragmentTransaction.commitNow();
        }

        if (mUpdateCapabilitiesOnCreate) {
            getGutenbergContainerFragment().updateCapabilities(mCurrentGutenbergPropsBuilder);
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

        initializeSavingProgressDialog();

        if (getArguments() != null) {
            mIsNewPost = getArguments().getBoolean(ARG_IS_NEW_POST);
            mStoryBlockEditRequestCode = getArguments().getInt(ARG_STORY_EDITOR_REQUEST_CODE);
        }

        ViewGroup gutenbergContainer = view.findViewById(R.id.gutenberg_container);
        getGutenbergContainerFragment().attachToContainer(gutenbergContainer,
                new OnMediaLibraryButtonListener() {
                    @Override public void onMediaLibraryImageButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddMediaImageClicked(allowMultipleSelection);
                    }

                    @Override
                    public void onMediaLibraryVideoButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddMediaVideoClicked(allowMultipleSelection);
                    }

                    @Override
                    public void onMediaLibraryMediaButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddLibraryMediaClicked(allowMultipleSelection);
                    }

                    @Override public void onMediaLibraryFileButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddLibraryFileClicked(allowMultipleSelection);
                    }

                    @Override public void onMediaLibraryAudioButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onTrackableEvent(TrackableEvent.MEDIA_BUTTON_TAPPED);
                        mEditorFragmentListener.onAddLibraryAudioFileClicked(allowMultipleSelection);
                    }

                    @Override
                    public void onUploadPhotoButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onAddPhotoClicked(allowMultipleSelection);
                    }

                    @Override
                    public void onUploadVideoButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onAddVideoClicked(allowMultipleSelection);
                    }

                    @Override
                    public void onUploadMediaButtonClicked(boolean allowMultipleSelection) {
                        mEditorFragmentListener.onAddDeviceMediaClicked(allowMultipleSelection);
                    }

                    @Override
                    public void onCaptureVideoButtonClicked() {
                        checkAndRequestCameraAndStoragePermissions(CAPTURE_VIDEO_PERMISSION_REQUEST_CODE);
                    }

                    @Override
                    public void onCapturePhotoButtonClicked() {
                        checkAndRequestCameraAndStoragePermissions(CAPTURE_PHOTO_PERMISSION_REQUEST_CODE);
                    }

                    @Override
                    public void onRetryUploadForMediaClicked(int mediaId) {
                        showRetryMediaUploadDialog(mediaId);
                    }

                    @Override
                    public void onCancelUploadForMediaClicked(int mediaId) {
                        showCancelMediaUploadDialog(mediaId);
                    }

                    @Override
                    public void onCancelUploadForMediaDueToDeletedBlock(int mediaId) {
                        cancelMediaUploadForDeletedBlock(mediaId);
                    }

                    @Override
                    public ArrayList<MediaOption> onGetOtherMediaImageOptions() {
                        ArrayList<MediaOption> otherMediaImageOptions = initOtherMediaImageOptions();
                        return otherMediaImageOptions;
                    }

                    @Override
                    public ArrayList<MediaOption> onGetOtherMediaFileOptions() {
                        ArrayList<MediaOption> otherMediaFileOptions = initOtherMediaFileOptions();
                        return otherMediaFileOptions;
                    }

                    @Override public ArrayList<MediaOption> onGetOtherMediaAudioFileOptions() {
                        return initOtherMediaAudioFileOptions();
                    }

                    @Override
                    public void onOtherMediaButtonClicked(String mediaSource, boolean allowMultipleSelection) {
                        switch (mediaSource) {
                            case MEDIA_SOURCE_STOCK_MEDIA:
                                mEditorFragmentListener.onAddStockMediaClicked(allowMultipleSelection);
                                break;
                            case GIF_MEDIA:
                                mEditorFragmentListener.onAddGifClicked(allowMultipleSelection);
                                break;
                            case MEDIA_SOURCE_FILE:
                                mEditorFragmentListener.onAddFileClicked(allowMultipleSelection);
                                break;
                            case MEDIA_SOURCE_AUDIO_FILE:
                                mEditorFragmentListener.onAddAudioFileClicked(allowMultipleSelection);
                                break;
                            default:
                                AppLog.e(T.EDITOR,
                                        "Unsupported media source " + mediaSource);
                        }
                    }
                },
                new OnReattachMediaUploadQueryListener() {
                    @Override
                    public void onQueryCurrentProgressForUploadingMedia() {
                        updateFailedMediaState();
                        updateMediaProgress();
                    }
                },
                new OnReattachMediaSavingQueryListener() {
                    @Override public void onQueryCurrentProgressForSavingMedia() {
                        // TODO: probably go through mFailedMediaIds, and see if any block in the post content
                        // has these mediaFIleIds. If there's a match, mark such a block in FAILED state.
                        updateFailedMediaState();
                        updateMediaProgress();
                    }
                },
                new OnEditorMountListener() {
                    @Override
                    public void onEditorDidMount(ArrayList<Object> unsupportedBlocks) {
                        mEditorDidMount = true;
                        mEditorFragmentListener.onEditorFragmentContentReady(unsupportedBlocks);

                        // Hide the progress bar when editor is ready
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                setEditorProgressBarVisibility(!mEditorDidMount);
                            }
                        });
                    }
                },
                mTextWatcher::postTextChanged,
                mEditorFragmentListener::onAuthHeaderRequested,
                mEditorFragmentListener::onPerformFetch,
                mEditorImagePreviewListener::onImagePreviewRequested,
                mEditorEditMediaListener::onMediaEditorRequested,
                new OnLogGutenbergUserEventListener() {
                    @Override
                    public void onGutenbergUserEvent(GutenbergUserEvent event, Map<String, Object> properties) {
                        switch (event) {
                            case EDITOR_SESSION_TEMPLATE_PREVIEW:
                                mEditorFragmentListener.onGutenbergEditorSessionTemplatePreviewTracked((String)
                                        properties.get(USER_EVENT_KEY_TEMPLATE));
                                break;
                            case EDITOR_SESSION_TEMPLATE_APPLY:
                                mEditorFragmentListener.onGutenbergEditorSessionTemplateApplyTracked((String)
                                        properties.get(USER_EVENT_KEY_TEMPLATE));
                                break;
                        }
                    }
                },
                new OnGutenbergDidRequestUnsupportedBlockFallbackListener() {
                    @Override
                    public void gutenbergDidRequestUnsupportedBlockFallback(UnsupportedBlock unsupportedBlock) {
                        openGutenbergWebViewActivity(
                                unsupportedBlock.getContent(),
                                unsupportedBlock.getId(),
                                unsupportedBlock.getName(),
                                unsupportedBlock.getTitle()
                        );
                    }
                },
                new OnGutenbergDidSendButtonPressedActionListener() {
                    @Override
                    public void gutenbergDidSendButtonPressedAction(String buttonType) {
                        mEditorFragmentListener.showJetpackSettings();
                    }
                },

                new ShowSuggestionsUtil() {
                    @Override public void showUserSuggestions(Consumer<String> onResult) {
                        mEditorFragmentListener.showUserSuggestions(onResult);
                    }

                    @Override public void showXpostSuggestions(Consumer<String> onResult) {
                        mEditorFragmentListener.showXpostSuggestions(onResult);
                    }
                },

                new OnMediaFilesCollectionBasedBlockEditorListener() {
                    @Override public void onRequestMediaFilesEditorLoad(ArrayList<Object> mediaFiles, String blockId) {
                        mEditorFragmentListener.onStoryComposerLoadRequested(mediaFiles, blockId);
                    }

                    @Override public void onCancelUploadForMediaCollection(ArrayList<Object> mediaFiles) {
                        showCancelMediaCollectionUploadDialog(mediaFiles);
                    }

                    @Override public void onRetryUploadForMediaCollection(ArrayList<Object> mediaFiles) {
                        showRetryMediaCollectionUploadDialog(mediaFiles);
                    }

                    @Override public void onCancelSaveForMediaCollection(ArrayList<Object> mediaFiles) {
                        showCancelMediaCollectionSaveDialog(mediaFiles);
                    }
                },
                GutenbergUtils.isDarkMode(getActivity()));

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

    private void initializeSavingProgressDialog() {
        if (mEditorFragmentListener != null) {
            mEditorFragmentListener.getSavingInProgressDialogVisibility()
                                   .observe(getViewLifecycleOwner(), visibility -> {
                                       if (DialogVisibility.Showing == visibility) {
                                           showSavingProgressDialogIfNeeded();
                                       } else {
                                           hideSavingProgressDialog();
                                       }
                                   });
        }
    }

    private void openGutenbergWebViewActivity(String content, String blockId, String blockName, String blockTitle) {
        GutenbergWebViewAuthorizationData gutenbergWebViewAuthData =
                getArguments().getParcelable(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA);

        // There is a chance that isJetpackSsoEnabled has changed on the server
        // so we need to make sure that we have fresh value of it.
        gutenbergWebViewAuthData.setJetpackSsoEnabled(mIsJetpackSsoEnabled);

        Intent intent = new Intent(getActivity(), WPGutenbergWebViewActivity.class);
        intent.putExtra(WPGutenbergWebViewActivity.ARG_BLOCK_ID, blockId);
        intent.putExtra(WPGutenbergWebViewActivity.ARG_BLOCK_TITLE, blockTitle);
        intent.putExtra(WPGutenbergWebViewActivity.ARG_BLOCK_CONTENT, content);
        intent.putExtra(WPGutenbergWebViewActivity.ARG_GUTENBERG_WEB_VIEW_AUTH_DATA, gutenbergWebViewAuthData);

        startActivityForResult(intent, UNSUPPORTED_BLOCK_REQUEST_CODE);

        HashMap<String, String> properties = new HashMap<>();
        properties.put("block", blockName);
        mEditorFragmentListener.onTrackableEvent(
                TrackableEvent.EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_SHOWN,
                properties);
    }

    private void trackWebViewClosed(String action) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("action", action);
        mEditorFragmentListener.onTrackableEvent(
                TrackableEvent.EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_CLOSED,
                properties);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UNSUPPORTED_BLOCK_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String blockId = data.getStringExtra(WPGutenbergWebViewActivity.ARG_BLOCK_ID);
                String content = data.getStringExtra(WPGutenbergWebViewActivity.ARG_BLOCK_CONTENT);
                getGutenbergContainerFragment().replaceUnsupportedBlock(content, blockId);
                // We need to send latest capabilities as JS side clears them
                getGutenbergContainerFragment().updateCapabilities(mCurrentGutenbergPropsBuilder);
                trackWebViewClosed("save");
            } else {
                trackWebViewClosed("dismiss");
            }
        } else if (requestCode == mStoryBlockEditRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                // handle edited block content
                String blockId = data.getStringExtra(ARG_STORY_BLOCK_ID);
                String updatedBlockContent = data.getStringExtra(ARG_STORY_BLOCK_UPDATED_CONTENT);
                getGutenbergContainerFragment().replaceStoryEditedBlock(updatedBlockContent, blockId);
                // TODO maybe we need to track something here?
            } else {
                // TODO maybe we need to track something here?
            }
        }
    }

    private ArrayList<MediaOption> initOtherMediaImageOptions() {
        ArrayList<MediaOption> otherMediaOptions = new ArrayList<>();

        Bundle arguments = getArguments();
        FragmentActivity activity = getActivity();
        if (activity == null || arguments == null) {
            AppLog.e(T.EDITOR,
                    "Failed to initialize other media options because the activity or getArguments() is null");
            return otherMediaOptions;
        }

        GutenbergWebViewAuthorizationData gutenbergWebViewAuthorizationData =
                arguments.getParcelable(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA);
        boolean supportStockPhotos = gutenbergWebViewAuthorizationData.isSiteUsingWPComRestAPI();
        boolean supportGifs = arguments.getBoolean(ARG_TENOR_ENABLED, false);

        String packageName = activity.getApplication().getPackageName();
        if (supportStockPhotos) {
            int stockMediaResourceId =
                    getResources().getIdentifier("photo_picker_stock_media", "string", packageName);

            otherMediaOptions.add(new MediaOption(MEDIA_SOURCE_STOCK_MEDIA, getString(stockMediaResourceId)));
        }
        if (supportGifs) {
            int gifMediaResourceId =
                    getResources().getIdentifier("photo_picker_gif", "string", packageName);
            otherMediaOptions.add(new MediaOption(GIF_MEDIA, getString(gifMediaResourceId)));
        }

        return otherMediaOptions;
    }

    private ArrayList<MediaOption> initOtherMediaFileOptions() {
        return initOtherMediaFileOptions(MEDIA_SOURCE_FILE);
    }

    private ArrayList<MediaOption> initOtherMediaAudioFileOptions() {
        return initOtherMediaFileOptions(MEDIA_SOURCE_AUDIO_FILE);
    }

    private ArrayList<MediaOption> initOtherMediaFileOptions(String mediaOptionId) {
        ArrayList<MediaOption> otherMediaOptions = new ArrayList<>();

        FragmentActivity activity = getActivity();
        if (activity == null) {
            AppLog.e(T.EDITOR,
                    "Failed to initialize other media options because the activity is null");
            return otherMediaOptions;
        }

        String packageName = activity.getApplication().getPackageName();

        int chooseFileResourceId =
                getResources().getIdentifier("photo_picker_choose_file", "string", packageName);

        otherMediaOptions.add(new MediaOption(mediaOptionId, getString(chooseFileResourceId)));

        return otherMediaOptions;
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
            // upload progress should work on numeric mediaIds only
            if (!TextUtils.isEmpty(mediaId) && TextUtils.isDigitsOnly(mediaId)) {
                getGutenbergContainerFragment().mediaFileUploadFailed(Integer.valueOf(mediaId));
            } else {
                getGutenbergContainerFragment().mediaFileSaveFailed(mediaId);
            }
        }
    }

    private void updateMediaProgress() {
        for (String mediaId : mUploadingMediaProgressMax.keySet()) {
            // upload progress should work on numeric mediaIds only
            if (!TextUtils.isEmpty(mediaId) && TextUtils.isDigitsOnly(mediaId)) {
                getGutenbergContainerFragment().mediaFileUploadProgress(Integer.valueOf(mediaId),
                        mUploadingMediaProgressMax.get(mediaId));
            } else {
                getGutenbergContainerFragment().mediaFileSaveProgress(mediaId,
                        mUploadingMediaProgressMax.get(mediaId));
            }
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
            mUploadingMediaProgressMax.remove(String.valueOf(localMediaId));
        } else {
            // upload has already finished by the time the user deleted the block, so no op
        }
    }

    private void showCancelMediaUploadDialog(final int localMediaId) {
        // Display 'cancel upload' dialog
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.stop_upload_dialog_title));
        builder.setPositiveButton(R.string.stop_upload_dialog_button_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mUploadingMediaProgressMax.containsKey(String.valueOf(localMediaId))) {
                            mEditorFragmentListener.onMediaUploadCancelClicked(String.valueOf(localMediaId));
                            // remove from editor
                            mEditorFragmentListener.onMediaDeleted(String.valueOf(localMediaId));
                            getGutenbergContainerFragment().clearMediaFileURL(localMediaId);
                            mCancelledMediaIds.put(String.valueOf(localMediaId), new Date());
                            mUploadingMediaProgressMax.remove(String.valueOf(localMediaId));
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
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.retry_failed_upload_title));
        String mediaErrorMessage = mEditorFragmentListener.getErrorMessageFromMedia(mediaId);
        if (!TextUtils.isEmpty(mediaErrorMessage)) {
            builder.setMessage(mediaErrorMessage);
        }
        builder.setPositiveButton(R.string.retry_failed_upload_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        mEditorFragmentListener.onMediaRetryAllClicked(mFailedMediaIds);
                    }
                });

        builder.setNegativeButton(R.string.retry_failed_upload_remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                mEditorFragmentListener.onMediaDeleted(String.valueOf(mediaId));
                mFailedMediaIds.remove(String.valueOf(mediaId));
                getGutenbergContainerFragment().clearMediaFileURL(mediaId);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showCancelMediaCollectionUploadDialog(ArrayList<Object> mediaFiles) {
        // Display 'cancel upload' dialog
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.stop_upload_dialog_title));
        builder.setPositiveButton(R.string.stop_upload_dialog_button_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mEditorFragmentListener.onCancelUploadForMediaCollection(mediaFiles);
                        // now signal Gutenberg upload failed, and remove the mediaIds from our tracking map
                        for (Object mediaFile : mediaFiles) {
                            // this conversion is needed to strip off decimals that can come from RN when using int as
                            // string
                            int localMediaId
                                    = StringUtils.stringToInt(
                                            ((HashMap<String, Object>) mediaFile).get("id").toString(), 0);
                            getGutenbergContainerFragment().mediaFileUploadFailed(localMediaId);
                            mUploadingMediaProgressMax.remove(localMediaId);
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

    private void showRetryMediaCollectionUploadDialog(ArrayList<Object> mediaFiles) {
        // Display 'retry upload' dialog
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.retry_failed_upload_title));
        builder.setPositiveButton(R.string.retry_failed_upload_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mEditorFragmentListener.onRetryUploadForMediaCollection(mediaFiles);
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showCancelMediaCollectionSaveDialog(ArrayList<Object> mediaFiles) {
        // Display 'cancel upload' dialog
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.stop_save_dialog_title));
        builder.setMessage(getString(R.string.stop_save_dialog_message));
        builder.setPositiveButton(R.string.stop_save_dialog_ok_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
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

        try {
            mEditorImagePreviewListener = (EditorImagePreviewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorImagePreviewListener");
        }

        try {
            mEditorEditMediaListener = (EditorEditMediaListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditorEditMediaListener");
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

    @Override
    public void onUndo() {
        // Analytics tracking is not available in GB mobile
    }

    @Override
    public void onRedo() {
        // Analytics tracking is not available in GB mobile
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

    public void setJetpackSsoEnabled(boolean jetpackSsoEnabled) {
        mIsJetpackSsoEnabled = jetpackSsoEnabled;
    }

    public void updateCapabilities(GutenbergPropsBuilder gutenbergPropsBuilder) {
        mCurrentGutenbergPropsBuilder = gutenbergPropsBuilder;
        if (isAdded()) {
            getGutenbergContainerFragment().updateCapabilities(gutenbergPropsBuilder);
        } else {
            mUpdateCapabilitiesOnCreate = true;
        }
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

        // Don't switch to HTML mode if currently uploading media
        if (!mUploadingMediaProgressMax.isEmpty() || isActionInProgress()) {
            ToastUtils.showToast(getActivity(), R.string.alert_action_while_uploading, ToastUtils.Duration.LONG);
            return;
        }

        mEditorFragmentListener.onHtmlModeToggledInToolbar();
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
    public CharSequence getTitle() throws EditorFragmentNotAddedException {
        if (!isAdded()) {
            throw new EditorFragmentNotAddedException();
        }
        return getGutenbergContainerFragment().getTitle(new OnGetContentTimeout() {
            @Override public void onGetContentTimeout(InterruptedException ie) {
                AppLog.e(T.EDITOR, ie);
                Thread.currentThread().interrupt();
            }
        });
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
        return getGutenbergContainerFragment().getContent(originalContent, new OnGetContentTimeout() {
            @Override public void onGetContentTimeout(InterruptedException ie) {
                AppLog.e(T.EDITOR, ie);
                Thread.currentThread().interrupt();
            }
        });
    }


    @Override
    public void showContentInfo() throws EditorFragmentNotAddedException {
        if (!isAdded()) {
            throw new EditorFragmentNotAddedException();
        }

        getGutenbergContainerFragment().triggerGetContentInfo(new OnContentInfoReceivedListener() {
            @Override
            public void onContentInfoFailed() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ToastUtils.showToast(getActivity(), R.string.toast_content_info_failed);
                    });
                }
            }

            @Override
            public void onEditorNotReady() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ToastUtils.showToast(getActivity(), R.string.toast_content_info_editor_not_ready);
                    });
                }
            }

            @Override
            public void onContentInfoReceived(HashMap<String, Object> contentInfo) {
                int blockCount = (int) Double.parseDouble(contentInfo.get("blockCount").toString());
                int wordCount = (int) Double.parseDouble(contentInfo.get("wordCount").toString());
                int charCount = (int) Double.parseDouble(contentInfo.get("characterCount").toString());

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
                        builder.setTitle(getString(R.string.dialog_content_info_title));
                        builder.setMessage(
                                getString(R.string.dialog_content_info_body, blockCount, wordCount, charCount));
                        builder.setPositiveButton(getString(R.string.dialog_button_ok), null);
                        builder.show();
                    });
                }
            }
        });
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

        ArrayList<Media> rnMediaList = new ArrayList<>();

        // Get media URL of first of media first to check if it is network or local one.
        String mediaUrl = "";
        Object[] mediaUrls = mediaList.keySet().toArray();
        if (mediaUrls != null && mediaUrls.length > 0) {
            mediaUrl = (String) mediaUrls[0];
        }

        boolean isNetworkUrl = URLUtil.isNetworkUrl(mediaUrl);
        if (!isNetworkUrl) {
            for (Media media : rnMediaList) {
                mUploadingMediaProgressMax.put(String.valueOf(media.getId()), 0f);
            }
        }

        for (Map.Entry<String, MediaFile> mediaEntry : mediaList.entrySet()) {
            int mediaId = isNetworkUrl ? Integer.valueOf(mediaEntry.getValue().getMediaId())
                    : mediaEntry.getValue().getId();
            String url = isNetworkUrl ? mediaEntry.getKey() : "file://" + mediaEntry.getKey();
            rnMediaList.add(createRNMediaUsingMimeType(mediaId,
                    url,
                    mediaEntry.getValue().getMimeType(),
                    mediaEntry.getValue().getCaption(),
                    mediaEntry.getValue().getTitle()));
        }

        getGutenbergContainerFragment().appendMediaFiles(rnMediaList);
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

    private boolean showSavingProgressDialogIfNeeded() {
        if (!isAdded()) {
            return false;
        }

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

    private boolean hideSavingProgressDialog() {
        if (mSavingContentProgressDialog != null && mSavingContentProgressDialog.isShowing()) {
            mSavingContentProgressDialog.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        hideSavingProgressDialog();
        super.onDestroy();
    }

    @Override public void mediaSelectionCancelled() {
        getGutenbergContainerFragment().mediaSelectionCancelled();
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
        if (!mCancelledMediaIds.containsKey(localMediaId)) {
            mUploadingMediaProgressMax.put(localMediaId, progress);
            getGutenbergContainerFragment().mediaFileUploadProgress(Integer.valueOf(localMediaId), progress);
        } else {
            // checks to ensure that its been two seconds since the last progress event and if so then
            // we treat the event as a new one and remove it from the cancelled media IDs being tracked.
            Date startTime = mCancelledMediaIds.get(localMediaId);
            if (DateTimeUtils.secondsBetween(startTime, new Date()) > 2) {
                mCancelledMediaIds.remove(localMediaId);
            }
        }
    }

    @Override
    public void onMediaUploadFailed(final String localMediaId) {
        getGutenbergContainerFragment().mediaFileUploadFailed(Integer.valueOf(localMediaId));
        mFailedMediaIds.add(localMediaId);
        mUploadingMediaProgressMax.remove(localMediaId);
    }

    @Override
    public void onGalleryMediaUploadSucceeded(final long galleryId, long remoteMediaId, int remaining) {
    }

    @Override
    public void onEditorThemeUpdated(Bundle editorTheme) {
        getGutenbergContainerFragment().updateTheme(editorTheme);
    }

    @Override public void onMediaSaveReattached(String localId, float currentProgress) {
        mUploadingMediaProgressMax.put(localId, currentProgress);
        getGutenbergContainerFragment().mediaFileSaveProgress(localId, currentProgress);
    }

    @Override public void onMediaSaveSucceeded(String localId, String mediaUrl) {
        mUploadingMediaProgressMax.remove(localId);
        getGutenbergContainerFragment().mediaFileSaveSucceeded(localId, mediaUrl);
    }

    @Override public void onMediaSaveProgress(String localId, float progress) {
        mUploadingMediaProgressMax.put(localId, progress);
        getGutenbergContainerFragment().mediaFileSaveProgress(localId, progress);
    }

    @Override public void onMediaSaveFailed(String localId) {
        getGutenbergContainerFragment().mediaFileSaveFailed(localId);
        mFailedMediaIds.add(localId);
        mUploadingMediaProgressMax.remove(localId);
    }

    @Override public void onStorySaveResult(String storyFirstMediaId, boolean success) {
        if (!success) {
            mFailedMediaIds.add(storyFirstMediaId);
            mUploadingMediaProgressMax.remove(storyFirstMediaId);
        }
        getGutenbergContainerFragment().onStorySaveResult(storyFirstMediaId, success);
    }

    @Override public void onMediaModelCreatedForFile(String oldId, String newId, String oldUrl) {
        getGutenbergContainerFragment().onMediaModelCreatedForFile(oldId, newId, oldUrl);
    }

    @Override
    public void showNotice(String message) {
        getGutenbergContainerFragment().showNotice(message);
    }
}
