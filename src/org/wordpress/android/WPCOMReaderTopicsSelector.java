package org.wordpress.android;

import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WPCOMReaderTopicsSelector extends WPCOMReaderBase{
	
	public static int activityRequestCode = 1234322;

	private String topicID = null;
	private String cachedTopicsPage = null;
	private ChangeTopicListener onChangeTopicListener;
	public WebView wv;
	
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
		String hybURL = this.getAuthorizeHybridURL(Constants.readerTopicsURL);
		
		if ( this.cachedTopicsPage != null )
			wv.loadData(Uri.encode(this.cachedTopicsPage), "text/html", HTTP.UTF_8);
		else
			wv.loadUrl(hybURL);
		
		return v;
	}
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			onChangeTopicListener = (ChangeTopicListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}

	//Methods called from JS
	public void setTitleFromJS(String title) {
		//onChangeTopicListener.onChangeTopic(topicID, fTitle);
	}
	
	public void selectTopicFromJS(String topicID, String topicName) {
	 	onChangeTopicListener.onChangeTopic(topicID, topicName);
	}
	//End of methods called from the JS code
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}
	
	public interface ChangeTopicListener {
		public void onChangeTopic(String topicID, String topicName);
	}
}
