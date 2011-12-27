package org.wordpress.android;

import java.util.Vector;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.EscapeUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

public class Read extends Activity {
	/** Called when the activity is first created. */
	public String[] authors;
	public String[] comments;
	private String httpuser = "";
	private String httppassword = "";
	private String loginURL = "";
	private boolean loadReader = false;
	private boolean loadAdmin = false;
	private boolean isPage = false;
	ImageButton backButton, forwardButton, refreshButton;
	public ProgressDialog pd;
	private WebView wv;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.reader);

		// setProgressBarIndeterminateVisibility(true);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			loadReader = extras.getBoolean("loadReader");
			loadAdmin = extras.getBoolean("loadAdmin");
		}

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

		if (loadReader || loadAdmin) {

			this.setTitle(getResources().getText(R.string.reader));
			wv = (WebView) findViewById(R.id.webView);
			wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
			new loadReaderTask().execute(null, null, null, null);

		} else {
			if (isPage) {
				this.setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
						.getBlogName())
						+ " - "
						+ getResources().getText(R.string.preview_page));
			} else {
				this.setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
						.getBlogName())
						+ " - "
						+ getResources().getText(R.string.preview_post));
			}

			loadPostFromPermalink();

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (loadReader) {
			menu.add(0, 0, 0, getResources().getText(R.string.home));
			MenuItem menuItem = menu.findItem(0);
			menuItem.setIcon(R.drawable.ic_menu_home);

			menu.add(0, 1, 0, getResources().getText(R.string.view_in_browser));
			menuItem = menu.findItem(1);
			menuItem.setIcon(android.R.drawable.ic_menu_view);

			menu.add(0, 2, 0, getResources().getText(R.string.refresh));
			menuItem = menu.findItem(2);
			menuItem.setIcon(R.drawable.ic_menu_refresh);
		}
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
		case 2:
			wv.reload();
			new Thread(new Runnable() {
				public void run() {
					// refresh stat
					try {
						HttpClient httpclient = new DefaultHttpClient();
						HttpProtocolParams.setUserAgent(httpclient.getParams(),
								"wp-android");
						String readerURL = Constants.readerURL;
						if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
							readerURL += "/?per_page=20";
						}

						httpclient.execute(new HttpGet(readerURL));
					} catch (Exception e) {
						// oh well
					}
				}
			}).start();
			break;
		}

		return false;
	}

	protected void loadPostFromPermalink() {

		WebView wv = (WebView) findViewById(R.id.webView);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		wv.getSettings().setBuiltInZoomControls(true);
		wv.getSettings().setJavaScriptEnabled(true);

		wv.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				Read.this.setTitle("Loading...");
				Read.this.setProgress(progress * 100);

				if (progress == 100) {
					if (isPage) {
						Read.this.setTitle(EscapeUtils
								.unescapeHtml(WordPress.currentBlog
										.getBlogName())
								+ " - "
								+ getResources().getText(R.string.preview_page));
					} else {
						Read.this.setTitle(EscapeUtils
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

	private class WordPressWebViewClient extends WebViewClient {
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

	private class loadReaderTask extends AsyncTask<String, Void, Vector<?>> {

		protected void onPostExecute(Vector<?> result) {
			new Thread(new Runnable() {
				public void run() {
					try {
						HttpClient httpclient = new DefaultHttpClient();
						HttpProtocolParams.setUserAgent(httpclient.getParams(),
								"wp-android");
						String readerURL = Constants.readerURL;
						if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
							readerURL += "/?per_page=20";
						}

						httpclient.execute(new HttpGet(readerURL));
					} catch (Exception e) {
						// oh well
					}
				}
			}).start();
		}

		@Override
		protected Vector<?> doInBackground(String... args) {

			if (WordPress.currentBlog == null) {
				try {
					WordPress.currentBlog = new Blog(
							WordPress.wpDB.getLastBlogID(Read.this), Read.this);
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
			String readerURL = Constants.readerURL;
			if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
				readerURL += "/?per_page=20";
			}
			if (loadAdmin) {
				if (WordPress.currentBlog.getUrl().lastIndexOf("/") != -1)
					readerURL = WordPress.currentBlog.getUrl().substring(0, WordPress.currentBlog.getUrl().lastIndexOf("/")) + "/wp-admin";
				else
					readerURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php", "wp-admin");
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
					public boolean shouldOverrideUrlLoading(WebView view,
							String url) {
						view.loadUrl(url);
						return false;
					}

					@Override
					public void onPageFinished(WebView view, String url) {
					}
				});

				wv.setWebChromeClient(new WebChromeClient() {
					public void onProgressChanged(WebView view, int progress) {
						Read.this.setTitle("Loading...");
						Read.this.setProgress(progress * 100);

						if (progress == 100) {
							if (loadReader)
								Read.this.setTitle(getResources().getText(
										R.string.reader));
							else
								Read.this.setTitle(getResources().getText(
										R.string.wp_admin));
						}
					}
				});

				wv.getSettings().setUserAgentString("wp-android");
				wv.getSettings().setCacheMode(
						WebSettings.LOAD_CACHE_ELSE_NETWORK);
				wv.getSettings().setSavePassword(false);
				wv.getSettings().setBuiltInZoomControls(true);
				wv.getSettings().setJavaScriptEnabled(true);
				wv.getSettings().setPluginsEnabled(true);
				wv.getSettings().setDomStorageEnabled(true);
				wv.loadData(responseContent, "text/html", HTTP.UTF_8);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;

		}

	}

	@Override
	public boolean onKeyDown(int i, KeyEvent event) {

		if (i == KeyEvent.KEYCODE_BACK) {
			if (loadReader) {
				if (wv.canGoBack()
						&& !wv.getUrl().startsWith(Constants.readerURL)
						&& !wv.getUrl().equals(loginURL)) {
					wv.goBack();
				} else {
					finish();
				}
			} else {
				finish();
			}
		}

		return false;
	}

}
