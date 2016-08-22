package org.wordpress.android.fluxc.network;

import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;

import java.util.List;

public interface MediaNetworkListener {
    /**
     * Notifies that an error has occurred while attempting to interface with network media.
     */
    void onMediaError(MediaAction cause, Exception error);

    /**
     * Media has been pulled successfully from a remote source.
     */
    void onMediaPulled(MediaAction cause, List<MediaModel> pulledMedia, List<Exception> errors);

    /**
     * Media changes have been successfully pushed to remote.
     */
    void onMediaPushed(MediaAction cause, List<MediaModel> pushedMedia, List<Exception> errors);

    /**
     * Media has been successfully deleted from remote.
     */
    void onMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia, List<Exception> errors);

    /**
     * Reports media upload progress as it is transmitted over the network.
     */
    void onMediaUploadProgress(MediaAction cause, MediaModel media, float progress);
}
