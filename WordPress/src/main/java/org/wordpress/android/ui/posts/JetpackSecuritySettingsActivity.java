package org.wordpress.android.ui.posts;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.wordpress.android.R;

public class JetpackSecuritySettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_jetpack_security_settings);

        setupToolbar();
    }

    private void setupToolbar() {
        setTitle(getResources().getText(R.string.jetpack_security_setting_title));

        Toolbar toolbar = findViewById(org.wordpress.mobile.ReactNativeGutenbergBridge.R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_24px);
                actionBar.setSubtitle("");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemID = item.getItemId();

        if (itemID == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
