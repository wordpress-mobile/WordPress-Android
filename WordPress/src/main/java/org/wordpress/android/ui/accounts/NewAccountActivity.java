package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;

import org.wordpress.android.R;

// TODO: merge it with WelcomeFragmentSignIn
public class NewAccountActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_new_account);
    }
}