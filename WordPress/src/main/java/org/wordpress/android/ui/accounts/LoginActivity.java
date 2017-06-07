package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.LoginEmailPasswordFragment;
import org.wordpress.android.ui.accounts.login.LoginListener;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkSentFragment;
import org.wordpress.android.ui.accounts.login.LoginPrologueFragment;
import org.wordpress.android.util.ToastUtils;

public class LoginActivity extends AppCompatActivity implements LoginListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            addLoginPrologueFragment();
        }
    }

    private void addLoginPrologueFragment() {
        LoginPrologueFragment loginSignupFragment = new LoginPrologueFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginSignupFragment, LoginPrologueFragment.TAG);
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

    private LoginPrologueFragment getLoginPrologueFragment() {
        return (LoginPrologueFragment) getSupportFragmentManager().findFragmentByTag(LoginPrologueFragment.TAG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }

    // LoginListener implementation methods

    @Override
    public void nextPromo() {
        if (getLoginPrologueFragment() != null) {
            getLoginPrologueFragment().onNextPromo();
        }
    }

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
    public void showMagicLinkRequestScreen(String email) {
        LoginMagicLinkRequestFragment loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(email);
        slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG);
    }

    @Override
    public void loginViaUsernamePassword() {
        ToastUtils.showToast(this, "Fall back to username/password is not implemented yet.");
    }

    @Override
    public void showMagicLinkSentScreen(String email) {
        LoginMagicLinkSentFragment loginMagicLinkSentFragment = LoginMagicLinkSentFragment.newInstance(email);
        slideInFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentFragment.TAG);
    }

    @Override
    public void openEmailClient() {
        ToastUtils.showToast(this, "Open email client is not implemented yet.");
    }

    @Override
    public void usePasswordInstead(String email) {
        LoginEmailPasswordFragment loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailFragment.TAG);
    }

    @Override
    public void forgotPassword() {
        ToastUtils.showToast(this, "Forgot password is not implemented yet");
    }

    @Override
    public void loggedInViaPassword() {
        ActivityLauncher.showMainActivityAndLoginEpilogue(this);
        finish();
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
