package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.media.services.MediaEvents.MediaChanged;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.CrashlyticsUtils.ExceptionType;
import org.wordpress.android.util.helpers.MediaFile;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;
import org.xmlrpc.android.ApiHelper.GetMediaItemTask;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * A service for uploading media files from the media browser.
 * Only one file is uploaded at a time.
 */
public class MediaUploadService extends Service {
    // time to wait before trying to upload the next file
    private static final int UPLOAD_WAIT_TIME = 1000;

    private static MediaUploadService mInstance;

    private Context mContext;
    private Handler mHandler = new Handler();

    private boolean mUploadInProgress;
    private ApiHelper.UploadMediaTask mCurrentUploadMediaTask;
    private String mCurrentUploadMediaId;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        mContext = this.getApplicationContext();
        mUploadInProgress = false;

        cancelOldUploads();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mHandler.post(mFetchQueueTask);
    }

    public static MediaUploadService getInstance() {
        return mInstance;
    }

    public void processQueue() {
        mHandler.post(mFetchQueueTask);
    }

    /**
     * Returns whether the service has any media uploads in progress or queued.
     */
    public boolean hasUploads() {
        if (mUploadInProgress) {
            return true;
        } else {
            Cursor queueCursor = getQueue();
            return (queueCursor == null || queueCursor.getCount() > 0);
        }
    }

    /**
     * Cancel the upload with the given id, whether it's currently uploading or queued.
     * @param mediaId the id of the media item
     * @param delete whether to delete the item from the queue or mark it as failed so it can be retried later
     */
    public void cancelUpload(String mediaId, boolean delete) {
        if (mediaId.equals(mCurrentUploadMediaId)) {
            // The media item is currently uploading - abort the upload process
            mCurrentUploadMediaTask.cancel(true);
            mUploadInProgress = false;
        } else {
            // Remove the media item from the upload queue
            if (WordPress.getCurrentBlog() != null) {
                String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
                if (delete) {
                    WordPress.wpDB.deleteMediaFile(blogId, mediaId);
                } else {
                    WordPress.wpDB.updateMediaUploadState(blogId, mediaId, MediaUploadState.FAILED);
                }
            }
        }
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

        if (WordPress.getCurrentBlog() != null) {
            String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
            WordPress.wpDB.setMediaUploadingToFailed(blogId);
        }
    }

    private Cursor getQueue() {
        if (WordPress.getCurrentBlog() == null)
            return null;

        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        return WordPress.wpDB.getMediaUploadQueue(blogId);
    }

    private void uploadMediaFile(Cursor cursor) {
        if (!cursor.moveToFirst())
            return;

        mUploadInProgress = true;

        final String blogIdStr = cursor.getString((cursor.getColumnIndex(WordPressDB.COLUMN_NAME_BLOG_ID)));
        final String mediaId = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID));
        String fileName = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME));
        String filePath = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH));
        String mimeType = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MIME_TYPE));

        MediaFile mediaFile = new MediaFile();
        mediaFile.setBlogId(blogIdStr);
        mediaFile.setFileName(fileName);
        mediaFile.setFilePath(filePath);
        mediaFile.setMimeType(mimeType);

        mCurrentUploadMediaId = mediaId;

        mCurrentUploadMediaTask = new ApiHelper.UploadMediaTask(mContext, mediaFile,
                new ApiHelper.UploadMediaTask.Callback() {
            @Override
            public void onSuccess(String remoteId, String remoteUrl, String secondaryId) {
                // once the file has been uploaded, update the local database entry (swap the id with the remote id)
                // and download the new one
                WordPress.wpDB.updateMediaLocalToRemoteId(blogIdStr, mediaId, remoteId);
                EventBus.getDefault().post(new MediaEvents.MediaUploadSucceeded(blogIdStr, mediaId,
                        remoteId, remoteUrl, secondaryId));
                fetchMediaFile(remoteId);
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, MediaUploadState.FAILED);
                mUploadInProgress = false;
                mCurrentUploadMediaId = "";

                MediaEvents.MediaUploadFailed event;
                if (errorMessage == null) {
                    event = new MediaEvents.MediaUploadFailed(mediaId, getString(R.string.upload_failed), true);
                } else {
                    event = new MediaEvents.MediaUploadFailed(mediaId, errorMessage);
                }

                EventBus.getDefault().post(event);
                mHandler.post(mFetchQueueTask);

                // Only log the error if it's not caused by the network (internal inconsistency)
                if (errorType != ErrorType.NETWORK_XMLRPC) {
                    CrashlyticsUtils.logException(throwable, ExceptionType.SPECIFIC, T.MEDIA, errorMessage);
                }
            }

            @Override
            public void onProgressUpdate(float progress) {
                EventBus.getDefault().post(new MediaEvents.MediaUploadProgress(mediaId, progress));
            }
        });

        WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, MediaUploadState.UPLOADING);
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        mCurrentUploadMediaTask.execute(apiArgs);
        mHandler.post(mFetchQueueTask);
    }

    private void fetchMediaFile(final String id) {
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        GetMediaItemTask task = new GetMediaItemTask(Integer.valueOf(id),
                new ApiHelper.GetMediaItemTask.Callback() {
            @Override
            public void onSuccess(MediaFile mediaFile) {
                String blogId = mediaFile.getBlogId();
                String mediaId = mediaFile.getMediaId();
                WordPress.wpDB.updateMediaUploadState(blogId, mediaId, MediaUploadState.UPLOADED);
                mUploadInProgress = false;
                mCurrentUploadMediaId = "";
                mHandler.post(mFetchQueueTask);
                EventBus.getDefault().post(new MediaChanged(blogId, mediaId));
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mUploadInProgress = false;
                mCurrentUploadMediaId = "";
                mHandler.post(mFetchQueueTask);
                // Only log the error if it's not caused by the network (internal inconsistency)
                if (errorType != ErrorType.NETWORK_XMLRPC) {
                    CrashlyticsUtils.logException(throwable, ExceptionType.SPECIFIC, T.MEDIA, errorMessage);
                }
            }
        });
        task.execute(apiArgs);
    }
}
