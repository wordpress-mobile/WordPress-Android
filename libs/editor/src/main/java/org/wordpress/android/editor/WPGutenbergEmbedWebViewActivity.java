package org.wordpress.android.editor;

import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergEmbedWebViewActivity;

public class WPGutenbergEmbedWebViewActivity extends GutenbergEmbedWebViewActivity {
    @Override
    protected void load() {
        String content = getIntent().getExtras().getString(ARG_CONTENT);
        mWebView.loadData(content, "text/html", "UTF-8");
    }

    protected String getToolbarTitle() {
        return getIntent().getExtras().getString(ARG_TITLE);
    }
}
