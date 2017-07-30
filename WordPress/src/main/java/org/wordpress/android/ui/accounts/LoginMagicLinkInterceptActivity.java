package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;

/**
 * Temporary deeplink receiver for magiclinks. Routes to the proper activity based on the login feature flag. Will be
 *  removed after the feature flag gets removed.
 */
public class LoginMagicLinkInterceptActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppPrefs.isLoginWizardStyleActivated()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED);

            Intent intent = new Intent(this, WPMainActivity.class);
            intent.setAction(getIntent().getAction());
            intent.setData(getIntent().getData());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            // just hand over control to the SignInActivity and don't come back
            Intent intent = new Intent(getIntent());
            intent.setClass(this, SignInActivity.class);
            startActivity(intent);
        }

        finish();
    }
}

