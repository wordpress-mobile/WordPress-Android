package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import org.json.JSONObject;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AlertUtil;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Dashboard extends WPActionBarActivity {
	private int id;
	boolean fromNotification = false;
	int uploadID = 0;
	public Integer default_blog;
	public LinearLayout mainDashboard;
	public Blog blog;
	private MenuItem refreshMenuItem;
	private TextView commentBadgeTextView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dashboard);
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
			if (status.equals("CANCEL") && WordPress.getCurrentBlog(Dashboard.this) == null) {
				finish();
			} else if (!status.equals("CANCEL")) {
				new refreshBlogContentTask().execute(true);
			}
		}
	}

	public boolean checkEULA() {
		return WordPress.wpDB.checkEULA(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		switchDashboardLayout(newConfig.orientation);
	}
	
	public void switchDashboardLayout(int orientation) {
		
		LayoutInflater inflater = LayoutInflater.from(Dashboard.this);
		RelativeLayout fullDashboard = (RelativeLayout)findViewById(R.id.full_dashboard);
		LinearLayout dashboard = (LinearLayout)findViewById(R.id.dashboard);
		int index = fullDashboard.indexOfChild(dashboard);
		fullDashboard.removeView(dashboard);
		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			dashboard = (LinearLayout) inflater.inflate(
					R.layout.dashboard_buttons_landscape, fullDashboard, false);
		else if (orientation == Configuration.ORIENTATION_PORTRAIT)
			dashboard = (LinearLayout) inflater.inflate(
					R.layout.dashboard_buttons_portrait, fullDashboard, false);

		fullDashboard.addView(dashboard, index);
		setupDashboardButtons();
	}

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.dashboard, menu);
	    refreshMenuItem = menu.findItem(R.id.menu_refresh);
	    return true;
	}

	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_refresh) {
			refreshMenuItem = item;
			new refreshBlogContentTask().execute(true);
			return true;
		} else if (itemId == R.id.menu_add_account) {
			Intent i = new Intent(this, NewAccount.class);
			startActivityForResult(i, 0);
			return true;
		} else if (itemId == R.id.menu_preferences) {
			Intent i2 = new Intent(this, Preferences.class);
			startActivity(i2);
			return true;
		} else if (itemId == R.id.menu_remove_account) {
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
		} else if (itemId == R.id.menu_about) {
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
			id = WordPress.getCurrentBlog(Dashboard.this).getId();
			setupDashboardButtons();			
			updateCommentBadge();
		}
	}
	
	private void setupDashboardButtons() {
		// dashboard button click handlers
		LinearLayout writeButton = (LinearLayout) findViewById(R.id.dashboard_newpost_btn);
		writeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, EditPost.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				startActivity(i);
			}
		});

		LinearLayout newPageButton = (LinearLayout) findViewById(R.id.dashboard_newpage_btn);
		newPageButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, EditPost.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.putExtra("isPage", true);
				startActivity(i);
			}
		});

		LinearLayout postsButton = (LinearLayout) findViewById(R.id.dashboard_posts_btn);
		postsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, Posts.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(i);
				
			}
		});

		LinearLayout pagesButton = (LinearLayout) findViewById(R.id.dashboard_pages_btn);
		pagesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, Posts.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.putExtra("viewPages", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(i);
				
			}
		});

		LinearLayout commentsButton = (LinearLayout) findViewById(R.id.dashboard_comments_btn);
		commentsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, Comments.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(i);
				
			}
		});

		LinearLayout statsButton = (LinearLayout) findViewById(R.id.dashboard_stats_btn);
		statsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, ViewWebStats.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(i);
				
			}
		});

		LinearLayout settingsButton = (LinearLayout) findViewById(R.id.dashboard_settings_btn);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Dashboard.this, Settings.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(i);
				
			}
		});

		LinearLayout readButton = (LinearLayout) findViewById(R.id.dashboard_subs_btn);
		readButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int readerBlogID = WordPress.wpDB.getWPCOMBlogID();
				if (WordPress.currentBlog.isDotcomFlag()) {
					Intent i = new Intent(Dashboard.this, WPCOMReaderPager.class);
					i.putExtra("id", readerBlogID);
					startActivity(i);
					
				} else {
					Intent i = new Intent(Dashboard.this, Read.class);
					i.putExtra("loadAdmin", true);
					startActivity(i);
					
				}
			}
		});

		LinearLayout picButton = (LinearLayout) findViewById(R.id.dashboard_quickphoto_btn);
		picButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PackageManager pm = Dashboard.this.getPackageManager();
				Intent i = new Intent(Dashboard.this, EditPost.class);
				if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
					i.putExtra("option", "newphoto");
				else
					i.putExtra("option", "photolibrary");
				i.putExtra("isNew", true);
				startActivity(i);
				
			}
		});

		LinearLayout videoButton = (LinearLayout) findViewById(R.id.dashboard_quickvideo_btn);
		videoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PackageManager pm = Dashboard.this.getPackageManager();
				Intent i = new Intent(Dashboard.this, EditPost.class);
				if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
					i.putExtra("option", "newvideo");
				else
					i.putExtra("option", "videolibrary");
				i.putExtra("isNew", true);
				startActivity(i);
				
			}
		});

		commentBadgeTextView = (TextView) findViewById(R.id.comment_badge_text);
		updateCommentBadge();
		updateReadButton();

	}
	
	public void updateCommentBadge() {
		if (WordPress.currentBlog != null) {
			int commentCount = WordPress.currentBlog
					.getUnmoderatedCommentCount(this);
			FrameLayout commentBadge = (FrameLayout) findViewById(R.id.comment_badge_frame);
			if (commentCount > 0) {
				commentBadge.setVisibility(View.VISIBLE);
			} else {
				commentBadge.setVisibility(View.GONE);
			}

			commentBadgeTextView.setText(String.valueOf(commentCount));

		}
	}
	
	private void updateReadButton() {
		if (WordPress.currentBlog == null)
			return;
		TextView readButtonText = (TextView) findViewById(R.id.read_button_text);
		ImageView readButtonImage = (ImageView) findViewById(R.id.read_button_image);
		if (WordPress.currentBlog.isDotcomFlag()){
			readButtonText.setText(getResources().getText(R.string.reader));
			readButtonImage.setImageDrawable(getResources().getDrawable(R.drawable.dashboard_icon_subs));
		}
		else {
			readButtonText.setText(getResources().getText(R.string.wp_admin));
			readButtonImage.setImageDrawable(getResources().getDrawable(R.drawable.dashboard_icon_wp));
		}	
	}

	private class refreshBlogContentTask extends AsyncTask<Boolean, Void, Boolean> {

		// refreshes blog level info (WP version number) and stuff related to
		// theme (available post types, recent comments etc)
		@Override
		protected void onPreExecute() {
			startAnimatingRefreshButton(refreshMenuItem);
		}

		@Override
		protected Boolean doInBackground(Boolean... params) {
			boolean commentsOnly = params[0];
			Blog blog = WordPress.getCurrentBlog(Dashboard.this);
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
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (!isFinishing() && refreshMenuItem != null) {
				stopAnimatingRefreshButton(refreshMenuItem);
			}
		}

	}

	@Override
	public void onBlogChanged() {
		try {
			blog = new Blog(id, Dashboard.this);
		} catch (Exception e) {
			Toast.makeText(Dashboard.this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
			finish();
		}
		updateCommentBadge();
		new refreshBlogContentTask().execute(false);
	}
}