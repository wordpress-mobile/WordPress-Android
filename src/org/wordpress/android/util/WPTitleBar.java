package org.wordpress.android.util;

import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.ActionItem;
import org.wordpress.android.EditPost;
import org.wordpress.android.QuickDashboard;
import org.wordpress.android.R;
import org.wordpress.android.ViewComments;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.ImageHelper.BitmapDownloaderTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class WPTitleBar extends RelativeLayout {

	QuickDashboard qa;
	public CharSequence[] blogNames;
	public int[] blogIDs;
	public Vector<?> accounts;
	private Context context;
	TextView blogTitle;
	public Button refreshButton;
	OnBlogChangedListener onBlogChangedListener = null;
	AlertDialog.Builder dialogBuilder;
	public boolean showPopoverOnLoad;

	public WPTitleBar(final Context ctx, AttributeSet attrs) {
		super(ctx, attrs);

		context = ctx;
		
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		final WordPressDB settingsDB = new WordPressDB(context);
		accounts = settingsDB.getAccounts(context);

		blogNames = new CharSequence[accounts.size()];
		blogIDs = new int[accounts.size()];

		for (int i = 0; i < accounts.size(); i++) {
			HashMap<?, ?> defHash = (HashMap<?, ?>) accounts.get(i);
			String curBlogName = EscapeUtils.unescapeHtml(defHash.get(
					"blogName").toString());

			blogNames[i] = curBlogName;
			blogIDs[i] = Integer.valueOf(defHash.get("id").toString());

			ActionItem blogIA = new ActionItem();

			blogIA.setTitle(curBlogName);
			blogIA.setIcon(getResources().getDrawable(R.drawable.wp_logo));

			blogTitle = (TextView) findViewById(R.id.blog_title);
			RelativeLayout rl = (RelativeLayout) findViewById(R.id.blogSelector);
			rl.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					dialogBuilder = new AlertDialog.Builder(context);
					dialogBuilder.setTitle(getResources().getText(
							R.string.choose_blog));
					dialogBuilder.setItems(blogNames,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int pos) {
									blogTitle.setText(blogNames[pos]);
									WordPress.currentBlog = new Blog(
											blogIDs[pos], context);
									settingsDB.updateLastBlogID(context,
											blogIDs[pos]);
									updateBlavatarImage();
									if (onBlogChangedListener != null) {
										onBlogChangedListener.OnBlogChanged();
									}
								}
							});
					dialogBuilder.show();
				}
			});

		}

		int lastBlogID = settingsDB.getLastBlogID(context);
		if (lastBlogID != -1) {
			try {
				boolean matchedID = false;
				for (int i = 0; i < blogIDs.length; i++) {
					if (blogIDs[i] == lastBlogID) {
						matchedID = true;
						WordPress.currentBlog = new Blog(blogIDs[i], context);
					}
				}
				if (!matchedID) {
					WordPress.currentBlog = new Blog(blogIDs[0], context);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (blogIDs.length > 0)
				WordPress.currentBlog = new Blog(blogIDs[0], context);
		}

		if (WordPress.currentBlog != null) {
			updateBlavatarImage();

			refreshButton = (Button) findViewById(R.id.action_refresh);
			
			blogTitle.setText(EscapeUtils.unescapeHtml(WordPress.currentBlog
					.getBlogName()));

			qa = new QuickDashboard(context);
			
			ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);

			showDashboard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					qa.show(v);
				}
			});
		}
	}
	
	public void showDashboard() {
		final ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);
		
		showDashboard.postDelayed(new Runnable()
		{ 
		    public void run()
		    { 
		    	if (!qa.isShowing) {
		    		showDashboard.performClick();
		    		showDashboard.setSelected(true);
		    	}
		    }
		}, 0);
		
	}

	private void updateBlavatarImage() {
		ImageView i = (ImageView) findViewById(R.id.blavatar_img);
		i.setImageDrawable(getResources().getDrawable(R.drawable.wp_logo_home));

		String url = WordPress.currentBlog.getUrl();
		url = url.replace("http://", "");
		url = url.replace("https://", "");
		String[] urlSplit = url.split("/");
		url = urlSplit[0];
		url = "http://gravatar.com/blavatar/"
				+ ViewComments.getMd5Hash(url.trim()) + "?s=60&d=404";

		ImageHelper ih = new ImageHelper();
		BitmapDownloaderTask task = ih.new BitmapDownloaderTask(i);
		task.execute(url);
	}

	public void reloadBlogs() {
		onFinishInflate();
	}

	// Listener for when user changes blog in the ActionBar
	public interface OnBlogChangedListener {
		public abstract void OnBlogChanged();
	}

	public void setOnBlogChangedListener(OnBlogChangedListener listener) {
		onBlogChangedListener = listener;
	}
	
	public void startRotatingRefreshIcon() {
		RotateAnimation anim = new RotateAnimation(0.0f, 180.0f, Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
	    anim.setInterpolator(new LinearInterpolator());
	    anim.setRepeatCount(Animation.INFINITE);
	    anim.setDuration(800);
	    ImageView iv = (ImageView) findViewById(R.id.refresh_icon);
	    iv.startAnimation(anim);
	}
	
	public void stopRotatingRefreshIcon() {
	    ImageView iv = (ImageView) findViewById(R.id.refresh_icon);
	    iv.clearAnimation();
	}
}
