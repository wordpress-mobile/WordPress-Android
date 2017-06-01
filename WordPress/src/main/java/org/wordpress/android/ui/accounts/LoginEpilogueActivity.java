package org.wordpress.android.ui.accounts;

import org.wordpress.android.R;
import org.wordpress.android.util.ToastUtils;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

public class LoginEpilogueActivity extends AppCompatActivity implements LoginEpilogueFragment.LoginEpilogueListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_epilogue_activity);

        if (savedInstanceState == null) {
            addPostLoginFragment();
        }
    }

    protected void addPostLoginFragment() {
        LoginEpilogueFragment loginEpilogueFragment = new LoginEpilogueFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginEpilogueFragment, LoginEpilogueFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onConnectanotherSite() {
        ToastUtils.showToast(this, "Connect another site is not implemented yet.");
    }

    @Override
    public void onContinue() {
        setResult(RESULT_OK);
        finish();
    }
}
