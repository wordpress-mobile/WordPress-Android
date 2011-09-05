package org.wordpress.android;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.WPTitleBar;
import org.wordpress.android.util.WPTitleBar.OnBlogChangedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

public class Dashboard extends Activity {
	public Vector<?> accounts;
	private String id = "";
	boolean fromNotification = false;
	int uploadID = 0;
	public Integer default_blog;
	public LinearLayout mainDashboard;
	Vector<?> loadedPosts, loadedPages, loadedComments;
	public Blog blog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dashboard);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

	}

	@Override
	protected void onResume() {
		super.onResume();
		final WordPressDB settingsDB = new WordPressDB(this);
		accounts = settingsDB.getAccounts(this);
		boolean eula = checkEULA();
		if (eula == false) {

			DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Accept so set that they've agreed to
					// the eula.
					WordPressDB eulaDB = new WordPressDB(Dashboard.this);
					eulaDB.setEULA(Dashboard.this);
					displayAccounts();
				}
			};

			DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish(); // goodbye!
				}
			};

			AlertUtil.showAlert(Dashboard.this, R.string.eula,
					R.string.eula_content, getString(R.string.accept),
					positiveListener, getString(R.string.decline),
					negativeListener);
		} else {
			displayAccounts();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {
			Bundle bundle = data.getExtras();
			String status = bundle.getString("returnStatus");
			if (status.equals("CANCEL") && WordPress.currentBlog != null) {
				finish();
			} else {
				WPTitleBar actionBar = (WPTitleBar) findViewById(R.id.actionBar);
				actionBar.reloadBlogs();
			}
		}
	}

	public boolean checkEULA() {
		WordPressDB eulaDB = new WordPressDB(this);
		boolean sEULA = eulaDB.checkEULA(this);

		return sEULA;

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, 0, 0, getResources().getText(R.string.add_account));
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(R.drawable.ic_menu_add);

		menu.add(0, 1, 0, getResources().getText(R.string.preferences));
		MenuItem menuItem2 = menu.findItem(1);
		menuItem2.setIcon(R.drawable.ic_menu_prefs);

		menu.add(0, 2, 0, getResources().getText(R.string.remove_account));
		MenuItem menuItem3 = menu.findItem(2);
		menuItem3.setIcon(R.drawable.ic_menu_close_clear_cancel);

		return true;
	}

	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(this, NewAccount.class);

			startActivityForResult(i, 0);

			return true;
		case 1:
			Intent i2 = new Intent(this, Preferences.class);

			startActivity(i2);
			finish();
			return true;
		case 2:
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					Dashboard.this);
			dialogBuilder.setTitle(getResources().getText(
					R.string.remove_account));
			dialogBuilder.setMessage(getResources().getText(
					R.string.sure_to_remove_account));
			dialogBuilder.setPositiveButton(getResources()
					.getText(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked Accept so set that they've
							// agreed to the eula.
							WordPressDB settingsDB = new WordPressDB(
									Dashboard.this);
							boolean deleteSuccess = settingsDB.deleteAccount(
									Dashboard.this, id);
							if (deleteSuccess) {
								Toast.makeText(
										Dashboard.this,
										getResources()
												.getText(
														R.string.blog_removed_successfully),
										Toast.LENGTH_SHORT).show();
								finish();
							} else {
								AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
										Dashboard.this);
								dialogBuilder.setTitle(getResources().getText(
										R.string.error));
								dialogBuilder
										.setMessage(getResources()
												.getText(
														R.string.could_not_remove_account));
								dialogBuilder.setPositiveButton("OK",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												// just close the dialog

											}
										});
								dialogBuilder.setCancelable(true);
								dialogBuilder.create().show();
							}

						}
					});
			dialogBuilder.setNegativeButton(
					getResources().getText(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// just close the window
						}
					});
			dialogBuilder.setCancelable(false);
			dialogBuilder.create().show();
			return true;
		}
		return false;

	}

	private void checkStats(final int numBlogs) {

		WordPressDB eulaDB = new WordPressDB(this);
		long lastStatsDate = eulaDB.getStatsDate(this);
		long now = System.currentTimeMillis();

		if ((now - lastStatsDate) > 604800000) { // works for first check too
			new Thread() {
				public void run() {
					uploadStats(numBlogs);
				}
			}.start();
			eulaDB.setStatsDate(this);
		}

	}

	private void uploadStats(int numBlogs) {

		// gather all of the device info
		WordPressDB eulaDB = new WordPressDB(this);
		String uuid = eulaDB.getUUID(this);
		if (uuid == "") {
			uuid = UUID.randomUUID().toString();
			eulaDB.updateUUID(this, uuid);
		}
		PackageManager pm = getPackageManager();
		String app_version = "";
		try {
			try {
				PackageInfo pi = pm.getPackageInfo("org.wordpress.android", 0);
				app_version = pi.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
				app_version = "N/A";
			}

			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String device_language = getResources().getConfiguration().locale
					.getLanguage();
			String mobile_country_code = tm.getNetworkCountryIso();
			String mobile_network_number = tm.getNetworkOperator();
			int network_type = tm.getNetworkType();

			// get the network type string
			String mobile_network_type = "N/A";
			switch (network_type) {
			case 0:
				mobile_network_type = "TYPE_UNKNOWN";
				break;
			case 1:
				mobile_network_type = "GPRS";
				break;
			case 2:
				mobile_network_type = "EDGE";
				break;
			case 3:
				mobile_network_type = "UMTS";
				break;
			case 4:
				mobile_network_type = "CDMA";
				break;
			case 5:
				mobile_network_type = "EVDO_0";
				break;
			case 6:
				mobile_network_type = "EVDO_A";
				break;
			case 7:
				mobile_network_type = "1xRTT";
				break;
			case 8:
				mobile_network_type = "HSDPA";
				break;
			case 9:
				mobile_network_type = "HSUPA";
				break;
			case 10:
				mobile_network_type = "HSPA";
				break;
			}

			String device_version = android.os.Build.VERSION.RELEASE;

			if (device_version == null) {
				device_version = "N/A";
			}
			int num_blogs = numBlogs;

			// post the data
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(
					"http://api.wordpress.org/androidapp/update-check/1.0/");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");

			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("device_uuid", uuid));
			pairs.add(new BasicNameValuePair("app_version", app_version));
			pairs.add(new BasicNameValuePair("device_language", device_language));
			pairs.add(new BasicNameValuePair("mobile_country_code",
					mobile_country_code));
			pairs.add(new BasicNameValuePair("mobile_network_number",
					mobile_network_number));
			pairs.add(new BasicNameValuePair("mobile_network_type",
					mobile_network_type));
			pairs.add(new BasicNameValuePair("device_version", device_version));
			pairs.add(new BasicNameValuePair("num_blogs", String
					.valueOf(num_blogs)));
			try {
				post.setEntity(new UrlEncodedFormEntity(pairs));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			try {
				client.execute(post);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void displayAccounts() {

		final WordPressDB settingsDB = new WordPressDB(this);
		accounts = settingsDB.getAccounts(this);

		checkStats(accounts.size());

		if (accounts.size() == 0) {
			// no account, load new account view
			Intent i = new Intent(Dashboard.this, NewAccount.class);
			startActivityForResult(i, 0);
		}
		else {
			WPTitleBar actionBar = (WPTitleBar) findViewById(R.id.actionBar);
			actionBar.showDashboard();
		}
	}
}