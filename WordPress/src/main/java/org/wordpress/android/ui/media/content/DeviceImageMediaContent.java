package org.wordpress.android.ui.media.content;

/**
 * Represents images stored on the device.
 */

public class DeviceImageMediaContent extends MediaContent {
    private String mDisplayName;
    private String mImageUri;
    private String mThumbUri;

    public DeviceImageMediaContent(String uri) {
        mImageUri = uri;
    }

    @Override
    public MEDIA_TYPE getType() {
        return MEDIA_TYPE.DEVICE_IMAGE;
    }

    @Override
    public String getName() {
        return mDisplayName;
    }

    @Override
    public String getData() {
        return mImageUri;
    }

    public void setName(String name) {
        mDisplayName = name;
    }

    public void setImageUri(String imageUri) {
        mImageUri = imageUri;
    }

    public void setThumbUri(String thumbUri) {
        mThumbUri = thumbUri;
    }

    public String getThumbUri() {
        return mThumbUri;
    }
}
