package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

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
        if (mDeleteInProgress == null) {
            deleteNextInQueue();
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

        deleteNextInQueue();
    }

    /**
     * @return true to continue deleting from queue
     */
    private boolean handleOnMediaChangedError(MediaStore.OnMediaChanged event) {
        switch (event.error.type) {
            case UNAUTHORIZED:
                // stop delete service until authorized to perform actions on site
                AppLog.v(T.MEDIA, "Unauthorized site access. Stopping service.");
                stopSelf();
                mDeleteInProgress = null;
                return false;
            case NULL_MEDIA_ARG:
                // shouldn't happen, get back to deleting the queue
                mDeleteInProgress = null;
                break;
            case MEDIA_NOT_FOUND:
                // remove media from local database
                MediaListPayload removePayload = newPayload(MediaAction.REMOVE_MEDIA);
                mDispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(removePayload));
                break;
            case PARSE_ERROR:
                // TODO
                break;
            default:
                // mark media as deleted to prevent continuous delete requests
                mDeleteInProgress.setDeleted(true);
                MediaListPayload updatePayload = newPayload(MediaAction.UPDATE_MEDIA);
                mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(updatePayload));
                return false;
        }
        return true;
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

        // dispatch delete action
        AppLog.v(T.MEDIA, "Deleting " + mDeleteInProgress.getTitle() + " (id=" + mDeleteInProgress.getMediaId() + ")");
        MediaListPayload payload = newPayload(MediaAction.DELETE_MEDIA);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
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

    private MediaListPayload newPayload(@NonNull MediaAction action) {
        ArrayList<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(mDeleteInProgress);
        return new MediaListPayload(action, mSite, mediaList);
    }
}
