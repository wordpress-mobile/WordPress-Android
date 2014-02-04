package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.passcodelock.AppLockManager;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.DeviceUtils;

public class AboutActivity extends Activity implements OnClickListener {

    private static final String URL_TOS = "http://en.wordpress.com/tos";
    private static final String URL_AUTOMATTIC = "http://automattic.com";
    private static final String URL_PRIVACY_POLICY = "/privacy";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.about);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (DeviceUtils.getInstance().isBlackBerry()) {
            TextView appTitle = (TextView) findViewById(R.id.about_first_line);
            appTitle.setText(getString(R.string.app_title_blackberry));
        }

        TextView version = (TextView) findViewById(R.id.about_version);
        version.setText(getString(R.string.version) + " "
                + WordPress.versionName);

        Button tos = (Button) findViewById(R.id.about_tos);
        tos.setOnClickListener(this);

        Button pp = (Button) findViewById(R.id.about_privacy);
        pp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Uri uri;
        int id = v.getId();
        if (id == R.id.about_url) {
            uri = Uri.parse(URL_AUTOMATTIC);
        } else if (id == R.id.about_tos) {
            uri = Uri.parse(URL_TOS);
        } else if (id == R.id.about_privacy) {
            uri = Uri.parse(URL_AUTOMATTIC + URL_PRIVACY_POLICY);
        } else {
            return;
        }
        AppLockManager.getInstance().setExtendedTimeout();
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
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
