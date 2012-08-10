package org.wordpress.android;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Signup extends Activity {
	public Activity activity = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		WebView webview = new WebView(this);

		setContentView(webview);
		setTitle(getResources().getText(R.string.new_account));

		setProgressBarIndeterminateVisibility(true);

		webview.getSettings().setUserAgentString("wp-android/" + WordPress.versionName);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webview.setWebViewClient(new WordPressWebViewClient());
		webview.loadUrl("https://en.wordpress.com/signup/?ref=wp-android");

	}

	private class WordPressWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("wordpress://wpcom_signup_completed")) {
				if (url.indexOf("username=") > 0) {
					String username = url.substring(url.indexOf("username=") + 9, url.length());
					Bundle bundle = new Bundle();
					bundle.putString("username", username);
					Intent mIntent = new Intent();
					mIntent.putExtras(bundle);
					setResult(RESULT_OK, mIntent);
					finish();
				}
			}
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			setProgressBarIndeterminateVisibility(false);
			view.clearCache(true);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

}
