package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.main.WPMainActivity;

/**
 * Deep link receiver for magic links.  Starts {@link WPMainActivity} where flow is routed to login
 * or signup based on deep link scheme, host, and parameters.
 */
public class LoginMagicLinkInterceptActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED);

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.setAction(getIntent().getAction());
        intent.setData(getIntent().getData());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
