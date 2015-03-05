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
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SettingsFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;

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

        String lastActivityStr = AppPrefs.getLastActivityStr();
        ActivityId id = ActivityId.getActivityIdFromName(lastActivityStr);
        Intent intent = WPActivityUtils.getIntentForActivityId(this, id);
        AppLog.v(T.UTILS, "WPLaunchActivity,  activityName: " + lastActivityStr + ", activityId: " + id + ", " +
                "intent: " + intent);
        if (intent == null) {
            AppLog.v(T.UTILS, "Launch default Activity: PostsActivity");
            intent = new Intent(this, PostsActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void applyLocale() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.contains(SettingsFragment.SETTINGS_PREFERENCES)) {
            String locale = sharedPreferences.getString(SettingsFragment.SETTINGS_PREFERENCES, "");
            Locale storedLocale = new Locale(locale);

            if (!storedLocale.getDisplayLanguage().equals(getResources().getConfiguration().locale.getDisplayLanguage())) {
                Resources resources = getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = storedLocale;
                resources.updateConfiguration(conf, resources.getDisplayMetrics());

                Intent refresh = new Intent(this, WPLaunchActivity.class);
                startActivity(refresh);
                finish();
            }
        }
    }
}
