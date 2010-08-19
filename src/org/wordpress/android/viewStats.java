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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class viewStats extends Activity {
	public boolean success = false;
	public String blogURL, xmlrpcURL;
	private ConnectionClient client;
	private HttpPost postMethod;
	private HttpParams httpParams;
	String id = "", accountName = "", errorMsg = "";
	boolean loginShowing = false;
	ProgressDialog loadingDialog;
	private int ID_DIALOG_GET_STATS = 0;
	private int firstRun = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_stats);  

		Bundle extras = getIntent().getExtras();
		if(extras !=null)
		{
			id = extras.getString("id");    
			accountName = extras.getString("accountName");
		}  

		//get the ball rolling...
		initStats();

		Button saveStatsLogin = (Button) findViewById(R.id.saveDotcom);   

		saveStatsLogin.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
				EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

				String dcUsername = dotcomUsername.getText().toString();
				String dcPassword = dotcomPassword.getText().toString();

				if (dcUsername.equals("") || dcPassword.equals("")){
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewStats.this);
					dialogBuilder.setTitle(getResources().getText(R.string.required_fields));
					dialogBuilder.setMessage(getResources().getText(R.string.username_password_required));
					dialogBuilder.setPositiveButton("OK",  new
							DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// just close the dialog

						}
					});
					dialogBuilder.setCancelable(true);
					dialogBuilder.create().show();
				}
				else{           	
					//store the login data
					WordPressDB settingsDB = new WordPressDB(viewStats.this);
					settingsDB.saveStatsLogin(viewStats.this, id, dcUsername, dcPassword);
					showOrHideLoginForm();
					initStats();  //start over again now that we have the login
				}
			}
		});

		Button go = (Button) findViewById(R.id.go);   

		go.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				Spinner reportType = (Spinner) findViewById(R.id.reportType);
				Spinner reportInterval = (Spinner) findViewById(R.id.reportInterval);

				final String type = parseType(reportType.getSelectedItemPosition());
				final int interval = parseInterval(reportInterval.getSelectedItemPosition());

				WordPressDB settingsDB = new WordPressDB(viewStats.this);
				Vector apiData = settingsDB.loadAPIData(viewStats.this, id);

				final String apiKey = apiData.get(0).toString();
				final String apiBlogID = apiData.get(1).toString();
				showDialog(ID_DIALOG_GET_STATS);
				Thread action = new Thread() 
				{ 
					public void run() 
					{
						getStatsData(apiKey, apiBlogID, type, interval);
					} 
				}; 
				action.start();


			}
		});
		
		TextView wpcomHelp = (TextView) findViewById(R.id.wpcomHelp);
		wpcomHelp.setOnClickListener(new TextView.OnClickListener() {
			public void onClick(View v) {

				Intent intent1 = new Intent(Intent.ACTION_VIEW);  
				intent1.setData(Uri.parse("http://en.support.wordpress.com/stats/"));  
				startActivity(intent1); 


			}
		});
		
		

	}

	protected int parseInterval(int position) {
		// TODO Auto-generated method stub
		int interval = 0;
		switch (position){

		case 0:
			interval = 7;
			break;
		case 1:
			interval = 30;
			break;
		case 2:
			interval = 90;
			break;
		case 3:
			interval = 365;
			break;
		case 4:
			interval = -1;
			break;
		};

		return interval;
	}

	protected String parseType(int position) {
		// TODO Auto-generated method stub
		String type = "";
		switch (position){

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
		};

		return type;
	}

	private void initStats() {

		WordPressDB settingsDB = new WordPressDB(this);
		Vector settings = settingsDB.loadSettings(this, id);
		final Vector statsData = settingsDB.loadAPIData(this, id);

		if (statsData == null){
			String sUsername = "";
			String sPassword = "";
			Vector apiLogin = settingsDB.loadStatsLogin(this, id);
			if (apiLogin != null){
				//we have an alternate login, use that instead
				sUsername = apiLogin.get(0).toString();
				sPassword = apiLogin.get(1).toString();
			}
			else{
				sUsername = settings.get(2).toString();
				sPassword = settings.get(3).toString();
			}

			//no apiKey found in db, go get it
			String sURL = "";
			if (settings.get(0).toString().contains("xmlrpc.php"))
			{
				sURL = settings.get(0).toString().replace("xmlrpc.php", "");
			}

			if (!sURL.endsWith("/")){
				sURL += "/";
			}

			sURL = sURL.replace("https://", "http://");

			String blogID = settings.get(10).toString();
			showProgressBar();
			new statsUserDataTask().execute(sUsername, sPassword, sURL, blogID);
		}
		else{
			//apiKey found, load default views chart and table
			showDialog(ID_DIALOG_GET_STATS);
			Thread action = new Thread() 
			{ 
				public void run() 
				{
					getStatsData(statsData.get(0).toString(), statsData.get(1).toString(), "views", 7);
				} 
			}; 
			action.start();

		}

	}

	private void getStatsData(String apiKey, String blogID, final String reportType, int interval) {
		Vector apiInfo = null;
		String DATE_FORMAT = "yyyy-MM-dd";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		Calendar c1 = Calendar.getInstance(); // today
		String period = "";
		if (interval == 90){
			period = "&period=week";
			interval = 12;
		}
		else if (interval == 365){
			period = "&period=month";
			interval = 11;
		}
		else if (interval == -1){
			period = "&period=month";
		}
		String uriString = "http://stats.wordpress.com/csv.php" + "?api_key=" + apiKey + "&blog_id=" + blogID + "&format=xml&table=" 
		+ reportType + "&end=" + sdf.format(c1.getTime()) + "&days=" + interval + "&limit=-1" + period;
		if (!reportType.equals("views")){
			uriString += "&summarize";
		}
		URI uri = URI.create(uriString);

		configureClient(uri, null, null);

		// execute HTTP POST request
		try {
			HttpResponse response;
			response = client.execute(postMethod);

			// setup pull parser

			XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
			HttpEntity entity = response.getEntity();
			//change to pushbackinput stream 1/18/2010 to handle self installed wp sites that insert the BOM

			PushbackInputStream is = new PushbackInputStream(entity.getContent());

			//get rid of junk characters before xml response.  60 = '<'.  Added stopper to prevent infinite loop
			int bomCheck = is.read();
			int stopper = 0;
			while (bomCheck != 60 && stopper < 20){
				bomCheck = is.read();
				stopper++;
			}
			is.unread(bomCheck);

			pullParser.setInput(is, "UTF-8");

			int eventType = pullParser.getEventType();
			HashMap statsData = new HashMap();
			boolean foundRoot = false;
			boolean foundDataItem = false;
			final Vector dataSet = new Vector();
			final Vector numDataSet = new Vector();
			String curCol1 = "";
			int rowCount = 0;
			//parse the xml response
			//most replies follow the same xml structure, so the data is stored in a vector for display after parsing
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if(eventType == XmlPullParser.START_DOCUMENT) {
					//System.out.println("Start document");
				} else if(eventType == XmlPullParser.END_DOCUMENT) {
					//System.out.println("End document");
				} else if(eventType == XmlPullParser.START_TAG) {
					String name = pullParser.getName();
					if (name.equals("views") || name.equals("postviews") || name.equals("referrers") || name.equals("clicks") 
							|| name.equals("searchterms") || name.equals("videoplays")){
						foundRoot = true;
					}
					else if (pullParser.getName().equals("total")){
						//that'll do, pig. that'll do.
						break;
					}
					else{
						foundDataItem = true;
						//loop through the attributes, add them to the hashmap
						HashMap dataRow = new HashMap();
						for (int i=0; i<pullParser.getAttributeCount();i++){
							dataRow.put(pullParser.getAttributeName(i).toString(), pullParser.getAttributeValue(i).toString());
						}
						if (dataRow != null){
							dataSet.add(rowCount, dataRow);
						}
					}

				} else if(eventType == XmlPullParser.END_TAG) {
					//System.out.println("End tag "+pullParser.getName());
				} else if(eventType == XmlPullParser.TEXT) {
					if (foundDataItem){
						String temp = pullParser.getText();
						numDataSet.add(rowCount, Integer.parseInt(pullParser.getText().toString()));
						rowCount++;
						foundDataItem = false;
					}
				}
				eventType = pullParser.next();
			}

			if (dataSet.size() > 0){
				//only continue if we received data from the api

				//ui thread
				Thread uiThread = new Thread() 
				{ 
					public void run() 
					{

						RelativeLayout filters = (RelativeLayout) findViewById(R.id.filters);
						filters.setVisibility(View.VISIBLE);

						TextView reportTitle = (TextView) findViewById(R.id.chartTitle);
						reportTitle.setVisibility(View.VISIBLE);
						ImageView iv = (ImageView) findViewById(R.id.chart);
						
						if (reportType.equals("views")){
							iv.setVisibility(View.VISIBLE);
							reportTitle.setText(getResources().getText(R.string.report_views));
							String dataValues = "", dateStrings = "";
							Object[] key = numDataSet.toArray();
							Arrays.sort(key);

							//also adding data to table for display here
							TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
							//clear out all existing rows
							tl.removeAllViews();

							//add header row
							LayoutInflater inflater = getLayoutInflater();
							TableRow table_row = (TableRow) inflater.inflate(R.layout.table_row_header, tl, false);

							TextView col_1 = (TextView)table_row.findViewById(R.id.col1);
							col_1.setText(getResources().getText(R.string.date));
							col_1.setTypeface(Typeface.DEFAULT_BOLD);

							TextView col_2 = (TextView)table_row.findViewById(R.id.col2);
							col_2.setText(getResources().getText(R.string.report_views));
							col_2.setTypeface(Typeface.DEFAULT_BOLD);
							//Add row to TableLayout.
							tl.addView(table_row);

							HashMap row;
							for   (int   i   =   0;   i   <   dataSet.size();   i++)   { 
								row = (HashMap) dataSet.get(i);
								String date = row.get("date").toString();
								String value = numDataSet.get(i).toString();
								dateStrings += date + ",";
								dataValues += value + ",";

								//table display work

								// Create a new row to be added.
								TableRow tr = (TableRow) inflater.inflate(R.layout.table_row, tl, false);

								//Create a view to be the row-content. 
								TextView col1 = (TextView)tr.findViewById(R.id.col1);
								col1.setText(date);

								TextView col2 = (TextView)tr.findViewById(R.id.col2);
								col2.setText(value);

								if (i % 2 == 0){
									//different background color for alternating rows
									tr.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									//col1.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									//col2.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
								}
								//Add row to TableLayout.
								tl.addView(tr);
							}

							int maxValue = Integer.parseInt(key[key.length - 1].toString());
							int minValue = Integer.parseInt(key[0].toString());

							dataValues = dataValues.substring(0, dataValues.length() - 1);
							dateStrings = dateStrings.substring(0, dateStrings.length() - 1);

							String[] dateArray = dateStrings.split(",");

							long minBuffer = Math.round(minValue - (maxValue *.10));
							if (minBuffer < 0){
								minBuffer = 0;
							}
							long maxBuffer = Math.round(maxValue + (maxValue *.10));
							//round to the lowest 10 for prettier charts
							for(int i = 0; i < 9; i++) {
								if(minBuffer % 10 == 0)
									break;
								else{
									minBuffer--;
								}
							}

							for(int i = 0; i < 9; i++) {
								if(maxBuffer % 10 == 0)
									break;
								else{
									maxBuffer++;
								}
							}

							long yInterval = maxBuffer / 10;
							//round the gap in y axis of the chart
							for(int i = 0; i < 9; i++) {
								if(yInterval % 10 == 0)
									break;
								else{
									yInterval++;
								}
							}

							//calculate the grid spacing variables
							float xGrid = 100.00f / 6;
							long numRows = (maxBuffer - minBuffer) / yInterval;
							float yGrid = 100.00f / numRows;

							//scale to screen size
							Display display = getWindowManager().getDefaultDisplay();
					        int width = display.getWidth();
					        int height = display.getHeight();
					        String screenSize = "320x240";
					        if (width > 480 || height > 480){
					        	screenSize = "480x360";
					        }
							
							//build the google chart api url
							final String chartViewURL = "http://chart.apis.google.com/chart?chts=464646,20&cht=lc&chd=t:"+ dataValues + "&chs=" + screenSize + 
							"&chl=" + dateArray[0].toString() + "|" + dateArray[dateArray.length - 1].toString() + "&chxt=y" + 
							"&chds=" + minBuffer + "," + maxBuffer + "&chxr=0," + minBuffer + "," + maxBuffer + "," + yInterval + 
							"&chf=c,lg,90,E2E2E2,0,FEFEFE,0.5&chm=o,14568A,0,-1,10.0&chco=14568A&chls=4&chg=" + xGrid + "," + yGrid;


							try {
								URL url = new URL(chartViewURL); 
								URLConnection conn = url.openConnection(); 
								conn.connect(); 
								InputStream is = conn.getInputStream(); 
								BufferedInputStream bis = new BufferedInputStream(is); 
								Bitmap bm = BitmapFactory.decodeStream(bis); 
								bis.close(); 
								is.close();
								iv.setImageBitmap(bm);
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}				  


						}
						else if (reportType.equals("postviews")){
							reportTitle.setText(getResources().getText(R.string.report_postviews));
							iv.setVisibility(View.GONE);

							Object[] key = numDataSet.toArray();
							Arrays.sort(key);

							//also adding data to table for display here
							TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
							//clear out all existing rows
							tl.removeAllViews();

							//add header row
							LayoutInflater inflater = getLayoutInflater();
							TableRow table_row = (TableRow) inflater.inflate(R.layout.table_row_header, tl, false);

							//Create a Button to be the row-content. 
							TextView col_1 = (TextView)table_row.findViewById(R.id.col1);
							col_1.setText(getResources().getText(R.string.report_post_title));
							col_1.setTypeface(Typeface.DEFAULT_BOLD);

							TextView col_2 = (TextView)table_row.findViewById(R.id.col2);
							col_2.setText(getResources().getText(R.string.report_views));
							col_2.setTypeface(Typeface.DEFAULT_BOLD);
							
							//Add row to TableLayout.
							tl.addView(table_row);

							HashMap row;
							for   (int   i   =   0;   i   <   dataSet.size();   i++)   { 
								row = (HashMap) dataSet.get(i);
								String date = row.get("title").toString();
								String value = numDataSet.get(i).toString();
								
								//table display work

								// Create a new row to be added.
								TableRow tr = (TableRow) inflater.inflate(R.layout.table_row, tl, false);
								

								//Create a Button to be the row-content. 
								TextView col1 = (TextView)tr.findViewById(R.id.col1);
								col1.setText(date);

								TextView col2 = (TextView)tr.findViewById(R.id.col2);
								col2.setText(value);

								if (i % 2 == 0){
									//different background color for alternating rows
									tr.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									//col1.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									//col2.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
								}
								//Add row to TableLayout.
								tl.addView(tr);
							}			  

						}
						else if (reportType.equals("referrers") || reportType.equals("searchterms") || reportType.equals("clicks")){
							iv.setVisibility(View.GONE);

							Object[] key = numDataSet.toArray();
							Arrays.sort(key);

							//also adding data to table for display here
							TableLayout tl = (TableLayout) findViewById(R.id.dataTable);
							//clear out all existing rows
							tl.removeAllViews();

							//add header row
							LayoutInflater inflater = getLayoutInflater();
							TableRow table_row = (TableRow) inflater.inflate(R.layout.table_row_header, tl, false);

							//Create a Button to be the row-content. 
							TextView col_1 = (TextView)table_row.findViewById(R.id.col1);
							if (reportType.equals("referrers")){
								col_1.setText(getResources().getText(R.string.report_referrers));
								reportTitle.setText(getResources().getText(R.string.report_referrers));
							}
							else if (reportType.equals("searchterms")){
								reportTitle.setText(getResources().getText(R.string.report_searchterms));
								col_1.setText(getResources().getText(R.string.report_searchterms));
							}
							else {
								reportTitle.setText(getResources().getText(R.string.report_clicks));
								col_1.setText(getResources().getText(R.string.report_clicks));
							}
							col_1.setTypeface(Typeface.DEFAULT_BOLD);

							TextView col_2 = (TextView)table_row.findViewById(R.id.col2);
							col_2.setText("Views");
							col_2.setTypeface(Typeface.DEFAULT_BOLD);
							//Add row to TableLayout.
							tl.addView(table_row);

							HashMap row;
							for   (int   i   =   0;   i   <   dataSet.size();   i++)   { 
								row = (HashMap) dataSet.get(i);
								String date = row.get("value").toString();
								String value = numDataSet.get(i).toString();

								//table display work

								// Create a new row to be added.
								TableRow tr = (TableRow) inflater.inflate(R.layout.table_row, tl, false);

								//Create a Button to be the row-content. 
								TextView col1 = (TextView)tr.findViewById(R.id.col1);
								col1.setText(date);
								Linkify.addLinks(col1, Linkify.WEB_URLS);

								TextView col2 = (TextView)tr.findViewById(R.id.col2);
								col2.setText(value);

								if (i % 2 == 0){
									//different background color for alternating rows
									tr.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									//col1.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
									//col2.setBackgroundColor(Color.parseColor("#FFE6F0FF"));
								}
								//Add row to TableLayout.
								tl.addView(tr);
							}			  

						}

						dismissDialog(ID_DIALOG_GET_STATS);
					}
				}; 
				this.runOnUiThread(uiThread);

			}
			else{
				Thread alert = new Thread() 
				{ 
					public void run() 
					{
						dismissDialog(ID_DIALOG_GET_STATS); 
						RelativeLayout filters = (RelativeLayout) findViewById(R.id.filters);
						filters.setVisibility(View.VISIBLE);
						Toast.makeText(viewStats.this, getResources().getText(R.string.no_data_found), Toast.LENGTH_SHORT).show();
					}
				}; 
				this.runOnUiThread(alert);
			}

		} catch (ClientProtocolException e) {
			dismissDialog(ID_DIALOG_GET_STATS);
			errorMsg = e.getMessage();
		} catch (IllegalStateException e) {
			dismissDialog(ID_DIALOG_GET_STATS);
			errorMsg = e.getMessage();
		} catch (IOException e) {
			dismissDialog(ID_DIALOG_GET_STATS);
			errorMsg = e.getMessage();
		} catch (XmlPullParserException e) {
			dismissDialog(ID_DIALOG_GET_STATS);
			errorMsg = e.getMessage();
		}
		
		if (errorMsg != ""){
			Thread error = new Thread() 
			{ 
			  public void run() 
			  {
				  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewStats.this);
				  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
				  dialogBuilder.setMessage(errorMsg);
				  dialogBuilder.setPositiveButton("OK",  new
          		  DialogInterface.OnClickListener() {
					  public void onClick(DialogInterface dialog, int whichButton) {
                  // Just close the window.
              	
					  }		
				  });
				  dialogBuilder.setCancelable(true);
				  dialogBuilder.create().show();
			  }
			}; 
			this.runOnUiThread(error);
		}

	}

	private Vector getAPIInfo(String username, String password, String url, String storedBlogID) {

		Vector apiInfo = null;
		
		URI blogHost = URI.create(url);
		String blogDomain = blogHost.getHost();
		
		URI uri = URI.create("https://public-api.wordpress.com/getuserblogs.php");

		configureClient(uri, username, password);

		// execute HTTP POST request
		HttpResponse response;
		try {
			response = client.execute(postMethod);
			/*ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			response.getEntity().writeTo(outstream);
			String text = outstream.toString();
			Log.i("WordPress", text);*/
			// check status code
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != HttpStatus.SC_OK) {
				throw new IOException("HTTP status code: " + statusCode + " was returned. " + response.getStatusLine().getReasonPhrase());
			}

			// setup pull parser
			try {
				XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
				HttpEntity entity = response.getEntity();
				//change to pushbackinput stream 1/18/2010 to handle self installed wp sites that insert the BOM
				PushbackInputStream is = new PushbackInputStream(entity.getContent());

				//get rid of junk characters before xml response.  60 = '<'.  Added stopper to prevent infinite loop
				int bomCheck = is.read();
				int stopper = 0;
				while (bomCheck != 60 && stopper < 20){
					bomCheck = is.read();
					stopper++;
				}
				is.unread(bomCheck);

				pullParser.setInput(is, "UTF-8");

				int eventType = pullParser.getEventType();
				String apiKey = "";
				String blogID = "";
				boolean foundKey = false;
				boolean foundID = false;
				boolean foundURL = false;
				String curBlogID = "";
				String curBlogURL = "";
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if(eventType == XmlPullParser.START_DOCUMENT) {
						//System.out.println("Start document");
					} else if(eventType == XmlPullParser.END_DOCUMENT) {
						//System.out.println("End document");
					} else if(eventType == XmlPullParser.START_TAG) {
						if (pullParser.getName().equals("apikey")){
							foundKey = true;
						}
						else if (pullParser.getName().equals("id")){
							foundID = true;
						}
						else if (pullParser.getName().equals("url")){
							foundURL = true;
						}
					} else if(eventType == XmlPullParser.END_TAG) {
						//System.out.println("End tag "+pullParser.getName());
					} else if(eventType == XmlPullParser.TEXT) {
						//System.out.println("Text "+pullParser.getText().toString());
						if (foundKey){
							apiKey = pullParser.getText();
							foundKey = false;
						}
						else if (foundID){
							curBlogID = pullParser.getText();
							foundID = false;
						}
						else if (foundURL){
							curBlogURL = pullParser.getText();
							URI curBlogHost = URI.create(curBlogURL);
							String curBlogDomain = curBlogHost.getHost();
							foundURL = false;
							/*if (url.endsWith("/")){
								url = url.substring(0, url.length() - 1);
							}*/
							if (curBlogDomain.equals(blogDomain) || storedBlogID.equals(curBlogID)){
								//yay, found a match
								blogID = curBlogID;
								apiInfo = new Vector();
								apiInfo.add(apiKey);
								apiInfo.add(blogID);
								return apiInfo;
							}

						}
					}
					eventType = pullParser.next();
				}

			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return apiInfo;
	}

	private void configureClient(URI uri, String username, String password) {
		postMethod = new HttpPost(uri);

		postMethod.addHeader("charset", "UTF-8");
		//UPDATE THE VERSION NUMBER BEFORE RELEASE! <3 Dan
		postMethod.addHeader("User-Agent", "wp-android/1.3.4");

		httpParams = postMethod.getParams();
		HttpProtocolParams.setUseExpectContinue(httpParams, false);
		UsernamePasswordCredentials creds;
		//username & password for basic http auth
		if (username != null){
			creds = new UsernamePasswordCredentials(username, password);
		}
		else{
			creds = new UsernamePasswordCredentials("", "");
		}

		//this gets connections working over https
		if (uri.getScheme() != null){
			if(uri.getScheme().equals("https")) { 
				if(uri.getPort() == -1)
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
			} 
			else {
				client = new ConnectionClient(creds); 
			}
		}
		else{
			client = new ConnectionClient(creds);
		}

	}

	private class statsUserDataTask extends AsyncTask<String, Void, Vector> {

		protected void onProgressUpdate() {
		}

		protected void onPostExecute(Vector result) {
			closeProgressBar();
			WordPressDB settingsDB = new WordPressDB(viewStats.this);
			if (result != null){
				//user's original login data worked
				//store the api key and blog id
				final String apiKey = result.get(0).toString();
				final String apiBlogID = result.get(1).toString();
				settingsDB.saveAPIData(viewStats.this, id, apiKey, apiBlogID);
				showDialog(ID_DIALOG_GET_STATS);
				Thread action = new Thread() 
				{ 
					public void run() 
					{
						getStatsData(apiKey, apiBlogID, "views", 7);
					} 
				}; 
				action.start();

			}
			else{
				//prompt for the username and password
				if (firstRun > 0){
					Toast.makeText(viewStats.this, getResources().getText(R.string.invalid_login) + " " + getResources().getText(R.string.site_not_found), Toast.LENGTH_SHORT).show();
				}
				firstRun++;
				showOrHideLoginForm(); 
			}
		}
		@Override
		protected Vector doInBackground(String... args) {

			Vector apiInfo = getAPIInfo(args[0], args[1], args[2], args[3]);

			return apiInfo;

		}

	}

	public void showOrHideLoginForm() {
		AnimationSet set = new AnimationSet(true);
		if (loginShowing){
			loginShowing = !loginShowing;

			Animation animation = new AlphaAnimation(1.0f, 0.0f);
			animation.setDuration(500);
			set.addAnimation(animation);

			animation = new TranslateAnimation(
					Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f
			);
			animation.setDuration(500);
			set.addAnimation(animation);
			;
			RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);       
			moderationBar.clearAnimation();
			moderationBar.startAnimation(set);
			moderationBar.setVisibility(View.INVISIBLE);
		}
		else{
			loginShowing = !loginShowing;

			Animation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setDuration(500);
			set.addAnimation(animation);

			animation = new TranslateAnimation(
					Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 0.0f
			);
			animation.setDuration(500);
			set.addAnimation(animation);

			RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);       
			moderationBar.setVisibility(View.VISIBLE);
			moderationBar.startAnimation(set);
		}

	}

	public void showProgressBar() {
		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);

		LayoutAnimationController controller =
			new LayoutAnimationController(set, 0.5f);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);       
		loading.setVisibility(View.VISIBLE);
		loading.setLayoutAnimation(controller);
	}

	public void closeProgressBar() {

		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, -1.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);

		LayoutAnimationController controller =
			new LayoutAnimationController(set, 0.5f);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);       

		loading.startAnimation(set);
		loading.setVisibility(View.INVISIBLE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_GET_STATS){
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setTitle(getResources().getText(R.string.retrieving_stats));
			loadingDialog.setMessage(getResources().getText(R.string.attempt_retrieve));
			loadingDialog.setCancelable(true);
			loadingDialog.setIndeterminate(true);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
	}

}