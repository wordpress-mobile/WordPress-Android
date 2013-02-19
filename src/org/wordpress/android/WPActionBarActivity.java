package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import net.simonvt.menudrawer.MenuDrawer;

import org.wordpress.android.models.Blog;
import org.wordpress.android.util.EscapeUtils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Base class for Activities that include a standard action bar and menu drawer.
 */
public abstract class WPActionBarActivity extends SherlockFragmentActivity
	implements ActionBar.OnNavigationListener {

	protected MenuDrawer menuDrawer;
	private static int[] blogIDs;
	protected boolean isAnimatingRefreshButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		String[] blogNames = getBlogNames(this);
		SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), R.layout.sherlock_spinner_dropdown_item, blogNames);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
		setupCurrentBlog();
		Blog currentBlog = WordPress.getCurrentBlog(this);
		if (currentBlog != null) {
			for (int i = 0; i < blogIDs.length; i++) {
				if (blogIDs[i] == currentBlog.getId()) {
					actionBar.setSelectedNavigationItem(i);
					return;
				}
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (isAnimatingRefreshButton)
			isAnimatingRefreshButton = false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// update menu drawer, since the current blog may have changed
		if (menuDrawer != null) {
			updateMenuDrawer();
		}
	}

	/**
	 * Create a menu drawer and attach it to the activity.
	 *
	 * @param contentView {@link View} of the main content for the activity.
	 */
	protected void createMenuDrawer(int contentView) {
		menuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
		menuDrawer.setContentView(contentView);
		menuDrawer.setMenuView(R.layout.menu_drawer);

		// setup listeners for menu buttons

		LinearLayout postsButton = (LinearLayout) findViewById(R.id.menu_posts_btn);
		postsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(WPActionBarActivity.this, Posts.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout pagesButton = (LinearLayout) findViewById(R.id.menu_pages_btn);
		pagesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(WPActionBarActivity.this, Posts.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.putExtra("viewPages", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout commentsButton = (LinearLayout) findViewById(R.id.menu_comments_btn);
		commentsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(WPActionBarActivity.this, Comments.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout picButton = (LinearLayout) findViewById(R.id.menu_quickphoto_btn);
		picButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PackageManager pm = WPActionBarActivity.this.getPackageManager();
				Intent i = new Intent(WPActionBarActivity.this, EditPost.class);
				if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
					i.putExtra("option", "newphoto");
				} else {
					i.putExtra("option", "photolibrary");
				}
				i.putExtra("isNew", true);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout videoButton = (LinearLayout) findViewById(R.id.menu_quickvideo_btn);
		videoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PackageManager pm = WPActionBarActivity.this.getPackageManager();
				Intent i = new Intent(WPActionBarActivity.this, EditPost.class);
				if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
					i.putExtra("option", "newvideo");
				} else {
					i.putExtra("option", "videolibrary");
				}
				i.putExtra("isNew", true);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout statsButton = (LinearLayout) findViewById(R.id.menu_stats_btn);
		statsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(WPActionBarActivity.this, ViewWebStats.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout dashboardButton = (LinearLayout) findViewById(R.id.menu_dashboard_btn);
		dashboardButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(WPActionBarActivity.this, WebViewActivity.class);
				i.putExtra("loadAdmin", true);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout settingsButton = (LinearLayout) findViewById(R.id.menu_settings_btn);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(WPActionBarActivity.this, Settings.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				menuDrawer.closeMenu();
				startActivity(i);
			}
		});

		LinearLayout readButton = (LinearLayout) findViewById(R.id.menu_reader_btn);
		readButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int readerBlogID = WordPress.wpDB.getWPCOMBlogID();
				if (WordPress.currentBlog.isDotcomFlag()) {
					Intent i = new Intent(WPActionBarActivity.this, WPCOMReaderPager.class);
					i.putExtra("id", readerBlogID);
					menuDrawer.closeMenu();
					startActivity(i);
				}
			}
		});

		updateMenuDrawer();
	}

	/**
	 * Update all of the items in the menu drawer based on the current active blog.
	 */
	protected void updateMenuDrawer() {
		updateMenuCommentBadge();
		updateMenuReaderButton();
	}

	/**
	 * Update the comment badge in the menu drawer to reflect the number of unmoderated comments for
	 * the current active blog.
	 */
	protected void updateMenuCommentBadge() {
		if (WordPress.currentBlog != null) {
			int commentCount = WordPress.currentBlog.getUnmoderatedCommentCount(this);
			TextView commentBadge = (TextView) findViewById(R.id.comment_badge);
			if (commentCount > 0) {
				commentBadge.setVisibility(View.VISIBLE);
			} else {
				commentBadge.setVisibility(View.GONE);
			}

			commentBadge.setText(String.valueOf(commentCount));
		}
	}

	/**
	 * Update the reader button in the menu drawer to only be visible if the current active blog is
	 * for a WordPress.com account.
	 */
	protected void updateMenuReaderButton() {
		// hide Reader menu item if current blog is not a WordPress.com blog
		View readButton = findViewById(R.id.menu_reader_btn);
		if (WordPress.currentBlog != null && WordPress.currentBlog.isDotcomFlag()) {
			readButton.setVisibility(View.VISIBLE);
		} else {
			readButton.setVisibility(View.GONE);
		}
	}

	/**
	 * Called when the activity has detected the user's press of the back key. If the activity has a
	 * menu drawer attached that is opened or in the process of opening, the back button press
	 * closes it. Otherwise, the normal back action is taken.
	 */
	@Override
	public void onBackPressed() {
		if (menuDrawer != null) {
			final int drawerState = menuDrawer.getDrawerState();
			if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
				menuDrawer.closeMenu();
				return;
			}
		}

		super.onBackPressed();
	}

	private static String[] getBlogNames(Context context) {
		Vector<?> accounts = WordPress.wpDB.getAccounts(context);

		blogIDs = new int[accounts.size()];

		int blogCount = accounts.size();
		if (accounts.size() >= 1)
			blogCount++;
		String[] blogNames = new String[blogCount];
		blogIDs = new int[blogCount];
		for (int i = 0; i < blogCount; i++) {
			if ((blogCount - 1) == i) {
				blogNames[i] = "+ " + context.getResources().getText(R.string.add_account);
				blogIDs[i] = -1;
			} else {
				HashMap<?, ?> accountHash = (HashMap<?, ?>) accounts.get(i);
				String curBlogName = accountHash.get("url").toString();
				if (accountHash.get("blogName") != null)
					curBlogName = EscapeUtils.unescapeHtml(accountHash.get("blogName").toString());
				blogNames[i] = curBlogName;
				blogIDs[i] = Integer.valueOf(accountHash.get("id").toString());
				// blogTitleTextView = (TextView) findViewById(R.id.blog_title);
			}
		}
		return blogNames;
	}

	public void setupCurrentBlog() {

		if (WordPress.currentBlog != null)
			return;

		int lastBlogID = WordPress.wpDB.getLastBlogID(this);
		if (lastBlogID != -1) {
			try {
				boolean matchedID = false;
				for (int i = 0; i < blogIDs.length; i++) {
					if (blogIDs[i] == lastBlogID) {
						matchedID = true;
						WordPress.currentBlog = new Blog(blogIDs[i], this);
					}
				}
				if (!matchedID) {
					WordPress.currentBlog = new Blog(blogIDs[0], this);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (blogIDs.length > 0)
				try {
					WordPress.currentBlog = new Blog(blogIDs[0], this);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}
	
	@Override
	public boolean onNavigationItemSelected(int pos, long itemId) {
		if (blogIDs[pos] == -1) {
			Intent i = new Intent(this, NewAccount.class);
			startActivityForResult(i, 0);
		} else {
			try {
				WordPress.currentBlog = new Blog(blogIDs[pos], this);
				WordPress.wpDB.updateLastBlogID(blogIDs[pos]);
				onBlogChanged();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (menuDrawer != null) {
					menuDrawer.toggleMenu();
					return true;
				}
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This method is called when the user changes the active blog.
	 */
	public void onBlogChanged() {
		updateMenuDrawer();
	}

	public void startAnimatingRefreshButton(MenuItem refreshItem) {
		if (refreshItem != null && !isAnimatingRefreshButton) {
			isAnimatingRefreshButton = true;
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ImageView iv = (ImageView) inflater.inflate(getResources().getLayout(R.layout.menu_refresh_view), null);
			RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
					Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
					0.5f);
			anim.setInterpolator(new LinearInterpolator());
			anim.setRepeatCount(Animation.INFINITE);
			anim.setDuration(1400);
			iv.startAnimation(anim);
			refreshItem.setActionView(iv);
		}
	}
	
	public void stopAnimatingRefreshButton(MenuItem refreshItem) {
		isAnimatingRefreshButton = false;
		if (refreshItem != null && refreshItem.getActionView() != null) {
			refreshItem.getActionView().clearAnimation();
			refreshItem.setActionView(null);
		}
	}
}
