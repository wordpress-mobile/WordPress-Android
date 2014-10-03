package org.wordpress.android.ui.reader;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

/**
 * generates and displays the HTML for post detail content - main purpose is to assign the
 * height/width attributes on image tags to (1) avoid the webView resizing as images are
 * loaded, and (2) avoid requesting images at a size larger than the display
 *
 * important to note that displayed images rely on dp rather than px sizes due to the
 * fact that WebView "converts CSS pixel values to density-independent pixel values"
 * http://developer.android.com/guide/webapps/targeting.html
 */
class ReaderPostRenderer {

    private final ReaderResourceVars mResourceVars;
    private final ReaderPost mPost;
    private final int mMinFullSizeWidthDp;
    private final int mMinMidSizeWidthDp;
    private final WeakReference<ReaderWebView> mWeakWebView;

    private StringBuilder mRenderBuilder;
    private String mRenderedHtml;
    private ImageSizeMap mAttachmentSizes;

    @SuppressLint("SetJavaScriptEnabled")
    ReaderPostRenderer(ReaderWebView webView, ReaderPost post) {
        if (webView == null) {
            throw new IllegalArgumentException("ReaderPostRenderer requires a webView");
        }
        if (post == null) {
            throw new IllegalArgumentException("ReaderPostRenderer requires a post");
        }

        mPost = post;
        mWeakWebView = new WeakReference<ReaderWebView>(webView);
        mResourceVars = new ReaderResourceVars(webView.getContext());

        mMinFullSizeWidthDp = pxToDp(mResourceVars.fullSizeImageWidthPx / 3);
        mMinMidSizeWidthDp = mMinFullSizeWidthDp / 2;

        // enable JavaScript in the webView if it's safe to do so, otherwise videos
        // and other embedded content won't work
        webView.getSettings().setJavaScriptEnabled(canEnableJavaScript());
    }

