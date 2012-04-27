package org.wordpress.android;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

public class WPCOMReaderDetailPage extends WPCOMReaderBase {
	
	public WebView wv;
	public String readerItems;
	public ImageButton nextPost, prevPost;
	private LoadExternalURLListener loadExternalURLListener;
	
	public static WPCOMReaderDetailPage newInstance() {
		WPCOMReaderDetailPage f = new WPCOMReaderDetailPage();
        return f;
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.reader_detail, container, false);
		
		wv = (WebView) v.findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);
		
		wv.addJavascriptInterface( new JavaScriptInterface(getActivity().getBaseContext()), "Android" );
		wv.setWebViewClient(new DetailWebViewClient());

		wv.loadUrl(Constants.readerDetailURL);
		
		nextPost = (ImageButton) v.findViewById(R.id.down);
		nextPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				wv.loadUrl("javascript:Reader2.show_next_item();");
				wv.loadUrl("javascript:Reader2.is_next_item();");
				wv.loadUrl("javascript:Reader2.is_prev_item();");
			}
		});
		
		prevPost = (ImageButton) v.findViewById(R.id.up);
		prevPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				wv.loadUrl("javascript:Reader2.show_prev_item();");
				wv.loadUrl("javascript:Reader2.is_next_item();");
				wv.loadUrl("javascript:Reader2.is_prev_item();");
			}
		});
		
		
		return v;
    }
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			loadExternalURLListener = (LoadExternalURLListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
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
		}
	}

	public void updateButtonStatus(int button, boolean enabled) {
		if (button == 0) {
			prevPost.setEnabled(enabled);
		} else if (button == 1) {
			nextPost.setEnabled(enabled);
		}
		
	}
	
	protected class DetailWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (!url.equalsIgnoreCase(Constants.readerDetailURL)) {
				loadExternalURLListener.loadExternalURL(url);
				return true;
			}
			return false;
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//Log.d("WP", url);

		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (readerItems != null) {
				String method = "Reader2.set_loaded_items(" + readerItems + ")";
				wv.loadUrl("javascript:" + method);
				nextPost.setEnabled(true);
				prevPost.setEnabled(true);
			}
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
			HttpAuthHandler handler, String host, String realm) {
			handler.proceed(httpuser, httppassword);
		}
	}
	
	public interface LoadExternalURLListener {
		public void loadExternalURL(String url);
	}
}

