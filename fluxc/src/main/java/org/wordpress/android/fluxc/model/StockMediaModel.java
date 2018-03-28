package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

public class StockMediaModel extends Payload<BaseNetworkError> {
    private String mId;
    private String mExtension;
    private String mFile;
    private String mGuid;
    private String mName;
    private String mTitle;
    private String mType;
    private String mUrl;
    private String mDate;

    private String mLargeThumbnail;
    private String mMediumThumbnail;
    private String mPostThumbnail;
    private String mThumbnail;

    private int mHeight;
    private int mWidth;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getExtension() {
        return mExtension;
    }

    public void setExtension(String extension) {
        this.mExtension = extension;
    }

    public String getFile() {
        return mFile;
    }

    public void setFile(String file) {
        this.mFile = file;
    }

    public String getGuid() {
        return mGuid;
    }

    public void setGuid(String guid) {
        this.mGuid = guid;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public String getLargeThumbnail() {
        return mLargeThumbnail;
    }

    public void setLargeThumbnail(String largeThumbnail) {
        this.mLargeThumbnail = largeThumbnail;
    }

    public String getMediumThumbnail() {
        return mMediumThumbnail;
    }

    public void setMediumThumbnail(String mediumThumbnail) {
        this.mMediumThumbnail = mediumThumbnail;
    }

    public String getPostThumbnail() {
        return mPostThumbnail;
    }

    public void setPostThumbnail(String postThumbnail) {
        this.mPostThumbnail = postThumbnail;
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.mThumbnail = thumbnail;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof StockMediaModel)) return false;

        StockMediaModel otherMedia = (StockMediaModel) other;

        return StringUtils.equals(this.getId(), otherMedia.getId());
    }
}
