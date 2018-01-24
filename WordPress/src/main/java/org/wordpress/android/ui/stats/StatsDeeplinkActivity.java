package org.wordpress.android.ui.stats;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

/**
 * An activity to handle Jetpack deeplink
 * <p>
 * wordpress://jetpack-connection?reason={error}
 * <p>
 * Redirects users to the stats activity if the jetpack connection was succesful
 */
public class StatsDeeplinkActivity extends AppCompatActivity {
    private String reason;

    @Inject
    AccountStore mAccountStore;
    @Inject
    SiteStore mSiteStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        AnalyticsUtils.trackWithDeepLinkData(AnalyticsTracker.Stat.DEEP_LINKED, action, uri);

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {

            reason = uri.getQueryParameter("reason");

            // if user is signed in wpcom show the post right away - otherwise show welcome activity
            // and then show the post once the user has signed in
            if (mAccountStore.hasAccessToken()) {
                showStats();
                finish();
            } else {
                ActivityLauncher.loginForDeeplink(this);
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // show the post if user is returning from successful login
        if (requestCode == RequestCodes.DO_LOGIN && resultCode == RESULT_OK) {
            showStats();
        }

        finish();
    }

    private void showStats() {
        if (!TextUtils.isEmpty(reason)) {
            ToastUtils.showToast(this, reason);
        } else {
            SiteModel site = mSiteStore.getSiteByLocalId(AppPrefs.getSelectedSite());
            ActivityLauncher.viewBlogStats(this, site);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
