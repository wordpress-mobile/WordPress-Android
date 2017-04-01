package org.wordpress.android.ui.posts;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.JavaScriptException;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.AztecEditorFragment;
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
import org.wordpress.android.fluxc.model.MediaModel.UploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon;
import org.wordpress.android.ui.posts.services.AztecImageLoader;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutolinkUtils;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.util.helpers.MediaGalleryImageSpan;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class EditPostActivity extends AppCompatActivity implements EditorFragmentListener, EditorDragAndDropListener,
        ActivityCompat.OnRequestPermissionsResultCallback, EditorWebViewCompatibility.ReflectionFailureListener,
        PhotoPickerFragment.PhotoPickerListener {
    public static final String EXTRA_POST = "postModel";
    public static final String EXTRA_IS_PAGE = "pageEh";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String EXTRA_HAS_CHANGES = "changesEh";
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
    public static final int PHOTO_PICKER_PERMISSION_REQUEST_CODE = 4;

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;

    private static final int AUTOSAVE_INTERVAL_MILLIS = 60000;

    private static final String PHOTO_PICKER_TAG = "photo_picker";

    private Handler mHandler;
    private boolean mShowAztecEditor;
    private boolean mShowNewEditor;

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

    private AztecEditorFragment mAztecEditorFragment;
    private EditorFragmentAbstract mEditorFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditPostPreviewFragment mEditPostPreviewFragment;

    private EditorMediaUploadListener mEditorMediaUploadListener;

    private boolean mNewPostEh;
    private boolean mPageEh;
    private boolean mSetPostContentEh;

    private View mPhotoPickerContainer;
    private PhotoPickerFragment mPhotoPickerFragment;
    private int mPhotoPickerOrientation = Configuration.ORIENTATION_UNDEFINED;

    // For opening the context menu after permissions have been granted
    private View mMenuView = null;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

    private SiteModel mSite;

    // for keeping the media uri while asking for permissions
    private ArrayList<Uri> mDroppedMediaUris;

    private Runnable mFetchMediaRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDroppedMediaUris != null) {
                final List<Uri> mediaUris = mDroppedMediaUris;
                mDroppedMediaUris = null;
                EditPostActivity.this.addAllMedia(mediaUris);
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
        //AppPrefs.setAztecEditorAvailable(true);
        //AppPrefs.setAztecEditorEnabled(true);
        mShowAztecEditor = AppPrefs.aztecEditorEnabledEh();
        mShowNewEditor = AppPrefs.visualEditorEnabledEh();

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
                    mPageEh = extras.getBoolean(EXTRA_IS_PAGE);
                }
                mNewPostEh = true;

                if (mSite == null) {
                    showErrorAndFinish(R.string.blog_not_found);
                    return;
                }
                if (!mSite.isVisible()) {
                    showErrorAndFinish(R.string.error_blog_hidden);
                    return;
                }

                // Create a new post
                List<Long> categories = new ArrayList<>();
                String postFormat = "";
                if (mSite.isWPCom() || mSite.isJetpackConnected()) {
                    // TODO: replace SiteSettingsInterface.getX by calls to mSite.getDefaultCategory
                    // and mSite.getDefaultFormat. We can get these from /me/sites endpoint for .com/jetpack sites.
                    // There might be a way to get that information from a XMLRPC request as well.
                    categories.add((long) SiteSettingsInterface.getDefaultCategory(WordPress.getContext()));
                    postFormat = SiteSettingsInterface.getDefaultFormat(WordPress.getContext());
                }
                mPost = mPostStore.instantiatePostModel(mSite, mPageEh, categories, postFormat);
            } else if (extras != null) {
                // Load post passed in extras
                mPost = (PostModel) extras.getSerializable(EXTRA_POST);
                if (mPost != null) {
                    mOriginalPost = mPost.clone();
                    mPageEh = mPost.pageEh();
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

        if (mSetPostContentEh = mEditorFragment != null) {
            mEditorFragment.setImageLoader(mImageLoader);
        }

        // Ensure we have a valid post
        if (mPost == null) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        if (mNewPostEh) {
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
                    setTitle(mPost.pageEh() ? R.string.page_settings : R.string.post_settings);
                    hidePhotoPicker();
                } else if (position == PAGE_PREVIEW) {
                    setTitle(mPost.pageEh() ? R.string.preview_page : R.string.preview_post);
                    hidePhotoPicker();
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
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mGalleryReceiver);
        } catch (IllegalArgumentException e) {
            AppLog.d(T.EDITOR, "Illegal state! Can't unregister receiver that was no registered");
        }

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // resize the photo picker if the user rotated the device
        int orientation = newConfig.orientation;
        if (orientation != mPhotoPickerOrientation) {
            resizePhotoPicker();
        }
    }

    private String getSaveButtonText() {
        if (!mSite.getHasCapabilityPublishPosts()) {
            return getString(R.string.submit_for_review);
        }

        switch (PostStatus.fromPost(mPost)) {
            case SCHEDULED:
                return getString(R.string.schedule_verb);
            case PUBLISHED:
            case UNKNOWN:
                if (mPost.localDraftEh()) {
                    return getString(R.string.publish_post);
                } else {
                    return getString(R.string.update_verb);
                }
            default:
                if (mPost.localDraftEh()) {
                    return getString(R.string.save);
                } else {
                    return getString(R.string.update_verb);
                }
        }
    }

    private boolean photoPickerShowingEh() {
        return mPhotoPickerContainer != null
                && mPhotoPickerContainer.getVisibility() == View.VISIBLE;
    }

    /*
     * native photo picker is only enabled for the Aztec editor
     */
    private boolean enablePhotoPicker() {
        return mShowAztecEditor;
    }

    /*
     * resizes the photo picker based on device orientation - full height in landscape, half
     * height in portrait
     */
    private void resizePhotoPicker() {
        if (mPhotoPickerContainer == null) return;

        if (DisplayUtils.landscapeEh(this)) {
            mPhotoPickerOrientation = Configuration.ORIENTATION_LANDSCAPE;
            mPhotoPickerContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            mPhotoPickerOrientation = Configuration.ORIENTATION_PORTRAIT;
            int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
            int containerHeight = (int) (displayHeight * 0.5f);
            mPhotoPickerContainer.getLayoutParams().height = containerHeight;
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

        mPhotoPickerFragment = PhotoPickerFragment.newInstance(this);

        getFragmentManager()
                .beginTransaction()
                .add(R.id.photo_fragment_container, mPhotoPickerFragment, PHOTO_PICKER_TAG)
                .commit();
    }

    /*
     * user has requested to show the photo picker
     */
    void showPhotoPicker() {
        // request permissions if we don't already have them
        if (!PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, PHOTO_PICKER_PERMISSION_REQUEST_CODE)) {
            return;
        }

        // make sure we initialized the photo picker
        if (mPhotoPickerFragment == null) {
            initPhotoPicker();
        }

        // hide soft keyboard
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // slide in the photo picker
        if (!photoPickerShowingEh()) {
            AniUtils.animateBottomBar(mPhotoPickerContainer, true, AniUtils.Duration.MEDIUM);
            mPhotoPickerFragment.refresh();
        }

        // fade in the overlay atop the editor, which effectively disables the editor
        // until the picker is closed
        View overlay = findViewById(R.id.view_overlay);
        if (overlay.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(overlay, AniUtils.Duration.MEDIUM);
        }

        if (mAztecEditorFragment != null) {
            mAztecEditorFragment.enableMediaMode(true);
        }
    }

    public void hidePhotoPicker() {
        if (photoPickerShowingEh()) {
            mPhotoPickerFragment.finishActionMode();
            AniUtils.animateBottomBar(mPhotoPickerContainer, false);
        }

        View overlay = findViewById(R.id.view_overlay);
        if (overlay.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(overlay, AniUtils.Duration.MEDIUM);
        }

        if (mAztecEditorFragment != null) {
            mAztecEditorFragment.enableMediaMode(false);
        }
    }

    /*
     * called by PhotoPickerFragment when media is selected - may be a single item or a list of items
     */
    @Override
    public void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList) {
        hidePhotoPicker();
        for (Uri uri: uriList) {
            addMedia(uri);
        }
    }

    /*
     * called by PhotoPickerFragment when user clicks an icon to launch the camera, native
     * picker, or WP media picker
     */
    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerIcon icon) {
        hidePhotoPicker();
        switch (icon) {
            case ANDROID_CAMERA:
                launchCamera();
                break;
            case ANDROID_PICKER:
                launchPictureLibrary();
                break;
            case WP_MEDIA:
                startMediaGalleryAddActivity();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (mShowNewEditor || mShowAztecEditor) {
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
                saveMenuItem.setTitle(getSaveButtonText());
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
                    mEditPostSettingsFragment.searchLocation();

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
            case PHOTO_PICKER_PERMISSION_REQUEST_CODE:
                boolean canShowPhotoPicker = true;
                for (int i = 0; i < grantResults.length; ++i) {
                    switch (permissions[i]) {
                        case Manifest.permission.CAMERA:
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                canShowPhotoPicker = false;
                            }
                            break;
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                canShowPhotoPicker = false;
                            }
                            break;
                    }
                }
                if (canShowPhotoPicker) {
                    showPhotoPicker();
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
                savePostAndFinish();
            }
            return true;
        }

        // Disable format bar buttons while a media upload is in progress
        if (mEditorFragment.uploadingMediaEh() || mEditorFragment.actionInProgressEh()) {
            ToastUtils.showToast(this, R.string.editor_toast_uploading_please_wait, Duration.SHORT);
            return false;
        }

        if (itemId == R.id.menu_save_post) {
            publishPost();
        } else if (itemId == R.id.menu_preview_post) {
            mViewPager.setCurrentItem(PAGE_PREVIEW);
        } else if (itemId == R.id.menu_post_settings) {
            InputMethodManager imm = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            if (mShowNewEditor || mShowAztecEditor) {
                mEditPostSettingsFragment.updateFeaturedImage(mPost.getFeaturedImageId());
            }
            mViewPager.setCurrentItem(PAGE_SETTINGS);
        }
        return false;
    }

    private void savePostOnlineAndFinishAsync(boolean firstTimePublishEh) {
        new SavePostOnlineAndFinishTask(firstTimePublishEh).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean failedMediaEh() {
        // Show an Alert Dialog asking the user if he wants to remove all failed media before upload
        if (mEditorFragment.failedMediaUploadsEh()) {
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
        return false;
    }

    @Override
    public void openContextMenu(View view) {
        // if we're using the native photo picker, ignore the request - if we're not using
        // the photo picker, then this will show the "seven item menu monstrosity"
        if (enablePhotoPicker()) {
            return;
        }
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
        if (DeviceUtils.getInstance().cameraEh(this)) {
            menu.add(0, CAPTURE_PHOTO_MENU_POSITION, 0, getResources().getText(R.string.media_add_popup_capture_photo));
        }
        menu.add(0, SELECT_VIDEO_MENU_POSITION, 0, getResources().getText(R.string.select_video));
        if (DeviceUtils.getInstance().cameraEh(this)) {
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

    private void onUploadSuccess(MediaModel media) {
        if (mEditorMediaUploadListener != null && media != null) {
            mEditorMediaUploadListener.onMediaUploadSucceeded(String.valueOf(media.getId()),
                    FluxCUtils.mediaFileFromMediaModel(media));
        }
        removeMediaFromPendingList(media);
    }

    private void onUploadCanceled(MediaModel media) {
        removeMediaFromPendingList(media);
    }

    private void onUploadError(MediaModel media, MediaStore.MediaError error) {
        String localMediaId = String.valueOf(media.getId());

        Map<String, Object> properties = null;
        MediaFile mf = FluxCUtils.mediaFileFromMediaModel(media);
        if (mf != null) {
            properties = AnalyticsUtils.getMediaProperties(this, mf.videoEh(), null, mf.getFilePath());
            properties.put("error_type", error.type.name());
        }
        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_FAILED, properties);

        // Display custom error depending on error type
        String errorMessage;
        switch (error.type) {
            case AUTHORIZATION_REQUIRED:
                errorMessage = getString(R.string.media_error_no_permission_upload);
                break;
            case REQUEST_TOO_LARGE:
                errorMessage = getString(R.string.media_error_too_large_upload);
                break;
            case GENERIC_ERROR:
            default:
                errorMessage = TextUtils.isEmpty(error.message) ? getString(R.string.tap_to_try_again) : error.message;
        }
        if (mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadFailed(localMediaId, errorMessage);
        }

        removeMediaFromPendingList(media);
    }

    private void onUploadProgress(MediaModel media, float progress) {
        String localMediaId = String.valueOf(media.getId());
        if (mEditorMediaUploadListener != null) {
            mEditorMediaUploadListener.onMediaUploadProgress(localMediaId, progress);
        }
    }

    private void removeMediaFromPendingList(MediaModel mediaToClear) {
        if (mediaToClear == null) {
            return;
        }
        for (MediaModel pendingUpload : mPendingUploads) {
            if (pendingUpload.getId() == mediaToClear.getId()) {
                mPendingUploads.remove(pendingUpload);
                break;
            }
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
        if (EditPostActivity.NEW_MEDIA_POST.equals(
                action)) {
            // Post created from the media library
            normalizedSourceName = "media-library";
        }
        if (intent != null && intent.hasExtra(EXTRA_IS_QUICKPRESS)) {
            // Quick press
            normalizedSourceName = "quick-press";
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
            if (mShowNewEditor || mShowAztecEditor) {
                updatePostContentNewEditor(isAutosave, (String) mEditorFragment.getTitle(),
                        (String) mEditorFragment.getContent());
            } else {
                // TODO: Remove when legacy editor is dropped
                updatePostContent(isAutosave);
            }
        }

        if (mEditPostSettingsFragment != null) {
            mEditPostSettingsFragment.updatePostSettings(mPost);
        }

        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
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

        // update the original post object, so we'll know of new changes
        mOriginalPost = mPost.clone();
    }

    @Override
    public void onBackPressed() {
        if (photoPickerShowingEh()) {
            hidePhotoPicker();
            return;
        }

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
            savePostAndFinish();
        }
    }

    public boolean newPostEh() {
        return mNewPostEh;
    }

    private class SavePostOnlineAndFinishTask extends AsyncTask<Void, Void, Void> {

        boolean firstTimePublishEh;

        SavePostOnlineAndFinishTask(boolean firstTimePublishEh) {
            this.firstTimePublishEh = firstTimePublishEh;
        }

        @Override
        protected Void doInBackground(Void... params) {

            PostUtils.trackSavePostAnalytics(mPost, mSiteStore.getSiteByLocalId(mPost.getLocalSiteId()));

            if (firstTimePublishEh) {
                PostUploadService.addPostToUploadAndTrackAnalytics(mPost);
            } else {
                PostUploadService.addPostToUpload(mPost);
            }
            PostUploadService.setLegacyMode(!mShowNewEditor && !mShowAztecEditor);
            startService(new Intent(EditPostActivity.this, PostUploadService.class));
            PendingDraftsNotificationsUtils.cancelPendingDraftAlarms(EditPostActivity.this, mPost.getId());

            return null;
        }

        @Override
        protected void onPostExecute(Void saved) {
            saveResult(true, false);
            finish();
        }
    }

    private class SavePostLocallyAndFinishTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            if (mOriginalPost != null && !PostUtils.postHasEdits(mOriginalPost, mPost)) {
                // If no changes have been made to the post, set it back to the original - don't save it
                mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mOriginalPost));
                return false;
            } else {
                // Changes have been made - save the post and ask for the post list to refresh
                // We consider this being "manual save", it will replace some Android "spans" by an html
                // or a shortcode replacement (for instance for images and galleries)
                if (mShowNewEditor || mShowAztecEditor) {
                    // Update the post object directly, without re-fetching the fields from the EditorFragment
                    updatePostContentNewEditor(false, mPost.getTitle(), mPost.getContent());
                }

                savePostToDb();

                // now set the pending notification alarm to be triggered in the next day, week, and month
                PendingDraftsNotificationsUtils.scheduleNextNotifications(EditPostActivity.this, mPost);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean saved) {
            saveResult(saved, true);
            finish();
        }
    }

    private void saveResult(boolean saved, boolean savedLocally) {
        Intent i = getIntent();
        i.putExtra(EXTRA_SAVED_AS_LOCAL_DRAFT, savedLocally);
        i.putExtra(EXTRA_IS_PAGE, mPageEh);
        i.putExtra(EXTRA_HAS_CHANGES, saved);
        i.putExtra(EXTRA_POST, mPost);
        setResult(RESULT_OK, i);
    }

    private void publishPost() {

        boolean firstTimePublishEh = firstTimePublishEh();

        boolean postUpdateSuccessful = updatePostObject();
        if (!postUpdateSuccessful) {
            // just return, since the only case updatePostObject() can fail is when the editor
            // fragment is not added to the activity
            return;
        }

        boolean publishableEh = PostUtils.publishableEh(mPost);

        // if post was modified or has unsaved local changes and is publishable, save it
        saveResult(publishableEh, false);

        if (publishableEh) {
            if (NetworkUtils.networkAvailableEh(this)) {
                if (!failedMediaEh()) {
                    savePostOnlineAndFinishAsync(firstTimePublishEh);
                }
            } else {
                savePostLocallyAndFinishAsync();
            }
        } else {
            ToastUtils.showToast(EditPostActivity.this, R.string.error_publish_empty_post, Duration.SHORT);
        }
    }

    private void savePostAndFinish() {

        // check if the opened post had some unsaved local changes
        boolean localChangesEh = mPost.isLocallyChanged() || mPost.localDraftEh();
        boolean firstTimePublishEh = firstTimePublishEh();

        boolean postUpdateSuccessful = updatePostObject();
        if (!postUpdateSuccessful) {
            // just return, since the only case updatePostObject() can fail is when the editor
            // fragment is not added to the activity
            return;
        }

        boolean changesEh = PostUtils.postHasEdits(mOriginalPost, mPost);
        boolean publishableEh = PostUtils.publishableEh(mPost);
        boolean unpublishedLocalDraftChangesEh = PostStatus.fromPost(mPost) == PostStatus.DRAFT &&
                publishableEh && localChangesEh;

        // if post was modified or has unsaved local changes and is publishable, save it
        boolean shouldSave = (changesEh || unpublishedLocalDraftChangesEh) && (publishableEh || !newPostEh());
        saveResult(shouldSave, false);

        if (shouldSave) {
            if (newPostEh()) {
                // new post - user just left the editor without publishing, they probably want
                // to keep the post as a draft
                mPost.setStatus(PostStatus.DRAFT.toString());
                if (mEditPostSettingsFragment != null) {
                    mEditPostSettingsFragment.updateStatusSpinner();
                }
            }

            if (PostStatus.fromPost(mPost) != PostStatus.PUBLISHED && publishableEh && NetworkUtils.networkAvailableEh(this)) {
                if (!failedMediaEh()) {
                    savePostOnlineAndFinishAsync(firstTimePublishEh);
                }
            } else {
                savePostLocallyAndFinishAsync();
            }
        } else {
            // discard post if new & empty
            if (!publishableEh && newPostEh()) {
                mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(mPost));
            }
            finish();
        }
    }

    private boolean firstTimePublishEh() {
        return PostStatus.fromPost(mPost) == PostStatus.PUBLISHED &&
                (mPost.localDraftEh() || PostStatus.fromPost(mOriginalPost) == PostStatus.DRAFT);
    }

    private boolean updatePostObject() {
        try {
            updatePostObject(false);
        } catch (IllegalEditorStateException e) {
            AppLog.e(T.EDITOR, "Impossible to save and publish the post, we weren't able to update it.");
            return false;
        }

        return true;
    }

    private void savePostLocallyAndFinishAsync() {
        new SavePostLocallyAndFinishTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Disable visual editor mode and log the exception if we get a Reflection failure when the webview is being
     * initialized.
     */
    @Override
    public void onReflectionFailure(ReflectionException e) {
        CrashlyticsUtils.logException(e, T.EDITOR, "Reflection Failure on Visual Editor init");
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
                    // TODO: Remove editor options after testing.
                    if (mShowAztecEditor) {
                        mAztecEditorFragment = AztecEditorFragment.newInstance("", "");
                        mAztecEditorFragment.setImageLoader(new AztecImageLoader(getBaseContext()));
                        return mAztecEditorFragment;
                    } else if (mShowNewEditor) {
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
            return ((mShowNewEditor || mShowAztecEditor) ? NUM_PAGES_VISUAL_EDITOR : NUM_PAGES_LEGACY_EDITOR);
        }
    }

    // Moved from EditPostContentFragment
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA_IDS = "NEW_MEDIA_POST_EXTRA_IDS";
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
            MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(media);
            trackAddMediaFromWPLibraryEvents(mediaFile.videoEh(), media.getMediaId());
            String urlToUse = TextUtils.isEmpty(media.getUrl()) ? media.getFilePath() : media.getUrl();
            mEditorFragment.appendMediaFile(mediaFile, urlToUse, mImageLoader);
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
                String stringUri = matcher.group(1);
                Uri uri = Uri.parse(stringUri);
                MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(queueFileForUpload(uri,
                        getContentResolver().getType(uri), UploadState.FAILED));
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
        mEditorFragment.setTitlePlaceholder(getString(mPageEh ? R.string.editor_page_title_placeholder :
                R.string.editor_post_title_placeholder));

        // Set post title and content
        if (mPost != null) {
            if (!TextUtils.isEmpty(mPost.getContent()) && !mSetPostContentEh) {
                mSetPostContentEh = true;
                if (mPost.localDraftEh() && !mShowNewEditor && !mShowAztecEditor) {
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
            // TODO: postSettingsButton.setText(post.pageEh() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setLocalDraft(mPost.localDraftEh());

            mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
        }

        // Special actions
        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            setPostContentFromShareAction();
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
            if (mEditorFragment instanceof LegacyEditorFragment) {
                mEditorFragment.setContent(WPHtml.fromHtml(StringUtils.addPTags(text), this, mPost,
                        getMaximumThumbnailWidthForEditor()));
            } else {
                mEditorFragment.setContent(text);
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

    private void prepareMediaPost() {
        long[] idsArray = getIntent().getLongArrayExtra(NEW_MEDIA_POST_EXTRA_IDS);
        ArrayList<Long> idsList = ListUtils.fromLongArray(idsArray);
        for (Long id: idsList) {
            addExistingMediaToEditor(id);
        }
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
        if (mPost.localDraftEh()) {
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
                    if (mediaFile == null) {
                        continue;
                    }

                    if (mediaFile.getMediaId() != null) {
                        updateMediaFileOnServer(mediaFile);
                    } else {
                        mediaFile.setFileName(wpIS.getImageSource().toString());
                        mediaFile.setFilePath(wpIS.getImageSource().toString());
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

        if (!mPost.localDraftEh()) {
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

        if (!mPost.localDraftEh()) {
            mPost.setIsLocallyChanged(true);
        }

        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
    }

    private void updateMediaFileOnServer(MediaFile mediaFile) {
        if (mediaFile == null) {
            return;
        }

        MediaPayload payload = new MediaPayload(mSite, FluxCUtils.mediaModelFromMediaFile(mediaFile));
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(payload));
    }

    /**
     * Analytics about media from device
     *
     * @param isNew Whether is a fresh media
     * @param videoEh Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    private void trackAddMediaFromDeviceEvents(boolean isNew, boolean videoEh, Uri uri) {
        if (uri == null) {
            AppLog.e(T.MEDIA, "Cannot track new media events if both path and mediaURI are null!!");
            return;
        }

        Map<String, Object> properties = AnalyticsUtils.getMediaProperties(this, videoEh, uri, null);
        Stat currentStat;
        if (videoEh) {
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

        AnalyticsTracker.track(currentStat, properties);
    }

    /**
     * Analytics about media already available in the blog's library.
     *
     * @param videoEh Whether is a video or not
     * @param mediaId The ID of the media in the WP blog's library, or null if device media.
     */
    private void trackAddMediaFromWPLibraryEvents(boolean videoEh, long mediaId) {
        if (mediaId == 0) {
            AppLog.e(T.MEDIA, "Cannot track media events if mediaId is 0");
            return;
        }

        if (videoEh) {
            AnalyticsTracker.track(Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY);
        } else {
            AnalyticsTracker.track(Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY);
        }
    }

    public boolean addMedia(Uri mediaUri) {
        if (mediaUri != null && !MediaUtils.inMediaStoreEh(mediaUri) && !mediaUri.toString().startsWith("/")
                && !mediaUri.toString().startsWith("file://") ) {
            mediaUri = MediaUtils.downloadExternalMedia(this, mediaUri);
        }

        if (mediaUri == null) {
            return false;
        }

        boolean videoEh = MediaUtils.videoEh(mediaUri.toString());

        if (mShowNewEditor || mShowAztecEditor) {
            return addMediaVisualEditor(mediaUri, videoEh);
        } else {
            return addMediaLegacyEditor(mediaUri, videoEh);
        }
    }

    private boolean addMediaVisualEditor(Uri uri, boolean videoEh) {
        String path;
        if (uri.toString().contains("content:")) {
            path = MediaUtils.getPath(this, uri);
        } else {
            // File is not in media library
            path = uri.toString().replace("file://", "");
        }

        if (path == null) {
            ToastUtils.showToast(this, R.string.file_not_found, Duration.SHORT);
            return false;
        }

        Uri optimizedMedia = WPMediaUtils.getOptimizedMedia(this, mSite, path, videoEh);
        if (optimizedMedia != null) {
            uri = optimizedMedia;
        }

        MediaModel media = queueFileForUpload(uri, getContentResolver().getType(uri));
        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(media);
        if (media != null) {
            mEditorFragment.appendMediaFile(mediaFile, path, mImageLoader);
        }

        return true;
    }

    private boolean addMediaLegacyEditor(Uri mediaUri, boolean videoEh) {
        MediaModel mediaModel = buildMediaModel(mediaUri, getContentResolver().getType(mediaUri), UploadState.QUEUED);
        if (videoEh) {
            mediaModel.setTitle(getResources().getString(R.string.video));
        } else {
            mediaModel.setTitle(ImageUtils.getTitleForWPImageSpan(this, mediaUri.getEncodedPath()));
        }
        mediaModel.setPostId(mPost.getId());

        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel));

        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(mediaModel);
        mEditorFragment.appendMediaFile(mediaFile, mediaFile.getFilePath(), mImageLoader);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO || requestCode == RequestCodes.TAKE_VIDEO))) {
            switch (requestCode) {
                case MediaGalleryActivity.REQUEST_CODE:
                    handleMediaGalleryResult(data);
                    // TODO: No analytics here?
                    break;
                case MediaGalleryPickerActivity.REQUEST_CODE:
                    handleMediaGalleryPickerResult(data);
                    // No need to bump analytics here. Bumped later in handleMediaGalleryPickerResult-> addExistingMediaToEditor
                    break;
                case RequestCodes.PICTURE_LIBRARY:
                    Uri imageUri = data.getData();
                    fetchMedia(imageUri);
                    trackAddMediaFromDeviceEvents(false, false, imageUri);
                    break;
                case RequestCodes.TAKE_PHOTO:
                    try {
                        File f = new File(mMediaCapturePath);
                        Uri capturedImageUri = Uri.fromFile(f);
                        if (!addMedia(capturedImageUri)) {
                            ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
                        } else {
                            trackAddMediaFromDeviceEvents(true, false, capturedImageUri);
                        }
                        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                                + Environment.getExternalStorageDirectory())));
                    } catch (RuntimeException e) {
                        AppLog.e(T.POSTS, e);
                    } catch (OutOfMemoryError e) {
                        AppLog.e(T.POSTS, e);
                    }
                    break;
                case RequestCodes.VIDEO_LIBRARY:
                    Uri videoUri = data.getData();
                    List<Uri> mediaUris = Arrays.asList(videoUri);
                    for (Uri mediaUri : mediaUris) {
                        trackAddMediaFromDeviceEvents(false, true, mediaUri);
                    }
                    addAllMedia(mediaUris);
                    break;
                case RequestCodes.TAKE_VIDEO:
                    Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(this);
                    if (!addMedia(capturedVideoUri)) {
                        ToastUtils.showToast(this, R.string.gallery_error, Duration.SHORT);
                    } else {
                        AnalyticsTracker.track(Stat.EDITOR_ADDED_VIDEO_NEW);
                        trackAddMediaFromDeviceEvents(true, true, capturedVideoUri);
                    }
                    break;
            }
        }
    }

    private ArrayList<MediaModel> mPendingUploads = new ArrayList<>();

    private void fetchMedia(Uri mediaUri) {
        if (!MediaUtils.inMediaStoreEh(mediaUri)) {
            // Create an AsyncTask to download the file
            new AsyncTask<Uri, Integer, Uri>() {
                @Override
                protected Uri doInBackground(Uri... uris) {
                    Uri imageUri = uris[0];
                    return MediaUtils.downloadExternalMedia(EditPostActivity.this, imageUri);
                }

                protected void onPostExecute(Uri uri) {
                    if (uri != null) {
                        addMedia(uri);
                    } else {
                        Toast.makeText(EditPostActivity.this, getString(R.string.error_downloading_image),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaUri);
        } else {
            queueFileForUpload(mediaUri, getContentResolver().getType(mediaUri));
        }
    }

    /**
     * Media
     */
    private void addAllMedia(List<Uri> mediaUris) {
        boolean errorAddingMediaEh = false;
        for (Uri mediaUri : mediaUris) {
            if (mediaUri == null || !addMedia(mediaUri)) {
                errorAddingMediaEh = true;
            }
        }
        if (errorAddingMediaEh) {
            Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String path;
        if ("content".equals(uri.getScheme())) {
            path = getRealPathFromContentURI(uri);
        } else if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        } else {
            path = uri.toString();
        }
        return path;
    }

    private String getRealPathFromContentURI(Uri contentUri) {
        if (contentUri == null)
            return null;

        String[] proj = { android.provider.MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();

        if (cursor == null)
            return null;

        int column_index = cursor.getColumnIndex(proj[0]);
        if (column_index == -1) {
            cursor.close();
            return null;
        }

        String path;
        if (cursor.moveToFirst()) {
            path = cursor.getString(column_index);
        } else {
            path = null;
        }

        cursor.close();
        return path;
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

    private void refreshBlogMedia() {
        if (NetworkUtils.networkAvailableEh(this)) {
            FetchMediaListPayload payload = new FetchMediaListPayload(mSite, false);
            mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload));
        } else {
            ToastUtils.showToast(this, R.string.error_media_refresh_no_connection, ToastUtils.Duration.SHORT);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (event.errorEh()) {
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
                    String posterUrl = WordPressMediaUtils.getVideoPressVideoPosterFromURL(videoUrl);

                    mEditorFragment.setUrlForVideoPressId(videoId, videoUrl, posterUrl);
                }

                mPendingVideoPressInfoRequests.clear();
            }
        }
    }

    /**
     * Starts the upload service to upload selected media.
     */
    private void startMediaUploadService() {
        if (mPendingUploads != null && !mPendingUploads.isEmpty()) {
            ArrayList<MediaModel> mediaList = new ArrayList<>();
            for (MediaModel media : mPendingUploads) {
                if (UploadState.QUEUED.name().equals(media.getUploadState())) {
                    mediaList.add(media);
                }
            }
            MediaUploadService.startService(this, mSite, mediaList);
        }
    }

    private String getVideoThumbnail(String videoPath) {
        String thumbnailPath = null;
        try {
            File outputFile = File.createTempFile("thumb", ".png", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath,
                    android.provider.MediaStore.Images.Thumbnails.MINI_KIND);
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
     * Queues a media file for upload and starts the MediaUploadService. Toasts will alert the user
     * if there are issues with the file.
     */
    private MediaModel queueFileForUpload(Uri uri, String mimeType) {
        return queueFileForUpload(uri, mimeType, UploadState.QUEUED);
    }

    private MediaModel queueFileForUpload(Uri uri, String mimeType, UploadState startingState) {
        String path = getRealPathFromURI(uri);

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

        MediaModel media = buildMediaModel(uri, mimeType, startingState);
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        mPendingUploads.add(media);
        startMediaUploadService();

        return media;
    }

    private MediaModel buildMediaModel(Uri uri, String mimeType, UploadState startingState) {
        String path = getRealPathFromURI(uri);

        MediaModel media = mMediaStore.instantiateMediaModel();
        AppLog.i(T.MEDIA, "New media instantiated localId=" + media.getId());
        String filename = org.wordpress.android.fluxc.utils.MediaUtils.getFileName(path);
        String fileExtension = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(path);

        // Try to get mimetype if none was passed to this method
        if (mimeType == null) {
            mimeType = getContentResolver().getType(uri);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            }
            if (mimeType == null) {
                // Default to image jpeg
                mimeType = "image/jpeg";
            }
        }
        // If file extension is null, upload won't work on wordpress.com
        if (fileExtension == null) {
            fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            filename += "." + fileExtension;
        }

        if (org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(mimeType)) {
            media.setThumbnailUrl(getVideoThumbnail(path));
        }

        media.setFileName(filename);
        media.setFilePath(path);
        media.setLocalSiteId(mSite.getId());
        media.setFileExtension(fileExtension);
        media.setMimeType(mimeType);
        media.setUploadState(startingState.name());
        media.setUploadDate(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));

        return media;
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
        if (enablePhotoPicker()) {
            if (!photoPickerShowingEh()) {
                showPhotoPicker();
            } else {
                hidePhotoPicker();
            }
        }
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
        MediaModel media = null;

        List<MediaModel> localMediaList = mMediaStore.getLocalSiteMedia(mSite);
        for (MediaModel localMedia : localMediaList) {
            if (String.valueOf(localMedia.getId()).equals(mediaId)) {
                media = localMedia;
                break;
            }
        }

        if (media != null) {
            media.setUploadState(UploadState.QUEUED.name());
            mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            mPendingUploads.add(media);
            startMediaUploadService();
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
        if (mSite.privateEh() && WPUrlUtils.safeToAddWordPressComAuthToken(url)
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
                            T.EDITOR,
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

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        if (isFinishing()) return;
        // event for unknown media, ignoring
        if (event.media == null) {
            AppLog.w(AppLog.T.MEDIA, "Media event not recognized: " + event.media);
            return;
        }

        if (event.errorEh()) {
            onUploadError(event.media, event.error);
        }
        else
        if (event.canceled) {
            onUploadCanceled(event.media);
        }
        else
        if (event.completed) {
            onUploadSuccess(event.media);
        }
        else {
            onUploadProgress(event.media, event.progress);
        }
    }

}
