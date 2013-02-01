package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.SpinnerAdapter;

public class WordPress extends Application {
	
	public static String versionName;
	public static Blog currentBlog;
	public static Comment currentComment;
	public static Post currentPost;
	public static WordPressDB wpDB;
	public static OnPostUploadedListener onPostUploadedListener = null;
	public static boolean postsShouldRefresh;
	
	@Override
	public void onCreate() {
		versionName = getVersionName();
		wpDB = new WordPressDB(this);
		super.onCreate();
	}
	
	/**
	 * Get versionName from Manifest.xml
	 * @return versionName
	 */
	private String getVersionName(){
		PackageManager pm = getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
			return pi.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	}

	public interface OnPostUploadedListener {
		public abstract void OnPostUploaded();
	}
	
	public static void setOnPostUploadedListener(OnPostUploadedListener listener) {
		onPostUploadedListener = listener;
	}

	public static void postUploaded() {
		if (onPostUploadedListener != null) {
			try {
				onPostUploadedListener.OnPostUploaded();
			} catch (Exception e) {
				postsShouldRefresh = true;
			}
		} else {
			postsShouldRefresh = true;
		}
		
	}
	
	public static Blog getCurrentBlog(Context context) {
		if (currentBlog != null)
			return currentBlog;
		
		Vector<?> accounts = WordPress.wpDB.getAccounts(context);

		int[] blogIDs = new int[accounts.size()];
		
		int blogCount = accounts.size();
		if (accounts.size() >= 1)
			blogCount++;
		CharSequence[] blogNames = new CharSequence[blogCount];
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
				//blogTitleTextView = (TextView) findViewById(R.id.blog_title);
			}
			
		}
		
		int lastBlogID = WordPress.wpDB.getLastBlogID(context);
		if (lastBlogID != -1) {
			try {
				boolean matchedID = false;
				for (int i = 0; i < blogIDs.length; i++) {
					if (blogIDs[i] == lastBlogID) {
						matchedID = true;
						currentBlog = new Blog(blogIDs[i], context);
					}
				}
				if (!matchedID) {
					currentBlog = new Blog(blogIDs[0], context);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (blogIDs.length > 0)
				try {
					currentBlog = new Blog(blogIDs[0], context);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		return currentBlog;	
	}
}
