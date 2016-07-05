package org.wordpress.android.ui.reader;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.ui.reader.utils.ImageSizeMap;
import org.wordpress.android.ui.reader.utils.ImageSizeMap.ImageSize;
import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils;
import org.wordpress.android.ui.reader.utils.ReaderIframeScanner;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

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
        mWeakWebView = new WeakReference<>(webView);
        mResourceVars = new ReaderResourceVars(webView.getContext());

        mMinFullSizeWidthDp = pxToDp(mResourceVars.fullSizeImageWidthPx / 3);
        mMinMidSizeWidthDp = mMinFullSizeWidthDp / 2;

        // enable JavaScript in the webView if it's safe to do so, otherwise videos
        // and other embedded content won't work
        webView.getSettings().setJavaScriptEnabled(canEnableJavaScript());
    }

    void beginRender() {
        final Handler handler = new Handler();
        mRenderBuilder = new StringBuilder(getPostContent());

        new Thread() {
            @Override
            public void run() {
                final boolean renderAsTiledGallery = shouldRenderAsTiledGallery();

                if (!renderAsTiledGallery) {
                    resizeImages();
                }

                resizeIframes();

                final String htmlContent = formatPostContentForWebView(mRenderBuilder.toString(), renderAsTiledGallery);
                mRenderBuilder = null;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        renderHtmlContent(htmlContent);
                    }
                });
            }
        }.start();
    }

    public boolean shouldRenderAsTiledGallery() {
        // determine whether a tiled-gallery exists in the content
        final boolean hasTiledGallery = Pattern.compile("tiled-gallery[\\s\"']").matcher(mRenderBuilder.toString())
                .find();

        return hasTiledGallery && mResourceVars.isWideDisplay;
    }

    /*
     * scan the content for images and make sure they're correctly sized for the device
     */
    private void resizeImages() {
        ReaderHtmlUtils.HtmlScannerListener imageListener = new ReaderHtmlUtils.HtmlScannerListener() {
            @Override
            public void onTagFound(String imageTag, String imageUrl) {
                if (!imageUrl.contains("wpcom-smileys")) {
                    replaceImageTag(imageTag, imageUrl);
                }
            }
        };
        ReaderImageScanner scanner = new ReaderImageScanner(mRenderBuilder.toString(), mPost.isPrivate);
        scanner.beginScan(imageListener);
    }

    /*
     * scan the content for iframes and make sure they're correctly sized for the device
     */
    private void resizeIframes() {
        ReaderHtmlUtils.HtmlScannerListener iframeListener = new ReaderHtmlUtils.HtmlScannerListener() {
            @Override
            public void onTagFound(String tag, String src) {
                replaceIframeTag(tag, src);
            }
        };
        ReaderIframeScanner scanner = new ReaderIframeScanner(mRenderBuilder.toString());
        scanner.beginScan(iframeListener);
    }

    /*
     * called once the content is ready to be rendered in the webView
     */
    private void renderHtmlContent(final String htmlContent) {
        mRenderedHtml = htmlContent;

        // make sure webView is still valid (containing fragment may have been detached)
        ReaderWebView webView = mWeakWebView.get();
        if (webView == null || webView.getContext() == null || webView.isDestroyed()) {
            AppLog.w(AppLog.T.READER, "reader renderer > webView invalid");
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
            return "<img class='" + imageClass + "'" +
                    " src='" + newImageUrl + "'" +
                    " width='" + pxToDp(width) + "'" +
                    " height='" + pxToDp(height) + "' />";
        } else {
            return "<img class='" + imageClass + "'" +
                    "src='" + newImageUrl + "'" +
                    " width='" + pxToDp(width) + "' />";
        }
    }

    private String makeFullSizeImageTag(final String imageUrl, int width, int height) {
        int newWidth;
        int newHeight;
        if (width > 0 && height > 0) {
            if (height > width) {
                //noinspection SuspiciousNameCombination
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

        // add the featured image (if any)
        if (shouldAddFeaturedImage()) {
            AppLog.d(AppLog.T.READER, "reader renderer > added featured image");
            content = getFeaturedImageHtml() + content;
        }

        // if this is a Discover post, add a link which shows the blog preview
        if (mPost.isDiscoverPost()) {
            ReaderPostDiscoverData discoverData = mPost.getDiscoverData();
            if (discoverData != null && discoverData.getBlogId() != 0 && discoverData.hasBlogName()) {
                String label = String.format(
                        WordPress.getContext().getString(R.string.reader_discover_visit_blog), discoverData.getBlogName());
                String url = ReaderUtils.makeBlogPreviewUrl(discoverData.getBlogId());

                String htmlDiscover = "<div id='discover'>"
                        + "<a href='" + url + "'>" + label + "</a>"
                        + "</div>";
                content += htmlDiscover;
            }
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
     * replace the passed iframe tag with one that's correctly sized for the device
     */
    private void replaceIframeTag(final String tag, final String src) {
        int width = ReaderHtmlUtils.getWidthAttrValue(tag);
        int height = ReaderHtmlUtils.getHeightAttrValue(tag);

        int newHeight;
        int newWidth;
        if (width > 0 && height > 0) {
            float ratio = ((float) height / (float) width);
            newWidth = mResourceVars.videoWidthPx;
            newHeight = (int) (newWidth * ratio);
        } else {
            newWidth = mResourceVars.videoWidthPx;
            newHeight = mResourceVars.videoHeightPx;
        }

        String newTag = "<iframe src='" + src + "'" +
                " frameborder='0' allowfullscreen='true' allowtransparency='true'" +
                " width='" + pxToDp(newWidth) + "'" +
                " height='" + pxToDp(newHeight) + "' />";

        int start = mRenderBuilder.indexOf(tag);
        if (start == -1) {
            AppLog.w(AppLog.T.READER, "reader renderer > iframe not found in builder");
            return;
        }

        mRenderBuilder.replace(start, start + tag.length(), newTag);
    }

    /*
     * returns the full content, including CSS, that will be shown in the WebView for this post
     */
    private String formatPostContentForWebView(final String content, boolean renderAsTiledGallery) {
        // unique CSS class assigned to the gallery elements for easy selection
        final String galleryOnlyClass = "gallery-only-class" + new Random().nextInt(1000);

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sbHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8' />");

        // title isn't necessary, but it's invalid html5 without one
        sbHtml.append("<title>Reader Post</title>")

        // https://developers.google.com/chrome/mobile/docs/webview/pixelperfect
        .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")

        // use Merriweather font assets
        .append("<link href='file:///android_asset/merriweather.css' rel='stylesheet' type='text/css'>")

        .append("<style type='text/css'>")
        .append("  body { font-family: Merriweather, serif; font-weight: 400; margin: 0px; padding: 0px;}")
        .append("  body, p, div { max-width: 100% !important; word-wrap: break-word; }")

        // set line-height, font-size but not for gallery divs when rendering as tiled gallery as those will be
        // handled with the .tiled-gallery rules bellow.
        .append("  p, div" + (renderAsTiledGallery ? ":not(." + galleryOnlyClass + ")" : "") +
                ", li { line-height: 1.6em; font-size: 0.95em; }")

        .append("  h1, h2 { line-height: 1.2em; }")

        // counteract pre-defined height/width styles, except for the tiled-gallery divs when rendering as tiled gallery
        // as those will be handled with the .tiled-gallery rules bellow.
        .append("  p, div" + (renderAsTiledGallery ? ":not(." + galleryOnlyClass + ")" : "") +
                ", dl, table { width: auto !important; height: auto !important; }")

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
        .append("  img { max-width: 100%; width: auto; height: auto; }")
        .append("  img.size-none { max-width: 100% !important; height: auto !important; }")

        // center large/medium images, provide a small bottom margin, and add a background color
        // so the user sees something while they're loading
        .append("  img.size-full, img.size-large, img.size-medium {")
        .append("     display: block; margin-left: auto; margin-right: auto;")
        .append("     background-color: ").append(mResourceVars.greyExtraLightStr).append(";")
        .append("     margin-bottom: ").append(mResourceVars.marginSmallPx).append("px; }");

        if (renderAsTiledGallery) {
            sbHtml
            .append("  .tiled-gallery {")
            .append("    clear:both;")
            .append("    overflow:hidden;}")
            .append(".tiled-gallery img {")
            .append("    margin:2px !important;}")
            .append(".tiled-gallery .gallery-group {")
            .append("    float:left;")
            .append("    position:relative;}")
            .append(".tiled-gallery .tiled-gallery-item {")
            .append("    float:left;")
            .append("    margin:0;")
            .append("    position:relative;")
            .append("    width:inherit;}")
            .append(".tiled-gallery .gallery-row {")
            .append("    position: relative;")
            .append("    left: 50%;")
            .append("    -webkit-transform: translateX(-50%);")
            .append("    -moz-transform: translateX(-50%);")
            .append("    transform: translateX(-50%);")
            .append("    overflow:hidden;}")
            .append(".tiled-gallery .tiled-gallery-item a {")
            .append("    background:transparent;")
            .append("    border:none;")
            .append("    color:inherit;")
            .append("    margin:0;")
            .append("    padding:0;")
            .append("    text-decoration:none;")
            .append("    width:auto;}")
            .append(".tiled-gallery .tiled-gallery-item img,")
            .append(".tiled-gallery .tiled-gallery-item img:hover {")
            .append("    background:none;")
            .append("    border:none;")
            .append("    box-shadow:none;")
            .append("    max-width:100%;")
            .append("    padding:0;")
            .append("    vertical-align:middle;}")
            .append(".tiled-gallery-caption {")
            .append("    background:#eee;")
            .append("    background:rgba( 255,255,255,0.8 );")
            .append("    color:#333;")
            .append("    font-size:13px;")
            .append("    font-weight:400;")
            .append("    overflow:hidden;")
            .append("    padding:10px 0;")
            .append("    position:absolute;")
            .append("    bottom:0;")
            .append("    text-indent:10px;")
            .append("    text-overflow:ellipsis;")
            .append("    width:100%;")
            .append("    white-space:nowrap;}")
            .append(".tiled-gallery .tiled-gallery-item-small .tiled-gallery-caption {")
            .append("    font-size:11px;}")
            .append(".widget-gallery .tiled-gallery-unresized {")
            .append("    visibility:hidden;")
            .append("    height:0px;")
            .append("    overflow:hidden;}")
            .append(".tiled-gallery .tiled-gallery-item img.grayscale {")
            .append("    position:absolute;")
            .append("    left:0;")
            .append("    top:0;}")
            .append(".tiled-gallery .tiled-gallery-item img.grayscale:hover {")
            .append("    opacity:0;}")
            .append(".tiled-gallery.type-circle .tiled-gallery-item img {")
            .append("    border-radius:50% !important;}")
            .append(".tiled-gallery.type-circle .tiled-gallery-caption {")
            .append("    display:none;")
            .append("    opacity:0;}");
        }

        // see http://codex.wordpress.org/CSS#WordPress_Generated_Classes
        sbHtml.append("  .wp-caption { background-color: ").append(mResourceVars.greyExtraLightStr).append("; }")
        .append("  .wp-caption img { margin-top: 0px; margin-bottom: 0px; }")
        .append("  .wp-caption .wp-caption-text {")
        .append("       font-size: smaller; line-height: 1.2em; margin: 0px;")
        .append("       padding: ").append(mResourceVars.marginExtraSmallPx).append("px; ")
        .append("       color: ").append(mResourceVars.greyMediumDarkStr).append("; }")

        // attribution for Discover posts
        .append("  div#discover { ")
        .append("       margin-top: ").append(mResourceVars.marginSmallPx).append("px;")
        .append("       font-family: sans-serif;")
        .append(" }")

        // horizontally center iframes
        .append("   iframe { display: block; margin: 0 auto; }")

        // make sure html5 videos fit the browser width and use 16:9 ratio (YouTube standard)
        .append("  video {")
        .append("     width: ").append(pxToDp(mResourceVars.videoWidthPx)).append("px !important;")
        .append("     height: ").append(pxToDp(mResourceVars.videoHeightPx)).append("px !important; }")

        .append("</style>");

        // add a custom CSS class to (any) tiled gallery elements to make them easier selectable for various rules
        final List<String> classAmendRegexes = Arrays.asList(
                "(tiled-gallery)([\\s\"\'])",
                "(gallery-row)([\\s\"'])",
                "(gallery-group)([\\s\"'])",
                "(tiled-gallery-item)([\\s\"'])");
        String contentCustomised = content;
        for (String classToAmend : classAmendRegexes) {
            contentCustomised = contentCustomised.replaceAll(classToAmend, "$1 " + galleryOnlyClass + "$2");
        }

        sbHtml.append("</head><body>")
        .append(contentCustomised)
        .append("</body></html>");

        return sbHtml.toString();
    }

    private ImageSize getImageSize(final String imageTag, final String imageUrl) {
        ImageSize size = getImageSizeFromAttachments(imageUrl);
        if (size == null && imageTag.contains("data-orig-size=")) {
            size = getImageOriginalSizeFromAttributes(imageTag);
        }
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
        return mAttachmentSizes.getImageSize(imageUrl);
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

    private ImageSize getImageOriginalSizeFromAttributes(final String imageTag) {
        return new ImageSize(
                ReaderHtmlUtils.getOriginalWidthAttrValue(imageTag),
                ReaderHtmlUtils.getOriginalHeightAttrValue(imageTag));
    }

    private ImageSize getImageSizeFromAttributes(final String imageTag) {
        return new ImageSize(
                ReaderHtmlUtils.getWidthAttrValue(imageTag),
                ReaderHtmlUtils.getHeightAttrValue(imageTag));
    }

    private int pxToDp(int px) {
        if (px == 0) {
            return 0;
        }
        return DisplayUtils.pxToDp(WordPress.getContext(), px);
    }

    /*
     * javascript should only be enabled for WordPress.com blogs (not feeds or Jetpack blogs)
     */
    private boolean canEnableJavaScript() {
        return mPost.isWP() && !mPost.isJetpack;
    }




}
