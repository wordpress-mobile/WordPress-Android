package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.media.MediaEditFragment.MediaEditFragmentCallback;
import org.wordpress.android.ui.media.MediaUploadFragment.MediaUploadFragmentCallback;


public class MediaUploadActivity extends SherlockFragmentActivity implements MediaEditFragmentCallback, MediaUploadFragmentCallback {

    private MediaUploadFragment mMediaUploadFragment;
    private MediaEditFragment mMediaEditFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.media_upload_title);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        setContentView(R.layout.media_upload_activity);
        
        FragmentManager fm = getSupportFragmentManager();
        mMediaUploadFragment = (MediaUploadFragment) fm.findFragmentById(R.id.mediaUploadFragment);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            } else {
                finish();
            }
            return true;
        } else if(itemId == R.id.menu_media_upload_clear_uploaded) {
            String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
            WordPress.wpDB.clearMediaUploaded(blogId);
            
            if (mMediaUploadFragment != null) 
                mMediaUploadFragment.refresh();
            
            return true;
        }
        return false;
    }

    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        if (mMediaEditFragment != null && !mMediaEditFragment.isInLayout() && mMediaEditFragment.isVisible()) {
            getSupportMenuInflater().inflate(R.menu.media_edit, menu);
        } else {
            getSupportMenuInflater().inflate(R.menu.media_upload, menu);
        }
        
        return true;
    }

    @Override
    public void onEditMediaItem(String mediaId) {
        FragmentManager fm = getSupportFragmentManager();
        
        if (mMediaEditFragment == null || !mMediaEditFragment.isInLayout()) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(mMediaUploadFragment);
            mMediaEditFragment = MediaEditFragment.newInstance(mediaId);
            ft.add(R.id.media_upload_container, mMediaEditFragment);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            mMediaEditFragment.loadMedia(mediaId);
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        invalidateOptionsMenu();   
    }

    @Override
    public void onPause(Fragment fragment) {
        invalidateOptionsMenu();        
    }
}
