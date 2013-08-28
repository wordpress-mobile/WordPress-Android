package org.wordpress.android.ui.media;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaGallery;
import org.wordpress.android.ui.media.MediaGallerySettingsFragment.MediaGallerySettingsCallback;
import org.wordpress.android.util.Utils;

public class MediaGalleryActivity extends SherlockFragmentActivity implements MediaGallerySettingsCallback {

    public static final int REQUEST_CODE = 3000;
    
    // params for the gallery
    public static final String PARAMS_MEDIA_GALLERY = "PARAMS_MEDIA_GALLERY";
    
    // result of the gallery
    public static final String RESULT_MEDIA_GALLERY = "RESULT_MEDIA_GALLERY";
    
    private MediaGalleryEditFragment mMediaGalleryEditFragment;
    private MediaGallerySettingsFragment mMediaGallerySettingsFragment;
    private SlidingUpPanelLayout mSlidingPanelLayout;
    private boolean mIsPanelCollapsed = true;

    private MediaGallery mMediaGallery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(R.string.media_gallery_edit);
        
        setContentView(R.layout.media_gallery_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        
        FragmentManager fm = getSupportFragmentManager();
        
        mMediaGallery = (MediaGallery) getIntent().getSerializableExtra(PARAMS_MEDIA_GALLERY);
        if (mMediaGallery == null)
            mMediaGallery = new MediaGallery();

        mMediaGalleryEditFragment = (MediaGalleryEditFragment) fm.findFragmentById(R.id.mediaGalleryEditFragment);
        mMediaGalleryEditFragment.setMediaIds(mMediaGallery.getIds());
        
        mMediaGallerySettingsFragment = (MediaGallerySettingsFragment) fm.findFragmentById(R.id.mediaGallerySettingsFragment);
        mMediaGallerySettingsFragment.setRandom(mMediaGallery.isRandom());
        mMediaGallerySettingsFragment.setNumColumns(mMediaGallery.getNumColumns());
        mMediaGallerySettingsFragment.setType(mMediaGallery.getType());
        
        mSlidingPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.media_gallery_root);
        if (mSlidingPanelLayout != null) {
            mSlidingPanelLayout.setDragView(mMediaGallerySettingsFragment.getDragView());
            mSlidingPanelLayout.setPanelHeight((int) Utils.dpToPx(48));
            mSlidingPanelLayout.setPanelSlideListener(new PanelSlideListener() {
                
                @Override
                public void onPanelSlide(View panel, float slideOffset) {
                    
                }
                
                @Override
                public void onPanelExpanded(View panel) {
                    mMediaGallerySettingsFragment.onPanelExpanded();
                    mIsPanelCollapsed = false;
                }
                
                @Override
                public void onPanelCollapsed(View panel) {
                    mMediaGallerySettingsFragment.onPanelCollapsed();
                    mIsPanelCollapsed = true;
                }
            });
        }

    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.media_gallery, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add_media) {
            handleAddMedia();
            return true;
        } else if (item.getItemId() == R.id.menu_save) {
            handleSaveMedia();
            return true;
        }
            
        return super.onOptionsItemSelected(item);
    }
    
    private void handleAddMedia() {
        // TODO Auto-generated method stub
        
    }


    private void handleSaveMedia() {
        
        Intent intent = new Intent();
        ArrayList<String> ids = mMediaGalleryEditFragment.getMediaIds();
        boolean isRandom = mMediaGallerySettingsFragment.isRandom();
        int numColumns = mMediaGallerySettingsFragment.getNumColumns();
        String type = mMediaGallerySettingsFragment.getType();
        
        mMediaGallery.setIds(ids);
        mMediaGallery.setRandom(isRandom);
        mMediaGallery.setNumColumns(numColumns);
        mMediaGallery.setType(type);
        
        intent.putExtra(RESULT_MEDIA_GALLERY, mMediaGallery);
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public void onBackPressed() {
        
        if (Utils.isTablet()) {
            super.onBackPressed();
        } else {
            if (mSlidingPanelLayout != null && !mIsPanelCollapsed)
                mSlidingPanelLayout.collapsePane();
            else
                super.onBackPressed();
        }
            
    }


    @Override
    public void onReverseClicked() {
        mMediaGalleryEditFragment.reverseIds();
    }
}
