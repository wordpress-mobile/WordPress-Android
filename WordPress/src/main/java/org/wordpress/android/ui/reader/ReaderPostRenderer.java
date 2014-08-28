package org.wordpress.android.ui.reader;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

/**
 * generates the HTML for displaying post detail content
 */
public class ReaderPostRenderer {

    private final ReaderResourceVars mResourceVars;
    private final ReaderPost mPost;

    ReaderPostRenderer(Context context, ReaderPost post) {
        mResourceVars = new ReaderResourceVars(context);
        mPost = post;
    }

    /*
     * returns the basic content of the post tweaked for use here
     */
    String getPostContent() {
        if (mPost == null) {
            return "";
        } else if (mPost.hasText()) {
            // some content (such as Vimeo embeds) don't have "http:" before links, correct this here
            String content = mPost.getText().replace("src=\"//", "src=\"http://");
            // insert video div before content if this is a VideoPress post (video otherwise won't appear)
            if (mPost.isVideoPress) {
                content = makeVideoDiv(mPost.getFeaturedVideo(), mPost.getFeaturedImage()) + content;
            } else if (mPost.hasFeaturedImage() && !PhotonUtils.isMshotsUrl(mPost.getFeaturedImage())) {
                // if the post has a featured image other than an mshot that's not in the content,
                // add it to the content
                Uri uri = Uri.parse(mPost.getFeaturedImage());
                String path = StringUtils.notNullStr(uri.getLastPathSegment());
                if (!content.contains(path)) {
                    AppLog.d(AppLog.T.READER, "reader post detail > added featured image to content");
                    content = String.format("<p><img class='img.size-full' src='%s' /></p>", mPost.getFeaturedImage())
                            + content;
                }
            }
            return content;
        } else if (mPost.hasFeaturedImage()) {
            // some photo blogs have posts with empty content but still have a featured image, so
            // use the featured image as the content
            return String.format("<p><img class='img.size-full' src='%s' /></p>", mPost.getFeaturedImage());
        } else {
            return "";
        }
    }

