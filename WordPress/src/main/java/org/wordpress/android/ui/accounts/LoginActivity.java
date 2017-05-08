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
import org.wordpress.android.ui.accounts.login.nav.ContextImplementation;
import org.wordpress.android.ui.accounts.login.nav.LoginFsmGetter;
import org.wordpress.android.ui.accounts.login.nav.LoginNav;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity implements LoginFsmGetter, ContextImplementation {
    private static final String KEY_NAV_HISTORY = "KEY_NAV_HISTORY";

    LoginNavController mLoginNavController;

    @SuppressWarnings("unchecked")
    private void initLoginNavController(Bundle savedInstanceState) {
        // perform the Nav history loading first so updated fragments can find their expected state there.
        if (savedInstanceState == null) {
            mLoginNavController = new LoginNavController(LoginNav.Prologue.class, this);
        } else {
            ArrayList<Class<? extends LoginNav>> history =
                    (ArrayList<Class<? extends LoginNav>>) savedInstanceState.getSerializable(KEY_NAV_HISTORY);
            mLoginNavController = new LoginNavController(history, this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // perform the Nav history loading first so updated fragments can find their expected state there.
        initLoginNavController(savedInstanceState);

        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            addLoginPrologueFragment();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<Class<? extends LoginNav>> navHistory = mLoginNavController.getNavHistory();
        outState.putSerializable(KEY_NAV_HISTORY, navHistory);
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
        mLoginNavController.goBack();

        super.onBackPressed();
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
