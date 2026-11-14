package org.wordpress.android.ui.reader.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.WPWebView
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.helpers.WebChromeClientWithVideoPoster
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import org.wordpress.android.editor.R as EditorR

/**
 * WebView used by ReaderPostDetailFragment, which handles fullscreen video and detects url and image clicks
 */
class ReaderWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : WPWebView(context, attrs, defStyle) {
    interface ReaderWebViewUrlClickListener {
        fun onUrlClick(url: String): Boolean
        fun onPageJumpClick(pageJump: String): Boolean
        fun onImageUrlClick(imageUrl: String, view: View, x: Int, y: Int): Boolean
        fun onFileDownloadClick(fileUrl: String): Boolean
    }

    interface ReaderCustomViewListener {
        fun onCustomViewShown()
        fun onCustomViewHidden()
        fun onRequestCustomView(): ViewGroup?
        fun onRequestContentView(): ViewGroup?
    }

    fun interface ReaderWebViewPageFinishedListener {
        fun onPageFinished(view: WebView, url: String?)
    }

    @Inject
    lateinit var userAgent: UserAgent

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    @Named(OkHttpClientQualifiers.REGULAR)
    lateinit var okHttpClient: OkHttpClient

    // the client that fetches a private post's images
    private val imageClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    private var readerChromeClient: ReaderWebChromeClient? = null
    private var token: String? = null

    var urlClickListener: ReaderWebViewUrlClickListener? = null
    var pageFinishedListener: ReaderWebViewPageFinishedListener? = null
    var customViewListener: ReaderCustomViewListener? = null

    /**
     * Private posts on https blogs have their images fetched with the account's token
     */
    var isPrivatePost = false
    var blogSchemeIsHttps = false

    var isDestroyed = false
        private set

    val isCustomViewShowing: Boolean
        get() = readerChromeClient?.isCustomViewShowing == true

    init {
        (context.applicationContext as WordPress).component().inject(this)
        setBackgroundColor(Color.TRANSPARENT)
        if (!isInEditMode) {
            token = accountStore.accessToken
            readerChromeClient = ReaderWebChromeClient(this).also { webChromeClient = it }
            webViewClient = ReaderWebViewClient(this)
            settings.userAgentString = userAgent.webViewUserAgent
            // third-party cookies are disabled by default, and authenticated images need them
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            setDownloadListener { url, _, _, _, _ -> urlClickListener?.onFileDownloadClick(url) }
        }
    }

    override fun destroy() {
        isDestroyed = true
        super.destroy()
    }

    fun clearContent() {
        loadUrl(BLANK_URL)
    }

    fun hideCustomView() {
        if (isCustomViewShowing) readerChromeClient?.onHideCustomView()
    }

    /**
     * Returns the href of the anchor wrapping the tapped image, if there is one
     */
    private fun tappedImageAnchorUrl(hitTestResult: HitTestResult): String? {
        if (hitTestResult.type != HitTestResult.SRC_IMAGE_ANCHOR_TYPE) return null
        val message = Handler(Looper.getMainLooper()).obtainMessage()
        requestFocusNodeHref(message)
        return message.data.getString("url")
    }

    private fun isVideoPressPreview(url: String) = url.startsWith("https://videos.files.wordpress.com")

    /*
     * detect when a link is tapped
     */
    @Suppress("ReturnCount")
    @SuppressLint("ClickableViewAccessibility") // works as is
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val listener = urlClickListener
        if (event.action != MotionEvent.ACTION_UP || listener == null) return super.onTouchEvent(event)

        val hitTestResult = hitTestResult
        val url = hitTestResult.extra
        if (url == null || !isValidClickedUrl(url)) {
            val pageJump = UrlUtils.getPageJumpOrNull(url)
            return if (pageJump != null) listener.onPageJumpClick(pageJump) else super.onTouchEvent(event)
        }

        // trust the hit type for images, so formats isImageUrl doesn't cover (e.g. .bmp, .webp) count as images
        val hitType = hitTestResult.type
        val isImage = hitType == HitTestResult.IMAGE_TYPE ||
            hitType == HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
            UrlUtils.isImageUrl(url)
        if (!isImage) return listener.onUrlClick(url)

