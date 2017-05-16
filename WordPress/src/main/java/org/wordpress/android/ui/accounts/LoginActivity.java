package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LogInOrSignUpFragment;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.LoginNavFragment;
import org.wordpress.android.ui.accounts.login.nav.LoginNavHandler;
import org.wordpress.android.ui.accounts.login.nav.LoginStateGetter;

public class LoginActivity extends AppCompatActivity implements LoginNavHandler, LoginStateGetter.FsmGetter {
    private static final String TAG_LOGIN_NAV_FRAGMENT = "TAG_LOGIN_NAV_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (!hasLoginNavFragment()) {
            addLoginNavFragment();
        }

        if (savedInstanceState == null) {
            addLoginPrologueFragment();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // perform the Nav back first so updated fragments can find their expected state there.
        getLoginNavFragment().goBack();

        super.onBackPressed();
    }

    /**
     * Retrieves the login navigation fragment from the FragmentManager.
     *  **NOTE** Should not be used too early before the navigation fragment has been added to the FragmentManager.
     * @return the login navigation fragment instance or null if not found
     */
    private LoginNavFragment getLoginNavFragment() {
        return (LoginNavFragment) getSupportFragmentManager().findFragmentByTag(TAG_LOGIN_NAV_FRAGMENT);
    }

    private boolean hasLoginNavFragment() {
        return getSupportFragmentManager().findFragmentByTag(TAG_LOGIN_NAV_FRAGMENT) != null;
    }

    private void addLoginNavFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(new LoginNavFragment(), TAG_LOGIN_NAV_FRAGMENT);
        fragmentTransaction.commit();
    }

    private void addLoginPrologueFragment() {
        LogInOrSignUpFragment loginSignupFragment = new LogInOrSignUpFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginSignupFragment, LogInOrSignUpFragment.TAG);
        fragmentTransaction.commit();
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
    public LoginStateGetter getLoginStateGetter() {
        return getLoginNavFragment().getLoginStateGetter();
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
