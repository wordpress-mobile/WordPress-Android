package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.JetpackConnectionWebViewActivity;

import javax.inject.Inject;

import static org.wordpress.android.WordPress.SITE;
import static org.wordpress.android.ui.JetpackConnectionWebViewActivity.Source.STATS;

/**
 * An activity that shows when user tries to open Stats without Jetpack connected.
 * It offers a link to the Jetpack connection flow.
 */

public class StatsConnectJetpackActivity extends AppCompatActivity {
    @Inject AccountStore mAccountStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.stats_jetpack_connection_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setTitle(R.string.stats);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.stats);

        Button setupButton = findViewById(R.id.jetpack_setup);
        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startJetpackConnectionFlow(
                        (SiteModel) StatsConnectJetpackActivity.this.getIntent().getSerializableExtra(SITE));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startJetpackConnectionFlow(SiteModel siteModel) {
        if (mAccountStore.hasAccessToken()) {
            JetpackConnectionWebViewActivity
                    .openJetpackConnectionFlow(StatsConnectJetpackActivity.this, STATS, siteModel);
        } else {
            JetpackConnectionWebViewActivity
                    .openUnauthorizedJetpackConnectionFlow(StatsConnectJetpackActivity.this, STATS, siteModel);
        }
        finish();
        if (!siteModel.isJetpackInstalled()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SELECTED_INSTALL_JETPACK);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SELECTED_CONNECT_JETPACK);
        }
    }
}
