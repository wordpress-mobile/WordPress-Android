package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.media.services.MediaEvents;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetFeatures.Callback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * The main activity in which the user can browse their media.
 */
public class MediaBrowserActivity extends AppCompatActivity implements MediaGridListener,
        MediaItemFragmentCallback, OnQueryTextListener, OnActionExpandListener,
        MediaEditFragmentCallback {
    private static final String SAVED_QUERY = "SAVED_QUERY";
    public static final int MEDIA_PERMISSION_REQUEST_CODE = 1;

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MediaEditFragment mMediaEditFragment;
    private MediaAddFragment mMediaAddFragment;
    private PopupWindow mAddMediaPopup;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private Menu mMenu;
    private FeatureSet mFeatureSet;
    private String mQuery;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                // Coming from zero connection. Continue what's pending for delete
                int blogId = WordPress.getCurrentLocalTableBlogId();
                if (blogId != -1 && WordPress.wpDB.hasMediaDeleteQueueItems(blogId)) {
                    startMediaDeleteService();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This should be removed when #2734 is fixed
        if (WordPress.getCurrentBlog() == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.media_browser_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.media);

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        FragmentTransaction ft = fm.beginTransaction();

        mMediaAddFragment = (MediaAddFragment) fm.findFragmentById(R.id.mediaAddFragment);
        mMediaGridFragment = (MediaGridFragment) fm.findFragmentById(R.id.mediaGridFragment);

        mMediaItemFragment = (MediaItemFragment) fm.findFragmentByTag(MediaItemFragment.TAG);
        if (mMediaItemFragment != null)
            ft.hide(mMediaGridFragment);

        mMediaEditFragment = (MediaEditFragment) fm.findFragmentByTag(MediaEditFragment.TAG);
        if (mMediaEditFragment != null && !mMediaEditFragment.isInLayout())
            ft.hide(mMediaItemFragment);

        ft.commitAllowingStateLoss();

        setupAddMenuPopup();

        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // We arrived here from a share action
            uploadSharedFiles();
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(mReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mReceiver);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_QUERY, mQuery);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mQuery = savedInstanceState.getString(SAVED_QUERY);
    }

    private void uploadSharedFiles() {
        Intent intent = getIntent();
        String action = intent.getAction();
        final List<Uri> multi_stream;
        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            multi_stream = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
        } else {
            multi_stream = new ArrayList<>();
            multi_stream.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
        }
        mMediaAddFragment.uploadList(multi_stream);

        // clear the intent's action, so that in case the user rotates, we don't re-upload the same
        // files
        getIntent().setAction(null);
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            FragmentManager manager = getFragmentManager();
            MediaGridFragment mediaGridFragment = (MediaGridFragment)manager.findFragmentById(R.id.mediaGridFragment);
            if (mediaGridFragment.isVisible()) {
                mediaGridFragment.refreshSpinnerAdapter();
            }
            ActivityUtils.hideKeyboard(MediaBrowserActivity.this);
        }
    };

    /** Setup the popup that allows you to add new media from camera, video camera or local files **/
    private void setupAddMenuPopup() {
        String capturePhoto = getResources().getString(R.string.media_add_popup_capture_photo);
        String captureVideo = getResources().getString(R.string.media_add_popup_capture_video);
        String pickPhotoFromGallery = getResources().getString(R.string.select_photo);
        String pickVideoFromGallery = getResources().getString(R.string.select_video);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(MediaBrowserActivity.this,
                R.layout.actionbar_add_media_cell,
                new String[] {
                        capturePhoto, captureVideo, pickPhotoFromGallery, pickVideoFromGallery
                });

        View layoutView = getLayoutInflater().inflate(R.layout.actionbar_add_media, null, false);
        ListView listView = (ListView) layoutView.findViewById(R.id.actionbar_add_media_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.notifyDataSetChanged();

                if (position == 0) {
                    mMediaAddFragment.launchCamera();
                } else if (position == 1) {
                    mMediaAddFragment.launchVideoCamera();
                } else if (position == 2) {
                    mMediaAddFragment.launchPictureLibrary();
                } else if (position == 3) {
                    mMediaAddFragment.launchVideoLibrary();
                }

                mAddMediaPopup.dismiss();
            }
        });

        int width = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_width);

        mAddMediaPopup = new PopupWindow(layoutView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAddMediaPopup.setBackgroundDrawable(new ColorDrawable());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService();
        getFeatureSet();
        ActivityId.trackLastActivity(ActivityId.MEDIA);
    }

    /** Get the feature set for a wordpress.com hosted blog **/
    private void getFeatureSet() {
        if (WordPress.getCurrentBlog() == null || !WordPress.getCurrentBlog().isDotcomFlag())
            return;

        ApiHelper.GetFeatures task = new ApiHelper.GetFeatures(new Callback() {
            @Override
            public void onResult(FeatureSet featureSet) {
                mFeatureSet = featureSet;
            }

        });

        List<Object> apiArgs = new ArrayList<>();
        apiArgs.add(WordPress.getCurrentBlog());
        task.execute(apiArgs);
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
    public void onMediaItemSelected(String mediaId) {
        String tempQuery = mQuery;
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }

        if (mSearchMenuItem != null) {
            MenuItemCompat.collapseActionView(mSearchMenuItem);
        }

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaGridFragment);
            mMediaGridFragment.clearSelectedItems();
            mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
            ft.add(R.id.media_browser_container, mMediaItemFragment, MediaItemFragment.TAG);
            ft.addToBackStack(null);
            ft.commitAllowingStateLoss();
            mQuery = tempQuery;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        getMenuInflater().inflate(R.menu.media, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        mSearchView.setOnQueryTextListener(this);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, this);

        //open search bar if we were searching for something before
        if (!TextUtils.isEmpty(mQuery) && mMediaGridFragment != null && mMediaGridFragment.isVisible()) {
            String tempQuery = mQuery; //temporary hold onto query
            MenuItemCompat.expandActionView(mSearchMenuItem); //this will reset mQuery
            onQueryTextSubmit(tempQuery);
            mSearchView.setQuery(mQuery, true);
        }

        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MEDIA_PERMISSION_REQUEST_CODE:
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        ToastUtils.showToast(this, getString(R.string.add_media_permission_required));
                        return;
                    }
                }
                showNewMediaMenu();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == R.id.menu_new_media) {
            if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(this, MEDIA_PERMISSION_REQUEST_CODE)) {
                showNewMediaMenu();
            }
            return true;
        } else if (i == R.id.menu_search) {
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
        } else if (i == R.id.menu_edit_media) {
            String mediaId = mMediaItemFragment.getMediaId();
            FragmentManager fm = getFragmentManager();

            if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
                // phone layout: hide item details, show and update edit fragment
                FragmentTransaction ft = fm.beginTransaction();

                if (mMediaItemFragment.isVisible())
                    ft.hide(mMediaItemFragment);

                mMediaEditFragment = MediaEditFragment.newInstance(mediaId);
                ft.add(R.id.media_browser_container, mMediaEditFragment, MediaEditFragment.TAG);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
            } else {
                // tablet layout: update edit fragment
                mMediaEditFragment.loadMedia(mediaId);
            }

            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMediaItemListDownloaded() {
        if (mMediaItemFragment != null) {
            mMediaGridFragment.setRefreshing(false);
            if (mMediaItemFragment.isInLayout()) {
                mMediaItemFragment.loadDefaultMedia();
            }
        }
    }

    @Override
    public void onMediaItemListDownloadStart() {
        mMediaGridFragment.setRefreshing(true);
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
    public void onResume(Fragment fragment) {
        invalidateOptionsMenu();
    }

    @Override
    public void onPause(Fragment fragment) {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // currently we don't support searching from within a filter, so hide it
        if (mMediaGridFragment != null) {
            mMediaGridFragment.setFilterVisibility(View.GONE);
            mMediaGridFragment.setFilter(Filter.ALL);
        }

        // load last search query
        if (!TextUtils.isEmpty(mQuery))
            onQueryTextChange(mQuery);
        mMenu.findItem(R.id.menu_new_media).setVisible(false);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (mMediaGridFragment != null) {
            mMediaGridFragment.setFilterVisibility(View.VISIBLE);
            mMediaGridFragment.setFilter(Filter.ALL);
        }
        mMenu.findItem(R.id.menu_new_media).setVisible(true);
        return true;
    }

    public void onSavedEdit(String mediaId, boolean result) {
        if (mMediaEditFragment != null && mMediaEditFragment.isVisible() && result) {
            FragmentManager fm = getFragmentManager();
            fm.popBackStack();

            // refresh media item details (phone-only)
            if (mMediaItemFragment != null)
                mMediaItemFragment.loadMedia(mediaId);

            // refresh grid
            mMediaGridFragment.refreshMediaFromDB();
        }
    }

    private void startMediaDeleteService() {
        if (NetworkUtils.isNetworkAvailable(this)) {
           startService(new Intent(this, MediaDeleteService.class));
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(MediaEvents.MediaChanged event) {
        updateOnMediaChanged(event.mLocalBlogId, event.mMediaId);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(MediaEvents.MediaUploadSucceeded event) {
        updateOnMediaChanged(event.mLocalBlogId, event.mLocalMediaId);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(MediaEvents.MediaUploadFailed event) {
        ToastUtils.showToast(this, event.mErrorMessage, ToastUtils.Duration.LONG);
    }

    public void updateOnMediaChanged(String blogId, String mediaId) {
        if (mediaId == null) {
            return;
        }

        // If the media was deleted, remove it from multi select (if it was selected) and hide it from the the detail
        // view (if it was the one displayed)
        if (!WordPress.wpDB.mediaFileExists(blogId, mediaId)) {
            mMediaGridFragment.removeFromMultiSelect(mediaId);
            if (mMediaEditFragment != null && mMediaEditFragment.isVisible()
                    && mediaId.equals(mMediaEditFragment.getMediaId())) {
                if (mMediaEditFragment.isInLayout()) {
                    mMediaEditFragment.loadMedia(null);
                } else {
                    getFragmentManager().popBackStack();
                }
            }
        }

        // Update Grid view
        mMediaGridFragment.refreshMediaFromDB();

        // Update Spinner views
        mMediaGridFragment.updateFilterText();
        mMediaGridFragment.updateSpinnerAdapter();
    }

    @Override
    public void onRetryUpload(String mediaId) {
        mMediaAddFragment.addToQueue(mediaId);
    }

    public void deleteMedia(final ArrayList<String> ids) {
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Set<String> sanitizedIds = new HashSet<>(ids.size());

        // phone layout: pop the item fragment if it's visible
        getFragmentManager().popBackStack();

        // Make sure there are no media in "uploading"
        for (String currentID : ids) {
            if (WordPressMediaUtils.canDeleteMedia(blogId, currentID)) {
                sanitizedIds.add(currentID);
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
        WordPress.wpDB.setMediaFilesMarkedForDelete(blogId, sanitizedIds);
        startMediaDeleteService();
        if (mMediaGridFragment != null) {
            mMediaGridFragment.clearSelectedItems();
            mMediaGridFragment.refreshMediaFromDB();
        }
    }

    private void showNewMediaMenu() {
        View view = findViewById(R.id.menu_new_media);
        if (view != null) {
            int y_offset = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_y_offset);
            int[] loc = new int[2];
            view.getLocationOnScreen(loc);
            mAddMediaPopup.showAtLocation(view, Gravity.TOP | Gravity.LEFT, loc[0],
                    loc[1] + view.getHeight() + y_offset);
        } else {
            // In case menu button is not on screen (declared showAsAction="ifRoom"), center the popup in the view.
            View gridView = findViewById(R.id.media_gridview);
            mAddMediaPopup.showAtLocation(gridView, Gravity.CENTER, 0, 0);
        }
    }
}
