package org.wordpress.android.ui.prefs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.widgets.WPTextView;

import java.util.Calendar;

public class AboutActivity extends AppCompatActivity implements OnClickListener {
    private static final String URL_TOS = "http://en.wordpress.com/tos";
    private static final String URL_AUTOMATTIC = "http://automattic.com";
    private static final String URL_PRIVACY_POLICY = "/privacy";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.about_activity);

        WPTextView version = (WPTextView) findViewById(R.id.about_version);
        version.setText(getString(R.string.version) + " " + WordPress.versionName);

        WPTextView tos = (WPTextView) findViewById(R.id.about_tos);
        tos.setOnClickListener(this);

        WPTextView pp = (WPTextView) findViewById(R.id.about_privacy);
        pp.setOnClickListener(this);

        WPTextView publisher = (WPTextView) findViewById(R.id.about_publisher);
        publisher.setText(getString(R.string.publisher) + " " + getString(R.string.automattic_inc));

        WPTextView copyright = (WPTextView) findViewById(R.id.about_copyright);
        copyright.setText("©" + Calendar.getInstance().get(Calendar.YEAR) + " " + getString(R.string.automattic_inc));

        WPTextView about = (WPTextView) findViewById(R.id.about_url);
        about.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String url;
        int id = v.getId();
        if (id == R.id.about_url) {
            url = URL_AUTOMATTIC;
        } else if (id == R.id.about_tos) {
            url = URL_TOS;
        } else if (id == R.id.about_privacy) {
            url = URL_AUTOMATTIC + URL_PRIVACY_POLICY;
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
