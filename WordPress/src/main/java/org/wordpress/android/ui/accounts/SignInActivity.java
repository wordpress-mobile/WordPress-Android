package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.google.android.gms.auth.api.credentials.Credential;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class SignInActivity extends FragmentActivity {
    public static final int SIGN_IN_REQUEST = 1;
    public static final int REQUEST_CODE = 5000;
    public static final int ADD_SELF_HOSTED_BLOG = 2;
    public static final int SHOW_CERT_DETAILS = 4;
    public static final int SMART_LOCK_SAVE = 5;
    public static final int SMART_LOCK_READ = 6;

    public static final String EXTRA_START_FRAGMENT = "start-fragment";
    public static final String EXTRA_JETPACK_SITE_AUTH = "EXTRA_JETPACK_SITE_AUTH";
    public static final String EXTRA_JETPACK_MESSAGE_AUTH = "EXTRA_JETPACK_MESSAGE_AUTH";
    public static final String EXTRA_IS_AUTH_ERROR = "EXTRA_IS_AUTH_ERROR";

    protected SignInFragment mSignInFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.welcome_activity);

        mSignInFragment = getSignInFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, mSignInFragment);
        fragmentTransaction.commit();

        actionMode(getIntent().getExtras());
        ActivityId.trackLastActivity(ActivityId.LOGIN);
    }

    protected void actionMode(Bundle extras) {
        int actionMode = SIGN_IN_REQUEST;
        if (extras != null) {
            actionMode = extras.getInt(EXTRA_START_FRAGMENT, -1);
            if (extras.containsKey(EXTRA_JETPACK_SITE_AUTH)) {
                Blog jetpackBlog = WordPress.getBlog(extras.getInt(EXTRA_JETPACK_SITE_AUTH));
                if (jetpackBlog != null) {
                    String customMessage = extras.getString(EXTRA_JETPACK_MESSAGE_AUTH, null);
                    mSignInFragment.setBlogAndCustomMessageForJetpackAuth(jetpackBlog, customMessage);
                }
            } else if (extras.containsKey(EXTRA_IS_AUTH_ERROR)) {
                mSignInFragment.showAuthErrorMessage();
            }
        }
        switch (actionMode) {
            case ADD_SELF_HOSTED_BLOG:
                mSignInFragment.forceSelfHostedMode();
                break;
            default:
                break;
        }
//        mSignInFragment.smartLockAutoFill();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHOW_CERT_DETAILS) {
            mSignInFragment.askForSslTrust();
        } else if (requestCode == SMART_LOCK_SAVE) {
            if (resultCode == RESULT_OK) {
                AnalyticsTracker.track(Stat.LOGIN_AUTOFILL_CREDENTIALS_UPDATED);
                AppLog.d(T.NUX, "Credentials saved");
            } else {
                AppLog.d(T.NUX, "Credentials save cancelled");
            }
        } else if (requestCode == SMART_LOCK_READ) {
            if (resultCode == RESULT_OK) {
                AppLog.d(T.NUX, "Credentials retrieved");
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mSignInFragment.onCredentialRetrieved(credential);
            } else {
                AppLog.e(T.NUX, "Credential read failed");
            }
        } else if (resultCode == RESULT_OK && data != null) {
            String username = data.getStringExtra("username");
            String password = data.getStringExtra("password");
            if (username != null) {
                mSignInFragment.signInDotComUser(username, password);
            }
        }
    }

    public SignInFragment getSignInFragment() {
        if (mSignInFragment == null) {
            return new SignInFragment();
        } else {
            return mSignInFragment;
        }
    }
}
