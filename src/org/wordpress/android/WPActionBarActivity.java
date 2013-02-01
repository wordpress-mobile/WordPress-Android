package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.util.EscapeUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;

import org.wordpress.android.models.Blog;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class WPActionBarActivity extends SherlockFragmentActivity implements ActionBar.OnNavigationListener {

	private static int[] blogIDs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
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
	
	public void onBlogChanged() {
		// Overridden by activities that inherit this class
	}
	
	public void startAnimatingRefreshButton(MenuItem refreshItem) {
		if (refreshItem != null) {
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
		if (refreshItem.getActionView() != null) {
			refreshItem.getActionView().clearAnimation();
			refreshItem.setActionView(null);
		}
	}

}
