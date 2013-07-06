package org.wordpress.android.ui.media;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import org.wordpress.android.ui.media.MediaGridFragment.MediaGridListener;
import org.wordpress.android.ui.media.MediaItemFragment.MediaItemFragmentCallback;
import org.wordpress.android.ui.posts.ViewPostFragment;

public class MediaBrowserActivity extends WPActionBarActivity implements MediaGridListener, MediaItemFragmentCallback, 
    OnQueryTextListener, OnActionExpandListener  {

    private MediaGridFragment mMediaGridFragment;
    private MediaItemFragment mMediaItemFragment;
    private MenuItem refreshMenuItem;
    
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private Spinner mSpinner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            Log.e("WordPress", "MediaBrowserActivity DB is null - finishing MediaBrowser");
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
        
        String[] filters = new String[] {
                getResources().getString(R.string.all),
                getResources().getString(R.string.images),
                getResources().getString(R.string.unattached) };
        mSpinner = (Spinner) findViewById(R.id.filterSpinner);
        if (mSpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filters);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);
        }

    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    
    @Override
    public void onMediaItemSelected(String mediaId) {
        FragmentManager fm = getSupportFragmentManager();
        
        if (mMediaItemFragment == null || !mMediaItemFragment.isInLayout()) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaGridFragment);
            mSpinner.setVisibility(View.GONE);
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
            mSearchMenuItem.setOnActionExpandListener(this);
            mSearchMenuItem.expandActionView();
            
            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);
            
            return true;
        } else if (itemId == R.id.menu_refresh) {

            if(mMediaGridFragment != null) {
                mMediaGridFragment.refreshMediaFromServer();
                startAnimatingRefreshButton();
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void popMediaItemDetails() {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.mediaItemFragment);
        if (!mSpinner.isShown()) {
            mSpinner.setVisibility(View.VISIBLE);
        }
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
    public void onPauseMediaItemFragment() {
        if (!mSpinner.isShown()) {
            mSpinner.setVisibility(View.VISIBLE);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onResumeMediaItemFragment() {
        invalidateOptionsMenu();
    }


    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onQueryTextChange("");
        return true;
    }
    
}
