package org.wordpress.android.ui.media.content;

/**
 * Represents media content that will be captured. Can be of image or video type.
 */

public class CaptureMediaContent extends MediaContent {
    public static final int CAPTURE_TYPE_NONE  = 0;
    public static final int CAPTURE_TYPE_IMAGE = 1;
    public static final int CAPTURE_TYPE_VIDEO = 2;

    private int mCaptureType;

    private CaptureMediaContent() {
        mCaptureType = CAPTURE_TYPE_NONE;
    }

    public CaptureMediaContent(int captureType) {
        mCaptureType = captureType;
    }

    @Override
    public MEDIA_TYPE getType() {
        return MEDIA_TYPE.CAPTURE;
    }

    public void setCaptureType(int type) {
        mCaptureType = type;
    }

    public int getCaptureType() {
        return mCaptureType;
    }

    public boolean isImageCapture() {
        return mCaptureType == CAPTURE_TYPE_IMAGE;
    }

    public boolean isVideoCapture() {
        return mCaptureType == CAPTURE_TYPE_VIDEO;
    }
}
