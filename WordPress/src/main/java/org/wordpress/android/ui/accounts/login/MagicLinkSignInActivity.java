package org.wordpress.android.ui.accounts.login;

import android.app.FragmentManager;
import android.app.FragmentTransaction;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.SignInFragment;

public class MagicLinkSignInActivity extends SignInActivity implements WPComMagicLinkFragment.OnMagicLinkFragmentInteraction, MagicLinkSignInFragment.OnMagicLinkRequestListener {
    private String mEmail = "";

    @Override
    public SignInFragment getSignInFragment() {
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
        magicLinkSignInFragment.setIsMagicLink(false);
        fragmentTransaction.replace(R.id.fragment_container, magicLinkSignInFragment);
        fragmentTransaction.addToBackStack("sign_in");
        fragmentTransaction.commit();
    }

    @Override
    public void onMagicLinkRequestSuccess() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        WPComMagicLinkFragment wpComMagicLinkFragment = WPComMagicLinkFragment.newInstance(mEmail);
        fragmentTransaction.replace(R.id.fragment_container, wpComMagicLinkFragment);
        fragmentTransaction.addToBackStack("sign_in");
        fragmentTransaction.commit();
    }
}
