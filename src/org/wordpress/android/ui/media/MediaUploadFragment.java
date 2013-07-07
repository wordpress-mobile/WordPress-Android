package org.wordpress.android.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.UploadMediaTask.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.media.MediaUtils.LaunchCameraCallback;
import org.wordpress.android.ui.media.MediaUtils.RequestCode;
import org.wordpress.android.util.DeviceUtils;

public class MediaUploadFragment extends Fragment implements LaunchCameraCallback {

    private String mMediaCapturePath = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        View view = inflater.inflate(R.layout.media_upload_fragment, container, false);
        

        Button uploadImageButton = (Button) view.findViewById(R.id.media_upload_image_button);
        Button uploadVideoButton = (Button) view.findViewById(R.id.media_upload_video_button);
        registerForContextMenu(uploadImageButton);
        registerForContextMenu(uploadVideoButton);
        
        Button uploadOtherButton = (Button) view.findViewById(R.id.media_upload_other_button);
        uploadOtherButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                MediaUtils.launchBrowseFiles(MediaUploadFragment.this);
            }
        });
        
        uploadImageButton.setOnClickListener(MediaUploadButtonListener);
        uploadVideoButton.setOnClickListener(MediaUploadButtonListener);
        
        
        return view;
    }
    

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (data != null || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO) {
            Bundle extras;

            String path = null;
            
            switch (requestCode) {
            case RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY:
                Uri imageUri = data.getData();
                path = getRealPathFromURI(imageUri);
                queueImageFileForUpload(path);
                break;
            case RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    path = mMediaCapturePath;
                    queueImageFileForUpload(path);
                }
                break;
            case RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY:
                break;
            case RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                break;
            case RequestCode.ACTIVITY_REQUEST_CODE_BROWSE_FILES:
                Uri uri = data.getData();
                path = getRealPathFromURI(uri);
                if (MediaUtils.isSupportedFile(path))
                    queueImageFileForUpload(path);
                else
                    Toast.makeText(getActivity(), "Unsupported file type", Toast.LENGTH_SHORT).show();
                break;
            }
            
            
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getActivity(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    
    private void queueImageFileForUpload(String path) {
        if (path == null)
            return;
        
        String fileName = new String(path).replaceAll("^.*/([A-Za-z0-9_-]+)\\.\\w+$", "$1");
        String fileType = new String(path).replaceAll(".*\\.(\\w+)$", "$1").toLowerCase();
        
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        
        MediaFile mediaFile = new MediaFile();
        mediaFile.setTitle(fileName + "." + fileType);
        mediaFile.setFilePath(path);
        mediaFile.setUploadState("queued");
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType);
        mediaFile.setMIMEType(mimeType);
        mediaFile.save();
        
        ApiHelper.UploadMediaTask task = new ApiHelper.UploadMediaTask(getActivity().getApplicationContext(), mediaFile, new Callback() {
            
            @Override
            public void onSuccess() {
                Toast.makeText(getActivity(), "Successfully uploaded image", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onFailure() {
                Toast.makeText(getActivity(), "OnFailure MediaUploadActivity", Toast.LENGTH_SHORT).show();
            }
        });
        task.execute(apiArgs) ;
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mMediaCapturePath = mediaCapturePath;
    }

    private OnClickListener MediaUploadButtonListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            getActivity().openContextMenu(v);
        }
    };
    
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.media_upload_image_button) {
            menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
            if (DeviceUtils.getInstance().hasCamera(getActivity().getApplicationContext())) {
                menu.add(0, 1, 0, getResources().getText(R.string.take_photo));
            }
        } else if (v.getId() == R.id.media_upload_video_button) {
            menu.add(0, 2, 0, getResources().getText(R.string.select_video));
            if (DeviceUtils.getInstance().hasCamera(getActivity().getApplicationContext())) {
                menu.add(0, 3, 0, getResources().getText(R.string.take_video));
            }
        }

    }
    
    public boolean onContextItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
        case 0:
            MediaUtils.launchPictureLibrary(this);
            return true;
        case 1:
            MediaUtils.launchCamera(this, this);
            return true;
        }
        return false;
    }
    
}
