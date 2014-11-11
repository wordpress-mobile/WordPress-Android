package org.wordpress.android.ui.media.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents images stored on the device.
 */

public class DeviceImageMediaContent extends MediaContent {
    private String mImageId;
    private String mDisplayName;
    private String mImageUri;
    private String mThumbUri;

    public static final Parcelable.Creator<DeviceImageMediaContent> CREATOR =
    new Parcelable.Creator<DeviceImageMediaContent>() {
        public DeviceImageMediaContent createFromParcel(Parcel in) {
            return new DeviceImageMediaContent(in);
        }

        public DeviceImageMediaContent[] newArray(int size) {
            return new DeviceImageMediaContent[size];
        }
    };

    public DeviceImageMediaContent(Parcel parcel) {
        List<String> data = new ArrayList<String>();
        parcel.readStringList(data);

        while(data.size() > 0) {
            String contentData = data.remove(0);
            String key = contentData.substring(0, contentData.indexOf('='));
            String value = contentData.substring(contentData.indexOf('=') + 1, contentData.length());
            writeDataFromParcel(key, value);
        }
    }

    public DeviceImageMediaContent(String imageId) {
        mImageId = imageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<String> stringList = new ArrayList<String>();
        if (mImageId != null) {
            stringList.add("mImageId=" + mImageId);
        }
        if (mDisplayName != null) {
            stringList.add("mDisplayName=" + mDisplayName);
        }
        if (mImageUri != null) {
            stringList.add("mImageUri=" + mImageUri);
        }
        if (mThumbUri != null) {
            stringList.add("mThumbUri=" + mThumbUri);
        }

        dest.writeStringList(stringList);
    }

    @Override
    public MEDIA_TYPE getType() {
        return MEDIA_TYPE.DEVICE_IMAGE;
    }

    @Override
    public String getId() {
        return mImageId;
    }

    @Override
    public String getData() {
        return mImageUri;
    }

    public String getName() {
        return mDisplayName;
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

    private void writeDataFromParcel(String key, String value) {
        if (key.equals("mImageId")) {
            mImageId = value;
        } else if (key.equals("mImageUri")) {
            mImageUri = value;
        } else if (key.equals("mDisplayName")) {
            mDisplayName = value;
        } else if (key.equals("mThumbUri")) {
            mThumbUri = value;
        }
    }
}
