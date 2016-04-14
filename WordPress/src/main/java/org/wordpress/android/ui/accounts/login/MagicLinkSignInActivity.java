package org.wordpress.android.ui.accounts.login;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.util.AnalyticsUtils;

public class MagicLinkSignInActivity extends SignInActivity implements WPComMagicLinkFragment.OnMagicLinkFragmentInteraction, MagicLinkSignInFragment.OnMagicLinkRequestInteraction, MagicLinkSentFragment.OnMagicLinkSentInteraction {
    private ProgressDialog mProgressDialog;

    @Override
    protected void onResume() {
        super.onResume();

        if (hasMagicLinkLoginIntent()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED);
            attemptLoginWithToken(getIntent().getData());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelProgressDialog();
    }

    @Override
    public MagicLinkSignInFragment getSignInFragment() {
        if (mSignInFragment == null) {
            return new MagicLinkSignInFragment();
        } else {
            return (MagicLinkSignInFragment) mSignInFragment;
        }
    }

    @Override
    public void onMagicLinkSent() {
        MagicLinkSentFragment magicLinkSentFragment = new MagicLinkSentFragment();
        slideInFragment(magicLinkSentFragment);
    }

    @Override
    public void onEnterPasswordRequested() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED);
        getSignInFragment().setShouldShowPassword(true);

        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStackImmediate();
        }

        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onMagicLinkRequestSuccess(String email) {
        saveEmailToAccount(email);

        WPComMagicLinkFragment wpComMagicLinkFragment = WPComMagicLinkFragment.newInstance(email);
        slideInFragment(wpComMagicLinkFragment);
    }

    private void cancelProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    private boolean hasMagicLinkLoginIntent() {
        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        return Intent.ACTION_VIEW.equals(action) && uri != null && uri.getHost().contains("magic-login");
    }

    private void attemptLoginWithToken(Uri uri) {
        getSignInFragment().setToken(uri.getQueryParameter("token"));
        MagicLinkSignInFragment magicLinkSignInFragment = getSignInFragment();
        slideInFragment(magicLinkSignInFragment, false);

        mProgressDialog = ProgressDialog.show(this, "", "Logging in", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                getSignInFragment().setToken("");
            }
        });
    }

    private void saveEmailToAccount(String email) {
        Account account = AccountHelper.getDefaultAccount();
        account.setUserName(email);
        account.save();
    }

    private void slideInFragment(Fragment fragment) {
        slideInFragment(fragment, true);
    }

    private void slideInFragment(Fragment fragment, boolean shouldAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.hs__slide_in_from_right, R.anim.hs__slide_out_to_left, R.anim.hs__slide_in_from_left, R.anim.hs__slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }
}
