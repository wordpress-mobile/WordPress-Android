package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LogInOrSignUpFragment;
import org.wordpress.android.ui.accounts.login.nav.LoginNavigationFsm;

public class LoginActivity extends AppCompatActivity implements LoginNavigationFsm.ContextImplementation {

    LoginNavigationFsm mLoginNavigationFsm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.welcome_activity);

        if (savedInstanceState == null) {
            addLoginPrologueFragment();
        }

         mLoginNavigationFsm = new LoginNavigationFsm(this);
    }

    protected void addLoginPrologueFragment() {
        LogInOrSignUpFragment loginSignupFragment = new LogInOrSignUpFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginSignupFragment, LogInOrSignUpFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLoginNavigationFsm.register();
    }

    @Override
    public void onStop() {
        mLoginNavigationFsm.unregister();
        super.onStop();
    }


    @Override
    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
