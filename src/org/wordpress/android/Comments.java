package org.wordpress.android;

import java.util.HashMap;

import org.wordpress.android.ViewCommentFragment.OnCommentStatusChangeListener;
import org.wordpress.android.ViewComments.OnAnimateRefreshButtonListener;
import org.wordpress.android.ViewComments.OnCommentSelectedListener;
import org.wordpress.android.ViewComments.OnContextCommentStatusChangeListener;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.WPTitleBar;
import org.wordpress.android.util.WPTitleBar.OnBlogChangedListener;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class Comments extends FragmentActivity implements
		OnCommentSelectedListener, OnCommentStatusChangeListener,
		OnAnimateRefreshButtonListener, OnContextCommentStatusChangeListener {

	private WPTitleBar titleBar;
	protected int id;
	public int ID_DIALOG_MODERATING = 1;
	public int ID_DIALOG_REPLYING = 2;
	public int ID_DIALOG_DELETING = 3;
	private XMLRPCClient client;
	public ProgressDialog pd;
	private ViewComments commentList;
	private boolean fromNotification = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comments);

		titleBar = (WPTitleBar) findViewById(R.id.commentsActionBar);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			fromNotification = extras.getBoolean("fromNotification");
			if (fromNotification) {
				try {
					WordPress.currentBlog = new Blog(extras.getInt("id"),
							Comments.this);
				} catch (Exception e) {
					Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
					finish();
				}
				titleBar.refreshBlog();
			}
		}

		FragmentManager fm = getSupportFragmentManager();
		commentList = (ViewComments) fm.findFragmentById(R.id.commentList);

		WordPress.currentComment = null;

		titleBar.refreshButton
				.setOnClickListener(new ImageButton.OnClickListener() {
					public void onClick(View v) {

						attemptToSelectComment();
						commentList.refreshComments(false, false, false);

					}
				});

		titleBar.setOnBlogChangedListener(new OnBlogChangedListener() {
			// user selected new blog in the title bar
			@Override
			public void OnBlogChanged() {

				FragmentManager fm = getSupportFragmentManager();
				ViewCommentFragment f = (ViewCommentFragment) fm
						.findFragmentById(R.id.commentDetail);
				if (f == null) {
					fm.popBackStack();
				}
				attemptToSelectComment();
				boolean commentsLoaded = commentList.loadComments(false, false);
				if (!commentsLoaded)
					commentList.refreshComments(false, false, false);

			}
		});

		attemptToSelectComment();
		if (fromNotification)
			commentList.refreshComments(false, false, false);

		/*
		 * titleBar.setOnBlogChangedListener(new OnBlogChangedListener() { //
		 * user selected new blog in the title bar
		 * 
		 * @Override public void OnBlogChanged() { // remove footer 'load more'
		 * button ListView listView = (ListView)
		 * findViewById(android.R.id.list); listView.removeFooterView(switcher);
		 * model.clear(); thumbs.notifyDataSetChanged();
		 * 
		 * id = WordPress.currentBlog.getId(); blog = new Blog(id,
		 * Comments.this); // query for comments and refresh view boolean
		 * loadedComments = loadComments(false, false);
		 * 
		 * if (!loadedComments) {
		 * 
		 * refreshComments(false, false, false); } } });
		 */

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);
		boolean commentsLoaded = commentList.loadComments(false, false);
		if (!commentsLoaded)
			commentList.refreshComments(false, false, false);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (commentList.getCommentsTask != null)
			commentList.getCommentsTask.cancel(true);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if (extras != null) {
			boolean fromNotification = false;
			fromNotification = extras.getBoolean("fromNotification");
			if (fromNotification) {
				try {
					WordPress.currentBlog = new Blog(extras.getInt("id"),
							Comments.this);
				} catch (Exception e) {
					Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
					finish();
				}
				titleBar.refreshBlog();
			}
		}

	}

	@Override
	public void onCommentSelected(Comment comment) {

		FragmentManager fm = getSupportFragmentManager();
		ViewCommentFragment f = (ViewCommentFragment) fm
				.findFragmentById(R.id.commentDetail);

		if (comment != null) {

			if (f == null || !f.isInLayout()) {
				WordPress.currentComment = comment;
				FragmentTransaction ft = fm.beginTransaction();
				ft.hide(commentList);
				f = new ViewCommentFragment();
				ft.add(R.id.commentDetailFragmentContainer, f);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.addToBackStack(null);
				ft.commit();
			} else {
				f.loadComment(comment);
			}
		}
	}

	@Override
	public void onCommentStatusChanged(final String status) {

		if (WordPress.currentComment != null) {

			String comment_id = WordPress.currentComment.commentID;

			if (status.equals("approve") || status.equals("hold")
					|| status.equals("spam")) {
				final int commentID = Integer.parseInt(comment_id);
				showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() {
						Looper.prepare();
						changeCommentStatus(status, commentID);
					}
				}.start();
			} else if (status.equals("delete")) {
				final int commentID_del = Integer.parseInt(comment_id);
				showDialog(ID_DIALOG_DELETING);
				// pop out of the detail view if on a smaller screen
				FragmentManager fm = getSupportFragmentManager();
				ViewCommentFragment f = (ViewCommentFragment) fm
						.findFragmentById(R.id.commentDetail);
				if (f == null) {
					fm.popBackStack();
				}
				new Thread() {
					public void run() {
						deleteComment(commentID_del);
					}
				}.start();
			} else if (status.equals("reply")) {

				Intent i = new Intent(Comments.this, ReplyToComment.class);
				i.putExtra("commentID", Integer.parseInt(comment_id));
				i.putExtra("postID", WordPress.currentComment.postID);
				startActivityForResult(i, 0);
			} else if (status.equals("clear")) {
				FragmentManager fm = getSupportFragmentManager();
				ViewCommentFragment f = (ViewCommentFragment) fm
						.findFragmentById(R.id.commentDetail);
				if (f != null) {
					f.clearContent();
				}
			}
			
		}
	}

	@SuppressWarnings("unchecked")
	private void changeCommentStatus(final String newStatus,
			final int selCommentID) {
		// for individual comment moderation
		String sSelCommentID = String.valueOf(selCommentID);
		WordPressDB db = new WordPressDB(Comments.this);
		client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
				WordPress.currentBlog.getHttpuser(),
				WordPress.currentBlog.getHttppassword());

		HashMap<String, String> contentHash, postHash = new HashMap<String, String>();
		contentHash = (HashMap<String, String>) commentList.allComments
				.get(sSelCommentID);
		postHash.put("status", newStatus);
		postHash.put("content", contentHash.get("comment"));
		postHash.put("author", contentHash.get("author"));
		postHash.put("author_url", contentHash.get("url"));
		postHash.put("author_email", contentHash.get("email"));

		Object[] params = { WordPress.currentBlog.getBlogId(),
				WordPress.currentBlog.getUsername(),
				WordPress.currentBlog.getPassword(), sSelCommentID, postHash };

		Object result = null;
		try {
			result = (Object) client.call("wp.editComment", params);
			boolean bResult = Boolean.parseBoolean(result.toString());
			if (bResult) {
				WordPress.currentComment.status = newStatus;
				commentList.model.set(WordPress.currentComment.position,
						WordPress.currentComment);
				db.updateCommentStatus(id, WordPress.currentComment.commentID,
						newStatus);
			}
			dismissDialog(ID_DIALOG_MODERATING);
			Thread action = new Thread() {
				public void run() {
					Toast.makeText(Comments.this,
							getResources().getText(R.string.comment_moderated),
							Toast.LENGTH_SHORT).show();
				}
			};
			runOnUiThread(action);
			Thread action2 = new Thread() {
				public void run() {
					commentList.thumbs.notifyDataSetChanged();
				}
			};
			runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_MODERATING);
			Thread action3 = new Thread() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							Comments.this);
					dialogBuilder.setTitle(getResources().getText(
							R.string.connection_error));
					dialogBuilder.setMessage(e.getLocalizedMessage());
					dialogBuilder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Just close the window.

								}
							});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()) {
						dialogBuilder.create().show();
					}
				}
			};
			runOnUiThread(action3);
		}
	}

	private void deleteComment(final int selCommentID) {
		// delete individual comment

		client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
				WordPress.currentBlog.getHttpuser(),
				WordPress.currentBlog.getHttppassword());

		Object[] params = { WordPress.currentBlog.getBlogId(),
				WordPress.currentBlog.getUsername(),
				WordPress.currentBlog.getPassword(), selCommentID };

		try {
			client.call("wp.deleteComment", params);
			dismissDialog(ID_DIALOG_DELETING);
			attemptToSelectComment();
			Thread action = new Thread() {
				public void run() {
					Toast.makeText(Comments.this,
							getResources().getText(R.string.comment_moderated),
							Toast.LENGTH_SHORT).show();
				}
			};
			runOnUiThread(action);
			Thread action2 = new Thread() {
				public void run() {
					pd = new ProgressDialog(Comments.this);
					commentList.refreshComments(false, true, false);
				}
			};
			runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_DELETING);
			Thread action3 = new Thread() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							Comments.this);
					dialogBuilder.setTitle(getResources().getText(
							R.string.connection_error));
					dialogBuilder.setMessage(e.getLocalizedMessage());
					dialogBuilder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Just close the window.

								}
							});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()) {
						dialogBuilder.create().show();
					}
				}
			};
			runOnUiThread(action3);
		}
	}

	private void replyToComment(final String postID, final int commentID,
			final String comment) {
		// reply to individual comment
		client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
				WordPress.currentBlog.getHttpuser(),
				WordPress.currentBlog.getHttppassword());

		HashMap<String, Object> replyHash = new HashMap<String, Object>();
		replyHash.put("comment_parent", commentID);
		replyHash.put("content", comment);
		replyHash.put("author", "");
		replyHash.put("author_url", "");
		replyHash.put("author_email", "");

		Object[] params = { WordPress.currentBlog.getBlogId(),
				WordPress.currentBlog.getUsername(),
				WordPress.currentBlog.getPassword(), Integer.valueOf(postID),
				replyHash };

		try {
			client.call("wp.newComment", params);
			dismissDialog(ID_DIALOG_REPLYING);
			Thread action = new Thread() {
				public void run() {
					Toast.makeText(Comments.this,
							getResources().getText(R.string.reply_added),
							Toast.LENGTH_SHORT).show();
				}
			};
			runOnUiThread(action);
			Thread action2 = new Thread() {
				public void run() {
					pd = new ProgressDialog(Comments.this); // to avoid
					// crash
					commentList.refreshComments(false, true, false);
				}
			};
			runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_REPLYING);
			Thread action3 = new Thread() {
				public void run() {
					
					Toast.makeText(Comments.this, getResources().getText(R.string.connection_error) + ": " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
					
					Intent i = new Intent(Comments.this, ReplyToComment.class);
					i.putExtra("commentID", commentID);
					i.putExtra("postID", WordPress.currentComment.postID);
					i.putExtra("comment", comment);
					startActivityForResult(i, 0);
				}
			};
			runOnUiThread(action3);

		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {

			Bundle extras = data.getExtras();

			switch (requestCode) {
			case 0:
				final String returnText = extras.getString("replyText");

				if (!returnText.equals("CANCEL")) {
					final String postID = extras.getString("postID");
					final int commentID = extras.getInt("commentID");
					showDialog(ID_DIALOG_REPLYING);

					new Thread(new Runnable() {
						public void run() {
							Looper.prepare();
							pd = new ProgressDialog(Comments.this);
							replyToComment(postID, commentID, returnText);
						}
					}).start();
				}

				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_MODERATING) {
			ProgressDialog loadingDialog = new ProgressDialog(Comments.this);
			if (commentList.checkedCommentTotal <= 1) {
				loadingDialog.setMessage(getResources().getText(
						R.string.moderating_comment));
			} else {
				loadingDialog.setMessage(getResources().getText(
						R.string.moderating_comments));
			}
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		} else if (id == ID_DIALOG_REPLYING) {
			ProgressDialog loadingDialog = new ProgressDialog(Comments.this);
			loadingDialog.setMessage(getResources().getText(
					R.string.replying_comment));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		} else if (id == ID_DIALOG_DELETING) {
			ProgressDialog loadingDialog = new ProgressDialog(Comments.this);
			if (commentList.checkedCommentTotal <= 1) {
				loadingDialog.setMessage(getResources().getText(
						R.string.deleting_comment));
			} else {
				loadingDialog.setMessage(getResources().getText(
						R.string.deleting_comments));
			}
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && titleBar.isShowingDashboard) {
			titleBar.hideDashboardOverlay();
			return false;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onAnimateRefreshButton(boolean start) {

		if (start) {
			titleBar.startRotatingRefreshIcon();
		} else {
			titleBar.stopRotatingRefreshIcon();
		}

	}

	private void attemptToSelectComment() {

		FragmentManager fm = getSupportFragmentManager();
		ViewCommentFragment f = (ViewCommentFragment) fm
				.findFragmentById(R.id.commentDetail);

		if (f != null && f.isInLayout()) {
			commentList.shouldSelectAfterLoad = true;
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		titleBar.switchDashboardLayout(newConfig.orientation);

	}
}
