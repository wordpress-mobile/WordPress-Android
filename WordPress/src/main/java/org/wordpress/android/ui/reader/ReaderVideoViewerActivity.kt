package org.wordpress.android.ui.reader

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.ui.WPWebView
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent
import org.wordpress.android.util.helpers.WebChromeClientWithVideoPoster
import javax.inject.Inject

private const val PROGRESS_COMPLETE = 100

/**
 * Full screen video player for the reader
 */
@AndroidEntryPoint
class ReaderVideoViewerActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var userAgent: UserAgent

    private lateinit var webView: WebView
    private var loadProgress by mutableIntStateOf(PROGRESS_COMPLETE)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WPWebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.userAgentString = userAgent.webViewUserAgent
            webChromeClient = object : WebChromeClientWithVideoPoster(
                this,
                org.wordpress.android.editor.R.drawable.media_movieclip
            ) {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    loadProgress = newProgress
                }
            }
        }
        if (savedInstanceState == null) {
            intent.getStringExtra(ReaderConstants.ARG_VIDEO_URL)?.let { webView.loadUrl(it) }
        } else {
            webView.restoreState(savedInstanceState)
        }

        setContent {
            AppThemeM3 {
                Box(modifier = Modifier.fillMaxSize().background(colorResource(R.color.neutral_70))) {
                    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                    if (loadProgress < PROGRESS_COMPLETE) {
                        LinearProgressIndicator(
                            progress = { loadProgress / PROGRESS_COMPLETE.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // the video must be paused here or else the audio will continue to play
        // even though the activity has been destroyed
        webView.onPause()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }
}
