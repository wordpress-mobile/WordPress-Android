package org.wordpress.android.ui.posts;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.wordpress.android.R;

public class JetpackSecuritySettingsActivity extends AppCompatActivity {
    public static final int JETPACK_SECURITY_SETTINGS_REQUEST_CODE = 101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_jetpack_security_settings);

        setupToolbar();
    }

    private void setupToolbar() {
        setTitle(getResources().getText(R.string.jetpack_security_setting_title));

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemID = item.getItemId();

        if (itemID == android.R.id.home) {
            setResult(RESULT_OK, null);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        finish();
    }
}
