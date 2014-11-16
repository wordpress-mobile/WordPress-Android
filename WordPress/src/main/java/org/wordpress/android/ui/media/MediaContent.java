package org.wordpress.android.ui.media;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for media content.
 *
 * Media content requires an ID, name (display name), and data (source).
 */

public class MediaContent implements Parcelable {
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

    private MEDIA_TYPE mMediaType;
    private String mTag = "";
    private String mContentId = "";
    private String mContentTitle = "";
    private String mContentDescription = "";
    private Uri mContentUri;
    private Uri mContentPreviewUri;

    public static final Parcelable.Creator<MediaContent> CREATOR =
            new Parcelable.Creator<MediaContent>() {
                public MediaContent createFromParcel(Parcel in) {
                    return new MediaContent(in);
                }

                public MediaContent[] newArray(int size) {
                    return new MediaContent[size];
                }
            };

    public MediaContent(Parcel parcel) {
        List<String> data = new ArrayList<String>();
        parcel.readStringList(data);

        while(data.size() > 0) {
            String contentData = data.remove(0);
            String key = contentData.substring(0, contentData.indexOf('='));
            String value = contentData.substring(contentData.indexOf('=') + 1, contentData.length());
            writeDataFromParcel(key, value);
        }
    }

    public MediaContent(MEDIA_TYPE type) {
        mMediaType = type;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<String> stringList = new ArrayList<String>();
        if (mTag != null) {
            stringList.add(("mTag=" + mTag));
        }
        if (mContentId != null) {
            stringList.add("mContentId=" + mContentId);
        }
        if (mContentTitle != null) {
            stringList.add("mContentTitle=" + mContentTitle);
        }
        if (mContentDescription != null) {
            stringList.add("mContentDescription=" + mContentDescription);
        }
        if (mContentUri != null) {
            stringList.add("mContentUri=" + mContentUri);
        }
        if (mContentPreviewUri != null) {
            stringList.add("mContentPreviewUri=" + mContentPreviewUri);
        }

        dest.writeStringList(stringList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public MEDIA_TYPE getType() {
        return mMediaType;
    }

    public String getTag() {
        return mTag;
    }

    public String getContentId() {
        return mContentId;
    }

    public Uri getContentUri() {
        return mContentUri;
    }

    public Uri getContentPreviewUri() {
        return mContentPreviewUri;
    }

    public String getContentTitle() {
        return mContentTitle;
    }

    public String getContentDescription() {
        return mContentDescription;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    public void setContentUri(Uri uri) {
        mContentUri = uri;
    }

    public void setContentId(String contentId) {
        mContentId = contentId;
    }

    public void setContentPreviewUri(Uri previewUri) {
        mContentPreviewUri = previewUri;
    }

    public void setContentTitle(String title) {
        mContentTitle = title;
    }

    public void setContentDescription(String contentDescription) {
        mContentDescription = contentDescription;
    }

    private void writeDataFromParcel(String key, String value) {
        if (key.equals("mTag")) {
            mTag = value;
        } else if (key.equals("mContentId")) {
            mContentId = value;
        } else if (key.equals("mContentTitle")) {
            mContentTitle = value;
        } else if (key.equals("mContentDescription")) {
            mContentDescription = value;
        } else if (key.equals("mContentUri")) {
            mContentUri = Uri.parse(value);
        } else if (key.equals("mContentPreviewUri")) {
            mContentPreviewUri = Uri.parse(value);
        }
    }
}
