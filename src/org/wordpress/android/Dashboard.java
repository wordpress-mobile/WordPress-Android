package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import org.json.JSONObject;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.WPTitleBar;
import org.wordpress.android.util.WPTitleBar.OnBlogChangedListener;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Dashboard extends Activity {
	private int id;
	boolean fromNotification = false;
	int uploadID = 0;
	public Integer default_blog;
	public LinearLayout mainDashboard;
	public Blog blog;
	WPTitleBar titleBar;

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
		boolean eula = checkEULA();
		if (eula == false) {

			DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Accept so set that they've agreed to
					// the eula.
					WordPress.wpDB.setEULA(Dashboard.this);
					displayAccounts();
				}
			};

			DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish(); // goodbye!
				}
			};

			AlertUtil.showAlert(Dashboard.this, R.string.eula, R.string.eula_content, getString(R.string.accept), positiveListener,
					getString(R.string.decline), negativeListener);
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
			if (status.equals("CANCEL") && WordPress.currentBlog == null) {
				finish();
			} else if (!status.equals("CANCEL")) {
				if (titleBar == null)
					titleBar = (WPTitleBar) findViewById(R.id.dashboardActionBar);
				titleBar.reloadBlogs();
				titleBar.updateBlogSelector(true);
				titleBar.startRotatingRefreshIcon();
				new refreshBlogContentTask().execute(true);
			}
		}
	}

	public boolean checkEULA() {
		boolean sEULA = WordPress.wpDB.checkEULA(this);

		return sEULA;

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (titleBar != null)
			titleBar.switchDashboardLayout(newConfig.orientation);
	}

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, 0, 0, getResources().getText(R.string.add_account));
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(android.R.drawable.ic_menu_add);

		menu.add(0, 1, 0, getResources().getText(R.string.preferences));
		MenuItem menuItem2 = menu.findItem(1);
		menuItem2.setIcon(android.R.drawable.ic_menu_preferences);

		menu.add(0, 2, 0, getResources().getText(R.string.remove_account));
		MenuItem menuItem3 = menu.findItem(2);
		menuItem3.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		menu.add(0, 3, 0, getResources().getText(R.string.about));
		MenuItem menuItem4 = menu.findItem(3);
		menuItem4.setIcon(android.R.drawable.ic_menu_info_details);

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
			return true;
		case 2:
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Dashboard.this);
			dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
			dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
			dialogBuilder.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					boolean deleteSuccess = WordPress.wpDB.deleteAccount(Dashboard.this, id);
					if (deleteSuccess) {
						Toast.makeText(Dashboard.this, getResources().getText(R.string.blog_removed_successfully), Toast.LENGTH_SHORT)
								.show();
						WordPress.wpDB.deleteLastBlogID();
						WordPress.currentBlog = null;
						titleBar.reloadBlogs();
						displayAccounts();
					} else {
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Dashboard.this);
						dialogBuilder.setTitle(getResources().getText(R.string.error));
						dialogBuilder.setMessage(getResources().getText(R.string.could_not_remove_account));
						dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// just close the dialog

							}
						});
						dialogBuilder.setCancelable(true);
						dialogBuilder.create().show();
					}

				}
			});
			dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// just close the window
				}
			});
			dialogBuilder.setCancelable(false);
			dialogBuilder.create().show();
			return true;
		case 3:
			Intent intent = new Intent(this, About.class);
			startActivity(intent);
			return true;
		}
		return false;

	}

	public void displayAccounts() {

		Vector<?> accounts = WordPress.wpDB.getAccounts(this);

		if (accounts.size() == 0) {
			// no account, load new account view
			Intent i = new Intent(Dashboard.this, NewAccount.class);
			startActivityForResult(i, 0);
		} else {
			id = WordPress.currentBlog.getId();
			titleBar = (WPTitleBar) findViewById(R.id.dashboardActionBar);
			titleBar.isHome = true;
			titleBar.refreshBlog();
			titleBar.updateBlogSelector(true);
			titleBar.showDashboard(600);

			titleBar.setOnBlogChangedListener(new OnBlogChangedListener() {
				// user selected new blog in the title bar
				@Override
				public void OnBlogChanged() {

					id = WordPress.currentBlog.getId();
					try {
						blog = new Blog(id, Dashboard.this);
					} catch (Exception e) {
						Toast.makeText(Dashboard.this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
						finish();
					}
					titleBar.startRotatingRefreshIcon();
					new refreshBlogContentTask().execute(false);
				}
			});

			titleBar.refreshButton.setOnClickListener(new ImageButton.OnClickListener() {
				public void onClick(View v) {

					titleBar.startRotatingRefreshIcon();
					new refreshBlogContentTask().execute(false);
				}
			});
			
			new refreshBlogContentTask().execute(false);

		}
	}

	private class refreshBlogContentTask extends AsyncTask<Boolean, Void, Boolean> {

		// refreshes blog level info (WP version number) and stuff related to
		// theme (available post types, recent comments etc)
		@Override
		protected void onPostExecute(Boolean result) {
			if (!isFinishing()) {
				Thread action = new Thread() {
					public void run() {
						titleBar.stopRotatingRefreshIcon();
						titleBar.updateCommentBadge();
					}
				};
				runOnUiThread(action);
			}
		}

		@Override
		protected Boolean doInBackground(Boolean... params) {
			boolean commentsOnly = params[0];
			Blog blog = WordPress.currentBlog;
			XMLRPCClient client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

			if (!commentsOnly) {
				// check the WP number if self-hosted
				HashMap<String, String> hPost = new HashMap<String, String>();
				hPost.put("software_version", "software_version");
				hPost.put("post_thumbnail", "post_thumbnail");
				hPost.put("jetpack_client_id", "jetpack_client_id");
				Object[] vParams = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), hPost };
				Object versionResult = new Object();
				try {
					versionResult = (Object) client.call("wp.getOptions", vParams);
				} catch (XMLRPCException e) {
				}

				if (versionResult != null) {
					try {
						HashMap<?, ?> contentHash = (HashMap<?, ?>) versionResult;
						blog.setBlogOptions(new JSONObject(contentHash));
						// Software version
						if (!blog.isDotcomFlag()) {
							HashMap<?, ?> sv = (HashMap<?, ?>) contentHash.get("software_version");
							String wpVersion = sv.get("value").toString();
							if (wpVersion.length() > 0) {
								blog.setWpVersion(wpVersion);
							}
						}
						// Featured image support
						HashMap<?, ?> featuredImageHash = (HashMap<?, ?>) contentHash.get("post_thumbnail");
						if (featuredImageHash != null) {
							boolean featuredImageCapable = Boolean.parseBoolean(featuredImageHash.get("value").toString());
							blog.setFeaturedImageCapable(featuredImageCapable);
						} else {
							blog.setFeaturedImageCapable(false);
						}
						blog.save(Dashboard.this, "");
					} catch (Exception e) {
					}
				}

				// get theme post formats
				Vector<Object> args = new Vector<Object>();
				args.add(blog);
				args.add(Dashboard.this);
				new ApiHelper.getPostFormatsTask().execute(args);
			}

			// refresh the comments
			HashMap<String, Object> hPost = new HashMap<String, Object>();
			hPost.put("number", 30);
			Object[] commentParams = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), hPost };

			try {
				ApiHelper.refreshComments(Dashboard.this, commentParams);
			} catch (XMLRPCException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		}

	}
}