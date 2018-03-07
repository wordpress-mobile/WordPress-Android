package org.wordpress.android.editor;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.wordpress.android.util.StringUtils;

import static org.wordpress.android.editor.EditorFragmentAbstract.ATTR_ALIGN;

public class EditorImageMetaData implements Parcelable {
    public static final String ARG_EDITOR_IMAGE_METADATA = "editor_image_metadata";

    @SerializedName("align")
    @Expose
    private String mAlign;

    @SerializedName("alt")
    @Expose
    private String mAlt;

    @SerializedName("attachment_id")
    @Expose
    private String mAttachmentId;

    @SerializedName("caption")
    @Expose
    private String mCaption;

    @SerializedName("captionClassName")
    @Expose
    private String mCaptionClassName;

    @SerializedName("captionId")
    @Expose
    private String mCaptionId;

    @SerializedName("classes")
    @Expose
    private String mClasses;

    @SerializedName("height")
    @Expose
    private String mHeight;

    @SerializedName("linkClassName")
    @Expose
    private String mLinkClassName;

    @SerializedName("linkRel")
    @Expose
    private String mLinkRel;

    @SerializedName("linkTargetBlank")
    @Expose
    private boolean mLinkTargetBlank;

    @SerializedName("linkUrl")
    @Expose
    private String mLinkUrl;

    @SerializedName("size")
    @Expose
    private String mSize;

    @SerializedName("src")
    @Expose
    private String mSrc;

    @SerializedName("title")
    @Expose
    private String mTitle;

    @SerializedName("width")
    @Expose
    private String mWidth;

    @SerializedName("naturalWidth")
    @Expose
    private int mNaturalWidth;

    @SerializedName("naturalHeight")
    @Expose
    private int mNaturalHeight;

    private boolean mIsRemoved;
    private int mLocalId;

    public String getAlign() {
        if (!TextUtils.isEmpty(mAlign) && mAlign.startsWith(ATTR_ALIGN)) {
            return mAlign.substring(ATTR_ALIGN.length(), mAlign.length());
        }
        return mAlign;
    }

    public void setAlign(String align) {
        this.mAlign = align;
    }

    public String getAlt() {
        return mAlt;
    }

    public void setAlt(String alt) {
        this.mAlt = alt;
    }

    public String getAttachmentId() {
        return mAttachmentId;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        this.mCaption = caption;
    }

    public String getCaptionClassName() {
        return mCaptionClassName;
    }

    public void setCaptionClassName(String captionClassName) {
        this.mCaptionClassName = captionClassName;
    }

    public String getCaptionId() {
        return mCaptionId;
    }

    public void setCaptionId(String captionId) {
        this.mCaptionId = captionId;
    }

    public String getClasses() {
        return mClasses;
    }

    public void setClasses(String classes) {
        this.mClasses = classes;
    }

    public String getHeight() {
        return mHeight;
    }

    public int getHeightInt() {
        return StringUtils.stringToInt(mHeight);
    }

    public void setHeight(String height) {
        this.mHeight = height;
    }

    public String getLinkClassName() {
        return mLinkClassName;
    }

    public void setLinkClassName(String linkClassName) {
        this.mLinkClassName = linkClassName;
    }

    public String getLinkRel() {
        return mLinkRel;
    }

    public void setLinkRel(String linkRel) {
        this.mLinkRel = linkRel;
    }

    public boolean isLinkTargetBlank() {
        return mLinkTargetBlank;
    }

    public void setLinkTargetBlank(boolean linkTargetBlank) {
        this.mLinkTargetBlank = linkTargetBlank;
    }

    public String getLinkUrl() {
        return mLinkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.mLinkUrl = linkUrl;
    }

    public String getSize() {
        return mSize;
    }

    public void setSize(String size) {
        this.mSize = size;
    }

    public String getSrc() {
        return mSrc;
    }

    public void setSrc(String src) {
        this.mSrc = src;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getWidth() {
        return mWidth;
    }

    public int getWidthInt() {
        return StringUtils.stringToInt(mWidth);
    }

    public void setWidth(String width) {
        this.mWidth = width;
    }

    public int getLocalId() {
        return mLocalId;
    }

    public void setLocalId(int localId) {
        mLocalId = localId;
    }

    public boolean isRemoved() {
        return mIsRemoved;
    }

    public void markAsRemoved() {
        mIsRemoved = true;
    }

    protected EditorImageMetaData(Parcel in) {
        mAlign = in.readString();
        mAlt = in.readString();
        mAttachmentId = in.readString();
        mCaption = in.readString();
        mCaptionClassName = in.readString();
        mCaptionId = in.readString();
        mClasses = in.readString();
        mHeight = in.readString();
        mLinkClassName = in.readString();
        mLinkRel = in.readString();
        mLinkTargetBlank = in.readByte() != 0x00;
        mLinkUrl = in.readString();
        mSize = in.readString();
        mSrc = in.readString();
        mTitle = in.readString();
        mWidth = in.readString();
        mNaturalWidth = in.readInt();
        mNaturalHeight = in.readInt();
        mIsRemoved = in.readByte() != 0x00;
        mLocalId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAlign);
        dest.writeString(mAlt);
        dest.writeString(mAttachmentId);
        dest.writeString(mCaption);
        dest.writeString(mCaptionClassName);
        dest.writeString(mCaptionId);
        dest.writeString(mClasses);
        dest.writeString(mHeight);
        dest.writeString(mLinkClassName);
        dest.writeString(mLinkRel);
        dest.writeByte((byte) (mLinkTargetBlank ? 0x01 : 0x00));
        dest.writeString(mLinkUrl);
        dest.writeString(mSize);
        dest.writeString(mSrc);
        dest.writeString(mTitle);
        dest.writeString(mWidth);
        dest.writeInt(mNaturalWidth);
        dest.writeInt(mNaturalHeight);
        dest.writeByte((byte) (mIsRemoved ? 0x01 : 0x00));
        dest.writeInt(mLocalId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<EditorImageMetaData> CREATOR =
            new Parcelable.Creator<EditorImageMetaData>() {
        @Override
        public EditorImageMetaData createFromParcel(Parcel in) {
            return new EditorImageMetaData(in);
        }

        @Override
        public EditorImageMetaData[] newArray(int size) {
            return new EditorImageMetaData[size];
        }
    };
}
