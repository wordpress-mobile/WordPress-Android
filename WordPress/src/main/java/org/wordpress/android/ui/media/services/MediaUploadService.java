package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * A service for uploading media. Only one media item is uploaded at a time.
 */

public class MediaUploadService extends Service {
    private SiteModel mSite;
    private MediaModel mUploadInProgress;

    // time to wait before trying to upload the next file
    private static final int UPLOAD_WAIT_TIME = 1000;

    private Handler mHandler = new Handler();

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        mUploadInProgress = null;
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mSite = (SiteModel) intent.getSerializableExtra(WordPress.SITE);
        }

        // start uploading queued media
        uploadNextInQueue();

        // only run while app process is running, allows service to be stopped by user force closing the app
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not supported
        return null;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        if (mUploadInProgress == null) {
            uploadNextInQueue();
            return;
        }

        // event for unknown media, ignoring
        if (event.media == null || !matchesInProgressMedia(event.media)) {
            return;
        }

        if (event.isError()) {
            if (!handleOnMediaUploadedError(event)) {
                return;
            }
        } else {
            if (event.completed) {
                // media upload completed
                AppLog.i(T.MEDIA, mUploadInProgress.getTitle() + " uploaded successfully!");
                mUploadInProgress = null;
            } else {
                AppLog.v(T.MEDIA, "Uploaded " + event.progress * 100.f + "% of " + mUploadInProgress.getTitle());
                return;
            }
        }

        uploadNextInQueue();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (mUploadInProgress == null) {
            uploadNextInQueue();
            return;
        }

        // event for unknown media, ignoring
        if (event.media == null || event.media.isEmpty() || !matchesInProgressMedia(event.media.get(0))) {
            return;
        }

        if (event.isError()) {
            if (!handleOnMediaChangedError(event)) {
                return;
            }
        } else {
            switch (event.cause) {
                case UPDATE_MEDIA:
                    mUploadInProgress = null;
                    break;
            }
        }

        uploadNextInQueue();
    }

    private boolean handleOnMediaChangedError(MediaStore.OnMediaChanged event) {
        switch (event.error.type) {
            case NULL_MEDIA_ARG:
            case MALFORMED_MEDIA_ARG:
                mUploadInProgress = null;
                return true;
        }
        return true;
    }

    private boolean handleOnMediaUploadedError(MediaStore.OnMediaUploaded event) {
        switch (event.error.type) {
            case FS_READ_PERMISSION_DENIED:
                break;
            case NULL_MEDIA_ARG:
                break;
            case MALFORMED_MEDIA_ARG:
                break;
            case UNAUTHORIZED:
                break;
            default:
                break;
        }
        mUploadInProgress = null;
        // TODO CrashlyticsUtils.logException(throwable, ExceptionType.SPECIFIC, T.MEDIA, errorMessage);
        return true;
    }

    private void uploadNextInQueue() {
        if (mUploadInProgress != null) {
            return;
        }

        if (mSite == null) {
            AppLog.v(T.MEDIA, "No site specified, stopping service.");
            stopSelf();
            return;
        }

        List<MediaModel> uploadQueue = mMediaStore.getLocalSiteMedia(mSite);
        if (uploadQueue == null || uploadQueue.isEmpty()) {
            AppLog.v(T.MEDIA, "No more media items in upload queue. Stopping service.");
            stopSelf();
            return;
        }

        mUploadInProgress = uploadQueue.get(0);

        // dispatch upload action
        MediaStore.UploadMediaPayload payload = new MediaStore.UploadMediaPayload(mSite, mUploadInProgress);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    /**
     * Returns whether the service has any media uploads in progress or queued.
     */
//    public boolean hasUploads() {
//        if (mUploadInProgress) {
//            return true;
//        } else {
//            Cursor queueCursor = getQueue();
//            return (queueCursor == null || queueCursor.getCount() > 0);
//        }
//    }

    /**
     * Cancel the upload with the given id, whether it's currently uploading or queued.
     * @param mediaId the id of the media item
     * @param delete whether to delete the item from the queue or mark it as failed so it can be retried later
     */
//    public void cancelUpload(String mediaId, boolean delete) {
//        if (mediaId.equals(mCurrentUploadMediaId)) {
//            // The media item is currently uploading - abort the upload process
//            mUploadInProgress = false;
//        } else {
//            // Remove the media item from the upload queue
//            if (delete) {
//                ArrayList<MediaModel> mediaList = new ArrayList<>();
//                MediaModel media = new MediaModel();
//                media.setMediaId(Long.valueOf(mediaId));
//                media.setSiteId(mSite.getSiteId());
//                mediaList.add(media);
//                MediaListPayload payload = new MediaListPayload(MediaAction.DELETE_MEDIA, mSite, mediaList);
//                mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
//            }
//        }
//    }

//    private Runnable mFetchQueueTask = new Runnable() {
//        @Override
//        public void run() {
//            Cursor cursor = getQueue();
//            try {
//                if ((cursor == null || cursor.getCount() == 0) && !mUploadInProgress) {
//                    MediaUploadService.this.stopSelf();
//                    return;
//                } else {
//                    if (mUploadInProgress) {
//                        mHandler.postDelayed(this, UPLOAD_WAIT_TIME);
//                    } else {
//                        uploadMediaFile(cursor);
//                    }
//                }
//            } finally {
//                if (cursor != null)
//                    cursor.close();
//            }
//
//        }
//    };

//    private void cancelOldUploads() {
//        // There should be no media files with an upload state of 'uploading' at the start of this service.
//        // Since we won't be able to receive notifications for these, set them to 'failed'.
//        String blogId = String.valueOf(mSite.getId());
//        WordPress.wpDB.setMediaUploadingToFailed(blogId);
//    }

//    private void uploadMediaFile(Cursor cursor) {
//        if (!cursor.moveToFirst())
//            return;
//
//        mUploadInProgress = true;
//
//        // TODO: use MediaModel
////        final String blogIdStr = cursor.getString((cursor.getColumnIndex(WordPressDB.COLUMN_NAME_BLOG_ID)));
////        final String mediaId = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MEDIA_ID));
////        String fileName = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME));
////        String filePath = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH));
////        String mimeType = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_MIME_TYPE));
////
////        MediaFile mediaFile = new MediaFile();
////        mediaFile.setBlogId(blogIdStr);
////        mediaFile.setFileName(fileName);
////        mediaFile.setFilePath(filePath);
////        mediaFile.setMimeType(mimeType);
//
////        mCurrentUploadMediaId = mediaId;
//
////        mCurrentUploadMediaTask = new ApiHelper.UploadMediaTask(getApplicationContext(), mSite, mediaFile,
////                new ApiHelper.UploadMediaTask.Callback() {
////            @Override
////            public void onSuccess(String remoteId, String remoteUrl, String secondaryId) {
////                // once the file has been uploaded, update the local database entry (swap the id with the remote id)
////                // and download the new one
////                WordPress.wpDB.updateMediaLocalToRemoteId(blogIdStr, mediaId, remoteId);
////                EventBus.getDefault().post(new MediaEvents.MediaUploadSucceeded(blogIdStr, mediaId,
////                        remoteId, remoteUrl, secondaryId));
////                fetchMediaFile(remoteId);
////            }
////
////            @Override
////            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
////                WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, MediaUploadState.FAILED);
////                mUploadInProgress = false;
////                mCurrentUploadMediaId = "";
////
////                MediaEvents.MediaUploadFailed event;
////                if (errorMessage == null) {
////                    event = new MediaEvents.MediaUploadFailed(mediaId, getString(R.string.upload_failed), true);
////                } else {
////                    event = new MediaEvents.MediaUploadFailed(mediaId, errorMessage);
////                }
////
////                EventBus.getDefault().post(event);
////                mHandler.post(mFetchQueueTask);
////
////                // Only log the error if it's not caused by the network (internal inconsistency)
////                if (errorType != ErrorType.NETWORK_XMLRPC) {
////                    CrashlyticsUtils.logException(throwable, ExceptionType.SPECIFIC, T.MEDIA, errorMessage);
////                }
////            }
////
////            @Override
////            public void onProgressUpdate(float progress) {
////                EventBus.getDefault().post(new MediaEvents.MediaUploadProgress(mediaId, progress));
////            }
////        });
//
////        WordPress.wpDB.updateMediaUploadState(blogIdStr, mediaId, MediaUploadState.UPLOADING);
////        mCurrentUploadMediaTask.execute();
////        mHandler.post(mFetchQueueTask);
//    }

    /**
     * Stop upload service until authorized to perform actions on site.
     */
    private void handleUnauthorizedError() {
        AppLog.v(T.MEDIA, "Unauthorized site access. Stopping service.");
        stopSelf();
    }

    /**
     * Compares site ID and media ID to determine if a given media item matches the current media item being deleted.
     */
    private boolean matchesInProgressMedia(final MediaModel media) {
        if (media == null || mUploadInProgress == null) {
            return media == mUploadInProgress;
        }

        return media.getSiteId() == mUploadInProgress.getSiteId() &&
                media.getMediaId() == mUploadInProgress.getMediaId();
    }
}
