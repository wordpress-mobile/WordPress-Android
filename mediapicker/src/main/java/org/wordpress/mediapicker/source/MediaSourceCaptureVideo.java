package org.wordpress.mediapicker.source;

import android.os.Parcel;

import org.wordpress.mediapicker.R;

public class MediaSourceCaptureVideo extends MediaSourceCaptureImage {
    @Override
    protected int getOverlayResource() {
        return R.drawable.video;
    }

    public static final Creator<MediaSourceCaptureVideo> CREATOR =
            new Creator<MediaSourceCaptureVideo>() {
                public MediaSourceCaptureVideo createFromParcel(Parcel in) {
                    return new MediaSourceCaptureVideo();
                }

                public MediaSourceCaptureVideo[] newArray(int size) {
                    return new MediaSourceCaptureVideo[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
