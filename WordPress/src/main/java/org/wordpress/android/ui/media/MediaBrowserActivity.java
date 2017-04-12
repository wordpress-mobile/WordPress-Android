package org.wordpress.android.ui.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * The main activity in which the user can browse their media.
 */
public class MediaBrowserActivity extends AppCompatActivity implements MediaGridListener,
        MediaItemFragmentCallback, OnQueryTextListener, OnActionExpandListener,
        MediaEditFragmentCallback, WordPressMediaUtils.LaunchCameraCallback {
    private static final int MEDIA_PERMISSION_REQUEST_CODE = 1;

    private static final String SAVED_QUERY = "SAVED_QUERY";
    private static final String BUNDLE_MEDIA_CAPTURE_PATH = "mediaCapturePath";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private SiteModel mSite;

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MediaEditFragment mMediaEditFragment;
    private PopupWindow mAddMediaPopup;

    // Views
    private Toolbar mToolbar;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private Menu mMenu;

    // Services
    private MediaDeleteService.MediaDeleteBinder mDeleteService;
    private boolean mDeleteServiceBound;

    private String mQuery;
    private String mMediaCapturePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((WordPress) getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.media_browser_activity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.media);
        }

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);

        mMediaGridFragment = (MediaGridFragment) fm.findFragmentById(R.id.mediaGridFragment);
        mMediaItemFragment = (MediaItemFragment) fm.findFragmentByTag(MediaItemFragment.TAG);
        mMediaEditFragment = (MediaEditFragment) fm.findFragmentByTag(MediaEditFragment.TAG);

        FragmentTransaction ft = fm.beginTransaction();
        if (mMediaItemFragment != null) {
            ft.hide(mMediaGridFragment);
        }
        if (mMediaEditFragment != null && !mMediaEditFragment.isInLayout()) {
            ft.hide(mMediaItemFragment);
        }
        ft.commitAllowingStateLoss();

        setupAddMenuPopup();

        // if media was shared add it to the library
        handleSharedMedia();
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
    public void onPause(Fragment fragment) {
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService(null);
        ActivityId.trackLastActivity(ActivityId.MEDIA);
    }

    @Override
    public void onResume(Fragment fragment) {
        invalidateOptionsMenu();
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
        if (!TextUtils.isEmpty(mMediaCapturePath)) {
            outState.putString(BUNDLE_MEDIA_CAPTURE_PATH, mMediaCapturePath);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        mMediaCapturePath = savedInstanceState.getString(BUNDLE_MEDIA_CAPTURE_PATH);
        mQuery = savedInstanceState.getString(SAVED_QUERY);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            if (mMediaEditFragment != null && mMediaEditFragment.isVisible() && mMediaEditFragment.isDirty()) {
                // alert the user that there are unsaved changes
                new AlertDialog.Builder(this)
                        .setMessage(R.string.confirm_discard_changes)
                        .setCancelable(true)
                        .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // make sure the keyboard is dismissed
                                WPActivityUtils.hideKeyboard(getCurrentFocus());

                                // pop the edit fragment
                                doPopBackStack(getFragmentManager());
                            }})
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show();
            } else {
                doPopBackStack(fm);
            }
        } else {
            super.onBackPressed();
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
                    Uri uri;
                    Uri optimizedMedia = WPMediaUtils.getOptimizedMedia(this, mSite, mMediaCapturePath, false);
                    if (optimizedMedia != null) {
                        uri = optimizedMedia;
                    } else {
                        uri = Uri.parse(mMediaCapturePath);
                    }
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
        // only MEDIA_PERMISSION_REQUEST_CODE is handled
        if (requestCode != MEDIA_PERMISSION_REQUEST_CODE) {
            return;
        }

        for (int grantResult : results) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                ToastUtils.showToast(this, getString(R.string.add_media_permission_required));
                return;
            }
        }

        showNewMediaMenu();
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

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_new_media:
                AppLockManager.getInstance().setExtendedTimeout();
                if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, MEDIA_PERMISSION_REQUEST_CODE)) {
                    showNewMediaMenu();
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
            case R.id.menu_edit_media:
                int localMediaId = mMediaItemFragment.getLocalMediaId();

                if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
                    // phone layout: hide item details, show and update edit fragment
                    FragmentManager fm = getFragmentManager();
                    mMediaEditFragment = MediaEditFragment.newInstance(mSite, localMediaId);

                    FragmentTransaction ft = fm.beginTransaction();
                    if (mMediaItemFragment.isVisible()) {
                        ft.hide(mMediaItemFragment);
                    }
                    ft.add(R.id.media_browser_container, mMediaEditFragment, MediaEditFragment.TAG);
                    ft.addToBackStack(null);
                    ft.commitAllowingStateLoss();
                } else {
                    // tablet layout: update edit fragment
                    mMediaEditFragment.loadMedia(localMediaId);
                }

                if (mSearchView != null) {
                    mSearchView.clearFocus();
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.setFilterEnabled(false);
        }

        // load last search query
        if (!TextUtils.isEmpty(mQuery)) {
            onQueryTextChange(mQuery);
        }

        mMenu.findItem(R.id.menu_new_media).setVisible(false);

        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.setFilterEnabled(true);
        }

        mMenu.findItem(R.id.menu_new_media).setVisible(true);
        invalidateOptionsMenu();

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
    public void setLookClosable() {
        mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    }

    @Override
    public void onMediaItemSelected(View sourceView, int localMediaId) {
        final String tempQuery = mQuery;

        if (mSearchView != null) {
            mSearchView.clearFocus();
        }

        if (mSearchMenuItem != null) {
            MenuItemCompat.collapseActionView(mSearchMenuItem);
        }

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            mMediaGridFragment.clearSelectedItems();
            mMediaItemFragment = MediaItemFragment.newInstance(mSite, localMediaId);

            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaGridFragment);
            ft.add(R.id.media_browser_container, mMediaItemFragment, MediaItemFragment.TAG);
            ft.addToBackStack(null);
            ft.commitAllowingStateLoss();

            mQuery = tempQuery;
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
        if (event.isError()) {
            AppLog.w(AppLog.T.MEDIA, "Received onMediaChanged error: " + event.error.type
                    + " - " + event.error.message);
            showMediaToastError(R.string.media_generic_error, event.error.message);
            return;
        }

        switch (event.cause) {
            case DELETE_MEDIA:
                if (event.mediaList == null || event.mediaList.isEmpty()) {
                    break;
                }

                // If the media was deleted, remove it from multi select (if it was selected) and hide it from the
                // detail view (if it was the one displayed)
                for (MediaModel mediaModel : event.mediaList) {
                    int localMediaId = mediaModel.getId();
                    mMediaGridFragment.removeFromMultiSelect(localMediaId);
                    if (mMediaEditFragment != null && mMediaEditFragment.isVisible()
                            && localMediaId == mMediaEditFragment.getLocalMediaId()) {
                        updateOnMediaChanged(localMediaId);
                        if (mMediaEditFragment.isInLayout()) {
                            mMediaEditFragment.loadMedia(MediaEditFragment.MISSING_MEDIA_ID);
                        } else {
                            getFragmentManager().popBackStack();
                        }
                    }
                }
                break;
        }
        updateViews();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaListFetched(OnMediaListFetched event) {
        if (event.isError()) {
            AppLog.w(AppLog.T.MEDIA, "Received OnMediaListFetched error: " + event.error.type
                    + " - " + event.error.message);
            ToastUtils.showToast(this, "Media fetch error occurred: " + event.error.message, ToastUtils.Duration.LONG);
            return;
        }
        updateViews();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.isError()) {
            AppLog.d(AppLog.T.MEDIA, "Received onMediaUploaded error:" + event.error.type
                    + " - " + event.error.message);
            switch (event.error.type) {
                case AUTHORIZATION_REQUIRED:
                    showMediaToastError(R.string.media_error_no_permission, null);
                    break;
                case REQUEST_TOO_LARGE:
                    showMediaToastError(R.string.media_error_too_large_upload, null);
                    break;
                default:
                    showMediaToastError(R.string.media_upload_error, event.error.message);
            }
            updateViews();
        } else if (event.completed) {
            String title = "";
            if (event.media != null) {
                title = event.media.getTitle();
            }
            AppLog.d(AppLog.T.MEDIA, "<" + title + "> upload complete");
            updateViews();
        }
    }

    public void onSavedEdit(int localMediaId, boolean result) {
        if (mMediaEditFragment != null && mMediaEditFragment.isVisible() && result) {
            doPopBackStack(getFragmentManager());

            // refresh media item details (phone-only)
            if (mMediaItemFragment != null)
                mMediaItemFragment.loadMedia(localMediaId);

            // refresh grid
            mMediaGridFragment.refreshMediaFromDB();
        }
    }

    private void updateOnMediaChanged(int localMediaId) {
        if (localMediaId == -1) {
            return;
        }

        // If the media was deleted, remove it from multi select (if it was selected) and hide it from the the detail
        // view (if it was the one displayed)
        if (mMediaStore.getMediaWithLocalId(localMediaId) == null) {
            mMediaGridFragment.removeFromMultiSelect(localMediaId);
            if (mMediaEditFragment != null && mMediaEditFragment.isVisible()
                    && localMediaId == mMediaEditFragment.getLocalMediaId()) {
                if (mMediaEditFragment.isInLayout()) {
                    mMediaEditFragment.loadMedia(MediaEditFragment.MISSING_MEDIA_ID);
                } else {
                    doPopBackStack(getFragmentManager());
                }
            }
        }
        updateViews();
    }

    public void deleteMedia(final ArrayList<Integer> ids) {
        Set<String> sanitizedIds = new HashSet<>(ids.size());

        // phone layout: pop the item fragment if it's visible
        doPopBackStack(getFragmentManager());

        final ArrayList<MediaModel> mediaToDelete = new ArrayList<>();
        // Make sure there are no media in "uploading"
        for (int currentId : ids) {
            MediaModel mediaModel = mMediaStore.getMediaWithLocalId(currentId);
            if (mediaModel == null) {
                continue;
            }

            if (WordPressMediaUtils.canDeleteMedia(mediaModel)) {
                if (mediaModel.getUploadState() != null &&
                        MediaUtils.isLocalFile(mediaModel.getUploadState().toLowerCase())) {
                    mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel));
                    sanitizedIds.add(String.valueOf(currentId));
                    continue;
                }
                mediaToDelete.add(mediaModel);
                mediaModel.setUploadState(MediaUploadState.DELETING.name());
                mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel));
                sanitizedIds.add(String.valueOf(currentId));
            }
        }

        if (sanitizedIds.size() != ids.size()) {
            if (ids.size() == 1) {
                Toast.makeText(this, R.string.wait_until_upload_completes, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.cannot_delete_multi_media_items, Toast.LENGTH_LONG).show();
            }
        }

        // mark items for delete without actually deleting items yet,
        // and then refresh the grid
        if (!mediaToDelete.isEmpty()) {
            startMediaDeleteService(mediaToDelete);
        }
        if (mMediaGridFragment != null) {
            mMediaGridFragment.clearSelectedItems();
        }
    }

    private void uploadList(List<Uri> uriList) {
        for (Uri uri : uriList) {
            fetchMedia(uri, getContentResolver().getType(uri));
        }
    }

    private final OnBackStackChangedListener mOnBackStackChangedListener = new OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            FragmentManager manager = getFragmentManager();
            MediaGridFragment mediaGridFragment = (MediaGridFragment) manager.findFragmentById(R.id.mediaGridFragment);
            if (mediaGridFragment.isVisible()) {
                mediaGridFragment.refreshSpinnerAdapter();
            }
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
    private void setupAddMenuPopup() {
        String capturePhoto = getString(R.string.media_add_popup_capture_photo);
        String captureVideo = getString(R.string.media_add_popup_capture_video);
        String pickPhotoFromGallery = getString(R.string.select_photo);
        String pickVideoFromGallery = getString(R.string.select_video);
        String[] items = new String[] {
                capturePhoto, captureVideo, pickPhotoFromGallery, pickVideoFromGallery
        };

        @SuppressLint("InflateParams")
        View menuView = getLayoutInflater().inflate(R.layout.actionbar_add_media, null, false);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.actionbar_add_media_cell, items);
        ListView listView = (ListView) menuView.findViewById(R.id.actionbar_add_media_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.notifyDataSetChanged();

                if (position == 0) {
                    MediaBrowserActivity enclosingActivity = MediaBrowserActivity.this;
                    WordPressMediaUtils.launchCamera(enclosingActivity, BuildConfig.APPLICATION_ID, enclosingActivity);
                } else if (position == 1) {
                    WordPressMediaUtils.launchVideoCamera(MediaBrowserActivity.this);
                } else if (position == 2) {
                    WordPressMediaUtils.launchPictureLibrary(MediaBrowserActivity.this);
                } else if (position == 3) {
                    WordPressMediaUtils.launchVideoLibrary(MediaBrowserActivity.this);
                }

                mAddMediaPopup.dismiss();
            }
        });

        int width = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_width);
        mAddMediaPopup = new PopupWindow(menuView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAddMediaPopup.setBackgroundDrawable(new ColorDrawable());
    }

    private void showNewMediaMenu() {
        View view = findViewById(R.id.menu_new_media);
        if (view != null) {
            int y_offset = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_y_offset);
            int[] loc = new int[2];
            view.getLocationOnScreen(loc);
            mAddMediaPopup.showAtLocation(view, Gravity.TOP | Gravity.START, loc[0],
                    loc[1] + view.getHeight() + y_offset);
        } else {
            // In case menu button is not on screen (declared showAsAction="ifRoom"), center the popup in the view.
            View gridView = findViewById(R.id.recycler);
            mAddMediaPopup.showAtLocation(gridView, Gravity.CENTER, 0, 0);
        }
    }

    private void fetchMedia(Uri mediaUri, final String mimeType) {
        if (!MediaUtils.isInMediaStore(mediaUri)) {
            // Create an AsyncTask to download the file
            new AsyncTask<Uri, Integer, Uri>() {
                @Override
                protected Uri doInBackground(Uri... uris) {
                    Uri imageUri = uris[0];
                    return MediaUtils.downloadExternalMedia(MediaBrowserActivity.this, imageUri);
                }

                protected void onPostExecute(Uri uri) {
                    if (uri != null) {
                        queueFileForUpload(uri, mimeType);
                    } else {
                        Toast.makeText(MediaBrowserActivity.this, getString(R.string.error_downloading_image),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaUri);
        } else {
            queueFileForUpload(mediaUri, mimeType);
        }
    }

    private void addMediaToUploadService(@NonNull MediaModel media) {
        // Start the upload service if it's not started and fill the media queue
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start MediaUploadService, internet connection required.");
            return;
        }

        ArrayList<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaUploadService.startService(this, mSite, mediaList);
    }

    private void queueFileForUpload(Uri uri, String mimeType) {
        // It is a regular local media file
        String path = getRealPathFromURI(uri);

        if (path == null || path.equals("")) {
            Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
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
        media.setFilePath(path);
        media.setLocalSiteId(mSite.getId());
        media.setFileExtension(fileExtension);
        media.setMimeType(mimeType);
        media.setUploadState(MediaUploadState.QUEUED.name());
        media.setUploadDate(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
        addMediaToUploadService(media);
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

    private void doPopBackStack(FragmentManager fm) {
        fm.popBackStack();

        // reset the button to "back" as it may have been altered by a fragment
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
    }

    private String getRealPathFromURI(Uri uri) {
        String path;
        if ("content".equals(uri.getScheme())) {
            path = MediaUtils.getPath(this, uri);
        } else if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        } else {
            path = uri.toString();
        }
        return path;
    }

    private void updateViews() {
        mMediaGridFragment.refreshMediaFromDB();
        mMediaGridFragment.updateFilterText();
        mMediaGridFragment.updateSpinnerAdapter();
    }
}