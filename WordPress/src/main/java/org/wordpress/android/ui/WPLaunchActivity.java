package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.SettingsFragment;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.Locale;

public class WPLaunchActivity extends Activity {

    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup - note that it's defined in the
     * manifest to have no UI
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfilingUtils.split("WPLaunchActivity.onCreate");

        applyLocale();

        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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
