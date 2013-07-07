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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.ui.media.MediaUploadListAdapter.OnButtonClickListener;
import org.wordpress.android.ui.media.MediaUtils.LaunchCameraCallback;
import org.wordpress.android.ui.media.MediaUtils.RequestCode;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.MediaUploadService;

public class MediaUploadFragment extends Fragment implements LaunchCameraCallback, OnButtonClickListener {

    private String mMediaCapturePath = "";
    private ListView mListView;
    private MediaUploadListAdapter mAdapter;
    private Cursor mCursor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        View view = inflater.inflate(R.layout.media_upload_fragment, container, false);
        
        mListView = (ListView) view.findViewById(R.id.media_upload_listview);

        Button uploadImageButton = (Button) view.findViewById(R.id.media_upload_image_button);
        Button uploadVideoButton = (Button) view.findViewById(R.id.media_upload_video_button);
        registerForContextMenu(uploadImageButton);
        registerForContextMenu(uploadVideoButton);
        uploadImageButton.setOnClickListener(MediaUploadButtonListener);
        uploadVideoButton.setOnClickListener(MediaUploadButtonListener);
        
        Button uploadOtherButton = (Button) view.findViewById(R.id.media_upload_other_button);
        uploadOtherButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                MediaUtils.launchBrowseFiles(MediaUploadFragment.this);
            }
        });

        return view;
    }

    private OnClickListener MediaUploadButtonListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            getActivity().openContextMenu(v);
        }
    };
    
    @Override
    public void onResume() {
        super.onResume();
        
        mCursor = fetchMediaForUpload();
        mAdapter = new MediaUploadListAdapter(getActivity(), mCursor, 0, this);
        mListView.setAdapter(mAdapter);
        registerForContextMenu(mListView);
        mListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.add(0, 0, 0, getResources().getText(R.string.media_remove_from_queue));
            }
        });
        
        
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
                refresh();
            }
        }
    };

    private Cursor fetchMediaForUpload() {
        Cursor cursor = null;
        Blog blog = WordPress.getCurrentBlog();
        if(blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            cursor = WordPress.wpDB.getMediaFilesForUpload(blogId);
        }
        return cursor;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (data != null || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO) {
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
                    mMediaCapturePath = null;
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
        
        refresh();
     
        startMediaUploadService();
    }

    private void startMediaUploadService() {
        getActivity().startService(new Intent(getActivity(), MediaUploadService.class));
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mMediaCapturePath = mediaCapturePath;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.media_upload_image_button) {
            menu.add(1, 0, 0, getResources().getText(R.string.select_photo));
            if (DeviceUtils.getInstance().hasCamera(getActivity().getApplicationContext())) {
                menu.add(1, 1, 0, getResources().getText(R.string.take_photo));
            }
        } else if (v.getId() == R.id.media_upload_video_button) {
            menu.add(1, 2, 0, getResources().getText(R.string.select_video));
            if (DeviceUtils.getInstance().hasCamera(getActivity().getApplicationContext())) {
                menu.add(1, 3, 0, getResources().getText(R.string.take_video));
            }
        }

    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int groupId = item.getGroupId();
        
        if (groupId == 0) {
            handleListItemSelected(item);
        } else if (groupId == 1) {
            switch (item.getItemId()) {
            case 0:
                MediaUtils.launchPictureLibrary(this);
                return true;
            case 1:
                MediaUtils.launchCamera(this, this);
                return true;
            }
        }
        return false;
    }

    private void handleListItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        String blogId = cursor.getString(cursor.getColumnIndex("blogId"));
        WordPress.wpDB.deleteMediaFile(blogId, mediaId);
        refresh();
    }

    @Override
    public void onRetryClicked(String mediaId) {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        WordPress.wpDB.updateMediaUploadState(blogId, mediaId, "queued");
        refresh();
        startMediaUploadService();
    }

    @Override
    public void onEditClicked(String mediaId) {
        // TODO Auto-generated method stub
    }
    
    public void refresh() {
        mCursor = fetchMediaForUpload();
        mAdapter.swapCursor(mCursor);
    }
}
