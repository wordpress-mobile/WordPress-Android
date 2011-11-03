package org.wordpress.android;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.wordpress.android.util.EscapeUtils;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class Read extends Activity {
	/** Called when the activity is first created. */
	private XMLRPCClient client;
	public String[] authors;
	public String[] comments;
	private int id;
	private String postID = "";
	private String accountName = "";
	private String httpuser = "";
	private String httppassword = "";
	private boolean loadReader = false;
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
			id = WordPress.currentBlog.getId();
			postID = extras.getString("postID");
			accountName = extras.getString("accountName");
			isPage = extras.getBoolean("isPage");
			loadReader = extras.getBoolean("loadReader");
		}

		if (loadReader) {

			this.setTitle(getResources().getText(R.string.reader));
			wv = (WebView) findViewById(R.id.webView);
			wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
			new loadReaderTask().execute(null, null, null, null);

		} else {
			if (isPage) {
				this.setTitle(EscapeUtils.unescapeHtml(accountName) + " - "
						+ getResources().getText(R.string.preview_page));
			} else {
				this.setTitle(EscapeUtils.unescapeHtml(accountName) + " - "
						+ getResources().getText(R.string.preview_post));
			}

			Thread t = new Thread() {
				public void run() {
					loadPostFromPermalink();
				}
			};
			t.start();
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
			menuItem.setIcon(R.drawable.ic_menu_view);

			menu.add(0, 2, 0, getResources().getText(R.string.refresh));
			menuItem = menu.findItem(2);
			menuItem.setIcon(R.drawable.browser_reload);
		}
		return true;
	}

	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			finish();
			break;
		case 1:

			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(wv.getUrl()));
			startActivity(i);
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
						HttpResponse response = httpclient
								.execute(new HttpGet(
										"http://wordpress.com/reader/mobile/v2"));
						InputStream content = response.getEntity().getContent();
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
		Vector<?> settings = WordPress.wpDB.loadSettings(id);

		String username = settings.get(2).toString();
		String password = settings.get(3).toString();
		httpuser = settings.get(4).toString();
		httppassword = settings.get(5).toString();

		String url = settings.get(0).toString();

		client = new XMLRPCClient(url, httpuser, httppassword);

		Object[] vParams = { postID, username, password };

		Object versionResult = new Object();
		try {
			versionResult = (Object) client.call("metaWeblog.getPost", vParams);
		} catch (XMLRPCException e) {
			// e.printStackTrace();
		}

		String permaLink = null, status = "", html = "";

		if (versionResult != null) {
			try {
				HashMap<?, ?> contentHash = (HashMap<?, ?>) versionResult;
				permaLink = contentHash.get("permaLink").toString();
				status = contentHash.get("post_status").toString();
				html = contentHash.get("description").toString();
			} catch (Exception e) {
			}
		}

		displayResults(permaLink, html, status);
	}

	private void displayResults(final String permaLink, final String html,
			final String status) {
		Thread t = new Thread() {
			public void run() {
				if (permaLink != null) {
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
											.unescapeHtml(accountName)
											+ " - "
											+ getResources().getText(
													R.string.preview_page));
								} else {
									Read.this.setTitle(EscapeUtils
											.unescapeHtml(accountName)
											+ " - "
											+ getResources().getText(
													R.string.preview_post));
								}
							}
						}
					});

					wv.setWebViewClient(new WordPressWebViewClient());
					if (status.equals("publish")) {
						int sdk_int = 0;
						try {
							sdk_int = Integer
									.valueOf(android.os.Build.VERSION.SDK);
						} catch (Exception e1) {
							sdk_int = 3; // assume they are on cupcake
						}
						if (sdk_int >= 8) {
							// only 2.2 devices can load https correctly
							wv.loadUrl(permaLink);
						} else {
							String url = permaLink.replace("https:", "http:");
							wv.loadUrl(url);
						}

					} else {
						wv.loadData(html, "text/html", "utf-8");
						Toast.makeText(Read.this,
								getResources().getText(R.string.basic_html),
								Toast.LENGTH_SHORT).show();
					}
				} else {
					setProgressBarIndeterminateVisibility(false);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							Read.this);
					dialogBuilder.setTitle(getResources().getText(
							R.string.connection_error));
					dialogBuilder.setMessage(getResources().getText(
							R.string.permalink_not_found));
					dialogBuilder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Just close the window.

								}
							});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()) {
						dialogBuilder.create().show();
					}
				}
			}
		};
		this.runOnUiThread(t);

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
						// load stat
						HttpClient httpclient = new DefaultHttpClient();
						HttpProtocolParams.setUserAgent(httpclient.getParams(),
								"wp-android");
						HttpResponse response = httpclient
								.execute(new HttpGet(
										"http://wordpress.com/reader/mobile/?template=stats&stats_name=home_page"));
						InputStream content = response.getEntity().getContent();
					} catch (Exception e) {
						// oh well
					}
				}
			}).start();
		}

		@Override
		protected Vector<?> doInBackground(String... args) {

			Vector<?> settings = WordPress.wpDB.loadSettings(id);
			try {
				String responseContent = "<head>"
						+ "<script type=\"text/javascript\">"
						+ "function submitform(){document.loginform.submit();} </script>"
						+ "</head>"
						+ "<body onload=\"submitform()\">"
						+ "<form style=\"visibility:hidden;\" name=\"loginform\" id=\"loginform\" action=\""
						+ settings.get(0).toString()
								.replace("xmlrpc.php", "wp-login.php")
						+ "\" method=\"post\">"
						+ "<input type=\"text\" name=\"log\" id=\"user_login\" value=\""
						+ settings.get(2).toString()
						+ "\"/></label>"
						+ "<input type=\"password\" name=\"pwd\" id=\"user_pass\" value=\""
						+ settings.get(3).toString()
						+ "\" /></label>"
						+ "<input type=\"submit\" name=\"wp-submit\" id=\"wp-submit\" value=\"Log In\" />"
						+ "<input type=\"hidden\" name=\"redirect_to\" value=\""
						+ "http://wordpress.com/reader/mobile?preload=false"
						+ "\" />" + "</form>" + "</body>";

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
							Read.this.setTitle(getResources().getText(
									R.string.reader));
						}
					}
				});

				wv.getSettings().setUserAgentString("wp-android");
				wv.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
				wv.getSettings().setSavePassword(false);
				wv.getSettings().setBuiltInZoomControls(true);
				wv.getSettings().setJavaScriptEnabled(true);
				wv.getSettings().setPluginsEnabled(true);
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
						&& !wv.getUrl()
								.equals("http://en.wordpress.com/reader/mobile/?preload=false")) {
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
