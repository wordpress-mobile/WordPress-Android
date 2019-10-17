package org.wordpress.android.ui.posts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.ContextThemeWrapper;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorDragAndDropListener;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentNotAddedException;
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.editor.EditorFragmentActivity;
import org.wordpress.android.editor.EditorImageMetaData;
import org.wordpress.android.editor.EditorImageSettingsListener;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorMediaUtils;
import org.wordpress.android.editor.GutenbergEditorFragment;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.editor.MediaToolbarAction;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.Shortcut;
import org.wordpress.android.ui.giphy.GiphyPickerActivity;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.media.MediaSettingsActivity;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon;
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostSettingsCallback;
import org.wordpress.android.ui.posts.InsertMediaDialog.InsertMediaCallback;
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Editor;
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Outcome;
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.PreviewLogicOperationResult;
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction;
import org.wordpress.android.ui.posts.editor.SecondaryEditorAction;
import org.wordpress.android.ui.posts.services.AztecImageLoader;
import org.wordpress.android.ui.posts.services.AztecVideoLoader;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stockmedia.StockMediaPickerActivity;
import org.wordpress.android.ui.uploads.PostEvents;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.uploads.VideoOptimizer;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.ui.utils.UiString.UiStringRes;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutolinkUtils;
import org.wordpress.android.util.CrashLoggingUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.ShortcutUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.BlockEditorEnabledSource;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.widgets.AppRatingDialog;
import org.wordpress.android.widgets.WPSnackbar;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.aztec.util.AztecLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE;
import static org.wordpress.android.ui.history.HistoryDetailContainerFragment.KEY_REVISION;

