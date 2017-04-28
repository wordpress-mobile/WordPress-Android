package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.login.LogInOrSignUpFragment;

public class LoginActivity extends AppCompatActivity implements
        LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction {

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
    public void onLoginTapped() {
        Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateSiteTapped() {
        Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
    }
}
