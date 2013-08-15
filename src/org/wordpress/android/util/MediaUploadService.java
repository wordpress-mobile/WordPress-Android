package org.wordpress.android.util;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.GetMediaItemTask;
import org.xmlrpc.android.ApiHelper.UploadMediaTask.Callback;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;

/**
 * A service for uploading media files from the media browser.
 * Only one file is uploaded at a time.
 */
public class MediaUploadService extends Service {

    // time to wait before trying to upload the next file
    private static final int UPLOAD_WAIT_TIME = 1000;

    /** Listen to this Intent for when there are updates to the upload queue **/
    public static final String MEDIA_UPLOAD_INTENT_NOTIFICATION = "MEDIA_UPLOAD_INTENT_NOTIFICATION";
    public static final String MEDIA_UPLOAD_INTENT_NOTIFICATION_EXTRA = "MEDIA_UPLOAD_INTENT_NOTIFICATION_EXTRA";
    
    private Context mContext;
    private Handler mHandler = new Handler();
    private boolean mUploadInProgress;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this.getApplicationContext();
        mUploadInProgress = false;
        
        cancelOldUploads();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        mHandler.post(mFetchQueueTask);
    }

    private Runnable mFetchQueueTask = new Runnable() {
        
        @Override
        public void run() {
            Cursor cursor = getQueue();
            try {
                if ((cursor == null || cursor.getCount() == 0 || mContext == null) && !mUploadInProgress) {
                    MediaUploadService.this.stopSelf();
                    return;
                } else {
                    if (mUploadInProgress) {
                        mHandler.postDelayed(this, UPLOAD_WAIT_TIME);
                    } else {
                        uploadMediaFile(cursor);
                    }
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }            
            
        }
    };
    
    private void cancelOldUploads() {
        // There should be no media files with an upload state of 'uploading' at the start of this service.
        // Since we won't be able to receive notifications for these, set them to 'failed'.
        
        if(WordPress.getCurrentBlog() != null){
            String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
            WordPress.wpDB.setMediaUploadingToFailed(blogId);
    
            sendUpdateBroadcast(null);
        }
    }
    
    private Cursor getQueue() {
        if (WordPress.getCurrentBlog() == null)
            return null;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        return WordPress.wpDB.getMediaUploadQueue(blogId);
    }
    
    private void uploadMediaFile(Cursor cursor) {

        if (!cursor.moveToFirst())
            return;
        
        mUploadInProgress = true;
        
        final String blogIdStr = cursor.getString((cursor.getColumnIndex("blogId")));
        final String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
        String filePath = cursor.getString(cursor.getColumnIndex("filePath"));
        String mimeType = cursor.getString(cursor.getColumnIndex("mimeType"));
        
        MediaFile mediaFile = new MediaFile();
        mediaFile.setBlogId(blogIdStr);
        mediaFile.setFileName(fileName);
        mediaFile.setFilePath(filePath);
        mediaFile.setMIMEType(mimeType);

        ApiHelper.UploadMediaTask task = new ApiHelper.UploadMediaTask(mContext, mediaFile, new Callback() {
            
            @Override
            public void onSuccess(String id) {
                // once the file has been uploaded, delete the local database entry and
                // download the new one so that we are up-to-date and so that users can edit it.
                WordPress.wpDB.deleteMediaFile(blogIdStr, mediaId);
                sendUpdateBroadcast(mediaId);
                fetchMediaFile(id);
            }
            
            @Override
            public void onFailure() {
                WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, "failed");
                mUploadInProgress = false;
                sendUpdateBroadcast(mediaId);
                mHandler.post(mFetchQueueTask);
            }
        });
        
        WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, "uploading");
        sendUpdateBroadcast(mediaId);
        
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        task.execute(apiArgs) ;
        
        mHandler.post(mFetchQueueTask);
    }

    private void fetchMediaFile(final String id) {
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        GetMediaItemTask task = new GetMediaItemTask(Integer.valueOf(id), new GetMediaItemTask.Callback() {
            
            @Override
            public void onSuccess(MediaFile mediaFile) {
                String blogId = mediaFile.getBlogId();
                String mediaId = mediaFile.getMediaId();
                WordPress.wpDB.updateMediaUploadState(blogId, mediaId, "uploaded");

                mUploadInProgress = false;
                sendUpdateBroadcast(id);
                mHandler.post(mFetchQueueTask);
            }
            
            @Override
            public void onFailure() {
                mUploadInProgress = false;
                sendUpdateBroadcast(id);
                mHandler.post(mFetchQueueTask);                
            }
        });
        task.execute(apiArgs);
    }

    private void sendUpdateBroadcast(String mediaId) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
        Intent intent = new Intent(MEDIA_UPLOAD_INTENT_NOTIFICATION);
        if (mediaId != null)
            intent.putExtra(MEDIA_UPLOAD_INTENT_NOTIFICATION_EXTRA, mediaId);
        lbm.sendBroadcast(intent);
        
    }
    
}