public class EditPostActivity extends AppCompatActivity implements
        EditorFragmentActivity,
        EditorImageSettingsListener,
        EditorDragAndDropListener,
        EditorFragmentListener,
        MediaToolbarAction.MediaToolbarButtonClickListener,
        OnRequestPermissionsResultCallback,
        PhotoPickerFragment.PhotoPickerListener,
        EditPostSettingsFragment.EditPostActivityHook,
        BasicFragmentDialog.BasicDialogPositiveClickInterface,
        BasicFragmentDialog.BasicDialogNegativeClickInterface,
        PostSettingsListDialogFragment.OnPostSettingsDialogFragmentListener,
        HistoryListFragment.HistoryItemClickInterface,
        EditPostSettingsCallback {
    public static final String EXTRA_POST_LOCAL_ID = "postModelLocalId";
    public static final String EXTRA_LOAD_AUTO_SAVE_REVISION = "loadAutosaveRevision";
    public static final String EXTRA_POST_REMOTE_ID = "postModelRemoteId";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_PROMO = "isPromo";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String EXTRA_HAS_FAILED_MEDIA = "hasFailedMedia";
    public static final String EXTRA_HAS_CHANGES = "hasChanges";
    public static final String EXTRA_IS_DISCARDABLE = "isDiscardable";
    public static final String EXTRA_RESTART_EDITOR = "isSwitchingEditors";
    public static final String EXTRA_INSERT_MEDIA = "insertMedia";
    public static final String EXTRA_IS_NEW_POST = "isNewPost";
    private static final String STATE_KEY_EDITOR_FRAGMENT = "editorFragment";
    private static final String STATE_KEY_DROPPED_MEDIA_URIS = "stateKeyDroppedMediaUri";
    private static final String STATE_KEY_POST_LOCAL_ID = "stateKeyPostModelLocalId";
    private static final String STATE_KEY_POST_REMOTE_ID = "stateKeyPostModelRemoteId";
    private static final String STATE_KEY_POST_LOADING_STATE = "stateKeyPostLoadingState";
    private static final String STATE_KEY_IS_NEW_POST = "stateKeyIsNewPost";
    private static final String STATE_KEY_IS_PHOTO_PICKER_VISIBLE = "stateKeyPhotoPickerVisible";
    private static final String STATE_KEY_HTML_MODE_ON = "stateKeyHtmlModeOn";
    private static final String STATE_KEY_REVISION = "stateKeyRevision";
    private static final String STATE_KEY_EDITOR_SESSION_DATA = "stateKeyEditorSessionData";
    private static final String STATE_KEY_GUTENBERG_IS_SHOWN = "stateKeyGutenbergIsShown";
    private static final String TAG_PUBLISH_CONFIRMATION_DIALOG = "tag_publish_confirmation_dialog";
    private static final String TAG_UPDATE_CONFIRMATION_DIALOG = "tag_update_confirmation_dialog";
    private static final String TAG_FAILED_MEDIA_UPLOADS_DIALOG = "tag_remove_failed_uploads_dialog";
    private static final String TAG_GB_INFORMATIVE_DIALOG = "tag_gb_informative_dialog";

    private static final int PAGE_CONTENT = 0;
    private static final int PAGE_SETTINGS = 1;
    private static final int PAGE_PUBLISH_SETTINGS = 2;
    private static final int PAGE_HISTORY = 3;

    private static final String PHOTO_PICKER_TAG = "photo_picker";
    
    private static final int CHANGE_SAVE_DELAY = 500;
    public static final int MAX_UNSAVED_POSTS = 50;
    private AztecImageLoader mAztecImageLoader;

    enum AddExistingdMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
    }

    enum RestartEditorOptions {
        NO_RESTART,
        RESTART_SUPPRESS_GUTENBERG,
        RESTART_DONT_SUPPRESS_GUTENBERG,
    }
    private RestartEditorOptions mRestartEditorOption = RestartEditorOptions.NO_RESTART;

    private Handler mHandler;
    private int mDebounceCounter = 0;
    private boolean mShowAztecEditor;
    private boolean mShowGutenbergEditor;
    private boolean mMediaInsertedOnCreation;
    private boolean mAllowMultipleSelection;

    private List<String> mPendingVideoPressInfoRequests;
    private List<String> mAztecBackspaceDeletedOrGbBlockDeletedMediaItemIds = new ArrayList<>();
    private List<String> mMediaMarkedUploadingOnStartIds = new ArrayList<>();
    private PostEditorAnalyticsSession mPostEditorAnalyticsSession;
    private boolean mIsConfigChange = false;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    WPViewPager mViewPager;

    private PostModel mPost;
    private PostModel mPostForUndo;
    // mPostSnapshotWhenEditorOpened should not be updated after the post editor session start
    private @Nullable PostModel mPostSnapshotWhenEditorOpened;

    private Revision mRevision;

    private EditorFragmentAbstract mEditorFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditorMediaUploadListener mEditorMediaUploadListener;

    private ProgressDialog mProgressDialog;

    private boolean mIsNewPost;
    private boolean mIsPage;
    private boolean mHasSetPostContent;
    private PostLoadingState mPostLoadingState = PostLoadingState.NONE;


    private enum PostLoadingState {
        NONE(0, ProgressDialogUiState.HiddenProgressDialog.INSTANCE),
        LOADING_REVISION(3, new ProgressDialogUiState.VisibleProgressDialog(
                new UiStringRes(R.string.history_loading_revision),
                false,
                true)),
        UPLOADING_FOR_PREVIEW(4, new ProgressDialogUiState.VisibleProgressDialog(
                new UiStringRes(R.string.post_preview_saving_draft),
                false,
                true)),
        REMOTE_AUTO_SAVING_FOR_PREVIEW(5, new ProgressDialogUiState.VisibleProgressDialog(
                new UiStringRes(R.string.post_preview_remote_auto_saving_post),
                false,
                true)),
        PREVIEWING(6, ProgressDialogUiState.HiddenProgressDialog.INSTANCE),
        REMOTE_AUTO_SAVE_PREVIEW_ERROR(7, ProgressDialogUiState.HiddenProgressDialog.INSTANCE);

        PostLoadingState(int value, ProgressDialogUiState dialogUiState) {
            mValue = value;
            mDialogUiState = dialogUiState;
        }

        private final int mValue;
        private final ProgressDialogUiState mDialogUiState;

        public int getValue() {
            return mValue;
        }

        public ProgressDialogUiState getProgressDialogUiState() {
            return mDialogUiState;
        }

        public static PostLoadingState fromInt(int value) {
            PostLoadingState state = null;

            for (PostLoadingState item : values()) {
                if (item.mValue == value) {
                    state = item;
                    break;
                }
            }

            if (state == null) {
                throw new IllegalArgumentException("PostLoadingState wrong value " + value);
            }
            return state;
        }
    }

    private View mPhotoPickerContainer;
    private PhotoPickerFragment mPhotoPickerFragment;
    private int mPhotoPickerOrientation = Configuration.ORIENTATION_UNDEFINED;

    // For opening the context menu after permissions have been granted
    private View mMenuView = null;

    private boolean mHtmlModeMenuStateOn = false;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject MediaStore mMediaStore;
    @Inject UploadStore mUploadStore;
    @Inject FluxCImageLoader mImageLoader;
    @Inject ShortcutUtils mShortcutUtils;
    @Inject QuickStartStore mQuickStartStore;
    @Inject ImageManager mImageManager;
    @Inject UiHelpers mUiHelpers;
    @Inject RemotePreviewLogicHelper mRemotePreviewLogicHelper;
    @Inject ProgressDialogHelper mProgressDialogHelper;
    @Inject FeaturedImageHelper mFeaturedImageHelper;

    private SiteModel mSite;

    // for keeping the media uri while asking for permissions
    private ArrayList<Uri> mDroppedMediaUris;

    private Runnable mFetchMediaRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDroppedMediaUris != null) {
                final List<Uri> mediaUris = mDroppedMediaUris;
                mDroppedMediaUris = null;
                addMediaList(mediaUris, false);
            }
        }
    };

    public static boolean checkToRestart(@NonNull Intent data) {
        return data.hasExtra(EditPostActivity.EXTRA_RESTART_EDITOR)
               && RestartEditorOptions.valueOf(data.getStringExtra(EditPostActivity.EXTRA_RESTART_EDITOR))
                  != RestartEditorOptions.NO_RESTART;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    private void newPostSetup() {
        mIsNewPost = true;

        if (mSite == null) {
            showErrorAndFinish(R.string.blog_not_found);
            return;
        }
        if (!mSite.isVisible()) {
            showErrorAndFinish(R.string.error_blog_hidden);
            return;
        }

        // Create a new post
        mPost = mPostStore.instantiatePostModel(mSite, mIsPage, null, null);
        mPost.setStatus(PostStatus.DRAFT.toString());
        EventBus.getDefault().postSticky(
                new PostEvents.PostOpenedInEditor(mPost.getLocalSiteId(), mPost.getId()));
        mShortcutUtils.reportShortcutUsed(Shortcut.CREATE_NEW_POST);
    }

    private void createPostEditorAnalyticsSessionTracker(boolean showGutenbergEditor, PostModel post, SiteModel site,
                                                         boolean isNewPost) {
        if (mPostEditorAnalyticsSession == null) {
            mPostEditorAnalyticsSession = new PostEditorAnalyticsSession(
                    showGutenbergEditor ? Editor.GUTENBERG : Editor.CLASSIC,
                    post, site, isNewPost);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        mHandler = new Handler();
        setContentView(R.layout.new_edit_post_activity);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        // FIXME: Make sure to use the latest fresh info about the site we've in the DB
        // set only the editor setting for now.
        if (mSite != null) {
            SiteModel refreshedSite = mSiteStore.getSiteByLocalId(mSite.getId());
            if (refreshedSite != null) {
                mSite.setMobileEditor(refreshedSite.getMobileEditor());
            }
        }

        // Check whether to show the visual editor
        PreferenceManager.setDefaultValues(this, R.xml.account_settings, false);
        mShowAztecEditor = AppPrefs.isAztecEditorEnabled();

        // TODO when aztec is the only editor, remove this part and set the overlay bottom margin in xml
        if (mShowAztecEditor) {
            View overlay = findViewById(R.id.view_overlay);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) overlay.getLayoutParams();
            layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.aztec_format_bar_height);
            overlay.setLayoutParams(layoutParams);
        }

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Bundle extras = getIntent().getExtras();
        String action = getIntent().getAction();
        boolean isRestarting = !RestartEditorOptions.NO_RESTART.name().equals(extras.getString(EXTRA_RESTART_EDITOR));
        if (savedInstanceState == null) {
            if (!getIntent().hasExtra(EXTRA_POST_LOCAL_ID)
                || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action)
                || NEW_MEDIA_POST.equals(action)
                || getIntent().hasExtra(EXTRA_IS_QUICKPRESS)) {
                if (getIntent().hasExtra(EXTRA_QUICKPRESS_BLOG_ID)) {
                    // QuickPress might want to use a different blog than the current blog
                    int localSiteId = getIntent().getIntExtra(EXTRA_QUICKPRESS_BLOG_ID, -1);
                    mSite = mSiteStore.getSiteByLocalId(localSiteId);
                }

                if (extras != null) {
                    mIsPage = extras.getBoolean(EXTRA_IS_PAGE);
                }
                newPostSetup();
            } else if (extras != null) {
                mPost = mPostStore.getPostByLocalPostId(extras.getInt(EXTRA_POST_LOCAL_ID)); // Load post from extras

                if (mPost != null) {
                    if (extras.getBoolean(EXTRA_LOAD_AUTO_SAVE_REVISION)) {
                        mPost.setTitle(TextUtils.isEmpty(mPost.getAutoSaveTitle()) ? mPost.getTitle()
                                : mPost.getAutoSaveTitle());
                        mPost.setContent(TextUtils.isEmpty(mPost.getAutoSaveContent()) ? mPost.getContent()
                                : mPost.getAutoSaveContent());
                        mPost.setExcerpt(TextUtils.isEmpty(mPost.getAutoSaveExcerpt()) ? mPost.getExcerpt()
                                : mPost.getAutoSaveExcerpt());
                    }

                    initializePostObject();
                } else if (isRestarting) {
                    newPostSetup();
                }
            }

            // retrieve Editor session data if switched editors
            if (isRestarting && extras.getSerializable(STATE_KEY_EDITOR_SESSION_DATA) != null) {
                mPostEditorAnalyticsSession =
                        (PostEditorAnalyticsSession) extras.getSerializable(STATE_KEY_EDITOR_SESSION_DATA);
            }
        } else {
            mDroppedMediaUris = savedInstanceState.getParcelable(STATE_KEY_DROPPED_MEDIA_URIS);
            mIsNewPost = savedInstanceState.getBoolean(STATE_KEY_IS_NEW_POST, false);
            updatePostLoadingAndDialogState(PostLoadingState.fromInt(
                    savedInstanceState.getInt(STATE_KEY_POST_LOADING_STATE, 0)));
            mRevision = savedInstanceState.getParcelable(STATE_KEY_REVISION);
            mPostEditorAnalyticsSession =
                    (PostEditorAnalyticsSession) savedInstanceState.getSerializable(STATE_KEY_EDITOR_SESSION_DATA);

            // if we have a remote id saved, let's first try that, as the local Id might have changed after FETCH_POSTS
            if (savedInstanceState.containsKey(STATE_KEY_POST_REMOTE_ID)) {
                mPost = mPostStore.getPostByRemotePostId(savedInstanceState.getLong(STATE_KEY_POST_REMOTE_ID), mSite);
                initializePostObject();
            } else if (savedInstanceState.containsKey(STATE_KEY_POST_LOCAL_ID)) {
                mPost = mPostStore.getPostByLocalPostId(savedInstanceState.getInt(STATE_KEY_POST_LOCAL_ID));
                initializePostObject();
            }

            mEditorFragment =
                    (EditorFragmentAbstract) fragmentManager.getFragment(savedInstanceState, STATE_KEY_EDITOR_FRAGMENT);

            if (mEditorFragment instanceof EditorMediaUploadListener) {
                mEditorMediaUploadListener = (EditorMediaUploadListener) mEditorFragment;
            }
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        // Ensure we have a valid post
        if (mPost == null) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        QuickStartUtils.completeTaskAndRemindNextOne(mQuickStartStore, QuickStartTask.PUBLISH_POST,
                mDispatcher, mSite, this);

        if (mHasSetPostContent = mEditorFragment != null) {
            mEditorFragment.setImageLoader(mImageLoader);
        }

        // Ensure that this check happens when mPost is set
        if (savedInstanceState == null) {
            String restartEditorOptionName = getIntent().getStringExtra(EXTRA_RESTART_EDITOR);
            RestartEditorOptions restartEditorOption =
                    restartEditorOptionName == null ? RestartEditorOptions.RESTART_DONT_SUPPRESS_GUTENBERG
                            : RestartEditorOptions.valueOf(restartEditorOptionName);

            mShowGutenbergEditor = PostUtils.shouldShowGutenbergEditor(mIsNewPost, mPost, mSite)
                                   && restartEditorOption != RestartEditorOptions.RESTART_SUPPRESS_GUTENBERG;
        } else {
            mShowGutenbergEditor = savedInstanceState.getBoolean(STATE_KEY_GUTENBERG_IS_SHOWN);
        }

        // ok now we are sure to have both a valid Post and showGutenberg flag, let's start the editing session tracker
        createPostEditorAnalyticsSessionTracker(mShowGutenbergEditor, mPost, mSite, mIsNewPost);

        // Bump post created analytics only once, first time the editor is opened
        if (mIsNewPost && savedInstanceState == null) {
            trackEditorCreatedPost(action, getIntent());
        }

        if (!mIsNewPost) {
            // if we are opening a Post for which an error notification exists, we need to remove it from the dashboard
            // to prevent the user from tapping RETRY on a Post that is being currently edited
            UploadService.cancelFinalNotification(this, mPost);
            resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued();
        }

        setTitle(SiteUtils.getSiteNameOrHomeURL(mSite));
        mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setPagingEnabled(false);

        // When swiping between different sections, select the corresponding tab. We can also use ActionBar.Tab#select()
        // to do this if we have a reference to the Tab.
        mViewPager.clearOnPageChangeListeners();
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
                if (position == PAGE_CONTENT) {
                    setTitle(SiteUtils.getSiteNameOrHomeURL(mSite));
                } else if (position == PAGE_SETTINGS) {
                    setTitle(mPost.isPage() ? R.string.page_settings : R.string.post_settings);
                    hidePhotoPicker();
                } else if (position == PAGE_PUBLISH_SETTINGS) {
                    setTitle(R.string.publish_date);
                    hidePhotoPicker();
                } else if (position == PAGE_HISTORY) {
                    setTitle(R.string.history_title);
                    hidePhotoPicker();
                }
            }
        });

        ActivityId.trackLastActivity(ActivityId.POST_EDITOR);
    }

    private void initializePostObject() {
        if (mPost != null) {
            mPostSnapshotWhenEditorOpened = mPost.clone();
            mPost = UploadService.updatePostWithCurrentlyCompletedUploads(mPost);
            if (mShowAztecEditor) {
                try {
                    mMediaMarkedUploadingOnStartIds =
                            AztecEditorFragment.getMediaMarkedUploadingInPostContent(this, mPost.getContent());
                    Collections.sort(mMediaMarkedUploadingOnStartIds);
                } catch (NumberFormatException err) {
                    // see: https://github.com/wordpress-mobile/AztecEditor-Android/issues/805
                    if (getSite() != null && getSite().isWPCom() && !getSite().isPrivate()
                            && TextUtils.isEmpty(mPost.getPassword())
                            && !PostStatus.PRIVATE.toString().equals(mPost.getStatus())) {
                        AppLog.e(T.EDITOR, "There was an error initializing post object!");
                        AppLog.e(AppLog.T.EDITOR, "HTML content of the post before the crash:");
                        AppLog.e(AppLog.T.EDITOR, mPost.getContent());
                        throw err;
                    }
                }
            }
            mIsPage = mPost.isPage();

            EventBus.getDefault().postSticky(
                    new PostEvents.PostOpenedInEditor(mPost.getLocalSiteId(), mPost.getId()));

            // run this purge in the background to not delay Editor initialization
            new Thread(this::purgeMediaToPostAssociationsIfNotInPostAnymore).start();
        }
    }

    private void purgeMediaToPostAssociationsIfNotInPostAnymore() {
        boolean useAztec = AppPrefs.isAztecEditorEnabled();
        boolean useGutenberg = AppPrefs.isGutenbergEditorEnabled();

        ArrayList<MediaModel> allMedia = new ArrayList<>();
        allMedia.addAll(mUploadStore.getFailedMediaForPost(mPost));
        allMedia.addAll(mUploadStore.getCompletedMediaForPost(mPost));
        allMedia.addAll(mUploadStore.getUploadingMediaForPost(mPost));

        if (!allMedia.isEmpty()) {
            HashSet<MediaModel> mediaToDeleteAssociationFor = new HashSet<>();
            for (MediaModel media : allMedia) {
                if (useAztec) {
                    if (!AztecEditorFragment.isMediaInPostBody(this,
                            mPost.getContent(), String.valueOf(media.getId()))) {
                        // don't delete featured image uploads
                        if (!media.getMarkedLocallyAsFeatured()) {
                            mediaToDeleteAssociationFor.add(media);
                        }
                    }
                } else if (useGutenberg) {
                    if (!PostUtils.isMediaInGutenbergPostBody(
                            mPost.getContent(), String.valueOf(media.getId()))) {
                        // don't delete featured image uploads
                        if (!media.getMarkedLocallyAsFeatured()) {
                            mediaToDeleteAssociationFor.add(media);
                        }
                    }
                }
            }

            if (!mediaToDeleteAssociationFor.isEmpty()) {
                // also remove the association of Media-to-Post for this post
                ClearMediaPayload clearMediaPayload = new ClearMediaPayload(mPost, mediaToDeleteAssociationFor);
                mDispatcher.dispatch(UploadActionBuilder.newClearMediaForPostAction(clearMediaPayload));
            }
        }
    }

    // this method aims at recovering the current state of media items if they're inconsistent within the PostModel.
    private void resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued() {
        boolean useAztec = AppPrefs.isAztecEditorEnabled();

        if (!useAztec || UploadService.hasPendingOrInProgressMediaUploadsForPost(mPost)) {
            return;
        }

        String oldContent = mPost.getContent();
        if (!AztecEditorFragment.hasMediaItemsMarkedUploading(this, oldContent)
            // we need to make sure items marked failed are still failed or not as well
            && !AztecEditorFragment.hasMediaItemsMarkedFailed(this, oldContent)) {
            return;
        }

        String newContent = AztecEditorFragment.resetUploadingMediaToFailed(this, oldContent);

        if (!TextUtils.isEmpty(oldContent) && newContent != null && oldContent.compareTo(newContent) != 0) {
            mPost.setContent(newContent);

            // we changed the post, so let’s mark this down
            if (!mPost.isLocalDraft()) {
                mPost.setIsLocallyChanged(true);
            }
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }
    }

    private Runnable mSave = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                mDebounceCounter = 0;
                try {
                    updatePostObject(true);
                } catch (EditorFragmentNotAddedException e) {
                    AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
                }
                // make sure we save the post only after the user made some changes
                if (mPostSnapshotWhenEditorOpened == null || !mPostSnapshotWhenEditorOpened.equals(mPost)) {
                    savePostToDb();
                }
            }).start();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        reattachUploadingMediaForAztec();

        // Bump editor opened event every time the activity is resumed, to match the EDITOR_CLOSED event onPause
        PostUtils.trackOpenEditorAnalytics(mPost, mSite);

        mIsConfigChange = false;
    }

    private void reattachUploadingMediaForAztec() {
        if (mEditorFragment instanceof AztecEditorFragment && mEditorMediaUploadListener != null) {
            // UploadService.getPendingMediaForPost will be populated only when the user exits the editor
            // But if the user doesn't exit the editor and sends the app to the background, a reattachment
            // for the media within this Post is needed as soon as the app comes back to foreground,
            // so we get the list of progressing media for this Post from the UploadService
            Set<MediaModel> uploadingMediaInPost = UploadService.getPendingMediaForPost(mPost);
            List<MediaModel> allUploadingMediaInPost = new ArrayList<>(uploadingMediaInPost);
            // add them to the array only if they are not in there yet
            for (MediaModel media1 : UploadService.getPendingOrInProgressMediaUploadsForPost(mPost)) {
                boolean found = false;
                for (MediaModel media2 : uploadingMediaInPost) {
                    if (media1.getId() == media2.getId()) {
                        // if it exists, just break the loop and check for the next one
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // we haven't found it before, so let's add it to the list.
                    allUploadingMediaInPost.add(media1);
                }
            }

            // now do proper re-attachment of upload progress on each media item
            for (MediaModel media : allUploadingMediaInPost) {
                if (media != null) {
                    mEditorMediaUploadListener.onMediaUploadReattached(String.valueOf(media.getId()),
                                                                       UploadService.getUploadProgressForMedia(media));
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);

        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_CLOSED);
    }

    @Override protected void onStop() {
        super.onStop();
        if (mAztecImageLoader != null && isFinishing()) {
            mAztecImageLoader.clearTargets();
            mAztecImageLoader = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (!mIsConfigChange && (mRestartEditorOption == RestartEditorOptions.NO_RESTART)) {
            if (mPostEditorAnalyticsSession != null) {
                mPostEditorAnalyticsSession.end();
            }
        }

        mDispatcher.unregister(this);
        if (mHandler != null) {
            mHandler.removeCallbacks(mSave);
            mHandler = null;
        }
        cancelAddMediaListThread();
        removePostOpenInEditorStickyEvent();
        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).disableContentLogOnCrashes();
        }
        super.onDestroy();
    }

    private void removePostOpenInEditorStickyEvent() {
        PostEvents.PostOpenedInEditor stickyEvent =
                EventBus.getDefault().getStickyEvent(PostEvents.PostOpenedInEditor.class);
        if (stickyEvent != null) {
            // "Consume" the sticky event
            EventBus.getDefault().removeStickyEvent(stickyEvent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves both post objects so we can restore them in onCreate()
        savePostAsync(null);
        outState.putInt(STATE_KEY_POST_LOCAL_ID, mPost.getId());
        if (!mPost.isLocalDraft()) {
            outState.putLong(STATE_KEY_POST_REMOTE_ID, mPost.getRemotePostId());
        }
        outState.putInt(STATE_KEY_POST_LOADING_STATE, mPostLoadingState.getValue());
        outState.putBoolean(STATE_KEY_IS_NEW_POST, mIsNewPost);
        outState.putBoolean(STATE_KEY_IS_PHOTO_PICKER_VISIBLE, isPhotoPickerShowing());
        outState.putBoolean(STATE_KEY_HTML_MODE_ON, mHtmlModeMenuStateOn);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putParcelable(STATE_KEY_REVISION, mRevision);

        outState.putSerializable(STATE_KEY_EDITOR_SESSION_DATA, mPostEditorAnalyticsSession);
        mIsConfigChange = true; // don't call sessionData.end() in onDestroy() if this is an Android config change

        outState.putBoolean(STATE_KEY_GUTENBERG_IS_SHOWN, mShowGutenbergEditor);

        outState.putParcelableArrayList(STATE_KEY_DROPPED_MEDIA_URIS, mDroppedMediaUris);

        if (mEditorFragment != null) {
            getSupportFragmentManager().putFragment(outState, STATE_KEY_EDITOR_FRAGMENT, mEditorFragment);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mHtmlModeMenuStateOn = savedInstanceState.getBoolean(STATE_KEY_HTML_MODE_ON);
        if (savedInstanceState.getBoolean(STATE_KEY_IS_PHOTO_PICKER_VISIBLE, false)) {
            showPhotoPicker();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // resize the photo picker if the user rotated the device
        int orientation = newConfig.orientation;
        if (orientation != mPhotoPickerOrientation) {
            resizePhotoPicker();
        }
    }

    private PrimaryEditorAction getPrimaryAction() {
        return PrimaryEditorAction.getPrimaryAction(PostStatus.fromPost(mPost), UploadUtils.userCanPublish(mSite));
    }

    private String getPrimaryActionText() {
        return getString(getPrimaryAction().getTitleResource());
    }

    private SecondaryEditorAction getSecondaryAction() {
        return SecondaryEditorAction.getSecondaryAction(PostStatus.fromPost(mPost), UploadUtils.userCanPublish(mSite));
    }

    private @Nullable String getSecondaryActionText() {
        @StringRes Integer titleResource = getSecondaryAction().getTitleResource();
        return titleResource != null ? getString(titleResource) : null;
    }

    private boolean isPhotoPickerShowing() {
        return mPhotoPickerContainer != null
               && mPhotoPickerContainer.getVisibility() == View.VISIBLE;
    }

    /*
     * resizes the photo picker based on device orientation - full height in landscape, half
     * height in portrait
     */
    private void resizePhotoPicker() {
        if (mPhotoPickerContainer == null) {
            return;
        }

        if (DisplayUtils.isLandscape(this)) {
            mPhotoPickerOrientation = Configuration.ORIENTATION_LANDSCAPE;
            mPhotoPickerContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            mPhotoPickerOrientation = Configuration.ORIENTATION_PORTRAIT;
            int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
            mPhotoPickerContainer.getLayoutParams().height = (int) (displayHeight * 0.5f);
        }

        if (mPhotoPickerFragment != null) {
            mPhotoPickerFragment.reload();
        }
    }

    /*
     * loads the photo picker fragment, which is hidden until the user taps the media icon
     */
    private void initPhotoPicker() {
        mPhotoPickerContainer = findViewById(R.id.photo_fragment_container);

        // size the picker before creating the fragment to avoid having it load media now
        resizePhotoPicker();

        mPhotoPickerFragment = (PhotoPickerFragment) getSupportFragmentManager().findFragmentByTag(PHOTO_PICKER_TAG);
        if (mPhotoPickerFragment == null) {
            MediaBrowserType mediaBrowserType =
                    mShowAztecEditor ? MediaBrowserType.AZTEC_EDITOR_PICKER : MediaBrowserType.EDITOR_PICKER;
            mPhotoPickerFragment = PhotoPickerFragment.newInstance(this, mediaBrowserType, getSite());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.photo_fragment_container, mPhotoPickerFragment, PHOTO_PICKER_TAG)
                    .commit();
        }
    }

    /*
     * shows/hides the overlay which appears atop the editor, which effectively disables it
     */
    private void showOverlay(boolean animate) {
        View overlay = findViewById(R.id.view_overlay);
        if (animate) {
            AniUtils.fadeIn(overlay, AniUtils.Duration.MEDIUM);
        } else {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideOverlay() {
        View overlay = findViewById(R.id.view_overlay);
        overlay.setVisibility(View.GONE);
    }

    /*
     * user has requested to show the photo picker
     */
    private void showPhotoPicker() {
        boolean isAlreadyShowing = isPhotoPickerShowing();

        // make sure we initialized the photo picker
        if (mPhotoPickerFragment == null) {
            initPhotoPicker();
        }

        // hide soft keyboard
        ActivityUtils.hideKeyboard(this);

        // slide in the photo picker
        if (!isAlreadyShowing) {
            AniUtils.animateBottomBar(mPhotoPickerContainer, true, AniUtils.Duration.MEDIUM);
            mPhotoPickerFragment.refresh();
            mPhotoPickerFragment.setPhotoPickerListener(this);
        }

        // animate in the editor overlay
        showOverlay(true);

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).enableMediaMode(true);
        }
    }

    private void hidePhotoPicker() {
        if (isPhotoPickerShowing()) {
            mPhotoPickerFragment.finishActionMode();
            mPhotoPickerFragment.setPhotoPickerListener(null);
            AniUtils.animateBottomBar(mPhotoPickerContainer, false);
        }

        hideOverlay();

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).enableMediaMode(false);
        }
    }

    private boolean shouldSwitchToGutenbergBeVisible(
            PostModel post,
            EditorFragmentAbstract editorFragment,
            SiteModel site
    ) {
        // Some guard conditions
        if (post == null) {
            AppLog.w(T.EDITOR, "shouldSwitchToGutenbergBeVisible got a null post parameter.");
            return false;
        }

        if (editorFragment == null) {
            AppLog.w(T.EDITOR, "shouldSwitchToGutenbergBeVisible got a null editorFragment parameter.");
            return false;
        }

        // Check whether the content has blocks.
        boolean hasBlocks = false;
        boolean isEmpty = false;
        try {
            final String content = (String) editorFragment.getContent(post.getContent());
            hasBlocks = PostUtils.contentContainsGutenbergBlocks(content);
            isEmpty = TextUtils.isEmpty(content);
        } catch (EditorFragmentNotAddedException e) {
            // legacy exception; just ignore.
        }

        // if content has blocks or empty, offer the switch to Gutenberg. The block editor doesn't have good
        //  "Classic Block" support yet so, don't offer a switch to it if content doesn't have blocks. If the post
        //  is empty but the user hasn't enabled "Use Gutenberg for new posts" in Site Setting,
        //  don't offer the switch.
        return hasBlocks || (SiteUtils.isBlockEditorDefaultForNewPost(site) && isEmpty);
    }

    /*
     * called by PhotoPickerFragment when media is selected - may be a single item or a list of items
     */
    @Override
    public void onPhotoPickerMediaChosen(@NonNull final List<Uri> uriList) {
        hidePhotoPicker();

        if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
            boolean hasSelectedPicture = false;
            for (Uri uri : uriList) {
                if (!MediaUtils.isVideo(uri.toString())) {
                    hasSelectedPicture = true;
                    break;
                }
            }
            if (hasSelectedPicture) {
                WPMediaUtils.advertiseImageOptimization(this, () -> addMediaList(uriList, false));
                return;
            }
        }

        addMediaList(uriList, false);
    }

    @Override
    public void onMediaToolbarButtonClicked(MediaToolbarAction action) {
        if (!isPhotoPickerShowing()) {
            return;
        }

        switch (action) {
            case CAMERA:
                mPhotoPickerFragment.showCameraPopupMenu(findViewById(action.getButtonId()));
                break;
            case GALLERY:
                mPhotoPickerFragment.showPickerPopupMenu(findViewById(action.getButtonId()));
                break;
            case LIBRARY:
                mPhotoPickerFragment.doIconClicked(PhotoPickerIcon.WP_MEDIA);
                break;
        }
    }

    /*
     * called by PhotoPickerFragment when user clicks an icon to launch the camera, native
     * picker, or WP media picker
     */
    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerIcon icon, boolean allowMultipleSelection) {
        hidePhotoPicker();
        mAllowMultipleSelection = allowMultipleSelection;
        switch (icon) {
            case ANDROID_CAPTURE_PHOTO:
                launchCamera();
                break;
            case ANDROID_CAPTURE_VIDEO:
                launchVideoCamera();
                break;
            case ANDROID_CHOOSE_PHOTO:
                launchPictureLibrary();
                break;
            case ANDROID_CHOOSE_VIDEO:
                launchVideoLibrary();
                break;
            case WP_MEDIA:
                ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.EDITOR_PICKER);
                break;
            case STOCK_MEDIA:
                ActivityLauncher.showStockMediaPickerForResult(
                        this, mSite, RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT);
                break;
            case GIPHY:
                ActivityLauncher.showGiphyPickerForResult(this, mSite, RequestCodes.GIPHY_PICKER);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_post, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showMenuItems = true;
        if (mViewPager != null && mViewPager.getCurrentItem() > PAGE_CONTENT) {
            showMenuItems = false;
        }

        MenuItem secondaryAction = menu.findItem(R.id.menu_secondary_action);
        MenuItem previewMenuItem = menu.findItem(R.id.menu_preview_post);
        MenuItem viewHtmlModeMenuItem = menu.findItem(R.id.menu_html_mode);
        MenuItem historyMenuItem = menu.findItem(R.id.menu_history);
        MenuItem settingsMenuItem = menu.findItem(R.id.menu_post_settings);

        if (secondaryAction != null && mPost != null) {
            secondaryAction.setVisible(showMenuItems && getSecondaryAction().isVisible());
            secondaryAction.setTitle(getSecondaryActionText());
        }

        if (previewMenuItem != null) {
            previewMenuItem.setVisible(showMenuItems);
        }

        if (viewHtmlModeMenuItem != null) {
            viewHtmlModeMenuItem.setVisible(((mEditorFragment instanceof AztecEditorFragment)
                                             || (mEditorFragment instanceof GutenbergEditorFragment)) && showMenuItems);
            viewHtmlModeMenuItem.setTitle(mHtmlModeMenuStateOn ? R.string.menu_visual_mode : R.string.menu_html_mode);
        }

        if (historyMenuItem != null) {
            boolean hasHistory = !mIsNewPost && (mSite.isWPCom() || mSite.isJetpackConnected());
            historyMenuItem.setVisible(showMenuItems && hasHistory);
        }

        if (settingsMenuItem != null) {
            settingsMenuItem.setTitle(mIsPage ? R.string.page_settings : R.string.post_settings);
            settingsMenuItem.setVisible(showMenuItems);
        }

        // Set text of the primary action button in the ActionBar
        if (mPost != null) {
            MenuItem primaryAction = menu.findItem(R.id.menu_primary_action);
            if (primaryAction != null) {
                primaryAction.setTitle(getPrimaryActionText());
                primaryAction.setVisible(mViewPager != null && mViewPager.getCurrentItem() != PAGE_HISTORY
                                         && mViewPager.getCurrentItem() != PAGE_PUBLISH_SETTINGS);
            }
        }

        MenuItem switchToAztecMenuItem = menu.findItem(R.id.menu_switch_to_aztec);
        MenuItem switchToGutenbergMenuItem = menu.findItem(R.id.menu_switch_to_gutenberg);

        // The following null checks should basically be redundant but were added to manage
        // an odd behaviour recorded with Android 8.0.0
        // (see https://github.com/wordpress-mobile/WordPress-Android/issues/9748 for more information)
        if (switchToAztecMenuItem != null && switchToGutenbergMenuItem != null) {
            if (mShowGutenbergEditor) {
                // we're showing Gutenberg so, just offer the Aztec switch
                switchToAztecMenuItem.setVisible(true);
                switchToGutenbergMenuItem.setVisible(false);
            } else {
                // we're showing Aztec so, hide the "Switch to Aztec" menu
                switchToAztecMenuItem.setVisible(false);

                switchToGutenbergMenuItem.setVisible(
                        shouldSwitchToGutenbergBeVisible(mPost, mEditorFragment, mSite)
                );
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);

        if (allGranted) {
            switch (requestCode) {
                case WPPermissionUtils.EDITOR_MEDIA_PERMISSION_REQUEST_CODE:
                    if (mMenuView != null) {
                        super.openContextMenu(mMenuView);
                        mMenuView = null;
                    }
                    break;
                case WPPermissionUtils.EDITOR_DRAG_DROP_PERMISSION_REQUEST_CODE:
                    runOnUiThread(mFetchMediaRunnable);
                    break;
            }
        }
    }

    private boolean handleBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
        if (fragment != null && fragment.isVisible()) {
            if (fragment instanceof ImageSettingsDialogFragment) {
                ImageSettingsDialogFragment imFragment = (ImageSettingsDialogFragment) fragment;
                imFragment.dismissFragment();
            }

            return false;
        }

        if (mViewPager.getCurrentItem() == PAGE_PUBLISH_SETTINGS) {
            mViewPager.setCurrentItem(PAGE_SETTINGS);
            invalidateOptionsMenu();
        } else if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
            if (mViewPager.getCurrentItem() == PAGE_SETTINGS) {
                mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
            }

            mViewPager.setCurrentItem(PAGE_CONTENT);
            invalidateOptionsMenu();
        } else if (isPhotoPickerShowing()) {
            hidePhotoPicker();
        } else {
            mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
            savePostAndOptionallyFinish(true);
        }

        return true;
    }

    private RemotePreviewLogicHelper.RemotePreviewHelperFunctions getEditPostActivityStrategyFunctions() {
       return new RemotePreviewLogicHelper.RemotePreviewHelperFunctions() {
            @Override
            public boolean notifyUploadInProgress(@NotNull PostModel post) {
                if (UploadService.hasInProgressMediaUploadsForPost(post)) {
                    ToastUtils.showToast(EditPostActivity.this,
                            getString(R.string.editor_toast_uploading_please_wait), Duration.SHORT);
                    return true;
                } else {
                    return false;
                }
            }

            @Nullable
            @Override
            public PostModel updatePostIfNeeded() {
                updatePostObject();
                return mPost;
            }

            @Override
            public void notifyEmptyDraft() {
                ToastUtils.showToast(EditPostActivity.this,
                        getString(R.string.error_preview_empty_draft), Duration.SHORT);
            }

            @Override
            public void startUploading(boolean isRemoteAutoSave, @Nullable PostModel post) {
                if (isRemoteAutoSave) {
                    updatePostLoadingAndDialogState(PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW, post);
                    savePostAndOptionallyFinish(false, true);
                } else {
                    updatePostLoadingAndDialogState(PostLoadingState.UPLOADING_FOR_PREVIEW, post);
                    savePostAndOptionallyFinish(false);
                }
            }

            @Override
            public void notifyEmptyPost() {
                String message =
                        getString(mIsPage ? R.string.error_preview_empty_page : R.string.error_preview_empty_post);
                ToastUtils.showToast(EditPostActivity.this, message, Duration.SHORT);
            }
        };
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            return handleBackPressed();
        }

        hidePhotoPicker();

        if (itemId == R.id.menu_primary_action) {
            performPrimaryAction();
        } else {
            // Disable other action bar buttons while a media upload is in progress
            // (unnecessary for Aztec since it supports progress reattachment)
            if (!(mShowAztecEditor || mShowGutenbergEditor)
                        && (mEditorFragment.isUploadingMedia() || mEditorFragment.isActionInProgress())) {
                ToastUtils.showToast(this, R.string.editor_toast_uploading_please_wait, Duration.SHORT);
                return false;
            }

            if (itemId == R.id.menu_history) {
                AnalyticsTracker.track(Stat.REVISIONS_LIST_VIEWED);
                ActivityUtils.hideKeyboard(this);
                mViewPager.setCurrentItem(PAGE_HISTORY);
            } else if (itemId == R.id.menu_preview_post) {
                PreviewLogicOperationResult opResult = mRemotePreviewLogicHelper.runPostPreviewLogic(
                        this,
                        mSite,
                        mPost,
                        getEditPostActivityStrategyFunctions());
                if (opResult == PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS
                    || opResult == PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT
                    || opResult == PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
                ) {
                    return false;
                } else if (opResult == PreviewLogicOperationResult.OPENING_PREVIEW) {
                    updatePostLoadingAndDialogState(PostLoadingState.PREVIEWING, mPost);
                }
            } else if (itemId == R.id.menu_post_settings) {
                if (mEditPostSettingsFragment != null) {
                    mEditPostSettingsFragment.refreshViews();
                }
                ActivityUtils.hideKeyboard(this);
                mViewPager.setCurrentItem(PAGE_SETTINGS);
            } else if (itemId == R.id.menu_secondary_action) {
                return performSecondaryAction();
            } else if (itemId == R.id.menu_html_mode) {
                // toggle HTML mode
                if (mEditorFragment instanceof AztecEditorFragment) {
                    ((AztecEditorFragment) mEditorFragment).onToolbarHtmlButtonClicked();
                    toggledHtmlModeSnackbar(view -> {
                        // switch back
                        ((AztecEditorFragment) mEditorFragment).onToolbarHtmlButtonClicked();
                    });
                } else if (mEditorFragment instanceof GutenbergEditorFragment) {
                    ((GutenbergEditorFragment) mEditorFragment).onToggleHtmlMode();
                    toggledHtmlModeSnackbar(view -> {
                        // switch back
                        ((GutenbergEditorFragment) mEditorFragment).onToggleHtmlMode();
                    });
                }
            } else if (itemId == R.id.menu_switch_to_aztec) {
                // The following boolean check should be always redundant but was added to manage
                // an odd behaviour recorded with Android 8.0.0
                // (see https://github.com/wordpress-mobile/WordPress-Android/issues/9748 for more information)
                if (mShowGutenbergEditor) {
                    // let's finish this editing instance and start again, but not letting Gutenberg be used
                    mRestartEditorOption = RestartEditorOptions.RESTART_SUPPRESS_GUTENBERG;
                    mPostEditorAnalyticsSession.switchEditor(Editor.CLASSIC);
                    mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
                    savePostAndOptionallyFinish(true);
                } else {
                    logWrongMenuState("Wrong state in menu_switch_to_aztec: menu should not be visible.");
                }
            } else if (itemId == R.id.menu_switch_to_gutenberg) {
                // The following boolean check should be always redundant but was added to manage
                // an odd behaviour recorded with Android 8.0.0
                // (see https://github.com/wordpress-mobile/WordPress-Android/issues/9748 for more information)
                if (shouldSwitchToGutenbergBeVisible(mPost, mEditorFragment, mSite)) {
                    // let's finish this editing instance and start again, but let GB be used
                    mRestartEditorOption = RestartEditorOptions.RESTART_DONT_SUPPRESS_GUTENBERG;
                    mPostEditorAnalyticsSession.switchEditor(Editor.GUTENBERG);
                    mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
                    savePostAndOptionallyFinish(true);
                } else {
                    logWrongMenuState("Wrong state in menu_switch_to_gutenberg: menu should not be visible.");
                }
            }
        }
        return false;
    }

    private void logWrongMenuState(String logMsg) {
        AppLog.w(T.EDITOR, logMsg);
        // Lets record this event in Sentry
        CrashLoggingUtils.logException(new IllegalStateException(logMsg), T.EDITOR);
    }

    private void showEmptyPostErrorForSecondaryAction() {
        String message = getString(mIsPage ? R.string.error_publish_empty_page : R.string.error_publish_empty_post);
        if (getSecondaryAction() == SecondaryEditorAction.SAVE_AS_DRAFT
            || getSecondaryAction() == SecondaryEditorAction.SAVE) {
            message = getString(R.string.error_save_empty_draft);
        }
        ToastUtils.showToast(EditPostActivity.this, message, Duration.SHORT);
    }

    private void saveAsDraft() {
        mEditPostSettingsFragment.updatePostStatus(PostStatus.DRAFT.toString());
        ToastUtils.showToast(EditPostActivity.this,
                getString(R.string.editor_post_converted_back_to_draft), Duration.SHORT);
        UploadUtils.showSnackbar(findViewById(R.id.editor_activity), R.string.editor_uploading_post);
        mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
        savePostAndOptionallyFinish(false);
    }

    private boolean performSecondaryAction() {
        if (UploadService.hasInProgressMediaUploadsForPost(mPost)) {
            ToastUtils.showToast(EditPostActivity.this,
                    getString(R.string.editor_toast_uploading_please_wait), Duration.SHORT);
            return false;
        }

        // we update the mPost object first, so we can pre-check Post publish-ability and inform the user
        updatePostObject();
        if (isDiscardable()) {
            showEmptyPostErrorForSecondaryAction();
            return false;
        }

        switch (getSecondaryAction()) {
            case SAVE_AS_DRAFT:
                // Force the new Draft status
                saveAsDraft();
                return true;
            case SAVE:
                uploadPost(false);
                return true;
            case PUBLISH_NOW:
                showPublishConfirmationDialogAndPublishPost();
                return true;
            case NONE:
                throw new IllegalStateException("Switch in `secondaryAction` shouldn't go through the NONE case");
        }
        return false;
    }

    private void toggledHtmlModeSnackbar(View.OnClickListener onUndoClickListener) {
        UploadUtils.showSnackbarSuccessActionOrange(findViewById(R.id.editor_activity),
                mHtmlModeMenuStateOn ? R.string.menu_html_mode_done_snackbar
                        : R.string.menu_visual_mode_done_snackbar,
                R.string.menu_undo_snackbar_action,
                onUndoClickListener);
    }

    private void refreshEditorContent() {
        mHasSetPostContent = false;
        fillContentEditorFields();
    }

    private void setPreviewingInEditorSticky(boolean enable, @Nullable PostModel post) {
        if (enable) {
            if (post != null) {
                EventBus.getDefault().postSticky(
                        new PostEvents.PostPreviewingInEditor(post.getLocalSiteId(), post.getId()));
            }
        } else {
            PostEvents.PostPreviewingInEditor stickyEvent =
                    EventBus.getDefault().getStickyEvent(PostEvents.PostPreviewingInEditor.class);
            if (stickyEvent != null) {
                EventBus.getDefault().removeStickyEvent(stickyEvent);
            }
        }
    }

    private void managePostLoadingStateTransitions(PostLoadingState postLoadingState, @Nullable PostModel post) {
        switch (postLoadingState) {
            case NONE:
                setPreviewingInEditorSticky(false, post);
                break;
            case UPLOADING_FOR_PREVIEW:
            case REMOTE_AUTO_SAVING_FOR_PREVIEW:
            case PREVIEWING:
            case REMOTE_AUTO_SAVE_PREVIEW_ERROR:
                setPreviewingInEditorSticky(true, post);
                break;
            case LOADING_REVISION:
                // nothing to do
                break;
        }
    }

    private void updatePostLoadingAndDialogState(PostLoadingState postLoadingState) {
        updatePostLoadingAndDialogState(postLoadingState, null);
    }

    private void updatePostLoadingAndDialogState(PostLoadingState postLoadingState, @Nullable PostModel post) {
        // We need only transitions, so...
        if (mPostLoadingState == postLoadingState) return;

        AppLog.d(
                AppLog.T.POSTS,
                "Editor post loading state machine: transition from " + mPostLoadingState + " to " + postLoadingState
        );

        // update the state
        mPostLoadingState = postLoadingState;

        // take care of exit actions on state transition
        managePostLoadingStateTransitions(postLoadingState, post);

        // update the progress dialog state
        mProgressDialog = mProgressDialogHelper.updateProgressDialogState(
                this,
                mProgressDialog,
                mPostLoadingState.getProgressDialogUiState(),
                mUiHelpers);
    }

    private void toggleHtmlModeOnMenu() {
        mHtmlModeMenuStateOn = !mHtmlModeMenuStateOn;
        trackPostSessionEditorModeSwitch();
        invalidateOptionsMenu();
    }

    private void trackPostSessionEditorModeSwitch() {
        boolean isGutenberg = mEditorFragment instanceof GutenbergEditorFragment;
        mPostEditorAnalyticsSession.switchEditor(
                mHtmlModeMenuStateOn ? Editor.HTML : (isGutenberg ? Editor.GUTENBERG : Editor.CLASSIC));
    }

    private void showUpdateConfirmationDialogAndUploadPost() {
        showConfirmationDialogAndUploadPost(TAG_UPDATE_CONFIRMATION_DIALOG,
                getString(R.string.dialog_confirm_update_title),
                mPost.isPage() ? getString(R.string.dialog_confirm_update_message_page)
                        : getString(R.string.dialog_confirm_update_message_post),
                getString(R.string.dialog_confirm_update_yes),
                getString(R.string.keep_editing));
    }

    private void showPublishConfirmationDialogAndPublishPost() {
        showConfirmationDialogAndUploadPost(TAG_PUBLISH_CONFIRMATION_DIALOG,
                getString(R.string.dialog_confirm_publish_title),
                mPost.isPage() ? getString(R.string.dialog_confirm_publish_message_page)
                        : getString(R.string.dialog_confirm_publish_message_post),
                getString(R.string.dialog_confirm_publish_yes),
                getString(R.string.keep_editing));
    }

    private void showConfirmationDialogAndUploadPost(@NonNull String identifier, @NonNull String title,
                                                     @NonNull String description, @NonNull String positiveButton,
                                                     @NonNull String negativeButton) {
        BasicFragmentDialog publishConfirmationDialog = new BasicFragmentDialog();
        publishConfirmationDialog.initialize(identifier, title, description, positiveButton, negativeButton, null);
        publishConfirmationDialog.show(getSupportFragmentManager(), identifier);
    }

    private void performPrimaryAction() {
        switch (getPrimaryAction()) {
            case UPDATE:
                showUpdateConfirmationDialogAndUploadPost();
                return;
            case PUBLISH_NOW:
                showPublishConfirmationDialogAndPublishPost();
                return;
            // In other cases, we'll upload the post without changing its status
            case SCHEDULE:
            case SUBMIT_FOR_REVIEW:
            case SAVE:
                uploadPost(false);
                break;
        }
    }

    private void showGutenbergInformativeDialog() {
        // Show the GB informative dialog on editing GB posts
        final PromoDialog gbInformativeDialog = new PromoDialog();
        gbInformativeDialog.initialize(TAG_GB_INFORMATIVE_DIALOG,
                getString(R.string.dialog_gutenberg_informative_title),
                mPost.isPage() ? getString(R.string.dialog_gutenberg_informative_description_page)
                        : getString(R.string.dialog_gutenberg_informative_description_post),
                getString(org.wordpress.android.editor.R.string.dialog_button_ok));

        gbInformativeDialog.show(getSupportFragmentManager(), TAG_GB_INFORMATIVE_DIALOG);
        AppPrefs.setGutenbergInfoPopupDisplayed(mSite.getUrl());
    }

    private void setGutenbergEnabledIfNeeded() {
        if (AppPrefs.isGutenbergInfoPopupDisplayed(mSite.getUrl())) {
            return;
        }

        boolean showPopup = AppPrefs.shouldShowGutenbergInfoPopupForTheNewPosts(mSite.getUrl());

        if (TextUtils.isEmpty(mSite.getMobileEditor()) && !mIsNewPost) {
            SiteUtils.enableBlockEditor(mDispatcher, mSite);
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_GUTENBERG_ENABLED, mSite,
                    BlockEditorEnabledSource.ON_BLOCK_POST_OPENING.asPropertyMap());
            showPopup = true;
        }

        if (showPopup) {
            showGutenbergInformativeDialog();
        }
    }

    private void savePostOnlineAndFinishAsync(
            boolean isFirstTimePublish,
            boolean doFinishActivity
    ) {
        new SavePostOnlineAndFinishTask(isFirstTimePublish, doFinishActivity)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onUploadSuccess(MediaModel media) {
        // TODO Should this statement check media.getLocalPostId() == mPost.getId()?
        if (media != null && !media.getMarkedLocallyAsFeatured() && mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadSucceeded(String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
        } else if (media != null && media.getMarkedLocallyAsFeatured() && media.getLocalPostId() == mPost.getId()) {
            setFeaturedImageId(media.getMediaId());
        }
    }

    private void onUploadError(MediaModel media, MediaError error) {
        String localMediaId = String.valueOf(media.getId());

        Map<String, Object> properties = null;
        MediaFile mf = FluxCUtils.mediaFileFromMediaModel(media);
        if (mf != null) {
            properties = AnalyticsUtils.getMediaProperties(this, mf.isVideo(), null, mf.getFilePath());
            properties.put("error_type", error.type.name());
        }
        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_FAILED, properties);

        // Display custom error depending on error type
        String errorMessage = WPMediaUtils.getErrorMessage(this, media, error);
        if (errorMessage == null) {
            errorMessage = TextUtils.isEmpty(error.message) ? getString(R.string.tap_to_try_again) : error.message;
        }

        if (mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadFailed(localMediaId,
                                                           EditorFragmentAbstract.getEditorMimeType(mf), errorMessage);
        }
    }

    private void onUploadProgress(MediaModel media, float progress) {
        String localMediaId = String.valueOf(media.getId());
        if (mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadProgress(localMediaId, progress);
        }
    }

    private void launchPictureLibrary() {
        WPMediaUtils.launchPictureLibrary(this, mAllowMultipleSelection);
    }

    private void launchVideoLibrary() {
        WPMediaUtils.launchVideoLibrary(this, mAllowMultipleSelection);
    }

    private void launchVideoCamera() {
        WPMediaUtils.launchVideoCamera(this);
    }

    private void showErrorAndFinish(int errorMessageId) {
        ToastUtils.showToast(this, errorMessageId, ToastUtils.Duration.LONG);
        finish();
    }

    private void trackEditorCreatedPost(String action, Intent intent) {
        Map<String, Object> properties = new HashMap<>();
        // Post created from the post list (new post button).
        String normalizedSourceName = "post-list";
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // Post created with share with WordPress
            normalizedSourceName = "shared-from-external-app";
        }
        if (EditPostActivity.NEW_MEDIA_POST.equals(
                action)) {
            // Post created from the media library
            normalizedSourceName = "media-library";
        }
        if (intent != null && intent.hasExtra(EXTRA_IS_QUICKPRESS)) {
            // Quick press
            normalizedSourceName = "quick-press";
        }
        PostUtils.addPostTypeToAnalyticsProperties(mPost, properties);
        properties.put("created_post_source", normalizedSourceName);
        AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.EDITOR_CREATED_POST,
                mSiteStore.getSiteByLocalId(mPost.getLocalSiteId()),
                properties
        );
    }

    private synchronized void updatePostObject(boolean isAutosave) throws EditorFragmentNotAddedException {
        if (mPost == null) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.");
            return;
        }

        // Update post object from fragment fields
        boolean postTitleOrContentChanged = false;
        if (mEditorFragment != null) {
            postTitleOrContentChanged =
                    updatePostContentNewEditor(isAutosave, (String) mEditorFragment.getTitle(),
                            (String) mEditorFragment.getContent(mPost.getContent()));
        }

        // only makes sense to change the publish date and locally changed date if the Post was actually changed
        if (postTitleOrContentChanged) {
            PostUtils.updatePublishDateIfShouldBePublishedImmediately(mPost);
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }
    }

    private void savePostAsync(final AfterSavePostListener listener) {
        new Thread(() -> {
            try {
                updatePostObject(false);
            } catch (EditorFragmentNotAddedException e) {
                AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
                return;
            }
            savePostToDb();
            if (listener != null) {
                listener.onPostSave();
            }
        }).start();
    }

    @Override
    public void initializeEditorFragment() {
        if (mEditorFragment instanceof AztecEditorFragment) {
            AztecEditorFragment aztecEditorFragment = (AztecEditorFragment) mEditorFragment;
            aztecEditorFragment.setEditorImageSettingsListener(EditPostActivity.this);
            aztecEditorFragment.setMediaToolbarButtonClickListener(EditPostActivity.this);

            // Here we should set the max width for media, but the default size is already OK. No need
            // to customize it further

            Drawable loadingImagePlaceholder = EditorMediaUtils.getAztecPlaceholderDrawableFromResID(
                    this,
                    org.wordpress.android.editor.R.drawable.ic_gridicons_image,
                    aztecEditorFragment.getMaxMediaSize()
            );
            mAztecImageLoader = new AztecImageLoader(getBaseContext(), mImageManager, loadingImagePlaceholder);
            aztecEditorFragment.setAztecImageLoader(mAztecImageLoader);
            aztecEditorFragment.setLoadingImagePlaceholder(loadingImagePlaceholder);

            Drawable loadingVideoPlaceholder = EditorMediaUtils.getAztecPlaceholderDrawableFromResID(
                    this,
                    org.wordpress.android.editor.R.drawable.ic_gridicons_video_camera,
                    aztecEditorFragment.getMaxMediaSize()
            );
            aztecEditorFragment.setAztecVideoLoader(new AztecVideoLoader(getBaseContext(), loadingVideoPlaceholder));
            aztecEditorFragment.setLoadingVideoPlaceholder(loadingVideoPlaceholder);

            if (getSite() != null && getSite().isWPCom() && !getSite().isPrivate()) {
                // Add the content reporting for wpcom blogs that are not private
                aztecEditorFragment.enableContentLogOnCrashes(
                        throwable -> {
                            // Do not log private or password protected post
                            return getPost() != null && TextUtils.isEmpty(getPost().getPassword())
                                   && !PostStatus.PRIVATE.toString().equals(getPost().getStatus());
                        }
                );
            }
            aztecEditorFragment.setExternalLogger(new AztecLog.ExternalLogger() {
                @Override
                public void log(@NotNull String s) {
                    // For now, we're wrapping up the actual log into an exception to reduce possibility
                    // of information not travelling to our Crash Logging Service.
                    // For more info: http://bit.ly/2oJHMG7 and http://bit.ly/2oPOtFX
                    CrashLoggingUtils.logException(new AztecEditorFragment.AztecLoggingException(s), T.EDITOR);
                }

                @Override
                public void logException(@NotNull Throwable throwable) {
                    CrashLoggingUtils.logException(new AztecEditorFragment.AztecLoggingException(throwable), T.EDITOR);
                }

                @Override
                public void logException(@NotNull Throwable throwable, String s) {
                    CrashLoggingUtils.logException(
                            new AztecEditorFragment.AztecLoggingException(throwable), T.EDITOR, s);
                }
            });
        }
    }

    @Override
    public void onImageSettingsRequested(EditorImageMetaData editorImageMetaData) {
        MediaSettingsActivity.showForResult(this, mSite, editorImageMetaData);
    }

    @Override
    public void onNegativeClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_FAILED_MEDIA_UPLOADS_DIALOG:
                // Clear failed uploads
                mFeaturedImageHelper.cancelFeaturedImageUpload(this, mSite, mPost, true);
                mEditorFragment.removeAllFailedMediaUploads();
                break;
            case TAG_PUBLISH_CONFIRMATION_DIALOG:
            case TAG_UPDATE_CONFIRMATION_DIALOG:
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    @Override
    public void onPositiveClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
            case TAG_UPDATE_CONFIRMATION_DIALOG:
                uploadPost(false);
                break;
            case TAG_PUBLISH_CONFIRMATION_DIALOG:
                uploadPost(true);
                AppRatingDialog.INSTANCE
                        .incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE);
                break;
            case TAG_FAILED_MEDIA_UPLOADS_DIALOG:
                savePostOnlineAndFinishAsync(isFirstTimePublish(false), true);
                break;
            case TAG_GB_INFORMATIVE_DIALOG:
                // no op
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog instanceTag is not recognized");
                throw new UnsupportedOperationException("Dialog instanceTag is not recognized");
        }
    }

    /*
     * user clicked OK on a settings list dialog displayed from the settings fragment - pass the event
     * along to the settings fragment
     */
    @Override
    public void onPostSettingsFragmentPositiveButtonClicked(@NonNull PostSettingsListDialogFragment dialog) {
        if (mEditPostSettingsFragment != null) {
            mEditPostSettingsFragment.onPostSettingsFragmentPositiveButtonClicked(dialog);
        }
    }

    private interface AfterSavePostListener {
        void onPostSave();
    }

    private synchronized void savePostToDb() {
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));

        if (mShowAztecEditor) {
            // update the list of uploading ids
            mMediaMarkedUploadingOnStartIds =
                    AztecEditorFragment.getMediaMarkedUploadingInPostContent(this, mPost.getContent());
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    @Override
    public void onHistoryItemClicked(@NonNull Revision revision, @NonNull ArrayList<Revision> revisions) {
        AnalyticsTracker.track(Stat.REVISIONS_DETAIL_VIEWED_FROM_LIST);
        mRevision = revision;

        ActivityLauncher.viewHistoryDetailForResult(this, mRevision, revisions);
    }

    private void loadRevision() {
        updatePostLoadingAndDialogState(PostLoadingState.LOADING_REVISION);
        mPostForUndo = mPost.clone();
        mPost.setTitle(mRevision.getPostTitle());
        mPost.setContent(mRevision.getPostContent());
        mPost.setIsLocallyChanged(true);
        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        refreshEditorContent();

        WPSnackbar.make(mViewPager, getString(R.string.history_loaded_revision), 4000)
                .setAction(getString(R.string.undo), view -> {
                    AnalyticsTracker.track(Stat.REVISIONS_LOAD_UNDONE);
                    RemotePostPayload payload = new RemotePostPayload(mPostForUndo, mSite);
                    mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload));
                    mPost = mPostForUndo.clone();
                    refreshEditorContent();
                })
                .show();

        updatePostLoadingAndDialogState(PostLoadingState.NONE);
    }

    private boolean isNewPost() {
        return mIsNewPost;
    }

    private class SavePostOnlineAndFinishTask extends AsyncTask<Void, Void, Void> {
        boolean mIsFirstTimePublish;
        boolean mDoFinishActivity;

        SavePostOnlineAndFinishTask(boolean isFirstTimePublish, boolean doFinishActivity) {
            this.mIsFirstTimePublish = isFirstTimePublish;
            this.mDoFinishActivity = doFinishActivity;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // mark as pending if the user doesn't have publishing rights
            if (!UploadUtils.userCanPublish(mSite)) {
                switch (PostStatus.fromPost(mPost)) {
                    case UNKNOWN:
                    case PUBLISHED:
                    case SCHEDULED:
                    case PRIVATE:
                        mPost.setStatus(PostStatus.PENDING.toString());
                        break;
                    case DRAFT:
                    case PENDING:
                    case TRASHED:
                        break;
                }
            }

            savePostToDb();
            PostUtils.trackSavePostAnalytics(mPost, mSiteStore.getSiteByLocalId(mPost.getLocalSiteId()));

            UploadService.uploadPost(EditPostActivity.this, mPost, mIsFirstTimePublish);

            PendingDraftsNotificationsUtils.cancelPendingDraftAlarms(EditPostActivity.this, mPost.getId());

            return null;
        }

        @Override
        protected void onPostExecute(Void saved) {
            if (mDoFinishActivity) {
                saveResult(true, false, false);
                removePostOpenInEditorStickyEvent();
                finish();
            }
        }
    }

    private class SavePostLocallyAndFinishTask extends AsyncTask<Void, Void, Boolean> {
        boolean mDoFinishActivity;

        SavePostLocallyAndFinishTask(boolean doFinishActivity) {
            this.mDoFinishActivity = doFinishActivity;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (PostUtils.postHasEdits(mPostSnapshotWhenEditorOpened, mPost)) {
                // Changes have been made - save the post and ask for the post list to refresh
                // We consider this being "manual save", it will replace some Android "spans" by an html
                // or a shortcode replacement (for instance for images and galleries)

                // Update the post object directly, without re-fetching the fields from the EditorFragment
                updatePostContentNewEditor(false, mPost.getTitle(), mPost.getContent());

                savePostToDb();

                // For self-hosted sites, when exiting the editor without uploading, the `PostUploadModel.uploadState`
                // can get stuck in `PENDING`. This happens in this scenario:
                //
                // 1. The user edits an existing post
                // 2. Adds an image -- this creates the `PostUploadModel` as `PENDING`
                // 3. Exits the editor by tapping on Back (not saving or publishing)
                //
                // If the `uploadState` is stuck at `PENDING`, the Post List will indefinitely show a “Queued post”
                // label.
                //
                // The `uploadState` does not get stuck on `PENDING` for WPCom because the app will automatically
                // start a remote auto-save when the editor exits. Hence, the `PostUploadModel` eventually gets updated.
                //
                // Marking the `PostUploadModel` as `CANCELLED` when exiting should be fine for all site types since
                // we do not currently have any special handling for cancelled uploads. Eventually, the user will
                // restart them and the `uploadState` will be corrected.
                //
                // See `PostListUploadStatusTracker` and `PostListItemUiStateHelper.createUploadUiState` for how
                // the Post List determines what label to use.
                mDispatcher.dispatch(UploadActionBuilder.newCancelPostAction(mPost));

                // now set the pending notification alarm to be triggered in the next day, week, and month
                PendingDraftsNotificationsUtils.scheduleNextNotifications(EditPostActivity.this, mPost);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean saved) {
            if (mDoFinishActivity) {
                saveResult(saved, false, true);
                removePostOpenInEditorStickyEvent();
                finish();
            }
        }
    }

    private void saveResult(boolean saved, boolean discardable, boolean savedLocally) {
        Intent i = getIntent();
        i.putExtra(EXTRA_SAVED_AS_LOCAL_DRAFT, savedLocally);
        i.putExtra(EXTRA_HAS_FAILED_MEDIA, hasFailedMedia());
        i.putExtra(EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EXTRA_HAS_CHANGES, saved);
        i.putExtra(EXTRA_POST_LOCAL_ID, mPost.getId());
        i.putExtra(EXTRA_POST_REMOTE_ID, mPost.getRemotePostId());
        i.putExtra(EXTRA_IS_DISCARDABLE, discardable);
        i.putExtra(EXTRA_RESTART_EDITOR, mRestartEditorOption.name());
        i.putExtra(STATE_KEY_EDITOR_SESSION_DATA, mPostEditorAnalyticsSession);
        i.putExtra(EXTRA_IS_NEW_POST, mIsNewPost);
        setResult(RESULT_OK, i);
    }

    private void uploadPost(final boolean publishPost) {
        AccountModel account = mAccountStore.getAccount();
        // prompt user to verify e-mail before publishing
        if (!account.getEmailVerified()) {
            String message = TextUtils.isEmpty(account.getEmail())
                    ? getString(R.string.editor_confirm_email_prompt_message)
                    : String.format(getString(R.string.editor_confirm_email_prompt_message_with_email),
                                    account.getEmail());

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, R.style.Calypso_Dialog));
            builder.setTitle(R.string.editor_confirm_email_prompt_title)
                   .setMessage(message)
                   .setPositiveButton(android.R.string.ok,
                           (dialog, id) -> {
                               ToastUtils.showToast(EditPostActivity.this,
                                                    getString(R.string.toast_saving_post_as_draft));
                               mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
                               savePostAndOptionallyFinish(true);
                           })
                   .setNegativeButton(R.string.editor_confirm_email_prompt_negative,
                           (dialog, id) -> mDispatcher
                                   .dispatch(AccountActionBuilder.newSendVerificationEmailAction()));
            builder.create().show();
            return;
        }

        // Loading the content from the GB HTML editor can take time on long posts.
        // Let's show a progress dialog for now. Ref: https://github.com/wordpress-mobile/gutenberg-mobile/issues/713
        mEditorFragment.showSavingProgressDialogIfNeeded();

        // Update post, save to db and publish in its own Thread, because 1. update can be pretty slow with a lot of
        // text 2. better not to call `updatePostObject()` from the UI thread due to weird thread blocking behavior
        // on API 16 (and 21) with the visual editor.
        new Thread(() -> {
            boolean isFirstTimePublish = isFirstTimePublish(publishPost);
            if (publishPost) {
                // now set status to PUBLISHED - only do this AFTER we have run the isFirstTimePublish() check,
                // otherwise we'd have an incorrect value
                // also re-set the published date in case it was SCHEDULED and they want to publish NOW
                if (PostStatus.fromPost(mPost) == PostStatus.SCHEDULED) {
                    mPost.setDateCreated(DateTimeUtils.iso8601FromDate(new Date()));
                }
                mPost.setStatus(PostStatus.PUBLISHED.toString());
                mPostEditorAnalyticsSession.setOutcome(Outcome.PUBLISH);
            } else {
                // particular case: if user is submitting for review (that is,
                // can't publish posts directly to this site), update the status
                if (!UploadUtils.userCanPublish(mSite)) {
                    mPost.setStatus(PostStatus.PENDING.toString());
                }
                mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
            }

            boolean postUpdateSuccessful = updatePostObject();
            if (!postUpdateSuccessful) {
                // just return, since the only case updatePostObject() can fail is when the editor
                // fragment is not added to the activity
                mEditorFragment.hideSavingProgressDialog();
                return;
            }

            boolean isPublishable = PostUtils.isPublishable(mPost);

            AppLog.d(T.POSTS, "User explicitly confirmed changes. Post Title: " + mPost.getTitle());
            // the user explicitly confirmed an intention to upload the post
            mPost.setChangesConfirmedContentHashcode(mPost.contentHashcode());

            // if post was modified or has unsaved local changes and is publishable, save it
            saveResult(isPublishable, false, false);

            // Hide the progress dialog now
            mEditorFragment.hideSavingProgressDialog();
            if (isPublishable) {
                if (NetworkUtils.isNetworkAvailable(getBaseContext())) {
                    // Show an Alert Dialog asking the user if they want to remove all failed media before upload
                    if (mEditorFragment.hasFailedMediaUploads()
                        || mFeaturedImageHelper.getFailedFeaturedImageUpload(mPost) != null) {
                        EditPostActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showRemoveFailedUploadsDialog();
                            }
                        });
                    } else {
                        savePostOnlineAndFinishAsync(isFirstTimePublish, true);
                    }
                } else {
                    savePostLocallyAndFinishAsync(true);
                }
            } else {
                // the user has just tapped on "PUBLISH" on an empty post, make sure to set the status back to the
                // original post's status as we could not proceed with the action
                if (mPostSnapshotWhenEditorOpened != null) {
                    mPost.setStatus(mPostSnapshotWhenEditorOpened.getStatus());
                } else {
                    mPost.setStatus(PostStatus.DRAFT.toString());
                }
                EditPostActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = getString(
                                mIsPage ? R.string.error_publish_empty_page : R.string.error_publish_empty_post);
                        ToastUtils.showToast(EditPostActivity.this, message, Duration.SHORT);
                    }
                });
            }
        }).start();
    }

    private void showRemoveFailedUploadsDialog() {
        BasicFragmentDialog removeFailedUploadsDialog = new BasicFragmentDialog();
        removeFailedUploadsDialog.initialize(
                TAG_FAILED_MEDIA_UPLOADS_DIALOG,
                "",
                getString(R.string.editor_toast_failed_uploads),
                getString(R.string.editor_retry_failed_uploads),
                getString(R.string.editor_remove_failed_uploads),
                null);
        removeFailedUploadsDialog.show(getSupportFragmentManager(), TAG_FAILED_MEDIA_UPLOADS_DIALOG);
    }

    private void savePostAndOptionallyFinish(final boolean doFinish) {
        savePostAndOptionallyFinish(doFinish, false);
    }

    private void savePostAndOptionallyFinish(final boolean doFinish, final boolean forceSave) {
        // Update post, save to db and post online in its own Thread, because 1. update can be pretty slow with a lot of
        // text 2. better not to call `updatePostObject()` from the UI thread due to weird thread blocking behavior
        // on API 16 (and 21) with the visual editor.
        new Thread(() -> {
            // check if the opened post had some unsaved local changes
            boolean isFirstTimePublish = isFirstTimePublish(false);

            boolean postUpdateSuccessful = updatePostObject();
            if (!postUpdateSuccessful) {
                // just return, since the only case updatePostObject() can fail is when the editor
                // fragment is not added to the activity
                return;
            }

            boolean isPublishable = PostUtils.isPublishable(mPost);

            // if post was modified during this editing session, save it
            boolean shouldSave = shouldSavePost() || forceSave;

            // if post is publishable or not new, sync it
            boolean shouldSync = isPublishable || !isNewPost();

            if (doFinish) {
                saveResult(shouldSave && shouldSync, isDiscardable(), false);
            }

            definitelyDeleteBackspaceDeletedMediaItems();

            if (shouldSave) {
                boolean isNotRestarting = mRestartEditorOption == RestartEditorOptions.NO_RESTART;
                /*
                 * Remote-auto-save isn't supported on self-hosted sites. We can save the post online (as draft)
                 * only when it doesn't exist in the remote yet. When it does exist in the remote, we can upload
                 * it only when the user explicitly confirms the changes - eg. clicks on save/publish/submit. The
                  * user didn't confirm the changes in this code path.
                 */
                boolean isWpComOrIsLocalDraft = mSite.isUsingWpComRestApi() || mPost.isLocalDraft();
                if (isPublishable && !hasFailedMedia() && NetworkUtils.isNetworkAvailable(getBaseContext())
                        && isNotRestarting && isWpComOrIsLocalDraft) {
                    mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
                    savePostOnlineAndFinishAsync(isFirstTimePublish, doFinish);
                } else {
                    mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
                    if (forceSave) {
                        savePostOnlineAndFinishAsync(false, false);
                    } else {
                        savePostLocallyAndFinishAsync(doFinish);
                    }
                }
            } else {
                // discard post if new & empty
                if (isDiscardable()) {
                    mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(mPost));
                }
                removePostOpenInEditorStickyEvent();
                if (doFinish) {
                    // if we shouldn't save and we should exit, set the session tracking outcome to CANCEL
                    mPostEditorAnalyticsSession.setOutcome(Outcome.CANCEL);
                    finish();
                }
            }
        }).start();
    }

    private boolean shouldSavePost() {
        boolean hasChanges = PostUtils.postHasEdits(mPostSnapshotWhenEditorOpened, mPost);
        boolean isPublishable = PostUtils.isPublishable(mPost);

        // if post was modified during this editing session, save it
        return (mPostSnapshotWhenEditorOpened != null && hasChanges) || (isPublishable && isNewPost());
    }


    private boolean isDiscardable() {
        return !PostUtils.isPublishable(mPost) && isNewPost();
    }

    private boolean isFirstTimePublish(final boolean publishPost) {
        final PostStatus originalStatus = PostStatus.fromPost(mPost);
        return ((originalStatus == PostStatus.DRAFT || originalStatus == PostStatus.UNKNOWN) && publishPost)
               || (originalStatus == PostStatus.SCHEDULED && publishPost)
               || (originalStatus == PostStatus.PUBLISHED && mPost.isLocalDraft())
               || (originalStatus == PostStatus.PUBLISHED && mPost.getRemotePostId() == 0);
    }

    /**
     * Can be dropped and replaced by mEditorFragment.hasFailedMediaUploads() when we drop the visual editor.
     * mEditorFragment.isActionInProgress() was added to address a timing issue when adding media and immediately
     * publishing or exiting the visual editor. It's not safe to upload the post in this state.
     * See https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/294
     */
    private boolean hasFailedMedia() {
        return mEditorFragment.hasFailedMediaUploads() || mEditorFragment.isActionInProgress();
    }

    private boolean updatePostObject() {
        try {
            updatePostObject(false);
        } catch (EditorFragmentNotAddedException e) {
            AppLog.e(T.EDITOR, "Impossible to save and publish the post, we weren't able to update it.");
            return false;
        }

        return true;
    }

    private void savePostLocallyAndFinishAsync(boolean doFinishActivity) {
        new SavePostLocallyAndFinishTask(doFinishActivity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private static final int NUM_PAGES_EDITOR = 4;
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case PAGE_CONTENT:
                    if (mShowGutenbergEditor) {
                        // Enable gutenberg on the site & show the informative popup upon opening
                        // the GB editor the first time when the remote setting value is still null
                        setGutenbergEnabledIfNeeded();
                        String languageString = LocaleManager.getLanguage(EditPostActivity.this);
                        String wpcomLocaleSlug = languageString.replace("_", "-").toLowerCase(Locale.ENGLISH);
                        return GutenbergEditorFragment.newInstance("", "", mIsNewPost, wpcomLocaleSlug);
                    } else {
                        // If gutenberg editor is not selected, default to Aztec.
                        return AztecEditorFragment.newInstance("", "", AppPrefs.isAztecEditorToolbarExpanded());
                    }
                case PAGE_SETTINGS:
                    return EditPostSettingsFragment.newInstance();
                case PAGE_PUBLISH_SETTINGS:
                    return EditPostPublishSettingsFragment.Companion.newInstance();
                case PAGE_HISTORY:
                    return HistoryListFragment.Companion.newInstance(mPost, mSite);
                default:
                    throw new IllegalArgumentException("Unexpected page type");
            }
        }

        @Override
        public @NotNull Object instantiateItem(@NotNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case PAGE_CONTENT:
                    mEditorFragment = (EditorFragmentAbstract) fragment;
                    mEditorFragment.setImageLoader(mImageLoader);

                    mEditorFragment.getTitleOrContentChanged().observe(EditPostActivity.this, editable -> {
                        if (mHandler != null) {
                            mHandler.removeCallbacks(mSave);
                            if (mDebounceCounter < MAX_UNSAVED_POSTS) {
                                mDebounceCounter++;
                                mHandler.postDelayed(mSave, CHANGE_SAVE_DELAY);
                            } else {
                                mHandler.post(mSave);
                            }
                        }
                    });

                    if (mEditorFragment instanceof EditorMediaUploadListener) {
                        mEditorMediaUploadListener = (EditorMediaUploadListener) mEditorFragment;

                        // Set up custom headers for the visual editor's internal WebView
                        mEditorFragment.setCustomHttpHeader("User-Agent", WordPress.getUserAgent());

                        reattachUploadingMediaForAztec();
                    }
                    break;
                case PAGE_SETTINGS:
                    mEditPostSettingsFragment = (EditPostSettingsFragment) fragment;
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return NUM_PAGES_EDITOR;
        }
    }

    // Moved from EditPostContentFragment
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA_IDS = "NEW_MEDIA_POST_EXTRA_IDS";
    private String mMediaCapturePath = "";
    private int mMaxThumbWidth = 0;

    private int getMaximumThumbnailWidthForEditor() {
        if (mMaxThumbWidth == 0) {
            mMaxThumbWidth = EditorMediaUtils.getMaximumThumbnailSizeForEditor(this);
        }
        return mMaxThumbWidth;
    }

    private boolean addExistingMediaToEditor(@NonNull AddExistingdMediaSource source, long mediaId) {
        MediaModel media = mMediaStore.getSiteMediaWithId(mSite, mediaId);
        if (media == null) {
            AppLog.w(T.MEDIA, "Cannot add null media to post");
            return false;
        }

        trackAddMediaEvent(source, media);

        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(media);
        String urlToUse = TextUtils.isEmpty(media.getUrl()) ? media.getFilePath() : media.getUrl();
        mEditorFragment.appendMediaFile(mediaFile, urlToUse, mImageLoader);
        return true;
    }

    private void addExistingMediaToEditor(@NonNull AddExistingdMediaSource source, List<Long> mediaIdList) {
        ArrayMap<String, MediaFile> mediaMap = new ArrayMap<>();
        for (Long mediaId : mediaIdList) {
            MediaModel media = mMediaStore.getSiteMediaWithId(mSite, mediaId);
            if (media == null) {
                AppLog.w(T.MEDIA, "Cannot add null media to post");
            } else {
                trackAddMediaEvent(source, media);

                MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(media);
                String urlToUse = TextUtils.isEmpty(media.getUrl()) ? media.getFilePath() : media.getUrl();

                mediaMap.put(urlToUse, mediaFile);
            }
        }
        mEditorFragment.appendMediaFiles(mediaMap);
    }

    private class LoadPostContentTask extends AsyncTask<String, Spanned, Spanned> {
        @Override
        protected Spanned doInBackground(String... params) {
            if (params.length < 1 || mPost == null) {
                return null;
            }

            String content = StringUtils.notNullStr(params[0]);
            return WPHtml.fromHtml(content, EditPostActivity.this, mPost, getMaximumThumbnailWidthForEditor());
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            if (spanned != null) {
                mEditorFragment.setContent(spanned);
            }
        }
    }

    private String getUploadErrorHtml(String mediaId, String path) {
        return String.format(Locale.US,
                "<span id=\"img_container_%s\" class=\"img_container failed\" data-failed=\"%s\">"
                + "<progress id=\"progress_%s\" value=\"0\" class=\"wp_media_indicator failed\" "
                + "contenteditable=\"false\"></progress>"
                + "<img data-wpid=\"%s\" src=\"%s\" alt=\"\" class=\"failed\"></span>",
                mediaId, getString(R.string.tap_to_try_again), mediaId, mediaId, path);
    }

    private String migrateLegacyDraft(String content) {
        if (content.contains("<img src=\"null\" android-uri=\"")) {
            // We must replace image tags specific to the legacy editor local drafts:
            // <img src="null" android-uri="file:///..." />
            // And trigger an upload action for the specific image / video
            Pattern pattern = Pattern.compile("<img src=\"null\" android-uri=\"([^\"]*)\".*>");
            Matcher matcher = pattern.matcher(content);
            StringBuffer stringBuffer = new StringBuffer();
            while (matcher.find()) {
                String stringUri = matcher.group(1);
                Uri uri = Uri.parse(stringUri);
                MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(
                        queueFileForUpload(uri, getContentResolver().getType(uri), MediaUploadState.FAILED));
                if (mediaFile == null) {
                    continue;
                }
                String replacement = getUploadErrorHtml(String.valueOf(mediaFile.getId()), mediaFile.getFilePath());
                matcher.appendReplacement(stringBuffer, replacement);
            }
            matcher.appendTail(stringBuffer);
            content = stringBuffer.toString();
        }
        if (content.contains("[caption")) {
            // Convert old legacy post caption formatting to new format, to avoid being stripped by the visual editor
            Pattern pattern = Pattern.compile("(\\[caption[^]]*caption=\"([^\"]*)\"[^]]*].+?)(\\[\\/caption])");
            Matcher matcher = pattern.matcher(content);
            StringBuffer stringBuffer = new StringBuffer();
            while (matcher.find()) {
                String replacement = matcher.group(1) + matcher.group(2) + matcher.group(3);
                matcher.appendReplacement(stringBuffer, replacement);
            }
            matcher.appendTail(stringBuffer);
            content = stringBuffer.toString();
        }
        return content;
    }

    private void fillContentEditorFields() {
        // Needed blog settings needed by the editor
        mEditorFragment.setFeaturedImageSupported(mSite.isFeaturedImageSupported());

        // Set up the placeholder text
        mEditorFragment.setContentPlaceholder(getString(R.string.editor_content_placeholder));
        mEditorFragment.setTitlePlaceholder(getString(mIsPage ? R.string.editor_page_title_placeholder
                                                              : R.string.editor_post_title_placeholder));

        // Set post title and content
        if (mPost != null) {
            // don't avoid calling setContent() for GutenbergEditorFragment so RN gets initialized
            if ((!TextUtils.isEmpty(mPost.getContent()) || mEditorFragment instanceof GutenbergEditorFragment)
                && !mHasSetPostContent) {
                mHasSetPostContent = true;
                // TODO: Might be able to drop .replaceAll() when legacy editor is removed
                String content = mPost.getContent().replaceAll("\uFFFC", "");
                // Prepare eventual legacy editor local draft for the new editor
                content = migrateLegacyDraft(content);
                mEditorFragment.setContent(content);
            }
            if (!TextUtils.isEmpty(mPost.getTitle())) {
                mEditorFragment.setTitle(mPost.getTitle());
            } else if (mEditorFragment instanceof GutenbergEditorFragment) {
                // don't avoid calling setTitle() for GutenbergEditorFragment so RN gets initialized
                mEditorFragment.setTitle("");
            }

            // TODO: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
        }

        // Special actions - these only make sense for empty posts that are going to be populated now
        if (TextUtils.isEmpty(mPost.getContent())) {
            String action = getIntent().getAction();
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                setPostContentFromShareAction();
            } else if (NEW_MEDIA_POST.equals(action)) {
                prepareMediaPost();
            }
        }
    }

    private void launchCamera() {
        WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                mediaCapturePath -> mMediaCapturePath = mediaCapturePath);
    }

    protected void setPostContentFromShareAction() {
        Intent intent = getIntent();

        // Check for shared text
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (text != null) {
            if (title != null) {
                mEditorFragment.setTitle(title);
                mPost.setTitle(title);
            }
            // Create an <a href> element around links
            text = AutolinkUtils.autoCreateLinks(text);
            mEditorFragment.setContent(text);

            // update PostModel
            mPost.setContent(text);
            PostUtils.updatePublishDateIfShouldBePublishedImmediately(mPost);
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }

        // Check for shared media
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            String action = intent.getAction();
            String type = intent.getType();
            ArrayList<Uri> sharedUris;

            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                sharedUris = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
            } else {
                // For a single media share, we only allow images and video types
                if (type != null && (type.startsWith("image") || type.startsWith("video"))) {
                    sharedUris = new ArrayList<>();
                    sharedUris.add(intent.getParcelableExtra(Intent.EXTRA_STREAM));
                } else {
                    sharedUris = null;
                }
            }

            if (sharedUris != null) {
                // removing this from the intent so it doesn't insert the media items again on each Activity re-creation
                getIntent().removeExtra(Intent.EXTRA_STREAM);
                addMediaList(sharedUris, false);
            }
        }
    }

    private void prepareMediaPost() {
        long[] idsArray = getIntent().getLongArrayExtra(NEW_MEDIA_POST_EXTRA_IDS);
        ArrayList<Long> idsList = ListUtils.fromLongArray(idsArray);
        for (Long id : idsList) {
            addExistingMediaToEditor(AddExistingdMediaSource.WP_MEDIA_LIBRARY, id);
        }
        savePostAsync(null);
    }

    /**
     * Updates post object with given title and content
     */
    public boolean updatePostContentNewEditor(boolean isAutoSave, String title, String content) {
        if (mPost == null) {
            return false;
        }

        if (!isAutoSave) {
            // TODO: Shortcode handling, media handling
        }

        boolean titleChanged = PostUtils.updatePostTitleIfDifferent(mPost, title);
        boolean contentChanged;
        if (mMediaInsertedOnCreation) {
            mMediaInsertedOnCreation = false;
            contentChanged = true;
        } else if (isCurrentMediaMarkedUploadingDifferentToOriginal(content)) {
            contentChanged = true;
        } else {
            contentChanged = mPost.getContent().compareTo(content) != 0;
        }
        if (contentChanged) {
            mPost.setContent(content);
        }

        boolean statusChanged = mPostSnapshotWhenEditorOpened != null
                                && !mPost.getStatus().equals(mPostSnapshotWhenEditorOpened.getStatus());

        if (!mPost.isLocalDraft() && (titleChanged || contentChanged || statusChanged)) {
            mPost.setIsLocallyChanged(true);
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }

        return titleChanged || contentChanged;
    }

    /*
      * for as long as the user is in the Editor, we check whether there are any differences in media items
      * being uploaded since they opened the Editor for this Post. If some items have finished, the current list
      * won't be equal and thus we'll know we need to save the Post content as it's changed, given the local
      * URLs will have been replaced with the remote ones.
     */
    private boolean isCurrentMediaMarkedUploadingDifferentToOriginal(String newContent) {
        // this method makes use of AztecEditorFragment methods. Make sure to only run if Aztec is the current editor.
        if (!mShowAztecEditor) {
            return false;
        }
        List<String> currentUploadingMedia = AztecEditorFragment.getMediaMarkedUploadingInPostContent(this, newContent);
        Collections.sort(currentUploadingMedia);
        return !mMediaMarkedUploadingOnStartIds.equals(currentUploadingMedia);
    }

    /**
     * Analytics about media from device
     *
     * @param isNew Whether is a fresh media
     * @param isVideo Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    private void trackAddMediaFromDeviceEvents(boolean isNew, boolean isVideo, Uri uri) {
        if (uri == null) {
            AppLog.e(T.MEDIA, "Cannot track new media events if both path and mediaURI are null!!");
            return;
        }

        Map<String, Object> properties = AnalyticsUtils.getMediaProperties(this, isVideo, uri, null);
        Stat currentStat;
        if (isVideo) {
            if (isNew) {
                currentStat = Stat.EDITOR_ADDED_VIDEO_NEW;
            } else {
                currentStat = Stat.EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY;
            }
        } else {
            if (isNew) {
                currentStat = Stat.EDITOR_ADDED_PHOTO_NEW;
            } else {
                currentStat = Stat.EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY;
            }
        }

        AnalyticsUtils.trackWithSiteDetails(currentStat, mSite, properties);
    }

    /**
     * Analytics about media already available in the blog's library.
     * @param source where the media is being added from
     * @param media media being added
     */
    private void trackAddMediaEvent(@NonNull AddExistingdMediaSource source, @NonNull MediaModel media) {
        switch (source) {
            case WP_MEDIA_LIBRARY:
                if (media.isVideo()) {
                    AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY, mSite, null);
                } else {
                    AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY, mSite, null);
                }
                break;
            case STOCK_PHOTO_LIBRARY:
                AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY, mSite, null);
                break;
        }
    }

    private boolean addMedia(Uri mediaUri, boolean isNew) {
        if (mediaUri == null) {
            return false;
        }

        List<Uri> uriList = new ArrayList<>();
        uriList.add(mediaUri);
        addMediaList(uriList, isNew);
        return true;
    }

    private AddMediaListThread mAddMediaListThread;

    private void addMediaList(@NonNull List<Uri> uriList, boolean isNew) {
        // fetch any shared media first - must be done on the main thread
        List<Uri> fetchedUriList = fetchMediaList(uriList);
        mAddMediaListThread = new AddMediaListThread(fetchedUriList, isNew, mAllowMultipleSelection);
        mAddMediaListThread.start();
    }

    private void cancelAddMediaListThread() {
        if (mAddMediaListThread != null && !mAddMediaListThread.isInterrupted()) {
            try {
                mAddMediaListThread.interrupt();
            } catch (SecurityException e) {
                AppLog.e(T.MEDIA, e);
            }
        }
    }

    /*
     * processes a list of media in the background (optimizing, resizing, etc.) and adds them to
     * the editor one at a time
     */
    private class AddMediaListThread extends Thread {
        private final List<Uri> mUriList = new ArrayList<>();
        private final boolean mIsNew;
        private ProgressDialog mProgressDialog;
        private boolean mDidAnyFail;
        private int mFinishedUploads = 0;
        private boolean mAllowMultipleSelection = false;
        private Map<String, MediaFile> mediaMap = new ArrayMap<>();

        AddMediaListThread(@NonNull List<Uri> uriList, boolean isNew) {
            this.mUriList.addAll(uriList);
            this.mIsNew = isNew;
            showOverlay(false);
        }

        AddMediaListThread(@NonNull List<Uri> uriList, boolean isNew, boolean allowMultipleSelection) {
            this.mUriList.addAll(uriList);
            this.mIsNew = isNew;
            this.mAllowMultipleSelection = allowMultipleSelection;
            showOverlay(false);
        }

        private void showProgressDialog(final boolean show) {
            runOnUiThread(() -> {
                try {
                    if (show) {
                        mProgressDialog = new ProgressDialog(EditPostActivity.this);
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.setIndeterminate(true);
                        mProgressDialog.setMessage(getString(R.string.add_media_progress));
                        mProgressDialog.show();
                    } else if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                } catch (IllegalArgumentException e) {
                    AppLog.e(T.MEDIA, e);
                }
            });
        }

        @Override
        public void run() {
            // adding multiple media items at once can take several seconds on slower devices, so we show a blocking
            // progress dialog in this situation - otherwise the user could accidentally back out of the process
            // before all items were added
            boolean shouldShowProgress = mUriList.size() > 2;
            if (shouldShowProgress) {
                showProgressDialog(true);
            }
            try {
                for (Uri mediaUri : mUriList) {
                    if (isInterrupted()) {
                        return;
                    }
                    if (!processMedia(mediaUri)) {
                        mDidAnyFail = true;
                    }
                }
            } finally {
                if (shouldShowProgress) {
                    showProgressDialog(false);
                }
            }


            runOnUiThread(() -> {
                if (!isInterrupted()) {
                    savePostAsync(null);
                    hideOverlay();
                    if (mDidAnyFail) {
                        ToastUtils.showToast(EditPostActivity.this, R.string.gallery_error,
                                             Duration.SHORT);
                    }
                }
            });
        }

        private boolean processMedia(Uri mediaUri) {
            if (mediaUri == null) {
                return false;
            }

            Activity activity = EditPostActivity.this;

            String path = MediaUtils.getRealPathFromURI(activity, mediaUri);
            if (path == null) {
                return false;
            }

            final boolean isVideo = MediaUtils.isVideo(mediaUri.toString());
            Uri optimizedMedia = WPMediaUtils.getOptimizedMedia(activity, path, isVideo);
            if (optimizedMedia != null) {
                mediaUri = optimizedMedia;
            } else {
                // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
                if (!mSite.isWPCom()) {
                    // If it's not wpcom we must rotate the picture locally
                    Uri rotatedMedia = WPMediaUtils.fixOrientationIssue(activity, path, isVideo);
                    if (rotatedMedia != null) {
                        mediaUri = rotatedMedia;
                    }
                }
            }

            if (isInterrupted()) {
                return false;
            }

            trackAddMediaFromDeviceEvents(mIsNew, isVideo, mediaUri);
            postProcessMedia(mediaUri, path);

            return true;
        }

    private void postProcessMedia(final Uri mediaUri, final String path) {
        if (mAllowMultipleSelection) {
            MediaFile mediaFile = getMediaFile(mediaUri);
            if (mediaFile != null) {
                mediaMap.put(path, mediaFile);
            }
            mFinishedUploads++;
            if (mUriList.size() == mFinishedUploads) {
                runOnUiThread(() -> mEditorFragment.appendMediaFiles(mediaMap));
            }
        } else {
            runOnUiThread(() -> addMediaVisualEditor(mediaUri, path));
        }
    }
}

    private void addMediaVisualEditor(Uri uri, String path) {
        MediaFile mediaFile = getMediaFile(uri);
        if (mediaFile != null) {
            mEditorFragment.appendMediaFile(mediaFile, path, mImageLoader);
        }
    }

    private MediaFile getMediaFile(Uri uri) {
        MediaModel media = queueFileForUpload(uri, getContentResolver().getType(uri));
        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(media);
        if (media != null) {
            return mediaFile;
        } else {
            return null;
        }
    }

    private void addMediaItemGroupOrSingleItem(Intent data) {
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            ArrayList<Uri> uriList = new ArrayList<>();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                uriList.add(item.getUri());
            }
            addMediaList(uriList, false);
        } else {
            addMedia(data.getData(), false);
        }
    }

    private void advertiseImageOptimisationAndAddMedia(final Intent data) {
        if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
            WPMediaUtils.advertiseImageOptimization(this,
                    () -> addMediaItemGroupOrSingleItem(data));
        } else {
            addMediaItemGroupOrSingleItem(data);
        }
    }

    private void setFeaturedImageId(final long mediaId) {
        mPost.setFeaturedImageId(mediaId);
        mPost.setIsLocallyChanged(true);
        savePostAsync(() -> EditPostActivity.this.runOnUiThread(() -> {
            if (mEditPostSettingsFragment != null) {
                mEditPostSettingsFragment.updateFeaturedImage(mediaId);
            }
        }));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // In case of Remote Preview we need to change state even if (resultCode != Activity.RESULT_OK)
        // so placing this here before the check
        if (requestCode == RequestCodes.REMOTE_PREVIEW_POST) {
            updatePostLoadingAndDialogState(PostLoadingState.NONE);
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO || requestCode == RequestCodes.TAKE_VIDEO
                              || requestCode == RequestCodes.PHOTO_PICKER))) {
            switch (requestCode) {
                case RequestCodes.MULTI_SELECT_MEDIA_PICKER:
                case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
                    handleMediaPickerResult(data);
                    // No need to bump analytics here. Bumped later in
                    // handleMediaPickerResult -> addExistingMediaToEditorAndSave
                    break;
                case RequestCodes.PHOTO_PICKER:
                case RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT:
                    // user chose a featured image
                    if (data.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_ID)) {
                        long mediaId = data.getLongExtra(PhotoPickerActivity.EXTRA_MEDIA_ID, 0);
                        setFeaturedImageId(mediaId);
                    } else if (data.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_QUEUED)) {
                        if (mEditPostSettingsFragment != null) {
                            mEditPostSettingsFragment.refreshViews();
                        }
                    }
                    break;
                case RequestCodes.MEDIA_LIBRARY:
                case RequestCodes.PICTURE_LIBRARY:
                    advertiseImageOptimisationAndAddMedia(data);
                    break;
                case RequestCodes.TAKE_PHOTO:
                    if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
                        WPMediaUtils.advertiseImageOptimization(this, this::addLastTakenPicture);
                    } else {
                        addLastTakenPicture();
                    }
                    break;
                case RequestCodes.VIDEO_LIBRARY:
                    addMediaItemGroupOrSingleItem(data);
                    break;
                case RequestCodes.TAKE_VIDEO:
                    Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(this);
                    if (addMedia(capturedVideoUri, true)) {
                        AnalyticsTracker.track(Stat.EDITOR_ADDED_VIDEO_NEW);
                    } else {
                        ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
                    }
                    break;
                case RequestCodes.MEDIA_SETTINGS:
                    if (mEditorFragment instanceof AztecEditorFragment) {
                        mEditorFragment.onActivityResult(AztecEditorFragment.EDITOR_MEDIA_SETTINGS,
                                                         Activity.RESULT_OK, data);
                    }
                    break;
                case RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT:
                    if (data.hasExtra(StockMediaPickerActivity.KEY_UPLOADED_MEDIA_IDS)) {
                        long[] mediaIds =
                                data.getLongArrayExtra(StockMediaPickerActivity.KEY_UPLOADED_MEDIA_IDS);
                        for (long id : mediaIds) {
                            addExistingMediaToEditor(AddExistingdMediaSource.STOCK_PHOTO_LIBRARY, id);
                        }
                        savePostAsync(null);
                    }
                    break;
                case RequestCodes.GIPHY_PICKER:
                    if (data.hasExtra(GiphyPickerActivity.KEY_SAVED_MEDIA_MODEL_LOCAL_IDS)) {
                        int[] localIds = data.getIntArrayExtra(GiphyPickerActivity.KEY_SAVED_MEDIA_MODEL_LOCAL_IDS);
                        ArrayList<MediaModel> mediaModels = new ArrayList<>();
                        for (int localId : localIds) {
                            mediaModels.add(mMediaStore.getMediaWithLocalId(localId));
                        }

                        startUploadService(mediaModels);

                        for (MediaModel mediaModel : mediaModels) {
                            mediaModel.setLocalPostId(mPost.getId());
                            mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel));

                            MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(mediaModel);
                            mEditorFragment.appendMediaFile(mediaFile, mediaFile.getFilePath(), mImageLoader);
                        }
                    }
                    break;
                case RequestCodes.HISTORY_DETAIL:
                    if (data.hasExtra(KEY_REVISION)) {
                        mViewPager.setCurrentItem(PAGE_CONTENT);

                        mRevision = data.getParcelableExtra(KEY_REVISION);
                        new Handler().postDelayed(this::loadRevision,
                                getResources().getInteger(R.integer.full_screen_dialog_animation_duration));
                    }
                    break;
            }
        }
    }

    private void addLastTakenPicture() {
        try {
            WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
            File f = new File(mMediaCapturePath);
            Uri capturedImageUri = Uri.fromFile(f);
            if (addMedia(capturedImageUri, true)) {
                final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(capturedImageUri);
                sendBroadcast(scanIntent);
            } else {
                ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
            }
        } catch (RuntimeException | OutOfMemoryError e) {
            AppLog.e(T.EDITOR, e);
        }
    }

    /*
     * called before we add media to make sure we have access to any media shared from another app (Google Photos, etc.)
     */
    private List<Uri> fetchMediaList(@NonNull List<Uri> uriList) {
        boolean didAnyFail = false;
        List<Uri> fetchedUriList = new ArrayList<>();
        for (int i = 0; i < uriList.size(); i++) {
            Uri mediaUri = uriList.get(i);
            if (mediaUri == null) {
                continue;
            }
            if (!MediaUtils.isInMediaStore(mediaUri)) {
                // Do not download the file in async task. See
                // https://github.com/wordpress-mobile/WordPress-Android/issues/5818
                Uri fetchedUri = null;
                try {
                    fetchedUri = MediaUtils.downloadExternalMedia(EditPostActivity.this, mediaUri);
                } catch (IllegalStateException e) {
                    // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                    AppLog.e(AppLog.T.UTILS, "Can't download the image at: " + mediaUri.toString(), e);
                    CrashLoggingUtils
                            .logException(e, AppLog.T.MEDIA, "Can't download the image at: " + mediaUri.toString()
                                                             + " See issue #5823");
                    didAnyFail = true;
                }
                if (fetchedUri != null) {
                    fetchedUriList.add(fetchedUri);
                } else {
                    didAnyFail = true;
                }
            } else {
                fetchedUriList.add(mediaUri);
            }
        }

        if (didAnyFail) {
            ToastUtils.showToast(EditPostActivity.this, R.string.error_downloading_image, ToastUtils.Duration.SHORT);
        }

        return fetchedUriList;
    }

    private void handleMediaPickerResult(Intent data) {
        ArrayList<Long> ids = ListUtils.fromLongArray(data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS));
        if (ids == null || ids.size() == 0) {
            return;
        }

        boolean allAreImages = true;
        for (Long id : ids) {
            MediaModel media = mMediaStore.getSiteMediaWithId(mSite, id);
            if (media != null && !MediaUtils.isValidImage(media.getUrl())) {
                allAreImages = false;
                break;
            }
        }

        // if the user selected multiple items and they're all images, show the insert media
        // dialog so the user can choose whether to insert them individually or as a gallery
        if (ids.size() > 1 && allAreImages && !mShowGutenbergEditor) {
            showInsertMediaDialog(ids);
        } else {
            // if mAllowMultipleSelection and gutenberg editor, pass all ids to addExistingMediaToEditor at once
            if (mShowGutenbergEditor && mAllowMultipleSelection) {
                addExistingMediaToEditor(AddExistingdMediaSource.WP_MEDIA_LIBRARY, ids);
                mAllowMultipleSelection = false;
            } else {
                for (Long id : ids) {
                    addExistingMediaToEditor(AddExistingdMediaSource.WP_MEDIA_LIBRARY, id);
                }
            }
            savePostAsync(null);
        }
    }

    /*
     * called after user selects multiple photos from WP media library
     */
    private void showInsertMediaDialog(final ArrayList<Long> mediaIds) {
        InsertMediaCallback callback = dialog -> {
            switch (dialog.getInsertType()) {
                case GALLERY:
                    MediaGallery gallery = new MediaGallery();
                    gallery.setType(dialog.getGalleryType().toString());
                    gallery.setNumColumns(dialog.getNumColumns());
                    gallery.setIds(mediaIds);
                    mEditorFragment.appendGallery(gallery);
                    break;
                case INDIVIDUALLY:
                    for (Long id : mediaIds) {
                        addExistingMediaToEditor(AddExistingdMediaSource.WP_MEDIA_LIBRARY, id);
                    }
                    savePostAsync(null);
                    break;
            }
        };
        InsertMediaDialog dialog = InsertMediaDialog.newInstance(callback, mSite);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, "insert_media");
        ft.commitAllowingStateLoss();
    }

    private void refreshBlogMedia() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            FetchMediaListPayload payload = new FetchMediaListPayload(
                    mSite, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false);
            mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload));
        } else {
            ToastUtils.showToast(this, R.string.error_media_refresh_no_connection, ToastUtils.Duration.SHORT);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.causeOfChange == AccountAction.SEND_VERIFICATION_EMAIL) {
            if (!event.isError()) {
                ToastUtils.showToast(this, getString(R.string.toast_verification_email_sent));
            } else {
                ToastUtils.showToast(this, getString(R.string.toast_verification_email_send_error));
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (event.isError()) {
            final String errorMessage;
            switch (event.error.type) {
                case FS_READ_PERMISSION_DENIED:
                    errorMessage = getString(R.string.error_media_insufficient_fs_permissions);
                    break;
                case NOT_FOUND:
                    errorMessage = getString(R.string.error_media_not_found);
                    break;
                case AUTHORIZATION_REQUIRED:
                    errorMessage = getString(R.string.error_media_unauthorized);
                    break;
                case PARSE_ERROR:
                    errorMessage = getString(R.string.error_media_parse_error);
                    break;
                case MALFORMED_MEDIA_ARG:
                case NULL_MEDIA_ARG:
                case GENERIC_ERROR:
                default:
                    errorMessage = getString(R.string.error_refresh_media);
                    break;
            }
            if (!TextUtils.isEmpty(errorMessage)) {
                ToastUtils.showToast(EditPostActivity.this, errorMessage, ToastUtils.Duration.SHORT);
            }
        } else {
            if (mPendingVideoPressInfoRequests != null && !mPendingVideoPressInfoRequests.isEmpty()) {
                // If there are pending requests for video URLs from VideoPress ids, query the DB for
                // them again and notify the editor
                for (String videoId : mPendingVideoPressInfoRequests) {
                    String videoUrl = mMediaStore.
                                                         getUrlForSiteVideoWithVideoPressGuid(mSite, videoId);
                    String posterUrl = WPMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

                    mEditorFragment.setUrlForVideoPressId(videoId, videoUrl, posterUrl);
                }

                mPendingVideoPressInfoRequests.clear();
            }
        }
    }

    /**
     * Starts the upload service to upload selected media.
     */
    private void startUploadService(MediaModel media) {
        final ArrayList<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        startUploadService(mediaList);
    }

    /**
     * Start the {@link UploadService} to upload the given {@code mediaModels}.
     *
     * Only {@link MediaModel} objects that have {@code MediaUploadState.QUEUED} statuses will be uploaded. .
     */
    private void startUploadService(@NonNull List<MediaModel> mediaModels) {
        // make sure we only pass items with the QUEUED state to the UploadService
        final ArrayList<MediaModel> queuedMediaModels = new ArrayList<>();
        for (MediaModel media : mediaModels) {
            if (MediaUploadState.QUEUED.toString().equals(media.getUploadState())) {
                queuedMediaModels.add(media);
            }
        }

        // before starting the service, we need to update the posts' contents so we are sure the service
        // can retrieve it from there on
        savePostAsync(() -> UploadService.uploadMediaFromEditor(EditPostActivity.this, queuedMediaModels));
    }

    private String getVideoThumbnail(String videoPath) {
        String thumbnailPath = null;
        try {
            File outputFile = File.createTempFile("thumb", ".png", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            Bitmap thumb = ImageUtils.getVideoFrameFromVideo(
                    videoPath,
                    EditorMediaUtils.getMaximumThumbnailSizeForEditor(this)
            );
            if (thumb != null) {
                thumb.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
                thumbnailPath = outputFile.getAbsolutePath();
            }
        } catch (IOException e) {
            AppLog.i(T.MEDIA, "Can't create thumbnail for video: " + videoPath);
        }
        return thumbnailPath;
    }

    /**
     * Queues a media file for upload and starts the UploadService. Toasts will alert the user
     * if there are issues with the file.
     */
    private MediaModel queueFileForUpload(Uri uri, String mimeType) {
        return queueFileForUpload(uri, mimeType, MediaUploadState.QUEUED);
    }

    private MediaModel queueFileForUpload(Uri uri, String mimeType, MediaUploadState startingState) {
        String path = MediaUtils.getRealPathFromURI(this, uri);

        // Invalid file path
        if (TextUtils.isEmpty(path)) {
            ToastUtils.showToast(this, R.string.editor_toast_invalid_path, ToastUtils.Duration.SHORT);
            return null;
        }

        // File not found
        File file = new File(path);
        if (!file.exists()) {
            ToastUtils.showToast(this, R.string.file_not_found, ToastUtils.Duration.SHORT);
            return null;
        }

        // we need to update media with the local post Id
        MediaModel media = buildMediaModel(uri, mimeType, startingState);
        if (media == null) {
            ToastUtils.showToast(this, R.string.file_not_found, ToastUtils.Duration.SHORT);
            return null;
        }
        media.setLocalPostId(mPost.getId());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        startUploadService(media);

        return media;
    }

    private MediaModel buildMediaModel(Uri uri, String mimeType, MediaUploadState startingState) {
        MediaModel media = FluxCUtils.mediaModelFromLocalUri(this, uri, mimeType, mMediaStore, mSite.getId());
        if (media == null) {
            return null;
        }
        if (org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(media.getMimeType())) {
            String path = MediaUtils.getRealPathFromURI(this, uri);
            media.setThumbnailUrl(getVideoThumbnail(path));
        }

        media.setUploadState(startingState);
        if (!mPost.isLocalDraft()) {
            media.setPostId(mPost.getRemotePostId());
        }

        return media;
    }

    @Override
    public void onEditPostPublishedSettingsClick() {
        mViewPager.setCurrentItem(PAGE_PUBLISH_SETTINGS);
    }

    /**
     * EditorFragmentListener methods
     */

    @Override
    public void onAddMediaClicked() {
        if (isPhotoPickerShowing()) {
            hidePhotoPicker();
         } else if (WPMediaUtils.currentUserCanUploadMedia(mSite)) {
            showPhotoPicker();
        } else {
            // show the WP media library instead of the photo picker if the user doesn't have upload permission
            ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.EDITOR_PICKER);
        }
    }

    @Override
    public void onAddMediaImageClicked(boolean allowMultipleSelection) {
        mAllowMultipleSelection = allowMultipleSelection;
        ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.GUTENBERG_IMAGE_PICKER);
    }

    @Override
    public void onAddMediaVideoClicked(boolean allowMultipleSelection) {
        mAllowMultipleSelection = allowMultipleSelection;
        ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.GUTENBERG_VIDEO_PICKER);
    }

    @Override
    public void onAddLibraryMediaClicked(boolean allowMultipleSelection) {
        mAllowMultipleSelection = allowMultipleSelection;
        if (mAllowMultipleSelection) {
            ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.EDITOR_PICKER);
        } else {
            ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER);
        }
    }

    @Override
    public void onAddPhotoClicked(boolean allowMultipleSelection) {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO, allowMultipleSelection);
    }

    @Override
    public void onCapturePhotoClicked() {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO, false);
    }

    @Override
    public void onAddVideoClicked(boolean allowMultipleSelection) {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_VIDEO, allowMultipleSelection);
    }

    @Override
    public void onAddDeviceMediaClicked(boolean allowMultipleSelection) {
        mAllowMultipleSelection = allowMultipleSelection;
        WPMediaUtils.launchMediaLibrary(this, mAllowMultipleSelection);
    }

    @Override
    public void onCaptureVideoClicked() {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_VIDEO, false);
    }

    @Override
    public void onMediaDropped(final ArrayList<Uri> mediaUris) {
        mDroppedMediaUris = mediaUris;
        if (PermissionUtils
                .checkAndRequestStoragePermission(this, WPPermissionUtils.EDITOR_DRAG_DROP_PERMISSION_REQUEST_CODE)) {
            runOnUiThread(mFetchMediaRunnable);
        }
    }

    @Override
    public void onRequestDragAndDropPermissions(DragEvent dragEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestTemporaryPermissions(dragEvent);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void requestTemporaryPermissions(DragEvent dragEvent) {
        requestDragAndDropPermissions(dragEvent);
    }

    @Override
    public void onMediaRetryAllClicked(Set<String> failedMediaIds) {
        UploadService.cancelFinalNotification(this, mPost);
        UploadService.cancelFinalNotificationForMedia(this, mSite);

        ArrayList<MediaModel> failedMediaList = new ArrayList<>();
        for (String mediaId : failedMediaIds) {
            failedMediaList.add(mMediaStore.getMediaWithLocalId(Integer.valueOf(mediaId)));
        }

        if (!failedMediaList.isEmpty()) {
            for (MediaModel mediaModel : failedMediaList) {
                mediaModel.setUploadState(MediaUploadState.QUEUED);
                mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel));
            }
            startUploadService(failedMediaList);
        }

        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED);
    }

    @Override
    public boolean onMediaRetryClicked(final String mediaId) {
        if (TextUtils.isEmpty(mediaId)) {
            AppLog.e(T.MEDIA, "Invalid media id passed to onMediaRetryClicked");
            return false;
        }
        MediaModel media = mMediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId));
        if (media == null) {
            AppLog.e(T.MEDIA, "Can't find media with local id: " + mediaId);
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, R.style.Calypso_Dialog));
            builder.setTitle(getString(R.string.cannot_retry_deleted_media_item));
            builder.setPositiveButton(R.string.yes, (dialog, id) -> {
                runOnUiThread(() -> mEditorFragment.removeMedia(mediaId));
                dialog.dismiss();
            });

            builder.setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();

            return false;
        }

        if (media.getUrl() != null && media.getUploadState().equals(MediaUploadState.UPLOADED.toString())) {
            // Note: we should actually do this when the editor fragment starts instead of waiting for user input.
            // Notify the editor fragment upload was successful and it should replace the local url by the remote url.
            if (mEditorMediaUploadListener != null) {
                mEditorMediaUploadListener.onMediaUploadSucceeded(String.valueOf(media.getId()),
                        FluxCUtils.mediaFileFromMediaModel(media));
            }
        } else {
            UploadService.cancelFinalNotification(this, mPost);
            UploadService.cancelFinalNotificationForMedia(this, mSite);
            media.setUploadState(MediaUploadState.QUEUED);
            mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            startUploadService(media);
        }

        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED);
        return true;
    }

    @Override
    public void onMediaUploadCancelClicked(String localMediaId) {
        if (!TextUtils.isEmpty(localMediaId)) {
            cancelMediaUpload(StringUtils.stringToInt(localMediaId), true);
        } else {
            // Passed mediaId is incorrect: cancel all uploads for this post
            ToastUtils.showToast(this, getString(R.string.error_all_media_upload_canceled));
            EventBus.getDefault().post(new PostEvents.PostMediaCanceled(mPost));
        }
    }

    @Override
    public void onMediaDeleted(String localMediaId) {
        if (!TextUtils.isEmpty(localMediaId)) {
            if (mShowAztecEditor && !mShowGutenbergEditor) {
                setDeletedMediaIdOnUploadService(localMediaId);
                // passing false here as we need to keep the media item in case the user wants to undo
                cancelMediaUpload(StringUtils.stringToInt(localMediaId), false);
            } else if (mShowGutenbergEditor) {
                MediaModel mediaModel = mMediaStore.getMediaWithLocalId(StringUtils.stringToInt(localMediaId));
                if (mediaModel == null) {
                    return;
                }

                setDeletedMediaIdOnUploadService(localMediaId);

                // also make sure it's not being uploaded anywhere else (maybe on some other Post,
                // simultaneously)
                if (mediaModel.getUploadState() != null
                    && MediaUtils.isLocalFile(mediaModel.getUploadState().toLowerCase(Locale.ROOT))
                    && !UploadService.isPendingOrInProgressMediaUpload(mediaModel)) {
                    mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel));
                }
            }
        }
    }

    private void setDeletedMediaIdOnUploadService(String localMediaId) {
        mAztecBackspaceDeletedOrGbBlockDeletedMediaItemIds.add(localMediaId);
        UploadService.setDeletedMediaItemIds(mAztecBackspaceDeletedOrGbBlockDeletedMediaItemIds);
    }

    private void cancelMediaUpload(int localMediaId, boolean delete) {
        MediaModel mediaModel = mMediaStore.getMediaWithLocalId(localMediaId);
        if (mediaModel != null) {
            CancelMediaPayload payload = new CancelMediaPayload(mSite, mediaModel, delete);
            mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
        }
    }

    /*
    * When the user deletes a media item that was being uploaded at that moment, we only cancel the
    * upload but keep the media item in FluxC DB because the user might have deleted it accidentally,
    * and they can always UNDO the delete action in Aztec.
    * So, when the user exits then editor (and thus we lose the undo/redo history) we are safe to
    * physically delete from the FluxC DB those items that have been deleted by the user using backspace.
    * */
    private void definitelyDeleteBackspaceDeletedMediaItems() {
        for (String mediaId : mAztecBackspaceDeletedOrGbBlockDeletedMediaItemIds) {
            if (!TextUtils.isEmpty(mediaId)) {
                // make sure the MediaModel exists
                MediaModel mediaModel = mMediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId));
                if (mediaModel == null) {
                    continue;
                }

                // also make sure it's not being uploaded anywhere else (maybe on some other Post,
                // simultaneously)
                if (mediaModel.getUploadState() != null
                    && MediaUtils.isLocalFile(mediaModel.getUploadState().toLowerCase(Locale.ROOT))
                    && !UploadService.isPendingOrInProgressMediaUpload(mediaModel)) {
                    mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel));
                }
            }
        }
    }

    @Override
    public void onUndoMediaCheck(final String undoedContent) {
        // here we check which elements tagged UPLOADING are there in undoedContent,
        // and check for the ones that ARE NOT being uploaded or queued in the UploadService.
        // These are the CANCELED ONES, so mark them FAILED now to retry.

        List<MediaModel> currentlyUploadingMedia = UploadService.getPendingOrInProgressMediaUploadsForPost(mPost);
        List<String> mediaMarkedUploading =
                AztecEditorFragment.getMediaMarkedUploadingInPostContent(EditPostActivity.this, undoedContent);

        // go through the list of items marked UPLOADING within the Post content, and look in the UploadService
        // to see whether they're really being uploaded or not. If an item is not really being uploaded,
        // mark that item failed
        for (String mediaId : mediaMarkedUploading) {
            boolean found = false;
            for (MediaModel media : currentlyUploadingMedia) {
                if (StringUtils.stringToInt(mediaId) == media.getId()) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                if (mEditorFragment instanceof AztecEditorFragment) {
                    mAztecBackspaceDeletedOrGbBlockDeletedMediaItemIds.remove(mediaId);
                    // update the mediaIds list in UploadService
                    UploadService.setDeletedMediaItemIds(mAztecBackspaceDeletedOrGbBlockDeletedMediaItemIds);
                    ((AztecEditorFragment) mEditorFragment).setMediaToFailed(mediaId);
                }
            }
        }
    }

    @Override
    public void onVideoPressInfoRequested(final String videoId) {
        String videoUrl = mMediaStore.getUrlForSiteVideoWithVideoPressGuid(mSite, videoId);

        if (videoUrl == null) {
            AppLog.w(T.EDITOR, "The editor wants more info about the following VideoPress code: " + videoId
                               + " but it's not available in the current site " + mSite.getUrl()
                               + " Maybe it's from another site?");
            return;
        }

        if (videoUrl.isEmpty()) {
            if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(
                    this, WPPermissionUtils.EDITOR_MEDIA_PERMISSION_REQUEST_CODE)) {
                runOnUiThread(() -> {
                    if (mPendingVideoPressInfoRequests == null) {
                        mPendingVideoPressInfoRequests = new ArrayList<>();
                    }
                    mPendingVideoPressInfoRequests.add(videoId);
                    refreshBlogMedia();
                });
            }
        }

        String posterUrl = WPMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

        mEditorFragment.setUrlForVideoPressId(videoId, videoUrl, posterUrl);
    }

    @Override
    public String onAuthHeaderRequested(String url) {
        String authHeader = "";
        String token = mAccountStore.getAccessToken();
        if (mSite.isPrivate() && WPUrlUtils.safeToAddWordPressComAuthToken(url)
            && !TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
        }
        return authHeader;
    }

    @Override
    public void onEditorFragmentInitialized() {
        boolean shouldFinishInit = true;
        // now that we have the Post object initialized,
        // check whether we have media items to insert from the WRITE POST with media functionality
        if (getIntent().hasExtra(EXTRA_INSERT_MEDIA)) {
            // Bump analytics
            AnalyticsTracker.track(Stat.NOTIFICATION_UPLOAD_MEDIA_SUCCESS_WRITE_POST);

            List<MediaModel> mediaList = (List<MediaModel>) getIntent().getSerializableExtra(EXTRA_INSERT_MEDIA);
            // removing this from the intent so it doesn't insert the media items again on each Activity re-creation
            getIntent().removeExtra(EXTRA_INSERT_MEDIA);
            if (mediaList != null && !mediaList.isEmpty()) {
                shouldFinishInit = false;
                mMediaInsertedOnCreation = true;
                for (MediaModel media : mediaList) {
                    addExistingMediaToEditor(AddExistingdMediaSource.WP_MEDIA_LIBRARY, media.getMediaId());
                }
                savePostAsync(() -> runOnUiThread(this::onEditorFinalTouchesBeforeShowing));
            }
        }

        if (shouldFinishInit) {
            onEditorFinalTouchesBeforeShowing();
        }
    }

    private void onEditorFinalTouchesBeforeShowing() {
        refreshEditorContent();
        // probably here is best for Gutenberg to start interacting with
        if (mShowGutenbergEditor && mEditorFragment instanceof GutenbergEditorFragment) {
            List<MediaModel> failedMedia = mMediaStore.getMediaForPostWithState(mPost, MediaUploadState.FAILED);
            if (failedMedia != null && !failedMedia.isEmpty()) {
                HashSet<Integer> mediaIds = new HashSet<>();
                for (MediaModel media : failedMedia) {
                    // featured image isn't in the editor but in the Post Settings fragment, so we want to skip it
                    if (!media.getMarkedLocallyAsFeatured()) {
                        mediaIds.add(media.getId());
                    }
                }
                ((GutenbergEditorFragment) mEditorFragment).resetUploadingMediaToFailed(mediaIds);
            }
        } else if (mShowAztecEditor && mEditorFragment instanceof AztecEditorFragment) {
            mPostEditorAnalyticsSession.start(null);
        }
    }

    @Override
    public void onEditorFragmentContentReady(ArrayList<Object> unsupportedBlocksList) {
        mPostEditorAnalyticsSession.start(unsupportedBlocksList);
    }

    @Override
    public void onHtmlModeToggledInToolbar() {
        toggleHtmlModeOnMenu();
    }

    @Override
    public void onTrackableEvent(TrackableEvent event) throws IllegalArgumentException {
        AnalyticsTracker.Stat currentStat = null;
        switch (event) {
            case BOLD_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_BOLD;
                break;
            case BLOCKQUOTE_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_BLOCKQUOTE;
                break;
            case ELLIPSIS_COLLAPSE_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_ELLIPSIS_COLLAPSE;
                AppPrefs.setAztecEditorToolbarExpanded(false);
                break;
            case ELLIPSIS_EXPAND_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_ELLIPSIS_EXPAND;
                AppPrefs.setAztecEditorToolbarExpanded(true);
                break;
            case HEADING_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING;
                break;
            case HEADING_1_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING_1;
                break;
            case HEADING_2_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING_2;
                break;
            case HEADING_3_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING_3;
                break;
            case HEADING_4_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING_4;
                break;
            case HEADING_5_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING_5;
                break;
            case HEADING_6_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HEADING_6;
                break;
            case HORIZONTAL_RULE_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HORIZONTAL_RULE;
                break;
            case FORMAT_ALIGN_LEFT_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ALIGN_LEFT);
                break;
            case FORMAT_ALIGN_CENTER_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ALIGN_CENTER);
                break;
            case FORMAT_ALIGN_RIGHT_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ALIGN_RIGHT);
                break;
            case HTML_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_HTML;
                hidePhotoPicker();
                break;
            case IMAGE_EDITED:
                currentStat = Stat.EDITOR_EDITED_IMAGE;
                break;
            case ITALIC_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_ITALIC;
                break;
            case LINK_ADDED_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_LINK_ADDED;
                hidePhotoPicker();
                break;
            case LINK_REMOVED_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_LINK_REMOVED;
                break;
            case LIST_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_LIST;
                break;
            case LIST_ORDERED_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_LIST_ORDERED;
                break;
            case LIST_UNORDERED_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_LIST_UNORDERED;
                break;
            case MEDIA_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_IMAGE;
                break;
            case NEXT_PAGE_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_NEXT_PAGE;
                break;
            case PARAGRAPH_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_PARAGRAPH;
                break;
            case PREFORMAT_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_PREFORMAT;
                break;
            case READ_MORE_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_READ_MORE;
                break;
            case STRIKETHROUGH_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_STRIKETHROUGH;
                break;
            case UNDERLINE_BUTTON_TAPPED:
                currentStat = Stat.EDITOR_TAPPED_UNDERLINE;
                break;
            case REDO_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_REDO);
                break;
            case UNDO_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_UNDO);
                break;
            default:
                AppLog.w(T.EDITOR, "onTrackableEvent event not being tracked in EditPostActivity: " + event.name());
                break;
        }

        if (currentStat != null) {
            Map<String, String> properties = new HashMap<>();
            String editorName = null;
            if (mEditorFragment instanceof GutenbergEditorFragment) {
                editorName = "gutenberg";
            } else if (mEditorFragment instanceof AztecEditorFragment) {
                editorName = "aztec";
            }
            if (editorName == null) {
                throw new IllegalArgumentException("Unexpected Editor Fragment - got "
                                                   + mEditorFragment.getClass().getName()
                                                   + " but expected GutenbergEditorFragment or AztecEditorFragment");
            }
            properties.put("editor", editorName);
            AnalyticsTracker.track(currentStat, properties);
        }
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (isFinishing()) {
            return;
        }

        // event for unknown media, ignoring
        if (event.media == null) {
            AppLog.w(AppLog.T.MEDIA, "Media event carries null media object, not recognized");
            return;
        }

        if (event.isError()) {
            onUploadError(event.media, event.error);
        } else if (event.completed) {
            // if the remote url on completed is null, we consider this upload wasn't successful
            if (event.media.getUrl() == null) {
                MediaError error = new MediaError(MediaErrorType.GENERIC_ERROR);
                onUploadError(event.media, error);
            } else {
                onUploadSuccess(event.media);
            }
        } else {
            onUploadProgress(event.media, event.progress);
        }
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        if (event.causeOfChange instanceof CauseOfOnPostChanged.UpdatePost) {
            if (!event.isError()) {
                // here update the menu if it's not a draft anymore
                invalidateOptionsMenu();
            } else {
                updatePostLoadingAndDialogState(PostLoadingState.NONE);
                AppLog.e(AppLog.T.POSTS, "UPDATE_POST failed: " + event.error.type + " - " + event.error.message);
            }
        } else if (event.causeOfChange instanceof CauseOfOnPostChanged.RemoteAutoSavePost) {
            if (event.isError()) {
                AppLog.e(T.POSTS, "REMOTE_AUTO_SAVE_POST failed: " + event.error.type + " - " + event.error.message);
            }
            mPost = mPostStore.getPostByLocalPostId(mPost.getId());
            handleRemoteAutoSave(event.isError(), mPost);
        }
    }

    private boolean isRemotePreviewingFromEditor() {
        return mPostLoadingState == PostLoadingState.UPLOADING_FOR_PREVIEW
                || mPostLoadingState == PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW
                || mPostLoadingState == PostLoadingState.PREVIEWING
                || mPostLoadingState == PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR;
    }

    private boolean isUploadingPostForPreview() {
        return mPostLoadingState == PostLoadingState.UPLOADING_FOR_PREVIEW
                || mPostLoadingState == PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW;
    }

    private void updatePostOnSuccessfulUpload(PostModel post) {
        mPost = post;
        mIsNewPost = false;
        invalidateOptionsMenu();
    }

    private boolean isRemoteAutoSaveError() {
        return mPostLoadingState == PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR;
    }

    private void handleRemoteAutoSave(boolean isError, PostModel post) {
        // We are in the process of remote previewing a post from the editor
        if (!isError && isUploadingPostForPreview()) {
            // We were uploading post for preview and we got no error:
            // update post status and preview it in the internal browser
            updatePostOnSuccessfulUpload(post);
            ActivityLauncher.previewPostOrPageForResult(
                    EditPostActivity.this,
                    mSite,
                    post,
                    mPostLoadingState == PostLoadingState.UPLOADING_FOR_PREVIEW
                            ? RemotePreviewLogicHelper.RemotePreviewType.REMOTE_PREVIEW
                            : RemotePreviewLogicHelper.RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
                                                       );
            updatePostLoadingAndDialogState(PostLoadingState.PREVIEWING, mPost);
        } else if (isError || isRemoteAutoSaveError()) {
            // We got an error from the uploading or from the remote auto save of a post: show snackbar error
            updatePostLoadingAndDialogState(PostLoadingState.NONE);
            UploadUtils.showSnackbarError(findViewById(R.id.editor_activity),
                    getString(R.string.remote_preview_operation_error));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        final PostModel post = event.post;
        if (post != null && post.getId() == mPost.getId()) {
            if (!isRemotePreviewingFromEditor()) {
                // We are not remote previewing a post: show snackbar and update post status if needed
                View snackbarAttachView = findViewById(R.id.editor_activity);
                UploadUtils.onPostUploadedSnackbarHandler(this, snackbarAttachView, event.isError(), post,
                        event.isError() ? event.error.message : null, getSite(), mDispatcher);
                if (!event.isError()) {
                    updatePostOnSuccessfulUpload(post);
                }
            } else {
                handleRemoteAutoSave(event.isError(), post);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(VideoOptimizer.ProgressEvent event) {
        if (!isFinishing()) {
            // use upload progress rather than optimizer progress since the former includes upload+optimization
            float progress = UploadService.getUploadProgressForMedia(event.media);
            onUploadProgress(event.media, progress);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UploadService.UploadMediaRetryEvent event) {
        if (!isFinishing()
            && event.mediaModelList != null
            && mEditorMediaUploadListener != null) {
            for (MediaModel media : event.mediaModelList) {
                String localMediaId = String.valueOf(media.getId());
                EditorFragmentAbstract.MediaType mediaType = media.isVideo()
                        ? EditorFragmentAbstract.MediaType.VIDEO : EditorFragmentAbstract.MediaType.IMAGE;
                mEditorMediaUploadListener.onMediaUploadRetry(localMediaId, mediaType);
            }
        }
    }

    // EditPostActivityHook methods

    @Override
    public PostModel getPost() {
        return mPost;
    }

    @Override
    public SiteModel getSite() {
        return mSite;
    }


    // External Access to the Image Loader
    public AztecImageLoader getAztecImageLoader() {
        return mAztecImageLoader;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        // This is a workaround for bag discovered on Chromebooks, where Enter key will not work in the toolbar menu
        // Editor fragments are messing with window focus, which causes keyboard events to get ignored

        // this fixes issue with GB editor
        View editorFragmentView = mEditorFragment.getView();
        if (editorFragmentView != null) {
            editorFragmentView.requestFocus();
        }

        // this fixes issue with Aztec editor
        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).requestContentAreaFocus();
        }
        return super.onMenuOpened(featureId, menu);
    }
}
