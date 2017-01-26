package org.wordpress.android.ui.posts;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.SuggestionSpan;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.JavaScriptException;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.editor.EditorFragment.IllegalEditorStateException;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorDragAndDropListener;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorWebViewAbstract.ErrorListener;
import org.wordpress.android.editor.EditorWebViewCompatibility;
import org.wordpress.android.editor.EditorWebViewCompatibility.ReflectionException;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.editor.LegacyEditorFragment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.InstantiatePostPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostInstantiated;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.media.services.MediaEvents;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutolinkUtils;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.CrashlyticsUtils.ExceptionType;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPStoreUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.util.helpers.MediaGalleryImageSpan;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static com.android.volley.Request.Method.HEAD;
import static org.wordpress.android.R.string.post;

public class EditPostActivity extends AppCompatActivity implements EditorFragmentListener, EditorDragAndDropListener,
        ActivityCompat.OnRequestPermissionsResultCallback, EditorWebViewCompatibility.ReflectionFailureListener {
    public static final String EXTRA_POST = "postModel";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String STATE_KEY_CURRENT_POST = "stateKeyCurrentPost";
    public static final String STATE_KEY_ORIGINAL_POST = "stateKeyOriginalPost";
    public static final String STATE_KEY_EDITOR_FRAGMENT = "editorFragment";
    public static final String STATE_KEY_DROPPED_MEDIA_URIS = "stateKeyDroppedMediaUri";

    // Context menu positioning
    private static final int SELECT_PHOTO_MENU_POSITION = 0;
    private static final int CAPTURE_PHOTO_MENU_POSITION = 1;
    private static final int SELECT_VIDEO_MENU_POSITION = 2;
    private static final int CAPTURE_VIDEO_MENU_POSITION = 3;
    private static final int ADD_GALLERY_MENU_POSITION = 4;
    private static final int SELECT_LIBRARY_MENU_POSITION = 5;

    public static final int MEDIA_PERMISSION_REQUEST_CODE = 1;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    public static final int DRAG_AND_DROP_MEDIA_PERMISSION_REQUEST_CODE = 3;

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;

    private static final int AUTOSAVE_INTERVAL_MILLIS = 60000;

    private Handler mHandler;
    private boolean mShowNewEditor;

    // Each element is a list of media IDs being uploaded to a gallery, keyed by gallery ID
    private Map<Long, List<Long>> mPendingGalleryUploads = new HashMap<>();

    private List<String> mPendingVideoPressInfoRequests;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    WPViewPager mViewPager;

    private PostModel mPost;
    private PostModel mOriginalPost;

    private EditorFragmentAbstract mEditorFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditPostPreviewFragment mEditPostPreviewFragment;

    private EditorMediaUploadListener mEditorMediaUploadListener;

    private boolean mIsNewPost;
    private boolean mIsPage;
    private boolean mHasSetPostContent;
    private CountDownLatch mNewPostLatch;

    // For opening the context menu after permissions have been granted
    private View mMenuView = null;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject MediaStore mMediaStore;

    private SiteModel mSite;

    // for keeping the media uri while asking for permissions
    private ArrayList<Uri> mDroppedMediaUris;

    private Runnable mFetchMediaRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDroppedMediaUris != null) {
                final List<Uri> mediaUris = mDroppedMediaUris;
                mDroppedMediaUris = null;

                fetchMedia(mediaUris);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        setContentView(R.layout.new_edit_post_activity);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        // Check whether to show the visual editor
        PreferenceManager.setDefaultValues(this, R.xml.account_settings, false);
        mShowNewEditor = AppPrefs.isVisualEditorEnabled();

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getFragmentManager();
        Bundle extras = getIntent().getExtras();
        String action = getIntent().getAction();
        if (savedInstanceState == null) {
            if (!getIntent().hasExtra(EXTRA_POST)
                    ||Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)
                    || NEW_MEDIA_GALLERY.equals(action)
                    || NEW_MEDIA_POST.equals(action)
                    || getIntent().hasExtra(EXTRA_IS_QUICKPRESS)
                    || (extras != null && extras.getInt("quick-media", -1) > -1)) {
                if (getIntent().hasExtra(EXTRA_QUICKPRESS_BLOG_ID)) {
                    // QuickPress might want to use a different blog than the current blog
                    int localSiteId = getIntent().getIntExtra(EXTRA_QUICKPRESS_BLOG_ID, -1);
                    SiteModel site = mSiteStore.getSiteByLocalId(localSiteId);
                    if (site == null) {
                        showErrorAndFinish(R.string.blog_not_found);
                        return;
                    }
                    if (!site.isVisible()) {
                        showErrorAndFinish(R.string.error_blog_hidden);
                        return;
                    }
                    mSite = site;
                }

                mIsPage = extras.getBoolean(EXTRA_IS_PAGE);
                mIsNewPost = true;
                mNewPostLatch = new CountDownLatch(1);

                // Create a new post
                List<Long> categories = new ArrayList<>();
                // TODO: Use TaxonomyStore in SiteSettingsInterface and get default category remote id
                String postFormat = SiteSettingsInterface.getDefaultFormat(WordPress.getContext());
                InstantiatePostPayload payload = new InstantiatePostPayload(mSite, mIsPage, categories, postFormat);
                mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(payload));

                // Wait for the OnPostInstantiated event to initialize the post
                try {
                    mNewPostLatch.await(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mPost == null) {
                    throw new RuntimeException("No callback received from INSTANTIATE_POST action");
                }
            } else if (extras != null) {
                // Load post passed in extras
                mPost = (PostModel) extras.getSerializable(EXTRA_POST);
                if (mPost != null) {
                    mOriginalPost = mPost.clone();
                    mIsPage = mPost.isPage();
                }
            } else {
                // A postId extra must be passed to this activity
                showErrorAndFinish(R.string.post_not_found);
                return;
            }
        } else {
            mDroppedMediaUris = savedInstanceState.getParcelable(STATE_KEY_DROPPED_MEDIA_URIS);

            if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_POST)) {
                try {
                    mPost = (PostModel) savedInstanceState.getSerializable(STATE_KEY_CURRENT_POST);
                    mOriginalPost = (PostModel) savedInstanceState.getSerializable(STATE_KEY_ORIGINAL_POST);
                } catch (ClassCastException e) {
                    mPost = null;
                }
            }
            mEditorFragment = (EditorFragmentAbstract) fragmentManager.getFragment(savedInstanceState, STATE_KEY_EDITOR_FRAGMENT);

            if (mEditorFragment instanceof EditorMediaUploadListener) {
                mEditorMediaUploadListener = (EditorMediaUploadListener) mEditorFragment;
            }
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        if (mHasSetPostContent = mEditorFragment != null) {
            mEditorFragment.setImageLoader(WordPress.imageLoader);
        }

        // Ensure we have a valid post
        if (mPost == null) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        if (mIsNewPost) {
            trackEditorCreatedPost(action, getIntent());
        }

        setTitle(StringUtils.unescapeHTML(SiteUtils.getSiteNameOrHomeURL(mSite)));
        mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (WPViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPagingEnabled(false);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
                if (position == PAGE_CONTENT) {
                    setTitle(StringUtils.unescapeHTML(SiteUtils.getSiteNameOrHomeURL(mSite)));
                } else if (position == PAGE_SETTINGS) {
                    setTitle(mPost.isPage() ? R.string.page_settings : R.string.post_settings);
                } else if (position == PAGE_PREVIEW) {
                    setTitle(mPost.isPage() ? R.string.preview_page : R.string.preview_post);
                    savePostAsync(new AfterSavePostListener() {
                        @Override
                        public void onPostSave() {
                            if (mEditPostPreviewFragment != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mEditPostPreviewFragment != null) {
                                            mEditPostPreviewFragment.loadPost();
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        ActivityId.trackLastActivity(ActivityId.POST_EDITOR);
    }

    private Runnable mAutoSave = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        updatePostObject(true);
                    } catch (IllegalEditorStateException e) {
                        AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
                        return;
                    }
                    savePostToDb();
                    if (mHandler != null) {
                        mHandler.postDelayed(mAutoSave, AUTOSAVE_INTERVAL_MILLIS);
                    }
                }
            }).start();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mHandler = new Handler();
        mHandler.postDelayed(mAutoSave, AUTOSAVE_INTERVAL_MILLIS);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mGalleryReceiver);
        } catch (IllegalArgumentException e) {
            AppLog.d(T.EDITOR, "Illegal state! Can't unregister receiver that was no registered");
        }

        stopMediaUploadService();
        mHandler.removeCallbacks(mAutoSave);
        mHandler = null;
    }

    @Override
    protected void onDestroy() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_CLOSED);
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves both post objects so we can restore them in onCreate()
        savePostAsync(null);
        outState.putSerializable(STATE_KEY_CURRENT_POST, mPost);
        outState.putSerializable(STATE_KEY_ORIGINAL_POST, mOriginalPost);
        outState.putSerializable(WordPress.SITE, mSite);

        outState.putParcelableArrayList(STATE_KEY_DROPPED_MEDIA_URIS, mDroppedMediaUris);

        if (mEditorFragment != null) {
            getFragmentManager().putFragment(outState, STATE_KEY_EDITOR_FRAGMENT, mEditorFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (mShowNewEditor) {
            inflater.inflate(R.menu.edit_post, menu);
        } else {
            inflater.inflate(R.menu.edit_post_legacy, menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showMenuItems = true;
        if (mViewPager != null && mViewPager.getCurrentItem() > PAGE_CONTENT) {
            showMenuItems = false;
        }

        MenuItem previewMenuItem = menu.findItem(R.id.menu_preview_post);
        MenuItem settingsMenuItem = menu.findItem(R.id.menu_post_settings);

        if (previewMenuItem != null) {
            previewMenuItem.setVisible(showMenuItems);
        }

        if (settingsMenuItem != null) {
            settingsMenuItem.setVisible(showMenuItems);
        }

        // Set text of the save button in the ActionBar
        if (mPost != null) {
            MenuItem saveMenuItem = menu.findItem(R.id.menu_save_post);
            if (saveMenuItem != null) {
                switch (PostStatus.fromPost(mPost)) {
                    case SCHEDULED:
                        saveMenuItem.setTitle(getString(R.string.schedule_verb));
                        break;
                    case PUBLISHED:
                    case UNKNOWN:
                        if (mPost.isLocalDraft()) {
                            saveMenuItem.setTitle(R.string.publish_post);
                        } else {
                            saveMenuItem.setTitle(R.string.update_verb);
                        }
                        break;
                    default:
                        if (mPost.isLocalDraft()) {
                            saveMenuItem.setTitle(R.string.save);
                        } else {
                            saveMenuItem.setTitle(R.string.update_verb);
                        }
                }
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                boolean shouldShowLocation = false;
                // Check if at least one of the location permission (coarse or fine) is granted
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        shouldShowLocation = true;
                    }
                }
                if (shouldShowLocation) {
                    // Permission request was granted, show Location buttons in Settings
                    mEditPostSettingsFragment.showLocationSearch();

                    // After permission request was granted add GeoTag to the new post (if GeoTagging is enabled)
                    if (SiteSettingsInterface.getGeotagging(this) && isNewPost()) {
                        mEditPostSettingsFragment.searchLocation();
                    }

                    return;
                }
                // Location permission denied
                ToastUtils.showToast(this, getString(R.string.add_location_permission_required));
                break;
            case MEDIA_PERMISSION_REQUEST_CODE:
                boolean shouldShowContextMenu = true;
                for (int i = 0; i < grantResults.length; ++i) {
                    switch (permissions[i]) {
                        case Manifest.permission.CAMERA:
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                shouldShowContextMenu = false;
                            }
                            break;
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                shouldShowContextMenu = false;
                            } else {
                                registerReceiver(mGalleryReceiver,
                                        new IntentFilter(LegacyEditorFragment.ACTION_MEDIA_GALLERY_TOUCHED));
                                refreshBlogMedia();
                            }
                            break;
                    }
                }
                if (shouldShowContextMenu) {
                    if (mMenuView != null) {
                        super.openContextMenu(mMenuView);
                        mMenuView = null;
                    }
                } else {
                    ToastUtils.showToast(this, getString(R.string.access_media_permission_required));
                }
                break;
            case DRAG_AND_DROP_MEDIA_PERMISSION_REQUEST_CODE:
                boolean mediaAccessGranted = false;
                for (int i = 0; i < grantResults.length; ++i) {
                    switch (permissions[i]) {
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                mediaAccessGranted = true;
                            }
                            break;
                    }
                }
                if (mediaAccessGranted) {
                    runOnUiThread(mFetchMediaRunnable);
                } else {
                    ToastUtils.showToast(this, getString(R.string.access_media_permission_required));
                }
            default:
                break;
        }
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            Fragment fragment = getFragmentManager().findFragmentByTag(
                    ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
            if (fragment != null && fragment.isVisible()) {
                return false;
            }
            if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
                if (mViewPager.getCurrentItem() == PAGE_SETTINGS) {
                    mPost.setFeaturedImageId(mEditPostSettingsFragment.getFeaturedImageId());
                    mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
                }
                mViewPager.setCurrentItem(PAGE_CONTENT);
                invalidateOptionsMenu();
            } else {
                saveAndFinish();
            }
            return true;
        }

        // Disable format bar buttons while a media upload is in progress
        if (!mMediaStore.getLocalSiteMedia(mSite).isEmpty() || mEditorFragment.isUploadingMedia() ||
                mEditorFragment.isActionInProgress()) {
            ToastUtils.showToast(this, R.string.editor_toast_uploading_please_wait, Duration.SHORT);
            return false;
        }

        if (itemId == R.id.menu_save_post) {
            return publishPost();
        } else if (itemId == R.id.menu_preview_post) {
            mViewPager.setCurrentItem(PAGE_PREVIEW);
        } else if (itemId == R.id.menu_post_settings) {
            InputMethodManager imm = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            if (mShowNewEditor) {
                mEditPostSettingsFragment.updateFeaturedImage(mPost.getFeaturedImageId());
            }
            mViewPager.setCurrentItem(PAGE_SETTINGS);
        }
        return false;
    }

    private boolean publishPost() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, R.string.error_publish_no_network, Duration.SHORT);
            return false;
        }

        // Show an Alert Dialog asking the user if he wants to remove all failed media before upload
        if (mEditorFragment.hasFailedMediaUploads()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.editor_toast_failed_uploads)
                    .setPositiveButton(R.string.editor_remove_failed_uploads, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Clear failed uploads
                            mEditorFragment.removeAllFailedMediaUploads();
                        }
                    }).setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
            return true;
        }

        // Update post, save to db and publish in its own Thread, because 1. update can be pretty slow with a lot of
        // text 2. better not to call `updatePostObject()` from the UI thread due to weird thread blocking behavior
        // on API 16 with the visual editor.
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isFirstTimePublish = false;
                if (PostStatus.fromPost(mPost) == PostStatus.PUBLISHED &&
                        (mPost.isLocalDraft() || PostStatus.fromPost(mOriginalPost) == PostStatus.DRAFT)) {
                    isFirstTimePublish = true;
                }
                try {
                    updatePostObject(false);
                } catch (IllegalEditorStateException e) {
                    AppLog.e(T.EDITOR, "Impossible to save and publish the post, we weren't able to update it.");
                    return;
                }

                savePostToDb();

                // If the post is empty, don't publish
                if (!PostUtils.isPublishable(mPost)) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showToast(EditPostActivity.this, R.string.error_publish_empty_post, Duration.SHORT);
                        }
                    });
                    return;
                }

                PostUtils.trackSavePostAnalytics(mPost, mSiteStore.getSiteByLocalId(mPost.getLocalSiteId()));

                if (isFirstTimePublish) {
                    PostUploadService.addPostToUploadAndTrackAnalytics(mPost);
                } else {
                    PostUploadService.addPostToUpload(mPost);
                }
                PostUploadService.setLegacyMode(!mShowNewEditor);
                startService(new Intent(EditPostActivity.this, PostUploadService.class));
                setResult(RESULT_OK);
                finish();
            }
        }).start();
        return true;
    }

    @Override
    public void openContextMenu(View view) {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, MEDIA_PERMISSION_REQUEST_CODE)) {
            super.openContextMenu(view);
        } else {
            AppLockManager.getInstance().setExtendedTimeout();
            mMenuView = view;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, SELECT_PHOTO_MENU_POSITION, 0, getResources().getText(R.string.select_photo));
        if (DeviceUtils.getInstance().hasCamera(this)) {
            menu.add(0, CAPTURE_PHOTO_MENU_POSITION, 0, getResources().getText(R.string.media_add_popup_capture_photo));
        }
        menu.add(0, SELECT_VIDEO_MENU_POSITION, 0, getResources().getText(R.string.select_video));
        if (DeviceUtils.getInstance().hasCamera(this)) {
            menu.add(0, CAPTURE_VIDEO_MENU_POSITION, 0, getResources().getText(R.string.media_add_popup_capture_video));
        }

        menu.add(0, ADD_GALLERY_MENU_POSITION, 0, getResources().getText(R.string.media_add_new_media_gallery));
        menu.add(0, SELECT_LIBRARY_MENU_POSITION, 0, getResources().getText(R.string.select_from_media_library));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SELECT_PHOTO_MENU_POSITION:
                launchPictureLibrary();
                return true;
            case CAPTURE_PHOTO_MENU_POSITION:
                launchCamera();
                return true;
            case SELECT_VIDEO_MENU_POSITION:
                launchVideoLibrary();
                return true;
            case CAPTURE_VIDEO_MENU_POSITION:
                launchVideoCamera();
                return true;
            case ADD_GALLERY_MENU_POSITION:
                startMediaGalleryActivity(null);
                return true;
            case SELECT_LIBRARY_MENU_POSITION:
                startMediaGalleryAddActivity();
                return true;
            default:
                return false;
        }
    }

    private void launchPictureLibrary() {
        WordPressMediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoLibrary() {
        WordPressMediaUtils.launchVideoLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoCamera() {
        WordPressMediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void showErrorAndFinish(int errorMessageId) {
        Toast.makeText(this, getResources().getText(errorMessageId), Toast.LENGTH_LONG).show();
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
        if (EditPostActivity.NEW_MEDIA_GALLERY.equals(action) || EditPostActivity.NEW_MEDIA_POST.equals(
                action)) {
            // Post created from the media library
            normalizedSourceName = "media-library";
        }
        if (intent != null && intent.hasExtra(EXTRA_IS_QUICKPRESS)) {
            // Quick press
            normalizedSourceName = "quick-press";
        }
        if (intent != null && intent.getIntExtra("quick-media", -1) > -1) {
            // Quick photo or quick video
            normalizedSourceName = "quick-media";
        }
        properties.put("created_post_source", normalizedSourceName);
        AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.EDITOR_CREATED_POST,
                mSiteStore.getSiteByLocalId(mPost.getLocalSiteId()),
                properties
        );
    }

    private synchronized void updatePostObject(boolean isAutosave) throws IllegalEditorStateException {
        if (mPost == null) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.");
            return;
        }

        // Update post object from fragment fields
        if (mEditorFragment != null) {
            if (mShowNewEditor) {
                updatePostContentNewEditor(isAutosave, (String) mEditorFragment.getTitle(),
                        (String) mEditorFragment.getContent());
            } else {
                // TODO: Remove when legacy editor is dropped
                updatePostContent(isAutosave);
            }
        }

        if (mEditPostSettingsFragment != null) {
            mEditPostSettingsFragment.updatePostSettings();
        }

        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis()));
    }

    private void savePostAsync(final AfterSavePostListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updatePostObject(false);
                } catch (IllegalEditorStateException e) {
                    AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
                    return;
                }
                savePostToDb();
                if (listener != null) {
                    listener.onPostSave();
                }
            }
        }).start();
    }

    private interface AfterSavePostListener {
        void onPostSave();
    }

    private synchronized void savePostToDb() {
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));
    }

    @Override
    public void onBackPressed() {
        Fragment imageSettingsFragment = getFragmentManager().findFragmentByTag(
                ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
        if (imageSettingsFragment != null && imageSettingsFragment.isVisible()) {
            ((ImageSettingsDialogFragment) imageSettingsFragment).dismissFragment();
            return;
        }

        if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
            if (mViewPager.getCurrentItem() == PAGE_SETTINGS) {
                mPost.setFeaturedImageId(mEditPostSettingsFragment.getFeaturedImageId());
                mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
            }
            mViewPager.setCurrentItem(PAGE_CONTENT);
            invalidateOptionsMenu();
            return;
        }

        if (mEditorFragment != null && !mEditorFragment.onBackPressed()) {
            saveAndFinish();
        }
    }

    public boolean isNewPost() {
        return mIsNewPost;
    }

    private class SaveAndFinishTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            // Fetch post title and content from editor fields and update the Post object
            try {
                updatePostObject(false);
            } catch (IllegalEditorStateException e) {
                AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
                return false;
            }

            if (mEditorFragment != null && PostUtils.hasEmptyContentFields(mPost)) {
                // New and empty post? delete it
                if (mIsNewPost) {
                    mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(mPost));
                    return false;
                }
            } else if (mOriginalPost != null && !PostUtils.postHasEdits(mOriginalPost, mPost)) {
                // If no changes have been made to the post, set it back to the original - don't save it
                mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mOriginalPost));
                return false;
            } else {
                // Changes have been made - save the post and ask for the post list to refresh
                // We consider this being "manual save", it will replace some Android "spans" by an html
                // or a shortcode replacement (for instance for images and galleries)
                if (mShowNewEditor) {
                    // Update the post object directly, without re-fetching the fields from the EditorFragment
                    updatePostContentNewEditor(false, mPost.getTitle(), mPost.getContent());
                    savePostToDb();
                } else {
                    try {
                        updatePostObject(false);
                    } catch (IllegalEditorStateException e) {
                        AppLog.e(T.EDITOR, "Impossible to save the post, we weren't able to update it.");
                        return false;
                    }
                    savePostToDb();
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean saved) {
            if (saved) {
                Intent i = new Intent();
                i.putExtra(EXTRA_SAVED_AS_LOCAL_DRAFT, true);
                i.putExtra(EXTRA_IS_PAGE, mIsPage);
                setResult(RESULT_OK, i);
                ToastUtils.showToast(EditPostActivity.this, R.string.editor_toast_changes_saved);
            }
            finish();
        }
    }

    private void saveAndFinish() {
        new SaveAndFinishTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Disable visual editor mode and log the exception if we get a Reflection failure when the webview is being
     * initialized.
     */
    @Override
    public void onReflectionFailure(ReflectionException e) {
        CrashlyticsUtils.logException(e, ExceptionType.SPECIFIC, T.EDITOR, "Reflection Failure on Visual Editor init");
        // Disable visual editor and show an error message
        AppPrefs.setVisualEditorEnabled(false);
        ToastUtils.showToast(this, R.string.new_editor_reflection_error, Duration.LONG);
        // Restart the activity (will start the legacy editor)
        finish();
        startActivity(getIntent());
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        // Show two pages for the visual editor, and add a third page for the EditPostPreviewFragment for legacy
        private static final int NUM_PAGES_VISUAL_EDITOR = 2;
        private static final int NUM_PAGES_LEGACY_EDITOR = 3;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    // TODO: switch between legacy and new editor here (AB test?)
                    if (mShowNewEditor) {
                        EditorWebViewCompatibility.setReflectionFailureListener(EditPostActivity.this);
                        return new EditorFragment();
                    } else {
                        return new LegacyEditorFragment();
                    }
                case 1:
                    return EditPostSettingsFragment.newInstance(mSite, mPost);
                default:
                    return EditPostPreviewFragment.newInstance(mSite, mPost);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mEditorFragment = (EditorFragmentAbstract) fragment;
                    if (mEditorFragment instanceof EditorMediaUploadListener) {
                        mEditorMediaUploadListener = (EditorMediaUploadListener) mEditorFragment;

                        // Set up custom headers for the visual editor's internal WebView
                        mEditorFragment.setCustomHttpHeader("User-Agent", WordPress.getUserAgent());
                    }
                    break;
                case 1:
                    mEditPostSettingsFragment = (EditPostSettingsFragment) fragment;
                    break;
                case 2:
                    mEditPostPreviewFragment = (EditPostPreviewFragment) fragment;
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return (mShowNewEditor ? NUM_PAGES_VISUAL_EDITOR : NUM_PAGES_LEGACY_EDITOR);
        }
    }

    // Moved from EditPostContentFragment
    public static final String NEW_MEDIA_GALLERY = "NEW_MEDIA_GALLERY";
    public static final String NEW_MEDIA_GALLERY_EXTRA_IDS = "NEW_MEDIA_GALLERY_EXTRA_IDS";
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA = "NEW_MEDIA_POST_ID";
    private String mMediaCapturePath = "";
    private int mMaxThumbWidth = 0;

    private int getMaximumThumbnailWidthForEditor() {
        if (mMaxThumbWidth == 0) {
            mMaxThumbWidth = ImageUtils.getMaximumThumbnailWidthForEditor(this);
        }
        return mMaxThumbWidth;
    }

    private void addExistingMediaToEditor(long mediaId) {
        MediaModel media = mMediaStore.getSiteMediaWithId(mSite, mediaId);
        if (media != null) {
            trackAddMediaEvents(media.isVideo(), true);
            // TODO: change signature of appendMediaFile to use MediaModel
        }
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
        String replacement;
        if (Build.VERSION.SDK_INT >= 19) {
            replacement = String.format(Locale.US,
                    "<span id=\"img_container_%s\" class=\"img_container failed\" data-failed=\"%s\"><progress " +
                            "id=\"progress_%s\" value=\"0\" class=\"wp_media_indicator failed\" contenteditable=\"false\">" +
                            "</progress><img data-wpid=\"%s\" src=\"%s\" alt=\"\" class=\"failed\"></span>",
                    mediaId, getString(R.string.tap_to_try_again), mediaId, mediaId, path);
        } else {
            // Before API 19, the WebView didn't support progress tags. Use an upload overlay instead of a progress bar
            replacement = String.format(Locale.US,
                    "<span id=\"img_container_%s\" class=\"img_container compat failed\" contenteditable=\"false\" " +
                            "data-failed=\"%s\"><span class=\"upload-overlay failed\" " +
                            "contenteditable=\"false\">Uploading…</span><span class=\"upload-overlay-bg\"></span>" +
                            "<img data-wpid=\"%s\" src=\"%s\" alt=\"\" class=\"failed\"></span>",
                    mediaId, getString(R.string.tap_to_try_again), mediaId, path);
        }
        return replacement;
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
                String path = null;
                String stringUri = matcher.group(1);
                Uri uri = Uri.parse(stringUri);
                if (uri != null && stringUri.contains("content:")) {
                    path = getPathFromContentUri(uri);
                    if (path == null) {
                        continue;
                    }
                } else {
                    path = stringUri.replace("file://", "");
                }
                MediaFile mediaFile = queueFileForUpload(path, null, "failed");
                if (mediaFile == null) {
                    continue;
                }
                String replacement = getUploadErrorHtml(mediaFile.getMediaId(), mediaFile.getFilePath());
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
        mEditorFragment.setTitlePlaceholder(getString(mIsPage ? R.string.editor_page_title_placeholder :
                R.string.editor_post_title_placeholder));

        // Set post title and content
        if (mPost != null) {
            if (!TextUtils.isEmpty(mPost.getContent()) && !mHasSetPostContent) {
                mHasSetPostContent = true;
                if (mPost.isLocalDraft() && !mShowNewEditor) {
                    // TODO: Unnecessary for new editor, as all images are uploaded right away, even for local drafts
                    // Load local post content in the background, as it may take time to generate images
                    new LoadPostContentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            mPost.getContent().replaceAll("\uFFFC", ""));
                } else {
                    // TODO: Might be able to drop .replaceAll() when legacy editor is removed
                    String content = mPost.getContent().replaceAll("\uFFFC", "");
                    // Prepare eventual legacy editor local draft for the new editor
                    content = migrateLegacyDraft(content);
                    mEditorFragment.setContent(content);
                }
            }
            if (!TextUtils.isEmpty(mPost.getTitle())) {
                mEditorFragment.setTitle(mPost.getTitle());
            }
            // TODO: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setLocalDraft(mPost.isLocalDraft());

            mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
        }

        // Special actions
        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            setPostContentFromShareAction();
        } else if (NEW_MEDIA_GALLERY.equals(action)) {
            prepareMediaGallery();
        } else if (NEW_MEDIA_POST.equals(action)) {
            prepareMediaPost();
        }
    }

    private void launchCamera() {
        WordPressMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                new WordPressMediaUtils.LaunchCameraCallback() {
                    @Override
                    public void onMediaCapturePathReady(String mediaCapturePath) {
                        mMediaCapturePath = mediaCapturePath;
                        AppLockManager.getInstance().setExtendedTimeout();
                    }
                });
    }

    protected void setPostContentFromShareAction() {
        Intent intent = getIntent();

        // Check for shared text
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (text != null) {
            if (title != null) {
                mEditorFragment.setTitle(title);
            }
            // Create an <a href> element around links
            text = AutolinkUtils.autoCreateLinks(text);
            if (mEditorFragment instanceof EditorFragment) {
                mEditorFragment.setContent(text);
            } else {
                mEditorFragment.setContent(WPHtml.fromHtml(StringUtils.addPTags(text), this, mPost,
                        getMaximumThumbnailWidthForEditor()));
            }
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
                    sharedUris = new ArrayList<Uri>();
                    sharedUris.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
                } else {
                    return;
                }
            }

            if (sharedUris != null) {
                for (Uri uri : sharedUris) {
                    addMedia(uri);
                }
            }
        }
    }

    private void startMediaGalleryActivity(MediaGallery mediaGallery) {
        ActivityLauncher.viewMediaGalleryForSiteAndGallery(this, mSite, mediaGallery);
    }

    private void prepareMediaGallery() {
        MediaGallery mediaGallery = new MediaGallery();
        long[] idsArray = getIntent().getLongArrayExtra(NEW_MEDIA_GALLERY_EXTRA_IDS);
        ArrayList<Long> idsList = ListUtils.fromLongArray(idsArray);
        mediaGallery.setIds(idsList);
        startMediaGalleryActivity(mediaGallery);
    }

    private void prepareMediaPost() {
        long mediaId = getIntent().getLongExtra(NEW_MEDIA_POST_EXTRA, 0);
        addExistingMediaToEditor(mediaId);
    }

    // TODO: Replace with contents of the updatePostContentNewEditor() method when legacy editor is dropped
    /**
     * Updates post object with content of this fragment
     */
    public void updatePostContent(boolean isAutoSave) throws IllegalEditorStateException {
        if (mPost == null) {
            return;
        }
        String title = StringUtils.notNullStr((String) mEditorFragment.getTitle());
        SpannableStringBuilder postContent;
        if (mEditorFragment.getSpannedContent() != null) {
            // needed by the legacy editor to save local drafts
            try {
                postContent = new SpannableStringBuilder(mEditorFragment.getSpannedContent());
            } catch (IndexOutOfBoundsException e) {
                // A core android bug might cause an out of bounds exception, if so we'll just use the current editable
                // See https://code.google.com/p/android/issues/detail?id=5164
                postContent = new SpannableStringBuilder(StringUtils.notNullStr((String) mEditorFragment.getContent()));
            }
        } else {
            postContent = new SpannableStringBuilder(StringUtils.notNullStr((String) mEditorFragment.getContent()));
        }

        String content;
        if (mPost.isLocalDraft()) {
            // remove suggestion spans, they cause craziness in WPHtml.toHTML().
            CharacterStyle[] characterStyles = postContent.getSpans(0, postContent.length(), CharacterStyle.class);
            for (CharacterStyle characterStyle : characterStyles) {
                if (characterStyle instanceof SuggestionSpan) {
                    postContent.removeSpan(characterStyle);
                }
            }
            content = WPHtml.toHtml(postContent);
            // replace duplicate <p> tags so there's not duplicates, trac #86
            content = content.replace("<p><p>", "<p>");
            content = content.replace("</p></p>", "</p>");
            content = content.replace("<br><br>", "<br>");
            // sometimes the editor creates extra tags
            content = content.replace("</strong><strong>", "").replace("</em><em>", "").replace("</u><u>", "")
                    .replace("</strike><strike>", "").replace("</blockquote><blockquote>", "");
        } else {
            if (!isAutoSave) {
                // Add gallery shortcode
                MediaGalleryImageSpan[] gallerySpans = postContent.getSpans(0, postContent.length(),
                        MediaGalleryImageSpan.class);
                for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
                    int start = postContent.getSpanStart(gallerySpan);
                    postContent.removeSpan(gallerySpan);
                    postContent.insert(start, WPHtml.getGalleryShortcode(gallerySpan));
                }
            }

            WPImageSpan[] imageSpans = postContent.getSpans(0, postContent.length(), WPImageSpan.class);
            if (imageSpans.length != 0) {
                for (WPImageSpan wpIS : imageSpans) {
                    MediaFile mediaFile = wpIS.getMediaFile();
                    if (mediaFile == null)
                        continue;
                    if (mediaFile.getMediaId() != null) {
                        updateMediaFileOnServer(mediaFile);
                    } else {
                        mediaFile.setFileName(wpIS.getImageSource().toString());
                        mediaFile.setFilePath(wpIS.getImageSource().toString());
                        // TODO: saveMediaFile
                    }

                    int tagStart = postContent.getSpanStart(wpIS);
                    if (!isAutoSave) {
                        postContent.removeSpan(wpIS);

                        // network image has a mediaId
                        if (mediaFile.getMediaId() != null && mediaFile.getMediaId().length() > 0) {
                            postContent.insert(tagStart, WPHtml.getContent(wpIS));
                        } else {
                            // local image for upload
                            postContent.insert(tagStart,
                                    "<img android-uri=\"" + wpIS.getImageSource().toString() + "\" />");
                        }
                    }
                }
            }
            content = postContent.toString();
        }

        mPost.setTitle(title);
        mPost.setContent(content);

        if (!mPost.isLocalDraft()) {
            mPost.setIsLocallyChanged(true);
        }
    }

    /**
     * Updates post object with given title and content
     */
    public void updatePostContentNewEditor(boolean isAutoSave, String title, String content) {
        if (mPost == null) {
            return;
        }

        if (!isAutoSave) {
            // TODO: Shortcode handling, media handling
        }

        mPost.setTitle(title);
        mPost.setContent(content);

        if (!mPost.isLocalDraft()) {
            mPost.setIsLocallyChanged(true);
        }

        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis()));
    }

    /**
     * Media
     */

    private void fetchMedia(List<Uri> mediaUris) {
        for (Uri mediaUri : mediaUris) {
            if (mediaUri == null) {
                Toast.makeText(EditPostActivity.this, getString(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                continue;
            }

            if (!addMedia(mediaUri)) {
                Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateMediaFileOnServer(MediaFile mediaFile) {
        if (mediaFile == null) {
            return;
        }

        MediaPayload payload = new MediaPayload(mSite, WPStoreUtils.fromMediaFile(mediaFile));
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(payload));
    }

    private void trackAddMediaEvents(boolean isVideo, boolean fromMediaLibrary) {
        if (isVideo) {
            AnalyticsTracker.track(fromMediaLibrary ? Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY
                    : Stat.EDITOR_ADDED_VIDEO_VIA_LOCAL_LIBRARY);
        } else {
            AnalyticsTracker.track(fromMediaLibrary ? Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY
                    : Stat.EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY);
        }
    }

    private boolean addMedia(Uri mediaUri) {
        if (mediaUri != null && !MediaUtils.isInMediaStore(mediaUri) && !mediaUri.toString().startsWith("/")) {
            mediaUri = MediaUtils.downloadExternalMedia(this, mediaUri);
        }

        if (mediaUri == null) {
            return false;
        }

        boolean isVideo = MediaUtils.isVideo(mediaUri.toString());
        trackAddMediaEvents(isVideo, false);

        if (mShowNewEditor) {
            // TODO: add video param
            return addMediaVisualEditor(mediaUri);
        } else {
            return addMediaLegacyEditor(mediaUri, isVideo);
        }
    }

    private String getPathFromContentUri(Uri imageUri) {
        String path = null;
        String[] projection = new String[]{android.provider.MediaStore.Images.Media.DATA};
        Cursor cur = getContentResolver().query(imageUri, projection, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            int dataColumn = cur.getColumnIndex(android.provider.MediaStore.Images.Media.DATA);
            path = cur.getString(dataColumn);
        }
        SqlUtils.closeCursor(cur);
        return path;
    }

    private boolean addMediaVisualEditor(Uri imageUri) {
        String path = "";
        if (imageUri.toString().contains("content:")) {
            path = getPathFromContentUri(imageUri);
        } else {
            // File is not in media library
            path = imageUri.toString().replace("file://", "");
        }

        if (path == null) {
            ToastUtils.showToast(this, R.string.file_not_found, Duration.SHORT);
            return false;
        }

        MediaFile mediaFile = queueFileForUpload(path, new ArrayList<Long>());
        if (mediaFile != null) {
            mEditorFragment.appendMediaFile(mediaFile, path, WordPress.imageLoader);
        }

        return true;
    }

    private boolean addMediaLegacyEditor(Uri mediaUri, boolean isVideo) {
        String mediaTitle;
        if (isVideo) {
            mediaTitle = getResources().getString(R.string.video);
        } else {
            mediaTitle = ImageUtils.getTitleForWPImageSpan(this, mediaUri.getEncodedPath());
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setPostID(mPost.getId());
        mediaFile.setTitle(mediaTitle);
        mediaFile.setFilePath(mediaUri.toString());
        if (mediaUri.getEncodedPath() != null) {
            mediaFile.setVideo(isVideo);
        }
        mEditorFragment.appendMediaFile(mediaFile, mediaFile.getFilePath(), WordPress.imageLoader);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO ||
                requestCode == RequestCodes.TAKE_VIDEO))) {
            switch (requestCode) {
                case MediaGalleryActivity.REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        handleMediaGalleryResult(data);
                    }
                    break;
                case MediaGalleryPickerActivity.REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        handleMediaGalleryPickerResult(data);
                    }
                    break;
                case RequestCodes.PICTURE_LIBRARY:
                    Uri imageUri = data.getData();
                    fetchMedia(Arrays.asList(imageUri));
                    break;
                case RequestCodes.TAKE_PHOTO:
                    if (resultCode == Activity.RESULT_OK) {
                        try {
                            File f = new File(mMediaCapturePath);
                            Uri capturedImageUri = Uri.fromFile(f);
                            if (!addMedia(capturedImageUri)) {
                                ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
                            }
                            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                                    + Environment.getExternalStorageDirectory())));
                        } catch (RuntimeException e) {
                            AppLog.e(T.POSTS, e);
                        } catch (OutOfMemoryError e) {
                            AppLog.e(T.POSTS, e);
                        }
                    }
                    break;
                case RequestCodes.VIDEO_LIBRARY:
                    Uri videoUri = data.getData();
                    fetchMedia(Arrays.asList(videoUri));
                    break;
                case RequestCodes.TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(this);
                        if (!addMedia(capturedVideoUri)) {
                            ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
                        }
                    }
                    break;
            }
        }
    }

    private void startMediaGalleryAddActivity() {
        ActivityLauncher.viewMediaGalleryPickerForSite(this, mSite);
    }

    private void handleMediaGalleryPickerResult(Intent data) {
        ArrayList<Long> ids = ListUtils.fromLongArray(data.getLongArrayExtra(MediaGalleryPickerActivity.RESULT_IDS));
        if (ids == null || ids.size() == 0) {
            return;
        }

        long mediaId = ids.get(0);
        addExistingMediaToEditor(mediaId);
    }

    private void handleMediaGalleryResult(Intent data) {
        MediaGallery gallery = (MediaGallery) data.getSerializableExtra(MediaGalleryActivity.RESULT_MEDIA_GALLERY);

        // if blank gallery returned, don't add to span
        if (gallery == null || gallery.getIds().size() == 0) {
            return;
        }
        mEditorFragment.appendGallery(gallery);
    }

    private BroadcastReceiver mGalleryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LegacyEditorFragment.ACTION_MEDIA_GALLERY_TOUCHED.equals(intent.getAction())) {
                startMediaGalleryActivity((MediaGallery)intent.getSerializableExtra(LegacyEditorFragment.EXTRA_MEDIA_GALLERY));
            }
        }
    };

    /**
     * Handles media upload notifications. Used by the visual editor when uploading local media, and for both
     * the visual and the legacy editor to create a gallery after media selection from local media.
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(MediaEvents.MediaUploadSucceeded event) {
        for (Long galleryId : mPendingGalleryUploads.keySet()) {
            if (mPendingGalleryUploads.get(galleryId).contains(event.mLocalMediaId)) {
                if (mEditorMediaUploadListener != null) {
                    // Notify the visual editor of gallery image upload
                    int remaining = mPendingGalleryUploads.get(galleryId).size() - 1;
                    mEditorMediaUploadListener.onGalleryMediaUploadSucceeded(galleryId, Long.parseLong(event.mRemoteMediaId), remaining);
                } else {
                    handleGalleryImageUploadedLegacyEditor(galleryId, Long.parseLong(event.mRemoteMediaId));
                }

                mPendingGalleryUploads.get(galleryId).remove(event.mLocalMediaId);
                if (mPendingGalleryUploads.get(galleryId).size() == 0) {
                    mPendingGalleryUploads.remove(galleryId);
                }

                if (mPendingGalleryUploads.size() == 0) {
                    stopMediaUploadService();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(
                            Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(10);
                }

                return;
            }
        }

        // Notify visual editor that a normal media item has finished uploading (not part of a gallery)
        if (mEditorMediaUploadListener != null) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPostID(mPost.getId());
            mediaFile.setMediaId(event.mRemoteMediaId);
            mediaFile.setFileURL(event.mRemoteMediaUrl);
            mediaFile.setVideoPressShortCode(event.mSecondaryRemoteMediaId);
            mediaFile.setThumbnailURL(WordPressMediaUtils.getVideoPressVideoPosterFromURL(event.mRemoteMediaUrl));

            mEditorMediaUploadListener.onMediaUploadSucceeded(event.mLocalMediaId, mediaFile);
        }
    }

    private void handleGalleryImageUploadedLegacyEditor(Long galleryId, long remoteId) {
        SpannableStringBuilder postContent;
        if (mEditorFragment.getSpannedContent() != null) {
            // needed by the legacy editor to save local drafts
            postContent = new SpannableStringBuilder(mEditorFragment.getSpannedContent());
        } else {
            try {
                postContent = new SpannableStringBuilder(StringUtils.notNullStr((String) mEditorFragment.getContent()));
            } catch (IllegalEditorStateException e) {
                AppLog.e(T.EDITOR, "Impossible to handle gallery upload, we weren't able to get content from the post");
                return;
            }
        }
        int selectionStart = 0;
        int selectionEnd = postContent.length();

        MediaGalleryImageSpan[] gallerySpans = postContent.getSpans(selectionStart, selectionEnd,
                MediaGalleryImageSpan.class);
        if (gallerySpans.length != 0) {
            for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
                MediaGallery gallery = gallerySpan.getMediaGallery();
                if (gallery.getUniqueId() == galleryId) {
                    ArrayList<Long> galleryIds = gallery.getIds();
                    galleryIds.add(remoteId);
                    gallery.setIds(galleryIds);
                    gallerySpan.setMediaGallery(gallery);
                    int spanStart = postContent.getSpanStart(gallerySpan);
                    int spanEnd = postContent.getSpanEnd(gallerySpan);
                    postContent.setSpan(gallerySpan, spanStart, spanEnd,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private void refreshBlogMedia() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            MediaListPayload payload = new MediaListPayload(mSite, null, null);
            mDispatcher.dispatch(MediaActionBuilder.newFetchAllMediaAction(payload));
        } else {
            ToastUtils.showToast(this, R.string.error_media_refresh_no_connection, ToastUtils.Duration.SHORT);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.isError()) {
            final String errorMessage;
            switch (event.error.type) {
                case FS_READ_PERMISSION_DENIED:
                    errorMessage = getString(R.string.error_media_insufficient_fs_permissions);
                    break;
                case MEDIA_NOT_FOUND:
                    errorMessage = getString(R.string.error_media_not_found);
                    break;
                case UNAUTHORIZED:
                    errorMessage = getString(R.string.error_media_unauthorized);
                    break;
                case PARSE_ERROR:
                    String errorFormat = getString(R.string.error_media_parse_format);
                    errorMessage = String.format(errorFormat, event.cause.toString());
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mPendingVideoPressInfoRequests != null && !mPendingVideoPressInfoRequests.isEmpty()) {
                        // If there are pending requests for video URLs from VideoPress ids, query the DB for
                        // them again and notify the editor
                        for (String videoId : mPendingVideoPressInfoRequests) {
                            String videoUrl = mMediaStore.
                                    getUrlForSiteVideoWithVideoPressGuid(mSite, videoId);
                            String posterUrl = WordPressMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

                            mEditorFragment.setUrlForVideoPressId(videoId, videoUrl, posterUrl);
                        }

                        mPendingVideoPressInfoRequests.clear();
                    }
                }
            });
        }
    }

    private MediaUploadService.MediaUploadBinder mUploadService;
    private MediaDeleteService.MediaDeleteBinder mDeleteService;

    private ServiceConnection mUploadConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mUploadService = (MediaUploadService.MediaUploadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mUploadService = null;
        }
    };

    private ServiceConnection mDeleteConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDeleteService = (MediaDeleteService.MediaDeleteBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDeleteService = null;
        }
    };

    /**
     * Starts the upload service to upload selected media.
     */
    private void startMediaUploadService() {
        if (mUploadService == null) {
            Intent intent = new Intent(this, MediaUploadService.class);
            intent.putExtra(WordPress.SITE, mSite);
            bindService(intent, mUploadConnection, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
            startService(intent);
        }
    }

    /**
     * Stops the upload service.
     */
    private void stopMediaUploadService() {
        if (mUploadService != null) {
            stopService(new Intent(this, MediaUploadService.class));
            unbindService(mUploadConnection);
            mUploadService = null;
        }
    }

    /**
     * Queues a media file for upload and starts the MediaUploadService. Toasts will alert the user
     * if there are issues with the file.
     *
     * @param path
     *  local path of the media file to upload
     * @param mediaIdOut
     *  the new {@link org.wordpress.android.util.helpers.MediaFile} ID is added if non-null
     */
    private MediaFile queueFileForUpload(String path, ArrayList<Long> mediaIdOut) {
        return queueFileForUpload(path, mediaIdOut, "queued");
    }

    private MediaFile queueFileForUpload(String path, ArrayList<Long> mediaIdOut, String startingState) {
        // Invalid file path
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this, R.string.editor_toast_invalid_path, Toast.LENGTH_SHORT).show();
            return null;
        }

        // File not found
        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            return null;
        }

        long currentTime = System.currentTimeMillis();
        String mimeType = MediaUtils.getMediaFileMimeType(file);
        String fileName = MediaUtils.getMediaFileName(file, mimeType);
        MediaFile mediaFile = new MediaFile();

        mediaFile.setBlogId(String.valueOf(mSite.getId()));
        mediaFile.setFileName(fileName);
        mediaFile.setFilePath(path);
        mediaFile.setUploadState(startingState);
        mediaFile.setDateCreatedGMT(currentTime);
        mediaFile.setMediaId(String.valueOf(currentTime));
        mediaFile.setVideo(MediaUtils.isVideo(path));

        if (mimeType != null && mimeType.startsWith("image")) {
            // get width and height
            BitmapFactory.Options bfo = new BitmapFactory.Options();
            bfo.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bfo);
            mediaFile.setWidth(bfo.outWidth);
            mediaFile.setHeight(bfo.outHeight);
        }

        if (!TextUtils.isEmpty(mimeType)) {
            mediaFile.setMimeType(mimeType);
        }

        if (mediaIdOut != null) {
            mediaIdOut.add(Long.parseLong(mediaFile.getMediaId()));
        }

        saveMediaFile(mediaFile);
        startMediaUploadService();

        return mediaFile;
    }

    /**
     * EditorFragmentListener methods
     */

    @Override
    public void onSettingsClicked() {
        mViewPager.setCurrentItem(PAGE_SETTINGS);
    }

    @Override
    public void onAddMediaClicked() {
        // no op
    }

    @Override
    public void onMediaDropped(final ArrayList<Uri> mediaUris) {
        mDroppedMediaUris = mediaUris;

        if (PermissionUtils.checkAndRequestStoragePermission(this, DRAG_AND_DROP_MEDIA_PERMISSION_REQUEST_CODE)) {
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
    public void onMediaRetryClicked(String mediaId) {
        if (mUploadService == null) {
            startMediaUploadService();
        } else {
        }
        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED);
    }

    @Override
    public void onMediaUploadCancelClicked(String mediaId, boolean delete) {
        MediaModel media = new MediaModel();
        media.setMediaId(Long.valueOf(mediaId));
        MediaPayload payload = new MediaPayload(mSite, media);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    @Override
    public void onFeaturedImageChanged(long mediaId) {
        mPost.setFeaturedImageId(mediaId);
        mEditPostSettingsFragment.updateFeaturedImage(mediaId);
    }

    @Override
    public void onVideoPressInfoRequested(final String videoId) {
        String videoUrl = mMediaStore.
                getUrlForSiteVideoWithVideoPressGuid(mSite, videoId);

        if (videoUrl.isEmpty()) {
            if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, MEDIA_PERMISSION_REQUEST_CODE)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mPendingVideoPressInfoRequests == null) {
                            mPendingVideoPressInfoRequests = new ArrayList<>();
                        }
                        mPendingVideoPressInfoRequests.add(videoId);
                        refreshBlogMedia();
                    }
                });
            } else {
                AppLockManager.getInstance().setExtendedTimeout();
            }
        }

        String posterUrl = WordPressMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

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
        fillContentEditorFields();
        // Set the error listener
        if (mEditorFragment instanceof EditorFragment) {
            mEditorFragment.setDebugModeEnabled(BuildConfig.DEBUG);
            ((EditorFragment) mEditorFragment).setWebViewErrorListener(new ErrorListener() {
                @Override
                public void onJavaScriptError(String sourceFile, int lineNumber, String message) {
                    CrashlyticsUtils.logException(new JavaScriptException(sourceFile, lineNumber, message),
                            ExceptionType.SPECIFIC, T.EDITOR,
                            String.format(Locale.US, "%s:%d: %s", sourceFile, lineNumber, message));
                }

                @Override
                public void onJavaScriptAlert(String url, String message) {
                    // no op
                }
            });
        }
    }

    @Override
    public void saveMediaFile(MediaFile mediaFile) {
    }

    @Override
    public void onTrackableEvent(TrackableEvent event) {
        switch (event) {
            case HTML_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HTML);
                break;
            case MEDIA_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_IMAGE);
                break;
            case UNLINK_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_UNLINK);
                break;
            case LINK_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LINK);
                break;
            case IMAGE_EDITED:
                AnalyticsTracker.track(Stat.EDITOR_EDITED_IMAGE);
                break;
            case BOLD_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_BOLD);
                break;
            case ITALIC_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ITALIC);
                break;
            case OL_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ORDERED_LIST);
                break;
            case UL_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_UNORDERED_LIST);
                break;
            case BLOCKQUOTE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_BLOCKQUOTE);
                break;
            case STRIKETHROUGH_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_STRIKETHROUGH);
                break;
            case UNDERLINE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_UNDERLINE);
                break;
            case MORE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_MORE);
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPostInstantiated(OnPostInstantiated event) {
        mPost = event.post;
        mNewPostLatch.countDown();
    }
}
