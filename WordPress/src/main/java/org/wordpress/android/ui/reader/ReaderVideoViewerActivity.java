package org.wordpress.android.ui.reader;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 *
 */

public class ReaderVideoViewerActivity extends AppCompatActivity {

    private String mVideoUrl;
    private WebView mWebView;
    private ProgressBar mProgress;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_video_player);

        mWebView = (WebView) findViewById(R.id.web_view);
        mWebView.setBackgroundColor(Color.TRANSPARENT);

        mProgress = (ProgressBar) findViewById(R.id.progress);

        if (savedInstanceState == null) {
            mVideoUrl = getIntent().getStringExtra(ReaderConstants.ARG_VIDEO_URL);
        } else {
            mVideoUrl = savedInstanceState.getString(ReaderConstants.ARG_VIDEO_URL);
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUserAgentString(WordPress.getUserAgent());

        showProgress();
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                mProgress.setProgress(progress);
                if (progress == 100) {
                    hideProgress();
                }
            }
        });

        mWebView.loadUrl(mVideoUrl);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ReaderConstants.ARG_VIDEO_URL, mVideoUrl);
        super.onSaveInstanceState(outState);
    }

    private void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgress.setVisibility(View.GONE);
    }

}
