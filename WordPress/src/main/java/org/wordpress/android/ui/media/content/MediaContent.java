package org.wordpress.android.ui.media.content;

/**
 * Data model for media content.
 */
public abstract class MediaContent {
    public enum MEDIA_TYPE {
        INVALID, CAPTURE, DEVICE_IMAGE, DEVICE_VIDEO, WEB_IMAGE, WEB_VIDEO
    }

    public MEDIA_TYPE getType() {
        return MEDIA_TYPE.INVALID;
    }
}
