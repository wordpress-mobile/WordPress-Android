package org.wordpress.android.ui.accounts.login;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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

                FragmentManager fragmentManager = getFragmentManager();
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
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MagicLinkSentFragment magicLinkSentFragment = new MagicLinkSentFragment();
        fragmentTransaction.replace(R.id.fragment_container, magicLinkSentFragment);
        fragmentTransaction.addToBackStack("magic_link_2");
        fragmentTransaction.commit();
    }

    @Override
    public void onEnterPasswordRequested() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MagicLinkSignInFragment magicLinkSignInFragment = getSignInFragment();
        fragmentTransaction.replace(R.id.fragment_container, magicLinkSignInFragment);
        fragmentTransaction.addToBackStack("magic_link_3");
        fragmentTransaction.commit();
    }

    @Override
    public void onMagicLinkRequestSuccess(String email) {
        Account account = AccountHelper.getDefaultAccount();
        account.setUserName(email);
        account.save();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        WPComMagicLinkFragment wpComMagicLinkFragment = WPComMagicLinkFragment.newInstance(email);
        fragmentTransaction.replace(R.id.fragment_container, wpComMagicLinkFragment);
        fragmentTransaction.addToBackStack("magic_link_1");
        fragmentTransaction.commit();
    }
}
