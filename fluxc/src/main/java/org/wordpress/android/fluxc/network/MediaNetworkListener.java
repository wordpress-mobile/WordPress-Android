package org.wordpress.android.fluxc.network;

import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;

import java.util.List;

public interface MediaNetworkListener {
    enum MediaNetworkError {
        NONE,
        MEDIA_NOT_FOUND,
        UNKNOWN;
        public Exception exception;
    }

    /**
     * Notifies that an error has occurred while attempting to interface with network media.
     */
    void onMediaError(MediaAction cause, MediaModel media, MediaNetworkError error);

    /**
     * Media has been pulled successfully from a remote source.
     */
    void onMediaPulled(MediaAction cause, List<MediaModel> pulledMedia);

    /**
     * Media changes have been successfully pushed to remote.
     */
    void onMediaPushed(MediaAction cause, List<MediaModel> pushedMedia);

    /**
     * Media has been successfully deleted from remote.
     */
    void onMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia);

    /**
     * Reports media upload progress as it is transmitted over the network.
     */
    void onMediaUploadProgress(MediaAction cause, MediaModel media, float progress);
}
