package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A service for deleting media. Only one media item is deleted at a time.
 */

@AndroidEntryPoint
public class MediaDeleteService extends Service {
    public static final String SITE_KEY = "mediaSite";
    public static final String MEDIA_LIST_KEY = "mediaList";

    public class MediaDeleteBinder extends Binder {
        public MediaDeleteService getService() {
            return MediaDeleteService.this;
        }

        public void addMediaToDeleteQueue(@NonNull MediaModel media) {
            getDeleteQueue().add(media);
            deleteNextInQueue();
        }

        public void removeMediaFromDeleteQueue(@NonNull MediaModel media) {
            getDeleteQueue().remove(media);
            deleteNextInQueue();
        }
    }

    private final IBinder mBinder = new MediaDeleteBinder();

    private SiteModel mSite; // required for payloads
    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private MediaModel mCurrentDelete;
    private List<MediaModel> mDeleteQueue;
    private List<MediaModel> mCompletedItems;

    @Override
    public void onCreate() {
        super.onCreate();
        mDispatcher.register(this);
        mCurrentDelete = null;
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        // TODO: if event not dispatched for ongoing delete cancel it and dispatch cancel event
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // stop service if no site is given
        if (intent == null || !intent.hasExtra(SITE_KEY)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mSite = (SiteModel) intent.getSerializableExtra(SITE_KEY);
        mDeleteQueue = (List<MediaModel>) intent.getSerializableExtra(MEDIA_LIST_KEY);

        // start deleting queued media
        deleteNextInQueue();

        // only run while app process is running, allows service to be stopped by user force closing the app
        return START_NOT_STICKY;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        // event for unknown media, ignoring
        if (event.mediaList == null || event.mediaList.isEmpty() || !matchesInProgressMedia(event.mediaList.get(0))) {
            AppLog.w(T.MEDIA, "Media event not recognized: " + event.mediaList);
            return;
        }

        if (event.isError()) {
            handleOnMediaChangedError(event);
        } else {
            handleMediaChangedSuccess(event);
        }

        deleteNextInQueue();
    }

    public @NonNull List<MediaModel> getDeleteQueue() {
        if (mDeleteQueue == null) {
            mDeleteQueue = new ArrayList<>();
        }
        return mDeleteQueue;
    }

    public @NonNull List<MediaModel> getCompletedItems() {
        if (mCompletedItems == null) {
            mCompletedItems = new ArrayList<>();
        }
        return mCompletedItems;
    }

    public boolean isMediaBeingDeleted(@NonNull MediaModel media) {
        if (mDeleteQueue != null) {
            for (MediaModel deletingMedia : mDeleteQueue) {
                if (deletingMedia.getId() == media.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAnyMediaBeingDeleted() {
        return mDeleteQueue != null && mDeleteQueue.size() > 0;
    }

    private void handleMediaChangedSuccess(@NonNull OnMediaChanged event) {
        switch (event.cause) {
            case DELETE_MEDIA:
                if (mCurrentDelete != null) {
                    AppLog.d(T.MEDIA, mCurrentDelete.getTitle() + " successfully deleted!");
                    completeCurrentDelete();
                }
                break;
            case REMOVE_MEDIA:
                if (mCurrentDelete != null) {
                    AppLog.d(T.MEDIA, "Successfully deleted " + mCurrentDelete.getTitle());
                    completeCurrentDelete();
                }
                break;
        }
    }

    private void handleOnMediaChangedError(@NonNull OnMediaChanged event) {
        MediaModel media = event.mediaList.get(0);

        switch (event.error.type) {
            case AUTHORIZATION_REQUIRED:
                AppLog.v(T.MEDIA, "Authorization required. Stopping MediaDeleteService.");
                // stop delete service until authorized to perform actions on site
                stopSelf();
                break;
            case NULL_MEDIA_ARG:
                // shouldn't happen, get back to deleting the queue
                AppLog.d(T.MEDIA, "Null media argument supplied, skipping current delete.");
                completeCurrentDelete();
                break;
            case NOT_FOUND:
                if (media == null) {
                    break;
                }
                AppLog.d(T.MEDIA, "Could not find media (id=" + media.getMediaId() + "). on remote");
                // remove media from local database
                mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mCurrentDelete));
                break;
            case PARSE_ERROR:
                AppLog.d(T.MEDIA, "Error parsing reponse to " + event.cause.toString() + ".");
                completeCurrentDelete();
                break;
            default:
                completeCurrentDelete();
                break;
        }
    }

    /**
     * Delete next media item in queue. Only one media item is deleted at a time.
     */
    private void deleteNextInQueue() {
        // waiting for response to current delete request
        if (mCurrentDelete != null) {
            AppLog.i(T.MEDIA, "Ignoring request to deleteNextInQueue, only one media item can be deleted at a time.");
            return;
        }

        // somehow lost our reference to the site, stop service
        if (mSite == null) {
            AppLog.i(T.MEDIA, "Unexpected state, site is null. Stopping MediaDeleteService.");
            stopSelf();
            return;
        }

        mCurrentDelete = nextMediaToDelete();

        // no more items to delete, stop service
        if (mCurrentDelete == null) {
            AppLog.v(T.MEDIA, "No more media items to delete. Stopping MediaDeleteService.");
            stopSelf();
            return;
        }

        dispatchDeleteAction(mCurrentDelete);
    }

    private void dispatchDeleteAction(@NonNull MediaModel media) {
        AppLog.v(T.MEDIA, "Deleting " + media.getTitle() + " (id=" + media.getMediaId() + ")");
        MediaPayload payload = new MediaPayload(mSite, media);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
    }

    /**
     * Compares site ID and media ID to determine if a given media item matches the current media item being deleted.
     */
    private boolean matchesInProgressMedia(final @NonNull MediaModel media) {
        return mCurrentDelete != null
               && media.getLocalSiteId() == mCurrentDelete.getLocalSiteId()
               && media.getMediaId() == mCurrentDelete.getMediaId();
    }

    /**
     * @return the next item in the queue to delete, null if queue is empty
     */
    private MediaModel nextMediaToDelete() {
        if (!getDeleteQueue().isEmpty()) {
            return getDeleteQueue().get(0);
        }
        return null;
    }

    /**
     * Moves current delete from the queue into the completed list.
     */
    private void completeCurrentDelete() {
        if (mCurrentDelete != null) {
            getCompletedItems().add(mCurrentDelete);
            getDeleteQueue().remove(mCurrentDelete);
            mCurrentDelete = null;
        }
    }
}
