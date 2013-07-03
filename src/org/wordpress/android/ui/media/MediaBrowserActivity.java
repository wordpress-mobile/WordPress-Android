package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaItemListFragment.MediaItemListListener;
import org.wordpress.android.ui.posts.ViewPostFragment;

public class MediaBrowserActivity extends WPActionBarActivity implements MediaItemListListener {

    private MediaItemListFragment mMediaItemListFragment;
    private MediaItemFragment mMediaItemFragment;

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
    };
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if(itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popMediaItemDetails();
                return true;
            }
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
        if(mMediaItemFragment != null && mMediaItemFragment.isInLayout()) {
            mMediaItemFragment.loadDefaultMedia();
        }
    }
    
}
