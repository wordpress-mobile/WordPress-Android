package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 *
 */

public class ReaderVideoViewerActivity extends AppCompatActivity {

    private String mVideoUrl;
    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_video_player);
        mWebView = (WebView) findViewById(R.id.web_view);

        if (savedInstanceState == null) {
            mVideoUrl = getIntent().getStringExtra(ReaderConstants.ARG_VIDEO_URL);
        } else {
            mVideoUrl = savedInstanceState.getString(ReaderConstants.ARG_VIDEO_URL);
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUserAgentString(WordPress.getUserAgent());

        mWebView.setWebViewClient(new ReaderVideoWebViewClient());

        mWebView.loadUrl(mVideoUrl);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ReaderConstants.ARG_VIDEO_URL, mVideoUrl);
        super.onSaveInstanceState(outState);
    }

    private class ReaderVideoWebViewClient extends WebViewClient {
        ReaderVideoWebViewClient() {
            // noop
        }

        @Override
        public void onPageFinished(WebView view, String url) {

        }
    }
}
