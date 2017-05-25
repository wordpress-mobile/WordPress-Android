package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LogInOrSignUpFragment;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.LoginListener;
import org.wordpress.android.util.ToastUtils;

public class LoginActivity extends AppCompatActivity implements LoginListener {

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

    // LoginListener implementation methods

    @Override
    public void showEmailLoginScreen() {
        LoginEmailFragment loginEmailFragment = new LoginEmailFragment();
        slideInFragment(loginEmailFragment, true, LoginEmailFragment.TAG);
    }

    @Override
    public void doStartSignup() {
        ToastUtils.showToast(this, "Signup is not implemented yet");
    }

    @Override
    public void gotEmail(String email) {
        ToastUtils.showToast(this, "Input email is not implemented yet. Input email: " + email);
    }

    @Override
    public void loginViaUsernamePassword() {
        ToastUtils.showToast(this, "Fall back to username/password is not implemented yet.");
    }

    @Override
    public void gotSiteAddress(String siteAddress) {
        ToastUtils.showToast(this, "Input site address is not implemented yet. Input site address: " + siteAddress);
    }

    @Override
    public void help() {
        ToastUtils.showToast(this, "Help is not implemented yet.");
    }
}
