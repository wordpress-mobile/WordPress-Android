package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.util.DeviceUtils;


public class MediaUploadActivity extends SherlockActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.media_upload_title);
        
        ActionBar actionBar = getSupportActionBar();
//        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        setContentView(R.layout.media_upload_activity);
        
        Button uploadImageButton = (Button) findViewById(R.id.media_upload_image_button);
        Button uploadVideoButton = (Button) findViewById(R.id.media_upload_video_button);
        registerForContextMenu(uploadImageButton);
        registerForContextMenu(uploadVideoButton);
        
        uploadImageButton.setOnClickListener(MediaUploadButtonListener);
        uploadVideoButton.setOnClickListener(MediaUploadButtonListener);
        
    }
    
    private OnClickListener MediaUploadButtonListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            openContextMenu(v);
        }
    };
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);     
        finish();   
        
        return false;
    }
    
    
    
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.media_upload_image_button) {
            menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
            if (DeviceUtils.getInstance().hasCamera(getApplicationContext())) {
                menu.add(0, 1, 0, getResources().getText(R.string.take_photo));
            }
        } else if (v.getId() == R.id.media_upload_video_button) {
            menu.add(0, 2, 0, getResources().getText(R.string.select_video));
            if (DeviceUtils.getInstance().hasCamera(getApplicationContext())) {
                menu.add(0, 3, 0, getResources().getText(R.string.take_video));
            }
        }


    }
    
}
