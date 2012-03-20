package org.wordpress.android;

import android.content.res.Configuration;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;

public class WPCOMReaderDetailPage extends WPCOMReaderBase {
	
	//private String cachedPage = null;
	private String requestedURL = null;
	public WebView wv;
	public String readerItems;
	public ImageButton nextPost, prevPost;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.reader_detail, container, false);
		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			//cachedPage = extras.getString("cachedPage");
			requestedURL = extras.getString("requestedURL");
		}
		
		wv = (WebView) v.findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);
		wv.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //override the default setting of NO_CACHE
		wv.getSettings().setJavaScriptEnabled(true);
		wv.addJavascriptInterface( new JavaScriptInterface(getActivity().getApplicationContext()), interfaceNameForJS );
		wv.setWebViewClient(new DetailWebViewClient());
		
		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				if (progress == 100) {
					
				}
			}
		});
		
		//wv.loadData(Uri.encode(this.cachedPage), "text/html", HTTP.UTF_8);
		wv.loadUrl(requestedURL);
		
		nextPost = (ImageButton) v.findViewById(R.id.down);
		nextPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				wv.loadUrl("javascript:Reader2.show_next_item();");
			}
		});
		
		prevPost = (ImageButton) v.findViewById(R.id.up);
		prevPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				wv.loadUrl("javascript:Reader2.show_prev_item();");
			}
		});
		
		
		return v;
    }
	
	public void updateLoadedItems(String items) {
		readerItems = items;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
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
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (readerItems != null) {
				String method = "Reader2.set_loaded_items(" + readerItems + ")";
				wv.loadUrl("javascript:" + method);
				wv.loadUrl("javascript:Reader2.is_next_item();");
				wv.loadUrl("javascript:Reader2.is_prev_item();");
				nextPost.setEnabled(true);
				prevPost.setEnabled(true);
			}
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			handler.proceed();
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
			HttpAuthHandler handler, String host, String realm) {
			handler.proceed(httpuser, httppassword);
		}
	}
}

