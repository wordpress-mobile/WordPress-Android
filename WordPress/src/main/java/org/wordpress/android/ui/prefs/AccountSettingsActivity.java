package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.util.LocaleManager;

public class AccountSettingsActivity extends AppCompatActivity {
    private static final String KEY_ACCOUNT_SETTINGS_FRAGMENT = "account-settings-fragment";

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
            actionBar.setTitle(R.string.account_settings);
        }

        FragmentManager fragmentManager = getFragmentManager();
        AccountSettingsFragment accountSettingsFragment =
                (AccountSettingsFragment) fragmentManager.findFragmentByTag(KEY_ACCOUNT_SETTINGS_FRAGMENT);
        if (accountSettingsFragment == null) {
            accountSettingsFragment = new AccountSettingsFragment();

            fragmentManager.beginTransaction()
                           .add(android.R.id.content, accountSettingsFragment, KEY_ACCOUNT_SETTINGS_FRAGMENT)
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
