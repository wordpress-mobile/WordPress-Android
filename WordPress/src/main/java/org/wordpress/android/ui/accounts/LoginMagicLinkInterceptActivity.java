package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.wordpress.android.WordPress;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.ui.JetpackConnectionSource;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.main.WPMainActivity;

import javax.inject.Inject;

/**
 * Deep link receiver for magic links. Starts {@link WPMainActivity} where flow is routed to login
 * or signup based on deep link scheme, host, and parameters.
 */
public class LoginMagicLinkInterceptActivity extends LocaleAwareActivity {
    private static final String PARAMETER_FLOW = "flow";
    private static final String PARAMETER_FLOW_JETPACK = "jetpack";
    private static final String PARAMETER_SOURCE = "source";

    private String mAction;
    private Uri mUri;

    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        mAction = getIntent().getAction();
        mUri = getIntent().getData();

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.setAction(mAction);
        intent.setData(mUri);

        if (hasMagicLinkLoginIntent()) {
            intent.putExtra(WPMainActivity.ARG_IS_MAGIC_LINK_LOGIN, true);

            if (hasMagicLinkSignupIntent()) {
                mLoginAnalyticsListener.trackSignupMagicLinkOpened();
                intent.putExtra(WPMainActivity.ARG_IS_MAGIC_LINK_SIGNUP, true);
            } else {
                mLoginAnalyticsListener.trackLoginMagicLinkOpened();
                intent.putExtra(WPMainActivity.ARG_IS_MAGIC_LINK_SIGNUP, false);
            }
        }

        if (isJetpackConnectFlow()) {
            intent.putExtra(WPMainActivity.ARG_JETPACK_CONNECT_SOURCE, getJetpackConnectSource());
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean hasMagicLinkLoginIntent() {
        String host = (mUri != null && mUri.getHost() != null) ? mUri.getHost() : "";
        return Intent.ACTION_VIEW.equals(mAction) && host.contains(LoginActivity.MAGIC_LOGIN);
    }

    private boolean hasMagicLinkSignupIntent() {
        if (mUri != null) {
            String parameter = SignupEpilogueActivity.MAGIC_SIGNUP_PARAMETER;
            String value = (mUri.getQueryParameterNames() != null && mUri.getQueryParameter(parameter) != null)
                    ? mUri.getQueryParameter(parameter) : "";
            return Intent.ACTION_VIEW.equals(mAction)
                   && value.equalsIgnoreCase(SignupEpilogueActivity.MAGIC_SIGNUP_VALUE);
        } else {
            return false;
        }
    }

    private boolean isJetpackConnectFlow() {
        if (mUri != null) {
            String value = (mUri.getQueryParameterNames() != null && mUri.getQueryParameter(PARAMETER_FLOW) != null)
                    ? mUri.getQueryParameter(PARAMETER_FLOW) : "";
            return Intent.ACTION_VIEW.equals(mAction) && value.equalsIgnoreCase(PARAMETER_FLOW_JETPACK);
        } else {
            return false;
        }
    }

    private JetpackConnectionSource getJetpackConnectSource() {
        String value = (mUri != null && mUri.getQueryParameterNames() != null
                        && mUri.getQueryParameter(PARAMETER_SOURCE) != null)
                ? mUri.getQueryParameter(PARAMETER_SOURCE) : "";

        return JetpackConnectionSource.fromString(value);
    }
}
