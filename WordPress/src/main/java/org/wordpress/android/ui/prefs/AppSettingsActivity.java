package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.util.LocaleManager;

public class AppSettingsActivity extends AppCompatActivity {
    private static final String KEY_APP_SETTINGS_FRAGMENT = "app-settings-fragment";

    private AppSettingsFragment mAppSettingsFragment;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.me_btn_app_settings);
        }

        FragmentManager fragmentManager = getFragmentManager();
        mAppSettingsFragment = (AppSettingsFragment) fragmentManager.findFragmentByTag(KEY_APP_SETTINGS_FRAGMENT);
        if (mAppSettingsFragment == null) {
            mAppSettingsFragment = new AppSettingsFragment();

            fragmentManager.beginTransaction()
                           .add(android.R.id.content, mAppSettingsFragment, KEY_APP_SETTINGS_FRAGMENT)
                           .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
