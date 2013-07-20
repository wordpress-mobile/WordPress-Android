
package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaGridFragment.Filter;
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.util.MediaDeleteService;

public class MediaBrowserActivity extends WPActionBarActivity implements MediaGridListener, MediaItemFragmentCallback, 
    OnQueryTextListener, OnActionExpandListener, MediaEditFragmentCallback, View.OnClickListener  {

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MediaEditFragment mMediaEditFragment;
    
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private MenuItem mRefreshMenuItem;
    private int mMultiSelectCount;
    
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

        mMediaGridFragment = (MediaGridFragment) fm.findFragmentById(R.id.mediaGridFragment);
        
        mMediaItemFragment = (MediaItemFragment) fm.findFragmentByTag(MediaItemFragment.TAG);
        if (mMediaItemFragment != null)
            ft.hide(mMediaGridFragment);
        
        mMediaEditFragment = (MediaEditFragment) fm.findFragmentByTag(MediaEditFragment.TAG);
        if (mMediaEditFragment != null && !mMediaEditFragment.isInLayout())
            ft.hide(mMediaItemFragment);
            
        
        ft.commit();
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

    @Override
    protected void onResume() {
        super.onResume();
        startMediaDeleteService();
    };
    
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
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaGridFragment);
            setupBaseLayout();
            
            mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
            ft.add(R.id.media_browser_container, mMediaItemFragment, MediaItemFragment.TAG);
            ft.addToBackStack(null);
            ft.commit();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } else {
            mMediaEditFragment.loadMedia(mediaId);
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
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
            Intent i = new Intent(this, MediaUploadActivity.class);
            i.putExtra("id", WordPress.currentBlog.getId());

            startActivity(i);

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
        } else if (itemId == R.id.menu_save_media) {
            mMediaEditFragment.editMedia();

        } else if (itemId == R.id.menu_delete) {
            Builder builder = new AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_delete_media)
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mMediaItemFragment != null && mMediaItemFragment.isVisible()) {
                                ArrayList<String> ids = new ArrayList<String>(1);
                                ids.add(mMediaItemFragment.getMediaId());
                                onDeleteMedia(ids);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
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
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onQueryTextChange("");
        if (mMediaGridFragment != null) {
            mMediaGridFragment.setFilterVisibility(View.VISIBLE);
            mMediaGridFragment.setFilter(Filter.ALL);
        }

        return true;
    }

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
    
    public void onEditCompleted(String mediaId, boolean result) {
        if (mMediaEditFragment != null && mMediaEditFragment.isVisible() && result) {
            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack();
            
            mMediaEditFragment.loadMedia(mediaId);
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
        if (isInMultiSelect()) {
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
        // TODO Auto-generated method stub
        
    }
}
