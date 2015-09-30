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
import android.text.TextUtils;

import org.wordpress.android.ui.prefs.SettingsFragment;

import java.util.Locale;

public class WPActivityUtils {
    public static void applyLocale(Activity context, boolean restart) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.contains(SettingsFragment.LANGUAGE_PREF_KEY)) {
            Locale contextLocale = context.getResources().getConfiguration().locale;
            String contextLanguage = contextLocale.getLanguage();
            String contextCountry = contextLocale.getCountry();
            String locale = sharedPreferences.getString(SettingsFragment.LANGUAGE_PREF_KEY, "");

            if (!TextUtils.isEmpty(contextCountry)) {
                contextLanguage += "-" + contextCountry;
            }

            if (!locale.equals(contextLanguage)) {
                Resources resources = context.getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = new Locale(locale);
                resources.updateConfiguration(conf, resources.getDisplayMetrics());

                if (restart) {
                    Intent refresh = new Intent(context, context.getClass());
                    refresh.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
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
