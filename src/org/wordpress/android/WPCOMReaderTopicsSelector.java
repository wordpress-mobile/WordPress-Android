package org.wordpress.android;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WPCOMReaderTopicsSelector extends WPCOMReaderBase{
	
	public static int activityRequestCode = 1234322;

	//private String topicID = null;
	public WebView wv;
	
	public static WPCOMReaderTopicsSelector newInstance() {
		WPCOMReaderTopicsSelector f = new WPCOMReaderTopicsSelector();
        return f;
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.reader, container, false);
		
		//this.setTitle("Loading...");

		wv = (WebView) v.findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);
		wv.addJavascriptInterface( new JavaScriptInterface(getActivity().getBaseContext()), "Android" );
		wv.setWebViewClient(new WordPressWebViewClient());
		wv.loadUrl(Constants.readerTopicsURL);
		
		return v;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (wv != null) {
			wv.stopLoading();
			wv.clearCache(true);
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}
}
