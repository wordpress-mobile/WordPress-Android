/**
 * 
 */
package org.wordpress.android;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.WPTitleBar;
import org.wordpress.android.util.WPTitleBar.OnBlogChangedListener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ConnectionClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Eric
 *
 */
public class ViewWebStats extends Activity {

	static String lastAuthedName = "";
	
	private ConnectionClient client;
	private HttpPost postMethod;
	private HttpParams httpParams;
	protected String errorMsg = "";
	
	private WPTitleBar titleBar;
	private WebView webView;
	boolean loginShowing = false;
	boolean authed = false;
	boolean isRetrying = false;
	private AsyncTask<String, Void, Vector<?>> currentTask = null;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.view_web_stats);
				
		webView = (WebView) findViewById(R.id.webView);
		webView.setWebViewClient(new StatsWebViewClient());
		WebSettings webSettings = webView.getSettings();
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setSavePassword(false);
		clearCookies();
		webView.clearCache(false);
		
		titleBar = (WPTitleBar) findViewById(R.id.actionBar);
		titleBar.refreshButton.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				reloadStats();
			}
		});
		titleBar.setOnBlogChangedListener(new OnBlogChangedListener() {
			// user selected new blog in the title bar
			@Override
			public void OnBlogChanged() {
				// stop any asyncTask that might be running.
				if (currentTask != null) {
					currentTask.cancel(true);
					currentTask = null;
				}
				authed = false;
				isRetrying = false;
				hideLoginForm();
				// hide the view 
				webView.clearView();
				webView.setVisibility(View.INVISIBLE);
				webView.clearHistory();
				webView.clearCache(false);
				clearCookies();
				initStats();
			}
		});
		
		Button saveStatsLogin = (Button) findViewById(R.id.saveDotcom);
		saveStatsLogin.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
				EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

				String dcUsername = dotcomUsername.getText().toString();
				String dcPassword = dotcomPassword.getText().toString();

				if (dcUsername.equals("") || dcPassword.equals("")) {
					showErrorDialog(
						getResources().getText(R.string.required_fields).toString(),
						getResources().getText(R.string.username_password_required).toString()
					);

				} else {
					WordPress.currentBlog.setDotcom_username(dcUsername);
					WordPress.currentBlog.setDotcom_password(dcPassword);
					WordPress.currentBlog.save(ViewWebStats.this, WordPress.currentBlog.getUsername());
					hideLoginForm();
					
					initStats(); // start over again now that we have the login
				}
			}
		});

		TextView wpcomHelp = (TextView) findViewById(R.id.wpcomHelp);
		wpcomHelp.setOnClickListener(new TextView.OnClickListener() {
			public void onClick(View v) {

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("http://jetpack.me/about"));
				startActivity(intent);

			}
		});
		
		CookieSyncManager.createInstance(this);

		this.initStats();
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		CookieSyncManager.getInstance().startSync();
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		CookieSyncManager.getInstance().stopSync();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		webView.clearCache(false);
	}
	
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		titleBar.refreshBlog();
	}


	private void initStats() {
		titleBar.startRotatingRefreshIcon();
		Blog blog = WordPress.currentBlog;
		
		int blogid = blog.getBlogId();
		
		if (blogid == 1 && blog.getApi_blogid() == null) {
			// first run or was deleted.
			this.checkAPIBlogInfo();
		} else if(!blog.isDotcomFlag() && blog.getDotcom_username() == null) {
			// .org blog with no corresponding .com jetpack login.
			this.showLoginForm();
		} else {
			this.loadStats();
		}
	}
	
	private void checkAPIBlogInfo() {
		String sUsername, sPassword;
		Blog blog = WordPress.currentBlog;
		
		if (blog.isDotcomFlag()) {
			sUsername = blog.getUsername();
			sPassword = blog.getPassword();
		} else {
			// we have an alternate login, use that instead
			sUsername = blog.getDotcom_username();
			sPassword = blog.getDotcom_password();
		}
		
		titleBar.startRotatingRefreshIcon();
		
		// Start an async task to retrieve the blog's data from the api.
		currentTask = new StatsAPIBlogInfoAsyncTask().execute(sUsername, sPassword);
	}
	
	private void clearCookies() {
		//get rid of old auth cookie
		CookieSyncManager.createInstance(ViewWebStats.this); 
	    CookieManager cookieManager = CookieManager.getInstance();
	    cookieManager.removeAllCookie();
	}
	
	protected void authStats() {
		String sUsername, sPassword;
		Blog blog = WordPress.currentBlog;
		
		if (blog.isDotcomFlag()) {
			sUsername = blog.getUsername();
			sPassword = blog.getPassword();
		} else {
			// we have an alternate login, use that instead
			sUsername = blog.getDotcom_username();
			sPassword = blog.getDotcom_password();
		}
		
		if(lastAuthedName.equals(sUsername)) {
			// Check for a valid auth cookie for this username.
			CookieManager cookieManager = CookieManager.getInstance();
			if(cookieManager.hasCookies()){
				String rawCookieString = cookieManager.getCookie("wordpress.com");
				if (rawCookieString != null && rawCookieString.length() > 0) {
					rawCookieString = rawCookieString.toLowerCase();
					Log.d("cookeis", rawCookieString);
					String[] rawCookies = rawCookieString.split(";");
				    String[] rawCookieNameAndValue = rawCookies[0].split("=");
				    String val = rawCookieNameAndValue[1].trim();
					if (val.indexOf(sUsername.toLowerCase()) != -1) {
						authed = true;
						this.loadStats();
						return;
					}
				}
			}
		}
		
		currentTask = new AuthStatsAsyncTask().execute(sUsername, sPassword);
	}
	
	
	protected void loadStats() {
		if (!authed) {
			this.authStats();
			return;
		}
		
		Blog blog = WordPress.currentBlog;
		String id = ""; 
		if(blog.isDotcomFlag()) {
			id = Integer.toString( WordPress.currentBlog.getBlogId() );
		} else {
			id = blog.getApi_blogid();
		}
		
		webView.setVisibility(View.VISIBLE);
		String path = "http://wordpress.com/?no-chrome#!/my-stats/?unit=1&blog=" + id;
		webView.loadUrl(path);
		
		// Clear the history here so in a case where the user has changed blogs via the titlebar, 
		// tapping the back button will not try to load the previous blog's stats.
		webView.clearHistory();
	}
	
	
	public void reloadStats() {
		webView.reload();
	}
	
	
	private void configureClient(URI uri, String username, String password) {
		postMethod = new HttpPost(uri);

		postMethod.addHeader("charset", "UTF-8");
		postMethod.addHeader("User-Agent", "wp-android/" + WordPress.versionName);

		httpParams = postMethod.getParams();
		HttpProtocolParams.setUseExpectContinue(httpParams, false);
		UsernamePasswordCredentials creds;
		// username & password for basic http auth
		if (username != null) {
			creds = new UsernamePasswordCredentials(username, password);
		} else {
			creds = new UsernamePasswordCredentials("", "");
		}

		// this gets connections working over https
		if (uri.getScheme() != null) {
			if (uri.getScheme().equals("https")) {
				if (uri.getPort() == -1)
					try {
						client = new ConnectionClient(creds, 443);
					} catch (KeyManagementException e) {
						client = new ConnectionClient(creds);
					} catch (NoSuchAlgorithmException e) {
						client = new ConnectionClient(creds);
					} catch (KeyStoreException e) {
						client = new ConnectionClient(creds);
					} catch (UnrecoverableKeyException e) {
						client = new ConnectionClient(creds);
					}
				else
					try {
						client = new ConnectionClient(creds, uri.getPort());
					} catch (KeyManagementException e) {
						client = new ConnectionClient(creds);
					} catch (NoSuchAlgorithmException e) {
						client = new ConnectionClient(creds);
					} catch (KeyStoreException e) {
						client = new ConnectionClient(creds);
					} catch (UnrecoverableKeyException e) {
						client = new ConnectionClient(creds);
					}
			} else {
				client = new ConnectionClient(creds);
			}
		} else {
			client = new ConnectionClient(creds);
		}
	}

	
	public void showLoginForm() {
		if (loginShowing) return;
		loginShowing = true;
		
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);
		moderationBar.setVisibility(View.VISIBLE);
		moderationBar.startAnimation(set);
	}
	
	
	public void hideLoginForm() {
		if(!loginShowing) return;
		loginShowing = false;
		
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		;
		RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);
		moderationBar.clearAnimation();
		moderationBar.startAnimation(set);
		moderationBar.setVisibility(View.INVISIBLE);
		
	}
	
	
	public void showErrorDialog(String title, String msg) {
		if(isFinishing()) return;
		
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ViewWebStats.this);
		dialogBuilder.setTitle(title);
		dialogBuilder.setMessage(msg);
		dialogBuilder.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Just close the window.
					}
				});
		dialogBuilder.setCancelable(true);
		dialogBuilder.create().show();
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) { 
			if (titleBar.isShowingDashboard) {
				titleBar.hideDashboardOverlay();
				return false;
			}
		    if ( webView.canGoBack() ) {
		        webView.goBack();
		        return true;
		    }
		}
	    return super.onKeyDown(keyCode, event);
	}
	
	
	/*
	 * 
	 */
	protected class StatsWebViewClient extends WebViewClient {
	
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//Log.d("WP", url);
			titleBar.startRotatingRefreshIcon();
		}

		
		@Override
		public void onPageFinished(WebView view, String url) {
			if (authed) {
				// The webview loads an empty string during init/auth. We don't want to stop the refresh icon in this case.
				titleBar.stopRotatingRefreshIcon();
			}
		}

		
		@Override
		public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
			titleBar.stopRotatingRefreshIcon();			
		}
		
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (!url.equalsIgnoreCase(Constants.readerDetailURL)) {
				
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				startActivity(intent);
				
				return true;
			}
			return false;
		}
	}
	
	
	/*
	 * Call to authenticate so we can display stats.  If auth fails prompt
	 * for updated wp.com credentials.
	 */
	private class AuthStatsAsyncTask extends AsyncTask<String, Void, Vector<?>> {
		
		int statusCode = 0;
		
		@Override
		protected Vector<?> doInBackground(String... args) {
			Vector<String> result = null;
			
			String sUsername = args[0];
			String sPassword = args[1];
			try {
				
				URI uri = URI.create("https://wordpress.com/wp-login.php");
				configureClient(uri, sUsername, sPassword);
							
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		        nameValuePairs.add(new BasicNameValuePair("log", sUsername));
		        nameValuePairs.add(new BasicNameValuePair("pwd", sPassword));
		        nameValuePairs.add(new BasicNameValuePair("rememberme", "forever"));
		        nameValuePairs.add(new BasicNameValuePair("wp-submit", "Log In"));
		        nameValuePairs.add(new BasicNameValuePair("redirect_to", "/"));
				postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
				
				HttpResponse response = client.execute(postMethod);
				
				statusCode = response.getStatusLine().getStatusCode();

				if (statusCode != HttpStatus.SC_OK) {
					throw new IOException("HTTP status code: " + statusCode
							+ " was returned. "
							+ response.getStatusLine().getReasonPhrase());
				}
				
				List<Cookie> cookies = client.getCookieStore().getCookies();
				if(!cookies.isEmpty()) {

					CookieManager cookieManager = CookieManager.getInstance();

					for (Cookie cookie : cookies){
						if (cookie.getName().equalsIgnoreCase("wordpress_logged_in")) {
							// Auth cookie found so mark the parent class authed and set the lastAuthedName
							authed = true;
							lastAuthedName = sUsername;
						}
						
						String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
						cookieManager.setCookie("wordpress.com", cookieString);
						CookieSyncManager.getInstance().sync();

					}
				}
				
				// Uncomment to review the html returned from the auth call. 
//				HttpEntity entity = response.getEntity();
//				InputStream is = entity.getContent();
//				InputStreamReader isr = new InputStreamReader(is);
//				BufferedReader br = new BufferedReader(isr);
//				
//				StringBuffer sb = new StringBuffer("");
//				String line = "";
//				String NL = System.getProperty("line.separator");
//				while((line = br.readLine()) != null){
//					sb.append(line + NL);
//				}
//				br.close();
//				
//				String res = sb.toString();
//				Log.d("AuthStatsAsyncTask", res);

			} catch (Exception e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
			}
			
			return result;
		}
		
		
		protected void onPostExecute(Vector<?> result) {
			currentTask = null;
			titleBar.stopRotatingRefreshIcon();
			if(authed) {
				loadStats();
			} else {
				// if we're here and not authed then there was either a server error
				// or the auth cookie was not set.
				if(errorMsg == "" || statusCode == 401){
					// Either there wasn't an error and the auth cookie wasn't set, or 
					// we received a 401 error.
					Toast.makeText(
							ViewWebStats.this, 
							getResources().getText(R.string.invalid_login),
							Toast.LENGTH_SHORT).show();
					showLoginForm();
				} else {
					// server error of some kind.
					Toast.makeText(
							ViewWebStats.this, 
							getResources().getText(R.string.stats_service_error),
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
	
	/*
	 * AsyncTask for retrieving a blog's key and id from the API 
	 */
	private class StatsAPIBlogInfoAsyncTask extends AsyncTask<String, Void, Vector<?>> {
		
		int statusCode = 0;
		
		@Override
		protected Vector<?> doInBackground(String... args) {

			String username = args[0];
			String password = args[1];
			String url = WordPress.currentBlog.getUrl();
			String homeURL = WordPress.currentBlog.getHomeURL();
			String storedBlogID = String.valueOf(WordPress.currentBlog.getBlogId());
			String wwwURL = "";						
			Vector<String> apiInfo = null;

			if (homeURL.equals("")) {
				//get the 'homePageLink' url from RSD to match with the stats api
				String homePageLink = ApiHelper.getXMLRPCUrl(url + "?rsd", true);
				if (homePageLink != null) {
					url = homePageLink;
					//home url was added in 2.2.2, it may need to be set if the user upgraded
					WordPress.currentBlog.setHomeURL(url);
					WordPress.currentBlog.save(ViewWebStats.this, "");
				} else {
					url = url.replace("xmlrpc.php", "");
				}
			} else {
				url = homeURL;
			}
			
			url = url.replace("https://", "http://");

			if (url.indexOf("http://www.") >= 0) {
				wwwURL = url;
				url = url.replace("http://www.", "http://");
			} else {
				wwwURL = url.replace("http://", "http://www.");
			}

			if (!url.endsWith("/")) {
				url += "/";
				wwwURL += "/";
			}

			URI uri = URI.create("https://public-api.wordpress.com/get-user-blogs/1.0");
			configureClient(uri, username, password);

			// execute HTTP POST request
			HttpResponse response;
			try {
				response = client.execute(postMethod);
				/*
				 * ByteArrayOutputStream outstream = new ByteArrayOutputStream();
				 * response.getEntity().writeTo(outstream); String text =
				 * outstream.toString(); Log.i("WordPress", text);
				 */
				// check status code
				this.statusCode = response.getStatusLine().getStatusCode();

				if (statusCode != HttpStatus.SC_OK) {
					
					throw new IOException("HTTP status code: " + statusCode
							+ " was returned. "
							+ response.getStatusLine().getReasonPhrase());
				}

				// setup pull parser
				try {
					XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
					HttpEntity entity = response.getEntity();
					// change to pushbackinput stream 1/18/2010 to handle self
					// installed wp sites that insert the BOM
					PushbackInputStream is = new PushbackInputStream(entity.getContent());

					// get rid of junk characters before xml response. 60 = '<'.
					// Added stopper to prevent infinite loop
					int bomCheck = is.read();
					int stopper = 0;
					while (bomCheck != 60 && stopper < 20) {
						bomCheck = is.read();
						stopper++;
					}
					is.unread(bomCheck);

					pullParser.setInput(is, "UTF-8");

					int eventType = pullParser.getEventType();
					String apiKey = "";
					boolean foundKey = false;
					boolean foundID = false;
					boolean foundURL = false;
					String curBlogID = "";
					String curBlogURL = "";
					while (eventType != XmlPullParser.END_DOCUMENT) {
						if (eventType == XmlPullParser.START_DOCUMENT) {
							// System.out.println("Start document");
						} else if (eventType == XmlPullParser.END_DOCUMENT) {
							// System.out.println("End document");
						} else if (eventType == XmlPullParser.START_TAG) {
							if (pullParser.getName().equals("apikey")) {
								foundKey = true;
							} else if (pullParser.getName().equals("id")) {
								foundID = true;
							} else if (pullParser.getName().equals("url")) {
								foundURL = true;
							}
						} else if (eventType == XmlPullParser.END_TAG) {
							// System.out.println("End tag "+pullParser.getName());
						} else if (eventType == XmlPullParser.TEXT) {
							// System.out.println("Text "+pullParser.getText().toString());
							if (foundKey) {
								apiKey = pullParser.getText();
								foundKey = false;
							} else if (foundID) {
								curBlogID = pullParser.getText();
								foundID = false;
							} else if (foundURL) {
								curBlogURL = pullParser.getText();
								foundURL = false;

								// make sure we're matching with a '/' at the end of
								// the string, the api returns both with and w/o
								if (!curBlogURL.endsWith("/"))
									curBlogURL += "/";

								if (((curBlogURL.equals(url) || (curBlogURL.equals(wwwURL))) || storedBlogID.equals(curBlogID)) && !curBlogID.equals("1")) {
									// yay, found a match
									apiInfo = new Vector<String>();
									apiInfo.add(apiKey);
									apiInfo.add(curBlogID);
								}
							}
						}
						eventType = pullParser.next();
					}

				} catch (XmlPullParserException e) {
					e.printStackTrace();
					errorMsg = e.getMessage();
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
			}

			return apiInfo;
		}
		
		
		protected void onPostExecute(Vector<?> result) {
			currentTask = null;
			titleBar.stopRotatingRefreshIcon();
			if (result != null) {
				// store the api key and blog id
				final String apiKey = result.get(0).toString();
				final String apiBlogID = result.get(1).toString();
				WordPress.currentBlog.setApi_blogid(apiBlogID);
				WordPress.currentBlog.setApi_key(apiKey);
				WordPress.currentBlog.save(ViewWebStats.this, "");
				
				if (!isFinishing())
					authStats();				
				
			} else {
				// Either there was a server error, an auth error, 
				// or the blog could not be found among the list of blogs
				// returned by the API.
				if (isRetrying) {
					if (errorMsg.equals("") || statusCode == 401) {
					
						Toast.makeText(
								ViewWebStats.this,
								getResources().getText(R.string.invalid_jp_login),
								Toast.LENGTH_SHORT).show();
					
					} else {
					
						Toast.makeText(
								ViewWebStats.this,
								getResources().getText(R.string.invalid_login)
										+ " "
										+ getResources().getText(
												R.string.site_not_found),
								Toast.LENGTH_SHORT).show();
					
						errorMsg = "";
						showErrorDialog(
								getResources().getText(R.string.connection_error).toString(), 
								getResources().getText(R.string.connection_error_occured).toString()
						);
					}
				}
				
				showLoginForm();
				isRetrying = true;
			}
		}
	}
}

