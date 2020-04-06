package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.widget.Toolbar;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.widgets.WPTextView;

import java.util.Calendar;

public class AboutActivity extends LocaleAwareActivity implements OnClickListener {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.about_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        WPTextView version = findViewById(R.id.about_version);
        version.setText(getString(R.string.version_with_name_param, WordPress.versionName));

        View tos = findViewById(R.id.about_tos);
        tos.setOnClickListener(this);

        View pp = findViewById(R.id.about_privacy);
        pp.setOnClickListener(this);

        WPTextView publisher = findViewById(R.id.about_publisher);
        publisher.setText(getString(R.string.publisher_with_company_param, getString(R.string.automattic_inc)));

        WPTextView copyright = findViewById(R.id.about_copyright);
        copyright.setText(
                getString(R.string.copyright_with_year_and_company_params, Calendar.getInstance().get(Calendar.YEAR),
                        getString(R.string.automattic_inc)));

        View about = findViewById(R.id.about_url);
        about.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String url;
        int id = v.getId();
        if (id == R.id.about_url) {
            url = Constants.URL_AUTOMATTIC;
        } else if (id == R.id.about_tos) {
            url = WPUrlUtils.buildTermsOfServiceUrl(this);
        } else if (id == R.id.about_privacy) {
            url = Constants.URL_PRIVACY_POLICY;
        } else {
            return;
        }
        ActivityLauncher.openUrlExternal(this, url);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
