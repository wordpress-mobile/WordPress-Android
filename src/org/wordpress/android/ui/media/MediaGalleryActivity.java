package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.posts.EditMediaGalleryFragment;
import org.wordpress.android.util.Utils;

public class MediaGalleryActivity extends SherlockFragmentActivity {

    
    private EditMediaGalleryFragment mMediaGalleryEditFragment;
    private MediaGallerySettingsFragment mMediaGallerySettingsFragment;
    private SlidingUpPanelLayout mSlidingPanelLayout;
    private boolean mIsPanelCollapsed = true;

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
        
        mMediaGalleryEditFragment = (EditMediaGalleryFragment) fm.findFragmentById(R.id.mediaGalleryEditFragment);
        
        mMediaGallerySettingsFragment = (MediaGallerySettingsFragment) fm.findFragmentById(R.id.mediaGallerySettingsFragment);
        
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
}
