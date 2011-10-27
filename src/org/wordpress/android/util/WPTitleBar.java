package org.wordpress.android.util;

import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.Comments;
import org.wordpress.android.EditPost;
import org.wordpress.android.Posts;
import org.wordpress.android.R;
import org.wordpress.android.Settings;
import org.wordpress.android.ViewComments;
import org.wordpress.android.Read;
import org.wordpress.android.ViewStats;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.ImageHelper.BitmapDownloaderTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WPTitleBar extends RelativeLayout {

	public CharSequence[] blogNames;
	public int[] blogIDs;
	public Vector<?> accounts;
	private Context context;
	TextView blogTitle;
	public Button refreshButton;
	OnBlogChangedListener onBlogChangedListener = null;
	AlertDialog.Builder dialogBuilder;
	public boolean showPopoverOnLoad;
	public RelativeLayout rl;
	public LinearLayout dashboard;
	public boolean isShowingDashboard;
	TextView commentBadge;

	public WPTitleBar(final Context ctx, AttributeSet attrs) {
		super(ctx, attrs);

		context = ctx;

	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		final WordPressDB settingsDB = new WordPressDB(context);
		accounts = settingsDB.getAccounts(context);

		dashboard = (LinearLayout) findViewById(R.id.dashboard);
		commentBadge = (TextView) findViewById(R.id.comment_badge);
		blogNames = new CharSequence[accounts.size()];
		blogIDs = new int[accounts.size()];

		for (int i = 0; i < accounts.size(); i++) {
			HashMap<?, ?> defHash = (HashMap<?, ?>) accounts.get(i);
			String curBlogName = EscapeUtils.unescapeHtml(defHash.get(
					"blogName").toString());

			blogNames[i] = curBlogName;
			blogIDs[i] = Integer.valueOf(defHash.get("id").toString());

			blogTitle = (TextView) findViewById(R.id.blog_title);
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
			updateCommentBadge();
			refreshButton = (Button) findViewById(R.id.action_refresh);

			blogTitle.setText(EscapeUtils.unescapeHtml(WordPress.currentBlog
					.getBlogName()));

			rl = (RelativeLayout) findViewById(R.id.blogSelector);
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
									updateCommentBadge();
									if (onBlogChangedListener != null) {
										onBlogChangedListener.OnBlogChanged();
									}
								}
							});
					dialogBuilder.show();
				}
			});

			ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);

			showDashboard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					if (dashboard.getVisibility() == View.GONE) {
						showDashboardOverlay();
						isShowingDashboard = true;
					} else {
						hideDashboardOverlay();
					}

				}
			});

			// dashboard button click handlers
			LinearLayout writeButton = (LinearLayout) findViewById(R.id.dashboard_newpost_btn);
			writeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, EditPost.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("isNew", true);
					context.startActivity(i);
				}
			});

			LinearLayout newPageButton = (LinearLayout) findViewById(R.id.dashboard_newpage_btn);
			newPageButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, EditPost.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("isNew", true);
					i.putExtra("isPage", true);
					context.startActivity(i);
				}
			});

			LinearLayout postsButton = (LinearLayout) findViewById(R.id.dashboard_posts_btn);
			postsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, Posts.class);
					context.startActivity(i);
				}
			});

			LinearLayout pagesButton = (LinearLayout) findViewById(R.id.dashboard_pages_btn);
			pagesButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, Posts.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("isNew", true);
					i.putExtra("viewPages", true);
					context.startActivity(i);
				}
			});

			LinearLayout commentsButton = (LinearLayout) findViewById(R.id.dashboard_comments_btn);
			commentsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, Comments.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("isNew", true);
					context.startActivity(i);
				}
			});

			LinearLayout statsButton = (LinearLayout) findViewById(R.id.dashboard_stats_btn);
			statsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, ViewStats.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("isNew", true);
					context.startActivity(i);
				}
			});

			LinearLayout settingsButton = (LinearLayout) findViewById(R.id.dashboard_settings_btn);
			settingsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, Settings.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("isNew", true);
					context.startActivity(i);
				}
			});

			LinearLayout subsButton = (LinearLayout) findViewById(R.id.dashboard_subs_btn);
			subsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(context, Read.class);
					i.putExtra("id", WordPress.currentBlog.getId());
					i.putExtra("loadReader", true);
					context.startActivity(i);
				}
			});

			LinearLayout picButton = (LinearLayout) findViewById(R.id.dashboard_quickphoto_btn);
			picButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					PackageManager pm = context.getPackageManager();
					if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
						Intent i = new Intent(context, EditPost.class);
						i.putExtra("option", "newphoto");
						i.putExtra("isNew", true);
						context.startActivity(i);
					} else {
						Toast.makeText(context,
								getResources()
										.getText(R.string.no_camera_found),
								Toast.LENGTH_LONG);
					}
				}
			});

			LinearLayout videoButton = (LinearLayout) findViewById(R.id.dashboard_quickvideo_btn);
			videoButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					PackageManager pm = context.getPackageManager();
					if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
						Intent i = new Intent(context, EditPost.class);
						i.putExtra("option", "newvideo");
						i.putExtra("isNew", true);
						context.startActivity(i);
					} else {
						Toast.makeText(context,
								getResources()
										.getText(R.string.no_camera_found),
								Toast.LENGTH_LONG);
					}
				}
			});
		}
	}

	public void hideDashboardOverlay() {
		dashboard.setBackgroundColor(Color.parseColor("#00000000"));
		Animation fadeOutAnimation = AnimationUtils.loadAnimation(context,
				R.anim.dashboard_hide);
		dashboard.startAnimation(fadeOutAnimation);
		dashboard.setVisibility(View.GONE);
		isShowingDashboard = false;

	}

	protected void showDashboardOverlay() {
		dashboard.setVisibility(View.VISIBLE);
		Animation fadeInAnimation = AnimationUtils.loadAnimation(context,
				R.anim.dashboard_show);
		dashboard.startAnimation(fadeInAnimation);
		dashboard.setBackgroundColor(Color.parseColor("#AA000000"));
	}

	public void showDashboard() {
		final ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);

		showDashboard.postDelayed(new Runnable() {
			public void run() {
				if (dashboard.getVisibility() == View.GONE) {
					showDashboardOverlay();
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
		RotateAnimation anim = new RotateAnimation(0.0f, 180.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
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

	public void updateCommentBadge() {
		if (WordPress.currentBlog != null) {
			commentBadge.setText(String.valueOf(WordPress.currentBlog
					.getUnmoderatedCommentCount(context)));
		}
	}
}
