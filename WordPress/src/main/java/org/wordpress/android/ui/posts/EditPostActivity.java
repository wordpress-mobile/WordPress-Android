package org.wordpress.android.ui.posts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.SuggestionSpan;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;

import com.android.volley.toolbox.ImageLoader;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.JavaScriptException;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.EditorBetaClickListener;
import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.editor.EditorFragment.IllegalEditorStateException;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorDragAndDropListener;
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener;
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent;
import org.wordpress.android.editor.EditorFragmentActivity;
import org.wordpress.android.editor.EditorMediaUploadListener;
import org.wordpress.android.editor.EditorWebViewAbstract.ErrorListener;
import org.wordpress.android.editor.EditorWebViewCompatibility;
import org.wordpress.android.editor.EditorWebViewCompatibility.ReflectionException;
import org.wordpress.android.editor.ImageSettingsDialogFragment;
import org.wordpress.android.editor.LegacyEditorFragment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
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
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerOption;
import org.wordpress.android.ui.posts.InsertMediaDialog.InsertMediaCallback;
import org.wordpress.android.ui.posts.services.AztecImageLoader;
import org.wordpress.android.ui.posts.services.AztecVideoLoader;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.ReleaseNotesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.uploads.PostEvents;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.VideoOptimizer;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutolinkUtils;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.DateTimeUtils;
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
import org.wordpress.android.util.WPPermissionUtils;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class EditPostActivity extends AppCompatActivity implements
        EditorFragmentActivity,
        EditorBetaClickListener,
        EditorDragAndDropListener,
        EditorFragmentListener,
        EditorWebViewCompatibility.ReflectionFailureListener,
        OnRequestPermissionsResultCallback,
        PhotoPickerFragment.PhotoPickerListener,
        EditPostSettingsFragment.EditPostActivityHook {

    public static final String EXTRA_POST_LOCAL_ID = "postModelLocalId";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_PROMO = "isPromo";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String EXTRA_HAS_FAILED_MEDIA = "hasFailedMedia";
    public static final String EXTRA_HAS_CHANGES = "hasChanges";
    private static final String STATE_KEY_CURRENT_POST = "stateKeyCurrentPost";
    private static final String STATE_KEY_ORIGINAL_POST = "stateKeyOriginalPost";
    private static final String STATE_KEY_EDITOR_FRAGMENT = "editorFragment";
    private static final String STATE_KEY_DROPPED_MEDIA_URIS = "stateKeyDroppedMediaUri";

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;

    private static final int AUTOSAVE_INTERVAL_MILLIS = 60000;

    private static final String PHOTO_PICKER_TAG = "photo_picker";
    private static final String ASYNC_PROMO_DIALOG_TAG = "async_promo";

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

    private EditorFragmentAbstract mEditorFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditPostPreviewFragment mEditPostPreviewFragment;

    private EditorMediaUploadListener mEditorMediaUploadListener;

    private boolean mIsNewPost;
    private boolean mIsPage;
    private boolean mIsPromo;
    private boolean mHasSetPostContent;

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
        mShowAztecEditor = AppPrefs.isAztecEditorEnabled();
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
                    mIsPromo = extras.getBoolean(EXTRA_IS_PROMO);
                }
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
                List<Long> categories = new ArrayList<>();
                String postFormat = "";
                if (mSite.isWPCom() || mSite.isJetpackConnected()) {
                    // TODO: replace SiteSettingsInterface.getX by calls to mSite.getDefaultCategory
                    // and mSite.getDefaultFormat. We can get these from /me/sites endpoint for .com/jetpack sites.
                    // There might be a way to get that information from a XMLRPC request as well.
                    categories.add((long) SiteSettingsInterface.getDefaultCategory(WordPress.getContext()));
                    postFormat = SiteSettingsInterface.getDefaultFormat(WordPress.getContext());
                }
                mPost = mPostStore.instantiatePostModel(mSite, mIsPage, categories, postFormat);
                mPost.setStatus(PostStatus.PUBLISHED.toString());
            } else if (extras != null) {
                // Load post passed in extras
                mPost = mPostStore.getPostByLocalPostId(extras.getInt(EXTRA_POST_LOCAL_ID));
                if (mPost != null) {
                    mOriginalPost = mPost.clone();
                    mPost = UploadService.updatePostWithCurrentlyCompletedUploads(mPost);
                    mIsPage = mPost.isPage();
                }
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
            mEditorFragment.setImageLoader(mImageLoader);
        }

        // Ensure we have a valid post
        if (mPost == null) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        if (mIsNewPost) {
            trackEditorCreatedPost(action, getIntent());
        } else {
            resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued();
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
                    hidePhotoPicker();
                } else if (position == PAGE_PREVIEW) {
                    setTitle(mPost.isPage() ? R.string.preview_page : R.string.preview_post);
                    hidePhotoPicker();
                    savePostAsync(new AfterSavePostListener() {
                        @Override
                        public void onPostSave() {
                            if (mEditPostPreviewFragment != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mEditPostPreviewFragment != null) {
                                            mEditPostPreviewFragment.loadPost(mPost);
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

        // now check if the newcontent still has items marked as failed. If it does,
        // then hook this post up to our error list, so it can be queried by the Posts List later
        // and be shown properly to the user
        if (AztecEditorFragment.hasMediaItemsMarkedFailed(this, newContent)) {
            UploadService.markPostAsError(mPost);
        } else {
            UploadService.removeUploadErrorForPost(mPost);
        }

        if (!TextUtils.isEmpty(oldContent) && newContent != null && oldContent.compareTo(newContent) != 0) {
            mPost.setContent(newContent);

            // we changed the post, so let’s mark this down
            if (!mPost.isLocalDraft()) {
                mPost.setIsLocallyChanged(true);
            }
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }
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

        EventBus.getDefault().register(this);

        if (mEditorMediaUploadListener != null) {
            List<MediaModel> uploadingMediaInPost = UploadService.getPendingMediaForPost(mPost);
            for (MediaModel media : uploadingMediaInPost) {
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

        mHandler.removeCallbacks(mAutoSave);
        mHandler = null;

        EventBus.getDefault().unregister(this);
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

        // If we're showing the Async promo dialog, we need to notify it to take the new orientation into account
        PromoDialog fragment = (PromoDialog) getSupportFragmentManager().findFragmentByTag(ASYNC_PROMO_DIALOG_TAG);
        if (fragment != null) {
            fragment.redrawForOrientationChange();
        }
    }

    @Override
    public void onBetaClicked() {
        ActivityLauncher.showAztecEditorReleaseNotes(this);
        AnalyticsTracker.track(Stat.EDITOR_AZTEC_BETA_LABEL);
    }

    private String getSaveButtonText() {
        if (!userCanPublishPosts()) {
            return getString(R.string.submit_for_review);
        }

        switch (PostStatus.fromPost(mPost)) {
            case SCHEDULED:
                return getString(R.string.schedule_verb);
            case PUBLISHED:
            case UNKNOWN:
                if (mPost.isLocalDraft()) {
                    return getString(R.string.post_status_publish_post);
                } else {
                    return getString(R.string.update_verb);
                }
            default:
                if (mPost.isLocalDraft()) {
                    return getString(R.string.save);
                } else {
                    return getString(R.string.update_verb);
                }
        }
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
        if (mPhotoPickerContainer == null) return;

        if (DisplayUtils.isLandscape(this)) {
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

        EnumSet<PhotoPickerOption> options =
                EnumSet.of(PhotoPickerOption.ALLOW_MULTI_SELECT);
        mPhotoPickerFragment = PhotoPickerFragment.newInstance(this, options);

        getFragmentManager()
                .beginTransaction()
                .add(R.id.photo_fragment_container, mPhotoPickerFragment, PHOTO_PICKER_TAG)
                .commit();
    }

    /*
     * user has requested to show the photo picker
     */
    private void showPhotoPicker() {
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
        if (!isPhotoPickerShowing()) {
            AniUtils.animateBottomBar(mPhotoPickerContainer, true, AniUtils.Duration.MEDIUM);
            mPhotoPickerFragment.refresh();
            mPhotoPickerFragment.setPhotoPickerListener(this);
        }

        // fade in the overlay atop the editor, which effectively disables the editor
        // until the picker is closed
        View overlay = findViewById(R.id.view_overlay);
        if (overlay.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(overlay, AniUtils.Duration.MEDIUM);
        }

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment)mEditorFragment).enableMediaMode(true);
        }
    }

    private void hidePhotoPicker() {
        if (isPhotoPickerShowing()) {
            mPhotoPickerFragment.finishActionMode();
            mPhotoPickerFragment.setPhotoPickerListener(null);
            AniUtils.animateBottomBar(mPhotoPickerContainer, false);
        }

        View overlay = findViewById(R.id.view_overlay);
        if (overlay.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(overlay, AniUtils.Duration.MEDIUM);
        }

        if (mEditorFragment instanceof AztecEditorFragment) {
            ((AztecEditorFragment)mEditorFragment).enableMediaMode(false);
        }
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
                WPMediaUtils.advertiseImageOptimization(this,
                        new WPMediaUtils.OnAdvertiseImageOptimizationListener() {
                            @Override
                            public void done() {
                                for (Uri uri: uriList) {
                                    if (addMedia(uri)) {
                                        boolean isVideo = MediaUtils.isVideo(uri.toString());
                                        trackAddMediaFromDeviceEvents(false, isVideo, uri);
                                    }
                                }
                            }
                        });
                return;
            }
        }

        for (Uri uri: uriList) {
            if (addMedia(uri)) {
                boolean isVideo = MediaUtils.isVideo(uri.toString());
                trackAddMediaFromDeviceEvents(false, isVideo, uri);
            }
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
                ActivityLauncher.viewMediaPickerForResult(this, mSite);
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
        Fragment fragment = getFragmentManager().findFragmentByTag(
                ImageSettingsDialogFragment.IMAGE_SETTINGS_DIALOG_TAG);
        if (fragment != null && fragment.isVisible()) {
            if (fragment instanceof  ImageSettingsDialogFragment) {
                ImageSettingsDialogFragment imFragment = (ImageSettingsDialogFragment) fragment;
                imFragment.dismissFragment();
            }
            return false;
        }
        if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
            if (mViewPager.getCurrentItem() == PAGE_SETTINGS) {
                mEditorFragment.setFeaturedImageId(mPost.getFeaturedImageId());
            }
            mViewPager.setCurrentItem(PAGE_CONTENT);
            invalidateOptionsMenu();
        } else {
            savePostAndFinish();
        }
        return true;
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        hidePhotoPicker();

        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            return handleBackPressed();
        }

        if (itemId == R.id.menu_save_post) {
            if (!AppPrefs.isAsyncPromoRequired()) {
                publishPost();
            } else {
                showAsyncPromoDialog();
            }
        } else {
            // Disable other action bar buttons while a media upload is in progress
            // (unnecessary for Aztec since it supports progress reattachment)
            if (!mShowAztecEditor && (mEditorFragment.isUploadingMedia() || mEditorFragment.isActionInProgress())) {
                ToastUtils.showToast(this, R.string.editor_toast_uploading_please_wait, Duration.SHORT);
                return false;
            }

            if (itemId == R.id.menu_preview_post) {
                mViewPager.setCurrentItem(PAGE_PREVIEW);
            } else if (itemId == R.id.menu_post_settings) {
                InputMethodManager imm = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                if (mEditPostSettingsFragment != null) {
                    mEditPostSettingsFragment.refreshViews();
                }
                mViewPager.setCurrentItem(PAGE_SETTINGS);
            }
        }
        return false;
    }

    private void savePostOnlineAndFinishAsync(boolean isFirstTimePublish) {
        new SavePostOnlineAndFinishTask(isFirstTimePublish).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        WPMediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoLibrary() {
        WPMediaUtils.launchVideoLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoCamera() {
        WPMediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
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
        boolean postTitleOrContentChanged = false;
        if (mEditorFragment != null) {
            if (mShowNewEditor || mShowAztecEditor) {
                postTitleOrContentChanged =
                        updatePostContentNewEditor(isAutosave, (String) mEditorFragment.getTitle(),
                        (String) mEditorFragment.getContent());
            } else {
                // TODO: Remove when legacy editor is dropped
                postTitleOrContentChanged = updatePostContent(isAutosave);
            }
        }

        // only makes sense to change the publish date and locallychanged date if the Post was actaully changed
        if (postTitleOrContentChanged) {
            PostUtils.updatePublishDateIfShouldBePublishedImmediately(mPost);
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }
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

    @Override
    public void initializeEditorFragment() {
        if (mEditorFragment instanceof AztecEditorFragment) {
            AztecEditorFragment aztecEditorFragment = (AztecEditorFragment)mEditorFragment;
            aztecEditorFragment.setEditorBetaClickListener(EditPostActivity.this);
            aztecEditorFragment.setAztecImageLoader(new AztecImageLoader(getBaseContext()));
            aztecEditorFragment.setAztecVideoLoader(new AztecVideoLoader(getBaseContext()));
        }
    }

    private void cancelMediaUpload(int localMediaId) {
        MediaModel mediaModel = mMediaStore.getMediaWithLocalId(localMediaId);
        if (mediaModel != null) {
            CancelMediaPayload payload = new CancelMediaPayload(mSite, mediaModel);
            mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
        }
    }

    private void notifyMediaDeleted(int localMediaId) {
        MediaModel mediaModel = mMediaStore.getMediaWithLocalId(localMediaId);
        if (mediaModel != null) {
            EventBus.getDefault().post(new PostEvents.PostMediaDeleted(mPost, mediaModel));
        }
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
        if (isPhotoPickerShowing()) {
            hidePhotoPicker();
            return;
        }

        handleBackPressed();
    }

    private boolean isNewPost() {
        return mIsNewPost;
    }

    /*
     * returns true if the user has permission to publish the post - assumed to be true for
     * dot.org sites because we can't retrieve their capabilities
     */
    private boolean userCanPublishPosts() {
        if (SiteUtils.isAccessedViaWPComRest(mSite)) {
            return mSite.getHasCapabilityPublishPosts();
        } else {
            return true;
        }
    }

    private class SavePostOnlineAndFinishTask extends AsyncTask<Void, Void, Void> {

        boolean isFirstTimePublish;

        SavePostOnlineAndFinishTask(boolean isFirstTimePublish) {
            this.isFirstTimePublish = isFirstTimePublish;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // mark as pending if the user doesn't have publishing rights
            if (!userCanPublishPosts()) {
               if (PostStatus.fromPost(mPost) != PostStatus.DRAFT && PostStatus.fromPost(mPost) != PostStatus.PENDING) {
                   mPost.setStatus(PostStatus.PENDING.toString());
               }
            }

            savePostToDb();
            PostUtils.trackSavePostAnalytics(mPost, mSiteStore.getSiteByLocalId(mPost.getLocalSiteId()));

            UploadService.setLegacyMode(!mShowNewEditor && !mShowAztecEditor);
            if (isFirstTimePublish) {
                UploadService.uploadPostAndTrackAnalytics(EditPostActivity.this, mPost);
            } else {
                UploadService.uploadPost(EditPostActivity.this, mPost);
            }

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
        i.putExtra(EXTRA_HAS_FAILED_MEDIA, hasFailedMedia());
        i.putExtra(EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EXTRA_HAS_CHANGES, saved);
        i.putExtra(EXTRA_POST_LOCAL_ID, mPost.getId());
        setResult(RESULT_OK, i);
    }

    private void publishPost() {
        AccountModel account = mAccountStore.getAccount();
        // prompt user to verify e-mail before publishing
        if (!account.getEmailVerified()) {
            String message = TextUtils.isEmpty(account.getEmail())
                    ? getString(R.string.editor_confirm_email_prompt_message)
                    : String.format(getString(R.string.editor_confirm_email_prompt_message_with_email), account.getEmail());

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.editor_confirm_email_prompt_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    ToastUtils.showToast(EditPostActivity.this, getString(R.string.toast_saving_post_as_draft));
                                    savePostAndFinish();
                                }
                            })
                    .setNegativeButton(R.string.editor_confirm_email_prompt_negative,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mDispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction());
                                }
                            });
            builder.create().show();
            return;
        }

        boolean postUpdateSuccessful = updatePostObject();
        if (!postUpdateSuccessful) {
            // just return, since the only case updatePostObject() can fail is when the editor
            // fragment is not added to the activity
            return;
        }

        // Update post, save to db and publish in its own Thread, because 1. update can be pretty slow with a lot of
        // text 2. better not to call `updatePostObject()` from the UI thread due to weird thread blocking behavior
        // on API 16 (and 21) with the visual editor.
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isFirstTimePublish = isFirstTimePublish();

                boolean postUpdateSuccessful = updatePostObject();
                if (!postUpdateSuccessful) {
                    // just return, since the only case updatePostObject() can fail is when the editor
                    // fragment is not added to the activity
                    return;
                }

                boolean isPublishable = PostUtils.isPublishable(mPost);

                // if post was modified or has unsaved local changes and is publishable, save it
                saveResult(isPublishable, false);

                if (isPublishable) {
                    if (NetworkUtils.isNetworkAvailable(getBaseContext())) {
                        // Show an Alert Dialog asking the user if they want to remove all failed media before upload
                        if (mEditorFragment.hasFailedMediaUploads()) {
                            EditPostActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showRemoveFailedUploadsDialog();
                                }
                            });
                        } else {
                            savePostOnlineAndFinishAsync(isFirstTimePublish);
                        }
                    } else {
                        savePostLocallyAndFinishAsync();
                    }
                } else {
                    EditPostActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showToast(EditPostActivity.this, R.string.error_publish_empty_post, Duration.SHORT);
                        }
                    });
                }
            }
        }).start();
    }

    private void showRemoveFailedUploadsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.editor_toast_failed_uploads)
                .setPositiveButton(R.string.editor_remove_failed_uploads, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Clear failed uploads
                        mEditorFragment.removeAllFailedMediaUploads();
                    }
                }).setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void savePostAndFinish() {
        // Update post, save to db and post online in its own Thread, because 1. update can be pretty slow with a lot of
        // text 2. better not to call `updatePostObject()` from the UI thread due to weird thread blocking behavior
        // on API 16 (and 21) with the visual editor.
        new Thread(new Runnable() {
            @Override
            public void run() {
                // check if the opened post had some unsaved local changes
                boolean hasLocalChanges = mPost.isLocallyChanged() || mPost.isLocalDraft();
                boolean isFirstTimePublish = isFirstTimePublish();

                boolean postUpdateSuccessful = updatePostObject();
                if (!postUpdateSuccessful) {
                    // just return, since the only case updatePostObject() can fail is when the editor
                    // fragment is not added to the activity
                    return;
                }

                boolean hasChanges = PostUtils.postHasEdits(mOriginalPost, mPost);
                boolean isPublishable = PostUtils.isPublishable(mPost);
                boolean hasUnpublishedLocalDraftChanges = PostStatus.fromPost(mPost) == PostStatus.DRAFT &&
                        isPublishable && hasLocalChanges;

                // if post was modified or has unpublished local changes, save it
                boolean shouldSave = (mOriginalPost != null && hasChanges)
                        || hasUnpublishedLocalDraftChanges || (isPublishable && isNewPost());
                // if post is publishable or not new, sync it
                boolean shouldSync = isPublishable || !isNewPost();

                saveResult(shouldSave && shouldSync, false);

                if (shouldSave) {
                    if (isNewPost()) {
                        // new post - user just left the editor without publishing, they probably want
                        // to keep the post as a draft
                        mPost.setStatus(PostStatus.DRAFT.toString());
                        if (mEditPostSettingsFragment != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mEditPostSettingsFragment.updatePostStatusRelatedViews();
                                }
                            });
                        }
                    }

                    if (PostStatus.fromPost(mPost) == PostStatus.DRAFT && isPublishable && !hasFailedMedia()
                            && NetworkUtils.isNetworkAvailable(getBaseContext())) {
                        savePostOnlineAndFinishAsync(isFirstTimePublish);
                    } else {
                        savePostLocallyAndFinishAsync();
                    }
                } else {
                    // discard post if new & empty
                    if (!isPublishable && isNewPost()) {
                        mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(mPost));
                    }
                    finish();
                }
            }
        }).start();
    }

    private boolean isFirstTimePublish() {
        return (PostStatus.fromPost(mPost) == PostStatus.UNKNOWN || PostStatus.fromPost(mPost) == PostStatus.PUBLISHED) &&
                (mPost.isLocalDraft() || PostStatus.fromPost(mOriginalPost) == PostStatus.DRAFT);
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

                        // Show confirmation message when coming from editor promotion dialog.
                        if (mIsPromo) {
                            showSnackbarConfirmation();
                        // Show open beta message when Aztec is already enabled.
                        } else if (AppPrefs.isAztecEditorEnabled() && AppPrefs.isNewEditorBetaRequired()) {
                            showSnackbarBeta();
                        }

                        return AztecEditorFragment.newInstance("", "", AppPrefs.isAztecEditorToolbarExpanded());
                    } else if (mShowNewEditor) {
                        EditorWebViewCompatibility.setReflectionFailureListener(EditPostActivity.this);
                        return new EditorFragment();
                    } else {
                        return new LegacyEditorFragment();
                    }
                case 1:
                    return EditPostSettingsFragment.newInstance();
                default:
                    return EditPostPreviewFragment.newInstance(mSite);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mEditorFragment = (EditorFragmentAbstract) fragment;
                    mEditorFragment.setImageLoader(mImageLoader);

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
            trackAddMediaFromWPLibraryEvents(mediaFile.isVideo(), media.getMediaId());
            String urlToUse = TextUtils.isEmpty(media.getUrl()) ? media.getFilePath() : media.getUrl();
            appendMediaFileAndSavePost(mediaFile, urlToUse, mImageLoader);
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
                        getContentResolver().getType(uri), MediaUploadState.FAILED));
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
        mEditorFragment.setTitlePlaceholder(getString(mIsPage ? R.string.editor_page_title_placeholder :
                R.string.editor_post_title_placeholder));

        // Set post title and content
        if (mPost != null) {
            if (!TextUtils.isEmpty(mPost.getContent()) && !mHasSetPostContent) {
                mHasSetPostContent = true;
                if (mPost.isLocalDraft() && !mShowNewEditor && !mShowAztecEditor) {
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
        } else if (NEW_MEDIA_POST.equals(action)) {
            prepareMediaPost();
        }
    }

    private void launchCamera() {
        WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                new WPMediaUtils.LaunchCameraCallback() {
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
    public boolean updatePostContent(boolean isAutoSave) throws IllegalEditorStateException {
        if (mPost == null) {
            return false;
        }
        String title = StringUtils.notNullStr((String) mEditorFragment.getTitle());
        SpannableStringBuilder postContent;
        if (mEditorFragment.getSpannedContent() != null) {
            // needed by the legacy editor to save local drafts
            try {
                postContent = new SpannableStringBuilder(mEditorFragment.getSpannedContent());
            } catch (RuntimeException e) {
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

        boolean titleChanged = PostUtils.updatePostTitleIfDifferent(mPost, title);
        boolean contentChanged = PostUtils.updatePostContentIfDifferent(mPost, content);

        if (!mPost.isLocalDraft() && (titleChanged || contentChanged)) {
            mPost.setIsLocallyChanged(true);
        }

        return titleChanged || contentChanged;
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
        boolean contentChanged = PostUtils.updatePostContentIfDifferent(mPost, content);

        if (!mPost.isLocalDraft() && (titleChanged || contentChanged)) {
            mPost.setIsLocallyChanged(true);
            mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        }

        return titleChanged || contentChanged;
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
     *
     * @param isVideo Whether is a video or not
     * @param mediaId The ID of the media in the WP blog's library, or null if device media.
     */
    private void trackAddMediaFromWPLibraryEvents(boolean isVideo, long mediaId) {
        if (mediaId == 0) {
            AppLog.e(T.MEDIA, "Cannot track media events if mediaId is 0");
            return;
        }

        if (isVideo) {
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY, mSite,  null);
        } else {
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY, mSite,  null);
        }
    }

    public boolean addMedia(Uri mediaUri) {
        if (mediaUri != null && !MediaUtils.isInMediaStore(mediaUri) && !mediaUri.toString().startsWith("/")
                && !mediaUri.toString().startsWith("file://") ) {
            mediaUri = MediaUtils.downloadExternalMedia(this, mediaUri);
        }

        if (mediaUri == null) {
            return false;
        }

        boolean isVideo = MediaUtils.isVideo(mediaUri.toString());

        if (mShowNewEditor || mShowAztecEditor) {
            return addMediaVisualEditor(mediaUri, isVideo);
        } else {
            return addMediaLegacyEditor(mediaUri, isVideo);
        }
    }

    private boolean addMediaVisualEditor(Uri uri, boolean isVideo) {
        String path = MediaUtils.getRealPathFromURI(this, uri);

        if (path == null) {
            ToastUtils.showToast(this, R.string.file_not_found, Duration.SHORT);
            return false;
        }

        Uri optimizedMedia = WPMediaUtils.getOptimizedMedia(this, path, isVideo);
        if (optimizedMedia != null) {
            uri = optimizedMedia;
            path = MediaUtils.getRealPathFromURI(this, uri);
        } else {
            // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
            if (!mSite.isWPCom()) {
                // If it's not wpcom we must rotate the picture locally
                Uri rotatedMedia = WPMediaUtils.fixOrientationIssue(this, path, isVideo);
                if (rotatedMedia != null) {
                    uri = rotatedMedia;
                    path = MediaUtils.getRealPathFromURI(this, uri);
                }
            } else {
                // It's a wpcom site. Just create a version of the picture rotated for the old visual editor
                // All the other editors read EXIF data
                if (mShowNewEditor) {
                    Uri rotatedMedia = WPMediaUtils.fixOrientationIssue(this, path, isVideo);
                    if (rotatedMedia != null) {
                        // The uri variable should remain the same since wpcom rotates the picture server side
                        path = MediaUtils.getRealPathFromURI(this, rotatedMedia);
                    }
                }
            }
        }

        MediaModel media = queueFileForUpload(uri, getContentResolver().getType(uri));
        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(media);
        if (media != null) {
            appendMediaFileAndSavePost(mediaFile, path, mImageLoader);
        }

        return true;
    }

    private void appendMediaFileAndSavePost(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader) {
        mEditorFragment.appendMediaFile(mediaFile, imageUrl, mImageLoader);
        // after asking the Editor to append the Media File, save the Post object to the DB
        // as we need to keep the new modifications made to the Post's body (that is, the inserted media)
        savePostAsync(null);
    }

    private boolean addMediaLegacyEditor(Uri mediaUri, boolean isVideo) {
        MediaModel mediaModel = buildMediaModel(mediaUri, getContentResolver().getType(mediaUri),
                MediaUploadState.QUEUED);
        if (isVideo) {
            mediaModel.setTitle(getResources().getString(R.string.video));
        } else {
            mediaModel.setTitle(ImageUtils.getTitleForWPImageSpan(this, mediaUri.getEncodedPath()));
        }
        mediaModel.setLocalPostId(mPost.getId());

        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel));

        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(mediaModel);
        appendMediaFileAndSavePost(mediaFile, mediaFile.getFilePath(), mImageLoader);
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
                case RequestCodes.MULTI_SELECT_MEDIA_PICKER:
                    handleMediaPickerResult(data);
                    // No need to bump analytics here. Bumped later in handleMediaPickerResult-> addExistingMediaToEditor
                    break;
                case RequestCodes.PICTURE_LIBRARY:
                    final Uri imageUri = data.getData();
                    if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
                        WPMediaUtils.advertiseImageOptimization(this,
                                new WPMediaUtils.OnAdvertiseImageOptimizationListener() {
                                    @Override
                                    public void done() {
                                        fetchMedia(imageUri);
                                        trackAddMediaFromDeviceEvents(false, false, imageUri);
                                    }
                                });
                    } else {
                        fetchMedia(imageUri);
                        trackAddMediaFromDeviceEvents(false, false, imageUri);
                    }
                    break;
                case RequestCodes.TAKE_PHOTO:
                    if (WPMediaUtils.shouldAdvertiseImageOptimization(this)) {
                        WPMediaUtils.advertiseImageOptimization(this,
                                new WPMediaUtils.OnAdvertiseImageOptimizationListener() {
                                    @Override
                                    public void done() {
                                        addLastTakenPicture();
                                    }
                                });
                    } else {
                        addLastTakenPicture();
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

    private void addLastTakenPicture() {
        try {
            WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
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
    }

    private ArrayList<MediaModel> mPendingUploads = new ArrayList<>();

    private void fetchMedia(Uri mediaUri) {
        if (!MediaUtils.isInMediaStore(mediaUri)) {
            // Do not download the file in async task. See https://github.com/wordpress-mobile/WordPress-Android/issues/5818
            Uri downloadedUri = null;
            try {
                downloadedUri = MediaUtils.downloadExternalMedia(EditPostActivity.this, mediaUri);
            } catch (IllegalStateException e) {
                // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                AppLog.e(AppLog.T.UTILS, "Can't download the image at: " + mediaUri.toString(), e);
                CrashlyticsUtils.logException(e, AppLog.T.MEDIA, "Can't download the image at: " + mediaUri.toString() +
                        " See issue #5823");
            }
            if (downloadedUri != null) {
                addMedia(downloadedUri);
            } else {
                ToastUtils.showToast(EditPostActivity.this, R.string.error_downloading_image,
                        ToastUtils.Duration.SHORT);
            }
        } else {
            addMedia(mediaUri);
        }
    }

    /**
     * Media
     */
    private void addAllMedia(List<Uri> mediaUris) {
        boolean isErrorAddingMedia = false;
        for (Uri mediaUri : mediaUris) {
            if (mediaUri == null || !addMedia(mediaUri)) {
                isErrorAddingMedia = true;
            }
        }
        if (isErrorAddingMedia) {
            ToastUtils.showToast(EditPostActivity.this, R.string.gallery_error, ToastUtils.Duration.SHORT);
        }
    }

    private void handleMediaPickerResult(Intent data) {
        ArrayList<Long> ids = ListUtils.fromLongArray(data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS));
        if (ids == null || ids.size() == 0) {
            return;
        }

        // if only one item was chosen insert it as a media object, otherwise show the insert
        // media dialog so the user can choose how to insert the items
        if (ids.size() == 1) {
            long mediaId = ids.get(0);
            addExistingMediaToEditor(mediaId);
        } else {
            showInsertMediaDialog(ids);
        }
    }

    /*
     * called after user selects multiple photos from WP media library
     */
    private void showInsertMediaDialog(final ArrayList<Long> mediaIds) {
        InsertMediaCallback callback = new InsertMediaCallback() {
            @Override
            public void onCompleted(@NonNull InsertMediaDialog dialog) {
                switch (dialog.getInsertType()) {
                    case GALLERY:
                        MediaGallery gallery = new MediaGallery();
                        gallery.setType(dialog.getGalleryType().toString());
                        gallery.setNumColumns(dialog.getNumColumns());
                        gallery.setIds(mediaIds);
                        mEditorFragment.appendGallery(gallery);
                        break;
                    case INDIVIDUALLY:
                        for (Long id: mediaIds) {
                            addExistingMediaToEditor(id);
                        }
                        break;
                }
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
    private void startUploadService() {
        if (mPendingUploads != null && !mPendingUploads.isEmpty()) {
            final ArrayList<MediaModel> mediaList = new ArrayList<>();
            for (MediaModel media : mPendingUploads) {
                if (MediaUploadState.QUEUED.toString().equals(media.getUploadState())) {
                    mediaList.add(media);
                }
            }

            if (!mediaList.isEmpty()) {
                // before starting the service, we need to update the posts' contents so we are sure the service
                // can retrieve it from there on
                hidePhotoPicker();
                savePostAsync(new AfterSavePostListener() {
                    @Override
                    public void onPostSave() {
                        UploadService.uploadMedia(EditPostActivity.this, mediaList);
                    }
                });

            }
        }
    }

    private String getVideoThumbnail(String videoPath) {
        String thumbnailPath = null;
        try {
            File outputFile = File.createTempFile("thumb", ".png", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            Bitmap thumb = ImageUtils.getVideoFrameFromVideo(
                    videoPath,
                    ImageUtils.getMaximumThumbnailWidthForEditor(this)
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
        media.setLocalPostId(mPost.getId());
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));

        // add this item to the queue - we keep it for visual aid atm
        mPendingUploads.add(media);

        startUploadService();

        return media;
    }

    private MediaModel buildMediaModel(Uri uri, String mimeType, MediaUploadState startingState) {
        String path = MediaUtils.getRealPathFromURI(this, uri);

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
        media.setUploadState(startingState);
        media.setUploadDate(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));
        if (!mPost.isLocalDraft()) {
            media.setPostId(mPost.getRemotePostId());
        }

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
        if (isPhotoPickerShowing()) {
            hidePhotoPicker();
        } else if (WPMediaUtils.currentUserCanUploadMedia(mSite)) {
            showPhotoPicker();
        } else {
            // show the WP media library instead of the photo picker if the user doesn't have upload permission
            ActivityLauncher.viewMediaPickerForResult(this, mSite);
        }
    }

    @Override
    public void onMediaDropped(final ArrayList<Uri> mediaUris) {
        mDroppedMediaUris = mediaUris;
        if (PermissionUtils.checkAndRequestStoragePermission(this, WPPermissionUtils.EDITOR_DRAG_DROP_PERMISSION_REQUEST_CODE)) {
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
        if (TextUtils.isEmpty(mediaId)) {
            AppLog.e(T.MEDIA, "Invalid media id passed to onMediaRetryClicked");
            return;
        }
        MediaModel media = mMediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId));
        if (media == null) {
            AppLog.e(T.MEDIA, "Can't find media with local id: " + mediaId);
            return;
        }

        if (media.getUploadState().equals(MediaUploadState.UPLOADED.toString())) {
            // Note: we should actually do this when the editor fragment starts instead of waiting for user input.
            // Notify the editor fragment upload was successful and it should replace the local url by the remote url.
            if (mEditorMediaUploadListener != null) {
                mEditorMediaUploadListener.onMediaUploadSucceeded(String.valueOf(media.getId()),
                        FluxCUtils.mediaFileFromMediaModel(media));
            }
        } else {
            media.setUploadState(MediaUploadState.QUEUED);
            mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
            mPendingUploads.add(media);
            startUploadService();
        }

        AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED);
    }

    @Override
    public void onMediaUploadCancelClicked(String localMediaId) {
        if (!TextUtils.isEmpty(localMediaId)) {
            cancelMediaUpload(StringUtils.stringToInt(localMediaId));
        } else {
            // Passed mediaId is incorrect: cancel all uploads for this post
            ToastUtils.showToast(this, getString(R.string.error_all_media_upload_canceled));
            EventBus.getDefault().post(new PostEvents.PostMediaCanceled(mPost));
        }
    }

    @Override
    public void onMediaDeleted(String localMediaId) {
        if (!TextUtils.isEmpty(localMediaId)) {
            notifyMediaDeleted(StringUtils.stringToInt(localMediaId));
        } else {
            AppLog.w(AppLog.T.MEDIA, "onMediaDeleted event carries null localMediaId, not recognized");
        }
    }

    @Override
    public void onUndoMediaCheck(final String undoedContent) {
        // here we check which elements tagged UPLOADING are there in undoedContent,
        // and check for the ones that ARE NOT being uploaded or queued in the UploadService.
        // These are the CANCELED ONES, so mark them FAILED now to retry.

        List <MediaModel> currentlyUploadingMedia = UploadService.getAllInProgressOrQueuedMediaItemsForPost(mPost);
        List<String> mediaMarkedUploading  = AztecEditorFragment.getMediaMarkedUploadingInPostContent(EditPostActivity.this, undoedContent);

        // go through the list of items marked UPLOADING within the Post content, and look in the UploadService
        // to see whether they're really being uploaded or not. If an item is not really being uploaded, mark that item failed
        for (String mediaId : mediaMarkedUploading) {
            boolean found = false;
            for (MediaModel media : currentlyUploadingMedia) {
                if (Integer.valueOf(mediaId) == media.getId()) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // 2- Run through loop and set each media to failed
                if (mEditorFragment instanceof AztecEditorFragment) {
                    ((AztecEditorFragment)mEditorFragment).setMediaToFailed(mediaId);
                }
            }
        }

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
            if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, WPPermissionUtils.EDITOR_MEDIA_PERMISSION_REQUEST_CODE)) {
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
            case BOLD_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_BOLD);
                break;
            case BLOCKQUOTE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_BLOCKQUOTE);
                break;
            case ELLIPSIS_COLLAPSE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ELLIPSIS_COLLAPSE);
                AppPrefs.setAztecEditorToolbarExpanded(false);
                break;
            case ELLIPSIS_EXPAND_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ELLIPSIS_EXPAND);
                AppPrefs.setAztecEditorToolbarExpanded(true);
                break;
            case HEADING_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING);
                break;
            case HEADING_1_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING_1);
                break;
            case HEADING_2_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING_2);
                break;
            case HEADING_3_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING_3);
                break;
            case HEADING_4_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING_4);
                break;
            case HEADING_5_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING_5);
                break;
            case HEADING_6_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HEADING_6);
                break;
            case HORIZONTAL_RULE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HORIZONTAL_RULE);
                break;
            case HTML_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_HTML);
                hidePhotoPicker();
                break;
            case IMAGE_EDITED:
                AnalyticsTracker.track(Stat.EDITOR_EDITED_IMAGE);
                break;
            case ITALIC_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ITALIC);
                break;
            case LINK_ADDED_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LINK_ADDED);
                hidePhotoPicker();
                break;
            case LINK_REMOVED_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LINK_REMOVED);
                break;
            case LIST_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LIST);
                break;
            case LIST_ORDERED_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LIST_ORDERED);
                break;
            case LIST_UNORDERED_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LIST_UNORDERED);
                break;
            case MEDIA_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_IMAGE);
                break;
            case NEXT_PAGE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_NEXT_PAGE);
                break;
            case PARAGRAPH_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_PARAGRAPH);
                break;
            case PREFORMAT_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_PREFORMAT);
                break;
            case READ_MORE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_READ_MORE);
                break;
            case STRIKETHROUGH_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_STRIKETHROUGH);
                break;
            case UNDERLINE_BUTTON_TAPPED:
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_UNDERLINE);
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
            AppLog.w(AppLog.T.MEDIA, "Media event carries null media object, not recognized");
            return;
        }

        if (event.isError()) {
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

    @SuppressWarnings("unused")
    public void onEventMainThread(VideoOptimizer.ProgressEvent event) {
        if (!isFinishing()) {
            // use upload progress rather than optimizer progress since the former includes upload+optimization
            float progress = UploadService.getUploadProgressForMedia(event.media);
            onUploadProgress(event.media, progress);
        }
    }

    protected void showSnackbarBeta() {
        Snackbar.make(
                mViewPager,
                getString(R.string.new_editor_beta_message),
                Snackbar.LENGTH_LONG
        )
                .setAction(
                        R.string.new_editor_beta_action,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityLauncher.showAztecEditorReleaseNotes(EditPostActivity.this);
                                AnalyticsTracker.track(Stat.EDITOR_AZTEC_BETA_LINK);
                            }
                        }
                )
                .show();
        AppPrefs.setNewEditorBetaRequired(false);
    }

    protected void showSnackbarConfirmation() {
        if (mViewPager != null) {
            Snackbar.make(
                    mViewPager,
                    getString(R.string.new_editor_promo_confirmation_message),
                    Snackbar.LENGTH_LONG
            )
                    .setAction(
                            R.string.new_editor_promo_confirmation_action,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ActivityLauncher.viewAppSettings(EditPostActivity.this);
                                    finish();
                                }
                            }
                    )
                    .show();
        }
    }

    private void showAsyncPromoDialog() {
        PromoDialogAdvanced asyncPromoDialog = new PromoDialogAdvanced.Builder(
                R.drawable.img_promo_async,
                R.string.async_promo_title,
                R.string.async_promo_description,
                android.R.string.ok)
                .setLinkText(R.string.async_promo_link)
                .build();

        asyncPromoDialog.setLinkOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditPostActivity.this, ReleaseNotesActivity.class);
                intent.putExtra(ReleaseNotesActivity.KEY_TARGET_URL,
                        "https://make.wordpress.org/mobile/whats-new-in-android-media-uploading/");
                startActivity(intent);
            }
        });

        asyncPromoDialog.setPositiveButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishPost();
            }
        });

        asyncPromoDialog.show(getSupportFragmentManager(), ASYNC_PROMO_DIALOG_TAG);
        AppPrefs.setAsyncPromoRequired(false);
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

}
