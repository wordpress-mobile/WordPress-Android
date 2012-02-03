package org.wordpress.android;

import org.apache.http.protocol.HTTP;

import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class WPCOMReaderDetailPage extends WPCOMReaderBase {
	
	private String cachedPage = null;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.reader);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			cachedPage = extras.getString("cachedPage");
		}
		
		this.setTitle("Loading...");

		final WebView wv = (WebView) findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);
		wv.addJavascriptInterface( new JavaScriptInterface(this), "Android" );
		wv.setWebViewClient(new WordPressWebViewClient());
		
		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				if (progress == 100) {
					//String methodCall = "Reader2.show_article_details()";
					//wv.loadUrl("javascript:"+methodCall);
				}
			}
		});
		
		wv.loadData(Uri.encode(this.cachedPage), "text/html", HTTP.UTF_8);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int i, KeyEvent event) {

		if (i == KeyEvent.KEYCODE_BACK) {
				finish();
		}

		return false;
	}
}
