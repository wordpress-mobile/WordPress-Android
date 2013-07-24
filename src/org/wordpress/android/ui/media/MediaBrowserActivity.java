
package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetFeatures.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaAddFragment.MediaAddFragmentCallback;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.MediaDeleteService;
import org.wordpress.android.util.WPAlertDialogFragment;

public class MediaBrowserActivity extends WPActionBarActivity implements MediaGridListener, MediaItemFragmentCallback, 
    OnQueryTextListener, OnActionExpandListener, MediaEditFragmentCallback, View.OnClickListener,
    MediaAddFragmentCallback {

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MediaEditFragment mMediaEditFragment;
    private MediaAddFragment mMediaAddFragment;
    private PopupWindow mAddMediaPopup;
    
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private MenuItem mRefreshMenuItem;
    private int mMultiSelectCount;
    private Menu mMenu;
    private FeatureSet mFeatureSet;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
        
        if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isDotcomFlag())
            getFeatureSet();
        
        ft.commit();
        
        setupAddMenuPopup();
    }

    /** Get the feature set for a wordpress.com hosted blog **/
    private void getFeatureSet() {
        new ApiHelper.GetFeatures(new Callback() {

            @Override
            public void onResult(FeatureSet featureSet) {
                mFeatureSet = featureSet;
            }
            
        });
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            setupBaseLayout();
        }
    };
    
    private void setupBaseLayout() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            mMenuDrawer.setDrawerIndicatorEnabled(true);
        } else {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        }
    }

    private void setupAddMenuPopup() {

        String capturePhoto = getResources().getString(R.string.media_add_popup_capture_photo);
        String captureVideo = getResources().getString(R.string.media_add_popup_capture_video);
        String pickFromGallery = getResources().getString(R.string.media_add_popup_pick_from_gallery);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MediaBrowserActivity.this, R.layout.actionbar_add_media_cell,  
                new String[] {capturePhoto, captureVideo, pickFromGallery});
        
        View layoutView = getLayoutInflater().inflate(R.layout.actionbar_add_media, null, false);
        ListView listView = (ListView) layoutView.findViewById(R.id.actionbar_add_media_listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.notifyDataSetChanged();
                
                boolean selfHosted = !WordPress.getCurrentBlog().isDotcomFlag();
                boolean isVideoEnabled = selfHosted || (mFeatureSet != null && mFeatureSet.isVideopressEnabled()); 
                
                if (position == 0) {
                    mMediaAddFragment.launchCamera();
                } else if (position == 1) {
                    if (isVideoEnabled) {
                        mMediaAddFragment.launchVideoCamera();
                    } else {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        String title = getString(R.string.media_no_video_title);
                        String message = getString(R.string.media_no_video_message);
                        WPAlertDialogFragment.newInstance(message, title, false).show(ft, "alert");
                    }
                } else if (position == 2) {
                    if (isVideoEnabled)
                        mMediaAddFragment.launchPictureVideoLibrary();
                    else
                        mMediaAddFragment.launchPictureLibrary();
                }

                mAddMediaPopup.dismiss();
                
            };
        });

        int width = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_width);

        mAddMediaPopup = new PopupWindow(layoutView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAddMediaPopup.setBackgroundDrawable(new ColorDrawable());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService();
    };
    
    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchMenuItem != null)
            mSearchMenuItem.collapseActionView();
    }
    
    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        if (mMediaGridFragment != null) {
            mMediaGridFragment.refreshMediaFromServer(0);
            startAnimatingRefreshButton();
        }
    };

    @Override
    public void onMediaItemSelected(String mediaId) {

        if (mSearchView != null)
            mSearchView.clearFocus();
        
        if(mSearchMenuItem != null)
            mSearchMenuItem.collapseActionView();
        
        FragmentManager fm = getSupportFragmentManager();

        if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
            if (fm.getBackStackEntryCount() == 0) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mMediaGridFragment);
                setupBaseLayout();
                
                mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
                ft.add(R.id.media_browser_container, mMediaItemFragment, MediaItemFragment.TAG);
                ft.addToBackStack(null);
                ft.commit();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            }
        } else {
            mMediaEditFragment.loadMedia(mediaId);
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        stopAnimatingRefreshButton();

        // reset action bar state to default
        ActionBar actionBar = getSupportActionBar();
        
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        
        MenuInflater inflater = getSupportMenuInflater();

        // show a separate menu when the media item fragment is in phone layout and visible
        if (isInMultiSelect()) {
            // show a custom view that emulates contextual action bar (CAB)
            // since CAB is not available in gingerbread
            actionBar.setCustomView(R.layout.media_multiselect_actionbar);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);

            TextView text = (TextView) findViewById(R.id.media_mutliselect_actionbar_count);
            text.setText(mMultiSelectCount + "");
            ImageButton button = (ImageButton) findViewById(R.id.media_multiselect_actionbar_ok);
            button.setOnClickListener(this);
            
            button = (ImageButton) findViewById(R.id.media_multiselect_actionbar_share);
            button.setOnClickListener(this);
            
            button = (ImageButton) findViewById(R.id.media_multiselect_actionbar_post);
            button.setOnClickListener(this);
            
            button = (ImageButton) findViewById(R.id.media_multiselect_actionbar_trash);
            button.setOnClickListener(this);
            
        } else {
            inflater.inflate(R.menu.media, menu);
            mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
            startAnimatingRefreshButton();
        }
        return true;
    }

    private void startAnimatingRefreshButton() {
        if (mRefreshMenuItem != null && mMediaGridFragment != null && mMediaGridFragment.isRefreshing())
            startAnimatingRefreshButton(mRefreshMenuItem);
    }

    private void stopAnimatingRefreshButton() {
        if (mRefreshMenuItem != null)
            stopAnimatingRefreshButton(mRefreshMenuItem);
        if (mAddMediaPopup != null)
            mAddMediaPopup.dismiss();
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
            int y_offset = getResources().getDimensionPixelSize(R.dimen.action_bar_spinner_y_offset);
            mAddMediaPopup.showAsDropDown(view, 0, y_offset);
            
            return true;
        } else if (itemId == R.id.menu_search) {

            mSearchMenuItem = item;
            mSearchMenuItem.setOnActionExpandListener(this);
            mSearchMenuItem.expandActionView();

            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);

            return true;
        } else if (itemId == R.id.menu_refresh) {

            if (mMediaGridFragment != null) {
                mMediaGridFragment.refreshMediaFromServer(0);
                startAnimatingRefreshButton();
            }
            return true;
        } else if (itemId == R.id.menu_edit_media) {
            String mediaId = mMediaItemFragment.getMediaId();
            FragmentManager fm = getSupportFragmentManager();

            if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {

                FragmentTransaction ft = fm.beginTransaction();
                
                if (mMediaItemFragment.isVisible())
                    ft.hide(mMediaItemFragment);
                
                mMediaEditFragment = MediaEditFragment.newInstance(mediaId);
                ft.add(R.id.media_browser_container, mMediaEditFragment, MediaEditFragment.TAG);
                ft.addToBackStack(null);
                ft.commit();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                mMediaEditFragment.loadMedia(mediaId);
            }

            if (mSearchView != null)
                mSearchView.clearFocus();

        }

        return super.onOptionsItemSelected(item);
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
        startAnimatingRefreshButton();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mMediaGridFragment != null)
            mMediaGridFragment.search(query);

        mSearchView.clearFocus();

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mMediaGridFragment != null)
            mMediaGridFragment.search(newText);
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

        mMenu.findItem(R.id.menu_refresh).setVisible(false);
        mMenu.findItem(R.id.menu_new_media).setVisible(false);
        
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onQueryTextChange("");
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
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());

        if (mMediaItemFragment != null && mMediaItemFragment.isVisible()) {
            getSupportFragmentManager().popBackStack();
        }

        // mark items for delete without actually deleting items yet,
        // and then refresh the grid
        WordPress.wpDB.setMediaFilesMarkedForDelete(blogId, ids);
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
        invalidateOptionsMenu();
    }
    
    private boolean isInMultiSelect() {
        return mMultiSelectCount > 0;
    }
    
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (mMenuDrawer.isShown()) {
            super.onBackPressed();
        } else if (isInMultiSelect()) {
            cancelMultiSelect();
        } else if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            setupBaseLayout();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.media_multiselect_actionbar_ok) {
            cancelMultiSelect();
        } else if (v.getId() == R.id.media_multiselect_actionbar_post) {
            handleMultiSelectPost();
        } else if (v.getId() == R.id.media_multiselect_actionbar_share) {
            handleMultiSelectShare();
        } else if (v.getId() == R.id.media_multiselect_actionbar_trash) {
            handleMultiSelectDelete();
        }
    }

    private void cancelMultiSelect() {
        mMediaGridFragment.clearCheckedItems();
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
                mMediaGridFragment.clearCheckedItems();
                mMediaGridFragment.refreshSpinnerAdapter();
            }
        })
        .setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleMultiSelectShare() {
        // TODO Auto-generated method stub
    }

    private void handleMultiSelectPost() {
        if (mMediaGridFragment == null)
            return;
        
        ArrayList<String> ids = mMediaGridFragment.getCheckedItems();
        String galleryIds = "";
        for (String id : ids) {
            galleryIds += id + ",";
        }
        galleryIds = galleryIds.substring(0, galleryIds.length() - 1); // remove last comma
         
        
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra("id", WordPress.currentBlog.getId());
        i.putExtra("isNew", true);
        i.putExtra("content", "[gallery ids=\"" + galleryIds + "\"]" );
        startActivity(i);
    }

    @Override
    public void onMediaAdded(String mediaId) {
        mMediaGridFragment.refreshMediaFromDB();
    }

    @Override
    public void onRetryUpload(String mediaId) {
        mMediaAddFragment.addToQueue(mediaId);
    }
}
