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
    OnQueryTextListener, OnActionExpandListener, MediaEditFragmentCallback  {

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MediaEditFragment mMediaEditFragment;
    
    private MenuItem refreshMenuItem;
    
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    
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
        
        mMediaGridFragment = (MediaGridFragment) fm.findFragmentById(R.id.mediaGridFragment);
        
        mMediaItemFragment = (MediaItemFragment) fm.findFragmentById(R.id.mediaItemFragment);
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    protected void onResume() {
        super.onResume();
        startMediaDeleteService();
    };
    
    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        if(mMediaGridFragment != null) {
            mMediaGridFragment.refreshMediaFromServer(0);
            startAnimatingRefreshButton();
        }
    };
    
    @Override
    public void onMediaItemSelected(String mediaId) {
        FragmentManager fm = getSupportFragmentManager();
        
        if (mMediaItemFragment == null || !mMediaItemFragment.isInLayout()) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaGridFragment);
            mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
            ft.add(R.id.media_browser_container, mMediaItemFragment);
            ft.addToBackStack(null);
            ft.commit();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } else {
            mMediaItemFragment.loadMedia(mediaId);
        }
        
        if (mSearchView != null)
            mSearchView.clearFocus();
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        stopAnimatingRefreshButton();
        
        MenuInflater inflater = getSupportMenuInflater();
        
        // show a separate menu when the media item fragment is in phone layout and visible
        if (mMediaItemFragment != null && !mMediaItemFragment.isInLayout() && mMediaItemFragment.isVisible()) {
            inflater.inflate(R.menu.media_details, menu);
        } else if (mMediaEditFragment != null && !mMediaEditFragment.isInLayout() && mMediaEditFragment.isVisible()) {
            inflater.inflate(R.menu.media_edit, menu);
        } else {
            inflater.inflate(R.menu.media, menu);
            
            refreshMenuItem = menu.findItem(R.id.menu_refresh);
            startAnimatingRefreshButton();
        }
        return true;
    }

    private void startAnimatingRefreshButton() {
        if (refreshMenuItem != null && mMediaGridFragment != null && mMediaGridFragment.isRefreshing())
            startAnimatingRefreshButton(refreshMenuItem);
    }
    
    private void stopAnimatingRefreshButton() {
        if (refreshMenuItem != null)
            stopAnimatingRefreshButton(refreshMenuItem);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
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

            if(mMediaGridFragment != null) {
                mMediaGridFragment.refreshMediaFromServer(0);
                startAnimatingRefreshButton();
            }
            return true;
        } else if (itemId == R.id.menu_edit_media) {
            String mediaId = mMediaItemFragment.getMediaId();
            FragmentManager fm = getSupportFragmentManager();
            
            if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(mMediaItemFragment);
                mMediaEditFragment = MediaEditFragment.newInstance(mediaId);
                ft.add(R.id.media_browser_container, mMediaEditFragment);
                ft.addToBackStack(null);
                ft.commit();
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            } else {
                mMediaItemFragment.loadMedia(mediaId);
            }
            
            if (mSearchView != null)
                mSearchView.clearFocus();
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
        
        if(mMediaItemFragment != null && mMediaItemFragment.isInLayout()) {
            mMediaItemFragment.loadDefaultMedia();
        }
    }
    

    @Override
    public void onMediaItemListDownloadStart() {
        startAnimatingRefreshButton();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(mMediaGridFragment != null)
            mMediaGridFragment.search(query);
        
        mSearchView.clearFocus();
        
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(mMediaGridFragment != null)
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
        
        if(mMediaItemFragment != null && mMediaItemFragment.isVisible()) {
            getSupportFragmentManager().popBackStack();
        }

        // mark items for delete without actually deleting items yet,
        // and then refresh the grid
        WordPress.wpDB.setMediaFilesMarkedForDelete(blogId, ids);
        mMediaGridFragment.refreshMediaFromDB();
        
        startMediaDeleteService();
    }
    
    private void startMediaDeleteService() {
        startService(new Intent(this, MediaDeleteService.class));
    }    
}
