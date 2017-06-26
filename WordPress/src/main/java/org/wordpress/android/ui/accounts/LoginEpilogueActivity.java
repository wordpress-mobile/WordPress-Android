package org.wordpress.android.ui.accounts;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.ActivityLauncher;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class LoginEpilogueActivity extends AppCompatActivity implements LoginEpilogueFragment.LoginEpilogueListener {
    public static final String EXTRA_SHOW_AND_RETURN = "EXTRA_SHOW_AND_RETURN";

    protected @Inject AccountStore mAccountStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_epilogue_activity);

        if (savedInstanceState == null) {
            boolean showAndReturn = getIntent().getBooleanExtra(EXTRA_SHOW_AND_RETURN, false);

            addPostLoginFragment(showAndReturn);
        }
    }

    protected void addPostLoginFragment(boolean showAndReturn) {
        LoginEpilogueFragment loginEpilogueFragment = LoginEpilogueFragment.newInstance(showAndReturn);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginEpilogueFragment, LoginEpilogueFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onConnectAnotherSite() {
        if (mAccountStore.hasAccessToken()) {
            ActivityLauncher.addSelfHostedSiteForResult(this);
        } else {
            ActivityLauncher.showSignInForResult(this);
        }

        finish();
    }

    @Override
    public void onContinue() {
        setResult(RESULT_OK);
        finish();
    }
}
