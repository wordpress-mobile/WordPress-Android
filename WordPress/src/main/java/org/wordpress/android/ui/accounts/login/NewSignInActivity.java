package org.wordpress.android.ui.accounts.login;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.SignInFragment;

public class NewSignInActivity extends AppCompatActivity implements WPComMagicLinkFragment.OnMagicLinkFragmentInteraction, NewSignInFragment.OnMagicLinkRequestListener {
    public String mEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_sign_in_activity);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        NewSignInFragment fragment = new NewSignInFragment();
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onMagicLinkSent() {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        MagicLinkSentFragment fragment = new MagicLinkSentFragment();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.setTransition(android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onSelfHostedRequested(boolean isWPCom) {
        Intent intent = new Intent(this, SignInActivity.class);

        if (isWPCom) {
            intent.putExtra(SignInActivity.START_FRAGMENT_KEY, SignInActivity.NEW_LOGIN_WP_COM);
        } else {
            intent.putExtra(SignInActivity.START_FRAGMENT_KEY, SignInActivity.NEW_LOGIN_SELF_HOSTED);
        }
        startActivity(intent);
    }

    @Override
    public void onMagicLinkRequestSuccess() {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        WPComMagicLinkFragment fragment = WPComMagicLinkFragment.newInstance(mEmail);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.setTransition(android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
