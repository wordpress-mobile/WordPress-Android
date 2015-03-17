package org.wordpress.mediapicker;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple data model to describe media information, including:
 * <ul>
 *     <li>Title</li>
 *     <li>Source</li>
 *     <li>Preview Image</li>
 * </ul>
 *
 * Implements {@link android.os.Parcelable} to allow passing via {@link android.content.Intent}'s.
 */

public class MediaItem implements Parcelable {
    private String mTag;
    private String mContentTitle;
    private Uri mContentPreviewSource;
    private Uri mContentSource;
    private int mRotation;

    /**
     * @param tag
     *  value to set; not filtered
     */
    public void setTag(final String tag) {
        mTag = tag;
    }

    /**
     * @return
     *  current Tag value; may be null
     */
    public String getTag() {
        return mTag;
    }

    /**
     * @param title
     *  value to set; not filtered
     */
    public void setTitle(final String title) {
        mContentTitle = title;
    }

    /**
     * @return
     *  current Title value; may be null
     */
    public String getTitle() {
        return mContentTitle;
    }

    /**
     * @param source
     *  value to set
     */
    public void setPreviewSource(final Uri source) {
        mContentPreviewSource = source;
    }

    /**
     * @param source
     *  value to {@link android.net.Uri#parse(String)} into the preview {@link android.net.Uri}
     */
    public void setPreviewSource(final String source) {
        mContentPreviewSource = Uri.parse(source);
    }

    /**
     * @return
     *  current Preview source value; may be null
     */
    public Uri getPreviewSource() {
        return mContentPreviewSource;
    }

    /**
     * @param source
     *  value to set
     */

    public void setSource(final Uri source) {
        mContentSource = source;
    }

    /**
     * @param source
     *  value to {@link android.net.Uri#parse(String)} into the content {@link android.net.Uri}
     */
    public void setSource(final String source) {
        mContentSource = Uri.parse(source);
    }

    /**
     * @return
     *  current Preview source value; may be null
     */
    public Uri getSource() {
        return mContentSource;
    }

    /**
     * @param rotation
     *  value to set; not filtered
     */
    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    /**
     * @return
     *  current Rotation value; defaults to 0
     */
    public int getRotation() {
        return mRotation;
    }

    /*
        Parcelable interface
    */
    public static final String PARCEL_KEY_TAG = "tag";
    public static final String PARCEL_KEY_TITLE = "title";
    public static final String PARCEL_KEY_PREVIEW = "preview";
    public static final String PARCEL_KEY_SOURCE = "source";
    public static final String PARCEL_KEY_ROTATION = "rotation";

    public static final Creator<MediaItem> CREATOR =
            new Creator<MediaItem>() {
                public MediaItem createFromParcel(Parcel in) {
                    List<String> parcelData = new ArrayList<>();
                    in.readStringList(parcelData);

                    if (parcelData.size() > 0) {
                        MediaItem newItem = new MediaItem();

                        while (parcelData.size() > 0) {
                            String data = parcelData.remove(0);
                            String key = data.substring(0, data.indexOf('='));
                            String value = data.substring(data.indexOf('=') + 1, data.length());

                            if (!key.isEmpty()) {
                                switch (key) {
                                    case PARCEL_KEY_TAG:
                                        newItem.setTag(value);
                                        break;
                                    case PARCEL_KEY_TITLE:
                                        newItem.setTitle(value);
                                        break;
                                    case PARCEL_KEY_PREVIEW:
                                        newItem.setPreviewSource(value);
                                        break;
                                    case PARCEL_KEY_SOURCE:
                                        newItem.setSource(value);
                                        break;
                                    case PARCEL_KEY_ROTATION:
                                        newItem.setRotation(Integer.parseInt(value));
                                        break;
                                }
                            }
                        }

                        return newItem;
                    }

                    return null;
                }

                public MediaItem[] newArray(int size) {
                    return new MediaItem[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<String> dataList = new ArrayList<>();

        dataList.add(PARCEL_KEY_ROTATION + "=" + mRotation);
        if (mTag != null && !mTag.isEmpty()) {
            dataList.add(PARCEL_KEY_TAG + "=" + mTag);
        }
        if (mContentTitle != null && !mContentTitle.isEmpty()) {
            dataList.add(PARCEL_KEY_TITLE + "=" + mContentTitle);
        }
        if (mContentPreviewSource != null && !mContentPreviewSource.toString().isEmpty()) {
            dataList.add(PARCEL_KEY_PREVIEW + "=" + mContentPreviewSource.toString());
        }
        if (mContentSource != null && !mContentSource.toString().isEmpty()) {
            dataList.add(PARCEL_KEY_SOURCE + "=" + mContentSource.toString());
        }

        dest.writeStringList(dataList);
    }
}
