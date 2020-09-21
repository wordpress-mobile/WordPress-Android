package org.wordpress.android.editor.gutenberg;

import org.wordpress.android.util.helpers.MediaFile;

public interface StorySaveMediaListener {
    void onMediaSaveReattached(String localId, float currentProgress);
    void onMediaSaveSucceeded(String localId, MediaFile mediaFile);
    void onMediaSaveProgress(String localId, float progress);
    void onMediaSaveFailed(String localId);
}
