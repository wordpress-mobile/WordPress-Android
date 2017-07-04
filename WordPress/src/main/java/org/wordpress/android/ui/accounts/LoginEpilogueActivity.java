package org.wordpress.android.ui.accounts;

import org.wordpress.android.R;
import org.wordpress.android.util.ToastUtils;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

public class LoginEpilogueActivity extends AppCompatActivity implements LoginEpilogueFragment.LoginEpilogueListener {
    public static final String EXTRA_SHOW_AND_RETURN = "EXTRA_SHOW_AND_RETURN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_epilogue_activity);

        if (savedInstanceState == null) {
            boolean showAndReturn = getIntent().getBooleanExtra(EXTRA_SHOW_AND_RETURN, false);

            addPostLoginFragment(showAndReturn);
        }
    }

    protected void addPostLoginFragment(boolean showAndReturn) {
        LoginEpilogueFragment loginEpilogueFragment = LoginEpilogueFragment.newInstance(showAndReturn);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginEpilogueFragment, LoginEpilogueFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onConnectAnotherSite() {
        ToastUtils.showToast(this, "Connect another site is not implemented yet.");
    }

    @Override
    public void onContinue() {
        setResult(RESULT_OK);
        finish();
    }
}
