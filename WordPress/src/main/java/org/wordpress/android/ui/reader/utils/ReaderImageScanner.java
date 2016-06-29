package org.wordpress.android.ui.reader.utils;

import android.text.TextUtils;

import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.models.ReaderImageList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderImageScanner {
    private final String mContent;
    private final boolean mIsPrivate;
    private final boolean mContentContainsImages;

    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img(\\s+.*?)(?:src\\s*=\\s*(?:'|\")(.*?)(?:'|\"))(.*?)>",
            Pattern.DOTALL| Pattern.CASE_INSENSITIVE);

    public ReaderImageScanner(String contentOfPost, boolean isPrivate) {
        mContent = contentOfPost;
        mIsPrivate = isPrivate;
        mContentContainsImages = mContent != null && mContent.contains("<img");
    }

    /*
     * start scanning the content for images and notify the passed listener about each one
     */
    public void beginScan(ReaderHtmlUtils.HtmlScannerListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("HtmlScannerListener is required");
        }

        if (!mContentContainsImages) {
            return;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imageTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
            String imageUrl = ReaderHtmlUtils.getSrcAttrValue(imageTag);
            if (!TextUtils.isEmpty(imageUrl)) {
                listener.onTagFound(imageTag, imageUrl);
            }
        }
    }

    /*
     * returns a list of all image URLs in the content above a certain width - pass zero
     * for the min to include all images regardless of size
     */
    public ReaderImageList getImageList() {
        return getImageList(0);
    }
    public ReaderImageList getGalleryImageList() {
        return getImageList(ReaderConstants.MIN_GALLERY_IMAGE_WIDTH);
    }
    public ReaderImageList getImageList(int minImageWidth) {
        ReaderImageList imageList = new ReaderImageList(mIsPrivate);

        if (!mContentContainsImages) {
            return imageList;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imgTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
            String imageUrl = ReaderHtmlUtils.getSrcAttrValue(imgTag);

            if (minImageWidth == 0) {
                imageList.addImageUrl(imageUrl);
            } else {
                int width = Math.max(ReaderHtmlUtils.getWidthAttrValue(imgTag), ReaderHtmlUtils.getIntQueryParam(imageUrl, "w"));
                if (width >= minImageWidth) {
                    imageList.addImageUrl(imageUrl);
                }
            }
        }

        return imageList;
    }

    /*
     * used when a post doesn't have a featured image assigned, searches post's content
     * for an image that may be large enough to be suitable as a featured image
     */
    public String getLargestImage(int minImageWidth) {
        if (!mContentContainsImages) {
            return null;
        }

        String currentImageUrl = null;
        int currentMaxWidth = minImageWidth;

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imgTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
            String imageUrl = ReaderHtmlUtils.getSrcAttrValue(imgTag);

            int width = Math.max(ReaderHtmlUtils.getWidthAttrValue(imgTag), ReaderHtmlUtils.getIntQueryParam(imageUrl, "w"));
            if (width > currentMaxWidth) {
                currentImageUrl = imageUrl;
                currentMaxWidth = width;
            }
        }

        return currentImageUrl;
    }

    /*
     * same as above, but doesn't enforce the max width - will return the first image found if
     * no images have their width set
     */
    public String getLargestImage() {
        return getLargestImage(-1);
    }
}
