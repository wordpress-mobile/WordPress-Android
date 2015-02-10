package org.wordpress.android.util.helpers;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;

import java.util.Date;
import java.util.Map;

public class MediaFile {
    protected int id;
    protected long postID;
    protected String filePath = null; //path of the file into disk
    protected String fileName = null; //name of the file into the server
    protected String title = null;
    protected String description = null;
    protected String caption = null;
    protected int horizontalAlignment; //0 = none, 1 = left, 2 = center, 3 = right
    protected boolean verticalAligment = false; //false = bottom, true = top
    protected int width = 500, height;
    protected String mimeType = "";
    protected String videoPressShortCode = null;
    protected boolean featured = false;
    protected boolean isVideo = false;
    protected boolean featuredInPost;
    protected String fileURL = null; // url of the file to download
    protected String thumbnailURL = null;  // url of the thumbnail to download
    private String blogId;
    private long dateCreatedGmt;
    private String uploadState = null;
    private String mediaId;

    public static String VIDEOPRESS_SHORTCODE_ID = "videopress_shortcode";

    public MediaFile(String blogId, Map<?, ?> resultMap, boolean isDotCom) {
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
        if (isDotCom) {
            fileUrl = fileUrl.replace("http:", "https:");
        }
        setFileURL(fileUrl);

        String thumbnailURL = MapUtils.getMapStr(resultMap, "thumbnail");
        if (thumbnailURL.startsWith("http")) {
            if (isDotCom) {
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
        this.id = mediaFile.id;
        this.postID = mediaFile.postID;
        this.filePath = mediaFile.filePath;
        this.fileName = mediaFile.fileName;
        this.title = mediaFile.title;
        this.description = mediaFile.description;
        this.caption = mediaFile.caption;
        this.horizontalAlignment = mediaFile.horizontalAlignment;
        this.verticalAligment = mediaFile.verticalAligment;
        this.width = mediaFile.width;
        this.height = mediaFile.height;
        this.mimeType = mediaFile.mimeType;
        this.videoPressShortCode = mediaFile.videoPressShortCode;
        this.featured = mediaFile.featured;
        this.isVideo = mediaFile.isVideo;
        this.featuredInPost = mediaFile.featuredInPost;
        this.fileURL = mediaFile.fileURL;
        this.thumbnailURL = mediaFile.thumbnailURL;
        this.blogId = mediaFile.blogId;
        this.dateCreatedGmt = mediaFile.dateCreatedGmt;
        this.uploadState = mediaFile.uploadState;
        this.mediaId = mediaFile.mediaId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String id) {
        mediaId = id;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public long getPostID() {
        return postID;
    }

    public void setPostID(long postID) {
        this.postID = postID;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public boolean isVerticalAlignmentOnTop() {
        return verticalAligment;
    }

    public void setVerticalAlignmentOnTop(boolean verticalAligment) {
        this.verticalAligment = verticalAligment;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return StringUtils.notNullStr(mimeType);
    }

    public void setMimeType(String type) {
        mimeType = StringUtils.notNullStr(type);
    }

    public String getVideoPressShortCode() {
        return videoPressShortCode;
    }

    public void setVideoPressShortCode(String videoPressShortCode) {
        this.videoPressShortCode = videoPressShortCode;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean isVideo) {
        this.isVideo = isVideo;
    }

    public boolean isFeaturedInPost() {
        return featuredInPost;
    }

    public void setFeaturedInPost(boolean featuredInPost) {
        this.featuredInPost = featuredInPost;
    }

    public String getBlogId() {
        return blogId;
    }

    public void setBlogId(String blogId) {
        this.blogId = blogId;
    }

    public void setDateCreatedGMT(long date_created_gmt) {
        this.dateCreatedGmt = date_created_gmt;
    }

    public long getDateCreatedGMT() {
        return dateCreatedGmt;
    }

    public void setUploadState(String uploadState) {
        this.uploadState = uploadState;
    }

    public String getUploadState() {
        return uploadState;
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
        if ((fullSizeUrl != null && fullSizeUrl.equalsIgnoreCase("")) ||
                (resizedPictureURL != null && resizedPictureURL.equalsIgnoreCase(""))) {
            return ""; // Not featured in Post. Do not add to the content.
        }

        if (fullSizeUrl == null && resizedPictureURL != null) {
            fullSizeUrl = resizedPictureURL;
        } else if (fullSizeUrl != null && resizedPictureURL == null) {
            resizedPictureURL = fullSizeUrl;
        }

        String mediaTitle = StringUtils.notNullStr(getTitle());

        String content = String.format("<a href=\"%s\"><img title=\"%s\" %s alt=\"image\" src=\"%s\" /></a>",
                fullSizeUrl, mediaTitle, alignmentCSS, resizedPictureURL);

        if (!TextUtils.isEmpty(getCaption())) {
            content = String.format("[caption id=\"\" align=\"%s\" width=\"%d\" caption=\"%s\"]%s[/caption]",
                    alignment, getWidth(), TextUtils.htmlEncode(getCaption()), content);
        }

        return content;
    }
}
