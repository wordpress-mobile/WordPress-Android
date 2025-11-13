package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.wordpress.android.R
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.support.createJsObject
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences.ThemeValues
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences.ThemeValues.Companion.from
import org.wordpress.android.ui.reader.utils.ImageSizeMap
import org.wordpress.android.ui.reader.utils.ReaderEmbedScanner
import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils
import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils.HtmlScannerListener
import org.wordpress.android.ui.reader.utils.ReaderIframeScanner
import org.wordpress.android.ui.reader.utils.ReaderImageScanner
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.views.ReaderWebView
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.StringUtils
import java.lang.ref.WeakReference
import java.text.Bidi
import java.util.Random
import java.util.regex.Pattern

/**
 * generates and displays the HTML for post detail content - main purpose is to assign the
 * height/width attributes on image tags to (1) avoid the webView resizing as images are
 * loaded, and (2) avoid requesting images at a size larger than the display
 *
 *
 * important to note that displayed images rely on dp rather than px sizes due to the
 * fact that WebView "converts CSS pixel values to density-independent pixel values"
 * http://developer.android.com/guide/webapps/targeting.html
 */
class ReaderPostRenderer(
    webView: ReaderWebView,
    post: ReaderPost,
    private val cssProvider: ReaderCssProvider,
    private val readingPreferences: ReaderReadingPreferences
) {
    private val resourceVars: ReaderResourceVars = ReaderResourceVars(webView.context)
    private val readerPost: ReaderPost = post
    private val weakWebView: WeakReference<ReaderWebView> = WeakReference(webView)

    private val minFullSizeWidthDp: Int
    private val minMidSizeWidthDp: Int

    private var renderBuilder = StringBuilder()

    private var attachmentSizes: ImageSizeMap? = null
    private val readingPreferencesTheme: ThemeValues = from(webView.context, this.readingPreferences.theme)
    private var postMessageListener: ReaderPostMessageListener? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        @Suppress("MagicNumber")
        minFullSizeWidthDp = pxToDp(resourceVars.fullSizeImageWidthPx / 3)
        minMidSizeWidthDp = minFullSizeWidthDp / 2

        // enable JavaScript in the webView, otherwise videos and other embedded content won't
        // work - note that the content is scrubbed on the backend so this is considered safe
        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        setWebViewMessageHandler(webView)
    }

    fun beginRender() {
        renderBuilder = StringBuilder(getPostContent())

        coroutineScope.launch {
            val hasTiledGallery = hasTiledGallery(renderBuilder.toString())
            if (resourceVars.isWideDisplay && !hasTiledGallery) {
                resizeImages()
            }

            resizeIframes()

            // inject the set of JS scripts to inject in our WebView to support some specific Embeds.
            val jsToInject = injectJSForSpecificEmbedSupport()

            val htmlContent = formatPostContentForWebView(
                content = renderBuilder.toString(),
                jsToInject = jsToInject,
                hasTiledGallery = hasTiledGallery,
                isWideDisplay = resourceVars.isWideDisplay
            )

            withContext(Dispatchers.Main) {
                renderHtmlContent(htmlContent)
                renderBuilder.clear()
            }
        }
    }

    /*
     * scan the content for images and make sure they're correctly sized for the device
     */
    private fun resizeImages() {
        val imageListener =
            HtmlScannerListener { imageTag, imageUrl ->
                // Exceptions which should keep their original tag attributes
                if (imageUrl.contains("wpcom-smileys") || imageTag.contains("wp-story")) {
                    return@HtmlScannerListener
                }
                replaceImageTag(imageTag, imageUrl)
            }
        val content = renderBuilder.toString()
        val scanner = ReaderImageScanner(content, readerPost.isPrivate)
        scanner.beginScan(imageListener)
    }

    /*
     * scan the content for iframes and make sure they're correctly sized for the device
     */
    private fun resizeIframes() {
        val iframeListener =
            HtmlScannerListener { tag, src ->
                replaceIframeTag(
                    tag,
                    src
                )
            }
        val content = renderBuilder.toString()
        val scanner = ReaderIframeScanner(content)
        scanner.beginScan(iframeListener)
    }

    private fun injectJSForSpecificEmbedSupport(): Set<String> {
        val jsToInject: MutableSet<String> = HashSet()
        val embedListener =
            HtmlScannerListener { _, src ->
                jsToInject.add(
                    src
                )
            }
        val content = renderBuilder.toString()
        val scanner = ReaderEmbedScanner(content)
        scanner.beginScan(embedListener)
        return jsToInject
    }

    /*
     * called once the content is ready to be rendered in the webView
     */
    private fun renderHtmlContent(htmlContent: String) {
        // make sure webView is still valid (containing fragment may have been detached)
        val webView = weakWebView.get()
        if (webView == null || webView.context == null || webView.isDestroyed) {
            AppLog.w(AppLog.T.READER, "reader renderer > webView invalid")
            return
        }

        // IMPORTANT: use loadDataWithBaseURL() since loadData() may fail
        // https://code.google.com/p/android/issues/detail?id=4401
        // Use android-app:// scheme as baseUrl to set HTTP referrer for YouTube embeds.
        // Google requires this for embedded videos to work properly.
        // https://developers.google.com/youtube/terms/required-minimum-functionality#set-the-referer
        // https://stackoverflow.com/questions/79761743/youtube-video-in-webview-gives-error-code-153-on-android/79809094#79809094
        webView.loadDataWithBaseURL(
            "https://wordpress.com/reader",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    /*
     * called when image scanner finds an image, tries to replace the image tag with one that
     * has height & width attributes set correctly for the current display, if that fails
     * replaces it with one that has our 'size-none' class
     */
    private fun replaceImageTag(imageTag: String, imageUrl: String) {
        val origSize = getImageSize(imageTag, imageUrl)
        val hasWidth = (origSize != null && origSize.width > 0)
        val isFullSize = hasWidth && (origSize!!.width >= minFullSizeWidthDp)
        val isMidSize = hasWidth
                && (origSize!!.width >= minMidSizeWidthDp)
                && (origSize.width < minFullSizeWidthDp)

        val newImageTag = if (isFullSize) {
            makeFullSizeImageTag(imageUrl, origSize!!.width, origSize.height)
        } else if (isMidSize) {
            makeImageTag(imageUrl, origSize!!.width, origSize.height, "size-medium")
        } else if (hasWidth) {
            makeImageTag(imageUrl, origSize!!.width, origSize.height, "size-none")
        } else {
            "<img class='size-none' src='$imageUrl' />"
        }

        val start = renderBuilder.indexOf(imageTag)
        if (start == -1) {
            AppLog.w(AppLog.T.READER, "reader renderer > image not found in builder")
            return
        }

        renderBuilder.replace(start, start + imageTag.length, newImageTag)
    }

    private fun makeImageTag(
        imageUrl: String,
        width: Int,
        height: Int,
        imageClass: String
    ): String {
        val newImageUrl = ReaderUtils.getResizedImageUrl(
            imageUrl, width, height, readerPost.isPrivate,
            false
        ) // don't use atomic proxy for WebView images
        return if (height > 0) {
            ("<img class='" + imageClass + "'"
                    + " src='" + newImageUrl + "'"
                    + " width='" + pxToDp(width) + "'"
                    + " height='" + pxToDp(height) + "' />")
        } else {
            ("<img class='" + imageClass + "'"
                    + "src='" + newImageUrl + "'"
                    + " width='" + pxToDp(width) + "' />")
        }
    }

    private fun makeFullSizeImageTag(
        imageUrl: String,
        width: Int,
        height: Int
    ): String {
        val newWidth: Int
        val newHeight: Int
        if (width > 0 && height > 0) {
            if (height > width) {
                newHeight = resourceVars.fullSizeImageWidthPx
                val ratio = (width.toFloat() / height.toFloat())
                newWidth = (newHeight * ratio).toInt()
            } else {
                val ratio = (height.toFloat() / width.toFloat())
                newWidth = resourceVars.fullSizeImageWidthPx
                newHeight = (newWidth * ratio).toInt()
            }
        } else {
            newWidth = resourceVars.fullSizeImageWidthPx
            newHeight = 0
        }

        return makeImageTag(imageUrl, newWidth, newHeight, "size-full")
    }

    /*
     * returns the basic content of the post tweaked for use here
     */
    private fun getPostContent(): String {
        var content = if (readerPost.shouldShowExcerpt()) {
            readerPost.excerpt
        } else {
            readerPost.text
        }
        content = removeInlineStyles(content)

        // some content (such as Vimeo embeds) don't have "http:" before links
        content = content.replace("src=\"//", "src=\"http://")

        // if this is a Discover post, add a link which shows the blog preview
        if (readerPost.isDiscoverPost) {
            val discoverData = readerPost.discoverData
            if (discoverData != null && discoverData.blogId != 0L && discoverData.hasBlogName()) {
                val label = String.format(
                    getContext()
                        .getString(R.string.reader_discover_visit_blog),
                    discoverData.blogName
                )
                val url =
                    ReaderUtils.makeBlogPreviewUrl(
                        discoverData.blogId
                    )

                val htmlDiscover = ("<div id='discover'>"
                        + "<a href='" + url + "'>" + label + "</a>"
                        + "</div>")
                content += htmlDiscover
            }
        }

        return content
    }

    /*
     * Strips inline styles from post content
     */
    @Suppress("TooGenericExceptionCaught")
    private fun removeInlineStyles(content: String): String {
        if (content.isEmpty()) {
            return content
        }

        return try {
            Jsoup.parseBodyFragment(content)
                .allElements
                .removeAttr("style")
                .select("body")
                .html()
        } catch (e: Exception) {
            AppLog.e(AppLog.T.READER, e)
            content
        }
    }

    /*
     * replace the passed iframe tag with one that's correctly sized for the device
     */
    private fun replaceIframeTag(
        tag: String,
        src: String
    ) {
        val width = ReaderHtmlUtils.getWidthAttrValue(tag)
        val height = ReaderHtmlUtils.getHeightAttrValue(tag)

        val newHeight: Int
        val newWidth: Int
        if (width > 0 && height > 0) {
            val ratio = (height.toFloat() / width.toFloat())
            newWidth = resourceVars.videoWidthPx
            newHeight = (newWidth * ratio).toInt()
        } else {
            newWidth = resourceVars.videoWidthPx
            newHeight = resourceVars.videoHeightPx
        }

        val newTag = ("<iframe src='" + src + "'"
                + " frameborder='0' allowfullscreen='true' allowtransparency='true'"
                + " width='" + pxToDp(newWidth) + "'"
                + " height='" + pxToDp(newHeight) + "' />")

        val start = renderBuilder.indexOf(tag)
        if (start == -1) {
            AppLog.w(AppLog.T.READER, "reader renderer > iframe not found in builder")
            return
        }

        renderBuilder.replace(start, start + tag.length, newTag)
    }

    /*
     * returns the full content, including CSS, that will be shown in the WebView for this post
     */
    @SuppressLint("WeakPrng")
    @Suppress("LongMethod")
    private fun formatPostContentForWebView(
        content: String,
        jsToInject: Set<String>,
        hasTiledGallery: Boolean,
        isWideDisplay: Boolean
    ): String {
        val renderAsTiledGallery = hasTiledGallery && isWideDisplay

        // unique CSS class assigned to the gallery elements for easy selection
        val galleryOnlyClass = "gallery-only-class" + Random().nextInt(RANDOM_BOUND)
        val str = if (isRTL(content)) {
            "<!DOCTYPE html><html dir='rtl' lang=''><head><meta charset='UTF-8' />"
        } else {
            "<!DOCTYPE html><html><head><meta charset='UTF-8' />"
        }

        val sbHtml = StringBuilder(str)

        // title isn't necessary, but it's invalid html5 without one
        sbHtml.append("<title>Reader Post</title>")
            .append(
                ("""<link rel="stylesheet" type="text/css"
          href="${cssProvider.getCssUrl()}">""")
            )

        // https://developers.google.com/chrome/mobile/docs/webview/pixelperfect
        sbHtml.append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
            .append("<style type='text/css'>")
        appendMappedColors(sbHtml)

        val contentTextProperties = contentTextProperties
        // force font style and 1px margin from the right to avoid elements being cut off
        sbHtml.append(" body.reader-full-post__story-content { ")
            .append(contentTextProperties)
            .append("margin: 0px; padding: 0px; margin-right: 1px; }")
            .append(" p, div, li { line-height: 1.6em; font-size: 100%; }")
            .append(" body, p, div { max-width: 100% !important; word-wrap: break-word; }")
            // set line-height, font-size but not for .tiled-gallery divs when rendering as tiled
            // gallery as those will be handled with the .tiled-gallery rules bellow.
            .append(
                (" p, div" + (if (renderAsTiledGallery) ":not(.$galleryOnlyClass)" else "")
                        + ", li { line-height: 1.6em; font-size: 100%; }")
            )
            .append(" h1, h2, h3 { line-height: 1.6em; }")
            // Counteract pre-defined height/width styles, expect for:
            // 1. Story blocks, which set their own mobile-friendly size we shouldn't override.
            // 2. The tiled-gallery divs when rendering as tiled gallery, as those will be handled
            // with the .tiled-gallery rules below.
            .append(
                (" p, div:not(.wp-story-container.*)" + (if (renderAsTiledGallery) ":not(.tiled-gallery.*)" else "")
                        + ", dl, table { width: auto !important; height: auto !important; }")
            )
            // make sure long strings don't force the user to scroll horizontally
            .append(" body, p, div, a { word-wrap: break-word; }")
            // change horizontal line color
            .append(" .reader-full-post__story-content hr { background-color: transparent; ")
            .append("border-color: var(--color-neutral-50); }")
            // use a consistent top/bottom margin for paragraphs, with no top margin for the first one
            .append(" p { margin-top: ").append(resourceVars.marginMediumPx).append("px;")
            .append(" margin-bottom: ").append(resourceVars.marginMediumPx).append("px; }")
            .append(" p:first-child { margin-top: 0px; }")
            // add background color, fontsize and padding to pre blocks, and wrap the text
            // so the user can see full block.
            .append(" pre { word-wrap: break-word; white-space: pre-wrap; ")
            .append(" background-color: var(--color-neutral-20);")
            .append(" padding: ").append(resourceVars.marginMediumPx).append("px; ")
            .append(" line-height: 1.2em; font-size: 14px; }")
            // add a left border to blockquotes
            .append(" .reader-full-post__story-content blockquote { color: var(--color-neutral-0); ")
            .append(" padding-left: 32px; ")
            .append(" margin-left: 0px; ")
            .append(" border-left: 3px solid var(--color-neutral-50); }")
            // show links in the same color they are elsewhere in the app
            .append(" a { text-decoration: underline; color: var(--main-link-color); }")
            // make sure images aren't wider than the display, strictly enforced for images without size
            .append(" img { max-width: 100%; width: auto; height: auto; }")
            .append(" img.size-none { max-width: 100% !important; height: auto !important; }")
            // center large/medium images, provide a small bottom margin, and add a background color
            // so the user sees something while they're loading
            .append(" img.size-full, img.size-large, img.size-medium {")
            .append(" display: block; margin-left: auto; margin-right: auto;")
            .append(" background-color: var(--color-neutral-0);")
            .append(" margin-bottom: ").append(resourceVars.marginMediumPx).append("px; }")

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
                .append(" opacity:0;}")
        }

        // see http://codex.wordpress.org/CSS#WordPress_Generated_Classes
        sbHtml
            .append(" .wp-caption img { margin-top: 0px; margin-bottom: 0px; }")
            .append(" .wp-caption .wp-caption-text {")
            .append(" font-size: smaller; line-height: 1.2em; margin: 0px;")
            .append(" text-align: center;")
            .append(" padding: ").append(resourceVars.marginMediumPx).append("px; ")
            .append(" color: var(--color-neutral-0); }")
            // attribution for Discover posts
            .append(" div#discover { ")
            .append(" margin-top: ").append(resourceVars.marginMediumPx).append("px;")
            .append(" font-family: sans-serif;")
            .append(" }")
            // horizontally center iframes
            .append(" iframe { display: block; margin: 0 auto; }")
            // hide forms, form-related elements, legacy RSS sharing links and other ad-related content
            // http://bit.ly/2FUTvsP
            .append(" form, input, select, button textarea { display: none; }")
            .append(" div.feedflare { display: none; }")
            .append(" .sharedaddy, .jp-relatedposts, .mc4wp-form, .wpcnt, ")
            .append(" .OUTBRAIN, .adsbygoogle { display: none; }")
            .append(" figure { display: block; margin-inline-start: 0px; margin-inline-end: 0px; }")
            .append("</style>")

        // add a custom CSS class to (any) tiled gallery elements to make them easier selectable for various rules
        val classAmendRegexes: List<String> = mutableListOf(
            "(tiled-gallery) ([\\s\"\'])",
            "(gallery-row) ([\\s\"'])",
            "(gallery-group) ([\\s\"'])",
            "(tiled-gallery-item) ([\\s\"'])"
        )
        var contentCustomised = content

        // removes background-color property from original content
        contentCustomised =
            contentCustomised.replace("\\s*(background-color)\\s*:\\s*.+?\\s*;\\s*".toRegex(), "")

        classAmendRegexes.forEach { classToAmend ->
            contentCustomised = contentCustomised.replace(
                classToAmend.toRegex(),
                "$1 $galleryOnlyClass$2"
            )
        }

        jsToInject.forEach { jsUrl ->
            sbHtml.append("<script src=\"").append(jsUrl)
                .append("\" type=\"text/javascript\" async></script>")
        }

        sbHtml.append("</head><body class=\"reader-full-post reader-full-post__story-content\">")
            .append("<script type=\"text/javascript\" src=\"file:///android_asset/reader_text_events.js\"></script>")
            .append(contentCustomised)
            .append("</body></html>")

        return sbHtml.toString()
    }

    private fun appendMappedColors(sb: StringBuilder) {
        sb.append(" :root { ")
            .append("--color-text: ").append(readingPreferencesTheme.cssTextColor).append("; ")
            .append("--color-neutral-0: ").append(readingPreferencesTheme.cssTextMediumColor).append("; ")
            .append("--color-neutral-5: ").append(readingPreferencesTheme.cssTextExtraLightColor).append("; ")
            .append("--color-neutral-10: ").append(readingPreferencesTheme.cssTextDisabledColor).append("; ")
            .append("--color-neutral-20: ").append(readingPreferencesTheme.cssTextExtraLightColor).append("; ")
            .append("--color-neutral-50: ").append(readingPreferencesTheme.cssTextLightColor).append("; ")
            .append("--color-neutral-70: ").append(readingPreferencesTheme.cssTextColor).append("; ")
            .append("--main-link-color: ").append(readingPreferencesTheme.cssLinkColor).append("; ")
            .append("} ")
    }

    private fun getImageSize(
        imageTag: String,
        imageUrl: String
    ): ImageSizeMap.ImageSize? {
        getImageSizeFromAttachments(imageUrl)?.let { imageSize ->
            return imageSize
        } ?: run {
            return if (imageTag.contains("data-orig-size=")) {
                getImageOriginalSizeFromAttributes(imageTag)
            } else if (imageUrl.contains("?")) {
                getImageSizeFromQueryParams(imageUrl)
            } else if (imageTag.contains("width=")) {
                getImageSizeFromAttributes(imageTag)
            } else {
                null
            }
        }
    }

    private fun getImageSizeFromAttachments(imageUrl: String): ImageSizeMap.ImageSize? {
        if (attachmentSizes == null) {
            attachmentSizes = ImageSizeMap(readerPost.text, readerPost.attachmentsJson)
        }
        return attachmentSizes!!.getImageSize(imageUrl)
    }

    @Suppress("ReturnCount")
    private fun getImageSizeFromQueryParams(imageUrl: String): ImageSizeMap.ImageSize? {
        if (imageUrl.contains("w=")) {
            val uri = imageUrl.replace("&#038;", "&").toUri()
            return ImageSizeMap.ImageSize(
                StringUtils.stringToInt(uri.getQueryParameter("w")),
                StringUtils.stringToInt(uri.getQueryParameter("h"))
            )
        } else if (imageUrl.contains("resize=")) {
            val uri = imageUrl.replace("&#038;", "&").toUri()
            val param = uri.getQueryParameter("resize")
            if (param != null) {
                val sizes = param.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (sizes.size == 2) {
                    return ImageSizeMap.ImageSize(
                        StringUtils.stringToInt(sizes[0]),
                        StringUtils.stringToInt(sizes[1])
                    )
                }
            }
        }

        return null
    }

    private fun getImageOriginalSizeFromAttributes(imageTag: String): ImageSizeMap.ImageSize {
        return ImageSizeMap.ImageSize(
            ReaderHtmlUtils.getOriginalWidthAttrValue(imageTag),
            ReaderHtmlUtils.getOriginalHeightAttrValue(imageTag)
        )
    }

    private fun getImageSizeFromAttributes(imageTag: String): ImageSizeMap.ImageSize {
        return ImageSizeMap.ImageSize(
            ReaderHtmlUtils.getWidthAttrValue(imageTag),
            ReaderHtmlUtils.getHeightAttrValue(imageTag)
        )
    }

    private fun pxToDp(px: Int): Int {
        if (px == 0) {
            return 0
        }
        return DisplayUtils.pxToDp(getContext(), px)
    }

    private fun isRTL(content: String): Boolean {
        val bidi = Bidi(content, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
        return bidi.isRightToLeft || bidi.isMixed
    }

    private val contentTextProperties: String
        get() = ("font-family: " + readingPreferences.fontFamily.value + "; "
                + "font-weight: 400; "
                + "font-size: " + readingPreferences.fontSize.value + "px; ")

    private fun setWebViewMessageHandler(webView: WebView) {
        val tag = webView.getTag(JS_OBJECT_ADDED_TAG.hashCode())
        if (tag != null && tag as Boolean) {
            return  // Exit if the object has already been added
        }

        val allowedOrigins: MutableSet<String> = HashSet()
        allowedOrigins.add("*")

        createJsObject(
            webView, JAVASCRIPT_MESSAGE_HANDLER, allowedOrigins
        ) { message: String? ->
            if (postMessageListener == null) {
                return@createJsObject
            }
            when (message) {
                ReaderPostMessageListener.MSG_ARTICLE_TEXT_COPIED ->
                    postMessageListener!!.onArticleTextCopied()
                ReaderPostMessageListener.MSG_ARTICLE_TEXT_HIGHLIGHTED ->
                    postMessageListener!!.onArticleTextHighlighted()
            }
        }

        // Set the tag that the JS object has been added, so we can check before adding it again
        webView.setTag(JS_OBJECT_ADDED_TAG.hashCode(), true)
    }

    fun setPostMessageListener(listener: ReaderPostMessageListener?) {
        postMessageListener = listener
    }

    interface ReaderPostMessageListener {
        fun onArticleTextCopied()
        fun onArticleTextHighlighted()

        companion object {
            const val MSG_ARTICLE_TEXT_COPIED: String = "articleTextCopied"
            const val MSG_ARTICLE_TEXT_HIGHLIGHTED: String = "articleTextHighlighted"
        }
    }

    companion object {
        private const val JAVASCRIPT_MESSAGE_HANDLER = "wvHandler"
        private const val JS_OBJECT_ADDED_TAG = "jsObjectAdded"
        private const val RANDOM_BOUND = 1000
        // determine whether a tiled-gallery exists in the content
        fun hasTiledGallery(text: String): Boolean {
            return Pattern.compile("tiled-gallery[\\s\"']").matcher(text).find()
        }
    }
}
