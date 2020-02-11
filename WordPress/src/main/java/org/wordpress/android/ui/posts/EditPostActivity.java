package org.wordpress.android.ui.posts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;

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
import org.wordpress.android.editor.EditorImagePreviewListener;
import org.wordpress.android.editor.EditorImageSettingsListener;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorMediaUtils;
import org.wordpress.android.editor.GutenbergEditorFragment;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemoteAutoSavePost;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.PagePostCreationSourcesDetail;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.Shortcut;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.ui.media.MediaSettingsActivity;
import org.wordpress.android.ui.pages.SnackbarMessageHolder;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon;
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult;
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostSettingsCallback;
import org.wordpress.android.ui.posts.InsertMediaDialog.InsertMediaCallback;
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Editor;
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Outcome;
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.PreviewLogicOperationResult;
import org.wordpress.android.ui.posts.editor.EditorActionsProvider;
import org.wordpress.android.ui.posts.editor.EditorPhotoPicker;
import org.wordpress.android.ui.posts.editor.EditorPhotoPickerListener;
import org.wordpress.android.ui.posts.editor.EditorTracker;
import org.wordpress.android.ui.posts.editor.PostLoadingState;
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction;
import org.wordpress.android.ui.posts.editor.SecondaryEditorAction;
import org.wordpress.android.ui.posts.editor.StorePostViewModel;
import org.wordpress.android.ui.posts.editor.StorePostViewModel.ActivityFinishState;
import org.wordpress.android.ui.posts.editor.StorePostViewModel.UpdateFromEditor;
import org.wordpress.android.ui.posts.editor.StorePostViewModel.UpdateFromEditor.PostFields;
import org.wordpress.android.ui.posts.editor.media.EditorMedia;
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddExistingMediaSource;
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener;
import org.wordpress.android.ui.posts.reactnative.ReactNativeRequestHandler;
import org.wordpress.android.ui.posts.services.AztecImageLoader;
import org.wordpress.android.ui.posts.services.AztecVideoLoader;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stockmedia.StockMediaPickerActivity;
import org.wordpress.android.ui.uploads.PostEvents;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.uploads.UploadUtilsWrapper;
import org.wordpress.android.ui.uploads.VideoOptimizer;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutolinkUtils;
import org.wordpress.android.util.CrashLoggingUtils;
import org.wordpress.android.util.DateTimeUtilsWrapper;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.LocaleManagerWrapper;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ShortcutUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.BlockEditorEnabledSource;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder;
import org.wordpress.android.widgets.AppRatingDialog;
import org.wordpress.android.widgets.WPSnackbar;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.aztec.exceptions.DynamicLayoutGetBlockIndexOutOfBoundsException;
import org.wordpress.aztec.util.AztecLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE;
import static org.wordpress.android.ui.PagePostCreationSourcesDetail.CREATED_POST_SOURCE_DETAIL_KEY;
import static org.wordpress.android.ui.history.HistoryDetailContainerFragment.KEY_REVISION;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class EditPostActivity extends AppCompatActivity implements
        EditorFragmentActivity,
        EditorImageSettingsListener,
        EditorImagePreviewListener,
        EditorDragAndDropListener,
        EditorFragmentListener,
        OnRequestPermissionsResultCallback,
        PhotoPickerFragment.PhotoPickerListener,
        EditorPhotoPickerListener,
        EditorMediaListener,
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
    public static final String EXTRA_UPLOAD_NOT_STARTED = "savedAsLocalDraft";
    public static final String EXTRA_HAS_FAILED_MEDIA = "hasFailedMedia";
    public static final String EXTRA_HAS_CHANGES = "hasChanges";
    public static final String EXTRA_RESTART_EDITOR = "isSwitchingEditors";
    public static final String EXTRA_INSERT_MEDIA = "insertMedia";
    public static final String EXTRA_IS_NEW_POST = "isNewPost";
    public static final String EXTRA_CREATION_SOURCE_DETAIL = "creationSourceDetail";
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
    private static final String TAG_GB_INFORMATIVE_DIALOG = "tag_gb_informative_dialog";
    private static final String TAG_GB_ROLLOUT_V2_INFORMATIVE_DIALOG = "tag_gb_rollout_v2_informative_dialog";

    private static final int PAGE_CONTENT = 0;
    private static final int PAGE_SETTINGS = 1;
    private static final int PAGE_PUBLISH_SETTINGS = 2;
    private static final int PAGE_HISTORY = 3;

    private AztecImageLoader mAztecImageLoader;

    enum RestartEditorOptions {
        NO_RESTART,
        RESTART_SUPPRESS_GUTENBERG,
        RESTART_DONT_SUPPRESS_GUTENBERG,
    }

    private RestartEditorOptions mRestartEditorOption = RestartEditorOptions.NO_RESTART;

    private boolean mShowAztecEditor;
    private boolean mShowGutenbergEditor;

    private List<String> mPendingVideoPressInfoRequests;

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

    private Revision mRevision;

    private EditorFragmentAbstract mEditorFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditorMediaUploadListener mEditorMediaUploadListener;
    private EditorPhotoPicker mEditorPhotoPicker;

    private ProgressDialog mProgressDialog;
    private ProgressDialog mAddingMediaToEditorProgressDialog;

    private boolean mIsNewPost;
    private boolean mIsPage;
    private boolean mHasSetPostContent;
    private PostLoadingState mPostLoadingState = PostLoadingState.NONE;

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
    @Inject ReactNativeRequestHandler mReactNativeRequestHandler;
    @Inject EditorMedia mEditorMedia;
    @Inject LocaleManagerWrapper mLocaleManagerWrapper;
    @Inject EditPostRepository mEditPostRepository;
    @Inject PostUtilsWrapper mPostUtils;
    @Inject EditorTracker mEditorTracker;
    @Inject UploadUtilsWrapper mUploadUtilsWrapper;
    @Inject EditorActionsProvider mEditorActionsProvider;
    @Inject DateTimeUtilsWrapper mDateTimeUtils;
    @Inject ViewModelProvider.Factory mViewModelFactory;

    private StorePostViewModel mViewModel;

    private SiteModel mSite;

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
        mEditPostRepository.set(() -> {
            PostModel post = mPostStore.instantiatePostModel(mSite, mIsPage, null, null);
            post.setStatus(PostStatus.DRAFT.toString());
            return post;
        });
        mEditPostRepository.savePostSnapshot();
        EventBus.getDefault().postSticky(
                new PostEvents.PostOpenedInEditor(mEditPostRepository.getLocalSiteId(), mEditPostRepository.getId()));
        mShortcutUtils.reportShortcutUsed(Shortcut.CREATE_NEW_POST);
    }

    private void createPostEditorAnalyticsSessionTracker(boolean showGutenbergEditor, PostImmutableModel post,
                                                         SiteModel site, boolean isNewPost) {
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
        mViewModel =
                ViewModelProviders.of(this, mViewModelFactory).get(StorePostViewModel.class);
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
        mEditorPhotoPicker = new EditorPhotoPicker(this, this, this, mShowAztecEditor);

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

                mIsPage = extras.getBoolean(EXTRA_IS_PAGE);
                newPostSetup();
            } else {
                mEditPostRepository.loadPostByLocalPostId(extras.getInt(EXTRA_POST_LOCAL_ID));
                // Load post from extra)s

                if (mEditPostRepository.hasPost()) {
                    if (extras.getBoolean(EXTRA_LOAD_AUTO_SAVE_REVISION)) {
                        mEditPostRepository.update(postModel -> {
                            boolean updateTitle = !TextUtils.isEmpty(postModel.getAutoSaveTitle());
                            if (updateTitle) {
                                postModel.setTitle(postModel.getAutoSaveTitle());
                            }
                            boolean updateContent = !TextUtils.isEmpty(postModel.getAutoSaveContent());
                            if (updateContent) {
                                postModel.setContent(postModel.getAutoSaveContent());
                            }
                            boolean updateExcerpt = !TextUtils.isEmpty(postModel.getAutoSaveExcerpt());
                            if (updateExcerpt) {
                                postModel.setExcerpt(postModel.getAutoSaveExcerpt());
                            }
                            return updateTitle || updateContent || updateExcerpt;
                        });
                        mEditPostRepository.savePostSnapshot();
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
            mEditorMedia.setDroppedMediaUris(savedInstanceState.getParcelableArrayList(STATE_KEY_DROPPED_MEDIA_URIS));
            mIsNewPost = savedInstanceState.getBoolean(STATE_KEY_IS_NEW_POST, false);
            updatePostLoadingAndDialogState(PostLoadingState.fromInt(
                    savedInstanceState.getInt(STATE_KEY_POST_LOADING_STATE, 0)));
            mRevision = savedInstanceState.getParcelable(STATE_KEY_REVISION);
            mPostEditorAnalyticsSession =
                    (PostEditorAnalyticsSession) savedInstanceState.getSerializable(STATE_KEY_EDITOR_SESSION_DATA);

            // if we have a remote id saved, let's first try that, as the local Id might have changed after FETCH_POSTS
            if (savedInstanceState.containsKey(STATE_KEY_POST_REMOTE_ID)) {
                mEditPostRepository.loadPostByRemotePostId(savedInstanceState.getLong(STATE_KEY_POST_REMOTE_ID), mSite);
                initializePostObject();
            } else if (savedInstanceState.containsKey(STATE_KEY_POST_LOCAL_ID)) {
                mEditPostRepository.loadPostByLocalPostId(savedInstanceState.getInt(STATE_KEY_POST_LOCAL_ID));
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
        if (!mEditPostRepository.hasPost()) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        mEditorMedia.start(mSite, this);
        startObserving();

        if (mHasSetPostContent = mEditorFragment != null) {
            mEditorFragment.setImageLoader(mImageLoader);
        }

        // Ensure that this check happens when mPost is set
        if (savedInstanceState == null) {
            String restartEditorOptionName = getIntent().getStringExtra(EXTRA_RESTART_EDITOR);
            RestartEditorOptions restartEditorOption =
                    restartEditorOptionName == null ? RestartEditorOptions.RESTART_DONT_SUPPRESS_GUTENBERG
                            : RestartEditorOptions.valueOf(restartEditorOptionName);

            mShowGutenbergEditor =
                    PostUtils.shouldShowGutenbergEditor(mIsNewPost, mEditPostRepository.getContent(), mSite)
                    && restartEditorOption != RestartEditorOptions.RESTART_SUPPRESS_GUTENBERG;
        } else {
            mShowGutenbergEditor = savedInstanceState.getBoolean(STATE_KEY_GUTENBERG_IS_SHOWN);
        }

        // ok now we are sure to have both a valid Post and showGutenberg flag, let's start the editing session tracker
        createPostEditorAnalyticsSessionTracker(mShowGutenbergEditor, mEditPostRepository.getPost(), mSite, mIsNewPost);

        // Bump post created analytics only once, first time the editor is opened
        if (mIsNewPost && savedInstanceState == null) {
            trackEditorCreatedPost(action, getIntent());
        }

        if (!mIsNewPost) {
            // if we are opening a Post for which an error notification exists, we need to remove it from the dashboard
            // to prevent the user from tapping RETRY on a Post that is being currently edited
            UploadService.cancelFinalNotification(this, mEditPostRepository.getPost());
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
                    setTitle(mEditPostRepository.isPage() ? R.string.page_settings : R.string.post_settings);
                    mEditorPhotoPicker.hidePhotoPicker();
                } else if (position == PAGE_PUBLISH_SETTINGS) {
                    setTitle(R.string.publish_date);
                    mEditorPhotoPicker.hidePhotoPicker();
                } else if (position == PAGE_HISTORY) {
                    setTitle(R.string.history_title);
                    mEditorPhotoPicker.hidePhotoPicker();
                }
            }
        });

        ActivityId.trackLastActivity(ActivityId.POST_EDITOR);
    }

    private void startObserving() {
        mEditorMedia.getUiState().observe(this, uiState -> {
            if (uiState != null) {
                updateAddingMediaToEditorProgressDialogState(uiState.getProgressDialogUiState());
                if (uiState.getEditorOverlayVisibility()) {
                    showOverlay(false);
                } else {
                    hideOverlay();
                }
            }
        });
        mEditorMedia.getSnackBarMessage().observe(this, event -> {
            SnackbarMessageHolder messageHolder = event.getContentIfNotHandled();
            if (messageHolder != null) {
                WPSnackbar
                        .make(findViewById(R.id.editor_activity), messageHolder.getMessageRes(), Snackbar.LENGTH_SHORT)
                        .show();
            }
        });
        mEditorMedia.getToastMessage().observe(this, event -> {
            ToastMessageHolder contentIfNotHandled = event.getContentIfNotHandled();
            if (contentIfNotHandled != null) {
                contentIfNotHandled.show(this);
            }
        });
        mViewModel.getOnSavePostTriggered().observe(this, unitEvent -> unitEvent.applyIfNotHandled(unit -> {
            updateAndSavePostAsync();
            return null;
        }));
        mViewModel.getOnFinish().observe(this, finishEvent -> finishEvent.applyIfNotHandled(activityFinishState -> {
            switch (activityFinishState) {
                case SAVED_ONLINE:
                    saveResult(true, false);
                    break;
                case SAVED_LOCALLY:
                    saveResult(true, true);
                    break;
                case CANCELLED:
                    saveResult(false, true);
                    break;
            }
            removePostOpenInEditorStickyEvent();
            mEditorMedia.definitelyDeleteBackspaceDeletedMediaItemsAsync();
            finish();
            return null;
        }));
        mEditPostRepository.getPostChanged().observe(this, postEvent -> postEvent.applyIfNotHandled(post -> {
            mViewModel.savePostToDb(this, mEditPostRepository, mSite);
            return null;
        }));
    }

    private void initializePostObject() {
        if (mEditPostRepository.hasPost()) {
            mEditPostRepository.savePostSnapshotWhenEditorOpened();
            mEditPostRepository.replace(UploadService::updatePostWithCurrentlyCompletedUploads);
            mIsPage = mEditPostRepository.isPage();

            EventBus.getDefault().postSticky(new PostEvents.PostOpenedInEditor(mEditPostRepository.getLocalSiteId(),
                    mEditPostRepository.getId()));

            mEditorMedia.purgeMediaToPostAssociationsIfNotInPostAnymoreAsync();
        }
    }

    // this method aims at recovering the current state of media items if they're inconsistent within the PostModel.
    private void resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued() {
        boolean useAztec = AppPrefs.isAztecEditorEnabled();

        if (!useAztec || UploadService.hasPendingOrInProgressMediaUploadsForPost(mEditPostRepository.getPost())) {
            return;
        }
        mEditPostRepository.updateAsync(postModel -> {
            String oldContent = postModel.getContent();
            if (!AztecEditorFragment.hasMediaItemsMarkedUploading(EditPostActivity.this, oldContent)
                // we need to make sure items marked failed are still failed or not as well
                && !AztecEditorFragment.hasMediaItemsMarkedFailed(EditPostActivity.this, oldContent)) {
                return false;
            }

            String newContent = AztecEditorFragment.resetUploadingMediaToFailed(EditPostActivity.this, oldContent);

            if (!TextUtils.isEmpty(oldContent) && newContent != null && oldContent.compareTo(newContent) != 0) {
                postModel.setContent(newContent);
                return true;
            }
            return false;
        }, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        reattachUploadingMediaForAztec();

        // Bump editor opened event every time the activity is resumed, to match the EDITOR_CLOSED event onPause
        PostUtils.trackOpenEditorAnalytics(mEditPostRepository.getPost(), mSite);

        mIsConfigChange = false;
    }

    private void reattachUploadingMediaForAztec() {
        if (mEditorMediaUploadListener != null) {
            mEditorMedia.reattachUploadingMediaForAztec(
                    mEditPostRepository,
                    mEditorFragment instanceof AztecEditorFragment,
                    mEditorMediaUploadListener
            );
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
        mEditorMedia.cancelAddMediaToEditorActions();
        removePostOpenInEditorStickyEvent();
        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).disableContentLogOnCrashes();
        }

        if (mReactNativeRequestHandler != null) {
            mReactNativeRequestHandler.destroy();
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
        updateAndSavePostAsync();
        outState.putInt(STATE_KEY_POST_LOCAL_ID, mEditPostRepository.getId());
        if (!mEditPostRepository.isLocalDraft()) {
            outState.putLong(STATE_KEY_POST_REMOTE_ID, mEditPostRepository.getRemotePostId());
        }
        outState.putInt(STATE_KEY_POST_LOADING_STATE, mPostLoadingState.getValue());
        outState.putBoolean(STATE_KEY_IS_NEW_POST, mIsNewPost);
        outState.putBoolean(STATE_KEY_IS_PHOTO_PICKER_VISIBLE, mEditorPhotoPicker.isPhotoPickerShowing());
        outState.putBoolean(STATE_KEY_HTML_MODE_ON, mHtmlModeMenuStateOn);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putParcelable(STATE_KEY_REVISION, mRevision);

        outState.putSerializable(STATE_KEY_EDITOR_SESSION_DATA, mPostEditorAnalyticsSession);
        mIsConfigChange = true; // don't call sessionData.end() in onDestroy() if this is an Android config change

        outState.putBoolean(STATE_KEY_GUTENBERG_IS_SHOWN, mShowGutenbergEditor);

        outState.putParcelableArrayList(STATE_KEY_DROPPED_MEDIA_URIS, mEditorMedia.getDroppedMediaUris());

        if (mEditorFragment != null) {
            getSupportFragmentManager().putFragment(outState, STATE_KEY_EDITOR_FRAGMENT, mEditorFragment);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mHtmlModeMenuStateOn = savedInstanceState.getBoolean(STATE_KEY_HTML_MODE_ON);
        if (savedInstanceState.getBoolean(STATE_KEY_IS_PHOTO_PICKER_VISIBLE, false)) {
            mEditorPhotoPicker.showPhotoPicker(mSite);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mEditorPhotoPicker.onOrientationChanged(newConfig.orientation);
    }

    private PrimaryEditorAction getPrimaryAction() {
        return mEditorActionsProvider
                .getPrimaryAction(mEditPostRepository.getStatus(), UploadUtils.userCanPublish(mSite));
    }

    private String getPrimaryActionText() {
        return getString(getPrimaryAction().getTitleResource());
    }

    private SecondaryEditorAction getSecondaryAction() {
        return mEditorActionsProvider
                .getSecondaryAction(mEditPostRepository.getStatus(), UploadUtils.userCanPublish(mSite));
    }

    private @Nullable String getSecondaryActionText() {
        @StringRes Integer titleResource = getSecondaryAction().getTitleResource();
        return titleResource != null ? getString(titleResource) : null;
    }

    private boolean shouldSwitchToGutenbergBeVisible(
            EditorFragmentAbstract editorFragment,
            SiteModel site
    ) {
        // Some guard conditions
        if (!mEditPostRepository.hasPost()) {
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
            final String content = (String) editorFragment.getContent(mEditPostRepository.getContent());
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

    @Override
    public void onPhotoPickerShown() {
        // animate in the editor overlay
        showOverlay(true);

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).enableMediaMode(true);
        }
    }

    @Override
    public void onPhotoPickerHidden() {
        hideOverlay();

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment) mEditorFragment).enableMediaMode(false);
        }
    }

    /*
     * called by PhotoPickerFragment when media is selected - may be a single item or a list of items
     */
    @Override
    public void onPhotoPickerMediaChosen(@NonNull final List<Uri> uriList) {
        mEditorPhotoPicker.hidePhotoPicker();
        mEditorMedia.onPhotoPickerMediaChosen(uriList);
    }

    /*
     * called by PhotoPickerFragment when user clicks an icon to launch the camera, native
     * picker, or WP media picker
     */
    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerIcon icon, boolean allowMultipleSelection) {
        mEditorPhotoPicker.hidePhotoPicker();
        if (!icon.requiresUploadPermission() || WPMediaUtils.currentUserCanUploadMedia(mSite)) {
            mEditorPhotoPicker.setAllowMultipleSelection(allowMultipleSelection);
            switch (icon) {
                case ANDROID_CAPTURE_PHOTO:
                    launchCamera();
                    break;
                case ANDROID_CAPTURE_VIDEO:
                    launchVideoCamera();
                    break;
                case ANDROID_CHOOSE_PHOTO_OR_VIDEO:
                    WPMediaUtils.launchMediaLibrary(this, allowMultipleSelection);
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
            }
        } else {
            WPSnackbar.make(findViewById(R.id.editor_activity), R.string.media_error_no_permission_upload,
                            Snackbar.LENGTH_SHORT).show();
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

        if (secondaryAction != null && mEditPostRepository.hasPost()) {
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
            boolean hasHistory = !mIsNewPost && mSite.isUsingWpComRestApi();
            historyMenuItem.setVisible(showMenuItems && hasHistory);
        }

        if (settingsMenuItem != null) {
            settingsMenuItem.setTitle(mIsPage ? R.string.page_settings : R.string.post_settings);
            settingsMenuItem.setVisible(showMenuItems);
        }

        // Set text of the primary action button in the ActionBar
        if (mEditPostRepository.hasPost()) {
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
                        shouldSwitchToGutenbergBeVisible(mEditorFragment, mSite)
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
                    mEditorMedia.addNewMediaItemsToEditorAsync(mEditorMedia.getDroppedMediaUris(), false);
                    mEditorMedia.getDroppedMediaUris().clear();
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
                mEditorFragment.setFeaturedImageId(mEditPostRepository.getFeaturedImageId());
            }

            mViewPager.setCurrentItem(PAGE_CONTENT);
            invalidateOptionsMenu();
        } else if (mEditorPhotoPicker.isPhotoPickerShowing()) {
            mEditorPhotoPicker.hidePhotoPicker();
        } else {
            savePostAndOptionallyFinish(true, false);
        }

        return true;
    }

    private RemotePreviewLogicHelper.RemotePreviewHelperFunctions getEditPostActivityStrategyFunctions() {
        return new RemotePreviewLogicHelper.RemotePreviewHelperFunctions() {
            @Override
            public boolean notifyUploadInProgress(@NotNull PostImmutableModel post) {
                if (UploadService.hasInProgressMediaUploadsForPost(post)) {
                    ToastUtils.showToast(EditPostActivity.this,
                            getString(R.string.editor_toast_uploading_please_wait), Duration.SHORT);
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void notifyEmptyDraft() {
                ToastUtils.showToast(EditPostActivity.this,
                        getString(R.string.error_preview_empty_draft), Duration.SHORT);
            }

            @Override
            public void startUploading(boolean isRemoteAutoSave, @Nullable PostImmutableModel post) {
                if (isRemoteAutoSave) {
                    updatePostLoadingAndDialogState(PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW, post);
                    savePostAndOptionallyFinish(false, true);
                } else {
                    updatePostLoadingAndDialogState(PostLoadingState.UPLOADING_FOR_PREVIEW, post);
                    savePostAndOptionallyFinish(false, false);
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

        mEditorPhotoPicker.hidePhotoPicker();

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
                        Objects.requireNonNull(mEditPostRepository.getPost()),
                        getEditPostActivityStrategyFunctions());
                if (opResult == PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS
                    || opResult == PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT
                    || opResult == PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
                ) {
                    return false;
                } else if (opResult == PreviewLogicOperationResult.OPENING_PREVIEW) {
                    updatePostLoadingAndDialogState(PostLoadingState.PREVIEWING, mEditPostRepository.getPost());
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
                    mViewModel.finish(ActivityFinishState.SAVED_LOCALLY);
                } else {
                    logWrongMenuState("Wrong state in menu_switch_to_aztec: menu should not be visible.");
                }
            } else if (itemId == R.id.menu_switch_to_gutenberg) {
                // The following boolean check should be always redundant but was added to manage
                // an odd behaviour recorded with Android 8.0.0
                // (see https://github.com/wordpress-mobile/WordPress-Android/issues/9748 for more information)
                if (shouldSwitchToGutenbergBeVisible(mEditorFragment, mSite)) {
                    // let's finish this editing instance and start again, but let GB be used
                    mRestartEditorOption = RestartEditorOptions.RESTART_DONT_SUPPRESS_GUTENBERG;
                    mPostEditorAnalyticsSession.switchEditor(Editor.GUTENBERG);
                    mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
                    mViewModel.finish(ActivityFinishState.SAVED_LOCALLY);
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
        mEditPostSettingsFragment.updatePostStatus(PostStatus.DRAFT);
        ToastUtils.showToast(EditPostActivity.this,
                getString(R.string.editor_post_converted_back_to_draft), Duration.SHORT);
        mUploadUtilsWrapper.showSnackbar(
                findViewById(R.id.editor_activity),
                R.string.editor_uploading_post);
        savePostAndOptionallyFinish(false, false);
    }

    private boolean performSecondaryAction() {
        if (UploadService.hasInProgressMediaUploadsForPost(mEditPostRepository.getPost())) {
            ToastUtils.showToast(EditPostActivity.this,
                    getString(R.string.editor_toast_uploading_please_wait), Duration.SHORT);
            return false;
        }

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
        mUploadUtilsWrapper.showSnackbarSuccessActionOrange(findViewById(R.id.editor_activity),
                mHtmlModeMenuStateOn ? R.string.menu_html_mode_done_snackbar
                        : R.string.menu_visual_mode_done_snackbar,
                R.string.menu_undo_snackbar_action,
                onUndoClickListener);
    }

    private void refreshEditorContent() {
        mHasSetPostContent = false;
        fillContentEditorFields();
    }

    private void setPreviewingInEditorSticky(boolean enable, @Nullable PostImmutableModel post) {
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

    private void managePostLoadingStateTransitions(PostLoadingState postLoadingState,
                                                   @Nullable PostImmutableModel post) {
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

    private void updatePostLoadingAndDialogState(PostLoadingState postLoadingState, @Nullable PostImmutableModel post) {
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
                mEditPostRepository.isPage() ? getString(R.string.dialog_confirm_update_message_page)
                        : getString(R.string.dialog_confirm_update_message_post),
                getString(R.string.dialog_confirm_update_yes),
                getString(R.string.keep_editing));
    }

    private void showPublishConfirmationDialogAndPublishPost() {
        showConfirmationDialogAndUploadPost(TAG_PUBLISH_CONFIRMATION_DIALOG,
                getString(R.string.dialog_confirm_publish_title),
                mEditPostRepository.isPage() ? getString(R.string.dialog_confirm_publish_message_page)
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
                mEditPostRepository.isPage() ? getString(R.string.dialog_gutenberg_informative_description_page)
                        : getString(R.string.dialog_gutenberg_informative_description_post),
                getString(org.wordpress.android.editor.R.string.dialog_button_ok));

        gbInformativeDialog.show(getSupportFragmentManager(), TAG_GB_INFORMATIVE_DIALOG);
        AppPrefs.setGutenbergInfoPopupDisplayed(mSite.getUrl(), true);
    }

    private void showGutenbergRolloutV2InformativeDialog() {
        // Show the GB informative dialog on editing GB posts
        final PromoDialog gbInformativeDialog = new PromoDialog();
        gbInformativeDialog.initialize(TAG_GB_ROLLOUT_V2_INFORMATIVE_DIALOG,
                getString(R.string.dialog_gutenberg_informative_title),
                getString(R.string.dialog_gutenberg_informative_description_v2),
                getString(org.wordpress.android.editor.R.string.dialog_button_ok));

        gbInformativeDialog.show(getSupportFragmentManager(), TAG_GB_ROLLOUT_V2_INFORMATIVE_DIALOG);
        AppPrefs.setGutenbergInfoPopupDisplayed(mSite.getUrl(), true);
    }

    private void setGutenbergEnabledIfNeeded() {
        if (AppPrefs.isGutenbergInfoPopupDisplayed(mSite.getUrl())) {
            return;
        }

        boolean showPopup = AppPrefs.shouldShowGutenbergInfoPopupForTheNewPosts(mSite.getUrl());
        boolean showRolloutPopupPhase2 = AppPrefs.shouldShowGutenbergInfoPopupPhase2ForNewPosts(mSite.getUrl());

        if (TextUtils.isEmpty(mSite.getMobileEditor()) && !mIsNewPost) {
            SiteUtils.enableBlockEditor(mDispatcher, mSite);
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_GUTENBERG_ENABLED, mSite,
                    BlockEditorEnabledSource.ON_BLOCK_POST_OPENING.asPropertyMap());
            showPopup = true;
        }

        if (showPopup) {
            showGutenbergInformativeDialog();
        } else if (showRolloutPopupPhase2) {
            showGutenbergRolloutV2InformativeDialog();
        }
    }

    private ActivityFinishState savePostOnline(boolean isFirstTimePublish) {
        return mViewModel.savePostOnline(isFirstTimePublish, this, mEditPostRepository, mSite);
    }

    private void onUploadSuccess(MediaModel media) {
        // TODO Should this statement check media.getLocalPostId() == mEditPostRepository.getId()?
        if (media != null && !media.getMarkedLocallyAsFeatured() && mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadSucceeded(String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
        } else if (media != null && media.getMarkedLocallyAsFeatured() && media.getLocalPostId() == mEditPostRepository
                .getId()) {
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
        WPMediaUtils.launchPictureLibrary(this, mEditorPhotoPicker.getAllowMultipleSelection());
    }

    private void launchVideoLibrary() {
        WPMediaUtils.launchVideoLibrary(this, mEditorPhotoPicker.getAllowMultipleSelection());
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
        PostUtils.addPostTypeToAnalyticsProperties(mEditPostRepository.getPost(), properties);
        properties.put("created_post_source", normalizedSourceName);

        if (intent != null
            && intent.hasExtra(EXTRA_CREATION_SOURCE_DETAIL)
            && normalizedSourceName == "post-list") {
            PagePostCreationSourcesDetail source =
                    (PagePostCreationSourcesDetail) intent.getSerializableExtra(EXTRA_CREATION_SOURCE_DETAIL);
            properties.put(
                    CREATED_POST_SOURCE_DETAIL_KEY,
                    source != null ? source.getLabel() : PagePostCreationSourcesDetail.NO_DETAIL.getLabel()
            );
        } else {
            properties.put(
                    CREATED_POST_SOURCE_DETAIL_KEY,
                    PagePostCreationSourcesDetail.NO_DETAIL.getLabel()
            );
        }

        AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.EDITOR_CREATED_POST,
                mSiteStore.getSiteByLocalId(mEditPostRepository.getLocalSiteId()),
                properties
        );
    }

    private void updateAndSavePostAsync() {
        mViewModel.updatePostObjectWithUIAsync(mEditPostRepository, this::updateFromEditor, null);
    }

    private void updateAndSavePostAsync(final OnPostUpdatedFromUIListener listener) {
        if (mEditorFragment == null) {
            AppLog.e(AppLog.T.POSTS, "Fragment not initialized");
            return;
        }
        mViewModel.updatePostObjectWithUIAsync(mEditPostRepository,
                this::updateFromEditor,
                (post, result) -> {
                    // Ignore the result as we want to invoke the listener even when the PostModel was up-to-date
                    if (listener != null) {
                        listener.onPostUpdatedFromUI();
                    }
                    return null;
                });
    }

    private UpdateFromEditor updateFromEditor(String oldContent) {
        try {
            String title = (String) mEditorFragment.getTitle();
            String content = (String) mEditorFragment.getContent(oldContent);
            return new PostFields(title, content);
        } catch (EditorFragmentNotAddedException e) {
            AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
            return new UpdateFromEditor.Failed(e);
        }
    }

    @Override
    public void initializeEditorFragment() {
        if (mEditorFragment instanceof AztecEditorFragment) {
            AztecEditorFragment aztecEditorFragment = (AztecEditorFragment) mEditorFragment;
            aztecEditorFragment.setEditorImageSettingsListener(EditPostActivity.this);
            aztecEditorFragment.setMediaToolbarButtonClickListener(mEditorPhotoPicker);

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
                            return mEditPostRepository.hasPost() && TextUtils.isEmpty(mEditPostRepository.getPassword())
                                   && !mEditPostRepository.hasStatus(PostStatus.PRIVATE);
                        }
                );
            }

            if (mEditPostRepository.hasPost() && AppPrefs
                    .isPostWithHWAccelerationOff(mEditPostRepository.getLocalSiteId(), mEditPostRepository.getId())) {
                // We need to disable HW Acc. on this post
                aztecEditorFragment.disableHWAcceleration();
            }
            aztecEditorFragment.setExternalLogger(new AztecLog.ExternalLogger() {
                // This method handles the custom Exception thrown by Aztec to notify the parent app of the error #8828
                // We don't need to log the error, since it was already logged by Aztec, instead we need to write the
                // prefs to disable HW acceleration for it.
                private boolean isError8828(@NotNull Throwable throwable) {
                    if (!(throwable instanceof DynamicLayoutGetBlockIndexOutOfBoundsException)) {
                        return false;
                    }
                    if (!mEditPostRepository.hasPost()) {
                        return false;
                    }
                    AppPrefs.addPostWithHWAccelerationOff(mEditPostRepository.getLocalSiteId(),
                            mEditPostRepository.getId());
                    return true;
                }

                @Override
                public void log(@NotNull String s) {
                    // For now, we're wrapping up the actual log into an exception to reduce possibility
                    // of information not travelling to our Crash Logging Service.
                    // For more info: http://bit.ly/2oJHMG7 and http://bit.ly/2oPOtFX
                    CrashLoggingUtils.logException(new AztecEditorFragment.AztecLoggingException(s), T.EDITOR);
                }

                @Override
                public void logException(@NotNull Throwable throwable) {
                    if (isError8828(throwable)) {
                        return;
                    }
                    CrashLoggingUtils.logException(new AztecEditorFragment.AztecLoggingException(throwable), T.EDITOR);
                }

                @Override
                public void logException(@NotNull Throwable throwable, String s) {
                    if (isError8828(throwable)) {
                        return;
                    }
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


    @Override public void onImagePreviewRequested(String mediaUrl) {
        MediaPreviewActivity.showPreview(this, null, mediaUrl);
    }

    @Override
    public void onNegativeClicked(@NonNull String instanceTag) {
        switch (instanceTag) {
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
            case TAG_GB_INFORMATIVE_DIALOG:
                // no op
                break;
            case TAG_GB_ROLLOUT_V2_INFORMATIVE_DIALOG:
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

    public interface OnPostUpdatedFromUIListener {
        void onPostUpdatedFromUI();
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    @Override
    public void onHistoryItemClicked(@NonNull Revision revision, @NonNull List<Revision> revisions) {
        AnalyticsTracker.track(Stat.REVISIONS_DETAIL_VIEWED_FROM_LIST);
        mRevision = revision;

        ActivityLauncher.viewHistoryDetailForResult(this, mRevision, revisions);
    }

    private void loadRevision() {
        updatePostLoadingAndDialogState(PostLoadingState.LOADING_REVISION);
        mEditPostRepository.saveForUndo();
        mEditPostRepository.updateAsync(postModel -> {
            postModel.setTitle(Objects.requireNonNull(mRevision.getPostTitle()));
            postModel.setContent(Objects.requireNonNull(mRevision.getPostContent()));
            return true;
        }, (postModel, result) -> {
            if (result == UpdatePostResult.Updated.INSTANCE) {
                refreshEditorContent();
                WPSnackbar.make(mViewPager, getString(R.string.history_loaded_revision), 4000)
                          .setAction(getString(R.string.undo), view -> {
                              AnalyticsTracker.track(Stat.REVISIONS_LOAD_UNDONE);
                              RemotePostPayload payload =
                                      new RemotePostPayload(mEditPostRepository.getPostForUndo(), mSite);
                              mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload));
                              mEditPostRepository.undo();
                              refreshEditorContent();
                          })
                          .show();

                updatePostLoadingAndDialogState(PostLoadingState.NONE);
            }
            return null;
        });
    }

    private boolean isNewPost() {
        return mIsNewPost;
    }

    private void saveResult(boolean saved, boolean uploadNotStarted) {
        Intent i = getIntent();
        i.putExtra(EXTRA_UPLOAD_NOT_STARTED, uploadNotStarted);
        i.putExtra(EXTRA_HAS_FAILED_MEDIA, hasFailedMedia());
        i.putExtra(EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EXTRA_HAS_CHANGES, saved);
        i.putExtra(EXTRA_POST_LOCAL_ID, mEditPostRepository.getId());
        i.putExtra(EXTRA_POST_REMOTE_ID, mEditPostRepository.getRemotePostId());
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
                               savePostAndOptionallyFinish(true, false);
                           })
                   .setNegativeButton(R.string.editor_confirm_email_prompt_negative,
                           (dialog, id) -> mDispatcher
                                   .dispatch(AccountActionBuilder.newSendVerificationEmailAction()));
            builder.create().show();
            return;
        }
        if (!mPostUtils.isPublishable(mEditPostRepository.getPost())) {
            // TODO we don't want to show "publish" message when the user clicked on eg. save
            mEditPostRepository.updateStatusFromPostSnapshotWhenEditorOpened();
            EditPostActivity.this.runOnUiThread(() -> {
                String message = getString(
                        mIsPage ? R.string.error_publish_empty_page : R.string.error_publish_empty_post);
                ToastUtils.showToast(EditPostActivity.this, message, Duration.SHORT);
            });
            return;
        }

        // Loading the content from the GB HTML editor can take time on long posts.
        // Let's show a progress dialog for now. Ref: https://github.com/wordpress-mobile/gutenberg-mobile/issues/713
        mEditorFragment.showSavingProgressDialogIfNeeded();

        boolean isFirstTimePublish = isFirstTimePublish(publishPost);
        mEditPostRepository.updateAsync(postModel -> {
            if (publishPost) {
                // now set status to PUBLISHED - only do this AFTER we have run the isFirstTimePublish() check,
                // otherwise we'd have an incorrect value
                // also re-set the published date in case it was SCHEDULED and they want to publish NOW
                if (postModel.getStatus().equals(PostStatus.SCHEDULED.toString())) {
                    postModel.setDateCreated(mDateTimeUtils.currentTimeInIso8601());
                }
                postModel.setStatus(PostStatus.PUBLISHED.toString());
                mPostEditorAnalyticsSession.setOutcome(Outcome.PUBLISH);
            } else {
                mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
            }

            AppLog.d(T.POSTS, "User explicitly confirmed changes. Post Title: " + postModel.getTitle());
            // the user explicitly confirmed an intention to upload the post
            postModel.setChangesConfirmedContentHashcode(postModel.contentHashcode());

            // Hide the progress dialog now
            mEditorFragment.hideSavingProgressDialog();
            return true;
        }, (postModel, result) -> {
            if (result == UpdatePostResult.Updated.INSTANCE) {
                ActivityFinishState activityFinishState = savePostOnline(isFirstTimePublish);
                mViewModel.finish(activityFinishState);
            }
            return null;
        });
    }

    private void savePostAndOptionallyFinish(final boolean doFinish, final boolean forceSave) {
        if (mEditorFragment == null || !mEditorFragment.isAdded()) {
            AppLog.e(AppLog.T.POSTS, "Fragment not initialized");
            return;
        }
        // check if the opened post had some unsaved local changes
        boolean isFirstTimePublish = isFirstTimePublish(false);

        // if post was modified during this editing session, save it
        boolean shouldSave = shouldSavePost() || forceSave;

        mPostEditorAnalyticsSession.setOutcome(Outcome.SAVE);
        ActivityFinishState activityFinishState = ActivityFinishState.CANCELLED;
        if (shouldSave) {
            /*
             * Remote-auto-save isn't supported on self-hosted sites. We can save the post online (as draft)
             * only when it doesn't exist in the remote yet. When it does exist in the remote, we can upload
             * it only when the user explicitly confirms the changes - eg. clicks on save/publish/submit. The
             * user didn't confirm the changes in this code path.
             */
            boolean isWpComOrIsLocalDraft = mSite.isUsingWpComRestApi() || mEditPostRepository.isLocalDraft();
            if (isWpComOrIsLocalDraft) {
                activityFinishState = savePostOnline(isFirstTimePublish);
            } else if (forceSave) {
                activityFinishState = savePostOnline(false);
            } else {
                activityFinishState = ActivityFinishState.SAVED_LOCALLY;
            }
        }
        // discard post if new & empty
        if (isDiscardable()) {
            mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(mEditPostRepository.getEditablePost()));
            mPostEditorAnalyticsSession.setOutcome(Outcome.CANCEL);
            activityFinishState = ActivityFinishState.CANCELLED;
        }
        if (doFinish) {
            mViewModel.finish(activityFinishState);
        }
    }

    private boolean shouldSavePost() {
        boolean hasChanges = mEditPostRepository.postWasChangedInCurrentSession();
        boolean isPublishable = mEditPostRepository.isPostPublishable();

        boolean existingPostWithChanges = mEditPostRepository.hasPostSnapshotWhenEditorOpened() && hasChanges;
        // if post was modified during this editing session, save it
        return isPublishable && (existingPostWithChanges || isNewPost());
    }


    private boolean isDiscardable() {
        return !mEditPostRepository.isPostPublishable() && isNewPost();
    }

    private boolean isFirstTimePublish(final boolean publishPost) {
        final PostStatus originalStatus = mEditPostRepository.getStatus();
        return ((originalStatus == PostStatus.DRAFT || originalStatus == PostStatus.UNKNOWN) && publishPost)
               || (originalStatus == PostStatus.SCHEDULED && publishPost)
               || (originalStatus == PostStatus.PUBLISHED && mEditPostRepository.isLocalDraft())
               || (originalStatus == PostStatus.PUBLISHED && mEditPostRepository.getRemotePostId() == 0);
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
                        String postType = mIsPage ? "page" : "post";
                        String languageString = LocaleManager.getLanguage(EditPostActivity.this);
                        String wpcomLocaleSlug = languageString.replace("_", "-").toLowerCase(Locale.ENGLISH);
                        boolean supportsStockPhotos = mSite.isUsingWpComRestApi();
                        return GutenbergEditorFragment.newInstance("",
                                "",
                                postType,
                                mIsNewPost,
                                wpcomLocaleSlug,
                                supportsStockPhotos);
                    } else {
                        // If gutenberg editor is not selected, default to Aztec.
                        return AztecEditorFragment.newInstance("", "", AppPrefs.isAztecEditorToolbarExpanded());
                    }
                case PAGE_SETTINGS:
                    return EditPostSettingsFragment.newInstance();
                case PAGE_PUBLISH_SETTINGS:
                    return EditPostPublishSettingsFragment.Companion.newInstance();
                case PAGE_HISTORY:
                    return HistoryListFragment.Companion.newInstance(mEditPostRepository.getId(), mSite);
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
                        mViewModel.savePostWithDelay();
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
                MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(mEditorMedia
                        .updateMediaUploadStateBlocking(uri, MediaUploadState.FAILED));
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

    private String migrateToGutenbergEditor(String content) {
        return "<!-- wp:paragraph --><p>" + content + "</p><!-- /wp:paragraph -->";
    }

    private void fillContentEditorFields() {
        // Needed blog settings needed by the editor
        mEditorFragment.setFeaturedImageSupported(mSite.isFeaturedImageSupported());

        // Special actions - these only make sense for empty posts that are going to be populated now
        if (TextUtils.isEmpty(mEditPostRepository.getContent())) {
            String action = getIntent().getAction();
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                setPostContentFromShareAction();
            } else if (NEW_MEDIA_POST.equals(action)) {
                mEditorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY,
                        getIntent().getLongArrayExtra(NEW_MEDIA_POST_EXTRA_IDS));
            }
        }

        // Set post title and content
        if (mEditPostRepository.hasPost()) {
            // don't avoid calling setContent() for GutenbergEditorFragment so RN gets initialized
            if ((!TextUtils.isEmpty(mEditPostRepository.getContent())
                 || mEditorFragment instanceof GutenbergEditorFragment)
                && !mHasSetPostContent) {
                mHasSetPostContent = true;
                // TODO: Might be able to drop .replaceAll() when legacy editor is removed
                String content = mEditPostRepository.getContent().replaceAll("\uFFFC", "");
                // Prepare eventual legacy editor local draft for the new editor
                content = migrateLegacyDraft(content);
                mEditorFragment.setContent(content);
            }
            if (!TextUtils.isEmpty(mEditPostRepository.getTitle())) {
                mEditorFragment.setTitle(mEditPostRepository.getTitle());
            } else if (mEditorFragment instanceof GutenbergEditorFragment) {
                // don't avoid calling setTitle() for GutenbergEditorFragment so RN gets initialized
                mEditorFragment.setTitle("");
            }

            // TODO: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setFeaturedImageId(mEditPostRepository.getFeaturedImageId());
        }
    }

    private void launchCamera() {
        WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                mediaCapturePath -> mMediaCapturePath = mediaCapturePath);
    }

    protected void setPostContentFromShareAction() {
        Intent intent = getIntent();

        // Check for shared text
        final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        final String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (text != null) {
            mHasSetPostContent = true;
            mEditPostRepository.updateAsync(postModel -> {
                if (title != null) {
                    postModel.setTitle(title);
                }
                // Create an <a href> element around links
                String updatedContent = AutolinkUtils.autoCreateLinks(text);

                // If editor is Gutenberg, add Gutenberg block around content
                if (mShowGutenbergEditor) {
                    updatedContent = migrateToGutenbergEditor(updatedContent);
                }

                // update PostModel
                postModel.setContent(updatedContent);
                mEditPostRepository.updatePublishDateIfShouldBePublishedImmediately(postModel);
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    mEditorFragment.setTitle(postModel.getTitle());
                    mEditorFragment.setContent(postModel.getContent());
                }
                return null;
            });
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
                mEditorMedia.addNewMediaItemsToEditorAsync(sharedUris, false);
            }
        }
    }

    private void setFeaturedImageId(final long mediaId) {
        if (mEditPostSettingsFragment != null) {
            mEditPostSettingsFragment.updateFeaturedImage(mediaId);
        }
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
            // for all media related intents, let editor fragment know about cancellation
            switch (requestCode) {
                case RequestCodes.MULTI_SELECT_MEDIA_PICKER:
                case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
                case RequestCodes.PHOTO_PICKER:
                case RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT:
                case RequestCodes.MEDIA_LIBRARY:
                case RequestCodes.PICTURE_LIBRARY:
                case RequestCodes.TAKE_PHOTO:
                case RequestCodes.VIDEO_LIBRARY:
                case RequestCodes.TAKE_VIDEO:
                case RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT:
                    mEditorFragment.mediaSelectionCancelled();
                    return;
                default:
                    // noop
                    return;
            }
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
                    mEditorMedia.advertiseImageOptimisationAndAddMedia(retrieveMediaUris(data));
                    break;
                case RequestCodes.TAKE_PHOTO:
                    if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
                        WPMediaUtils.advertiseImageOptimization(this, this::addLastTakenPicture);
                    } else {
                        addLastTakenPicture();
                    }
                    break;
                case RequestCodes.VIDEO_LIBRARY:
                    mEditorMedia.addNewMediaItemsToEditorAsync(retrieveMediaUris(data), false);
                    break;
                case RequestCodes.TAKE_VIDEO:
                    mEditorMedia.addFreshlyTakenVideoToEditor();
                    break;
                case RequestCodes.MEDIA_SETTINGS:
                    if (mEditorFragment instanceof AztecEditorFragment) {
                        mEditorFragment.onActivityResult(AztecEditorFragment.EDITOR_MEDIA_SETTINGS,
                                                         Activity.RESULT_OK, data);
                    }
                    break;
                case RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT:
                    if (data.hasExtra(StockMediaPickerActivity.KEY_UPLOADED_MEDIA_IDS)) {
                        long[] mediaIds = data.getLongArrayExtra(StockMediaPickerActivity.KEY_UPLOADED_MEDIA_IDS);
                        mEditorMedia
                                .addExistingMediaToEditorAsync(AddExistingMediaSource.STOCK_PHOTO_LIBRARY, mediaIds);
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

    private List<Uri> retrieveMediaUris(Intent data) {
        ClipData clipData = data.getClipData();
        ArrayList<Uri> uriList = new ArrayList<>();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                uriList.add(item.getUri());
            }
        } else {
            uriList.add(data.getData());
        }
        return uriList;
    }

    private void addLastTakenPicture() {
        try {
            // TODO why do we scan the file twice? Also how come it can result in OOM?
            WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
            File f = new File(mMediaCapturePath);
            Uri capturedImageUri = Uri.fromFile(f);
            if (capturedImageUri != null) {
                mEditorMedia.addNewMediaToEditorAsync(capturedImageUri, true);
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

    private void handleMediaPickerResult(Intent data) {
        // TODO move this to EditorMedia
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
            // if allowMultipleSelection and gutenberg editor, pass all ids to addExistingMediaToEditor at once
            mEditorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, ids);
            if (mShowGutenbergEditor && mEditorPhotoPicker.getAllowMultipleSelection()) {
                mEditorPhotoPicker.setAllowMultipleSelection(false);
            }
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
                    mEditorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, mediaIds);
                    break;
            }
        };
        InsertMediaDialog dialog = InsertMediaDialog.newInstance(callback, mSite);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, "insert_media");
        ft.commitAllowingStateLoss();
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

    @Override
    public void onEditPostPublishedSettingsClick() {
        mViewPager.setCurrentItem(PAGE_PUBLISH_SETTINGS);
    }

    /**
     * EditorFragmentListener methods
     */

    @Override
    public void onAddMediaClicked() {
        if (mEditorPhotoPicker.isPhotoPickerShowing()) {
            mEditorPhotoPicker.hidePhotoPicker();
         } else if (WPMediaUtils.currentUserCanUploadMedia(mSite)) {
            mEditorPhotoPicker.showPhotoPicker(mSite);
        } else {
            // show the WP media library instead of the photo picker if the user doesn't have upload permission
            ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.EDITOR_PICKER);
        }
    }

    @Override
    public void onAddMediaImageClicked(boolean allowMultipleSelection) {
        mEditorPhotoPicker.setAllowMultipleSelection(allowMultipleSelection);
        ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.GUTENBERG_IMAGE_PICKER);
    }

    @Override
    public void onAddMediaVideoClicked(boolean allowMultipleSelection) {
        mEditorPhotoPicker.setAllowMultipleSelection(allowMultipleSelection);
        ActivityLauncher.viewMediaPickerForResult(this, mSite, MediaBrowserType.GUTENBERG_VIDEO_PICKER);
    }

    @Override
    public void onAddLibraryMediaClicked(boolean allowMultipleSelection) {
        mEditorPhotoPicker.setAllowMultipleSelection(allowMultipleSelection);
        if (allowMultipleSelection) {
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
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO_OR_VIDEO, allowMultipleSelection);
    }

    @Override
    public void onAddStockMediaClicked(boolean allowMultipleSelection) {
        onPhotoPickerIconClicked(PhotoPickerIcon.STOCK_MEDIA, allowMultipleSelection);
    }

    @Override
    public void onPerformFetch(String path, Consumer<String> onResult, Consumer<Bundle> onError) {
        if (mSite != null) {
            mReactNativeRequestHandler.performGetRequest(path, mSite, onResult, onError);
        }
    }

    @Override
    public void onCaptureVideoClicked() {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_VIDEO, false);
    }

    @Override
    public void onMediaDropped(final ArrayList<Uri> mediaUris) {
        mEditorMedia.setDroppedMediaUris(mediaUris);
        if (PermissionUtils
                .checkAndRequestStoragePermission(this, WPPermissionUtils.EDITOR_DRAG_DROP_PERMISSION_REQUEST_CODE)) {
            mEditorMedia.addNewMediaItemsToEditorAsync(mEditorMedia.getDroppedMediaUris(), false);
            mEditorMedia.getDroppedMediaUris().clear();
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
        UploadService.cancelFinalNotification(this, mEditPostRepository.getPost());
        UploadService.cancelFinalNotificationForMedia(this, mSite);
        ArrayList<Integer> localMediaIds = new ArrayList<>();
        for (String idString : failedMediaIds) {
            localMediaIds.add(Integer.valueOf(idString));
        }
        mEditorMedia.retryFailedMediaAsync(localMediaIds);
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
            UploadService.cancelFinalNotification(this, mEditPostRepository.getPost());
            UploadService.cancelFinalNotificationForMedia(this, mSite);
            mEditorMedia.retryFailedMediaAsync(Collections.singletonList(media.getId()));
        }

        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED);
        return true;
    }

    @Override
    public void onMediaUploadCancelClicked(String localMediaId) {
        if (!TextUtils.isEmpty(localMediaId)) {
            mEditorMedia.cancelMediaUploadAsync(StringUtils.stringToInt(localMediaId), true);
        } else {
            // Passed mediaId is incorrect: cancel all uploads for this post
            ToastUtils.showToast(this, getString(R.string.error_all_media_upload_canceled));
            EventBus.getDefault().post(new PostEvents.PostMediaCanceled(mEditPostRepository.getEditablePost()));
        }
    }

    @Override
    public void onMediaDeleted(String localMediaId) {
        if (!TextUtils.isEmpty(localMediaId)) {
            mEditorMedia.onMediaDeleted(mShowAztecEditor, mShowGutenbergEditor, localMediaId);
        }
    }

    @Override
    public void onUndoMediaCheck(final String undoedContent) {
        // here we check which elements tagged UPLOADING are there in undoedContent,
        // and check for the ones that ARE NOT being uploaded or queued in the UploadService.
        // These are the CANCELED ONES, so mark them FAILED now to retry.

        List<MediaModel> currentlyUploadingMedia =
                UploadService.getPendingOrInProgressMediaUploadsForPost(mEditPostRepository.getPost());
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
                    mEditorMedia.updateDeletedMediaItemIds(mediaId);
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
                    mEditorMedia.refreshBlogMedia();
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
                mEditorMedia.addExistingMediaToEditorAsync(mediaList, AddExistingMediaSource.WP_MEDIA_LIBRARY);
            }
        }

        onEditorFinalTouchesBeforeShowing();
    }

    private void onEditorFinalTouchesBeforeShowing() {
        refreshEditorContent();
        // probably here is best for Gutenberg to start interacting with
        if (mShowGutenbergEditor && mEditorFragment instanceof GutenbergEditorFragment) {
            List<MediaModel> failedMedia =
                    mMediaStore.getMediaForPostWithState(mEditPostRepository.getPost(), MediaUploadState.FAILED);
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
        mEditorTracker.trackEditorEvent(event, mEditorFragment.getEditorName());
        switch (event) {
            case ELLIPSIS_COLLAPSE_BUTTON_TAPPED:
                AppPrefs.setAztecEditorToolbarExpanded(false);
                break;
            case ELLIPSIS_EXPAND_BUTTON_TAPPED:
                AppPrefs.setAztecEditorToolbarExpanded(true);
                break;
            case HTML_BUTTON_TAPPED:
            case LINK_ADDED_BUTTON_TAPPED:
                mEditorPhotoPicker.hidePhotoPicker();
                break;
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
            if (!mEditPostRepository.hasPost() || (mEditPostRepository.getId()
                                                   != ((RemoteAutoSavePost) event.causeOfChange).getLocalPostId())) {
                AppLog.e(T.POSTS,
                        "Ignoring REMOTE_AUTO_SAVE_POST in EditPostActivity as mPost is null or id of the opened post"
                        + " doesn't match the event.");
                return;
            }
            if (event.isError()) {
                AppLog.e(T.POSTS, "REMOTE_AUTO_SAVE_POST failed: " + event.error.type + " - " + event.error.message);
            }
            mEditPostRepository.loadPostByLocalPostId(mEditPostRepository.getId());
            mEditPostRepository.replace(postModel -> handleRemoteAutoSave(event.isError(), postModel));
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

    private void updateOnSuccessfulUpload() {
        mIsNewPost = false;
        invalidateOptionsMenu();
    }

    private boolean isRemoteAutoSaveError() {
        return mPostLoadingState == PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR;
    }

    @Nullable
    private PostModel handleRemoteAutoSave(boolean isError, PostModel post) {
        // We are in the process of remote previewing a post from the editor
        if (!isError && isUploadingPostForPreview()) {
            // We were uploading post for preview and we got no error:
            // update post status and preview it in the internal browser
            updateOnSuccessfulUpload();
            ActivityLauncher.previewPostOrPageForResult(
                    EditPostActivity.this,
                    mSite,
                    post,
                    mPostLoadingState == PostLoadingState.UPLOADING_FOR_PREVIEW
                            ? RemotePreviewLogicHelper.RemotePreviewType.REMOTE_PREVIEW
                            : RemotePreviewLogicHelper.RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
                                                       );
            updatePostLoadingAndDialogState(PostLoadingState.PREVIEWING, post);
        } else if (isError || isRemoteAutoSaveError()) {
            // We got an error from the uploading or from the remote auto save of a post: show snackbar error
            updatePostLoadingAndDialogState(PostLoadingState.NONE);
            mUploadUtilsWrapper.showSnackbarError(findViewById(R.id.editor_activity),
                    getString(R.string.remote_preview_operation_error));
        }
        return post;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        final PostModel post = event.post;
        if (post != null && post.getId() == mEditPostRepository.getId()) {
            if (!isRemotePreviewingFromEditor()) {
                // We are not remote previewing a post: show snackbar and update post status if needed
                View snackbarAttachView = findViewById(R.id.editor_activity);
                mUploadUtilsWrapper.onPostUploadedSnackbarHandler(this, snackbarAttachView, event.isError(), post,
                        event.isError() ? event.error.message : null, getSite());
                if (!event.isError()) {
                    mEditPostRepository.set(() -> {
                        updateOnSuccessfulUpload();
                        return post;
                    });
                }
            } else {
                mEditPostRepository.set(() -> handleRemoteAutoSave(event.isError(), post));
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
    public EditPostRepository getEditPostRepository() {
        return mEditPostRepository;
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

    // EditorMediaListener
    @Override
    public void appendMediaFiles(@NotNull Map<String, ? extends MediaFile> mediaFiles) {
        mEditorFragment.appendMediaFiles((Map<String, MediaFile>) mediaFiles);
    }

    @NotNull @Override
    public PostImmutableModel getImmutablePost() {
        return Objects.requireNonNull(mEditPostRepository.getPost());
    }

    @Override
    public void syncPostObjectWithUiAndSaveIt(@Nullable OnPostUpdatedFromUIListener listener) {
        updateAndSavePostAsync(listener);
    }

    @Override public void advertiseImageOptimization(@NotNull Function0<Unit> listener) {
        WPMediaUtils.advertiseImageOptimization(this, listener::invoke);
    }

    private void updateAddingMediaToEditorProgressDialogState(ProgressDialogUiState uiState) {
        mAddingMediaToEditorProgressDialog = mProgressDialogHelper
                .updateProgressDialogState(this, mAddingMediaToEditorProgressDialog, uiState, mUiHelpers);
    }
}
