package org.wordpress.android.ui.accounts.login;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.SignInActivity;

public class MagicLinkSignInActivity extends SignInActivity implements WPComMagicLinkFragment.OnMagicLinkFragmentInteraction, MagicLinkSignInFragment.OnMagicLinkRequestListener {
    private String mEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            if (uri.getHost().equals("magic-login")) {
                attemptLoginWithMagicLink(uri.getQueryParameter("token"));
            }
        }
    }

    @Override
    public MagicLinkSignInFragment getSignInFragment() {
        return new MagicLinkSignInFragment();
    }

    @Override
    public void onMagicLinkSent() {

    }

    @Override
    public void onEnterPasswordRequested() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MagicLinkSignInFragment magicLinkSignInFragment = new MagicLinkSignInFragment();
        fragmentTransaction.replace(R.id.fragment_container, magicLinkSignInFragment);
        fragmentTransaction.addToBackStack("sign_in");
        fragmentTransaction.commit();
    }

    @Override
    public void onMagicLinkRequestSuccess(String email) {
        mEmail = email;

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        WPComMagicLinkFragment wpComMagicLinkFragment = WPComMagicLinkFragment.newInstance(mEmail);
        fragmentTransaction.replace(R.id.fragment_container, wpComMagicLinkFragment);
        fragmentTransaction.addToBackStack("sign_in");
        fragmentTransaction.commit();
    }

    private void attemptLoginWithMagicLink(String token) {
        getSignInFragment().signInAndFetchBlogListWPCom(token);
    }
}
