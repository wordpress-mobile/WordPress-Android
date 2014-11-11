package org.wordpress.android.ui.media.content;

import android.os.Parcel;
import android.os.Parcelable;
/**
 * Represents media content that will be captured. Can be of image or video type.
 */

public class CaptureMediaContent extends MediaContent {
    public static final int CAPTURE_TYPE_NONE  = 0;
    public static final int CAPTURE_TYPE_IMAGE = 1;
    public static final int CAPTURE_TYPE_VIDEO = 2;

    private int mCaptureType;

    public static final Parcelable.Creator<CaptureMediaContent> CREATOR =
    new Parcelable.Creator<CaptureMediaContent>() {
        public CaptureMediaContent createFromParcel(Parcel in) {
            return new CaptureMediaContent(in);
        }

        public CaptureMediaContent[] newArray(int size) {
            return new CaptureMediaContent[size];
        }
    };

    public CaptureMediaContent(Parcel parcel) {
    }

    public CaptureMediaContent(int captureType) {
        mCaptureType = captureType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public MEDIA_TYPE getType() {
        return MEDIA_TYPE.CAPTURE;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getName() {
        return (mCaptureType == CAPTURE_TYPE_IMAGE) ? "Take a picture" : "Shoot a video";
    }

    @Override
    public String getData() {
        return "";
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
