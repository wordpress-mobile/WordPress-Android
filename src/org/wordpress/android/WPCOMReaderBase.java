package org.wordpress.android;

import java.net.URLEncoder;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public abstract class WPCOMReaderBase extends Fragment {
	
	protected final String interfaceNameForJS = "Android";
	
	protected String httpuser = "";
	protected String httppassword = "";
	
	public String topicTitle;

	private UpdateTopicTitleListener updateTopicTitleListener;
	private UpdateTopicIDListener updateTopicIDListener;
	private ChangeTopicListener onChangeTopicListener;
	private GetLoadedItemsListener getLoadedItemsListener;
	private UpdateButtonStatusListener updateButtonStatusListener;
	private GetPermalinkListener getPermalinkListener;
	private GetLastSelectedItemListener getLastSelectedItemListener;
	
	protected void setDefaultWebViewSettings(WebView wv) {
		WebSettings webSettings = wv.getSettings();
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setUserAgentString("wp-android");
		webSettings.setSavePassword(false);
	}
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			updateTopicTitleListener = (UpdateTopicTitleListener) activity;
			updateTopicIDListener = (UpdateTopicIDListener) activity;
			onChangeTopicListener = (ChangeTopicListener) activity;
			getLoadedItemsListener = (GetLoadedItemsListener) activity;
			updateButtonStatusListener = (UpdateButtonStatusListener) activity;
			getPermalinkListener = (GetPermalinkListener) activity;
			getLastSelectedItemListener = (GetLastSelectedItemListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}
	
	protected String getAuthorizeHybridURL(String URL) {
		
		if( ! isValidHybridURL(URL) ) return URL;
		
		if( URL.contains("?") )
			return URL + "&wpcom-hybrid-auth-token=" + URLEncoder.encode( this.getHybridAuthToken() );
		else 
			return URL + "?wpcom-hybrid-auth-token=" + URLEncoder.encode( this.getHybridAuthToken() );
	}
	
	protected boolean isValidHybridURL(String URL) {
		return URL.contains(Constants.authorizedHybridHost);
	}

	protected String getHybridAuthToken() {
		// gather all of the device info
		String uuid = WordPress.wpDB.getUUID(getActivity().getApplicationContext());
		if (uuid == "") {
			uuid = UUID.randomUUID().toString();
			WordPress.wpDB.updateUUID(uuid);
		}
		return uuid;
	}
	
	protected class WordPressWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// setProgressBarIndeterminateVisibility(false);
			view.clearCache(true);
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			handler.proceed(httpuser, httppassword);
		}
	}
	
	
	protected class JavaScriptInterface {
	    Context mContext;

	    /** Instantiate the interface and set the context */
	    JavaScriptInterface(Context c) {
	        mContext = c;
	    }

	    public void setTopicTitle(String topicTitle) {
	    	updateTopicTitleListener.updateTopicTitle(topicTitle);
	    }
	    
	    public void setSelectedTopic(String topicID) {
	    	updateTopicIDListener.onUpdateTopicID(topicID);
	    }
	    
	    public void selectTopic(String topicID, String topicName) {
		 	onChangeTopicListener.onChangeTopic(topicID, topicName);
		}
	    
	    public void getLoadedItems(String items) {
	    	getLoadedItemsListener.getLoadedItems(items);
	    }
	    
	    public void getArticlePermalink(String permalink) {
	    	getPermalinkListener.getPermalink(permalink);
	    }
	    
	    public void getLastSelectedItem(String item) {
	    	getLastSelectedItemListener.getLastSelectedItem(item);
	    }
	    
	    public void hasPrev(boolean isPrev) {
	    	updateButtonStatusListener.updateButtonStatus(0, isPrev);
	    }
	    
	    public void hasNext(String isNext) {
	    	updateButtonStatusListener.updateButtonStatus(1, Boolean.parseBoolean(isNext));
	    }
	}
	
	public interface UpdateTopicTitleListener {
		public void updateTopicTitle(String topicTitle);
	}
	
	public interface UpdateTopicIDListener {
		public void onUpdateTopicID(String topicID);
	}
	
	public interface ChangeTopicListener {
		public void onChangeTopic(String topicID, String topicName);
	}
	
	public interface GetLoadedItemsListener {
		public void getLoadedItems(String items);
	}
	
	public interface UpdateButtonStatusListener {
		public void updateButtonStatus(int button, boolean enabled);
	}
	
	public interface GetPermalinkListener {
		public void getPermalink(String permalink);
	}
	
	public interface GetLastSelectedItemListener {
		public void getLastSelectedItem(String lastSelectedItem);
	}
	
	
}
