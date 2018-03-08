package org.wordpress.android.util.helpers;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MediaFile {
    protected int mId;
    protected long mPostID;
    protected String mFilePath = null; // path of the file into disk
    protected String mFileName = null; // name of the file into the server
    protected String mTitle = null;
    protected String mDescription = null;
    protected String mCaption = null;
    protected int mHorizontalAlignment; // 0 = none, 1 = left, 2 = center, 3 = right
    protected boolean mVerticalAligment = false; // false = bottom, true = top
    protected int mWidth = 500;
    protected int mHeight;
    protected String mMimeType = "";
    protected String mVideoPressShortCode = null;
    protected boolean mFeatured = false;
    protected boolean mIsVideo = false;
    protected boolean mFeaturedInPost;
    protected String mFileURL = null; // url of the file to download
    protected String mThumbnailURL = null; // url of the thumbnail to download
    private String mBlogId;
    private long mDateCreatedGmt;
    private String mUploadState = null;
    private String mMediaId;

    private static final String VIDEOPRESS_SHORTCODE_ID = "videopress_shortcode";

    public MediaFile(String blogId, Map<?, ?> resultMap, boolean isWPCom) {
        setBlogId(blogId);
        setMediaId(MapUtils.getMapStr(resultMap, "attachment_id"));
        setPostID(MapUtils.getMapLong(resultMap, "parent"));
        setTitle(MapUtils.getMapStr(resultMap, "title"));
        setCaption(MapUtils.getMapStr(resultMap, "caption"));
        setDescription(MapUtils.getMapStr(resultMap, "description"));
        setVideoPressShortCode(MapUtils.getMapStr(resultMap, VIDEOPRESS_SHORTCODE_ID));

        // get the file name from the link
        String link = MapUtils.getMapStr(resultMap, "link");
        setFileName(new String(link).replaceAll("^.*/([A-Za-z0-9_-]+)\\.\\w+$", "$1"));

        String fileType = new String(link).replaceAll(".*\\.(\\w+)$", "$1").toLowerCase();
        String fileMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileType);
        setMimeType(fileMimeType);

        // make the file urls be https://... so that we can get these images with oauth when the blogs are private
        // assume no https for images in self-hosted blogs
        String fileUrl = MapUtils.getMapStr(resultMap, "link");
        if (isWPCom) {
            fileUrl = fileUrl.replace("http:", "https:");
        }
        setFileURL(fileUrl);

        String thumbnailURL = MapUtils.getMapStr(resultMap, "thumbnail");
        if (thumbnailURL.startsWith("http")) {
            if (isWPCom) {
                thumbnailURL = thumbnailURL.replace("http:", "https:");
            }
            setThumbnailURL(thumbnailURL);
        }

        Date date = MapUtils.getMapDate(resultMap, "date_created_gmt");
        if (date != null) {
            setDateCreatedGMT(date.getTime());
        }

        Object meta = resultMap.get("metadata");
        if (meta != null && meta instanceof Map) {
            Map<?, ?> metadata = (Map<?, ?>) meta;
            setWidth(MapUtils.getMapInt(metadata, "width"));
            setHeight(MapUtils.getMapInt(metadata, "height"));
        }
    }

    public MediaFile() {
        // default constructor
    }

    public MediaFile(MediaFile mediaFile) {
        this.mId = mediaFile.mId;
        this.mPostID = mediaFile.mPostID;
        this.mFilePath = mediaFile.mFilePath;
        this.mFileName = mediaFile.mFileName;
        this.mTitle = mediaFile.mTitle;
        this.mDescription = mediaFile.mDescription;
        this.mCaption = mediaFile.mCaption;
        this.mHorizontalAlignment = mediaFile.mHorizontalAlignment;
        this.mVerticalAligment = mediaFile.mVerticalAligment;
        this.mWidth = mediaFile.mWidth;
        this.mHeight = mediaFile.mHeight;
        this.mMimeType = mediaFile.mMimeType;
        this.mVideoPressShortCode = mediaFile.mVideoPressShortCode;
        this.mFeatured = mediaFile.mFeatured;
        this.mIsVideo = mediaFile.mIsVideo;
        this.mFeaturedInPost = mediaFile.mFeaturedInPost;
        this.mFileURL = mediaFile.mFileURL;
        this.mThumbnailURL = mediaFile.mThumbnailURL;
        this.mBlogId = mediaFile.mBlogId;
        this.mDateCreatedGmt = mediaFile.mDateCreatedGmt;
        this.mUploadState = mediaFile.mUploadState;
        this.mMediaId = mediaFile.mMediaId;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getMediaId() {
        return mMediaId;
    }

    public void setMediaId(String id) {
        mMediaId = id;
    }

    public boolean isFeatured() {
        return mFeatured;
    }

    public void setFeatured(boolean featured) {
        this.mFeatured = featured;
    }

    public long getPostID() {
        return mPostID;
    }

    public void setPostID(long postID) {
        this.mPostID = postID;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        this.mFilePath = filePath;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        this.mCaption = caption;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public String getFileURL() {
        return mFileURL;
    }

    public void setFileURL(String fileURL) {
        this.mFileURL = fileURL;
    }

    public String getThumbnailURL() {
        return mThumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.mThumbnailURL = thumbnailURL;
    }

    public boolean isVerticalAlignmentOnTop() {
        return mVerticalAligment;
    }

    public void setVerticalAlignmentOnTop(boolean verticalAligment) {
        this.mVerticalAligment = verticalAligment;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    public String getMimeType() {
        return StringUtils.notNullStr(mMimeType);
    }

    public void setMimeType(String type) {
        mMimeType = StringUtils.notNullStr(type);
    }

    public String getVideoPressShortCode() {
        return mVideoPressShortCode;
    }

    public void setVideoPressShortCode(String videoPressShortCode) {
        this.mVideoPressShortCode = videoPressShortCode;
    }

    public int getHorizontalAlignment() {
        return mHorizontalAlignment;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        this.mHorizontalAlignment = horizontalAlignment;
    }

    public boolean isVideo() {
        return mIsVideo;
    }

    public void setVideo(boolean isVideo) {
        this.mIsVideo = isVideo;
    }

    public boolean isFeaturedInPost() {
        return mFeaturedInPost;
    }

    public void setFeaturedInPost(boolean featuredInPost) {
        this.mFeaturedInPost = featuredInPost;
    }

    public String getBlogId() {
        return mBlogId;
    }

    public void setBlogId(String blogId) {
        this.mBlogId = blogId;
    }

    public void setDateCreatedGMT(long dateCreatedGmt) {
        this.mDateCreatedGmt = dateCreatedGmt;
    }

    public long getDateCreatedGMT() {
        return mDateCreatedGmt;
    }

    public void setUploadState(String uploadState) {
        this.mUploadState = uploadState;
    }

    public String getUploadState() {
        return mUploadState;
    }

    /**
     * Outputs the Html for an image
     * If a fullSizeUrl exists, a link will be created to it from the resizedPictureUrl
     */
    public String getImageHtmlForUrls(String fullSizeUrl, String resizedPictureURL, boolean shouldAddImageWidthCSS) {
        String alignment = "";
        switch (getHorizontalAlignment()) {
            case 0:
                alignment = "alignnone";
                break;
            case 1:
                alignment = "alignleft";
                break;
            case 2:
                alignment = "aligncenter";
                break;
            case 3:
                alignment = "alignright";
                break;
        }

        String alignmentCSS = "class=\"" + alignment + " size-full\" ";

        if (shouldAddImageWidthCSS) {
            alignmentCSS += "style=\"max-width: " + getWidth() + "px\" ";
        }

        // Check if we uploaded a featured picture that is not added to the Post content (normal case)
        if ((fullSizeUrl != null && fullSizeUrl.equalsIgnoreCase(""))
            || (resizedPictureURL != null && resizedPictureURL.equalsIgnoreCase(""))) {
            return ""; // Not featured in Post. Do not add to the content.
        }

        if (fullSizeUrl == null && resizedPictureURL != null) {
            fullSizeUrl = resizedPictureURL;
        } else if (fullSizeUrl != null && resizedPictureURL == null) {
            resizedPictureURL = fullSizeUrl;
        }

        String mediaTitle = StringUtils.notNullStr(getTitle());

        String content = String.format(Locale.US, "<a href=\"%s\"><img title=\"%s\" %s alt=\"image\" src=\"%s\" /></a>",
                                       fullSizeUrl, mediaTitle, alignmentCSS, resizedPictureURL);

        if (!TextUtils.isEmpty(getCaption())) {
            content = String.format(Locale.US, "[caption id=\"\" align=\"%s\" width=\"%d\"]%s%s[/caption]",
                                    alignment, getWidth(), content, TextUtils.htmlEncode(getCaption()));
        }

        return content;
    }
}
