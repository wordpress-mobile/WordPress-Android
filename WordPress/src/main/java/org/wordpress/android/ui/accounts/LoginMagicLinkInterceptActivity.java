package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.WordPress;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.ui.main.WPMainActivity;

import javax.inject.Inject;

/**
 * Deep link receiver for magic links. Starts {@link WPMainActivity} where flow is routed to login
 * or signup based on deep link scheme, host, and parameters.
 */
public class LoginMagicLinkInterceptActivity extends Activity {
    @Inject protected LoginAnalyticsListener mAnalyticsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((WordPress) getApplication()).component().inject(this);
        super.onCreate(savedInstanceState);

        mAnalyticsListener.trackLoginMagicLinkOpened();

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.setAction(getIntent().getAction());
        intent.setData(getIntent().getData());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
