package org.wordpress.android;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class WPCOMReaderWebPage extends WPCOMReaderBase {

	// private String cachedPage = null;
	public WebView wv;
	public ProgressBar progressBar;
	
	public static WPCOMReaderWebPage newInstance() {
		WPCOMReaderWebPage f = new WPCOMReaderWebPage();
        return f;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.reader_web_page, container, false);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		wv = (WebView) v.findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		
		wv.addJavascriptInterface(new JavaScriptInterface(getActivity()
				.getApplicationContext()), interfaceNameForJS);
		wv.setWebViewClient(new DetailWebViewClient());
		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				progressBar.setProgress(progress);
				if (progress == 100)
					progressBar.setVisibility(View.GONE);
			}
		});

		return v;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (wv != null) {
			wv.stopLoading();
			wv.clearCache(true);
		}
	}

	protected class DetailWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted (WebView view, String url, Bitmap favicon) {
			progressBar.setVisibility(View.VISIBLE);
		}
	}
}
