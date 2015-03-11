package org.wordpress.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.prefs.SettingsFragment;
import org.wordpress.android.util.ToastUtils;

import java.util.Locale;

public class WPLaunchActivity extends ActionBarActivity {

    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyLocale();

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

    private void applyLocale() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.contains(SettingsFragment.SETTINGS_PREFERENCES)) {
            String locale = sharedPreferences.getString(SettingsFragment.SETTINGS_PREFERENCES, "");

            if (!locale.equals(Locale.getDefault().getDisplayLanguage())) {
                Resources resources = getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = new Locale(locale);
                resources.updateConfiguration(conf, resources.getDisplayMetrics());

                Intent refresh = new Intent(this, WPLaunchActivity.class);
                startActivity(refresh);
                finish();
            }
        }
    }
}
