package org.wordpress.android.ui.reader.utils;

import android.net.Uri;
import android.text.TextUtils;

import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.UrlUtils;

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
            listener.onScanCompleted();
            return;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imageTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
            String imageUrl = ReaderHtmlUtils.getSrcAttrValue(imageTag);
            if (!TextUtils.isEmpty(imageUrl)) {
                listener.onTagFound(imageTag, imageUrl, imgMatcher.start(), imgMatcher.end());
            }
        }

        listener.onScanCompleted();
    }

    /*
     * returns a list of all images in the content
     */
    public ReaderImageList getImageList() {
        ReaderImageList imageList = new ReaderImageList(mIsPrivate);

        if (!mContentContainsImages) {
            return imageList;
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(mContent);
        while (imgMatcher.find()) {
            String imgTag = mContent.substring(imgMatcher.start(), imgMatcher.end());
            imageList.addImageUrl(ReaderHtmlUtils.getSrcAttrValue(imgTag));
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
     * returns the actual image url from a Freshly Pressed featured image url - this is necessary because the
     * featured image returned by the API is often an ImagePress url that formats the actual image url for a
     * specific size, and we want to define the size in the app when the image is requested.
     * here's an example of an ImagePress featured image url from a freshly-pressed post:
     *   https://s1.wp.com/imgpress?
     *          crop=0px%2C0px%2C252px%2C160px
     *          &url=https%3A%2F%2Fs2.wp.com%2Fimgpress%3Fw%3D252%26url%3Dhttp%253A%252F%252Fmostlybrightideas.files.wordpress.com%252F2013%252F08%252Ftablet.png
     *          &unsharpmask=80,0.5,3
     */
    public static String getImageUrlFromFPFeaturedImageUrl(final String url) {
        if (url == null || !url.startsWith("http")) {
            return null;
        } else if (url.contains("/imgpress")) {
            String imageUrl = Uri.parse(url).getQueryParameter("url");
            if (imageUrl != null && imageUrl.contains("url=")) {
                // url still contains a url= param, process it again
                return getImageUrlFromFPFeaturedImageUrl(imageUrl);
            } else {
                return UrlUtils.removeQuery(imageUrl);
            }
        } else if (PhotonUtils.isMshotsUrl(url)) {
            // if this is an mshots image, return the actual url without the query string (?h=n&w=n),
            // and change it from https to http so it can be cached
            return UrlUtils.removeQuery(url).replaceFirst("https", "http");
        } else {
            return url;
        }
    }
}
