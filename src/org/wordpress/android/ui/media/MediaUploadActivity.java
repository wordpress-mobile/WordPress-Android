package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;


public class MediaUploadActivity extends SherlockFragmentActivity {

    private MediaUploadFragment mMediaUploadFragment;

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
            finish();   
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
        getSupportMenuInflater().inflate(R.menu.media_upload, menu);
        return true;
    }
    
}
