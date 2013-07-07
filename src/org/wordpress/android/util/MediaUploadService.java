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
import android.util.Log;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.UploadMediaTask.Callback;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;

/**
 * A service for uploading media files from the media browser.
 * Only one file is uploaded at a time.
 */
public class MediaUploadService extends Service {

    // time to wait before trying to upload the next file
    private static final int UPLOAD_WAIT_TIME = 500;

    /** Intent to listen for notifications **/
    public static final String MEDIA_UPLOAD_INTENT_NOTIFICATION = "MEDIA_UPLOAD_INTENT_NOTIFICATION";
    
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
        mHandler.post(FetchQueueTask);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Runnable FetchQueueTask = new Runnable() {
        
        @Override
        public void run() {
            Cursor cursor = getQueue();
            if ((cursor.getCount() == 0 || mContext == null) && !mUploadInProgress) {
                MediaUploadService.this.stopSelf();
                return;
            } else {
                if (!mUploadInProgress) {
                    uploadMediaFile(cursor);
                } else {
                    mHandler.postDelayed(this, UPLOAD_WAIT_TIME);
                }
            }
        }
    };
    

    private void cancelOldUploads() {
        // There should be no media files with an upload state of 'uploading' at the start of this service.
        // Since we won't be able to receive notifications for these, set them to 'failed'.
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        WordPress.wpDB.setMediaUploadingToFailed(blogId);

        sendUpdateBroadcast();
    }
    
    private Cursor getQueue() {
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        return WordPress.wpDB.getMediaQueue(blogId);
    }
    
    private void uploadMediaFile(Cursor cursor) {

        if (!cursor.moveToFirst())
            return;
        
        mUploadInProgress = true;
        
        int blogId = Integer.valueOf(cursor.getString((cursor.getColumnIndex("blogId"))));
        final String blogIdStr = String.valueOf(blogId);
        
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
                mUploadInProgress = false;
                WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, "uploaded");
                sendUpdateBroadcast();
                mHandler.post(FetchQueueTask);
            }
            
            @Override
            public void onFailure() {
                mUploadInProgress = false;
                WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, "failed");
                sendUpdateBroadcast();
                mHandler.post(FetchQueueTask);
            }
        });
        
        WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, "uploading");
        sendUpdateBroadcast();
        
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        task.execute(apiArgs) ;
        
        mHandler.post(FetchQueueTask);
    }

    private void sendUpdateBroadcast() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
        lbm.sendBroadcast(new Intent(MEDIA_UPLOAD_INTENT_NOTIFICATION));
    }
    
}
