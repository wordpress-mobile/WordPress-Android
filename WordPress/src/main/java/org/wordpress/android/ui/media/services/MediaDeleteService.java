package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
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

import javax.inject.Inject;

/**
 * A service for deleting media. Only one media item is deleted at a time.
 */

public class MediaDeleteService extends Service {
    private SiteModel mSite;
    private MediaModel mDeleteInProgress;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        mDeleteInProgress = null;
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

        // start deleting queued media
        deleteNextInQueue();

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
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (mDeleteInProgress != null) {
            if (event.isError()) {
                switch (event.error.type) {
                    case UNAUTHORIZED:
                        handleUnauthorizedError();
                        break;
                    case MEDIA_NOT_FOUND:
                        handleMediaNotFoundError();
                        break;
                    default:
                        handleUnknownError();
                        break;
                }
            } else if (event.media != null && !event.media.isEmpty() && matchesInProgressMedia(event.media.get(0))) {
                switch (event.cause) {
                    case DELETE_MEDIA:
                    case REMOVE_MEDIA:
                        AppLog.v(T.MEDIA, "Successfully deleted " + mDeleteInProgress.getTitle());
                        break;
                    case UPDATE_MEDIA:
                        AppLog.v(T.MEDIA, mDeleteInProgress.getTitle() + " marked as deleted.");
                        break;
                }
                mDeleteInProgress = null;
            }
        }

        deleteNextInQueue();
    }

    /**
     * Delete next media item in queue. Only one media item is deleted at a time.
     */
    private void deleteNextInQueue() {
        // waiting for response to current delete request
        if (mDeleteInProgress != null) {
            return;
        }

        // site is missing or there are no more items to delete, stop service
        if (mSite == null || (mDeleteInProgress = mMediaStore.getNextSiteMediaToDelete(mSite)) == null) {
            AppLog.v(T.MEDIA, "No more media items in delete queue. Stopping service.");
            stopSelf();
            return;
        }

        AppLog.v(T.MEDIA, "Deleting " + mDeleteInProgress.getTitle() + " (id=" + mDeleteInProgress.getMediaId() + ")");

        // dispatch delete action
        MediaListPayload payload = new MediaListPayload(MediaAction.DELETE_MEDIA, mSite, mediaList(mDeleteInProgress));
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
    }

    /**
     * Stop delete service until authorized to perform actions on site.
     */
    private void handleUnauthorizedError() {
        AppLog.v(T.MEDIA, "Unauthorized site access. Stopping service.");
        stopSelf();
    }

    /**
     * Remove media from local database.
     */
    private void handleMediaNotFoundError() {
        if (mDeleteInProgress == null) {
            return;
        }

        MediaListPayload payload = new MediaListPayload(MediaAction.REMOVE_MEDIA, mSite, mediaList(mDeleteInProgress));
        mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(payload));
    }

    /**
     * Mark media as deleted to prevent continuous delete requests.
     */
    private void handleUnknownError() {
        if (mDeleteInProgress == null) {
            return;
        }

        mDeleteInProgress.setDeleted(true);
        MediaListPayload payload = new MediaListPayload(MediaAction.UPDATE_MEDIA, mSite, mediaList(mDeleteInProgress));
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(payload));
    }

    /**
     * Compares site ID and media ID to determine if a given media item matches the current media item being deleted.
     */
    private boolean matchesInProgressMedia(final MediaModel media) {
        if (media == null || mDeleteInProgress == null) {
            return media == mDeleteInProgress;
        }

        return media.getSiteId() == mDeleteInProgress.getSiteId() &&
               media.getMediaId() == mDeleteInProgress.getMediaId();
    }

    /**
     * Creates a list for single media item.
     */
    private ArrayList<MediaModel> mediaList(final MediaModel media) {
        ArrayList<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        return mediaList;
    }
}
