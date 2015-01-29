package org.wordpress.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.ToastUtils;

public class WPLaunchActivity extends ActionBarActivity {

    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent = new Intent(this, WPMainActivity.class);
        startActivity(intent);
        finish();
    }
}
