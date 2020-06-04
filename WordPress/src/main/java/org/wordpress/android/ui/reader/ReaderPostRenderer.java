package org.wordpress.android.ui.reader;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.ui.reader.utils.FeaturedImageUtils;
import org.wordpress.android.ui.reader.utils.ImageSizeMap;
import org.wordpress.android.ui.reader.utils.ImageSizeMap.ImageSize;
import org.wordpress.android.ui.reader.utils.ReaderEmbedScanner;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * generates and displays the HTML for post detail content - main purpose is to assign the
 * height/width attributes on image tags to (1) avoid the webView resizing as images are
 * loaded, and (2) avoid requesting images at a size larger than the display
 * <p>
 * important to note that displayed images rely on dp rather than px sizes due to the
 * fact that WebView "converts CSS pixel values to density-independent pixel values"
 * http://developer.android.com/guide/webapps/targeting.html
 */
public class ReaderPostRenderer {
    private final ReaderResourceVars mResourceVars;
    private final ReaderPost mPost;
    private final int mMinFullSizeWidthDp;
    private final int mMinMidSizeWidthDp;
    private final WeakReference<ReaderWebView> mWeakWebView;

    private StringBuilder mRenderBuilder;
    private String mRenderedHtml;
    private ImageSizeMap mAttachmentSizes;
    private FeaturedImageUtils mFeaturedImageUtils;

    @SuppressLint("SetJavaScriptEnabled")
    public ReaderPostRenderer(ReaderWebView webView, ReaderPost post, FeaturedImageUtils featuredImageUtils) {
        if (webView == null) {
            throw new IllegalArgumentException("ReaderPostRenderer requires a webView");
        }
        if (post == null) {
            throw new IllegalArgumentException("ReaderPostRenderer requires a post");
        }

        mPost = post;
        mWeakWebView = new WeakReference<>(webView);
        mResourceVars = new ReaderResourceVars(webView.getContext());
        mFeaturedImageUtils = featuredImageUtils;

        mMinFullSizeWidthDp = pxToDp(mResourceVars.mFullSizeImageWidthPx / 3);
        mMinMidSizeWidthDp = mMinFullSizeWidthDp / 2;

        // enable JavaScript in the webView, otherwise videos and other embedded content won't
        // work - note that the content is scrubbed on the backend so this is considered safe
        webView.getSettings().setJavaScriptEnabled(true);
    }

