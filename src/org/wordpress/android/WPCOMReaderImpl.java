package org.wordpress.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.wordpress.android.models.Blog;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
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
//	private boolean isPage = false;
	private WebView wv;
	private String topicsID;
	private String cachedTopicsPage = null;
	private String cachedDetailPage = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		setContentView(R.layout.reader_wpcom);

		//Bundle extras = getIntent().getExtras();

		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);
		if (WordPress.currentBlog == null) {
			try {
				WordPress.currentBlog = new Blog(
						WordPress.wpDB.getLastBlogID(this), this);
			} catch (Exception e) {
				Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
				finish();
			}
		}
		
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.topicSelector);
		rl.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(getBaseContext(), WPCOMReaderTopicsSelector.class);
				i.putExtra("currentTopic",WPCOMReaderImpl.this.topicsID);
				if( WPCOMReaderImpl.this.cachedTopicsPage != null )
					i.putExtra("cachedTopicsPage", WPCOMReaderImpl.this.cachedTopicsPage);
				startActivityForResult(i, WPCOMReaderTopicsSelector.activityRequestCode);
			}
		});
		
		Button refreshButton = (Button) findViewById(R.id.action_refresh);
		refreshButton
		.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				startRotatingRefreshIcon();
				wv.reload();
				new Thread(new Runnable() {
					public void run() {
						// refresh stat
						try {
							HttpClient httpclient = new DefaultHttpClient();
							HttpProtocolParams.setUserAgent(httpclient.getParams(),
									"wp-android");
							String readerURL = Constants.readerURL + "/?template=stats&stats_name=home_page_refresh";
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

		//this.setTitle(getResources().getText(R.string.reader)); //FIXME: set the title of the screen here
		wv = (WebView) findViewById(R.id.webView);
		wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		wv.addJavascriptInterface( new JavaScriptInterface(this), interfaceNameForJS );
		this.setDefaultWebViewSettings(wv);
		new loadReaderTask().execute(null, null, null, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, 0, 0, getResources().getText(R.string.home));
		MenuItem menuItem = menu.findItem(0);
		menuItem.setIcon(R.drawable.ic_menu_home);

		menu.add(0, 1, 0, getResources().getText(R.string.view_in_browser));
		menuItem = menu.findItem(1);
		menuItem.setIcon(android.R.drawable.ic_menu_view);
	
		return true;
	}
	
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			finish();
			break;
		case 1:
			if (!wv.getUrl().contains("wp-login.php")) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(wv.getUrl()));
				startActivity(i);
			}
			break;
		default:
			break;
		}
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == WPCOMReaderTopicsSelector.activityRequestCode) { //When the topic is selected
			if (resultCode == RESULT_OK) { 
				//call the JS code to load the topic selected by the user
				Bundle extras = data.getExtras();
				if( extras != null ) {
					String newTopicID = extras.getString("topicID");
					String newTopicName = extras.getString("topicName");
					if (newTopicName != null) {
						TextView topicTV = (TextView) findViewById(R.id.topic_title);
						topicTV.setText(newTopicName);
					}
					if ( topicsID.equalsIgnoreCase( newTopicID )) return; 
					topicsID = newTopicID;
					String methodCall = "Reader2.load_topic('"+topicsID+"')";
					wv.loadUrl("javascript:"+methodCall);
				}
			}
		}
	}

	/*
	protected void loadPostFromPermalink() {

		WebView wv = (WebView) findViewById(R.id.webView);
		this.setDefaultWebViewSettings(wv);

		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				WPCOMReaderImpl.this.setTitle("Loading...");
				WPCOMReaderImpl.this.setProgress(progress * 100);

				if (progress == 100) {
					if (isPage) {
						WPCOMReaderImpl.this.setTitle(EscapeUtils
								.unescapeHtml(WordPress.currentBlog
										.getBlogName())
								+ " - "
								+ getResources().getText(R.string.preview_page));
					} else {
						WPCOMReaderImpl.this.setTitle(EscapeUtils
								.unescapeHtml(WordPress.currentBlog
										.getBlogName())
								+ " - "
								+ getResources().getText(R.string.preview_post));
					}
				}
			}
		});

		wv.setWebViewClient(new WordPressWebViewClient());
		if (WordPress.currentPost != null) {
			int sdk_int = 0;
			try {
				sdk_int = Integer.valueOf(android.os.Build.VERSION.SDK);
			} catch (Exception e1) {
				sdk_int = 3; // assume they are on cupcake
			}
			if (sdk_int >= 8) {
				// only 2.2 devices can load https correctly
				wv.loadUrl(WordPress.currentPost.getPermaLink());
			} else {
				String url = WordPress.currentPost.getPermaLink().replace(
						"https:", "http:");
				wv.loadUrl(url);
			}
		}

	}
*/
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

	//The JS calls this method on first loading
	public void setSelectedTopicFromJS(String topicsID) {
		this.topicsID = topicsID;
	}
	
	public void setTitleFromJS(final String newTopicName) {
		runOnUiThread(new Runnable() {
		     public void run() {
		    	 if (newTopicName != null) {
		    		 TextView topicTV = (TextView) findViewById(R.id.topic_title);
		    		 topicTV.setText(newTopicName);
		    	 }
		    }
		});
	}
	
	private class loadReaderTask extends AsyncTask<String, Void, Vector<?>> {

		@Override 
		protected void onPreExecute() {
			startRotatingRefreshIcon();
		}
		
		protected void onPostExecute(Vector<?> result) {
			
			//Read the WordPress.com cookies from the wv and pass them to the connections below!
			CookieManager cookieManager = CookieManager.getInstance();
			final String cookie = cookieManager.getCookie("wordpress.com");
			stopRotatingRefreshIcon();
      	
			new Thread(new Runnable() {
				public void run() {
					try {
						HttpClient httpclient = new DefaultHttpClient();
						HttpProtocolParams.setUserAgent(httpclient.getParams(),	"wp-android");
						
						String readerURL = Constants.readerURL + "/?template=stats&stats_name=home_page";
						HttpGet httpGet = new HttpGet(readerURL);
						httpGet.setHeader("Cookie", cookie);
						httpclient.execute(httpGet);

						//Cache the Topics page
						String hybURL = WPCOMReaderImpl.this.getAuthorizeHybridURL(Constants.readerTopicsURL);
   	    		        WPCOMReaderImpl.this.cachedTopicsPage = cachePage(hybURL, cookie);
   	    		        
						//Cache the DAtil page
						hybURL = WPCOMReaderImpl.this.getAuthorizeHybridURL(Constants.readerDetailURL);
   	    		        WPCOMReaderImpl.this.cachedDetailPage = cachePage(hybURL, cookie);
						
					} catch (Exception e) {
						// oh well
						e.printStackTrace();
					}
				}
			}).start();
		}

		private String cachePage(String hybURL, String cookie) {
			HttpClient httpclient = new DefaultHttpClient();

			try {
				HttpProtocolParams.setUserAgent(httpclient.getParams(), "wp-android");
				HttpGet request = new HttpGet(hybURL);
				request.setHeader("Cookie", cookie);
				HttpResponse response = httpclient.execute(request);

				// Check if server response is valid
				StatusLine status = response.getStatusLine();
				if (status.getStatusCode() != 200) {
					throw new IOException("Invalid response from server when caching the page: " + status.toString());
				}

				// Pull content stream from response
				HttpEntity entity = response.getEntity();
				InputStream inputStream = (InputStream) entity.getContent();

				ByteArrayOutputStream content = new ByteArrayOutputStream();

				// Read response into a buffered stream
				int readBytes = 0;
				byte[] sBuffer = new byte[512];
				while ((readBytes = inputStream.read(sBuffer)) != -1) {
					content.write(sBuffer, 0, readBytes);
				}
				// Return result from buffered stream
				String dataAsString = new String(content.toByteArray());
				return dataAsString;
			} catch (Exception e) {
				// oh well
				Log.d("Error while caching the page" + hybURL, e.getLocalizedMessage());
				return null;

			} finally {
				// When HttpClient instance is no longer needed,
				// shut down the connection manager to ensure
				// immediate deallocation of all system resources
				httpclient.getConnectionManager().shutdown();
			}
		}

		
		@Override
		protected Vector<?> doInBackground(String... args) {

			if (WordPress.currentBlog == null) {
				try {
					WordPress.currentBlog = new Blog(
							WordPress.wpDB.getLastBlogID(WPCOMReaderImpl.this), WPCOMReaderImpl.this);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			loginURL = WordPress.currentBlog.getUrl()
					.replace("xmlrpc.php", "wp-login.php");
			if (WordPress.currentBlog.getUrl().lastIndexOf("/") != -1)
				loginURL = WordPress.currentBlog.getUrl().substring(0, WordPress.currentBlog.getUrl().lastIndexOf("/")) + "/wp-login.php";
			else
				loginURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php", "wp-login.php");
			
			String readerURL = WPCOMReaderImpl.this.getAuthorizeHybridURL(Constants.readerURL_v3);
		
			if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
				if( readerURL.contains("?") )
					readerURL += "&per_page=20";
				else 
					readerURL += "?per_page=20";
			}
			
			try {
				String responseContent = "<head>"
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

				wv.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						if( url.equalsIgnoreCase( Constants.readerDetailURL ) ) {
							Intent i = new Intent(getBaseContext(), WPCOMReaderDetailPage.class);
							i.putExtra("requestedURL", url);
							i.putExtra("cachedPage", WPCOMReaderImpl.this.cachedDetailPage);
							startActivity(i);
							return true;
						}
						view.loadUrl(url);
						return false;
					}
					
					@Override
					public void onPageFinished(WebView view, String url) {
					}
				});


				wv.setWebChromeClient(new WebChromeClient() {
					public void onProgressChanged(WebView view, int progress) {
						WPCOMReaderImpl.this.setTitle("Loading...");
						//WPCOMReaderImpl.this.setProgress(progress * 100);

						if (progress == 100) {
							WPCOMReaderImpl.this.setTitle(getResources().getText(R.string.reader));
						}
					}
				});
		
				wv.loadData(Uri.encode(responseContent), "text/html", HTTP.UTF_8);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;

		}

	}

	@Override
	public boolean onKeyDown(int i, KeyEvent event) {

		if (i == KeyEvent.KEYCODE_BACK) {
			if (wv.canGoBack()
					&& !wv.getUrl().startsWith(Constants.readerURL)
					&& !wv.getUrl().equals(loginURL)) {
				wv.goBack();
			} else {
				finish();
			}
		}

		return false;
	}
	
	public void startRotatingRefreshIcon() {

		RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		anim.setInterpolator(new LinearInterpolator());
		anim.setRepeatCount(Animation.INFINITE);
		anim.setDuration(1400);
		ImageView iv = (ImageView) findViewById(R.id.refresh_icon);
		iv.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_refresh_active));
		iv.startAnimation(anim);
	}

	public void stopRotatingRefreshIcon() {
		ImageView iv = (ImageView) findViewById(R.id.refresh_icon);
		iv.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_refresh));
		iv.clearAnimation();
	}
	
}
