package org.wordpress.android.ui.accounts.login;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.wordpress.android.R;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.accounts.SignInActivity;

public class MagicLinkSignInActivity extends SignInActivity implements WPComMagicLinkFragment.OnMagicLinkFragmentInteraction, MagicLinkSignInFragment.OnMagicLinkRequestListener {
    private String mEmail = "";

    @Override
    protected void onResume() {
        super.onResume();

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            if (uri.getHost().contains("magic-login")) {
                getSignInFragment().setToken(uri.getQueryParameter("token"));

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                MagicLinkSignInFragment magicLinkSignInFragment = getSignInFragment();
                fragmentTransaction.replace(R.id.fragment_container, magicLinkSignInFragment);
                fragmentTransaction.addToBackStack("magic_link_9");
                fragmentTransaction.commit();
            } else {
                // handle error
            }
        }
    }

    @Override
    public MagicLinkSignInFragment getSignInFragment() {
        if (mSignInFragment != null && mSignInFragment instanceof MagicLinkSignInFragment) {
            return (MagicLinkSignInFragment) mSignInFragment;
        } else {
            return new MagicLinkSignInFragment();
        }
    }

    @Override
    public void onMagicLinkSent() {
        MagicLinkSentFragment magicLinkSentFragment = new MagicLinkSentFragment();
        slideInFragment(magicLinkSentFragment);
    }

    @Override
    public void onEnterPasswordRequested() {
        MagicLinkSignInFragment magicLinkSignInFragment = getSignInFragment();
        slideInFragment(magicLinkSignInFragment);
    }

    @Override
    public void onMagicLinkRequestSuccess(String email) {
        saveEmailToAccount(email);

        WPComMagicLinkFragment wpComMagicLinkFragment = WPComMagicLinkFragment.newInstance(email);
        slideInFragment(wpComMagicLinkFragment);
    }

    private void saveEmailToAccount(String email) {
        Account account = AccountHelper.getDefaultAccount();
        account.setUserName(email);
        account.save();
    }

    private void slideInFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.hs__slide_in_from_right, R.anim.hs__slide_out_to_left);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
