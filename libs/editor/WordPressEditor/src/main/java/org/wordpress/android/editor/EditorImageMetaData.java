package org.wordpress.android.editor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EditorImageMetaData {

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
    @SerializedName("mNaturalHeight")
    @Expose
    private int mNaturalHeight;

    private boolean mCanBeFeatured;
    private boolean mIsFeatured;
    private String mBlogMaxImageWidth;

    public String getAlign() {
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

    public void setAttachmentId(String attachmentId) {
        this.mAttachmentId = attachmentId;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        this.mCaption = caption;
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

    public String getBlogMaxImageWidth() {
        return mBlogMaxImageWidth;
    }

    public void setBlogMaxImageWidth(String blogMaxImageWidth) {
        mBlogMaxImageWidth = blogMaxImageWidth;
    }
}