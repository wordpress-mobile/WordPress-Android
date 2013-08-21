package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.posts.EditMediaGalleryFragment;

public class MediaGalleryActivity extends WPActionBarActivity {

    
    private EditMediaGalleryFragment mMediaGalleryEditFragment;
    private MediaGallerySettingsFragment mMediaGallerySettingsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(R.string.media_gallery_edit);
        
        createMenuDrawer(R.layout.media_gallery_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        mMediaGalleryEditFragment = (EditMediaGalleryFragment) fm.findFragmentById(R.id.mediaGalleryEditFragment);
        
        mMediaGallerySettingsFragment = (MediaGallerySettingsFragment) fm.findFragmentById(R.id.mediaGallerySettingsFragment);
        
        

    }
}
