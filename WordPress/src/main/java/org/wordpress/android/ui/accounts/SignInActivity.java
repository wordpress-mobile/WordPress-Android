package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityId;

public class SignInActivity extends Activity {
    public static final int SIGN_IN_REQUEST = 1;
    public static final int REQUEST_CODE = 5000;
    public static final int ADD_SELF_HOSTED_BLOG = 2;
    public static final int CREATE_ACCOUNT_REQUEST = 3;
    public static final int SHOW_CERT_DETAILS = 4;
    public static String START_FRAGMENT_KEY = "start-fragment";
    public static final String ARG_JETPACK_SITE_AUTH = "ARG_JETPACK_SITE_AUTH";
    public static final String ARG_IS_AUTH_ERROR = "ARG_IS_AUTH_ERROR";

    private SignInFragment mSignInFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);
        FragmentManager fragmentManager = getFragmentManager();
        mSignInFragment = (SignInFragment) fragmentManager.findFragmentById(R.id.sign_in_fragment);
        actionMode(getIntent().getExtras());

        ActivityId.trackLastActivity(ActivityId.LOGIN);
    }

    private void actionMode(Bundle extras) {
        int actionMode = SIGN_IN_REQUEST;
        if (extras != null) {
            actionMode = extras.getInt(START_FRAGMENT_KEY, -1);

            if (extras.containsKey(ARG_JETPACK_SITE_AUTH)) {
                Blog jetpackBlog = WordPress.getBlog(extras.getInt(ARG_JETPACK_SITE_AUTH));
                if (jetpackBlog != null) {
                    mSignInFragment.setBlog(jetpackBlog);
                }
            } else if (extras.containsKey(ARG_IS_AUTH_ERROR)) {
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


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHOW_CERT_DETAILS) {
            mSignInFragment.askForSslTrust();
        } else if (resultCode == RESULT_OK && data != null) {
            String username = data.getStringExtra("username");
            String password = data.getStringExtra("password");
            if (username != null) {
                mSignInFragment.signInDotComUser(username, password);
            }
        }
    }
}
