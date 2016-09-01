package org.wordpress.android.fluxc.network;

import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;

import java.util.List;

public interface MediaNetworkListener {
    enum MediaNetworkError {
        NONE,
        MEDIA_NOT_FOUND,
        RESPONSE_PARSE_ERROR,
        UNKNOWN
    }

    /**
     * Notifies that an error has occurred while attempting to interface with network media.
     */
    void onMediaError(MediaAction cause, MediaModel media, MediaNetworkError error);

    /**
     * Media has been pulled successfully from a remote source.
     */
    void onMediaFetched(MediaAction cause, List<MediaModel> fetchedMedia);

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
