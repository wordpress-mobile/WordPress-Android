package org.wordpress.android;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WPCOMReaderDetailPage extends WPCOMReaderBase {
	
	//private String cachedPage = null;
	private String requestedURL = null;
	public WebView wv;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.reader, container, false);
		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			//cachedPage = extras.getString("cachedPage");
			requestedURL = extras.getString("requestedURL");
		}
		
		wv = (WebView) v.findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);
		wv.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); //override the default setting of NO_CACHE
		//wv.addJavascriptInterface( new JavaScriptInterface(getActivity().getApplicationContext()), interfaceNameForJS );
		wv.setWebViewClient(new WordPressWebViewClient());
		
		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				if (progress == 100) {
					//String methodCall = "Reader2.show_article_details()";
					//wv.loadUrl("javascript:"+methodCall);
				}
			}
		});
		
		//wv.loadData(Uri.encode(this.cachedPage), "text/html", HTTP.UTF_8);
		wv.loadUrl(requestedURL);
		return v;
    }
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}
}
