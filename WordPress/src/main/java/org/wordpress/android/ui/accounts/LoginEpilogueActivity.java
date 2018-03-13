package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.login.LoginEpilogueFragment;
import org.wordpress.android.ui.accounts.login.LoginEpilogueListener;
import org.wordpress.android.util.LocaleManager;

import java.util.ArrayList;

import javax.inject.Inject;

public class LoginEpilogueActivity extends AppCompatActivity implements LoginEpilogueListener {
    public static final String EXTRA_DO_LOGIN_UPDATE = "EXTRA_DO_LOGIN_UPDATE";
    public static final String EXTRA_SHOW_AND_RETURN = "EXTRA_SHOW_AND_RETURN";
    public static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";

    @Inject AccountStore mAccountStore;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_epilogue_activity);

        if (savedInstanceState == null) {
            boolean doLoginUpdate = getIntent().getBooleanExtra(EXTRA_DO_LOGIN_UPDATE, false);
            boolean showAndReturn = getIntent().getBooleanExtra(EXTRA_SHOW_AND_RETURN, false);
            ArrayList<Integer> oldSitesIds = getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS);

            addPostLoginFragment(doLoginUpdate, showAndReturn, oldSitesIds);
        }
    }

    protected void addPostLoginFragment(boolean doLoginUpdate, boolean showAndReturn, ArrayList<Integer> oldSitesIds) {
        LoginEpilogueFragment loginEpilogueFragment = LoginEpilogueFragment.newInstance(doLoginUpdate, showAndReturn,
                                                                                        oldSitesIds);
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
