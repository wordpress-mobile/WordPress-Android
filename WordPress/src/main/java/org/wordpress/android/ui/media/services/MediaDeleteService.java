package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaUploadState;
import org.xmlrpc.android.ApiHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A service for deleting media files from the media browser.
 * Only one file is deleted at a time.
 */
public class MediaDeleteService extends Service {
    // time to wait before trying to delete the next file
    private static final int DELETE_WAIT_TIME = 1000;

    private Context mContext;
    private Handler mHandler = new Handler();
    private boolean mDeleteInProgress;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this.getApplicationContext();
        mDeleteInProgress = false;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mHandler.post(mFetchQueueTask);
    }

    private Runnable mFetchQueueTask = new Runnable() {
        @Override
        public void run() {
            Cursor cursor = getQueueItem();
            try {
                if ((cursor == null || cursor.getCount() == 0 || mContext == null) && !mDeleteInProgress) {
                    MediaDeleteService.this.stopSelf();
                    return;
                } else {
                    if (mDeleteInProgress) {
                        mHandler.postDelayed(this, DELETE_WAIT_TIME);
                    } else {
                        deleteMediaFile(cursor);
                    }
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }

        }
    };

    private Cursor getQueueItem() {
        if (WordPress.getCurrentBlog() == null)
            return null;

        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        return WordPress.wpDB.getMediaDeleteQueueItem(blogId);
    }

    private void deleteMediaFile(Cursor cursor) {
        if (!cursor.moveToFirst())
            return;

        mDeleteInProgress = true;

        final String blogId = cursor.getString((cursor.getColumnIndex("blogId")));
        final String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));

        ApiHelper.DeleteMediaTask task = new ApiHelper.DeleteMediaTask(mediaId,
                new ApiHelper.GenericCallback() {
            @Override
            public void onSuccess() {
                // only delete them once we get an ok from the server
                if (WordPress.getCurrentBlog() != null && mediaId != null) {
                    WordPress.wpDB.deleteMediaFile(blogId, mediaId);
                }

                mDeleteInProgress = false;
                mHandler.post(mFetchQueueTask);
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                // Ideally we would do handle the 401 (unauthorized) and 404 (not found) errors,
                // but the XMLRPCExceptions don't seem to give messages when they are thrown.

                // Instead we'll just set them as "deleted" so they don't show up in the delete queue.
                // Otherwise the service will continuously try to delete an item they can't delete.

                WordPress.wpDB.updateMediaUploadState(blogId, mediaId, MediaUploadState.DELETED);

                mDeleteInProgress = false;
                mHandler.post(mFetchQueueTask);
            }
        });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        task.execute(apiArgs) ;

        mHandler.post(mFetchQueueTask);
    }
}
