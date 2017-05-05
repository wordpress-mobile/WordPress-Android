package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LogInOrSignUpFragment;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController.*;
import org.wordpress.android.ui.accounts.login.nav.LoginFsmGetter;
import org.wordpress.android.ui.accounts.login.nav.LoginNav;

public class LoginActivity extends AppCompatActivity implements LoginFsmGetter, ContextImplementation {

    LoginNavController mLoginNavController = new LoginNavController(LoginNav.Prologue.class, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            addLoginPrologueFragment();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        mLoginNavController.goBack();
    }

    protected void addLoginPrologueFragment() {
        LogInOrSignUpFragment loginSignupFragment = new LogInOrSignUpFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginSignupFragment, LogInOrSignUpFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public LoginNav.Prologue getLoginNavPrologue() {
        return mLoginNavController.getLoginNavPrologue();
    }

    @Override
    public LoginNav.InputEmail getLoginNavInputEmail() {
        return mLoginNavController.getLoginNavInputEmail();
    }

    @Override
    public LoginNav.InputSiteAddress getLoginNavInputSiteAddress() {
        return mLoginNavController.getLoginNavInputSiteAddress();
    }

    private void slideInFragment(Fragment fragment, String tag) {
        slideInFragment(fragment, true, tag);
    }

    private void slideInFragment(Fragment fragment, boolean shouldAddToBackStack, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showEmailLoginScreen() {
        LoginEmailFragment loginEmailFragment = new LoginEmailFragment();
        slideInFragment(loginEmailFragment, LoginEmailFragment.TAG);
    }
}
