package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.WebViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Display release notes in a WebView, with share and bug report buttons.
 */
public class ReleaseNotesActivity extends WebViewActivity {
    private static final String KEY_TARGET_URL = "KEY_TARGET_URL";
    private static final String KEY_ORIGIN = "KEY_ORIGIN";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ZendeskHelper mZendeskHelper;

    public static Intent createIntent(Context context, @NonNull String targetUrl, @Nullable Origin origin,
                                      @Nullable SiteModel selectedSite) {
        Intent intent = new Intent(context, ReleaseNotesActivity.class);
        intent.putExtra(ReleaseNotesActivity.KEY_TARGET_URL, targetUrl);
        intent.putExtra(ReleaseNotesActivity.KEY_ORIGIN, origin != null ? origin : Origin.RELEASE_NOTES);
        if (selectedSite != null) {
            intent.putExtra(WordPress.SITE, selectedSite);
        }
        return intent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void configureWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        super.configureWebView();
    }

    @Override
    protected void loadContent() {
        loadUrl(getIntent().getStringExtra(KEY_TARGET_URL));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mWebView.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        ReleaseNotesActivity.this.setTitle(view.getTitle());
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview_release_notes, menu);
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
                HelpActivity.Origin origin = (HelpActivity.Origin) getIntent().getSerializableExtra(KEY_ORIGIN);
                SiteModel selectedSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
                mZendeskHelper.createNewTicket(ReleaseNotesActivity.this, origin, selectedSite);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
