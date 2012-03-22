package org.wordpress.android;

import java.util.Vector;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.wordpress.android.models.Blog;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WPCOMReaderImpl extends WPCOMReaderBase {
	/** Called when the activity is first created. */
	private String loginURL = "";
	public WebView wv;
	public String topicsID;
	private PostSelectedListener onPostSelectedListener;
	private ShowTopicsListener showTopicsListener;
	public TextView topicTV;
	private ImageView refreshIcon;
	
	public static WPCOMReaderImpl newInstance() {
		WPCOMReaderImpl f = new WPCOMReaderImpl();
        return f;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.reader_wpcom, container, false);
		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(getActivity()
					.getApplicationContext());
		if (WordPress.currentBlog == null) {
			try {
				WordPress.currentBlog = new Blog(
						WordPress.wpDB.getLastBlogID(getActivity()
								.getApplicationContext()), getActivity()
								.getApplicationContext());
			} catch (Exception e) {
				Toast.makeText(getActivity().getApplicationContext(),
						getResources().getText(R.string.blog_not_found),
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}
		}

		topicTV = (TextView) v.findViewById(R.id.topic_title);
		refreshIcon = (ImageView) v.findViewById(R.id.refresh_icon);

		// this.setTitle(getResources().getText(R.string.reader)); //FIXME: set
		// the title of the screen here
		wv = (WebView) v.findViewById(R.id.webView);
		wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		wv.addJavascriptInterface(new JavaScriptInterface(getActivity()
				.getApplicationContext()), interfaceNameForJS);

		wv.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (url.equalsIgnoreCase(Constants.readerDetailURL)) {
					view.stopLoading();
					wv.loadUrl("javascript:Reader2.get_loaded_items();");
					wv.loadUrl("javascript:Reader2.get_last_selected_item();");
					onPostSelectedListener.onPostSelected(url);
				} else {
					startRotatingRefreshIcon();
				}

			}

			@Override
			public void onPageFinished(WebView view, String url) {
				stopRotatingRefreshIcon();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				/*
				 * if (url.equalsIgnoreCase(Constants.readerDetailURL)) {
				 * onPostSelectedListener.onPostSelected(url,
				 * WPCOMReaderImpl.this.cachedDetailPage); return true; }
				 */
				return false;
			}
		});

		this.setDefaultWebViewSettings(wv);
		new loadReaderTask().execute(null, null, null, null);

		RelativeLayout rl = (RelativeLayout) v.findViewById(R.id.topicSelector);
		rl.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showTopicsListener.showTopics();
			}
		});

		Button refreshButton = (Button) v.findViewById(R.id.action_refresh);
		refreshButton.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				startRotatingRefreshIcon();
				wv.reload();
				new Thread(new Runnable() {
					public void run() {
						// refresh stat
						try {
							HttpClient httpclient = new DefaultHttpClient();
							HttpProtocolParams.setUserAgent(
									httpclient.getParams(), "wp-android");
							String readerURL = Constants.readerURL
									+ "/?template=stats&stats_name=home_page_refresh";
							if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
								readerURL += "&per_page=20";
							}

							httpclient.execute(new HttpGet(readerURL));
						} catch (Exception e) {
							// oh well
						}
					}
				}).start();

			}
		});

		return v;
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			onPostSelectedListener = (PostSelectedListener) activity;
			showTopicsListener = (ShowTopicsListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (wv != null) {
			wv.stopLoading();
		}
	}

	private class loadReaderTask extends AsyncTask<String, Void, Vector<?>> {

		@Override
		protected void onPreExecute() {
			startRotatingRefreshIcon();
		}

		protected void onPostExecute(Vector<?> result) {

			// Read the WordPress.com cookies from the wv and pass them to the
			// connections below!
			CookieManager cookieManager = CookieManager.getInstance();
			final String cookie = cookieManager.getCookie("wordpress.com");

			new Thread(new Runnable() {
				public void run() {
					try {
						HttpClient httpclient = new DefaultHttpClient();
						HttpProtocolParams.setUserAgent(httpclient.getParams(),
								"wp-android");

						String readerURL = Constants.readerURL
								+ "/?template=stats&stats_name=home_page";
						HttpGet httpGet = new HttpGet(readerURL);
						httpGet.setHeader("Cookie", cookie);
						httpclient.execute(httpGet);

					} catch (Exception e) {
						// oh well
						e.printStackTrace();
					}
				}
			}).start();
		}

		@Override
		protected Vector<?> doInBackground(String... args) {

			if (WordPress.currentBlog == null) {
				try {
					WordPress.currentBlog = new Blog(
							WordPress.wpDB.getLastBlogID(getActivity()
									.getApplicationContext()), getActivity()
									.getApplicationContext());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			loginURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php",
					"wp-login.php");
			if (WordPress.currentBlog.getUrl().lastIndexOf("/") != -1)
				loginURL = WordPress.currentBlog.getUrl().substring(0,
						WordPress.currentBlog.getUrl().lastIndexOf("/"))
						+ "/wp-login.php";
			else
				loginURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php",
						"wp-login.php");

			String readerURL = WPCOMReaderImpl.this
					.getAuthorizeHybridURL(Constants.readerURL_v3);

			if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
				if (readerURL.contains("?"))
					readerURL += "&per_page=20";
				else
					readerURL += "?per_page=20";
			}

			try {
				final String responseContent = "<head>"
						+ "<script type=\"text/javascript\">"
						+ "function submitform(){document.loginform.submit();} </script>"
						+ "</head>"
						+ "<body onload=\"submitform()\">"
						+ "<form style=\"visibility:hidden;\" name=\"loginform\" id=\"loginform\" action=\""
						+ loginURL
						+ "\" method=\"post\">"
						+ "<input type=\"text\" name=\"log\" id=\"user_login\" value=\""
						+ WordPress.currentBlog.getUsername()
						+ "\"/></label>"
						+ "<input type=\"password\" name=\"pwd\" id=\"user_pass\" value=\""
						+ WordPress.currentBlog.getPassword()
						+ "\" /></label>"
						+ "<input type=\"submit\" name=\"wp-submit\" id=\"wp-submit\" value=\"Log In\" />"
						+ "<input type=\"hidden\" name=\"redirect_to\" value=\""
						+ readerURL + "\" />" + "</form>" + "</body>";

				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						wv.loadData(Uri.encode(responseContent), "text/html",
								HTTP.UTF_8);
					}
				});

			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;

		}

	}

	public void startRotatingRefreshIcon() {

		RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		anim.setInterpolator(new LinearInterpolator());
		anim.setRepeatCount(Animation.INFINITE);
		anim.setDuration(1400);
		refreshIcon.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_refresh_active));
		refreshIcon.startAnimation(anim);
	}

	public void stopRotatingRefreshIcon() {
		refreshIcon.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_refresh));
		refreshIcon.clearAnimation();
	}

	public interface ChangePageListener {
		public void onChangePage(int position);
	}

	public interface PostSelectedListener {
		public void onPostSelected(String requestedURL);
	}

	public interface ShowTopicsListener {
		public void showTopics();
	}

}
