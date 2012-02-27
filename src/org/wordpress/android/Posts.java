package org.wordpress.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.wordpress.android.ViewPostFragment.OnDetailPostActionListener;
import org.wordpress.android.ViewPosts.OnPostActionListener;
import org.wordpress.android.ViewPosts.OnPostSelectedListener;
import org.wordpress.android.ViewPosts.OnRefreshListener;
import org.wordpress.android.models.Post;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class Posts extends FragmentActivity implements OnPostSelectedListener,
		OnRefreshListener, OnPostActionListener, OnDetailPostActionListener {

	private WPTitleBar titleBar;
	private ViewPosts postList;
	private int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
	public static int POST_DELETE = 0, POST_SHARE = 1, POST_EDIT = 2, POST_CLEAR = 3;
	public ProgressDialog loadingDialog;
	public boolean isPage = false;
	public String errorMsg = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.posts);

		FragmentManager fm = getSupportFragmentManager();
		postList = (ViewPosts) fm.findFragmentById(R.id.postList);
		postList.setListShown(true);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			isPage = extras.getBoolean("viewPages");
			String errorMessage = extras.getString("errorMessage");
			if (errorMessage != null) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						Posts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.error));
				dialogBuilder.setMessage(errorMessage);
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
		}

		WordPress.currentPost = null;

		titleBar = (WPTitleBar) findViewById(R.id.postsActionBar);
		titleBar.refreshButton
				.setOnClickListener(new ImageButton.OnClickListener() {
					public void onClick(View v) {

						postList.refreshPosts(false);

					}
				});

		titleBar.setOnBlogChangedListener(new OnBlogChangedListener() {
			// user selected new blog in the title bar
			@Override
			public void OnBlogChanged() {

				FragmentManager fm = getSupportFragmentManager();
				ViewPostFragment f = (ViewPostFragment) fm
						.findFragmentById(R.id.postDetail);
				if (f == null) {
					fm.popBackStack();
				}

				attemptToSelectPost();
				postList.loadPosts(false);

			}
		});
		
	     WordPress.setOnPostUploadedListener(new WordPress.OnPostUploadedListener(){

			@Override
			public void OnPostUploaded() {
				if (isFinishing())
					return;
				
				FragmentManager fm = getSupportFragmentManager();
				ViewPostFragment f = (ViewPostFragment) fm
						.findFragmentById(R.id.postDetail);
				if (f == null) {
					fm.popBackStack();
				}

				attemptToSelectPost();
				postList.refreshPosts(false);
			}
	     
	     });

		attemptToSelectPost();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);
		attemptToSelectPost();
		postList.loadPosts(false);
		if (WordPress.postsShouldRefresh) {
			postList.refreshPosts(false);
			WordPress.postsShouldRefresh = false;
		}
		
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Bundle extras = intent.getExtras();
		if (extras != null) {
			isPage = extras.getBoolean("viewPages");
			postList.isPage = isPage;
			String errorMessage = extras.getString("errorMessage");
			if (errorMessage != null) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						Posts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.error));
				dialogBuilder.setMessage(errorMessage);
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
		}
		else {
			isPage = false;
			postList.isPage = isPage;
		}

		titleBar.refreshBlog();
		attemptToSelectPost();
		postList.loadPosts(false);

	}

	@Override
	protected void onStop() {
		super.onStop();
		if (postList.getPostsTask != null)
			postList.getPostsTask.cancel(true);
	}

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		if (isPage)
			menu.add(0, 0, 0, getResources().getText(R.string.new_page));
		else
			menu.add(0, 0, 0, getResources().getText(R.string.new_post));
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(android.R.drawable.ic_menu_add);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(this, EditPost.class);
			i.putExtra("id", WordPress.currentBlog.getId());
			i.putExtra("isNew", true);
			if (isPage)
				i.putExtra("isPage", true);
			startActivity(i);
			return true;
		}
		return false;

	}

	private void attemptToSelectPost() {

		FragmentManager fm = getSupportFragmentManager();
		ViewPostFragment f = (ViewPostFragment) fm
				.findFragmentById(R.id.postDetail);

		if (f != null && f.isInLayout()) {
			postList.shouldSelectAfterLoad = true;
		}

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
	public void onPostSelected(Post post) {
		FragmentManager fm = getSupportFragmentManager();
		ViewPostFragment f = (ViewPostFragment) fm
				.findFragmentById(R.id.postDetail);

		if (post != null) {

			WordPress.currentPost = post;
			if (f == null || !f.isInLayout()) {
				FragmentTransaction ft = fm.beginTransaction();
				ft.hide(postList);
				f = new ViewPostFragment();
				ft.add(R.id.postDetailFragmentContainer, f);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.addToBackStack(null);
				ft.commit();
			} else {
				f.loadPost(post);
			}
		}
	}

	@Override
	public void onRefresh(boolean start) {
		if (start) {
			titleBar.startRotatingRefreshIcon();
		} else {
			titleBar.stopRotatingRefreshIcon();
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		loadingDialog = new ProgressDialog(this);
		if (id == ID_DIALOG_DELETING) {
			loadingDialog.setTitle(getResources().getText(
					(isPage) ? R.string.delete_page : R.string.delete_post));
			loadingDialog.setMessage(getResources().getText(
					(isPage) ? R.string.attempt_delete_page
							: R.string.attempt_delete_post));
			loadingDialog.setCancelable(false);
			return loadingDialog;
		} else if (id == ID_DIALOG_SHARE) {
			loadingDialog.setTitle(getResources().getText(R.string.share_url));
			loadingDialog.setMessage(getResources().getText(
					R.string.attempting_fetch_url));
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
	}

	public class deletePostTask extends AsyncTask<Post, Void, Boolean> {

		Post post;

		@Override
		protected void onPreExecute() {
			// pop out of the detail view if on a smaller screen
			FragmentManager fm = getSupportFragmentManager();
			ViewPostFragment f = (ViewPostFragment) fm
					.findFragmentById(R.id.postDetail);
			if (f == null) {
				fm.popBackStack();
			}
			showDialog(ID_DIALOG_DELETING);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			dismissDialog(ID_DIALOG_DELETING);
			attemptToSelectPost();
			if (result) {
				Toast.makeText(
						Posts.this,
						getResources().getText(
								(isPage) ? R.string.page_deleted
										: R.string.post_deleted),
						Toast.LENGTH_SHORT).show();
				postList.refreshPosts(false);
			} else {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						Posts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.connection_error));
				dialogBuilder.setMessage(errorMsg);
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

		}

		@Override
		protected Boolean doInBackground(Post... params) {
			boolean result = false;
			post = params[0];
			XMLRPCClient client = new XMLRPCClient(
					WordPress.currentBlog.getUrl(),
					WordPress.currentBlog.getHttpuser(),
					WordPress.currentBlog.getHttppassword());

			Object[] postParams = { "", post.getPostid(),
					WordPress.currentBlog.getUsername(),
					WordPress.currentBlog.getPassword() };
			Object[] pageParams = { WordPress.currentBlog.getBlogId(),
					WordPress.currentBlog.getUsername(),
					WordPress.currentBlog.getPassword(), post.getPostid() };

			try {
				client.call((isPage) ? "wp.deletePage" : "blogger.deletePost",
						(isPage) ? pageParams : postParams);

				result = true;
			} catch (final XMLRPCException e) {
				errorMsg = e.getLocalizedMessage();
				result = false;
			}
			return result;
		}

	}

	public class shareURLTask extends AsyncTask<Post, Void, String> {

		Post post;

		@Override
		protected void onPreExecute() {
			showDialog(ID_DIALOG_SHARE);
		}

		@Override
		protected void onPostExecute(String shareURL) {
			dismissDialog(ID_DIALOG_SHARE);
			if (shareURL == null) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						Posts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.connection_error));
				dialogBuilder.setMessage(errorMsg);
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
			} else {
				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("text/plain");
				share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
				share.putExtra(Intent.EXTRA_TEXT, shareURL);
				startActivity(Intent.createChooser(share, getResources()
						.getText(R.string.share_url)));

			}

		}

		@Override
		protected String doInBackground(Post... params) {
			String result = null;
			post = params[0];
			XMLRPCClient client = new XMLRPCClient(
					WordPress.currentBlog.getUrl(),
					WordPress.currentBlog.getHttpuser(),
					WordPress.currentBlog.getHttppassword());

			Object versionResult = new Object();
			try {
				if (isPage) {
					Object[] vParams = { WordPress.currentBlog.getBlogId(),
							post.getPostid(),
							WordPress.currentBlog.getUsername(),
							WordPress.currentBlog.getPassword() };
					versionResult = (Object) client.call("wp.getPage", vParams);
				} else {
					Object[] vParams = { post.getPostid(),
							WordPress.currentBlog.getUsername(),
							WordPress.currentBlog.getPassword() };
					versionResult = (Object) client.call("metaWeblog.getPost",
							vParams);
				}
			} catch (XMLRPCException e) {
				errorMsg = e.getMessage();
				Log.d("WP", "Error", e);
				return null;
			}

			if (versionResult != null) {
				try {
					HashMap<?, ?> contentHash = (HashMap<?, ?>) versionResult;

					if ((isPage && !"publish".equals(contentHash.get(
							"page_status").toString()))
							|| (!isPage && !"publish".equals(contentHash.get(
									"post_status").toString()))) {
						if (isPage) {
							errorMsg = getResources().getText(
									R.string.page_not_published).toString();
						} else {
							errorMsg = getResources().getText(
									R.string.post_not_published).toString();
						}
						return null;
					} else {
						String postURL = contentHash.get("permaLink")
								.toString();
						String shortlink = getShortlinkTagHref(postURL);
						if (shortlink == null) {
							result = postURL;
						} else {
							result = shortlink;
						}
					}
				} catch (Exception e) {
					errorMsg = e.getMessage();
					Log.d("WP", "Error", e);
					return null;
				}
			}

			return result;
		}
	}

	private String getShortlinkTagHref(String urlString) {
		String html = getHTML(urlString);
		
		if (html != "") {
			try {
				int location = html.indexOf("http://wp.me");
				String shortlink = html.substring(location, location + 30);
				shortlink = shortlink.substring(0, shortlink.indexOf("'"));
				return shortlink;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null; // never found the shortlink tag
	}

	public String getHTML(String urlSource) {
	      URL url;
	      HttpURLConnection conn;
	      BufferedReader rd;
	      String line;
	      String result = ""; 
	      try {
	         url = new URL(urlSource);
	         conn = (HttpURLConnection) url.openConnection();
	         conn.setRequestMethod("GET");
	         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	         while ((line = rd.readLine()) != null) {
	            result += line;
	         }
	         rd.close();
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	      return result;
	   }

	@Override
	public void onPostAction(int action, final Post post) {
		if (postList.getPostsTask != null) {
			postList.getPostsTask.cancel(true);
			titleBar.stopRotatingRefreshIcon();
		}
		if (action == POST_DELETE) {
			if (post.isLocalDraft()) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						Posts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.delete_draft));
				dialogBuilder.setMessage(getResources().getText(
						R.string.delete_sure)
						+ " '" + post.getTitle() + "'?");
				dialogBuilder.setPositiveButton(
						getResources().getText(R.string.yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								post.delete();
								attemptToSelectPost();
								postList.loadPosts(false);
							}
						});
				dialogBuilder.setNegativeButton(
						getResources().getText(R.string.no),
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

			} else {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						Posts.this);
				dialogBuilder.setTitle(getResources().getText(
						(post.isPage()) ? R.string.delete_page
								: R.string.delete_post));
				dialogBuilder.setMessage(getResources().getText(
						(post.isPage()) ? R.string.delete_sure_page
								: R.string.delete_sure_post)
						+ " '" + post.getTitle() + "'?");
				dialogBuilder.setPositiveButton(
						getResources().getText(R.string.yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								new Posts.deletePostTask().execute(post);
							}
						});
				dialogBuilder.setNegativeButton(
						getResources().getText(R.string.no),
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
		} else if (action == POST_SHARE) {
			new Posts.shareURLTask().execute(post);
		} else if (action == POST_CLEAR) {
			FragmentManager fm = getSupportFragmentManager();
			ViewPostFragment f = (ViewPostFragment) fm
					.findFragmentById(R.id.postDetail);
			if (f != null) {
				f.clearContent();
			}
		}

	}

	@Override
	public void onDetailPostAction(int action, Post post) {

		onPostAction(action, post);

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		titleBar.switchDashboardLayout(newConfig.orientation);

	}
}
