package org.wordpress.android.ui.posts;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.SuggestionSpan;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorWebViewAbstract.ErrorListener;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.editor.LegacyEditorFragment;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.MediaGridFragment;
import org.wordpress.android.ui.media.MediaPickerActivity;
import org.wordpress.android.ui.media.MediaSourceWPImages;
import org.wordpress.android.ui.media.MediaSourceWPVideos;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.media.services.MediaEvents;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.suggestion.adapters.TagSuggestionAdapter;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutolinkUtils;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.CrashlyticsUtils.ExceptionType;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.util.helpers.MediaGalleryImageSpan;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;
import org.wordpress.mediapicker.source.MediaSourceDeviceImages;
import org.wordpress.mediapicker.source.MediaSourceDeviceVideos;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.ApiHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

public class EditPostActivity extends AppCompatActivity implements EditorFragmentListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String EXTRA_POSTID = "postId";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_NEW_POST = "isNewPost";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String STATE_KEY_CURRENT_POST = "stateKeyCurrentPost";
    public static final String STATE_KEY_ORIGINAL_POST = "stateKeyOriginalPost";
    public static final String STATE_KEY_EDITOR_FRAGMENT = "editorFragment";

    // Context menu positioning
    private static final int SELECT_PHOTO_MENU_POSITION = 0;
    private static final int CAPTURE_PHOTO_MENU_POSITION = 1;
    private static final int SELECT_VIDEO_MENU_POSITION = 2;
    private static final int CAPTURE_VIDEO_MENU_POSITION = 3;
    private static final int ADD_GALLERY_MENU_POSITION = 4;
    private static final int SELECT_LIBRARY_MENU_POSITION = 5;
    private static final int NEW_PICKER_MENU_POSITION = 6;

    public static final int MEDIA_PERMISSION_REQUEST_CODE = 1;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    private static final String PROP_LOCAL_PHOTOS = "number_of_local_photos_added";
    private static final String PROP_LIBRARY_PHOTOS = "number_of_wp_library_photos_added";
    private static final String PROP_LOCAL_VIDEOS = "number_of_local_videos_added";
    private static final String PROP_LIBRARY_VIDEOS = "number_of_wp_library_videos_added";

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;

    private static final int AUTOSAVE_INTERVAL_MILLIS = 10000;
    private Timer mAutoSaveTimer;

    private boolean mShowNewEditor;

    // Each element is a list of media IDs being uploaded to a gallery, keyed by gallery ID
    private Map<Long, List<String>> mPendingGalleryUploads = new HashMap<>();

    // -1=no response yet, 0=unavailable, 1=available
    private int mBlogMediaStatus = -1;
    private boolean mMediaUploadServiceStarted;
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

    private Post mPost;
    private Post mOriginalPost;
    private TagSuggestionAdapter mTagSuggestionAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;
    private int remoteBlogId;

    private EditorFragmentAbstract mEditorFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditPostPreviewFragment mEditPostPreviewFragment;
    private SuggestionAutoCompleteText mTags;

    private EditorMediaUploadListener mEditorMediaUploadListener;

    private boolean mIsNewPost;
    private boolean mIsPage;
    private boolean mHasSetPostContent;

    // For opening the context menu after permissions have been granted
    private View mMenuView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_edit_post_activity);

        // Check whether to show the visual editor
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
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
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)
                    || NEW_MEDIA_GALLERY.equals(action)
                    || NEW_MEDIA_POST.equals(action)
                    || getIntent().hasExtra(EXTRA_IS_QUICKPRESS)
                    || (extras != null && extras.getInt("quick-media", -1) > -1)) {
                if (getIntent().hasExtra(EXTRA_QUICKPRESS_BLOG_ID)) {
                    // QuickPress might want to use a different blog than the current blog
                    int blogId = getIntent().getIntExtra(EXTRA_QUICKPRESS_BLOG_ID, -1);
                    Blog quickPressBlog = WordPress.wpDB.instantiateBlogByLocalId(blogId);
                    if (quickPressBlog == null) {
                        showErrorAndFinish(R.string.blog_not_found);
                        return;
                    }
                    if (quickPressBlog.isHidden()) {
                        showErrorAndFinish(R.string.error_blog_hidden);
                        return;
                    }
                    WordPress.currentBlog = quickPressBlog;
                }

                // Create a new post for share intents and QuickPress
                mPost = new Post(WordPress.getCurrentLocalTableBlogId(), false);
                mPost.setCategories("[" + SiteSettingsInterface.getDefaultCategory(this) +"]");
                mPost.setPostFormat(SiteSettingsInterface.getDefaultFormat(this));
                WordPress.wpDB.savePost(mPost);
                mIsNewPost = true;
            } else if (extras != null) {
                // Load post from the postId passed in extras
                long localTablePostId = extras.getLong(EXTRA_POSTID, -1);
                mIsPage = extras.getBoolean(EXTRA_IS_PAGE);
                mIsNewPost = extras.getBoolean(EXTRA_IS_NEW_POST);
                mPost = WordPress.wpDB.getPostForLocalTablePostId(localTablePostId);
                mOriginalPost = WordPress.wpDB.getPostForLocalTablePostId(localTablePostId);
            } else {
                // A postId extra must be passed to this activity
                showErrorAndFinish(R.string.post_not_found);
                return;
            }
        } else {
            if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_POST)) {
                try {
                    mPost = (Post) savedInstanceState.getSerializable(STATE_KEY_CURRENT_POST);
                    mOriginalPost = (Post) savedInstanceState.getSerializable(STATE_KEY_ORIGINAL_POST);
                } catch (ClassCastException e) {
                    mPost = null;
                }
            }
            mEditorFragment = (EditorFragmentAbstract) fragmentManager.getFragment(savedInstanceState, STATE_KEY_EDITOR_FRAGMENT);

            if (mEditorFragment instanceof EditorMediaUploadListener) {
                mEditorMediaUploadListener = (EditorMediaUploadListener) mEditorFragment;
            }
        }

        if (mHasSetPostContent = mEditorFragment != null) {
            mEditorFragment.setImageLoader(WordPress.imageLoader);
        }

        // Ensure we have a valid blog
        if (WordPress.getCurrentBlog() == null) {
            showErrorAndFinish(R.string.blog_not_found);
            return;
        }

        // Ensure we have a valid post
        if (mPost == null) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        if (mIsNewPost) {
            trackEditorCreatedPost(action, getIntent());
        }

        setTitle(StringUtils.unescapeHTML(WordPress.getCurrentBlog().getBlogName()));

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
                    setTitle(StringUtils.unescapeHTML(WordPress.getCurrentBlog().getBlogName()));
                } else if (position == PAGE_SETTINGS) {
                    setTitle(mPost.isPage() ? R.string.page_settings : R.string.post_settings);
                } else if (position == PAGE_PREVIEW) {
                    setTitle(mPost.isPage() ? R.string.preview_page : R.string.preview_post);
                    savePost(true);
                    if (mEditPostPreviewFragment != null) {
                        mEditPostPreviewFragment.loadPost();
                    }
                }
            }
        });
        ActivityId.trackLastActivity(ActivityId.POST_EDITOR);
    }

    class AutoSaveTask extends TimerTask {
        public void run() {
            savePost(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAutoSaveTimer = new Timer();
        mAutoSaveTimer.scheduleAtFixedRate(new AutoSaveTask(), AUTOSAVE_INTERVAL_MILLIS, AUTOSAVE_INTERVAL_MILLIS);
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
        mAutoSaveTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_CLOSED);

        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves both post objects so we can restore them in onCreate()
        savePost(true);
        outState.putSerializable(STATE_KEY_CURRENT_POST, mPost);
        outState.putSerializable(STATE_KEY_ORIGINAL_POST, mOriginalPost);

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

        mTags = (SuggestionAutoCompleteText) findViewById(R.id.tags);
        if (mTags != null) {
            mTags.setTokenizer(new SuggestionAutoCompleteText.CommaTokenizer());

            remoteBlogId = -1;
            String blogID = WordPress.getCurrentRemoteBlogId();
            if (blogID != null) {
                try {
                    remoteBlogId = Integer.parseInt(blogID);
                } catch (NumberFormatException e) {
                    AppLog.e(T.EDITOR, "The remote blog ID can't be parsed as Integer: " + remoteBlogId);
                }
            }

            mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(this, remoteBlogId);
            mTagSuggestionAdapter = SuggestionUtils.setupTagSuggestions(remoteBlogId, this, mSuggestionServiceConnectionManager);
            if (mTagSuggestionAdapter != null) {
                mTags.setAdapter(mTagSuggestionAdapter);
            }
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
                switch (mPost.getStatusEnum()) {
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
            default:
                break;
        }
    }

    private void trackSavePostAnalytics() {
        PostStatus status = mPost.getStatusEnum();
        switch (status) {
            case PUBLISHED:
                if (!mPost.isLocalDraft()) {
                    AnalyticsUtils.trackWithBlogDetails(
                            AnalyticsTracker.Stat.EDITOR_UPDATED_POST,
                            WordPress.getBlog(mPost.getLocalTableBlogId())
                    );
                } else {
                    // Analytics for the event EDITOR_PUBLISHED_POST are tracked in PostUploadService
                }
                break;
            case SCHEDULED:
                if (!mPost.isLocalDraft()) {
                    AnalyticsUtils.trackWithBlogDetails(
                            AnalyticsTracker.Stat.EDITOR_UPDATED_POST,
                            WordPress.getBlog(mPost.getLocalTableBlogId())
                    );
                } else {
                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put("word_count", AnalyticsUtils.getWordCount(mPost.getContent()));
                    AnalyticsUtils.trackWithBlogDetails(
                            AnalyticsTracker.Stat.EDITOR_SCHEDULED_POST,
                            WordPress.getBlog(mPost.getLocalTableBlogId()),
                            properties
                    );
                }
                break;
            case DRAFT:
                AnalyticsUtils.trackWithBlogDetails(
                        AnalyticsTracker.Stat.EDITOR_SAVED_DRAFT,
                        WordPress.getBlog(mPost.getLocalTableBlogId())
                );
                break;
            default:
                // No-op
        }
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

        MediaUploadService mediaUploadService = MediaUploadService.getInstance();

        // Disable format bar buttons while a media upload is in progress
        if (mediaUploadService != null && mediaUploadService.hasUploads()) {
            ToastUtils.showToast(this, R.string.editor_toast_uploading_please_wait, Duration.SHORT);
            return false;
        }

        // Disable ActionBar buttons while there are failed media uploads in the post/page
        if (mEditorFragment.hasFailedMediaUploads()) {
            ToastUtils.showToast(this, R.string.editor_toast_failed_uploads, Duration.SHORT);
            return false;
        }

        if (itemId == R.id.menu_save_post) {
            // If the post is new and there are no changes, don't publish
            updatePostObject(false);
            if (!mPost.isPublishable()) {
                ToastUtils.showToast(this, R.string.error_publish_empty_post, Duration.SHORT);
                return false;
            }

            savePostToDb();
            trackSavePostAnalytics();

            if (!NetworkUtils.isNetworkAvailable(this)) {
                ToastUtils.showToast(this, R.string.error_publish_no_network, Duration.SHORT);
                return false;
            }

            PostUploadService.addPostToUpload(mPost);
            PostUploadService.setLegacyMode(!mShowNewEditor);
            startService(new Intent(this, PostUploadService.class));
            setResult(RESULT_OK);
            finish();
            return true;
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

    @Override
    public void openContextMenu(View view) {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, MEDIA_PERMISSION_REQUEST_CODE)) {
            super.openContextMenu(view);
        } else {
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
        menu.add(0, NEW_PICKER_MENU_POSITION, 0, getResources().getText(R.string.select_from_new_picker));
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
            case NEW_PICKER_MENU_POSITION:
                startMediaSelection();
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

    public Post getPost() {
        return mPost;
    }

    private void trackEditorCreatedPost(String action, Intent intent) {
        Map<String, Object> properties = new HashMap<String, Object>();
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
        AnalyticsUtils.trackWithBlogDetails(
                AnalyticsTracker.Stat.EDITOR_CREATED_POST,
                WordPress.getBlog(mPost.getLocalTableBlogId()),
                properties
        );
    }

    private void updatePostObject(boolean isAutosave) {
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
    }

    private void savePost(boolean isAutosave) {
        updatePostObject(isAutosave);

        savePostToDb();
    }

    private void savePostToDb() {
        WordPress.wpDB.updatePost(mPost);
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

    private void saveAndFinish() {
        // Fetch post title and content from editor fields and update the Post object
        savePost(true);

        if (mEditorFragment != null && mPost.hasEmptyContentFields()) {
            // new and empty post? delete it
            if (mIsNewPost) {
                WordPress.wpDB.deletePost(mPost);
                finish();
                return;
            }
        } else if (mOriginalPost != null && !mPost.hasChanges(mOriginalPost)) {
            // if no changes have been made to the post, set it back to the original don't save it
            WordPress.wpDB.updatePost(mOriginalPost);
            finish();
            return;
        } else {
            // changes have been made, save the post and ask for the post list to refresh.
            // We consider this being "manual save", it will replace some Android "spans" by an html
            // or a shortcode replacement (for instance for images and galleries)
            if (mShowNewEditor) {
                // Update the post object directly, without re-fetching the fields from the EditorFragment
                updatePostContentNewEditor(false, mPost.getTitle(), mPost.getContent());
                savePostToDb();
            } else {
                // TODO: Remove when legacy editor is dropped
                savePost(false);
            }
        }

        Intent i = new Intent();
        i.putExtra(EXTRA_SAVED_AS_LOCAL_DRAFT, true);
        i.putExtra(EXTRA_IS_PAGE, mIsPage);
        setResult(RESULT_OK, i);

        ToastUtils.showToast(this, R.string.editor_toast_changes_saved);

        finish();
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
                        return new EditorFragment();
                    } else {
                        return new LegacyEditorFragment();
                    }
                case 1:
                    return new EditPostSettingsFragment();
                default:
                    return new EditPostPreviewFragment();
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

    public boolean isEditingPostContent() {
        return (mViewPager.getCurrentItem() == PAGE_CONTENT);
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

    private MediaFile createMediaFile(String blogId, final String mediaId) {
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);

        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }

        String url = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL));
        if (url == null) {
            cursor.close();
            return null;
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaId(mediaId);
        mediaFile.setBlogId(blogId);
        mediaFile.setFileURL(url);
        mediaFile.setCaption(cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_CAPTION)));
        mediaFile.setDescription(cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_DESCRIPTION)));
        mediaFile.setTitle(cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_TITLE)));
        mediaFile.setWidth(cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_WIDTH)));
        mediaFile.setHeight(cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_HEIGHT)));
        mediaFile.setFileName(cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME)));
        mediaFile.setDateCreatedGMT(cursor.getLong(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_DATE_CREATED_GMT)));
        mediaFile.setVideoPressShortCode(cursor.getString(cursor.getColumnIndex(
                WordPressDB.COLUMN_NAME_VIDEO_PRESS_SHORTCODE)));

        String mimeType = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MIME_TYPE));
        mediaFile.setMimeType(mimeType);

        if (mimeType != null && !mimeType.isEmpty()) {
            mediaFile.setVideo(mimeType.contains("video"));
        } else {
            mediaFile.setVideo(MediaUtils.isVideo(url));
        }

        // Make sure we're using a valid thumbnail for video. XML-RPC returns the video URL itself as the thumbnail URL
        // for videos. If we can't get a real thumbnail for the Media Library video (currently only possible for
        // VideoPress videos), we should not set any thumbnail.
        String thumbnailUrl = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_THUMBNAIL_URL));
        if (mediaFile.isVideo() && !MediaUtils.isValidImage(thumbnailUrl)) {
            if (WPUrlUtils.isWordPressCom(url)) {
                thumbnailUrl = WordPressMediaUtils.getVideoPressVideoPosterFromURL(url);
            } else {
                thumbnailUrl = "";
            }
        }
        mediaFile.setThumbnailURL(thumbnailUrl);

        WordPress.wpDB.saveMediaFile(mediaFile);
        cursor.close();
        return mediaFile;
    }

    private void addExistingMediaToEditor(String mediaId) {
        if (WordPress.getCurrentBlog() == null) {
            return;
        }
        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        MediaFile mediaFile = createMediaFile(blogId, mediaId);
        if (mediaFile == null) {
            return;
        }
        trackAddMediaEvents(mediaFile.isVideo(), true);
        mEditorFragment.appendMediaFile(mediaFile, getMediaUrl(mediaFile), WordPress.imageLoader);
    }

    /**
     * Get media url from a MediaFile, returns a photon URL if the selected blog is Photon capable.
     */
    private String getMediaUrl(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }

        // Since Photon doesn't support video, skip Photon checking and return the existing file URL
        // (using a Photon URL for video will result in a 404 error)
        if (mediaFile.isVideo()) {
            return mediaFile.getFileURL();
        }

        String imageURL;
        if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable()) {
            String photonUrl = mediaFile.getFileURL();
            imageURL = StringUtils.getPhotonUrl(photonUrl, getMaximumThumbnailWidthForEditor());
        } else {
            // Not a Jetpack or wpcom blog
            // imageURL = mediaFile.getThumbnailURL(); // do not use fileURL here since downloading picture
            // of big dimensions can result in OOM Exception
            imageURL = mediaFile.getFileURL() != null ?  mediaFile.getFileURL() : mediaFile.getThumbnailURL();
        }
        return imageURL;
    }

    private class LoadPostContentTask extends AsyncTask<String, Spanned, Spanned> {
        @Override
        protected Spanned doInBackground(String... params) {
            if (params.length < 1 || getPost() == null) {
                return null;
            }

            String content = StringUtils.notNullStr(params[0]);
            return WPHtml.fromHtml(content, EditPostActivity.this, getPost(), getMaximumThumbnailWidthForEditor());
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            if (spanned != null) {
                mEditorFragment.setContent(spanned);
            }
        }
    }

    private class HandleMediaSelectionTask extends AsyncTask<Intent, Void, Void> {
        @Override
        protected Void doInBackground(Intent... params) {
            handleMediaSelectionResult(params[0]);
            return null;
        }
    }

    private void fillContentEditorFields() {
        // Needed blog settings needed by the editor
        if (WordPress.getCurrentBlog() != null) {
            mEditorFragment.setFeaturedImageSupported(WordPress.getCurrentBlog().isFeaturedImageCapable());
            mEditorFragment.setBlogSettingMaxImageWidth(WordPress.getCurrentBlog().getMaxImageWidth());
        }

        // Set up the placeholder text
        mEditorFragment.setContentPlaceholder(getString(R.string.editor_content_placeholder));
        mEditorFragment.setTitlePlaceholder(getString(mIsPage ? R.string.editor_page_title_placeholder :
                R.string.editor_post_title_placeholder));

        // Set post title and content
        Post post = getPost();
        if (post != null) {
            if (!TextUtils.isEmpty(post.getContent()) && !mHasSetPostContent) {
                mHasSetPostContent = true;
                if (post.isLocalDraft() && !mShowNewEditor) {
                    // TODO: Unnecessary for new editor, as all images are uploaded right away, even for local drafts
                    // Load local post content in the background, as it may take time to generate images
                    new LoadPostContentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            post.getContent().replaceAll("\uFFFC", ""));
                } else {
                    // TODO: Might be able to drop .replaceAll() when legacy editor is removed
                    mEditorFragment.setContent(post.getContent().replaceAll("\uFFFC", ""));
                }
            }
            if (!TextUtils.isEmpty(post.getTitle())) {
                mEditorFragment.setTitle(post.getTitle());
            }
            // TODO: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setLocalDraft(post.isLocalDraft());

            mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
        }

        // Special actions
        String action = getIntent().getAction();
        int quickMediaType = getIntent().getIntExtra("quick-media", -1);
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            setPostContentFromShareAction();
        } else if (NEW_MEDIA_GALLERY.equals(action)) {
            prepareMediaGallery();
        } else if (NEW_MEDIA_POST.equals(action)) {
            prepareMediaPost();
        } else if (quickMediaType >= 0) {
            // User selected 'Quick Photo' in the menu drawer
            if (quickMediaType == Constants.QUICK_POST_PHOTO_CAMERA) {
                launchCamera();
            } else if (quickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY) {
                WordPressMediaUtils.launchPictureLibrary(this);
            }
            if (post != null) {
                post.setQuickPostType(Post.QUICK_MEDIA_TYPE_PHOTO);
            }
        }
    }

    private void launchCamera() {
        WordPressMediaUtils.launchCamera(this, new WordPressMediaUtils.LaunchCameraCallback() {
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
            mEditorFragment.setContent(WPHtml.fromHtml(StringUtils.addPTags(text), this, getPost(),
                    getMaximumThumbnailWidthForEditor()));
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
        Intent intent = new Intent(this, MediaGalleryActivity.class);
        intent.putExtra(MediaGalleryActivity.PARAMS_MEDIA_GALLERY, mediaGallery);
        if (mediaGallery == null) {
            intent.putExtra(MediaGalleryActivity.PARAMS_LAUNCH_PICKER, true);
        }
        startActivityForResult(intent, MediaGalleryActivity.REQUEST_CODE);
    }

    private void prepareMediaGallery() {
        MediaGallery mediaGallery = new MediaGallery();
        mediaGallery.setIds(getIntent().getStringArrayListExtra(NEW_MEDIA_GALLERY_EXTRA_IDS));
        startMediaGalleryActivity(mediaGallery);
    }

    private void prepareMediaPost() {
        String mediaId = getIntent().getStringExtra(NEW_MEDIA_POST_EXTRA);
        addExistingMediaToEditor(mediaId);
    }

    // TODO: Replace with contents of the updatePostContentNewEditor() method when legacy editor is dropped
    /**
     * Updates post object with content of this fragment
     */
    public void updatePostContent(boolean isAutoSave) {
        Post post = getPost();

        if (post == null) {
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
        if (post.isLocalDraft()) {
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
                        updateMediaFileOnServer(wpIS);
                    } else {
                        mediaFile.setFileName(wpIS.getImageSource().toString());
                        mediaFile.setFilePath(wpIS.getImageSource().toString());
                        WordPress.wpDB.saveMediaFile(mediaFile);
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

        String moreTag = "<!--more-->";

        post.setTitle(title);
        // split up the post content if there's a more tag
        if (post.isLocalDraft() && content.contains(moreTag)) {
            post.setDescription(content.substring(0, content.indexOf(moreTag)));
            post.setMoreText(content.substring(content.indexOf(moreTag) + moreTag.length(), content.length()));
        } else {
            post.setDescription(content);
            post.setMoreText("");
        }

        if (!post.isLocalDraft()) {
            post.setLocalChange(true);
        }
    }

    /**
     * Updates post object with given title and content
     */
    public void updatePostContentNewEditor(boolean isAutoSave, String title, String content) {
        Post post = getPost();

        if (post == null) {
            return;
        }

        if (!isAutoSave) {
            // TODO: Shortcode handling, media handling
        }

        String moreTag = "<!--more-->";

        post.setTitle(title);
        // split up the post content if there's a more tag
        if (post.isLocalDraft() && content.contains(moreTag)) {
            post.setDescription(content.substring(0, content.indexOf(moreTag)));
            post.setMoreText(content.substring(content.indexOf(moreTag) + moreTag.length(), content.length()));
        } else {
            post.setDescription(content);
            post.setMoreText("");
        }

        if (!post.isLocalDraft()) {
            post.setLocalChange(true);
        }
    }

    /**
     * Media
     */

    private void fetchMedia(Uri mediaUri) {
        if (mediaUri == null) {
            Toast.makeText(EditPostActivity.this,
                    getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
            return;
        }
        if (URLUtil.isNetworkUrl(mediaUri.toString())) {
            // Create an AsyncTask to download the file
            new DownloadMediaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaUri);
        } else {
            // It is a regular local image file
            if (!addMedia(mediaUri)) {
                Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private class DownloadMediaTask extends AsyncTask<Uri, Integer, Uri> {
        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return MediaUtils.downloadExternalMedia(EditPostActivity.this, imageUri);
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(EditPostActivity.this, R.string.download, Toast.LENGTH_SHORT).show();
        }

        protected void onPostExecute(Uri newUri) {
            if (newUri != null) {
                addMedia(newUri);
            } else {
                Toast.makeText(EditPostActivity.this, getString(R.string.error_downloading_image), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void updateMediaFileOnServer(WPImageSpan wpIS) {
        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null || wpIS == null)
            return;

        MediaFile mf = wpIS.getMediaFile();

        final String mediaId = mf.getMediaId();
        final String title = mf.getTitle();
        final String description = mf.getDescription();
        final String caption = mf.getCaption();

        ApiHelper.EditMediaItemTask task = new ApiHelper.EditMediaItemTask(mf.getMediaId(), mf.getTitle(),
                mf.getDescription(), mf.getCaption(),
                new ApiHelper.GenericCallback() {
                    @Override
                    public void onSuccess() {
                        if (WordPress.getCurrentBlog() == null) {
                            return;
                        }
                        String localBlogTableIndex = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
                        WordPress.wpDB.updateMediaFile(localBlogTableIndex, mediaId, title, description, caption);
                    }

                    @Override
                    public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                        Toast.makeText(EditPostActivity.this, R.string.media_edit_failure, Toast.LENGTH_LONG).show();
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(currentBlog);
        task.execute(apiArgs);
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

    private boolean addMediaVisualEditor(Uri imageUri) {
        String path = "";
        if (imageUri.toString().contains("content:")) {
            String[] projection = new String[]{MediaStore.Images.Media.DATA};

            Cursor cur = getContentResolver().query(imageUri, projection, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                path = cur.getString(dataColumn);
                cur.close();
            }
        } else {
            // File is not in media library
            path = imageUri.toString().replace("file://", "");
        }

        if (path == null) {
            ToastUtils.showToast(this, R.string.file_not_found, Duration.SHORT);
            return false;
        }

        Blog blog = WordPress.getCurrentBlog();
        if (!blog.getMaxImageWidth().equals("Original Size")) {
            // If the user has selected a maximum image width for uploads, rescale the image accordingly
            path = ImageUtils.createResizedImageWithMaxWidth(this, path, Integer.parseInt(blog.getMaxImageWidth()));
        }

        MediaFile mediaFile = queueFileForUpload(path, new ArrayList<String>());
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
        mediaFile.setPostID(getPost().getLocalTablePostId());
        mediaFile.setTitle(mediaTitle);
        mediaFile.setFilePath(mediaUri.toString());
        if (mediaUri.getEncodedPath() != null) {
            mediaFile.setVideo(isVideo);
        }
        WordPress.wpDB.saveMediaFile(mediaFile);
        mEditorFragment.appendMediaFile(mediaFile, mediaFile.getFilePath(), WordPress.imageLoader);

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO ||
                requestCode == RequestCodes.TAKE_VIDEO))) {
            switch (requestCode) {
                case MediaPickerActivity.ACTIVITY_REQUEST_CODE_MEDIA_SELECTION:
                    if (resultCode == MediaPickerActivity.ACTIVITY_RESULT_CODE_MEDIA_SELECTED) {
                        new HandleMediaSelectionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                data);
                    } else if (resultCode == MediaPickerActivity.ACTIVITY_RESULT_CODE_GALLERY_CREATED) {
                        handleGalleryResult(data);
                    }
                    break;
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
                    fetchMedia(imageUri);
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
                    } else if (TextUtils.isEmpty(mEditorFragment.getContent())) {
                        // TODO: check if it was mQuickMediaType > -1
                        // Quick Photo was cancelled, delete post and finish activity
                        WordPress.wpDB.deletePost(getPost());
                        finish();
                    }
                    break;
                case RequestCodes.VIDEO_LIBRARY:
                    Uri videoUri = data.getData();
                    fetchMedia(videoUri);
                    break;
                case RequestCodes.TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(this);
                        if (!addMedia(capturedVideoUri)) {
                            ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
                        }
                    } else if (TextUtils.isEmpty(mEditorFragment.getContent())) {
                        // TODO: check if it was mQuickMediaType > -1
                        // Quick Photo was cancelled, delete post and finish activity
                        WordPress.wpDB.deletePost(getPost());
                        finish();
                    }
                    break;
            }
        }
    }

    private void startMediaGalleryAddActivity() {
        Intent intent = new Intent(this, MediaGalleryPickerActivity.class);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECT_ONE_ITEM, true);
        startActivityForResult(intent, MediaGalleryPickerActivity.REQUEST_CODE);
    }

    private void handleMediaGalleryPickerResult(Intent data) {
        ArrayList<String> ids = data.getStringArrayListExtra(MediaGalleryPickerActivity.RESULT_IDS);
        if (ids == null || ids.size() == 0) {
            return;
        }

        String mediaId = ids.get(0);
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

    /**
     * Handles result from {@link org.wordpress.android.ui.media.MediaPickerActivity}. Uploads local
     * media to users blog then adds a gallery to the Post with all the selected media.
     *
     * @param data
     *  contains the selected media content with key
     *  {@link org.wordpress.android.ui.media.MediaPickerActivity#SELECTED_CONTENT_RESULTS_KEY}
     */
    private void handleGalleryResult(Intent data) {
        if (data != null) {
            List<MediaItem> selectedContent = data.getParcelableArrayListExtra(MediaPickerActivity.SELECTED_CONTENT_RESULTS_KEY);

            if (selectedContent != null && selectedContent.size() > 0) {
                ArrayList<String> blogMediaIds = new ArrayList<>();
                ArrayList<String> localMediaIds = new ArrayList<>();

                for (MediaItem content : selectedContent) {
                    Uri source = content.getSource();
                    final String id = content.getTag();

                    if (source != null && id != null) {
                        final String sourceString = source.toString();

                        if (MediaUtils.isVideo(sourceString)) {
                            // Videos cannot be added to a gallery, insert inline instead
                            addMedia(source);
                        } else if (URLUtil.isNetworkUrl(sourceString)) {
                            blogMediaIds.add(id);
                        } else if (MediaUtils.isValidImage(sourceString)) {
                            queueFileForUpload(sourceString, localMediaIds);
                        }
                    }
                }

                MediaGallery gallery = new MediaGallery();
                gallery.setIds(blogMediaIds);

                if (localMediaIds.size() > 0) {
                    NotificationManager notificationManager = (NotificationManager) getSystemService(
                            Context.NOTIFICATION_SERVICE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                    builder.setSmallIcon(android.R.drawable.stat_sys_upload);
                    builder.setContentTitle("Uploading gallery");
                    notificationManager.notify(10, builder.build());

                    mPendingGalleryUploads.put(gallery.getUniqueId(), new ArrayList<>(localMediaIds));
                }

                // Only insert gallery span if images were added
                if (localMediaIds.size() > 0 || blogMediaIds.size() > 0) {
                    mEditorFragment.appendGallery(gallery);
                }
            }
        }
    }

    /**
     * Handles result from {@link org.wordpress.android.ui.media.MediaPickerActivity} by adding the
     * selected media to the Post.
     *
     * @param data
     *  result {@link android.content.Intent} with selected media items
     */
    private void handleMediaSelectionResult(Intent data) {
        if (data == null) {
            return;
        }
        final List<MediaItem> selectedContent =
                data.getParcelableArrayListExtra(MediaPickerActivity.SELECTED_CONTENT_RESULTS_KEY);
        if (selectedContent != null && selectedContent.size() > 0) {
            for (MediaItem media : selectedContent) {
                if (URLUtil.isNetworkUrl(media.getSource().toString())) {
                    addExistingMediaToEditor(media.getTag());
                } else {
                    addMedia(media.getSource());
                }
            }
        }
    }

    /**
     * Create image {@link org.wordpress.mediapicker.source.MediaSource}'s for media selection.
     *
     * @return
     *  list containing all sources to gather image media from
     */
    private ArrayList<MediaSource> imageMediaSelectionSources() {
        ArrayList<MediaSource> imageMediaSources = new ArrayList<>();
        imageMediaSources.add(new MediaSourceDeviceImages());

        return imageMediaSources;
    }

    private ArrayList<MediaSource> blogImageMediaSelectionSources() {
        ArrayList<MediaSource> imageMediaSources = new ArrayList<>();
        imageMediaSources.add(new MediaSourceWPImages());

        return imageMediaSources;
    }

    private ArrayList<MediaSource> blogVideoMediaSelectionSources() {
        ArrayList<MediaSource> imageMediaSources = new ArrayList<>();
        imageMediaSources.add(new MediaSourceWPVideos());

        return imageMediaSources;
    }

    /**
     * Create video {@link org.wordpress.mediapicker.source.MediaSource}'s for media selection.
     *
     * @return
     *  list containing all sources to gather video media from
     */
    private ArrayList<MediaSource> videoMediaSelectionSources() {
        ArrayList<MediaSource> videoMediaSources = new ArrayList<>();
        videoMediaSources.add(new MediaSourceDeviceVideos());

        return videoMediaSources;
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
                    mEditorMediaUploadListener.onGalleryMediaUploadSucceeded(galleryId, event.mRemoteMediaId, remaining);
                } else {
                    handleGalleryImageUploadedLegacyEditor(galleryId, event.mLocalMediaId, event.mRemoteMediaId);
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
            mediaFile.setPostID(getPost().getLocalTablePostId());
            mediaFile.setMediaId(event.mRemoteMediaId);
            mediaFile.setFileURL(event.mRemoteMediaUrl);
            mediaFile.setVideoPressShortCode(event.mSecondaryRemoteMediaId);
            mediaFile.setThumbnailURL(WordPressMediaUtils.getVideoPressVideoPosterFromURL(event.mRemoteMediaUrl));

            mEditorMediaUploadListener.onMediaUploadSucceeded(event.mLocalMediaId, mediaFile);
        }
    }

    public void onEventMainThread(MediaEvents.MediaUploadFailed event) {
        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_FAILED);
        if (mEditorMediaUploadListener != null) {
            if (event.mIsGenericMessage) {
                mEditorMediaUploadListener.onMediaUploadFailed(event.mLocalMediaId, getString(R.string.tap_to_try_again));
            } else {
                mEditorMediaUploadListener.onMediaUploadFailed(event.mLocalMediaId, event.mErrorMessage);
            }
        }
    }

    public void onEventMainThread(MediaEvents.MediaUploadProgress event) {
        if (mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadProgress(event.mLocalMediaId, event.mProgress);
        }
    }

    private void handleGalleryImageUploadedLegacyEditor(Long galleryId, String localId, String remoteId) {
        SpannableStringBuilder postContent;
        if (mEditorFragment.getSpannedContent() != null) {
            // needed by the legacy editor to save local drafts
            postContent = new SpannableStringBuilder(mEditorFragment.getSpannedContent());
        } else {
            postContent = new SpannableStringBuilder(StringUtils.notNullStr((String)
                    mEditorFragment.getContent()));
        }
        int selectionStart = 0;
        int selectionEnd = postContent.length();

        MediaGalleryImageSpan[] gallerySpans = postContent.getSpans(selectionStart, selectionEnd,
                MediaGalleryImageSpan.class);
        if (gallerySpans.length != 0) {
            for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
                MediaGallery gallery = gallerySpan.getMediaGallery();
                if (gallery.getUniqueId() == galleryId) {
                    ArrayList<String> galleryIds = gallery.getIds();
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

    /**
     * Starts {@link org.wordpress.android.ui.media.MediaPickerActivity} after refreshing the blog media.
     */
    private void startMediaSelection() {
        Intent intent = new Intent(this, MediaPickerActivity.class);
        intent.putExtra(MediaPickerActivity.ACTIVITY_TITLE_KEY, getString(R.string.add_to_post));
        intent.putParcelableArrayListExtra(MediaPickerActivity.DEVICE_IMAGE_MEDIA_SOURCES_KEY,
                imageMediaSelectionSources());
        intent.putParcelableArrayListExtra(MediaPickerActivity.DEVICE_VIDEO_MEDIA_SOURCES_KEY,
                videoMediaSelectionSources());
        if (mBlogMediaStatus != 0) {
            intent.putParcelableArrayListExtra(MediaPickerActivity.BLOG_IMAGE_MEDIA_SOURCES_KEY,
                    blogImageMediaSelectionSources());
            intent.putParcelableArrayListExtra(MediaPickerActivity.BLOG_VIDEO_MEDIA_SOURCES_KEY,
                    blogVideoMediaSelectionSources());
        }

        startActivityForResult(intent, MediaPickerActivity.ACTIVITY_REQUEST_CODE_MEDIA_SELECTION);
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
    }

    private void refreshBlogMedia() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            List<Object> apiArgs = new ArrayList<Object>();
            apiArgs.add(WordPress.getCurrentBlog());
            ApiHelper.SyncMediaLibraryTask.Callback callback = new ApiHelper.SyncMediaLibraryTask.Callback() {
                @Override
                public void onSuccess(int count) {
                    mBlogMediaStatus = 1;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mPendingVideoPressInfoRequests != null && !mPendingVideoPressInfoRequests.isEmpty()) {
                                // If there are pending requests for video URLs from VideoPress ids, query the DB for
                                // them again and notify the editor
                                String blogId = String.valueOf(WordPress.currentBlog.getLocalTableBlogId());
                                for (String videoId : mPendingVideoPressInfoRequests) {
                                    String videoUrl = WordPress.wpDB.getMediaUrlByVideoPressId(blogId, videoId);
                                    String posterUrl = WordPressMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

                                    mEditorFragment.setUrlForVideoPressId(videoId, videoUrl, posterUrl);
                                }

                                mPendingVideoPressInfoRequests.clear();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    mBlogMediaStatus = 0;
                    ToastUtils.showToast(EditPostActivity.this, R.string.error_refresh_media, ToastUtils.Duration.SHORT);
                }
            };
            ApiHelper.SyncMediaLibraryTask getMediaTask = new ApiHelper.SyncMediaLibraryTask(0,
                    MediaGridFragment.Filter.ALL, callback);
            getMediaTask.execute(apiArgs);
        } else {
            mBlogMediaStatus = 0;
            ToastUtils.showToast(this, R.string.error_refresh_media, ToastUtils.Duration.SHORT);
        }
    }

    /**
     * Starts the upload service to upload selected media.
     */
    private void startMediaUploadService() {
        if (!mMediaUploadServiceStarted) {
            startService(new Intent(this, MediaUploadService.class));
            mMediaUploadServiceStarted = true;
        }
    }

    /**
     * Stops the upload service.
     */
    private void stopMediaUploadService() {
        if (mMediaUploadServiceStarted) {
            stopService(new Intent(this, MediaUploadService.class));
            mMediaUploadServiceStarted = false;
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
    private MediaFile queueFileForUpload(String path, ArrayList<String> mediaIdOut) {
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

        Blog blog = WordPress.getCurrentBlog();
        long currentTime = System.currentTimeMillis();
        String mimeType = MediaUtils.getMediaFileMimeType(file);
        String fileName = MediaUtils.getMediaFileName(file, mimeType);
        MediaFile mediaFile = new MediaFile();

        mediaFile.setBlogId(String.valueOf(blog.getLocalTableBlogId()));
        mediaFile.setFileName(fileName);
        mediaFile.setFilePath(path);
        mediaFile.setUploadState("queued");
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
            mediaIdOut.add(mediaFile.getMediaId());
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
    public void onMediaRetryClicked(String mediaId) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        WordPress.wpDB.updateMediaUploadState(blogId, mediaId, MediaUploadState.QUEUED);

        MediaUploadService mediaUploadService = MediaUploadService.getInstance();
        if (mediaUploadService == null) {
            startMediaUploadService();
        } else {
            mediaUploadService.processQueue();
        }
        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED);
    }

    @Override
    public void onMediaUploadCancelClicked(String mediaId, boolean delete) {
        MediaUploadService mediaUploadService = MediaUploadService.getInstance();
        if (mediaUploadService != null) {
            mediaUploadService.cancelUpload(mediaId, delete);
        }
    }

    @Override
    public void onFeaturedImageChanged(int mediaId) {
        mPost.setFeaturedImageId(mediaId);
        mEditPostSettingsFragment.updateFeaturedImage(mediaId);
    }

    @Override
    public void onVideoPressInfoRequested(final String videoId) {
        String blogId = String.valueOf(WordPress.currentBlog.getLocalTableBlogId());
        String videoUrl = WordPress.wpDB.getMediaUrlByVideoPressId(blogId, videoId);

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
            }
        }

        String posterUrl = WordPressMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

        mEditorFragment.setUrlForVideoPressId(videoId, videoUrl, posterUrl);
    }

    @Override
    public String onAuthHeaderRequested(String url) {
        String authHeader = "";
        Blog currentBlog = WordPress.getCurrentBlog();
        String token = AccountHelper.getDefaultAccount().getAccessToken();

        if (currentBlog != null && currentBlog.isPrivate() && WPUrlUtils.safeToAddWordPressComAuthToken(url) &&
                !TextUtils.isEmpty(token)) {
            authHeader = "Bearer " + token;
        }
        return authHeader;
    }

    @Override
    public void onEditorFragmentInitialized() {
        fillContentEditorFields();
        // Set the error listener
        if (mEditorFragment instanceof EditorFragment) {
            ((EditorFragment) mEditorFragment).setWebViewErrorListener(new ErrorListener() {
                @Override
                public void onJavaScriptError(String sourceFile, int lineNumber, String message) {
                    CrashlyticsUtils.logException(new Exception(message), ExceptionType.SPECIFIC, T.EDITOR,
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
        WordPress.wpDB.saveMediaFile(mediaFile);
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
}
