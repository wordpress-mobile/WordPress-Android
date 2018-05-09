package org.wordpress.android.ui.reader.utils;

import android.support.annotation.NonNull;

import org.wordpress.android.ui.reader.models.ReaderImageList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderImageScanner {
    private final String mContent;
    private final boolean mIsPrivate;
    private final boolean mContentContainsImages;

    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img[^>]* src=\\\"([^\\\"]*)\\\"[^>]*>",
            Pattern.CASE_INSENSITIVE);

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
            String imageTag = imgMatcher.group(0);
            String imageUrl = imgMatcher.group(1);
            listener.onTagFound(imageTag, imageUrl);
        }
    }

    /*
     * returns a list of image URLs in the content up to the max above a certain width - pass zero
     * to include all images regardless of size
     */
    public ReaderImageList getImageList(int maxImageCount, int minImageWidth) {
        ReaderImageList imageList = new ReaderImageList(mIsPrivate);

        if (!mContentContainsImages) {
            return imageList;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imageTag = imgMatcher.group(0);
            String imageUrl = imgMatcher.group(1);

            if (minImageWidth == 0) {
                imageList.addImageUrl(imageUrl);
            } else {
                int width = Math.max(ReaderHtmlUtils.getWidthAttrValue(imageTag),
                                     ReaderHtmlUtils.getIntQueryParam(imageUrl, "w"));
                if (width >= minImageWidth) {
                    imageList.addImageUrl(imageUrl);
                    if (maxImageCount > 0 && imageList.size() >= maxImageCount) {
                        break;
                    }
                }
            }
        }

        return imageList;
    }

    /*
     * returns true if there at least `minImageCount` images in the post content that are at
     * least `minImageWidth` in size
     */
    public boolean hasUsableImageCount(int minImageCount, int minImageWidth) {
        return getImageList(minImageCount, minImageWidth).size() == minImageCount;
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
            String imageTag = imgMatcher.group(0);
            String imageUrl = imgMatcher.group(1);

            int width = Math.max(ReaderHtmlUtils.getWidthAttrValue(imageTag),
                                 ReaderHtmlUtils.getIntQueryParam(imageUrl, "w"));
            if (width > currentMaxWidth) {
                currentImageUrl = imageUrl;
                currentMaxWidth = width;
            } else if (currentImageUrl == null && hasSuitableClassForFeaturedImage(imageTag)) {
                currentImageUrl = imageUrl;
            }
        }

        return currentImageUrl;
    }

    /*
     * returns true if the passed image tag has a "size-" class attribute which would make it
     * suitable for use as a featured image
     */
    private boolean hasSuitableClassForFeaturedImage(@NonNull String imageTag) {
        String tagClass = ReaderHtmlUtils.getClassAttrValue(imageTag);
        return (tagClass != null
                && (tagClass.contains("size-full")
                    || tagClass.contains("size-large")
                    || tagClass.contains("size-medium")));
    }

    /*
     * same as above, but doesn't enforce the max width - will return the first image found if
     * no images have their width set
     */
    public String getLargestImage() {
        return getLargestImage(-1);
    }
}
