package org.wordpress.android.ui.media;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.media.MediaUtils.LaunchCameraCallback;
import org.wordpress.android.ui.media.MediaUtils.RequestCode;
import org.wordpress.android.util.MediaUploadService;

/**
 * An invisible fragment in charge of launching the right intents to camera, video, and image library.
 * Also queues up media for upload and listens to notifications from the upload service.
 */
public class MediaAddFragment extends Fragment implements LaunchCameraCallback {

    private String mMediaCapturePath = "";
    private MediaAddFragmentCallback mCallback;
    
    public interface MediaAddFragmentCallback {
        public void onMediaAdded(String mediaId);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // This view doesn't really matter as this fragment is invisible
        return inflater.inflate(R.layout.actionbar_add_media_cell, container, false);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (MediaAddFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + MediaAddFragment.class.getSimpleName());
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(MediaUploadService.MEDIA_UPLOAD_INTENT_NOTIFICATION));

        startMediaUploadService();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaUploadService.MEDIA_UPLOAD_INTENT_NOTIFICATION)) {
                String mediaId = intent.getStringExtra(MediaUploadService.MEDIA_UPLOAD_INTENT_NOTIFICATION_EXTRA);
                mCallback.onMediaAdded(mediaId);
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (data != null || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO) {
            String path = null;
            
            switch (requestCode) {
            case RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_VIDEO_LIBRARY:
                Uri imageUri = data.getData();
                path = getRealPathFromURI(imageUri);
                queueFileForUpload(path);
                break;
            case RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    path = mMediaCapturePath;
                    mMediaCapturePath = null;
                    queueFileForUpload(path);
                }
                break;
            case RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri videoUri = data.getData();
                    path = getRealPathFromURI(videoUri);
                    queueFileForUpload(path);
                }
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
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }
    
    private void queueFileForUpload(String path) {
        if (path == null || path.equals("")) {
            Toast.makeText(getActivity(), "Error opening file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Blog blog = WordPress.getCurrentBlog();
        
        String fileName = new String(path).replaceAll("^.*/([A-Za-z0-9_-]+)\\.\\w+$", "$1");
        String fileType = new String(path).replaceAll(".*\\.(\\w+)$", "$1").toLowerCase();
        
        MediaFile mediaFile = new MediaFile();
        mediaFile.setBlogId(String.valueOf(blog.getBlogId()));
        mediaFile.setFileName(fileName + "." + fileType);
        mediaFile.setFilePath(path);
        mediaFile.setUploadState("queued");
        mediaFile.setDateCreatedGMT(System.currentTimeMillis());
        mediaFile.setMediaId(String.valueOf(System.currentTimeMillis()));
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType);
        mediaFile.setMIMEType(mimeType);
        mediaFile.save();
        
        mCallback.onMediaAdded(mediaFile.getMediaId());
     
        startMediaUploadService();
    }

    private void startMediaUploadService() {
        getActivity().startService(new Intent(getActivity(), MediaUploadService.class));
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mMediaCapturePath = mediaCapturePath;
    }

    public void launchCamera(){
        MediaUtils.launchCamera(this, this);
    }

    public void launchVideoCamera() {
        MediaUtils.launchVideoCamera(this);
    }
    
    public void launchPictureVideoLibrary() {
        MediaUtils.launchPictureVideoLibrary(this);
    }
    
    public void addToQueue(String mediaId) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        WordPress.wpDB.updateMediaUploadState(blogId, mediaId, "queued");
        startMediaUploadService();
    }
    
}
