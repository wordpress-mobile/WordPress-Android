package org.wordpress.android.ui.media;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaAddFragment.MediaAddFragmentCallback;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.EditPostContentFragment;
import org.wordpress.android.util.MediaDeleteService;
import org.wordpress.android.util.Utils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetFeatures.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * The main activity in which the user can browse their media.
 * Accessible via the menu drawer as "Media"
 */

public class MediaBrowserActivity extends WPActionBarActivity implements MediaGridListener,
        MediaItemFragmentCallback,
        OnQueryTextListener, OnActionExpandListener, MediaEditFragmentCallback,
        MediaAddFragmentCallback,
        com.actionbarsherlock.view.ActionMode.Callback {

    private static final String SAVED_QUERY = "SAVED_QUERY";

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MediaEditFragment mMediaEditFragment;
    private MediaAddFragment mMediaAddFragment;
    private PopupWindow mAddMediaPopup;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private MenuItem mRefreshMenuItem;
    private Menu mMenu;
    private FeatureSet mFeatureSet;
    private ActionMode mActionMode;

    private Handler mHandler;
    private int mMultiSelectCount;
    private String mQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mHandler = new Handler();

        setTitle(R.string.media);

        createMenuDrawer(R.layout.media_browser_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
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

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            setupBaseLayout();
        }
    };

    private void setupBaseLayout() {
        // hide access to the drawer when there are fragments in the back stack
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            mMenuDrawer.setDrawerIndicatorEnabled(true);
        } else {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        }
    }

    /** Setup the popup that allows you to add new media from camera, video camera or local files **/
    private void setupAddMenuPopup() {

        String capturePhoto = getResources().getString(R.string.media_add_popup_capture_photo);
        String captureVideo = getResources().getString(R.string.media_add_popup_capture_video);
        String pickPhotoFromGallery = getResources().getString(
                R.string.select_photo);
        String pickVideoFromGallery = getResources().getString(
                R.string.select_video);
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
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        String title = getString(R.string.media_no_video_title);
        String message = getString(R.string.media_no_video_message);
        String infoTitle = getString(R.string.learn_more);
        String infoURL = Constants.videoPressURL;
        WPAlertDialogFragment alert = WPAlertDialogFragment.newInstance(message, title, false, infoTitle, infoURL);
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

        if (mSearchMenuItem != null)
            mSearchMenuItem.collapseActionView();
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();

        // clear edit fragment
        if (mMediaEditFragment != null) {
            mMediaEditFragment.loadMedia(null);

            // hide if in phone
            if (!mMediaEditFragment.isInLayout() && mMediaEditFragment.isVisible())
                getSupportFragmentManager().popBackStack();
        }

        getSupportFragmentManager().executePendingTransactions();

        // clear item fragment (only visible on phone)
        if (mMediaItemFragment != null && mMediaItemFragment.isVisible())
            getSupportFragmentManager().popBackStack();

        // reset the media fragment
        if (mMediaGridFragment != null) {
            mMediaGridFragment.reset();
            mMediaGridFragment.refreshSpinnerAdapter();

            if (!mMediaGridFragment.hasRetrievedAllMediaFromServer()) {
                mMediaGridFragment.refreshMediaFromServer(0, false);
                startAnimatingRefreshButton();
            }
        }

        // check what features (e.g. video) the user has
        getFeatureSet();
    };

    @Override
    public void onMediaItemSelected(String mediaId) {

        if (mSearchView != null)
            mSearchView.clearFocus();

        // collapse the search menu on phone
        if (mSearchMenuItem != null && !Utils.isTablet())
            mSearchMenuItem.collapseActionView();

        FragmentManager fm = getSupportFragmentManager();

        if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
            // phone: hide the grid and show the item details
            if (fm.getBackStackEntryCount() == 0) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mMediaGridFragment);
                mMediaGridFragment.clearCheckedItems();
                setupBaseLayout();

                mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
                ft.add(R.id.media_browser_container, mMediaItemFragment, MediaItemFragment.TAG);
                ft.addToBackStack(null);
                ft.commit();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            }
        } else {
            // tablet: update the edit fragment with the new item
            mMediaEditFragment.loadMedia(mediaId);
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        stopAnimatingRefreshButton();

        getSupportMenuInflater().inflate(R.menu.media, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        startAnimatingRefreshButton();

        return true;
    }

    private void startAnimatingRefreshButton() {
        if (mRefreshMenuItem != null && mMediaGridFragment != null && mMediaGridFragment.isRefreshing())
            startAnimatingRefreshButton(mRefreshMenuItem);
    }

    private void stopAnimatingRefreshButton() {
        if (mRefreshMenuItem != null)
            stopAnimatingRefreshButton(mRefreshMenuItem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
                setupBaseLayout();
                return true;
            }
        } else if (itemId == R.id.menu_new_media) {
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
            stopAnimatingRefreshButton(mRefreshMenuItem);
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
        } else if (itemId == R.id.menu_refresh) {

            if (mMediaGridFragment != null) {
                mMediaGridFragment.refreshMediaFromServer(0, false);
                startAnimatingRefreshButton();
            }
            return true;
        } else if (itemId == R.id.menu_edit_media) {
            String mediaId = mMediaItemFragment.getMediaId();
            FragmentManager fm = getSupportFragmentManager();

            if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
                // phone layout: hide item details, show and update edit fragment
                FragmentTransaction ft = fm.beginTransaction();

                if (mMediaItemFragment.isVisible())
                    ft.hide(mMediaItemFragment);

                mMediaEditFragment = MediaEditFragment.newInstance(mediaId);
                ft.add(R.id.media_browser_container, mMediaEditFragment, MediaEditFragment.TAG);
                ft.addToBackStack(null);
                ft.commit();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                // tablet layout: update edit fragment
                mMediaEditFragment.loadMedia(mediaId);
            }

            if (mSearchView != null)
                mSearchView.clearFocus();

        } else if (itemId == R.id.menu_delete) {
            if (mMediaEditFragment != null && mMediaEditFragment.isInLayout()) {
                String mediaId = mMediaEditFragment.getMediaId();
                launchConfirmDeleteDialog(mediaId);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchConfirmDeleteDialog(final String mediaId) {
        if (mediaId == null)
            return;

        Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_media)
                .setCancelable(true)
                .setPositiveButton(R.string.delete, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<String> ids = new ArrayList<String>(1);
                        ids.add(mediaId);
                        onDeleteMedia(ids);
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_delete).setVisible(Utils.isTablet());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onMediaItemListDownloaded() {
        stopAnimatingRefreshButton();

        if (mMediaItemFragment != null && mMediaItemFragment.isInLayout()) {
            mMediaItemFragment.loadDefaultMedia();
        }
    }

    @Override
    public void onMediaItemListDownloadStart() {
        // start animation delayed to prevent glitch where the progress spinner
        // disappears when it is started and stopped and then restarted too quickly in succession
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                startAnimatingRefreshButton();
            }
        }, 500);
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
        if (mMediaGridFragment != null)
            mMediaGridFragment.search(newText);

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

        mMenu.findItem(R.id.menu_refresh).setVisible(false);
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

        mMenu.findItem(R.id.menu_refresh).setVisible(true);
        mMenu.findItem(R.id.menu_new_media).setVisible(true);
        return true;
    }

    @Override
    public void onDeleteMedia(final List<String> ids) {
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        List<String> sanitizedIds = new ArrayList<String>(ids.size());

        if (mMediaItemFragment != null && mMediaItemFragment.isVisible()) {
            // phone layout: pop the item fragment if it's visible
            getSupportFragmentManager().popBackStack();
        }

        //Make sure there are no media in "uploading"
        for (String currentID : ids) {
            if (MediaUtils.canDeleteMedia(blogId, currentID))
                sanitizedIds.add(currentID);
        }

        if( sanitizedIds.size() != ids.size()) {
            if ( ids.size() == 1  )
                Toast.makeText(this, R.string.wait_until_upload_completes, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, R.string.cannot_delete_multi_media_items, Toast.LENGTH_LONG).show();
        }

        // mark items for delete without actually deleting items yet,
        // and then refresh the grid
        WordPress.wpDB.setMediaFilesMarkedForDelete(blogId, sanitizedIds);

        if (mMediaEditFragment != null) {
            String mediaId = mMediaEditFragment.getMediaId();
            for (String id : sanitizedIds) {
                if (id.equals(mediaId)) {
                    mMediaEditFragment.loadMedia(null);
                    break;
                }
            }
        }
        mMediaGridFragment.clearCheckedItems();
        mMediaGridFragment.refreshMediaFromDB();

        startMediaDeleteService();
    }

    public void onSavedEdit(String mediaId, boolean result) {
        if (mMediaEditFragment != null && mMediaEditFragment.isVisible() && result) {
            FragmentManager fm = getSupportFragmentManager();
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
    public void onMultiSelectChange(int count) {
        mMultiSelectCount = count;

        if (count > 0 && mActionMode == null) {
            mActionMode = getSherlock().startActionMode(this);
        } else if (count == 0 && mActionMode != null) {
            mActionMode.finish();
        }

        // update contextual action bar title
        if (count > 0 && mActionMode != null)
            mActionMode.setTitle(count + " selected");

        // update contextual action bar menu items
        if (mActionMode != null)
            mActionMode.invalidate();

        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (mMenuDrawer.isMenuVisible()) {
            super.onBackPressed();
        } else if (fm.getBackStackEntryCount() > 0) {
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
                    getSupportFragmentManager().popBackStack();
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.media_multiselect, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (mActionMode != null) {
            if (mMultiSelectCount == 1) {
                menu.findItem(R.id.media_multiselect_actionbar_post).setVisible(true);
                menu.findItem(R.id.media_multiselect_actionbar_gallery).setVisible(false);
            } else if (mMultiSelectCount > 1) {
                menu.findItem(R.id.media_multiselect_actionbar_post).setVisible(false);
                menu.findItem(R.id.media_multiselect_actionbar_gallery).setVisible(true);
            } else {
                menu.findItem(R.id.media_multiselect_actionbar_post).setVisible(false);
                menu.findItem(R.id.media_multiselect_actionbar_gallery).setVisible(false);
            }
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.media_multiselect_actionbar_post:
                handleNewPost();
                return true;
            case R.id.media_multiselect_actionbar_gallery:
                handleMultiSelectPost();
                return true;
            case R.id.media_multiselect_actionbar_trash:
                handleMultiSelectDelete();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        cancelMultiSelect();
    }

    private void cancelMultiSelect() {
        mMediaGridFragment.clearCheckedItems();
    }

    private void handleNewPost() {
        if (mMediaGridFragment == null)
            return;

        Post newPost = new Post(WordPress.getCurrentBlog().getLocalTableBlogId(), false);
        if (newPost.getId() < 0) {
            return;
        }

        ArrayList<String> ids = mMediaGridFragment.getCheckedItems();

        Intent i = new Intent(this, EditPostActivity.class);
        i.setAction(EditPostContentFragment.NEW_MEDIA_POST);
        i.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getId());
        i.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        i.putExtra(EditPostContentFragment.NEW_MEDIA_POST_EXTRA, ids.get(0));
        startActivity(i);
    }

    private void handleMultiSelectDelete() {
        Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_multi_media)
                .setCancelable(true)
                .setPositiveButton(R.string.delete, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<String> ids = mMediaGridFragment.getCheckedItems();
                        onDeleteMedia(ids);
                        mMediaGridFragment.refreshSpinnerAdapter();
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleMultiSelectPost() {
        if (mMediaGridFragment == null)
            return;

        ArrayList<String> ids = mMediaGridFragment.getCheckedItems();

        Post newPost = new Post(WordPress.getCurrentBlog().getLocalTableBlogId(), false);
        if (newPost.getId() < 0) {
            return;
        }
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getId());
        i.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        i.setAction(EditPostContentFragment.NEW_MEDIA_GALLERY);
        i.putExtra(EditPostContentFragment.NEW_MEDIA_GALLERY_EXTRA_IDS, ids);
        startActivity(i);
    }
}
