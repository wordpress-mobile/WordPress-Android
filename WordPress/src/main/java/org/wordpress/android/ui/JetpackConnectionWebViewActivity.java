package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebViewClient;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.List;

/**
 * Activity that opens the Jetpack login flow and returns to StatsActivity when finished.
 * Use one of the static factory methods to start the flow.
 */
public class JetpackConnectionWebViewActivity extends WPWebViewActivity {

    public static final String JETPACK_CONNECTION_DEEPLINK = "wordpress://jetpack-connection";

    private JetpackConnectionWebViewClient mWebViewClient;

    public static void openJetpackConnectionFlow(Context context, String url, SiteModel site) {
        openJetpackConnectionFlow(context, url, site, true);
    }

    public static void openUnauthorizedJetpackConnectionFlow(Context context, String url, SiteModel site) {
        openJetpackConnectionFlow(context, url, site, false);
    }

    private static void openJetpackConnectionFlow(Context context, String url, SiteModel site, boolean authorized) {
        if (!checkContextAndUrl(context, url)) {
            return;
        }

        Intent intent = new Intent(context, JetpackConnectionWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        if (authorized) {
            intent.putExtra(WPWebViewActivity.USE_GLOBAL_WPCOM_USER, true);
            intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPCOM_LOGIN_URL);
        }
        if (site != null) {
            intent.putExtra(WordPress.SITE, site);
        }
        context.startActivity(intent);
    }

    @Override
    protected WebViewClient createWebViewClient(List<String> allowedURL) {
        mWebViewClient = new JetpackConnectionWebViewClient(this, mAccountStore, (SiteModel) getIntent().getSerializableExtra(WordPress.SITE));
        return mWebViewClient;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mWebViewClient != null) {
            this.mWebViewClient.activityResult(this, requestCode);
        }
    }

    @Override
    protected void cancel() {
        if (mWebViewClient != null) {
            this.mWebViewClient.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mWebViewClient != null) {
            this.mWebViewClient.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mWebViewClient != null) {
            this.mWebViewClient.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