    void beginRender() {
        mRenderBuilder = new StringBuilder(getPostContent());

        // start image scanner to find images so we can replace them with ones that have h/w set
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                ReaderImageScanner.ImageScanListener imageListener = new ReaderImageScanner.ImageScanListener() {
                    @Override
                    public void onImageFound(String imageTag, String imageUrl, int start, int end) {
                        replaceImageTag(imageTag, imageUrl);
                    }

                    @Override
                    public void onScanCompleted() {
                        final String htmlContent = formatPostContentForWebView(mRenderBuilder.toString());
                        mRenderBuilder = null;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                renderHtmlContent(htmlContent);
                            }
                        });
                    }
                };
                ReaderImageScanner scanner = new ReaderImageScanner(mRenderBuilder.toString(), mPost.isPrivate);
                scanner.beginScan(imageListener);
            }
        }.start();
    }

    /*
     * called once the content is ready to be rendered in the webView
     */
    private void renderHtmlContent(final String htmlContent) {
        mRenderedHtml = htmlContent;

        // make sure webView is still valid (containing fragment may have been detached)
        ReaderWebView webView = mWeakWebView.get();
        if (webView == null) {
            AppLog.w(AppLog.T.READER, "reader renderer > null webView");
            return;
        }

        // IMPORTANT: use loadDataWithBaseURL() since loadData() may fail
        // https://code.google.com/p/android/issues/detail?id=4401
        // also important to use null as the baseUrl since onPageFinished
        // doesn't appear to fire when it's set to an actual url
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    /*
     * called when image scanner finds an image, tries to replace the image tag with one that
     * has height & width attributes set correctly for the current display, if that fails
     * replaces it with one that has our 'size-none' class
     */
    private void replaceImageTag(final String imageTag, final String imageUrl) {
        ImageSize origSize = getImageSize(imageTag, imageUrl);
        boolean hasWidth = (origSize != null && origSize.width > 0);
        boolean isFullSize = hasWidth && (origSize.width >= mMinFullSizeWidthDp);
        boolean isMidSize = hasWidth
                && (origSize.width >= mMinMidSizeWidthDp)
                && (origSize.width < mMinFullSizeWidthDp);

        final String newImageTag;
        if (isFullSize) {
            newImageTag = makeFullSizeImageTag(imageUrl, origSize.width, origSize.height);
        } else if (isMidSize) {
            newImageTag = makeImageTag(imageUrl, origSize.width, origSize.height, "size-medium");
        } else if (hasWidth) {
            newImageTag = makeImageTag(imageUrl, origSize.width, origSize.height, "size-none");
        } else {
            newImageTag = "<img class='size-none' src='" + imageUrl + "' />";
        }

        int start = mRenderBuilder.indexOf(imageTag);
        if (start == -1) {
            AppLog.w(AppLog.T.READER, "reader renderer > image not found in builder");
            return;
        }

        mRenderBuilder.replace(start, start + imageTag.length(), newImageTag);
    }

    private String makeImageTag(final String imageUrl, int width, int height, final String imageClass) {
        String newImageUrl = ReaderUtils.getResizedImageUrl(imageUrl, width, height, mPost.isPrivate);
        if (height > 0) {
            return new StringBuilder("<img class='").append(imageClass).append("'")
                    .append(" src='").append(newImageUrl).append("'")
                    .append(" width='").append(pxToDp(width)).append("'")
                    .append(" height='").append(pxToDp(height)).append("' />")
                    .toString();
        } else {
            return new StringBuilder("<img class='").append(imageClass).append("'")
                    .append( "src='").append(newImageUrl).append("'")
                    .append(" width='").append(pxToDp(width)).append("' />")
                    .toString();
        }
    }

    private String makeFullSizeImageTag(final String imageUrl, int width, int height) {
        int newWidth;
        int newHeight;
        if (width > 0 && height > 0) {
            if (height > width) {
                newHeight = mResourceVars.fullSizeImageWidthPx;
                float ratio = ((float) width / (float) height);
                newWidth = (int) (newHeight * ratio);
            } else {
                float ratio = ((float) height / (float) width);
                newWidth = mResourceVars.fullSizeImageWidthPx;
                newHeight = (int) (newWidth * ratio);
            }
        } else {
            newWidth = mResourceVars.fullSizeImageWidthPx;
            newHeight = 0;
        }

        return makeImageTag(imageUrl, newWidth, newHeight, "size-full");
    }

    /*
     * returns true if the post has a featured image and there are no images in the
     * post's content - when this is the case, the featured image is inserted at
     * the top of the content
     */
    private boolean shouldAddFeaturedImage() {
        return mPost.hasFeaturedImage()
            && !mPost.getText().contains("<img")
            && !PhotonUtils.isMshotsUrl(mPost.getFeaturedImage());
    }

    /*
     * returns the basic content of the post tweaked for use here
     */
    private String getPostContent() {
        // some content (such as Vimeo embeds) don't have "http:" before links
        String content = mPost.getText().replace("src=\"//", "src=\"http://");
        if (shouldAddFeaturedImage()) {
            AppLog.d(AppLog.T.READER, "reader renderer > added featured image");
            content = getFeaturedImageHtml() + content;
        }
        return content;
    }

    /*
     * returns the HTML that was last rendered, will be null prior to rendering
     */
    String getRenderedHtml() {
        return mRenderedHtml;
    }

    /*
     * returns the HTML to use when inserting a featured image into the rendered content
     */
    private String getFeaturedImageHtml() {
        String imageUrl = ReaderUtils.getResizedImageUrl(
                mPost.getFeaturedImage(),
                mResourceVars.fullSizeImageWidthPx,
                mResourceVars.featuredImageHeightPx,
                mPost.isPrivate);

        return "<img class='size-full' src='" + imageUrl + "'/>";
    }

    /*
     * returns the full content, including CSS, that will be shown in the WebView for this post
     */
    private String formatPostContentForWebView(final String content) {
        StringBuilder sbHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8' />");

        // title isn't necessary, but it's invalid html5 without one
        sbHtml.append("<title>Reader Post</title>")

        // https://developers.google.com/chrome/mobile/docs/webview/pixelperfect
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")

        // use "Open Sans" Google font
        .append("<link rel='stylesheet' type='text/css' href='http://fonts.googleapis.com/css?family=Open+Sans' />")

        .append("<style type='text/css'>")
        .append("  body { font-family: 'Open Sans', sans-serif; margin: 0px; padding: 0px;}")
        .append("  body, p, div { max-width: 100% !important; word-wrap: break-word; }")
        .append("  p, div { line-height: 1.6em; font-size: 1em; }")
        .append("  h1, h2 { line-height: 1.2em; }")

        // counteract pre-defined height/width styles
        .append("  p, div, dl, table { width: auto !important; height: auto !important; }")

        // make sure long strings don't force the user to scroll horizontally
        .append("  body, p, div, a { word-wrap: break-word; }")

        // use a consistent top/bottom margin for paragraphs, with no top margin for the first one
        .append("  p { margin-top: ").append(mResourceVars.marginSmallPx).append("px;")
        .append("      margin-bottom: ").append(mResourceVars.marginSmallPx).append("px; }")
        .append("  p:first-child { margin-top: 0px; }")

        // add background color and padding to pre blocks, and add overflow scrolling
        // so user can scroll the block if it's wider than the display
        .append("  pre { overflow-x: scroll;")
        .append("        background-color: ").append(mResourceVars.greyExtraLightStr).append("; ")
        .append("        padding: ").append(mResourceVars.marginSmallPx).append("px; }")

        // add a left border to blockquotes
        .append("  blockquote { margin-left: ").append(mResourceVars.marginSmallPx).append("px; ")
        .append("               padding-left: ").append(mResourceVars.marginSmallPx).append("px; ")
        .append("               border-left: 3px solid ").append(mResourceVars.greyLightStr).append("; }")

        // show links in the same color they are elsewhere in the app
        .append("  a { text-decoration: none; color: ").append(mResourceVars.linkColorStr).append("; }")

        // make sure images aren't wider than the display, strictly enforced for images without size
        .append("  img { max-width: 100%; }")
        .append("  img.size-none { max-width: 100% !important; height: auto !important; }")

        // center large/medium images, provide a small bottom margin, and add a background color
        // so the user sees something while they're loading
        .append("  img.size-full, img.size-large, img.size-medium {")
        .append("     display: block; margin-left: auto; margin-right: auto;")
        .append("     background-color: ").append(mResourceVars.greyExtraLightStr).append(";")
        .append("     margin-bottom: ").append(mResourceVars.marginSmallPx).append("px; }")

        // set tiled gallery containers to auto height/width
        .append("  div.gallery-row, div.gallery-group { width: auto !important; height: auto !important; }")
        .append("  div.tiled-gallery-caption { clear: both; }")

        // see http://codex.wordpress.org/CSS#WordPress_Generated_Classes
        .append("  .wp-caption { background-color: ").append(mResourceVars.greyExtraLightStr).append("; }")
        .append("  .wp-caption img { margin-top: 0px; margin-bottom: 0px; }")
        .append("  .wp-caption .wp-caption-text {")
        .append("       font-size: smaller; line-height: 1.2em; margin: 0px;")
        .append("       padding: ").append(mResourceVars.marginExtraSmallPx).append("px; ")
        .append("       color: ").append(mResourceVars.greyMediumDarkStr).append("; }");

        // if javascript is allowed, make sure embedded videos fit the browser width and
        // use 16:9 ratio (YouTube standard) - if not allowed, hide iframes/embeds
        if (canEnableJavaScript()) {
            sbHtml.append("  iframe, embed { width: ").append(pxToDp(mResourceVars.videoWidthPx)).append("px !important;")
                  .append("                  height: ").append(pxToDp(mResourceVars.videoHeightPx)).append("px !important; }");
        } else {
            sbHtml.append("  iframe, embed { display: none; }");
        }

        // html5 video doesn't require javascript
        sbHtml.append(" video { width: ").append(pxToDp(mResourceVars.videoWidthPx)).append("px !important;")
              .append("         height: ").append(pxToDp(mResourceVars.videoHeightPx)).append("px !important; }");

        sbHtml.append("</style>")
              .append("</head><body>")
              .append(content)
              .append("</body></html>");

        return sbHtml.toString();
    }

    private ImageSize getImageSize(final String imageTag, final String imageUrl) {
        ImageSize size = getImageSizeFromAttachments(imageUrl);
        if (size == null && imageUrl.contains("?")) {
            size = getImageSizeFromQueryParams(imageUrl);
        }
        if (size == null && imageTag.contains("width=")) {
            size = getImageSizeFromAttributes(imageTag);
        }
        return size;
    }

    private ImageSize getImageSizeFromAttachments(final String imageUrl) {
        if (mAttachmentSizes == null) {
            mAttachmentSizes = new ImageSizeMap(mPost.getAttachmentsJson());
        }
        return mAttachmentSizes.getAttachmentSize(imageUrl);
    }

    private ImageSize getImageSizeFromQueryParams(final String imageUrl) {
        if (imageUrl.contains("w=")) {
            Uri uri = Uri.parse(imageUrl.replace("&#038;", "&"));
            return new ImageSize(
                    StringUtils.stringToInt(uri.getQueryParameter("w")),
                    StringUtils.stringToInt(uri.getQueryParameter("h")));
        } else if (imageUrl.contains("resize=")) {
            Uri uri = Uri.parse(imageUrl.replace("&#038;", "&"));
            String param = uri.getQueryParameter("resize");
            if (param != null) {
                String[] sizes = param.split(",");
                if (sizes.length == 2) {
                    return new ImageSize(
                            StringUtils.stringToInt(sizes[0]),
                            StringUtils.stringToInt(sizes[1]));
                }
            }
        }

        return null;
    }

    private ImageSize getImageSizeFromAttributes(final String imageTag) {
        return new ImageSize(
                ReaderImageScanner.getWidthAttrValue(imageTag),
                ReaderImageScanner.getHeightAttrValue(imageTag));
    }

    private int pxToDp(int px) {
        if (px == 0) {
            return 0;
        }
        return DisplayUtils.pxToDp(WordPress.getContext(), px);
    }

    /*
     * javascript should only be enabled for wp blogs (not external feeds)
     */
    private boolean canEnableJavaScript() {
        return mPost.isWP();
    }

    /*
     * hash map of sizes of attachments in the current post for quick lookup - created from
     * the json "attachments" section of the post endpoints
     */
    class ImageSizeMap extends HashMap<String, ImageSize> {
        ImageSizeMap(String jsonString) {
            if (TextUtils.isEmpty(jsonString)) {
                return;
            }

            try {
                JSONObject json = new JSONObject(jsonString);
                Iterator<String> it = json.keys();
                if (!it.hasNext()) {
                    return;
                }

                while (it.hasNext()) {
                    JSONObject jsonAttach = json.optJSONObject(it.next());
                    if (jsonAttach != null && JSONUtil.getString(jsonAttach, "mime_type").startsWith("image")) {
                        String normUrl = UrlUtils.normalizeUrl(UrlUtils.removeQuery(JSONUtil.getString(json, "URL")));
                        int width = jsonAttach.optInt("width");
                        int height = jsonAttach.optInt("height");
                        this.put(normUrl, new ImageSize(width, height));
                    }
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        }

        ImageSize getAttachmentSize(final String imageUrl) {
            if (imageUrl == null) {
                return null;
            } else {
                return super.get(UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl)));
            }
        }
    }

    static class ImageSize {
        final int width;
        final int height;
        ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
