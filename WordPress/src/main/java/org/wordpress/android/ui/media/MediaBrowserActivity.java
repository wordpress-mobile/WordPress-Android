package org.wordpress.android.ui.media;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaGridFragment.MediaFilter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * The main activity in which the user can browse their media.
 */
public class MediaBrowserActivity extends AppCompatActivity implements MediaGridListener,
        OnQueryTextListener, OnActionExpandListener,
        WPMediaUtils.LaunchCameraCallback {

    public enum MediaBrowserType {
        BROWSER,                              // browse & manage media
        MULTI_SELECT_IMAGE_AND_VIDEO_PICKER,  // select multiple images or videos
        SINGLE_SELECT_IMAGE_PICKER;           // select a single image

        public boolean isPicker() {
            return this == MULTI_SELECT_IMAGE_AND_VIDEO_PICKER || this == SINGLE_SELECT_IMAGE_PICKER;
        }
    }

    public static final String ARG_BROWSER_TYPE = "media_browser_type";
    public static final String ARG_FILTER = "filter";
    public static final String RESULT_IDS = "result_ids";

    private static final String SAVED_QUERY = "SAVED_QUERY";
    private static final String BUNDLE_MEDIA_CAPTURE_PATH = "mediaCapturePath";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private SiteModel mSite;

    private MediaGridFragment mMediaGridFragment;
    private PopupWindow mAddMediaPopup;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private Menu mMenu;
    private TabLayout mTabLayout;

    private MediaDeleteService.MediaDeleteBinder mDeleteService;
    private boolean mDeleteServiceBound;

    private String mQuery;
    private String mMediaCapturePath;
    private MediaBrowserType mBrowserType;
    private int mLastAddMediaItemClickedPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((WordPress) getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mBrowserType = (MediaBrowserType) getIntent().getSerializableExtra(ARG_BROWSER_TYPE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mBrowserType = (MediaBrowserType) savedInstanceState.getSerializable(ARG_BROWSER_TYPE);
            mMediaCapturePath = savedInstanceState.getString(BUNDLE_MEDIA_CAPTURE_PATH);
            mQuery = savedInstanceState.getString(SAVED_QUERY);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        if (mBrowserType == null) {
            // default to browser mode if missing type
            AppLog.w(AppLog.T.MEDIA, "MediaBrowserType is null. Defaulting to MediaBrowserType.BROWSER mode.");
            mBrowserType = MediaBrowserType.BROWSER;
        }

        setContentView(R.layout.media_browser_activity);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);

        // if media was shared add it to the library
        handleSharedMedia();

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        setupTabs();

        MediaFilter filter;
        if (mBrowserType == MediaBrowserType.SINGLE_SELECT_IMAGE_PICKER) {
            filter = MediaFilter.FILTER_IMAGES;
        } else if (savedInstanceState != null) {
            filter = (MediaFilter) savedInstanceState.getSerializable(ARG_FILTER);
        } else {
            filter = MediaFilter.FILTER_ALL;
        }

        mMediaGridFragment = (MediaGridFragment) fm.findFragmentByTag(MediaGridFragment.TAG);
        if (mMediaGridFragment == null) {
            mMediaGridFragment = MediaGridFragment.newInstance(mSite, mBrowserType, filter);
            getFragmentManager().beginTransaction()
                    .replace(R.id.media_browser_container, mMediaGridFragment, MediaGridFragment.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else {
            setFilter(filter);
        }
    }

    /*
     * only show tabs when being used as a media browser rather than a media picker
     */
    private boolean shouldShowTabs() {
        return mBrowserType == MediaBrowserType.BROWSER;
    }

    public void enableTabs(boolean enable) {
        if (!shouldShowTabs()) return;

        if (enable && mTabLayout.getVisibility() != View.VISIBLE) {
            AniUtils.fadeIn(mTabLayout, AniUtils.Duration.MEDIUM);
        } else if (!enable && mTabLayout.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mTabLayout, AniUtils.Duration.MEDIUM, View.INVISIBLE);
        }
    }

    private void setupTabs() {
        if (shouldShowTabs()) {
            int normalColor = ContextCompat.getColor(this, R.color.blue_light);
            int selectedColor = ContextCompat.getColor(this, R.color.white);
            mTabLayout.setTabTextColors(normalColor, selectedColor);

            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_all));         // FILTER_ALL
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_images));      // FILTER_IMAGES
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_documents));   // FILTER_DOCUMENTS
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_videos));      // FILTER_VIDEOS
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.media_audio));       // FILTER_AUDIO

            mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    setFilter(getFilterForPosition(tab.getPosition()));
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // noop
                }
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    setFilter(getFilterForPosition(tab.getPosition()));
                }
            });

            // tabMode is set to scrollable in layout, set to fixed if there's enough space to show them all
            mTabLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mTabLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if (mTabLayout.getChildCount() > 0) {
                        int tabLayoutWidth = 0;
                        LinearLayout tabFirstChild = (LinearLayout) mTabLayout.getChildAt(0);
                        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                            LinearLayout tabView = (LinearLayout) (tabFirstChild.getChildAt(i));
                            tabLayoutWidth += (tabView.getMeasuredWidth() + tabView.getPaddingLeft() + tabView.getPaddingRight());
                        }

                        int displayWidth = DisplayUtils.getDisplayPixelWidth(MediaBrowserActivity.this);
                        if (tabLayoutWidth < displayWidth) {
                            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
                            mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
                        }
                    }
                }
            });
        } else {
            mTabLayout.setVisibility(View.GONE);
        }
    }

    private int getPositionForFilter(@NonNull MediaFilter filter) {
        return filter.getValue();
    }

    private MediaFilter getFilterForPosition(int position) {
        for (MediaFilter filter: MediaFilter.values()) {
            if (filter.getValue() == position) {
                return filter;
            }
        }
        return MediaFilter.FILTER_ALL;
    }

    private void setFilter(@NonNull MediaFilter filter) {
        int position = getPositionForFilter(filter);
        if (shouldShowTabs()
                && mTabLayout != null
                && mTabLayout.getSelectedTabPosition() != position) {
            mTabLayout.setScrollPosition(position, 0f, true);
        }

        if (mMediaGridFragment != null &&
                (mMediaGridFragment.getFilter() != filter || mMediaGridFragment.isEmpty())) {
            mMediaGridFragment.setFilter(filter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(mReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mDispatcher.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchMenuItem != null) {
            String tempQuery = mQuery;
            MenuItemCompat.collapseActionView(mSearchMenuItem);
            mQuery = tempQuery;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService(null);
        ActivityId.trackLastActivity(ActivityId.MEDIA);
    }

    @Override
    public void onStop() {
        unregisterReceiver(mReceiver);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindDeleteService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_QUERY, mQuery);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(ARG_BROWSER_TYPE, mBrowserType);
        if (mMediaGridFragment != null) {
            outState.putSerializable(ARG_FILTER, mMediaGridFragment.getFilter());
        }
        if (!TextUtils.isEmpty(mMediaCapturePath)) {
            outState.putString(BUNDLE_MEDIA_CAPTURE_PATH, mMediaCapturePath);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.PICTURE_LIBRARY:
            case RequestCodes.VIDEO_LIBRARY:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri imageUri = data.getData();
                    String mimeType = getContentResolver().getType(imageUri);
                    fetchMedia(imageUri, mimeType);
                    trackAddMediaFromDeviceEvents(
                            false,
                            requestCode == RequestCodes.VIDEO_LIBRARY,
                            imageUri
                    );
                }
                break;
            case RequestCodes.TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
                    Uri uri = getOptimizedPictureIfNecessary(Uri.parse(mMediaCapturePath));
                    mMediaCapturePath = null;
                    queueFileForUpload(uri, getContentResolver().getType(uri));
                    trackAddMediaFromDeviceEvents(true, false, uri);
                }
                break;
            case RequestCodes.TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = MediaUtils.getLastRecordedVideoUri(this);
                    queueFileForUpload(uri, getContentResolver().getType(uri));
                    trackAddMediaFromDeviceEvents(true, true, uri);
                }
                break;
        }
    }

    /**
     * Analytics about new media
     *
     * @param isNewMedia Whether is a fresh (just taken) photo/video or not
     * @param isVideo Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    private void trackAddMediaFromDeviceEvents(boolean isNewMedia, boolean isVideo, Uri uri) {
        if (uri == null) {
            AppLog.e(AppLog.T.MEDIA, "Cannot track new media event if mediaURI is null!!");
            return;
        }

        Map<String, Object> properties = AnalyticsUtils.getMediaProperties(this, isVideo, uri, null);
        properties.put("via", isNewMedia ? "device_camera" : "device_library");

        if (isVideo) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_LIBRARY_ADDED_VIDEO, properties);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_LIBRARY_ADDED_PHOTO, properties);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] results) {
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, results, true);

        if (allGranted && requestCode == WPPermissionUtils.MEDIA_BROWSER_PERMISSION_REQUEST_CODE) {
            doAddMediaItemClicked(mLastAddMediaItemClickedPosition);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.media_browser, menu);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, this);

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        // open search bar if we were searching for something before
        if (!TextUtils.isEmpty(mQuery) && mMediaGridFragment != null && mMediaGridFragment.isVisible()) {
            String tempQuery = mQuery; //temporary hold onto query
            MenuItemCompat.expandActionView(mSearchMenuItem); //this will reset mQuery
            onQueryTextSubmit(tempQuery);
            mSearchView.setQuery(mQuery, true);
        }

        // hide "add media" if this is used as a media picker or the user doesn't have upload permission
        if (mBrowserType.isPicker() || !WPMediaUtils.currentUserCanUploadMedia(mSite)) {
            menu.findItem(R.id.menu_new_media).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (mAddMediaPopup != null && mAddMediaPopup.isShowing()) {
            mAddMediaPopup.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_new_media:
                if (isAddMediaPopupShowing()) {
                    hideAddMediaPopup();
                } else {
                    showAddMediaPopup();
                }
                return true;
            case R.id.menu_search:
                mSearchMenuItem = item;
                MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, this);
                MenuItemCompat.expandActionView(mSearchMenuItem);

                mSearchView = (SearchView) item.getActionView();
                mSearchView.setOnQueryTextListener(this);

                // load last saved query
                if (!TextUtils.isEmpty(mQuery)) {
                    onQueryTextSubmit(mQuery);
                    mSearchView.setQuery(mQuery, true);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mMenu.findItem(R.id.menu_new_media).setVisible(false);

        enableTabs(false);

        // load last search query
        if (!TextUtils.isEmpty(mQuery)) {
            onQueryTextChange(mQuery);
        }

        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mMenu.findItem(R.id.menu_new_media).setVisible(true);
        invalidateOptionsMenu();

        enableTabs(true);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.search(query);
        }

        mQuery = query;
        mSearchView.clearFocus();

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.search(newText);
        }

        mQuery = newText;

        return true;
    }

    @Override
    public void onMediaItemSelected(View sourceView, int localMediaId) {
        MediaModel media = mMediaStore.getMediaWithLocalId(localMediaId);
        if (media == null) {
            AppLog.w(AppLog.T.MEDIA, "Media browser > unable to load localMediaId = " + localMediaId);
            ToastUtils.showToast(this, R.string.error_media_load);
            return;
        }

        // if this is being used as a media picker return the selected item and finish, otherwise
        // preview the selected item
        if (mBrowserType.isPicker()) {
            Intent intent = new Intent();
            ArrayList<Long> remoteMediaIds = new ArrayList<>();
            remoteMediaIds.add(media.getMediaId());
            intent.putExtra(RESULT_IDS, ListUtils.toLongArray(remoteMediaIds));
            setResult(RESULT_OK, intent);
            finish();
        } else {
            // TODO: right now only images & videos are supported
            String mimeType = StringUtils.notNullStr(media.getMimeType()).toLowerCase();
            if (mimeType.startsWith("image") || mimeType.startsWith("video")) {
                if (media.getUploadState() != null &&
                        MediaUtils.isLocalFile(media.getUploadState().toLowerCase())) {
                    // Show the simple preview in case of uploading items. i.e: No metadata info, and other options only available
                    // for files already on the remote site.
                    MediaPreviewActivity.showPreview(this, sourceView, media.getFilePath(), mimeType.startsWith("video"));
                } else {
                    MediaPreviewActivity.showPreview(this, sourceView, mSite, localMediaId);
                }
            }
        }
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mMediaCapturePath = mediaCapturePath;
    }

    @Override
    public void onRetryUpload(int localMediaId) {
        MediaModel media = mMediaStore.getMediaWithLocalId(localMediaId);
        if (media == null) {
            ToastUtils.showToast(this, R.string.file_not_found, ToastUtils.Duration.SHORT);
            return;
        }
        addMediaToUploadService(media);
    }

    private void showMediaToastError(@StringRes int message, @Nullable String messageDetail) {
        if (isFinishing()) {
            return;
        }
        String errorMessage = getString(message);
        if (!TextUtils.isEmpty(messageDetail)) {
            errorMessage += ". " + messageDetail;
        }
        ToastUtils.showToast(this, errorMessage, ToastUtils.Duration.LONG);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        AppLog.d(AppLog.T.MEDIA, "MediaBrowser onMediaChanged > " + event.cause);

        if (event.isError()) {
            AppLog.w(AppLog.T.MEDIA, "Received onMediaChanged error: " + event.error.type
                    + " - " + event.error.message);
            showMediaToastError(R.string.media_generic_error, event.error.message);
            return;
        }

        switch (event.cause) {
            case DELETE_MEDIA:
                if (event.mediaList != null) {
                    // If the media was deleted, remove it from multi select if it was selected
                    for (MediaModel mediaModel : event.mediaList) {
                        int localMediaId = mediaModel.getId();
                        mMediaGridFragment.removeFromMultiSelect(localMediaId);
                    }
                }
                break;
        }

        if (event.mediaList != null && event.mediaList.size() == 1) {
            updateMediaGridItem(event.mediaList.get(0), true);
        } else {
            reloadMediaGrid();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.isError()) {
            AppLog.d(AppLog.T.MEDIA, "Received onMediaUploaded error:" + event.error.type
                    + " - " + event.error.message);
            String errorMessage = WPMediaUtils.getErrorMessage(this, event.media, event.error);
            if (errorMessage != null) {
                ToastUtils.showToast(this, errorMessage, ToastUtils.Duration.LONG);
            } else {
                showMediaToastError(R.string.media_upload_error, event.error.message);
            }
        } else if (event.completed) {
            String title = "";
            if (event.media != null) {
                title = event.media.getTitle();
            }
            AppLog.d(AppLog.T.MEDIA, "<" + title + "> upload complete");
        }

        if (event.media != null) {
            updateMediaGridItem(event.media, event.isError());
        } else {
            reloadMediaGrid();
        }
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        enableTabs(false);
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        enableTabs(true);
    }

    public void deleteMedia(final ArrayList<Integer> ids) {
        final ArrayList<MediaModel> mediaToDelete = new ArrayList<>();
        int processedItemCount = 0;

        for (int currentId : ids) {
            MediaModel mediaModel = mMediaStore.getMediaWithLocalId(currentId);
            if (mediaModel == null) {
                continue;
            }

            // if uploading, first issue a cancel upload command
            if (UploadService.isPendingOrInProgressMediaUpload(mediaModel)) {
                MediaStore.CancelMediaPayload payload = new MediaStore.CancelMediaPayload(mSite, mediaModel, false);
                mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
            }

            if (mediaModel.getUploadState() != null &&
                    MediaUtils.isLocalFile(mediaModel.getUploadState().toLowerCase())) {
                mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel));
            } else {
                mediaToDelete.add(mediaModel);
            }
            processedItemCount++;
        }

        if (processedItemCount != ids.size()) {
            ToastUtils.showToast(this, R.string.cannot_delete_multi_media_items, ToastUtils.Duration.LONG);
        }

        // mark items for delete without actually deleting items yet,
        // and then refresh the grid
        if (!mediaToDelete.isEmpty()) {
            startMediaDeleteService(mediaToDelete);
        }
        if (mMediaGridFragment != null) {
            mMediaGridFragment.clearSelection();
        }
    }

    private void uploadList(List<Uri> uriList) {
        if (uriList == null || uriList.size() == 0) {
            return;
        }
        for (Uri uri : uriList) {
            if (uri != null) {
                fetchMedia(uri, getContentResolver().getType(uri));
            }
        }
    }

    private final OnBackStackChangedListener mOnBackStackChangedListener = new OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            ActivityUtils.hideKeyboard(MediaBrowserActivity.this);
        }
    };

    private void doBindDeleteService(Intent intent) {
        mDeleteServiceBound = bindService(intent, mDeleteConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
    }

    private void doUnbindDeleteService() {
        if (mDeleteServiceBound) {
            unbindService(mDeleteConnection);
            mDeleteServiceBound = false;
        }
    }

    private final ServiceConnection mDeleteConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDeleteService = (MediaDeleteService.MediaDeleteBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDeleteService = null;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                // Coming from zero connection. Continue what's pending for delete
                if (mMediaStore.hasSiteMediaToDelete(mSite)) {
                    startMediaDeleteService(null);
                }
            }
        }
    };

    /** Setup the popup that allows you to add new media from camera, video camera or local files **/
    private void createAddMediaPopup() {
        String[] items = new String[]{
                getString(R.string.photo_picker_capture_photo),
                getString(R.string.photo_picker_capture_video),
                getString(R.string.photo_picker_choose_photo),
                getString(R.string.photo_picker_choose_video)
        };

        @SuppressLint("InflateParams")
        View menuView = getLayoutInflater().inflate(R.layout.actionbar_add_media, null, false);
        ListView listView = (ListView) menuView.findViewById(R.id.actionbar_add_media_listview);
        listView.setAdapter(new ArrayAdapter<>(this, R.layout.actionbar_add_media_cell, items));
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doAddMediaItemClicked(position);
                mAddMediaPopup.dismiss();
            }
        });

        int width = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_width);
        mAddMediaPopup = new PopupWindow(menuView, width, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mAddMediaPopup.setFocusable(true);
    }

    private boolean isAddMediaPopupShowing() {
        return mAddMediaPopup != null && mAddMediaPopup.isShowing();
    }

    private void hideAddMediaPopup() {
        if (mAddMediaPopup != null) {
            mAddMediaPopup.dismiss();
        }
    }

    private void showAddMediaPopup() {
        if (mAddMediaPopup == null) {
            createAddMediaPopup();
        }

        View view = findViewById(R.id.menu_new_media);
        if (view != null) {
            int y_offset = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_y_offset);
            int[] loc = new int[2];
            view.getLocationOnScreen(loc);
            mAddMediaPopup.showAtLocation(
                    view,
                    Gravity.TOP | Gravity.START,
                    loc[0],
                    loc[1] + view.getHeight() + y_offset);
        } else {
            // In case menu button is not on screen (declared showAsAction="ifRoom"), center the popup in the view.
            View gridView = findViewById(R.id.recycler);
            mAddMediaPopup.showAtLocation(gridView, Gravity.CENTER, 0, 0);
        }
    }

    private static final int ITEM_CAPTURE_PHOTO = 0;
    private static final int ITEM_CAPTURE_VIDEO = 1;
    private static final int ITEM_CHOOSE_PHOTO  = 2;
    private static final int ITEM_CHOOSE_VIDEO  = 3;

    private void doAddMediaItemClicked(int position) {
        mLastAddMediaItemClickedPosition = position;

        String[] permissions;
        if (position == ITEM_CAPTURE_PHOTO || position == ITEM_CAPTURE_VIDEO) {
            permissions = new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        } else {
            permissions = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };
        }
        if (PermissionUtils.checkAndRequestPermissions(
                this, WPPermissionUtils.MEDIA_BROWSER_PERMISSION_REQUEST_CODE, permissions)) {
            switch (position) {
                case ITEM_CAPTURE_PHOTO:
                    WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID, this);
                    break;
                case ITEM_CAPTURE_VIDEO:
                    WPMediaUtils.launchVideoCamera(this);
                    break;
                case ITEM_CHOOSE_PHOTO:
                    WPMediaUtils.launchPictureLibrary(this);
                    break;
                case ITEM_CHOOSE_VIDEO:
                    WPMediaUtils.launchVideoLibrary(this);
                    break;
            }
        }
    }

    private void fetchMedia(Uri mediaUri, final String mimeType) {
        if (!MediaUtils.isInMediaStore(mediaUri)) {
            // Do not download the file in async task. See https://github.com/wordpress-mobile/WordPress-Android/issues/5818
            Uri downloadedUri = null;
            try {
                downloadedUri = MediaUtils.downloadExternalMedia(MediaBrowserActivity.this, mediaUri);
            } catch (IllegalStateException e) {
                // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                AppLog.e(AppLog.T.UTILS, "Can't download the image at: " + mediaUri.toString(), e);
                CrashlyticsUtils.logException(e, AppLog.T.MEDIA, "Can't download the image at: " + mediaUri.toString() +
                        " See issue #5823");
            }
            if (downloadedUri != null) {
                queueFileForUpload(getOptimizedPictureIfNecessary(downloadedUri), mimeType);
            } else {
                ToastUtils.showToast(MediaBrowserActivity.this, R.string.error_downloading_image,
                        ToastUtils.Duration.SHORT);
            }
        } else {
            queueFileForUpload(getOptimizedPictureIfNecessary(mediaUri), mimeType);
        }
    }

    private Uri getOptimizedPictureIfNecessary(Uri originalUri) {
        String filePath = MediaUtils.getRealPathFromURI(this, originalUri);
        if (TextUtils.isEmpty(filePath)) {
            return originalUri;
        }
        Uri optimizedMedia = WPMediaUtils.getOptimizedMedia(this, filePath, false);
        if (optimizedMedia != null) {
            return optimizedMedia;
        } else {
            // Optimization is OFF. Make sure the picture is in portrait for .org site
            // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
            if (!mSite.isWPCom()) {
                // If it's not wpcom we must rotate the picture locally
                Uri rotatedMedia = WPMediaUtils.fixOrientationIssue(this, filePath, false);
                if (rotatedMedia != null) {
                    return rotatedMedia;
                }
            }
        }

        return originalUri;
    }

    private void addMediaToUploadService(@NonNull MediaModel media) {
        // Start the upload service if it's not started and fill the media queue
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start UploadService, internet connection required.");
            ToastUtils.showToast(this, R.string.no_network_message, ToastUtils.Duration.SHORT);
            return;
        }

        ArrayList<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        UploadService.uploadMedia(this, mediaList);
    }

    private void queueFileForUpload(Uri uri, String mimeType) {
        // It is a regular local media file
        String path = MediaUtils.getRealPathFromURI(this,uri);

        if (TextUtils.isEmpty(path)) {
            ToastUtils.showToast(this, R.string.file_not_found, ToastUtils.Duration.SHORT);
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            return;
        }

        MediaModel media = mMediaStore.instantiateMediaModel();
        String filename = org.wordpress.android.fluxc.utils.MediaUtils.getFileName(path);
        String fileExtension = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(path);

        // Try to get mime type if none was passed to this method
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

        media.setFileName(filename);
        media.setTitle(filename);
        media.setFilePath(path);
        media.setLocalSiteId(mSite.getId());
        media.setFileExtension(fileExtension);
        media.setMimeType(mimeType);
        media.setUploadState(MediaUploadState.QUEUED);
        media.setUploadDate(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        addMediaToUploadService(media);

        updateMediaGridItem(media, false);
    }

    private void handleSharedMedia() {
        Intent intent = getIntent();

        final List<Uri> multi_stream;
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            multi_stream = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            multi_stream = new ArrayList<>();
            multi_stream.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
        } else {
            multi_stream = null;
        }

        if (multi_stream != null) {
            uploadList(multi_stream);
        }

        // clear the intent's action, so that in case the user rotates, we don't re-upload the same files
        getIntent().setAction(null);
    }

    private void startMediaDeleteService(ArrayList<MediaModel> mediaToDelete) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start MediaDeleteService, internet connection required.");
            return;
        }

        if (mDeleteService != null) {
            if (mediaToDelete != null && !mediaToDelete.isEmpty()) {
                for (MediaModel media : mediaToDelete) {
                    mDeleteService.addMediaToDeleteQueue(media);
                }
            }
        } else {
            Intent intent = new Intent(this, MediaDeleteService.class);
            intent.putExtra(MediaDeleteService.SITE_KEY, mSite);
            if (mediaToDelete != null) {
                intent.putExtra(MediaDeleteService.MEDIA_LIST_KEY, mediaToDelete);
                doBindDeleteService(intent);
            }
            startService(intent);
        }
    }

    private void updateMediaGridItem(@NonNull MediaModel media, boolean forceUpdate) {
        if (mMediaGridFragment != null) {
            if (mMediaStore.getMediaWithLocalId(media.getId()) != null) {
                mMediaGridFragment.updateMediaItem(media, forceUpdate);
            } else {
                mMediaGridFragment.removeMediaItem(media);
            }
        }
    }

    private void reloadMediaGrid() {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.reload();
        }
    }
}
