package org.wordpress.android;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Post;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

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
}
