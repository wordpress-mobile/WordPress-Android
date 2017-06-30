package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.WebViewActivity;
import org.wordpress.android.util.HelpshiftHelper;

import javax.inject.Inject;

/**
 * Display release notes for editor.
 */
public class EditorReleaseNotesActivity extends WebViewActivity {
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void configureWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        super.configureWebView();
    }

    @Override
    protected void loadContent() {
        loadUrl("https://make.wordpress.org/mobile/whats-new-in-beta-android-editor/");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mWebView.setWebViewClient(
            new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    EditorReleaseNotesActivity.this.setTitle(view.getTitle());
                }
            }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview_editor_release_notes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_share:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                share.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                startActivity(Intent.createChooser(share, getText(R.string.share_link)));
                return true;
            case R.id.menu_bug:
                HelpshiftHelper.Tag origin = HelpshiftHelper.Tag.ORIGIN_UNKNOWN;
                HelpshiftHelper.getInstance().showConversation(EditorReleaseNotesActivity.this, mSiteStore, origin, mAccountStore.getAccount().getUserName());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
