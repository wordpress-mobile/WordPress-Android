package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.ui.prefs.SettingsFragment;

import java.util.Locale;

public class WPActivityUtils {
    public static void applyLocale(Activity context, boolean restart) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.contains(SettingsFragment.LANGUAGE_PREF_KEY)) {
            String contextLocale = context.getResources().getConfiguration().locale.getLanguage();
            String locale = sharedPreferences.getString(SettingsFragment.LANGUAGE_PREF_KEY, "");

            if (!locale.equals(contextLocale)) {
                Resources resources = context.getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = new Locale(locale);
                resources.updateConfiguration(conf, resources.getDisplayMetrics());

                if (restart) {
                    Intent refresh = new Intent(context, context.getClass());
                    context.startActivity(refresh);
                    context.finish();
                    context.overridePendingTransition(0, 0);
                }
            }
        }
    }

    public static Context getThemedContext(Context context) {
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity)context).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
        }
        return context;
    }
}
