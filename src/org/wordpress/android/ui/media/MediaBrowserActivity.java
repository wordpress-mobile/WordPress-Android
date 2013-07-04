package org.wordpress.android.ui.media;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaItemListFragment.MediaItemListListener;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.ViewPostFragment;

public class MediaBrowserActivity extends WPActionBarActivity implements MediaItemListListener, OnQueryTextListener  {

    private MediaItemListFragment mMediaItemListFragment;
    private MediaItemFragment mMediaItemFragment;
    private MenuItem refreshMenuItem;
    
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d("WordPress", "MediaBrowserActivity started");
        
        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            Log.d("WordPress", "MediaBrowserActivity DB is null - finishing");
            finish();
            return;
        }
        
        setTitle(R.string.media);
        
        createMenuDrawer(R.layout.media_browser_activity);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        
        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        
        mMediaItemListFragment = (MediaItemListFragment) fm.findFragmentById(R.id.mediaItemListFragment);
        mMediaItemListFragment.setListShown(true);
        
        mMediaItemFragment = (MediaItemFragment) fm.findFragmentById(R.id.mediaItemFragment);
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("WordPress", "MediaBrowserActivity onResume");
    }

    @Override
    public void onMediaItemSelected(String mediaId) {
        FragmentManager fm = getSupportFragmentManager();
        
        if (mMediaItemFragment == null || !mMediaItemFragment.isInLayout()) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaItemListFragment);
            mMediaItemFragment = MediaItemFragment.newInstance(mediaId);
            ft.add(R.id.media_browser_container, mMediaItemFragment);
            ft.addToBackStack(null);
            ft.commit();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } else {
            mMediaItemFragment.loadMedia(mediaId);
        }
        
        if(mSearchView != null)
            mSearchView.clearFocus();
        
        if(mSearchMenuItem != null)
            mSearchMenuItem.collapseActionView();
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.media, menu);
        
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        startAnimatingRefreshButton();
        
        return true;
    }

    private void startAnimatingRefreshButton() {
        if(refreshMenuItem != null && mMediaItemListFragment != null && mMediaItemListFragment.isRefreshing())
            startAnimatingRefreshButton(refreshMenuItem);
    }
    
    private void stopAnimatingRefreshButton() {
        if(refreshMenuItem != null)
            stopAnimatingRefreshButton(refreshMenuItem);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popMediaItemDetails();
                return true;
            }
        } else if (itemId == R.id.menu_new_media) {
            Intent i = new Intent(this, MediaUploadActivity.class);
            i.putExtra("id", WordPress.currentBlog.getId());
            
            startActivity(i);
            
            return true;
        } else if (itemId == R.id.menu_search) {

            mSearchMenuItem = item;
            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);
            
            mSearchMenuItem.expandActionView();
            
            return true;
        } else if (itemId == R.id.menu_refresh) {

            if(mMediaItemListFragment != null) {
                mMediaItemListFragment.refreshMediaFromServer();
                startAnimatingRefreshButton();
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void popMediaItemDetails() {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.mediaItemFragment);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMediaItemListDownloaded() {
        
        stopAnimatingRefreshButton();
        
        if(mMediaItemFragment != null && mMediaItemFragment.isInLayout()) {
            mMediaItemFragment.loadDefaultMedia();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(mMediaItemListFragment != null)
            mMediaItemListFragment.search(query);
        
        mSearchView.clearFocus();
        
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(mMediaItemListFragment != null)
            mMediaItemListFragment.search(newText);
        return true;
    }
    
    
}
