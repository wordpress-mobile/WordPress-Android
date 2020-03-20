package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;

public class WPLaunchActivity extends AppCompatActivity {
    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup - note that it's defined in the
     * manifest to have no UI
     */

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfilingUtils.split("WPLaunchActivity.onCreate");
        launchWPMainActivity();
    }

    private void launchWPMainActivity() {
        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(getIntent().getAction());
        intent.setData(getIntent().getData());
        startActivity(intent);
        finish();
    }
}
