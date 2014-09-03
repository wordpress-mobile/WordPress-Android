package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import org.wordpress.android.R;

// TODO: merge it with WelcomeFragmentSignIn
public class NewAccountActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_new_account);
    }
}