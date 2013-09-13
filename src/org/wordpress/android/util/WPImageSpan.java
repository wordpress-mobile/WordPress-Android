//Add WordPress image fields to ImageSpan object

package org.wordpress.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.style.ImageSpan;
//import android.util.Log;

public class WPImageSpan extends ImageSpan {

        private Uri imageSource = null;

        private int width = 500;
        private String title = "";
        private String description = "";
        private String caption = "";
        private int horizontalAlignment = 0;
        private boolean isVideo;
        private boolean featured = false;
        private boolean featuredInPost = false;
        private String mediaId;
        private int height;
        private String mimeType;
        private String thumbnailURL;
        private boolean networkImageLoaded = false;

        private String fileName;

        private long date_created_gmt;

        public WPImageSpan(Context context, Bitmap b, Uri src) {
            super(context, b);
            this.imageSource = src;
        }

        public WPImageSpan(Context context, int resId, Uri src) {
            super(context, resId);
            this.imageSource = src;
        }
        
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getHorizontalAlignment() {
            return horizontalAlignment;
        }

        public void setHorizontalAlignment(int horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
        }

        public void setImageSource(Uri imageSource) {
            this.imageSource = imageSource;
        }

        public Uri getImageSource() {
                return imageSource;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public String getCaption() {
            return caption;
        }

        public void setCaption(String caption) {
            this.caption = caption;
        }

        public boolean isFeatured() {
            return featured;
        }

        public void setFeatured(boolean featured) {
            this.featured = featured;
        }

        public boolean isFeaturedInPost() {
            return featuredInPost;
        }

        public void setFeaturedInPost(boolean featuredInPost) {
            this.featuredInPost = featuredInPost;
        }

        public boolean isVideo() {
            return isVideo;
        }

        public void setVideo(boolean isVideo) {
            this.isVideo = isVideo;
        }

        // methods below for use when WPImageSpan uses media files from the server
        
        public String getMediaId() {
            return mediaId;
        }

        public void setMediaId(String mediaId) {
            this.mediaId = mediaId;
        }

        public void setHeight(int height) {
            this.height = height;
        }
        
        public int getHeight() {
            return height;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
        
        public String getMimeType() {
            return mimeType;
        }

        public String getThumbnailURL() {
            return thumbnailURL;
        }

        public void setThumbnailURL(String thumbnailURL) {
            this.thumbnailURL = thumbnailURL;
        }

        public boolean isNetworkImageLoaded() {
            return networkImageLoaded;
        }

        public void setNetworkImageLoaded(boolean networkImageLoaded) {
            this.networkImageLoaded = networkImageLoaded;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setDateCreatedGMT(long date) {
            this.date_created_gmt = date;
        }
        
        public long getDateCreatedGMT() {
            return date_created_gmt;
        }
        
        // -- end of methods for media files from the server
}