    public void beginRender() {
        final Handler handler = new Handler();
        mRenderBuilder = new StringBuilder(getPostContent());

        new Thread() {
            @Override
            public void run() {
                final boolean hasTiledGallery = hasTiledGallery(mRenderBuilder.toString());

                if (!(hasTiledGallery && mResourceVars.mIsWideDisplay)) {
                    resizeImages();
                }

                resizeIframes();

                // Get the set of JS scripts to inject in our Webview to support some specific Embeds.
                Set<String> jsToInject = injectJSForSpecificEmbedSupport();

                final String htmlContent =
                        formatPostContentForWebView(
                                mRenderBuilder.toString(),
                                jsToInject,
                                hasTiledGallery,
                                mResourceVars.mIsWideDisplay);

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

    public static boolean hasTiledGallery(String text) {
        // determine whether a tiled-gallery exists in the content
        return Pattern.compile("tiled-gallery[\\s\"']").matcher(text).find();
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
        String content = mRenderBuilder.toString();
        ReaderImageScanner scanner = new ReaderImageScanner(content, mPost.isPrivate);
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
        String content = mRenderBuilder.toString();
        ReaderIframeScanner scanner = new ReaderIframeScanner(content);
        scanner.beginScan(iframeListener);
    }

    private Set<String> injectJSForSpecificEmbedSupport() {
        final Set<String> jsToInject = new HashSet<>();
        ReaderHtmlUtils.HtmlScannerListener embedListener = new ReaderHtmlUtils.HtmlScannerListener() {
            @Override
            public void onTagFound(String tag, String src) {
                jsToInject.add(src);
            }
        };
        String content = mRenderBuilder.toString();
        ReaderEmbedScanner scanner = new ReaderEmbedScanner(content);
        scanner.beginScan(embedListener);
        return jsToInject;
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
        String newImageUrl = ReaderUtils.getResizedImageUrl(imageUrl, width, height, mPost.isPrivate,
                false); // don't use atomic proxy for WebView images
        if (height > 0) {
            return "<img class='" + imageClass + "'"
                   + " src='" + newImageUrl + "'"
                   + " width='" + pxToDp(width) + "'"
                   + " height='" + pxToDp(height) + "' />";
        } else {
            return "<img class='" + imageClass + "'"
                   + "src='" + newImageUrl + "'"
                   + " width='" + pxToDp(width) + "' />";
        }
    }

    private String makeFullSizeImageTag(final String imageUrl, int width, int height) {
        int newWidth;
        int newHeight;
        if (width > 0 && height > 0) {
            if (height > width) {
                //noinspection SuspiciousNameCombination
                newHeight = mResourceVars.mFullSizeImageWidthPx;
                float ratio = ((float) width / (float) height);
                newWidth = (int) (newHeight * ratio);
            } else {
                float ratio = ((float) height / (float) width);
                newWidth = mResourceVars.mFullSizeImageWidthPx;
                newHeight = (int) (newWidth * ratio);
            }
        } else {
            newWidth = mResourceVars.mFullSizeImageWidthPx;
            newHeight = 0;
        }

        return makeImageTag(imageUrl, newWidth, newHeight, "size-full");
    }

    /*
     * returns true if the post has a featured image and the featured image is not found in the post body
     */
    private boolean shouldAddFeaturedImage() {
        return mPost.hasFeaturedImage()
               && !PhotonUtils.isMshotsUrl(mPost.getFeaturedImage())
               && mFeaturedImageUtils.showFeaturedImage(mPost.getFeaturedImage(), mPost.getText());
    }

    /*
     * returns the basic content of the post tweaked for use here
     */
    private String getPostContent() {
        String content = mPost.shouldShowExcerpt() ? mPost.getExcerpt() : mPost.getText();

        // some content (such as Vimeo embeds) don't have "http:" before links
        content = content.replace("src=\"//", "src=\"http://");

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
                        WordPress.getContext().getString(R.string.reader_discover_visit_blog),
                        discoverData.getBlogName());
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
                mResourceVars.mFullSizeImageWidthPx,
                mResourceVars.mFeaturedImageHeightPx,
                mPost.isPrivate,
                mPost.isPrivateAtomic);

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
            newWidth = mResourceVars.mVideoWidthPx;
            newHeight = (int) (newWidth * ratio);
        } else {
            newWidth = mResourceVars.mVideoWidthPx;
            newHeight = mResourceVars.mVideoHeightPx;
        }

        String newTag = "<iframe src='" + src + "'"
                        + " frameborder='0' allowfullscreen='true' allowtransparency='true'"
                        + " width='" + pxToDp(newWidth) + "'"
                        + " height='" + pxToDp(newHeight) + "' />";

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
    private String formatPostContentForWebView(final String content, final Set<String> jsToInject,
                                               boolean hasTiledGallery, boolean isWideDisplay) {
        final boolean renderAsTiledGallery = hasTiledGallery && isWideDisplay;

        // unique CSS class assigned to the gallery elements for easy selection
        final String galleryOnlyClass = "gallery-only-class" + new Random().nextInt(1000);

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sbHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8' />");

        // title isn't necessary, but it's invalid html5 without one
        sbHtml.append("<title>Reader Post</title>")
              .append("<link rel=\"stylesheet\" type=\"text/css\"\n"
                      + "          href=\"https://wordpress.com/calypso/evergreen/async-load-blocks-reader-full-post.c8fffa7882601876d9f3.min.css/">\n");
        // https://developers.google.com/chrome/mobile/docs/webview/pixelperfect
        sbHtml.append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
              .append("<style type='text/css'>");
        appendMappedColors(sbHtml);
              // force font style
        sbHtml.append(" body.reader-full-post__story-content { font-family: 'Noto Serif', serif; font-weight: 400; font-size: 16px; margin: 0px; padding: 0px; }")
              .append(" p, div, li { line-height: 1.6em; font-size: 100%; }")
              .append(" body, p, div { max-width: 100% !important; word-wrap: break-word; }")
              // set line-height, font-size but not for .tiled-gallery divs when rendering as tiled
              // gallery as those will be handled with the .tiled-gallery rules bellow.
              .append(" p, div" + (renderAsTiledGallery ? ":not(." + galleryOnlyClass + ")" : "")
                      + ", li { line-height: 1.6em; font-size: 100%; }")
              .append(" h1, h2, h3 { line-height: 1.6em; }")
              // counteract pre-defined height/width styles, expect for the tiled-gallery divs when rendering as
              // tiled gallery as those will be handled with the .tiled-gallery rules bellow.
              .append(" p, div" + (renderAsTiledGallery ? ":not(.tiled-gallery.*)" : "")
                      + ", dl, table { width: auto !important; height: auto !important; }")
              // make sure long strings don't force the user to scroll horizontally
              .append(" body, p, div, a { word-wrap: break-word; }")
               // change horizontal line color
              .append(" hr { border-color: ").append(mResourceVars.mGreyExtraLightStr).append("; }")
              // use a consistent top/bottom margin for paragraphs, with no top margin for the first one
              .append(" p { margin-top: ").append(mResourceVars.mMarginMediumPx).append("px;")
              .append(" margin-bottom: ").append(mResourceVars.mMarginMediumPx).append("px; }")
              .append(" p:first-child { margin-top: 0px; }")
              // add background color and padding to pre blocks, and add overflow scrolling
              // so user can scroll the block if it's wider than the display
              .append(" pre { overflow-x: scroll;")
              .append(" background-color: ").append(mResourceVars.mGreyExtraLightStr).append(";")
              .append(" padding: ").append(mResourceVars.mMarginMediumPx).append("px; }")

              // show links in the same color they are elsewhere in the app
              .append(" a:any-link { text-decoration: none; color: var(--main-link-color); }")
              // make sure images aren't wider than the display, strictly enforced for images without size
              .append(" img { max-width: 100%; width: auto; height: auto; }")
              .append(" img.size-none { max-width: 100% !important; height: auto !important; }")
              // center large/medium images, provide a small bottom margin, and add a background color
              // so the user sees something while they're loading
              .append(" img.size-full, img.size-large, img.size-medium {")
              .append(" display: block; margin-left: auto; margin-right: auto;")
              .append(" background-color: ").append(mResourceVars.mGreyMediumDarkStr).append(";")
              .append(" margin-bottom: ").append(mResourceVars.mMarginMediumPx).append("px; }");

        if (renderAsTiledGallery) {
            // tiled-gallery related styles
            sbHtml
                    .append(".tiled-gallery {")
                    .append(" clear:both;")
                    .append(" overflow:hidden;}")
                    .append(".tiled-gallery img {")
                    .append(" margin:2px !important;}")
                    .append(".tiled-gallery .gallery-group {")
                    .append(" float:left;")
                    .append(" position:relative;}")
                    .append(".tiled-gallery .tiled-gallery-item {")
                    .append(" float:left;")
                    .append(" margin:0;")
                    .append(" position:relative;")
                    .append(" width:inherit;}")
                    .append(".tiled-gallery .gallery-row {")
                    .append(" position: relative;")
                    .append(" left: 50%;")
                    .append(" -webkit-transform: translateX(-50%);")
                    .append(" -moz-transform: translateX(-50%);")
                    .append(" transform: translateX(-50%);")
                    .append(" overflow:hidden;}")
                    .append(".tiled-gallery .tiled-gallery-item a {")
                    .append(" background:transparent;")
                    .append(" border:none;")
                    .append(" color:inherit;")
                    .append(" margin:0;")
                    .append(" padding:0;")
                    .append(" text-decoration:none;")
                    .append(" width:auto;}")
                    .append(".tiled-gallery .tiled-gallery-item img,")
                    .append(".tiled-gallery .tiled-gallery-item img:hover {")
                    .append(" background:none;")
                    .append(" border:none;")
                    .append(" box-shadow:none;")
                    .append(" max-width:100%;")
                    .append(" padding:0;")
                    .append(" vertical-align:middle;}")
                    .append(".tiled-gallery-caption {")
                    .append(" background:#eee;")
                    .append(" background:rgba( 255,255,255,0.8 );")
                    .append(" color:#333;")
                    .append(" font-size:13px;")
                    .append(" font-weight:400;")
                    .append(" overflow:hidden;")
                    .append(" padding:10px 0;")
                    .append(" position:absolute;")
                    .append(" bottom:0;")
                    .append(" text-indent:10px;")
                    .append(" text-overflow:ellipsis;")
                    .append(" width:100%;")
                    .append(" white-space:nowrap;}")
                    .append(".tiled-gallery .tiled-gallery-item-small .tiled-gallery-caption {")
                    .append(" font-size:11px;}")
                    .append(".widget-gallery .tiled-gallery-unresized {")
                    .append(" visibility:hidden;")
                    .append(" height:0px;")
                    .append(" overflow:hidden;}")
                    .append(".tiled-gallery .tiled-gallery-item img.grayscale {")
                    .append(" position:absolute;")
                    .append(" left:0;")
                    .append(" top:0;}")
                    .append(".tiled-gallery .tiled-gallery-item img.grayscale:hover {")
                    .append(" opacity:0;}")
                    .append(".tiled-gallery.type-circle .tiled-gallery-item img {")
                    .append(" border-radius:50% !important;}")
                    .append(".tiled-gallery.type-circle .tiled-gallery-caption {")
                    .append(" display:none;")
                    .append(" opacity:0;}");
        }

        // see http://codex.wordpress.org/CSS#WordPress_Generated_Classes
        sbHtml
                .append(" .wp-caption img { margin-top: 0px; margin-bottom: 0px; }")
                .append(" .wp-caption .wp-caption-text {")
                .append(" font-size: smaller; line-height: 1.2em; margin: 0px;")
                .append(" text-align: center;")
                .append(" padding: ").append(mResourceVars.mMarginMediumPx).append("px; ")
                .append(" color: var(--color-neutral-0); }")
                // attribution for Discover posts
                .append(" div#discover { ")
                .append(" margin-top: ").append(mResourceVars.mMarginMediumPx).append("px;")
                .append(" font-family: sans-serif;")
                .append(" }")
                // horizontally center iframes
                .append(" iframe { display: block; margin: 0 auto; }")
                // make sure html5 videos fit the browser width and use 16:9 ratio (YouTube standard)
                .append(" video {")
                .append(" width: ").append(pxToDp(mResourceVars.mVideoWidthPx)).append("px !important;")
                .append(" height: ").append(pxToDp(mResourceVars.mVideoHeightPx)).append("px !important; }")
                // hide forms, form-related elements, legacy RSS sharing links and other ad-related content
                // http://bit.ly/2FUTvsP
                .append(" form, input, select, button textarea { display: none; }")
                .append(" div.feedflare { display: none; }")
                .append(" .sharedaddy, .jp-relatedposts, .mc4wp-form, .wpcnt, ")
                .append(" .OUTBRAIN, .adsbygoogle { display: none; }")
                .append("</style>");

        // add a custom CSS class to (any) tiled gallery elements to make them easier selectable for various rules
        final List<String> classAmendRegexes = Arrays.asList(
                "(tiled-gallery) ([\\s\"\'])",
                "(gallery-row) ([\\s\"'])",
                "(gallery-group) ([\\s\"'])",
                "(tiled-gallery-item) ([\\s\"'])");
        String contentCustomised = content;
        for (String classToAmend : classAmendRegexes) {
            contentCustomised = contentCustomised.replaceAll(classToAmend, "$1 " + galleryOnlyClass + "$2");
        }

        for (String jsUrl : jsToInject) {
            sbHtml.append("<script src=\"").append(jsUrl).append("\" type=\"text/javascript\" async></script>");
        }

        sbHtml.append("</head><body class=\"reader-full-post reader-full-post__story-content\">")
              .append(contentCustomised)
              .append("</body></html>");

        return sbHtml.toString();
    }

    private void appendMappedColors(StringBuilder sb) {
        sb.append(" :root { ")
          .append("--color-text: ").append(mResourceVars.mTextColor).append("; ")
          .append("--color-neutral-70: ").append(mResourceVars.mTextColor).append("; ")
          .append("--color-neutral-0: ").append(mResourceVars.mGreyMediumDarkStr).append("; ")
          .append("--color-neutral-50: ").append(mResourceVars.mGreyExtraLightStr).append("; ")
          .append("--main-link-color: ").append(mResourceVars.mLinkColorStr).append("; ")
          .append("} ");
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
            mAttachmentSizes = new ImageSizeMap(mPost.getText(), mPost.getAttachmentsJson());
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
}
