package org.wordpress.android;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WPCOMReaderTopicsSelector extends WPCOMReaderBase {
	
	public static int activityRequestCode = 1234322;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.reader);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			 extras.getString("currentTopic");
		}

		WebView wv = (WebView) findViewById(R.id.webView);
		WebSettings webSettings = wv.getSettings();
//		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setUserAgentString("wp-android");
		wv.addJavascriptInterface( new JavaScriptInterface(this), "Android" );
		this.setTitle("Loading...");

		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				WPCOMReaderTopicsSelector.this.setProgress(progress * 100);
			}
		});

		wv.setWebViewClient(new WordPressWebViewClient());
		String hybURL = this.getAuthorizeHybridURL(Constants.readerTopicsURL);
		wv.loadUrl(hybURL);
	}

	//Methods called from JS
	public void setTitleFromJS(String title) {
		final String fTitle = title;
		runOnUiThread(new Runnable() {
		     public void run() {
		    	 WPCOMReaderTopicsSelector.this.setTitle(fTitle);
		    }
		});
	}
	
	public void selectTopicFromJS(String topicID, String topicName) {
	 	Intent databackIntent = new Intent(); 
	 	databackIntent.putExtra("topicID", topicID); 
	 	databackIntent.putExtra("topicName", topicName);
	 	setResult(RESULT_OK, databackIntent);
		finish();
	}
	//End of methods declartion called from the JS code
	
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
