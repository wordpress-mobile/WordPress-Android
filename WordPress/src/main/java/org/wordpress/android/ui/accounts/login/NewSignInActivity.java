package org.wordpress.android.ui.accounts.login;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;

public class NewSignInActivity extends AppCompatActivity implements WPComMagicLinkFragment.OnFragmentInteractionListener {
    public String email = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_sign_in_activity);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        NewSignInActivityFragment fragment = new NewSignInActivityFragment();
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public void logIn() {
        // Logic for checking if WordPress.com or self-hosted
        boolean isWPCom = true;
        if (isWPCom) {
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            WPComMagicLinkFragment fragment = WPComMagicLinkFragment.newInstance(email);
            fragmentTransaction.add(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        } else {
            // self-hosted flow
        }
    }

    public void requestMagicLink() {

    }

    @Override
    public void onFragmentInteraction(Boolean shouldSendMagicLink) {
        if (shouldSendMagicLink) {
            // request url
        } else {
            // send to password screen
        }
    }
}
