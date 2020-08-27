package org.wordpress.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.ui.RequestCodes.JETPACK_LOGIN;

/**
 * An activity to handle result of Jetpack connection
 * <p>
 * wordpress://jetpack-connection?reason={error}
 * <p>
 * Redirects users to the stats activity if the jetpack connection was succesful
 */
public class JetpackConnectionResultActivity extends LocaleAwareActivity {
    private static final String ALREADY_CONNECTED = "already-connected";
    private static final String REASON_PARAM = "reason";
    private static final String SOURCE_PARAM = "source";

    private JetpackConnectionSource mSource;
    private String mReason;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.stats_loading_activity);


        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setTitle(R.string.stats);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();
        String host = "";
        if (uri != null) {
            host = uri.getHost();
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            AnalyticsUtils.trackWithDeepLinkData(Stat.DEEP_LINKED, action, host, uri);
        }

        // check if this intent is started via custom scheme link
        if (uri != null) {
            // Non-empty reason does not mean we're not connected to Jetpack
            // - one of the errors is "already-connected"
            mReason = uri.getQueryParameter(REASON_PARAM);
            mSource = JetpackConnectionSource.fromString(uri.getQueryParameter(SOURCE_PARAM));
            if (mAccountStore.hasAccessToken()) {
                // if user is signed in wpcom show the stats or notifications right away
                trackResult();
                finishAndGoBackToSource();
            } else {
                // An edgecase when the user is logged out in the app but logged in in webview
                Intent loginIntent = new Intent(this, LoginActivity.class);
                LoginMode.JETPACK_STATS.putInto(loginIntent);
                this.startActivityForResult(loginIntent, JETPACK_LOGIN);
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.JETPACK_LOGIN) {
            if (resultCode == RESULT_OK) {
                trackResult();
            } else {
                finishAndGoBackToSource();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAndGoBackToSource();
    }

    private void trackResult() {
        if (!TextUtils.isEmpty(mReason)) {
            if (mReason.equals(ALREADY_CONNECTED)) {
                AppLog.d(AppLog.T.API, "Already connected to Jetpack.");
                ToastUtils.showToast(this, getString(R.string.jetpack_already_connected_toast));
            } else {
                AppLog.e(AppLog.T.API, "Could not connect to Jetpack, reason: " + mReason);
                JetpackConnectionUtils.trackFailureWithSource(mSource, mReason);
                ToastUtils.showToast(this, getString(R.string.jetpack_connection_failed_with_reason, mReason));
            }
        } else {
            JetpackConnectionUtils.trackWithSource(Stat.SIGNED_INTO_JETPACK, mSource);
        }
    }

    private void finishAndGoBackToSource() {
        if (mSource == JetpackConnectionSource.STATS) {
            SiteModel site = (SiteModel) getIntent().getSerializableExtra(SITE);
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
            ActivityLauncher.viewBlogStatsAfterJetpackSetup(this, site);
        }
        finish();
    }
}