    /*
     * returns the full content, including CSS, that will be shown in the WebView for this post
     */
    String getPostContentForWebView(Context context) {
        if (mPost == null || context == null) {
            return "";
        }

        String content = getPostContent();

        StringBuilder sbHtml = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8' />");

        // title isn't strictly necessary, but source is invalid html5 without one
        sbHtml.append("<title>Reader Post</title>");

        // https://developers.google.com/chrome/mobile/docs/webview/pixelperfect
        sbHtml.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");

        // use "Open Sans" Google font
        sbHtml.append("<link rel='stylesheet' type='text/css' href='http://fonts.googleapis.com/css?family=Open+Sans' />");

        sbHtml.append("<style type='text/css'>")
                .append("  body { font-family: 'Open Sans', sans-serif; margin: 0px; padding: 0px;}")
                .append("  body, p, div { max-width: 100% !important; word-wrap: break-word; }")
                .append("  p, div { line-height: 1.6em; font-size: 1em; }")
                .append("  h1, h2 { line-height: 1.2em; }");

        // make sure long strings don't force the user to scroll horizontally
        sbHtml.append("  body, p, div, a { word-wrap: break-word; }");

        // use a consistent top/bottom margin for paragraphs, with no top margin for the first one
        sbHtml.append(String.format("  p { margin-top: %dpx; margin-bottom: %dpx; }",
                mResourceVars.marginSmall, mResourceVars.marginSmall))
                .append("    p:first-child { margin-top: 0px; }");

        // add border, background color, and padding to pre blocks, and add overflow scrolling
        // so user can scroll the block if it's wider than the display
        sbHtml.append("  pre { overflow-x: scroll;")
                .append("        border: 1px solid ").append(mResourceVars.greyLightStr).append("; ")
                .append("        background-color: ").append(mResourceVars.greyExtraLightStr).append("; ")
                .append("        padding: ").append(mResourceVars.marginSmall).append("px; }");

        // add a left border to blockquotes
        sbHtml.append("  blockquote { margin-left: ").append(mResourceVars.marginSmall).append("px; ")
                .append("               padding-left: ").append(mResourceVars.marginSmall).append("px; ")
                .append("               border-left: 3px solid ").append(mResourceVars.greyLightStr).append("; }");

        // show links in the same color they are elsewhere in the app
        sbHtml.append("  a { text-decoration: none; color: ").append(mResourceVars.linkColorStr).append("; }");

        // if javascript is allowed, make sure embedded videos fit the browser width and
        // use 16:9 ratio (YouTube standard) - if not allowed, hide iframes/embeds
        if (canEnableJavaScript(mPost)) {
            int videoWidth = DisplayUtils.pxToDp(context,
                    mResourceVars.fullSizeImageWidth - (mResourceVars.marginLarge * 2));
            int videoHeight = (int) (videoWidth * 0.5625f);
            sbHtml.append("  iframe, embed { width: ").append(videoWidth).append("px !important;")
                    .append("                  height: ").append(videoHeight).append("px !important; }");
        } else {
            sbHtml.append("  iframe, embed { display: none; }");
        }

        // don't allow any image to be wider than the screen
        sbHtml.append("  img { max-width: 100% !important; height: auto;}");

        // show large wp images full-width (unnecessary in most cases since they'll already be at least
        // as wide as the display, except maybe when viewed on a large landscape tablet)
        sbHtml.append("  img.size-full, img.size-large { display: block; width: 100% !important; height: auto; }");

        // center medium-sized wp image
        sbHtml.append("  img.size-medium { display: block; margin-left: auto !important; margin-right: auto !important; }");

        // tiled image galleries look bad on mobile due to their hard-coded DIV and IMG sizes, so if
        // content contains a tiled image gallery, remove the height params and replace the width
        // params with ones that make images fit the width of the listView item, then adjust the
        // relevant CSS classes so their height/width are auto, and add top/bottom margin to images
        if (content.contains("tiled-gallery-item")) {
            String widthParam = "w=" + Integer.toString(mResourceVars.fullSizeImageWidth);
            content = content.replaceAll("w=[0-9]+", widthParam).replaceAll("h=[0-9]+", "");
            sbHtml.append("  div.gallery-row, div.gallery-group { width: auto !important; height: auto !important; }")
                    .append("  div.tiled-gallery-item img { ")
                    .append("     width: auto !important; height: auto !important;")
                    .append("     margin-top: ").append(mResourceVars.marginExtraSmall).append("px; ")
                    .append("     margin-bottom: ").append(mResourceVars.marginExtraSmall).append("px; ")
                    .append("  }")
                    .append("  div.tiled-gallery-caption { clear: both; }");
        }

        sbHtml.append("</style></head><body>")
                .append(content)
                .append("</body></html>");

        return sbHtml.toString();
    }

    /*
     * creates formatted div for passed video with passed (optional) thumbnail
     */
    private static final String OVERLAY_IMG = "file:///android_asset/ic_reader_video_overlay.png";

    private String makeVideoDiv(String videoUrl, String thumbnailUrl) {
        if (TextUtils.isEmpty(videoUrl)) {
            return "";
        }

        // sometimes we get src values like "//player.vimeo.com/video/70534716" - prefix these with http:
        if (videoUrl.startsWith("//")) {
            videoUrl = "http:" + videoUrl;
        }

        int overlaySz = mResourceVars.videoOverlaySize / 2;
        if (TextUtils.isEmpty(thumbnailUrl)) {
            return String.format("<div class='wpreader-video' align='center'><a href='%s'><img style='width:%dpx; height:%dpx; display:block;' src='%s' /></a></div>", videoUrl, overlaySz, overlaySz, OVERLAY_IMG);
        } else {
            return "<div style='position:relative'>"
                    + String.format("<a href='%s'><img src='%s' style='width:100%%; height:auto;' /></a>", videoUrl, thumbnailUrl)
                    + String.format("<a href='%s'><img src='%s' style='width:%dpx; height:%dpx; position:absolute; left:0px; right:0px; top:0px; bottom:0px; margin:auto;'' /></a>", videoUrl, OVERLAY_IMG, overlaySz, overlaySz)
                    + "</div>";
        }
    }

    /*
     * javascript should only be enabled for wp blogs (not external feeds)
     */
    static boolean canEnableJavaScript(ReaderPost post) {
        return (post != null && post.isWP());
    }
}
