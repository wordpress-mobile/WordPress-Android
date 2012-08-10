package org.wordpress.android;

import org.wordpress.android.R;
import org.wordpress.android.util.BlackBerryUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity implements OnClickListener {

	private static final String URL_TOS = "http://en.wordpress.com/tos";
	private static final String URL_AUTOMATTIC = "http://automattic.com";
	private static final String URL_PRIVACY_POLICY = "/privacy";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.about);

		if (BlackBerryUtils.getInstance().isBlackBerry()) {
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
		switch (v.getId()) {
		case R.id.about_url:
			uri = Uri.parse(URL_AUTOMATTIC);
			break;
		case R.id.about_tos:
			uri = Uri.parse(URL_TOS);
			break;
		case R.id.about_privacy:
			uri = Uri.parse(URL_AUTOMATTIC + URL_PRIVACY_POLICY);
			break;
		default:
			return;
		}
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
}
