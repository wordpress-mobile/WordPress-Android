package org.wordpress.android.util;

import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.ActionItem;
import org.wordpress.android.EditPost;
import org.wordpress.android.QuickAction;
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

	QuickAction qa;
	public QuickAction qaBlogs;
	public CharSequence[] blogNames;
	public int[] blogIDs;
	public Vector<?> accounts;
	private Context context;
	TextView blogTitle;
	public Button refreshButton;
	OnBlogChangedListener onBlogChangedListener = null;
	AlertDialog.Builder dialogBuilder;

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

		qaBlogs = new QuickAction(context);
		qaBlogs.setAnimStyle(QuickAction.ANIM_AUTO);

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

			final ActionItem newpost = new ActionItem();

			newpost.setTitle("Add New Post");
			newpost.setIcon(getResources().getDrawable(R.drawable.posts_tab));

			final ActionItem newpage = new ActionItem();

			newpage.setTitle("Add New Page");
			newpage.setIcon(getResources().getDrawable(R.drawable.pages_tab));

			final ActionItem addOldPhoto = new ActionItem();
			addOldPhoto.setTitle("Add Image From Gallery");
			addOldPhoto.setIcon(getResources().getDrawable(R.drawable.media));

			final ActionItem takeNewPhoto = new ActionItem();
			takeNewPhoto.setTitle("Take Photo");
			takeNewPhoto.setIcon(getResources().getDrawable(R.drawable.media));

			final ActionItem addOldVideo = new ActionItem();
			addOldVideo.setTitle("Add Video from Gallery");
			addOldVideo.setIcon(getResources().getDrawable(R.drawable.media));

			final ActionItem takeNewVideo = new ActionItem();
			takeNewVideo.setTitle("Take Video");
			takeNewVideo.setIcon(getResources().getDrawable(R.drawable.media));

			qa = new QuickAction(context);
			qa.addActionItem(newpost);
			qa.addActionItem(newpage);
			qa.addActionItem(addOldPhoto);
			qa.addActionItem(takeNewPhoto);
			qa.addActionItem(addOldVideo);
			qa.addActionItem(takeNewVideo);
			qa.setAnimStyle(QuickAction.ANIM_AUTO);

			qa.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
				@Override
				public void onItemClick(int pos) {
					Intent i = new Intent(context, EditPost.class);

					switch (pos) {
					case 1:
						i.putExtra("isPage", true);
						break;
					case 2:
						i.putExtra("option", "photolibrary");
						break;
					case 3:
						i.putExtra("option", "newphoto");
						break;
					case 4:
						i.putExtra("option", "videolibrary");
						break;
					case 5:
						i.putExtra("option", "newvideo");
						break;
					}

					i.putExtra("isNew", true);
					context.startActivity(i);
				}
			});

			ImageButton showDashboard = (ImageButton) findViewById(R.id.home_small);

			showDashboard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					qa.show(v);
				}
			});
		}
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
