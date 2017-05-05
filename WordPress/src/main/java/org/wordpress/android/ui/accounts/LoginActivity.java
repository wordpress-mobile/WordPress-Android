package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LogInOrSignUpFragment;
import org.wordpress.android.ui.accounts.login.nav.LoginEvents;
import org.wordpress.android.ui.accounts.login.nav.LoginNavigationController;
import org.wordpress.android.ui.accounts.login.nav.LoginNavigationFsmGetter;
import org.wordpress.android.ui.accounts.login.nav.LoginState;

public class LoginActivity extends AppCompatActivity implements
        LoginNavigationFsmGetter,
        LoginNavigationController.ContextImplementation {

    LoginNavigationController mLoginNavigationController = new LoginNavigationController(LoginState.PROLOGUE, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.welcome_activity);

        if (savedInstanceState == null) {
            addLoginPrologueFragment();
        }
    }

    protected void addLoginPrologueFragment() {
        LogInOrSignUpFragment loginSignupFragment = new LogInOrSignUpFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginSignupFragment, LogInOrSignUpFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public LoginEvents.LoginNavPrologue getLoginNavPrologue() {
        return mLoginNavigationController.getLoginNavPrologue();
    }

    @Override
    public LoginEvents.LoginNavInputEmail getLoginNavInputEmail() {
        return mLoginNavigationController.getLoginNavInputEmail();
    }

    @Override
    public LoginEvents.LoginNavInputSiteAddress getLoginNavInputSiteAddress() {
        return mLoginNavigationController.getLoginNavInputSiteAddress();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
