package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.ui.WPDrawerActivity;
import org.wordpress.android.ui.media.MediaAddFragment.MediaAddFragmentCallback;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetFeatures.Callback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The main activity in which the user can browse their media.
 * Accessible via the menu drawer as "Media"
 */
public class MediaBrowserActivity extends WPDrawerActivity implements MediaGridListener,
        MediaItemFragmentCallback, OnQueryTextListener, OnActionExpandListener, MediaEditFragmentCallback,
        MediaAddFragmentCallback {
    private static final String SAVED_QUERY = "SAVED_QUERY";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createMenuDrawer(R.layout.media_browser_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.media);
        }

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        FragmentTransaction ft = fm.beginTransaction();
        setupBaseLayout();

        mMediaAddFragment = (MediaAddFragment) fm.findFragmentById(R.id.mediaAddFragment);
        mMediaGridFragment = (MediaGridFragment) fm.findFragmentById(R.id.mediaGridFragment);

        mMediaItemFragment = (MediaItemFragment) fm.findFragmentByTag(MediaItemFragment.TAG);
        if (mMediaItemFragment != null)
            ft.hide(mMediaGridFragment);

        mMediaEditFragment = (MediaEditFragment) fm.findFragmentByTag(MediaEditFragment.TAG);
        if (mMediaEditFragment != null && !mMediaEditFragment.isInLayout())
            ft.hide(mMediaItemFragment);

        ft.commit();

        setupAddMenuPopup();

        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // We arrived here from a share action
            uploadSharedFiles();
        }
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
            multi_stream = new ArrayList<Uri>();
            multi_stream.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
        }
        mMediaAddFragment.uploadList(multi_stream);

        // clear the intent's action, so that in case the user rotates, we don't re-upload the same
        // files
        getIntent().setAction(null);
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            setupBaseLayout();
        }
    };

    private void setupBaseLayout() {
        // hide access to the drawer when there are fragments in the back stack
        if (getDrawerToggle() != null) {
            getDrawerToggle().setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() == 0);
        }
    }

    /** Setup the popup that allows you to add new media from camera, video camera or local files **/
    private void setupAddMenuPopup() {
        String capturePhoto = getResources().getString(R.string.media_add_popup_capture_photo);
        String captureVideo = getResources().getString(R.string.media_add_popup_capture_video);
        String pickPhotoFromGallery = getResources().getString(R.string.select_photo);
        String pickVideoFromGallery = getResources().getString(R.string.select_video);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MediaBrowserActivity.this,
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

                // Support video only if you are self-hosted or are a dot-com blog with the video
                // press upgrade
                boolean selfHosted = !WordPress.getCurrentBlog().isDotcomFlag();
                boolean isVideoEnabled = selfHosted
                        || (mFeatureSet != null && mFeatureSet.isVideopressEnabled());

                if (position == 0) {
                    mMediaAddFragment.launchCamera();
                } else if (position == 1) {
                    if (isVideoEnabled) {
                        mMediaAddFragment.launchVideoCamera();
                    } else {
                        showVideoPressUpgradeDialog();
                    }
                } else if (position == 2) {
                    mMediaAddFragment.launchPictureLibrary();
                } else if (position == 3) {
                    if (isVideoEnabled) {
                        mMediaAddFragment.launchVideoLibrary();
                    } else {
                        showVideoPressUpgradeDialog();
                    }
                }

                mAddMediaPopup.dismiss();

            }

        });

        int width = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_width);

        mAddMediaPopup = new PopupWindow(layoutView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAddMediaPopup.setBackgroundDrawable(new ColorDrawable());
    }

    private void showVideoPressUpgradeDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        String title = getString(R.string.media_no_video_title);
        String message = getString(R.string.media_no_video_message);
        String infoTitle = getString(R.string.learn_more);
        String infoURL = Constants.videoPressURL;
        WPAlertDialogFragment alert = WPAlertDialogFragment.newUrlInfoDialog(title, message, infoTitle, infoURL);
        ft.add(alert, "alert");
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService();
        getFeatureSet();
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

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        task.execute(apiArgs);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }
    }

    @Override
    public void onBlogChanged() {
        // clear edit fragment
        if (mMediaEditFragment != null) {
            mMediaEditFragment.loadMedia(null);

            // hide if in phone
            if (!mMediaEditFragment.isInLayout() && mMediaEditFragment.isVisible()) {
                getFragmentManager().popBackStack();
            }
        }

        getFragmentManager().executePendingTransactions();

        // clear item fragment (only visible on phone)
        if (mMediaItemFragment != null && mMediaItemFragment.isVisible()) {
            getFragmentManager().popBackStack();
        }

        // reset the media fragment
        if (mMediaGridFragment != null) {
            mMediaGridFragment.reset();
            mMediaGridFragment.refreshSpinnerAdapter();

            if (!mMediaGridFragment.hasRetrievedAllMediaFromServer()) {
                mMediaGridFragment.setRefreshing(true);
                mMediaGridFragment.refreshMediaFromServer(0, false);
            }
        }

        // check what features (e.g. video) the user has
        getFeatureSet();
    }

    @Override
    public void onMediaItemSelected(String mediaId) {
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }

        if (mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaGridFragment);
            mMediaGridFragment.clearSelectedItems();
            setupBaseLayout();
            mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
            ft.add(R.id.media_browser_container, mMediaItemFragment, MediaItemFragment.TAG);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        getMenuInflater().inflate(R.menu.media, menu);
        mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_new_media) {
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
            return true;
        } else if (itemId == R.id.menu_search) {
            mSearchMenuItem = item;
            mSearchMenuItem.setOnActionExpandListener(this);
            mSearchMenuItem.expandActionView();

            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);

            // load last saved query
            if (!TextUtils.isEmpty(mQuery)) {
                onQueryTextSubmit(mQuery);
                mSearchView.setQuery(mQuery, true);
            }
            return true;
        } else if (itemId == R.id.menu_edit_media) {
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
                ft.commit();
                if (getDrawerToggle() != null) {
                    getDrawerToggle().setDrawerIndicatorEnabled(false);
                }
            } else {
                // tablet layout: update edit fragment
                mMediaEditFragment.loadMedia(mediaId);
            }

            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
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
        // preserve the previous query
        String tmpQuery = mQuery;
        onQueryTextChange("");
        mQuery = tmpQuery;

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
        startService(new Intent(this, MediaDeleteService.class));
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            setupBaseLayout();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMediaAdded(String mediaId) {
        if (WordPress.getCurrentBlog() == null || mediaId == null) {
            return;
        }
        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);

        if (cursor == null || !cursor.moveToFirst()) {
            mMediaGridFragment.removeFromMultiSelect(mediaId);
            if (mMediaEditFragment != null && mMediaEditFragment.isVisible()
                    && mediaId.equals(mMediaEditFragment.getMediaId())) {
                if (mMediaEditFragment.isInLayout()) {
                    mMediaEditFragment.loadMedia(null);
                } else {
                    getFragmentManager().popBackStack();
                }
            }
        } else {
            mMediaGridFragment.refreshMediaFromDB();
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void onRetryUpload(String mediaId) {
        mMediaAddFragment.addToQueue(mediaId);
    }

    public void deleteMedia(final ArrayList<String> ids) {
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Set<String> sanitizedIds = new HashSet<String>(ids.size());

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
        startService(new Intent(this, MediaDeleteService.class));
        if (mMediaGridFragment != null) {
            mMediaGridFragment.clearSelectedItems();
            mMediaGridFragment.refreshMediaFromDB();
        }
    }
}