        // The WebView handles the tap if the image is inside an anchor we want to respect - an Instagram embed or
        // a Story block (see https://pacheco.dev/posts/android/webview-image-anchor/) - or one that's a fragment
        // link (e.g. a footnote back-reference), so the JS interceptor can handle it. Otherwise it's an image
        // click, e.g. a gallery image linked to its media file.
        if (isVideoPressPreview(url)) return super.onTouchEvent(event)
        val anchorUrl = tappedImageAnchorUrl(hitTestResult)
        val deferToWebView = anchorUrl != null &&
            (anchorUrl.contains("ig_embed") || anchorUrl.contains("wp-story") || anchorUrl.contains("#"))
        if (deferToWebView) return super.onTouchEvent(event)
        return listener.onImageUrlClick(url, this, event.x.toInt(), event.y.toInt())
    }

    private class ReaderWebViewClient(private val readerWebView: ReaderWebView) : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            readerWebView.pageFinishedListener?.onPageFinished(view, url)
        }

        /*
         * Diagnostics only - the post body renders from local data, so a failed subresource (a blocked tracker, a
         * dead image, an unreachable embed script) is not worth surfacing to the user. It is worth recording: a
         * stalled or failing subresource is what made CMM-2254 so hard to pin down, and nothing in the app logged it.
         */
        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            log(request, "reader webView > error ${error.errorCode} (${error.description}) loading ${request.url}")
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            log(request, "reader webView > http ${errorResponse.statusCode} loading ${request.url}")
        }

        private fun log(request: WebResourceRequest, message: String) {
            if (request.isForMainFrame) {
                AppLog.w(AppLog.T.READER, message)
            } else {
                AppLog.d(AppLog.T.READER, message)
            }
        }

        // Fire the url click listener only once the WebView is visible: some posts with iframes try to open urls
        // without being clicked before the page has loaded
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            return view.visibility == View.VISIBLE &&
                isValidClickedUrl(url) &&
                readerWebView.urlClickListener?.onUrlClick(url) == true
        }

        /**
         * Adds the WP.com authorization header to requests for a private post's images
         */
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()
            val token = readerWebView.token
            val imageUrl = if (readerWebView.isPrivatePost && readerWebView.blogSchemeIsHttps &&
                UrlUtils.isImageUrl(url)
            ) {
                try {
                    URL(UrlUtils.makeHttps(url))
                } catch (e: MalformedURLException) {
                    AppLog.e(AppLog.T.READER, e)
                    null
                }
            } else {
                null
            }
            if (imageUrl != null && WPUrlUtils.safeToAddWordPressComAuthToken(imageUrl) && !token.isNullOrEmpty()) {
                try {
                    val request = Request.Builder()
                        .url(imageUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("User-Agent", readerWebView.userAgent.webViewUserAgent)
                        .addHeader("Connection", "Keep-Alive")
                        .build()
                    val response = readerWebView.imageClient.newCall(request).execute()
                    return WebResourceResponse(
                        response.header("Content-Type"),
                        response.header("Content-Encoding"),
                        response.body.byteStream()
                    )
                } catch (e: IOException) {
                    AppLog.e(AppLog.T.READER, e)
                }
            }
            // the default, which lets the WebView load it
            return null
        }
    }

    private class ReaderWebChromeClient(
        private val readerWebView: ReaderWebView
    ) : WebChromeClientWithVideoPoster(readerWebView, EditorR.drawable.media_movieclip) {
        private var customView: View? = null
        private var customViewCallback: CustomViewCallback? = null

        val isCustomViewShowing: Boolean
            get() = customView != null

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            AppLog.i(AppLog.T.READER, "onShowCustomView")
            if (customView != null) {
                AppLog.w(AppLog.T.READER, "customView already showing")
                onHideCustomView()
                return
            }

            val listener = readerWebView.customViewListener
            // hide the post detail content and show the fullscreen view
            listener?.onRequestContentView()?.visibility = View.INVISIBLE
            listener?.onRequestCustomView()?.let {
                it.addView(view)
                it.visibility = View.VISIBLE
            }
            listener?.onCustomViewShown()

            customView = view
            customViewCallback = callback
        }

        override fun onHideCustomView() {
            AppLog.i(AppLog.T.READER, "onHideCustomView")
            val view = customView
            if (view == null) {
                AppLog.w(AppLog.T.READER, "customView does not exist")
                return
            }

            val listener = readerWebView.customViewListener
            listener?.onRequestCustomView()?.let {
                it.removeView(view)
                it.visibility = View.GONE
            }
            listener?.onRequestContentView()?.visibility = View.VISIBLE
            customViewCallback?.onCustomViewHidden()
            listener?.onCustomViewHidden()

            customView = null
            customViewCallback = null
        }
    }

    companion object {
        /**
         * The blank page [clearContent] loads to drop the previous post - the only page this WebView ever loads
         * that isn't a rendered article
         */
        const val BLANK_URL = "about:blank"

        private const val TIMEOUT_MS = 30_000L

        // only http(s) urls, so file: and data: clicks are ignored
        private fun isValidClickedUrl(url: String) = url.startsWith("http") || url.startsWith("wordpress:")
    }
}
