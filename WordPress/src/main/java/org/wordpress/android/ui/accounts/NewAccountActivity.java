package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.R;

// TODO: merge it with WelcomeFragmentSignIn
public class NewAccountActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_account_activity);
    }
}
