package org.wordpress.android.ui.reader;

import android.content.Context;
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
import org.wordpress.android.util.LocaleManager;

/**
 * Full screen landscape video player for the reader
 */
public class ReaderVideoViewerActivity extends AppCompatActivity {
    private String mVideoUrl;
    private WebView mWebView;
    private ProgressBar mProgress;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_video_player);

        mWebView = (WebView) findViewById(R.id.web_view);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUserAgentString(WordPress.getUserAgent());

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    mProgress.setVisibility(View.GONE);
                } else {
                    mProgress.setProgress(progress);
                    if (mProgress.getVisibility() != View.VISIBLE) {
                        mProgress.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        if (savedInstanceState == null) {
            mVideoUrl = getIntent().getStringExtra(ReaderConstants.ARG_VIDEO_URL);
            mWebView.loadUrl(mVideoUrl);
        } else {
            mVideoUrl = savedInstanceState.getString(ReaderConstants.ARG_VIDEO_URL);
            mWebView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onDestroy() {
        // the video must be paused here or else the audio will continue to play
        // even though the activity has been destroyed
        mWebView.onPause();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ReaderConstants.ARG_VIDEO_URL, mVideoUrl);
        mWebView.saveState(outState);
        super.onSaveInstanceState(outState);
    }
}
