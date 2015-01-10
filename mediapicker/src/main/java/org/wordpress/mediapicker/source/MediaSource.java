package org.wordpress.mediapicker.source;

import org.wordpress.mediapicker.MediaItem;

import java.util.List;

public interface MediaSource {
    public List<MediaItem> getMediaItems();
}
