package org.wordpress.android;

/**
 * In order to make requests for WPCOM stats for a given blog you need some specific information. 
 * In order to get this additional info a new URL was created:

 https://public-api.wordpress.com/getuserblogs.php
 http://stats.wordpress.com/csv.php

 It requires SSL, uses HTTP BASIC AUTHENTICATION and returns XML data
 that looks like:

 <?xml version="1.0" encoding="UTF-8"?>
 <userinfo>
 <apikey>XXXXXXXXXXX</apikey>
 <blog>
 <id>XYXYXY</id>
 <url>http://example.wordpress.com/</url>
 </blog>
 <blog>
 <id>XYXYXY</id>
 <url>http://myblog.example.com/</url>
 </blog>
 </userinfo>

 Optionally you can get the data in JSON format -
 https://public-api.wordpress.com/getuserblogs.php?f=json
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.wordpress.android.util.WPTitleBar;
import org.wordpress.android.util.WPTitleBar.OnBlogChangedListener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.ConnectionClient;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class ViewStats extends Activity {
	public boolean success = false;
	public String blogURL, xmlrpcURL, vsoURI;
	private ConnectionClient client;
	private HttpPost postMethod;
	private HttpParams httpParams;
	String errorMsg = "";
	boolean loginShowing = false;
	private int firstRun = 0;
	private WPTitleBar titleBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.view_stats);
		titleBar = (WPTitleBar) findViewById(R.id.actionBar);
		// get the ball rolling...
		initStats();

		Button saveStatsLogin = (Button) findViewById(R.id.saveDotcom);

		saveStatsLogin.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
				EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

				String dcUsername = dotcomUsername.getText().toString();
				String dcPassword = dotcomPassword.getText().toString();

				if (dcUsername.equals("") || dcPassword.equals("")) {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							ViewStats.this);
					dialogBuilder.setTitle(getResources().getText(
							R.string.required_fields));
					dialogBuilder.setMessage(getResources().getText(
							R.string.username_password_required));
					dialogBuilder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// just close the dialog

								}
							});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()) {
						dialogBuilder.create().show();
					}
				} else {
					WordPress.currentBlog.setDotcom_username(dcUsername);
					WordPress.currentBlog.setDotcom_password(dcPassword);
					WordPress.currentBlog.save(ViewStats.this,
							WordPress.currentBlog.getUsername());
					showOrHideLoginForm();
					initStats(); // start over again now that we have the login
				}
			}
		});

		final Button go = (Button) findViewById(R.id.go);

		go.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				Spinner reportType = (Spinner) findViewById(R.id.reportType);
				Spinner reportInterval = (Spinner) findViewById(R.id.reportInterval);

				final String type = parseType(reportType
						.getSelectedItemPosition());
				final int interval = parseInterval(reportInterval
						.getSelectedItemPosition());

				final String apiKey = WordPress.currentBlog.getApi_key();
				final String apiBlogID = WordPress.currentBlog.getApi_blogid();
				if (!isFinishing())
					titleBar.startRotatingRefreshIcon();
				new getStatsDataTask().execute(apiKey, apiBlogID, type,
						interval);

			}
		});

		titleBar.refreshButton
				.setOnClickListener(new ImageButton.OnClickListener() {
					public void onClick(View v) {

						go.performClick();
					}
				});

		titleBar.setOnBlogChangedListener(new OnBlogChangedListener() {
			// user selected new blog in the title bar
			@Override
			public void OnBlogChanged() {

				// hide all of the report views
				ImageView iv = (ImageView) findViewById(R.id.chart);
				iv.setVisibility(View.GONE);
				RelativeLayout filters = (RelativeLayout) findViewById(R.id.filters);
				filters.setVisibility(View.GONE);
				TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
				tl.removeAllViews();
				RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);
				moderationBar.setVisibility(View.GONE);
				TextView reportTitle = (TextView) findViewById(R.id.chartTitle);
				reportTitle.setVisibility(View.GONE);

				// load stats again for the new blog
				initStats();
			}
		});

		TextView wpcomHelp = (TextView) findViewById(R.id.wpcomHelp);
		wpcomHelp.setOnClickListener(new TextView.OnClickListener() {
			public void onClick(View v) {

				Intent intent1 = new Intent(Intent.ACTION_VIEW);
				intent1.setData(Uri
						.parse("http://en.support.wordpress.com/stats/"));
				startActivity(intent1);

			}
		});

		Spinner reportInterval = (Spinner) findViewById(R.id.reportInterval);
		reportInterval.setSelection(1);

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		titleBar.refreshBlog();

		initStats();

	}

	protected int parseInterval(int position) {
		int interval = 0;
		switch (position) {

		case 0:
			interval = 1;
			break;
		case 1:
			interval = 7;
			break;
		case 2:
			interval = 30;
			break;
		case 3:
			interval = 90;
			break;
		case 4:
			interval = 365;
			break;
		case 5:
			interval = -1;
			break;
		}
		;

		return interval;
	}

	protected String parseType(int position) {
		String type = "";
		switch (position) {

		case 0:
			type = "views";
			break;
		case 1:
			type = "postviews";
			break;
		case 2:
			type = "referrers";
			break;
		case 3:
			type = "searchterms";
			break;
		case 4:
			type = "clicks";
			break;
		case 5:
			type = "videoplays";
			break;
		}
		;

		return type;
	}

	private void initStats() {
		String sUsername, sPassword;
		if (WordPress.currentBlog.getApi_key() == null) {
			if (WordPress.currentBlog.getDotcom_username() != null) {
				// we have an alternate login, use that instead
				sUsername = WordPress.currentBlog.getDotcom_username();
				sPassword = WordPress.currentBlog.getDotcom_password();
			} else {
				sUsername = WordPress.currentBlog.getUsername();
				sPassword = WordPress.currentBlog.getPassword();
			}
			titleBar.startRotatingRefreshIcon();
			new statsUserDataTask().execute(sUsername, sPassword,
					WordPress.currentBlog.getUrl(),
					String.valueOf(WordPress.currentBlog.getBlogId()));
		} else {
			// apiKey found, load default views chart and table
			if (titleBar == null)
				titleBar = (WPTitleBar) findViewById(R.id.actionBar);
			titleBar.startRotatingRefreshIcon();
			new getStatsDataTask().execute(WordPress.currentBlog.getApi_key(),
					WordPress.currentBlog.getApi_blogid(), "views", 7);

		}

	}

	private Vector<String> getAPIInfo(String username, String password,
			String url, String storedBlogID) {
		Vector<String> apiInfo = null;

		if (!WordPress.currentBlog.isDotcomFlag()) {
			// get the blog's url
			XMLRPCClient xmlClient = new XMLRPCClient(url,
					WordPress.currentBlog.getHttpuser(),
					WordPress.currentBlog.getHttppassword());
			Object[] params = { WordPress.currentBlog.getUsername(),
					WordPress.currentBlog.getPassword() };
			try {
				Object[] result = (Object[]) xmlClient.call("wp.getUsersBlogs",
						params);
				if (result != null) {
					if (result.length == 1) {
						// with one result, let's just match on the host
						HashMap<?, ?> blog_info = (HashMap<?, ?>) result[0];
						if (blog_info != null) {
							if (blog_info.get("url") != null) {
								String apiURL = blog_info.get("url").toString();
								URI apiURI = new URI(apiURL);
								String apiHost = apiURI.getHost().replace(
										"www.", "");
								URI localURI = new URI(url);
								String localHost = localURI.getHost().replace(
										"www.", "");

								if (localHost.equals(apiHost)) {
									url = apiURL;
								}
							}
						}
					} else {
						// for multiple results, let's match on the host+path
						for (int i = 0; i < result.length; i++) {
							HashMap<?, ?> blog_info = (HashMap<?, ?>) result[i];
							if (blog_info != null) {
								if (blog_info.get("url") != null) {
									String apiURL = blog_info.get("url")
											.toString();
									URI apiURI = new URI(apiURL);
									String apiHost = apiURI.getHost().replace(
											"www.", "");
									URI localURI = new URI(url);
									String localHost = localURI.getHost()
											.replace("www.", "");

									String apiURIPath = "", localURIPath = "";
									if (apiURI.getPath() != null) {
										apiURIPath = apiURI.getPath();
									}
									if (localURI.getPath() != null) {
										if (localURI.getPath().lastIndexOf("/") > 0)
											localURIPath = localURI
													.getPath()
													.substring(
															0,
															localURI.getPath()
																	.lastIndexOf(
																			"/") + 1);
									}

									if ((localHost + localURIPath)
											.equals(apiHost + apiURIPath)) {
										url = apiURL;
										break;
									}
								}
							}
						}
					}
				}
			} catch (XMLRPCException e) {
				url = url.replace("xmlrpc.php", "");
			} catch (URISyntaxException e) {
				url = url.replace("xmlrpc.php", "");
			}
		} else {
			url = url.replace("xmlrpc.php", "");
		}

		String wwwURL = "";
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

		URI uri = URI
				.create("https://public-api.wordpress.com/getuserblogs.php");
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
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != HttpStatus.SC_OK) {
				throw new IOException("HTTP status code: " + statusCode
						+ " was returned. "
						+ response.getStatusLine().getReasonPhrase());
			}

			// setup pull parser
			try {
				XmlPullParser pullParser = XmlPullParserFactory.newInstance()
						.newPullParser();
				HttpEntity entity = response.getEntity();
				// change to pushbackinput stream 1/18/2010 to handle self
				// installed wp sites that insert the BOM
				PushbackInputStream is = new PushbackInputStream(
						entity.getContent());

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

							if (((curBlogURL.equals(url) || (curBlogURL
									.equals(wwwURL))) || storedBlogID
									.equals(curBlogID))
									&& !curBlogID.equals("1")) {
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
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return apiInfo;
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

	private class statsUserDataTask extends AsyncTask<String, Void, Vector<?>> {

		protected void onPostExecute(Vector<?> result) {
			titleBar.stopRotatingRefreshIcon();
			if (result != null) {
				// user's original login data worked
				// store the api key and blog id
				final String apiKey = result.get(0).toString();
				final String apiBlogID = result.get(1).toString();
				WordPress.currentBlog.setApi_blogid(apiBlogID);
				WordPress.currentBlog.setApi_key(apiKey);
				WordPress.currentBlog.save(ViewStats.this, "");
				if (!isFinishing())
					titleBar.startRotatingRefreshIcon();
				new getStatsDataTask().execute(apiKey, apiBlogID, "views", 7);

			} else {
				// prompt for the username and password
				if (firstRun > 0) {
					Toast.makeText(
							ViewStats.this,
							getResources().getText(R.string.invalid_login)
									+ " "
									+ getResources().getText(
											R.string.site_not_found),
							Toast.LENGTH_SHORT).show();
				}
				firstRun++;
				if (errorMsg.equals("")) {
					showOrHideLoginForm();
				} else {
					errorMsg = "";
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							ViewStats.this);
					dialogBuilder.setTitle(getResources().getText(
							R.string.connection_error));
					dialogBuilder.setMessage(R.string.connection_error_occured);
					dialogBuilder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Just close the window.
								}
							});
					dialogBuilder.setCancelable(true);
					if (!isFinishing())
						dialogBuilder.create().show();
				}
			}
		}

		@Override
		protected Vector<?> doInBackground(String... args) {

			Vector<?> apiInfo = getAPIInfo(args[0], args[1], args[2], args[3]);

			return apiInfo;

		}

	}

	public void showOrHideLoginForm() {
		AnimationSet set = new AnimationSet(true);
		if (loginShowing) {
			loginShowing = !loginShowing;

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
		} else {
			loginShowing = !loginShowing;

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

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && titleBar.isShowingDashboard) {
			titleBar.hideDashboardOverlay();

			return false;
		}

		return super.onKeyDown(keyCode, event);
	}

	private class statsChartTask extends AsyncTask<String, Bitmap, Bitmap> {

		protected void onPostExecute(Bitmap bm) {

			if (bm != null) {
				ImageView iv = (ImageView) findViewById(R.id.chart);
				iv.setImageBitmap(bm);
			}

		}

		@Override
		protected Bitmap doInBackground(String... args) {

			Bitmap bm = null;
			try {
				URL url = new URL(args[0]);
				URLConnection conn = url.openConnection();
				conn.connect();
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				bm = BitmapFactory.decodeStream(bis);
				bis.close();
				is.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return bm;

		}

	}

	private class getStatsDataTask extends AsyncTask<Object, String, String> {

		String reportType;
		int interval;
		Vector<HashMap<String, String>> dataSet = new Vector<HashMap<String, String>>();
		Vector<Integer> numDataSet = new Vector<Integer>();

		protected void onPostExecute(String result) {

			if (result != null) {
				try {
					if (dataSet.size() > 0) {
						// only continue if we received data from the api

						// ui thread
						final int intervalT = interval;

						RelativeLayout filters = (RelativeLayout) findViewById(R.id.filters);
						filters.setVisibility(View.VISIBLE);

						TextView reportTitle = (TextView) findViewById(R.id.chartTitle);
						reportTitle.setVisibility(View.VISIBLE);
						ImageView iv = (ImageView) findViewById(R.id.chart);

						if (reportType.equals("views")) {
							if (intervalT != 1) {
								iv.setVisibility(View.VISIBLE);
							} else {
								iv.setVisibility(View.GONE);
							}

							reportTitle.setText(getResources().getText(
									R.string.report_views));
							String dataValues = "", dateStrings = "", xLabels = "";
							Object[] key = numDataSet.toArray();
							Arrays.sort(key);

							// also adding data to table for display here
							TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
							// clear out all existing rows
							tl.removeAllViews();

							// add header row
							LayoutInflater inflater = getLayoutInflater();
							TableRow table_row = (TableRow) inflater.inflate(
									R.layout.table_row_header, tl, false);

							TextView col_1 = (TextView) table_row
									.findViewById(R.id.col1);
							col_1.setText(getResources().getText(R.string.date));
							col_1.setTypeface(Typeface.DEFAULT_BOLD);

							TextView col_2 = (TextView) table_row
									.findViewById(R.id.col2);
							col_2.setText(getResources().getText(
									R.string.report_views));
							col_2.setTypeface(Typeface.DEFAULT_BOLD);
							// Add row to TableLayout.
							tl.addView(table_row);

							HashMap<?, ?> row;
							for (int i = 0; i < dataSet.size(); i++) {
								row = (HashMap<?, ?>) dataSet.get(i);
								String date;
								try {
									date = row.get("date").toString();
								} catch (Exception e) {
									return;
								}
								String value = numDataSet.get(i).toString();
								dateStrings += date + ",";
								dataValues += value + ",";
								if (i == 0)
									xLabels += date + "|";
								else if (i == (dataSet.size() - 1))
									xLabels += date;
								else
									xLabels += "|";

								// table display work

								// Create a new row to be added.
								TableRow tr = (TableRow) inflater.inflate(
										R.layout.table_row, tl, false);

								// Create a view to be the row-content.
								TextView col1 = (TextView) tr
										.findViewById(R.id.col1);
								col1.setText(date);

								TextView col2 = (TextView) tr
										.findViewById(R.id.col2);
								col2.setText(value);

								if (i % 2 == 0) {
									// different background color for
									// alternating rows
									tr.setBackgroundColor(Color
											.parseColor("#FFE6F0FF"));
									// col1.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									// col2.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
								}
								// Add row to TableLayout.
								tl.addView(tr);
							}

							int maxValue = Integer.parseInt(key[key.length - 1]
									.toString());
							int minValue = Integer.parseInt(key[0].toString());

							dataValues = dataValues.substring(0,
									dataValues.length() - 1);
							dateStrings = dateStrings.substring(0,
									dateStrings.length() - 1);

							long minBuffer = Math.round(minValue
									- (maxValue * .10));
							if (minBuffer < 0) {
								minBuffer = 0;
							}
							long maxBuffer = Math.round(maxValue
									+ (maxValue * .10));
							// round to the lowest 10 for prettier charts
							for (int i = 0; i < 9; i++) {
								if (minBuffer % 10 == 0)
									break;
								else {
									minBuffer--;
								}
							}

							for (int i = 0; i < 9; i++) {
								if (maxBuffer % 10 == 0)
									break;
								else {
									maxBuffer++;
								}
							}

							long yInterval = maxBuffer / 10;
							// round the gap in y axis of the chart
							for (int i = 0; i < 9; i++) {
								if (yInterval % 10 == 0)
									break;
								else {
									yInterval++;
								}
							}

							// calculate the grid spacing variables
							float xGrid = 100.00f / 6;
							if (yInterval == 0) {
								// don't divide by zero!
								yInterval = 1;
							}
							long numRows = (maxBuffer - minBuffer) / yInterval;
							float yGrid = 100.00f / numRows;

							// scale to screen size
							Display display = getWindowManager()
									.getDefaultDisplay();
							int width = display.getWidth();
							int height = display.getHeight();
							String screenSize = "320x240";
							if (width > 480 || height > 480) {
								screenSize = "480x360";
							}

							// build the google chart api url
							final String chartViewURL = "http://chart.apis.google.com/chart?chts=464646,20"
									+ "&cht=bvs" + "&chbh=a" + "&chd=t:"
									+ dataValues
									+ "&chs="
									+ screenSize
									+ "&chxt=y,x"
									+ "&chxl=1:|"
									+ xLabels
									+ "&chds="
									+ minBuffer
									+ ","
									+ maxBuffer
									+ "&chxr=0,"
									+ minBuffer
									+ ","
									+ maxBuffer
									+ ","
									+ yInterval
									+ "&chf=c,lg,90,FFFFFF,0,FFFFFF,0.5"
									+ "&chco=a3bcd3,cccccc77"
									+ "&chls=4"
									+ "&chf=c,lg,90,FFFFFF,0,FFFFFF,0.5&chls=4&chxs=0,464646,19,0,t|1,464646,16,0,t,ffffff&chxtc=0,0"
									+ "&chg=" + xGrid + "," + yGrid + ",1,0";

							new statsChartTask().execute(chartViewURL);

						} else if (reportType.equals("postviews")) {
							reportTitle.setText(getResources().getText(
									R.string.report_postviews));
							iv.setVisibility(View.GONE);

							Object[] key = numDataSet.toArray();
							Arrays.sort(key);

							// also adding data to table for display here
							TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
							// clear out all existing rows
							tl.removeAllViews();

							// add header row
							LayoutInflater inflater = getLayoutInflater();
							TableRow table_row = (TableRow) inflater.inflate(
									R.layout.table_row_header, tl, false);

							// Create a Button to be the row-content.
							TextView col_1 = (TextView) table_row
									.findViewById(R.id.col1);
							col_1.setText(getResources().getText(
									R.string.report_post_title));
							col_1.setTypeface(Typeface.DEFAULT_BOLD);

							TextView col_2 = (TextView) table_row
									.findViewById(R.id.col2);
							col_2.setText(getResources().getText(
									R.string.report_views));
							col_2.setTypeface(Typeface.DEFAULT_BOLD);

							// Add row to TableLayout.
							tl.addView(table_row);

							HashMap<?, ?> row;
							for (int i = 0; i < dataSet.size(); i++) {
								row = (HashMap<?, ?>) dataSet.get(i);
								String date = row.get("title").toString();
								String value = numDataSet.get(i).toString();

								// table display work

								// Create a new row to be added.
								TableRow tr = (TableRow) inflater.inflate(
										R.layout.table_row, tl, false);

								// Create a Button to be the row-content.
								TextView col1 = (TextView) tr
										.findViewById(R.id.col1);
								col1.setText(date);

								TextView col2 = (TextView) tr
										.findViewById(R.id.col2);
								col2.setText(value);

								if (i % 2 == 0) {
									// different background color for
									// alternating rows
									tr.setBackgroundColor(Color
											.parseColor("#FFE6F0FF"));
									// col1.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									// col2.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
								}
								// Add row to TableLayout.
								tl.addView(tr);
							}

						} else if (reportType.equals("referrers")
								|| reportType.equals("searchterms")
								|| reportType.equals("clicks")) {
							iv.setVisibility(View.GONE);

							Object[] key = numDataSet.toArray();
							Arrays.sort(key);

							// also adding data to table for display here
							TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
							// clear out all existing rows
							tl.removeAllViews();

							// add header row
							LayoutInflater inflater = getLayoutInflater();
							TableRow table_row = (TableRow) inflater.inflate(
									R.layout.table_row_header, tl, false);

							// Create a Button to be the row-content.
							TextView col_1 = (TextView) table_row
									.findViewById(R.id.col1);
							if (reportType.equals("referrers")) {
								col_1.setText(getResources().getText(
										R.string.report_referrers));
								reportTitle.setText(getResources().getText(
										R.string.report_referrers));
							} else if (reportType.equals("searchterms")) {
								reportTitle.setText(getResources().getText(
										R.string.report_searchterms));
								col_1.setText(getResources().getText(
										R.string.report_searchterms));
							} else {
								reportTitle.setText(getResources().getText(
										R.string.report_clicks));
								col_1.setText(getResources().getText(
										R.string.report_clicks));
							}
							col_1.setTypeface(Typeface.DEFAULT_BOLD);

							TextView col_2 = (TextView) table_row
									.findViewById(R.id.col2);
							col_2.setText("Views");
							col_2.setTypeface(Typeface.DEFAULT_BOLD);
							// Add row to TableLayout.
							tl.addView(table_row);

							HashMap<?, ?> row;
							for (int i = 0; i < dataSet.size(); i++) {
								row = (HashMap<?, ?>) dataSet.get(i);
								String date = row.get("value").toString();
								String value = numDataSet.get(i).toString();

								// table display work

								// Create a new row to be added.
								TableRow tr = (TableRow) inflater.inflate(
										R.layout.table_row, tl, false);

								// Create a Button to be the row-content.
								TextView col1 = (TextView) tr
										.findViewById(R.id.col1);
								col1.setText(date);
								Linkify.addLinks(col1, Linkify.WEB_URLS);

								TextView col2 = (TextView) tr
										.findViewById(R.id.col2);
								col2.setText(value);

								if (i % 2 == 0) {
									// different background color for
									// alternating rows
									tr.setBackgroundColor(Color
											.parseColor("#FFE6F0FF"));
									// col1.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									// col2.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
								}
								// Add row to TableLayout.
								tl.addView(tr);
							}

						}
						if (!isFinishing())
							titleBar.stopRotatingRefreshIcon();

					} else {
						titleBar.stopRotatingRefreshIcon();
						RelativeLayout filters = (RelativeLayout) findViewById(R.id.filters);
						filters.setVisibility(View.VISIBLE);
						Toast.makeText(ViewStats.this,
								getResources().getText(R.string.no_data_found),
								Toast.LENGTH_SHORT).show();

					}
				} catch (NumberFormatException e) {
					errorMsg = e.getMessage();
				} catch (IllegalStateException e) {
					errorMsg = e.getMessage();
				} catch (NotFoundException e) {
					errorMsg = e.getMessage();
				}
			}
			if (!errorMsg.equals("")) {
				titleBar.stopRotatingRefreshIcon();
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						ViewStats.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.connection_error));
				dialogBuilder.setMessage(errorMsg);
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
			} else {
				titleBar.stopRotatingRefreshIcon();
			}

		}

		@Override
		protected String doInBackground(Object... args) {
			if (isFinishing()) {
				finish();
			}

			String apiKey = (String) args[0];
			String blogID = (String) args[1];
			reportType = (String) args[2];
			interval = (Integer) args[3];

			String DATE_FORMAT = "yyyy-MM-dd";
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			Calendar c1 = Calendar.getInstance(); // today
			String period = "";
			if (interval == 90) {
				period = "&period=week";
				interval = 12;
			} else if (interval == 365) {
				period = "&period=month";
				interval = 11;
			} else if (interval == -1) {
				period = "&period=month";
			}
			String uriString = "https://ssl-stats.wordpress.com/csv.php"
					+ "?api_key=" + apiKey + "&blog_id=" + blogID
					+ "&format=xml&table=" + reportType + "&end="
					+ sdf.format(c1.getTime()) + "&days=" + interval
					+ "&limit=-1" + period;
			vsoURI = uriString;
			if (!reportType.equals("views")) {
				uriString += "&summarize";
			}
			URI uri = URI.create(uriString);

			configureClient(uri, null, null);

			// execute HTTP POST request

			HttpResponse response;
			try {
				response = client.execute(postMethod);
				XmlPullParser pullParser = XmlPullParserFactory.newInstance()
						.newPullParser();
				HttpEntity entity = response.getEntity();
				// change to pushbackinput stream 1/18/2010 to handle self
				// installed
				// wp sites that insert the BOM

				PushbackInputStream is = new PushbackInputStream(
						entity.getContent());

				// get rid of junk characters before xml response. 60 = '<'.
				// Added
				// stopper to prevent infinite loop
				int bomCheck = is.read();
				int stopper = 0;
				while (bomCheck != 60 && stopper < 20) {
					bomCheck = is.read();
					stopper++;
				}
				is.unread(bomCheck);

				pullParser.setInput(is, "UTF-8");

				int eventType = pullParser.getEventType();
				boolean foundDataItem = false;

				int rowCount = 0;
				// parse the xml response
				// most replies follow the same xml structure, so the data
				// is stored
				// in a vector for display after parsing
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_DOCUMENT) {
						// System.out.println("Start document");
					} else if (eventType == XmlPullParser.END_DOCUMENT) {
						// System.out.println("End document");
					} else if (eventType == XmlPullParser.START_TAG) {
						String name = pullParser.getName();
						if (name.equals("views") || name.equals("postviews")
								|| name.equals("referrers")
								|| name.equals("clicks")
								|| name.equals("searchterms")
								|| name.equals("videoplays")) {
						} else if (pullParser.getName().equals("total")) {
							// that'll do, pig. that'll do.
							break;
						} else {
							foundDataItem = true;
							// loop through the attributes, add them to the
							// hashmap
							HashMap<String, String> dataRow = new HashMap<String, String>();
							for (int i = 0; i < pullParser.getAttributeCount(); i++) {
								dataRow.put(pullParser.getAttributeName(i)
										.toString(), pullParser
										.getAttributeValue(i).toString());
							}
							if (dataRow != null) {
								dataSet.add(rowCount, dataRow);
							}
						}

					} else if (eventType == XmlPullParser.END_TAG) {
						// System.out.println("End tag "+pullParser.getName());
					} else if (eventType == XmlPullParser.TEXT) {
						if (foundDataItem) {
							if (pullParser.getText().toString() == "") {
								numDataSet.add(rowCount, 0);
							} else {
								int value = 0;
								// sometimes we get an empty string from the
								// stats api, adding a catch here.
								try {
									value = Integer.parseInt(pullParser
											.getText().toString());
								} catch (NumberFormatException e) {
								}
								numDataSet.add(rowCount, value);
							}
							rowCount++;
							foundDataItem = false;
						}
					}
					eventType = pullParser.next();
				}
				return "OK";
			} catch (ClientProtocolException e) {
				errorMsg = e.getMessage();
				return null;
			} catch (IOException e) {
				errorMsg = e.getMessage();
				return null;
			} catch (XmlPullParserException e) {
				errorMsg = e.getMessage();
				return null;
			}
		}

	}

}