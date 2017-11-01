package org.wordpress.android.editor;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EditorImageMetaData implements Parcelable {

    public static final String ARG_EDITOR_IMAGE_METADATA = "editor_image_metadata";

    @SerializedName("align")
    @Expose
    private String align;
    @SerializedName("alt")
    @Expose
    private String alt;
    @SerializedName("attachment_id")
    @Expose
    private String attachmentId;
    @SerializedName("caption")
    @Expose
    private String caption;
    @SerializedName("captionClassName")
    @Expose
    private String captionClassName;
    @SerializedName("captionId")
    @Expose
    private String captionId;
    @SerializedName("classes")
    @Expose
    private String classes;
    @SerializedName("height")
    @Expose
    private String height;
    @SerializedName("linkClassName")
    @Expose
    private String linkClassName;
    @SerializedName("linkRel")
    @Expose
    private String linkRel;
    @SerializedName("linkTargetBlank")
    @Expose
    private boolean linkTargetBlank;
    @SerializedName("linkUrl")
    @Expose
    private String linkUrl;
    @SerializedName("size")
    @Expose
    private String size;
    @SerializedName("src")
    @Expose
    private String src;
    @SerializedName("title")
    @Expose
    private String title;

    @SerializedName("width")
    @Expose
    private String width;

    @SerializedName("naturalWidth")
    @Expose
    private int naturalWidth;

    @SerializedName("naturalHeight")
    @Expose
    private int mNaturalHeight;

    private boolean mCanBeFeatured;
    private boolean mIsFeatured;
    private String mBlogMaxImageWidth;
    private int mLocalId;

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCaptionClassName() {
        return captionClassName;
    }

    public void setCaptionClassName(String captionClassName) {
        this.captionClassName = captionClassName;
    }

    public String getCaptionId() {
        return captionId;
    }

    public void setCaptionId(String captionId) {
        this.captionId = captionId;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getLinkClassName() {
        return linkClassName;
    }

    public void setLinkClassName(String linkClassName) {
        this.linkClassName = linkClassName;
    }

    public String getLinkRel() {
        return linkRel;
    }

    public void setLinkRel(String linkRel) {
        this.linkRel = linkRel;
    }

    public boolean isLinkTargetBlank() {
        return linkTargetBlank;
    }

    public void setLinkTargetBlank(boolean linkTargetBlank) {
        this.linkTargetBlank = linkTargetBlank;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public int getNaturalWidth() {
        return naturalWidth;
    }

    public void setNaturalWidth(int naturalWidth) {
        this.naturalWidth = naturalWidth;
    }

    public int getNaturalHeight() {
        return mNaturalHeight;
    }

    public void setNaturalHeight(int naturalHeight) {
        this.mNaturalHeight = naturalHeight;
    }

    public boolean isCanBeFeatured() {
        return mCanBeFeatured;
    }

    public void setCanBeFeatured(boolean canBeFeatured) {
        mCanBeFeatured = canBeFeatured;
    }

    public boolean isFeatured() {
        return mIsFeatured;
    }

    public void setFeatured(boolean featured) {
        mIsFeatured = featured;
    }

    public int getBlogMaxImageWidth() {
        try{
            return Integer.parseInt(mBlogMaxImageWidth);
        }catch (NumberFormatException ex){
            return 9999;
        }
    }

    public void setBlogMaxImageWidth(String blogMaxImageWidth) {
        mBlogMaxImageWidth = blogMaxImageWidth;
    }

    public int getLocalId() {
        return mLocalId;
    }

    public void setLocalId(int localId) {
        mLocalId = localId;
    }

    protected EditorImageMetaData(Parcel in) {
        align = in.readString();
        alt = in.readString();
        attachmentId = in.readString();
        caption = in.readString();
        captionClassName = in.readString();
        captionId = in.readString();
        classes = in.readString();
        height = in.readString();
        linkClassName = in.readString();
        linkRel = in.readString();
        linkTargetBlank = in.readByte() != 0x00;
        linkUrl = in.readString();
        size = in.readString();
        src = in.readString();
        title = in.readString();
        width = in.readString();
        naturalWidth = in.readInt();
        mNaturalHeight = in.readInt();
        mCanBeFeatured = in.readByte() != 0x00;
        mIsFeatured = in.readByte() != 0x00;
        mBlogMaxImageWidth = in.readString();
        mLocalId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(align);
        dest.writeString(alt);
        dest.writeString(attachmentId);
        dest.writeString(caption);
        dest.writeString(captionClassName);
        dest.writeString(captionId);
        dest.writeString(classes);
        dest.writeString(height);
        dest.writeString(linkClassName);
        dest.writeString(linkRel);
        dest.writeByte((byte) (linkTargetBlank ? 0x01 : 0x00));
        dest.writeString(linkUrl);
        dest.writeString(size);
        dest.writeString(src);
        dest.writeString(title);
        dest.writeString(width);
        dest.writeInt(naturalWidth);
        dest.writeInt(mNaturalHeight);
        dest.writeByte((byte) (mCanBeFeatured ? 0x01 : 0x00));
        dest.writeByte((byte) (mIsFeatured ? 0x01 : 0x00));
        dest.writeString(mBlogMaxImageWidth);
        dest.writeInt(mLocalId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<EditorImageMetaData> CREATOR = new Parcelable.Creator<EditorImageMetaData>() {
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