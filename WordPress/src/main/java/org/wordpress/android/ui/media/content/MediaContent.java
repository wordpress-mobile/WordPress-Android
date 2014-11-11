package org.wordpress.android.ui.media.content;

import android.os.Parcelable;

/**
 * Data model for media content.
 *
 * Media content requires an ID, name (display name), and data (source).
 */

public abstract class MediaContent implements Parcelable {
    public enum MEDIA_TYPE {
        INVALID(0), CAPTURE(1), DEVICE_IMAGE(2), DEVICE_VIDEO(3), WEB_IMAGE(4), WEB_VIDEO(5), COUNT(6);

        private int mValue;

        MEDIA_TYPE(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }
    }

    public abstract MEDIA_TYPE getType();

    public abstract String getId();

    public abstract String getName();

    public abstract String getData();
}
