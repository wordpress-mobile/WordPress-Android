package org.wordpress.android.util;

import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.Comments;
import org.wordpress.android.Dashboard;
import org.wordpress.android.EditPost;
import org.wordpress.android.NewAccount;
import org.wordpress.android.Posts;
import org.wordpress.android.R;
import org.wordpress.android.Read;
import org.wordpress.android.Settings;
import org.wordpress.android.ViewComments;
import org.wordpress.android.ViewStats;
import org.wordpress.android.ViewWebStats;
import org.wordpress.android.WPCOMReaderPager;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.ImageHelper.BitmapDownloaderTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class WPTitleBar extends RelativeLayout {

	public CharSequence[] blogNames;
	public int[] blogIDs;
	public Vector<?> accounts;
	private Context context;
	TextView blogTitleTextView;
	public Button refreshButton;
	OnBlogChangedListener onBlogChangedListener = null;
	AlertDialog.Builder dialogBuilder;
	public boolean showPopoverOnLoad;
	public RelativeLayout rl;
	public LinearLayout dashboard;
	public boolean isShowingDashboard;
	public boolean isHome;
	TextView commentBadgeText;
	boolean isRotating = false;

	public WPTitleBar(final Context ctx, AttributeSet attrs) {
		super(ctx, attrs);

		context = ctx;

	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		initViews();
		updateBlogSelector(false);
	}

	public void updateBlogSelector(boolean isDashboard) {
		int blogCount = accounts.size();
		if (accounts.size() >= 1 && isDashboard)
			blogCount++;
		blogNames = new CharSequence[blogCount];
		blogIDs = new int[blogCount];
		for (int i = 0; i < blogCount; i++) {
			if ((blogCount - 1) == i && isDashboard) {
				blogNames[i] = "+ " + getResources().getText(R.string.add_account);
				blogIDs[i] = -1;
			} else {
				HashMap<?, ?> accountHash = (HashMap<?, ?>) accounts.get(i);
				String curBlogName = accountHash.get("url").toString();
				if (accountHash.get("blogName") != null)
					curBlogName = EscapeUtils.unescapeHtml(accountHash.get("blogName").toString());
				blogNames[i] = curBlogName;
				blogIDs[i] = Integer.valueOf(accountHash.get("id").toString());
				blogTitleTextView = (TextView) findViewById(R.id.blog_title);
			}
			
		}

		setupCurrentBlog();

		if (WordPress.currentBlog != null) {
			WordPress.wpDB.updateLastBlogID(WordPress.currentBlog.getId());
			updateBlavatarImage();
			updateCommentBadge();
			refreshButton = (Button) findViewById(R.id.action_refresh);

			blogTitleTextView.setText(EscapeUtils.unescapeHtml(WordPress.currentBlog
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
									if (blogIDs[pos] == -1) {
										Intent i = new Intent(context, NewAccount.class);
										((Dashboard) context).startActivityForResult(i, 0);
									}
									else {
										blogTitleTextView.setText(EscapeUtils.unescapeHtml(blogNames[pos].toString()));
										try {
											WordPress.currentBlog = new Blog(
													blogIDs[pos], context);
										} catch (Exception e) {
											e.printStackTrace();
										}
										WordPress.wpDB
										.updateLastBlogID(blogIDs[pos]);
										updateBlavatarImage();
										updateCommentBadge();
										updateReadButton();
										if (onBlogChangedListener != null) {
											onBlogChangedListener.OnBlogChanged();
										}
									}
								}

							});
					dialogBuilder.show();
				}
			});

			final ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);

			showDashboard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					if (dashboard.getVisibility() == View.GONE) {
						showDashboardOverlay(0);
						isShowingDashboard = true;
					} else {
						hideDashboardOverlay();
					}

				}
			});

			setupDashboardButtons();
		}
		
	}
	
	public void setupCurrentBlog() {
		
		if (WordPress.currentBlog != null)
			return;
		
		int lastBlogID = WordPress.wpDB.getLastBlogID(context);
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
				try {
					WordPress.currentBlog = new Blog(blogIDs[0], context);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	private void setupDashboardButtons() {
		// dashboard button click handlers
		LinearLayout writeButton = (LinearLayout) findViewById(R.id.dashboard_newpost_btn);
		writeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(context, EditPost.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				context.startActivity(i);
				hideOverlay();
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
				hideOverlay();
			}
		});

		LinearLayout postsButton = (LinearLayout) findViewById(R.id.dashboard_posts_btn);
		postsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(context, Posts.class);
				context.startActivity(i);
				hideOverlay();
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
				hideOverlay();
			}
		});

		LinearLayout commentsButton = (LinearLayout) findViewById(R.id.dashboard_comments_btn);
		commentsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(context, Comments.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				context.startActivity(i);
				hideOverlay();
			}
		});

		LinearLayout statsButton = (LinearLayout) findViewById(R.id.dashboard_stats_btn);
		statsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				Intent i = new Intent(context, ViewStats.class);
				Intent i = new Intent(context, ViewWebStats.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				context.startActivity(i);
				hideOverlay();
			}
		});

		LinearLayout settingsButton = (LinearLayout) findViewById(R.id.dashboard_settings_btn);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(context, Settings.class);
				i.putExtra("id", WordPress.currentBlog.getId());
				i.putExtra("isNew", true);
				context.startActivity(i);
				hideOverlay();
			}
		});

		LinearLayout readButton = (LinearLayout) findViewById(R.id.dashboard_subs_btn);
		readButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int readerBlogID = WordPress.wpDB.getWPCOMBlogID();
				if (WordPress.currentBlog.isDotcomFlag()) {
					Intent i = new Intent(context, WPCOMReaderPager.class);
					i.putExtra("id", readerBlogID);
					context.startActivity(i);
					hideOverlay();
				} else {
					Intent i = new Intent(context, Read.class);
					i.putExtra("loadAdmin", true);
					context.startActivity(i);
					hideOverlay();
				}
			}
		});

		LinearLayout picButton = (LinearLayout) findViewById(R.id.dashboard_quickphoto_btn);
		picButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PackageManager pm = context.getPackageManager();
				Intent i = new Intent(context, EditPost.class);
				if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
					i.putExtra("option", "newphoto");
				else
					i.putExtra("option", "photolibrary");
				i.putExtra("isNew", true);
				context.startActivity(i);
				hideOverlay();
			}
		});

		LinearLayout videoButton = (LinearLayout) findViewById(R.id.dashboard_quickvideo_btn);
		videoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PackageManager pm = context.getPackageManager();
				Intent i = new Intent(context, EditPost.class);
				if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
					i.putExtra("option", "newvideo");
				else
					i.putExtra("option", "videolibrary");
				i.putExtra("isNew", true);
				context.startActivity(i);
				hideOverlay();
			}
		});

		commentBadgeText = (TextView) findViewById(R.id.comment_badge_text);
		updateCommentBadge();
		updateReadButton();

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

	protected void hideOverlay() {
		if (!isHome) {
			hideDashboardOverlay();
		}

	}

	public void hideDashboardOverlay() {

		ImageButton showDashboardButton = (ImageButton) findViewById(R.id.home_small);
		showDashboardButton.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_home));

		Animation fadeOutAnimation = AnimationUtils.loadAnimation(context,
				R.anim.dashboard_hide);
		dashboard.startAnimation(fadeOutAnimation);
		dashboard.setVisibility(View.GONE);
		isShowingDashboard = false;

	}

	protected void showDashboardOverlay(long delay) {

		ImageButton showDashboardButton = (ImageButton) findViewById(R.id.home_small);
		showDashboardButton.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_home_active));

		dashboard.setVisibility(View.VISIBLE);
		Animation fadeInAnimation = AnimationUtils.loadAnimation(context,
				R.anim.dashboard_show);
		if (delay > 0)
			fadeInAnimation.setStartOffset(delay);
		dashboard.startAnimation(fadeInAnimation);
		isShowingDashboard = true;
	}

	public void showDashboard(final long delay) {
		final ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);
		if (dashboard == null)
			initViews();
		showDashboard.postDelayed(new Runnable() {
			public void run() {
				try {
					if (dashboard.getVisibility() == View.GONE) {
						showDashboardOverlay(delay);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0);

	}

	private void initViews() {
		accounts = WordPress.wpDB.getAccounts(context);
		
		if (dashboard == null)
			dashboard = (LinearLayout) findViewById(R.id.dashboard_overlay);
		
		commentBadgeText = (TextView) findViewById(R.id.comment_badge_text);

		blogNames = new CharSequence[accounts.size()];
		blogIDs = new int[accounts.size()];

	}

	private void updateBlavatarImage() {
		ImageView i = (ImageView) findViewById(R.id.blavatar_img);
		i.setImageDrawable(getResources().getDrawable(
				R.drawable.wp_logo_actionbar));

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
		initViews();
		updateBlogSelector(false);
	}

	// Listener for when user changes blog in the ActionBar
	public interface OnBlogChangedListener {
		public abstract void OnBlogChanged();
	}

	public void setOnBlogChangedListener(OnBlogChangedListener listener) {
		onBlogChangedListener = listener;
	}

	public void startRotatingRefreshIcon() {
		if (isRotating) return;
		isRotating = true;
		this.refreshButton.setEnabled(false);
		RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		anim.setInterpolator(new LinearInterpolator());
		anim.setRepeatCount(Animation.INFINITE);
		anim.setDuration(1400);
		ImageView iv = (ImageView) findViewById(R.id.refresh_icon);
		iv.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_refresh_active));
		iv.startAnimation(anim);
	}

	public void stopRotatingRefreshIcon() {
		isRotating = false;
		this.refreshButton.setEnabled(true);
		ImageView iv = (ImageView) findViewById(R.id.refresh_icon);
		iv.setImageDrawable(getResources().getDrawable(
				R.drawable.icon_titlebar_refresh));
		iv.clearAnimation();
	}

	public void updateCommentBadge() {
		if (WordPress.currentBlog != null) {
			int commentCount = WordPress.currentBlog
					.getUnmoderatedCommentCount(context);
			FrameLayout commentBadge = (FrameLayout) findViewById(R.id.comment_badge_frame);
			if (commentCount > 0) {
				commentBadge.setVisibility(View.VISIBLE);
			} else {
				commentBadge.setVisibility(View.GONE);
			}

			commentBadgeText.setText(String.valueOf(commentCount));

		}
	}

	public void switchDashboardLayout(int orientation) {

		if (dashboard == null)
			initViews();
		
		LayoutInflater inflater = LayoutInflater.from(context);
		ViewGroup parent = (ViewGroup) dashboard.getParent();
		int index = parent.indexOfChild(dashboard);
		parent.removeView(dashboard);
		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			dashboard = (LinearLayout) inflater.inflate(
					R.layout.dashboard_buttons_landscape, parent, false);
		else if (orientation == Configuration.ORIENTATION_PORTRAIT)
			dashboard = (LinearLayout) inflater.inflate(
					R.layout.dashboard_buttons_portrait, parent, false);

		parent.addView(dashboard, index);
		if (isShowingDashboard)
			dashboard.setVisibility(View.VISIBLE);
		setupDashboardButtons();

	}

	public void refreshBlog() {
		blogTitleTextView.setText(EscapeUtils.unescapeHtml(WordPress.currentBlog.getBlogName()));
		updateBlavatarImage();
	}
}
