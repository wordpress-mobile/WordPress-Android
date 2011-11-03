package org.wordpress.android;

import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class ViewComments extends ListFragment {
	private static final int[] IMAGE_IDS = { R.id.avatar };
	public ThumbnailAdapter thumbs = null;
	public ArrayList<Comment> model = null;
	private XMLRPCClient client;
	private String accountName = "", moderateErrorMsg = "";
	public int[] changedStatuses;
	public HashMap<String, HashMap<?, ?>> allComments = new HashMap<String, HashMap<?, ?>>();
	public int ID_DIALOG_MODERATING = 1;
	public int ID_DIALOG_REPLYING = 2;
	public int ID_DIALOG_DELETING = 3;
	public boolean initializing = true, shouldSelectAfterLoad = false;
	public int selectedID = 0, rowID = 0, numRecords = 0, id,
			totalComments = 0, commentsToLoad = 30, checkedCommentTotal = 0, selectedPosition;
	public ProgressDialog pd;
	private ViewSwitcher switcher;
	boolean loadMore = false, doInBackground = false, refreshOnly = false;
	private Vector<String> checkedComments;
	private Blog blog;
	Object[] commentParams;
	boolean dualView;
	private OnCommentSelectedListener onCommentSelectedListener;
	private OnAnimateRefreshButtonListener onAnimateRefreshButton;
	private OnContextCommentStatusChangeListener onCommentStatusChangeListener;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		id = WordPress.currentBlog.getId();
		blog = new Blog(id, getActivity().getApplicationContext());
	}

	@Override
	public void onActivityCreated(Bundle bundle) {
		super.onActivityCreated(bundle);

		// query for comments and refresh view
		/*boolean loadedComments = loadComments(false, false);

		if (!loadedComments) {

			refreshComments(false, false, false);
		}*/
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			onCommentSelectedListener = (OnCommentSelectedListener) activity;
			onAnimateRefreshButton = (OnAnimateRefreshButtonListener) activity;
			onCommentStatusChangeListener = (OnContextCommentStatusChangeListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.moderatecomments, container, false);

		// create the ViewSwitcher in the current context
		switcher = new ViewSwitcher(getActivity().getApplicationContext());
		Button footer = (Button) View.inflate(getActivity()
				.getApplicationContext(), R.layout.list_footer_btn, null);
		footer.setText(getResources().getText(R.string.load_more) + " "
				+ getResources().getText(R.string.tab_comments));

		footer.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				switcher.showNext();
				refreshComments(true, false, false);
			}
		});

		View progress = View.inflate(getActivity().getApplicationContext(),
				R.layout.list_footer_progress, null);

		switcher.addView(footer);
		switcher.addView(progress);

		/*
		 * if (fromNotification) // dismiss the notification { //
		 * NotificationManager nm = (NotificationManager) //
		 * getSystemService(NOTIFICATION_SERVICE); // nm.cancel(22 +
		 * Integer.valueOf(id)); // loadComments(false, false); }
		 */

		getActivity().setTitle(accountName + " - Moderate Comments");

		Button deleteComments = (Button) v.findViewById(R.id.bulkDeleteComment);

		deleteComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				getActivity().showDialog(ID_DIALOG_DELETING);
				new Thread() {
					public void run() {
						Looper.prepare();
						deleteComments();
					}
				}.start();

			}
		});

		Button approveComments = (Button) v
				.findViewById(R.id.bulkApproveComment);

		approveComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				getActivity().showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() {
						Looper.prepare();
						moderateComments("approve");
					}
				}.start();
			}
		});

		Button unapproveComments = (Button) v
				.findViewById(R.id.bulkUnapproveComment);

		unapproveComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				getActivity().showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() {
						Looper.prepare();
						moderateComments("hold");
					}
				}.start();
			}
		});

		Button spamComments = (Button) v.findViewById(R.id.bulkMarkSpam);

		spamComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				getActivity().showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() {
						Looper.prepare();
						moderateComments("spam");
					}
				}.start();
			}
		});
		return v;
	}

	protected void showOrHideBulkCheckBoxes() {
		ListView listView = getListView();
		int loopMax = 0;
		if (listView.getFooterViewsCount() >= 1) {
			// we don't want a checkmark on the footer view
			if (listView.getLastVisiblePosition() == thumbs.getCount()) {
				loopMax = listView.getChildCount() - 1;
			} else {
				loopMax = listView.getChildCount();
			}
		} else {
			loopMax = listView.getChildCount();
		}
		for (int i = 0; i < loopMax; i++) {
			RelativeLayout rl = (RelativeLayout) (View) listView.getChildAt(i)
					.findViewById(R.id.bulkEditGroup);
			showBulkCheckBoxes(rl);
		}

	}

	@SuppressWarnings("unchecked")
	protected void moderateComments(String newStatus) {
		// handles bulk moderation
		for (int i = 0; i < checkedComments.size(); i++) {
			if (checkedComments.get(i).toString().equals("true")) {

				client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
						blog.getHttppassword());

				Comment listRow = (Comment) getListView().getItemAtPosition(i);
				String curCommentID = listRow.commentID;

				HashMap<String, String> contentHash, postHash = new HashMap<String, String>();
				contentHash = (HashMap<String, String>) allComments.get(curCommentID);
				postHash.put("status", newStatus);
				postHash.put("content", contentHash.get("comment"));
				postHash.put("author", contentHash.get("author"));
				postHash.put("author_url", contentHash.get("url"));
				postHash.put("author_email", contentHash.get("email"));

				Object[] params = { blog.getBlogId(), blog.getUsername(),
						blog.getPassword(), curCommentID, postHash };

				Object result = null;
				try {
					result = (Object) client.call("wp.editComment", params);
					boolean bResult = Boolean.parseBoolean(result.toString());
					if (bResult) {
						checkedComments.set(i, "false");
						listRow.status = newStatus;
						model.set(i, listRow);
						WordPress.wpDB.updateCommentStatus(getActivity()
								.getApplicationContext(), id,
								listRow.commentID, newStatus);
					}
				} catch (XMLRPCException e) {
					moderateErrorMsg = e.getLocalizedMessage();
				}
			}
		}
		getActivity().dismissDialog(ID_DIALOG_MODERATING);
		Thread action = new Thread() {
			public void run() {
				if (moderateErrorMsg == "") {
					Toast.makeText(
							getActivity().getApplicationContext(),
							getResources().getText(R.string.comments_moderated),
							Toast.LENGTH_SHORT).show();
				} else {
					// there was an xmlrpc error
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					WPAlertDialogFragment alert = WPAlertDialogFragment
					        .newInstance(moderateErrorMsg);
					    alert.show(ft, "alert");
				}
			}
		};
		getActivity().runOnUiThread(action);
		if (moderateErrorMsg == "") {
			// no errors, refresh list
			checkedCommentTotal = 0;
			Thread action2 = new Thread() {
				public void run() {
					pd = new ProgressDialog(getActivity()
							.getApplicationContext()); // to avoid
					// crash
					showOrHideBulkCheckBoxes();
					hideModerationBar();
					thumbs.notifyDataSetChanged();
				}
			};
			getActivity().runOnUiThread(action2);
		}
	}

	protected void hideBulkCheckBoxes(RelativeLayout rl) {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		rl.startAnimation(set);
		rl.setVisibility(View.GONE);
		if (checkedCommentTotal > 0) {
			hideModerationBar();
		}
	}

	protected void showBulkCheckBoxes(RelativeLayout rl) {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		rl.setVisibility(View.VISIBLE);
		rl.startAnimation(set);
		if (checkedCommentTotal > 0) {
			showModerationBar();
		}
	}

	protected void deleteComments() {
		// bulk detete comments

		for (int i = 0; i < checkedComments.size(); i++) {
			if (checkedComments.get(i).toString().equals("true")) {

				client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
						blog.getHttppassword());

				Comment listRow = (Comment) getListView().getItemAtPosition(i);
				String curCommentID = listRow.commentID;

				Object[] params = { blog.getBlogId(), blog.getUsername(),
						blog.getPassword(), curCommentID };

				try {
					client.call("wp.deleteComment", params);
				} catch (final XMLRPCException e) {
					moderateErrorMsg = e.getLocalizedMessage();
				}
			}
		}
		getActivity().dismissDialog(ID_DIALOG_DELETING);
		Thread action = new Thread() {
			public void run() {
				if (moderateErrorMsg == "") {
					Toast.makeText(getActivity().getApplicationContext(),
							getResources().getText(R.string.comment_moderated),
							Toast.LENGTH_SHORT).show();
				} else {
					// error occured during delete request
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					WPAlertDialogFragment alert = WPAlertDialogFragment
					        .newInstance(moderateErrorMsg);
					    alert.show(ft, "alert");
				}
			}
		};
		getActivity().runOnUiThread(action);
		Thread action2 = new Thread() {
			public void run() {
				if (moderateErrorMsg == "") {
					pd = new ProgressDialog(getActivity()
							.getApplicationContext()); // to avoid
					// crash
					refreshComments(false, true, true);
				}
			}
		};
		getActivity().runOnUiThread(action2);
		checkedCommentTotal = 0;

	}

	@SuppressWarnings("unchecked")
	public boolean loadComments(boolean addMore, boolean refresh) {
		refreshOnly = refresh;
		String author, postID, commentID, comment, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
		if (!addMore) {
			Vector<?> loadedPosts = WordPress.wpDB.loadComments(getActivity()
					.getApplicationContext(), WordPress.currentBlog.getId());
			if (loadedPosts != null) {
				HashMap<Object, Object> countHash = new HashMap<Object, Object>();
				countHash = (HashMap<Object, Object>) loadedPosts.get(0);
				numRecords = Integer.parseInt(countHash.get("numRecords")
						.toString());
				if (refreshOnly) {
					if (model != null) {
						model.clear();
					}
				} else {
					model = new ArrayList<Comment>();
				}

				checkedComments = new Vector<String>();
				for (int i = 1; i < loadedPosts.size(); i++) {
					checkedComments.add(i - 1, "false");
					HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
					allComments.put(contentHash.get("commentID").toString(),
							contentHash);
					author = EscapeUtils.unescapeHtml(contentHash.get("author")
							.toString());
					commentID = contentHash.get("commentID").toString();
					postID = contentHash.get("postID").toString();
					comment = EscapeUtils.unescapeHtml(contentHash.get(
							"comment").toString());
					dateCreatedFormatted = contentHash.get(
							"commentDateFormatted").toString();
					status = contentHash.get("status").toString();
					authorEmail = EscapeUtils.unescapeHtml(contentHash.get(
							"email").toString());
					authorURL = EscapeUtils.unescapeHtml(contentHash.get("url")
							.toString());
					postTitle = EscapeUtils.unescapeHtml(contentHash.get(
							"postTitle").toString());

					if (model == null) {
						model = new ArrayList<Comment>();
					}

					// add to model
					model.add(new Comment(postID, commentID, i-1, author,
							dateCreatedFormatted, comment, status, postTitle,
							authorURL, authorEmail, URI
									.create("http://gravatar.com/avatar/"
											+ getMd5Hash(authorEmail.trim())
											+ "?s=60&d=identicon")));
				}

				if (!refreshOnly) {
					try {
						ThumbnailBus bus = new ThumbnailBus();
						thumbs = new ThumbnailAdapter(
								getActivity(),
								new CommentAdapter(),
								new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(
										null, null, 101, bus), IMAGE_IDS);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					ListView listView = this.getListView();
					listView.removeFooterView(switcher);
					if (loadedPosts.size() >= 30) {
						listView.addFooterView(switcher);
					}
					setListAdapter(thumbs);

					listView.setOnItemClickListener(new OnItemClickListener() {

						public void onItemClick(AdapterView<?> arg0, View view,
								int position, long id) {
							selectedPosition = position;
							Comment comment = model.get((int) id);
							onCommentSelectedListener
									.onCommentSelected(comment);
						}
					});

					listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

						public void onCreateContextMenu(ContextMenu menu,
								View v, ContextMenuInfo menuInfo) {
							AdapterView.AdapterContextMenuInfo info;
							try {
								info = (AdapterView.AdapterContextMenuInfo) menuInfo;
							} catch (ClassCastException e) {
								// Log.e(TAG, "bad menuInfo", e);
								return;
							}

							WordPress.currentComment = model.get(info.position);

							menu.setHeaderTitle(getResources().getText(
									R.string.comment_actions));
							menu.add(
									0,
									0,
									0,
									getResources().getText(
											R.string.mark_approved));
							menu.add(
									0,
									1,
									0,
									getResources().getText(
											R.string.mark_unapproved));
							menu.add(0, 2, 0,
									getResources().getText(R.string.mark_spam));
							menu.add(0, 3, 0,
									getResources().getText(R.string.reply));
							menu.add(0, 4, 0,
									getResources().getText(R.string.delete));
						}
					});
				} else {
					if (thumbs != null) {
						thumbs.notifyDataSetChanged();
					}
				}
				
				if (this.shouldSelectAfterLoad) {
					if (model != null) {
						if (model.size() > 0) {
							
							selectedPosition = 0;
							Comment aComment = model.get((int) 0);
							onCommentSelectedListener
									.onCommentSelected(aComment);
							 thumbs.notifyDataSetChanged();
							 
						}
					}
					shouldSelectAfterLoad = false;
				}
				
				return true;
			} else {
				return false;
			}
		} else {
			Vector<?> latestComments = WordPress.wpDB.loadMoreComments(getActivity()
					.getApplicationContext(), id, commentsToLoad);
			if (latestComments != null) {
				numRecords += latestComments.size();
				for (int i = latestComments.size(); i > 0; i--) {
					HashMap<?, ?> contentHash = (HashMap<?, ?>) latestComments.get(i - 1);
					allComments.put(contentHash.get("commentID").toString(),
							contentHash);
					author = EscapeUtils.unescapeHtml(contentHash.get("author")
							.toString());
					commentID = contentHash.get("commentID").toString();
					postID = contentHash.get("postID").toString();
					comment = EscapeUtils.unescapeHtml(contentHash.get(
							"comment").toString());
					dateCreatedFormatted = contentHash.get(
							"commentDateFormatted").toString();
					status = contentHash.get("status").toString();
					authorEmail = EscapeUtils.unescapeHtml(contentHash.get(
							"email").toString());
					authorURL = EscapeUtils.unescapeHtml(contentHash.get("url")
							.toString());
					postTitle = EscapeUtils.unescapeHtml(contentHash.get(
							"postTitle").toString());

					// add to model
					model.add(new Comment(postID, commentID, i, author,
							dateCreatedFormatted, comment, status, postTitle,
							authorURL, authorEmail, URI
									.create("http://gravatar.com/avatar/"
											+ getMd5Hash(authorEmail.trim())
											+ "?s=100&d=identicon")));
				}
				thumbs.notifyDataSetChanged();
			}
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public void refreshComments(final boolean more, final boolean refresh,
			final boolean background) {
		loadMore = more;
		refreshOnly = refresh;
		doInBackground = background;

		if (!loadMore && !doInBackground) {
			onAnimateRefreshButton.onAnimateRefreshButton(true);
		}
		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(),
				blog.getHttppassword());

		HashMap<String, Object> hPost = new HashMap<String, Object>();
		hPost.put("status", "");
		hPost.put("post_id", "");
		if (loadMore) {
			hPost.put("offset", numRecords);
		}
		if (totalComments != 0 && ((totalComments - numRecords) < 30)) {
			commentsToLoad = totalComments - numRecords;
			hPost.put("number", commentsToLoad);
		} else {
			hPost.put("number", 30);
		}

		Object[] params = { blog.getBlogId(), blog.getUsername(),
				blog.getPassword(), hPost };

		commentParams = params;
		new getRecentCommentsTask().execute();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	/*
	 * @Override public Object onRetainNonConfigurationInstance() { return
	 * (model); }
	 */

	private void goBlooey(Throwable t) {
		Log.e("WordPress", "Exception!", t);

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		WPAlertDialogFragment alert = WPAlertDialogFragment
		        .newInstance(t.toString());
		    alert.show(ft, "alert");
	}

	class CommentAdapter extends ArrayAdapter<Comment> {
		CommentAdapter() {
			super(getActivity().getApplicationContext(), R.layout.row, model);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			CommentEntryWrapper wrapper = null;

			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();

				row = inflater.inflate(R.layout.row, null);
				wrapper = new CommentEntryWrapper(row);
				row.setTag(wrapper);
			} else {
				wrapper = (CommentEntryWrapper) row.getTag();
			}
			Comment commentEntry = getItem(position);
			
			if (position == selectedPosition) {
				row.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_highlight_bg));
			} else if ("hold".equals(commentEntry.status)) {
				row.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.comment_pending_bg_selector));
			} else {
				row.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.list_bg_selector));
			}
			
			wrapper.populateFrom(commentEntry, position);

			return (row);
		}
	}

	class CommentEntryWrapper {
		private TextView name = null;
		private TextView emailURL = null;
		private TextView comment = null;
		private TextView status = null;
		private TextView postTitle = null;
		private ImageView avatar = null;
		private View row = null;
		private CheckBox bulkCheck = null;
		private RelativeLayout bulkEditGroup = null;

		CommentEntryWrapper(View row) {
			this.row = row;

		}

		void populateFrom(Comment s, final int position) {
			getName().setText(s.name);

			String fEmailURL = s.authorURL;
			// use the required email address if the commenter didn't leave a
			// url
			if (fEmailURL == "") {
				fEmailURL = s.emailURL;
			}

			getEmailURL().setText(fEmailURL);
			getComment().setText(s.comment);
			getPostTitle().setText(
					getResources().getText(R.string.on) + " " + s.postTitle);

			row.setId(Integer.valueOf(s.commentID));

			String prettyComment, textColor = "";

			if (s.status.equals("spam")) {
				prettyComment = getResources().getText(R.string.spam)
						.toString();
				textColor = "#FF0000";
			} else if (s.status.equals("hold")) {
				prettyComment = getResources().getText(R.string.unapproved)
						.toString();
				textColor = "#D54E21";
			} else {
				prettyComment = getResources().getText(R.string.approved)
						.toString();
				textColor = "#006505";
			}

			getBulkEditGroup().setVisibility(View.VISIBLE);

			getStatus().setText(prettyComment);
			getStatus().setTextColor(Color.parseColor(textColor));

			getBulkCheck().setChecked(
					Boolean.parseBoolean(checkedComments.get(position)
							.toString()));
			getBulkCheck().setTag(position);
			getBulkCheck().setOnClickListener(new OnClickListener() {

				public void onClick(View arg0) {
					checkedComments.set(position,
							String.valueOf(getBulkCheck().isChecked()));
					showOrHideModerateButtons();
				}
			});

			if (s.profileImageUrl != null) {
				try {
					getAvatar().setImageResource(R.drawable.placeholder);
					getAvatar().setTag(s.profileImageUrl.toString());
				} catch (Throwable t) {
					goBlooey(t);
				}
			}
		}

		TextView getName() {
			if (name == null) {
				name = (TextView) row.findViewById(R.id.name);
			}

			return (name);
		}

		TextView getEmailURL() {
			if (emailURL == null) {
				emailURL = (TextView) row.findViewById(R.id.email_url);
			}

			return (emailURL);
		}

		TextView getComment() {
			if (comment == null) {
				comment = (TextView) row.findViewById(R.id.comment);
			}

			return (comment);
		}

		TextView getStatus() {
			if (status == null) {
				status = (TextView) row.findViewById(R.id.status);
			}

			status.setTextSize(10);

			return (status);
		}

		TextView getPostTitle() {
			if (postTitle == null) {
				postTitle = (TextView) row.findViewById(R.id.postTitle);
			}

			return (postTitle);
		}

		ImageView getAvatar() {
			if (avatar == null) {
				avatar = (ImageView) row.findViewById(R.id.avatar);
			}

			return (avatar);
		}

		CheckBox getBulkCheck() {
			if (bulkCheck == null) {
				bulkCheck = (CheckBox) row.findViewById(R.id.bulkCheck);
			}

			return (bulkCheck);
		}

		RelativeLayout getBulkEditGroup() {
			if (bulkEditGroup == null) {
				bulkEditGroup = (RelativeLayout) row
						.findViewById(R.id.bulkEditGroup);
			}

			return (bulkEditGroup);
		}

		protected void showOrHideModerateButtons() {
			int previousTotal = checkedCommentTotal;
			checkedCommentTotal = 0;
			for (int i = 0; i < checkedComments.size(); i++) {
				if (checkedComments.get(i).equals("true")) {
					checkedCommentTotal++;
				}
			}
			if (checkedCommentTotal > 0 && previousTotal == 0) {
				showModerationBar();
			}
			if (checkedCommentTotal == 0 && previousTotal > 0) {

				hideModerationBar();

			}

		}
	}

	public static String getMd5Hash(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1, messageDigest);
			String md5 = number.toString(16);

			while (md5.length() < 32)
				md5 = "0" + md5;

			return md5;
		} catch (NoSuchAlgorithmException e) {
			Log.e("MD5", e.getLocalizedMessage());
			return null;
		}
	}

	public void hideModerationBar() {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		RelativeLayout moderationBar = (RelativeLayout) getActivity()
				.findViewById(R.id.moderationBar);
		moderationBar.clearAnimation();
		moderationBar.startAnimation(set);
		moderationBar.setVisibility(View.INVISIBLE);

	}

	public void showModerationBar() {
		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		RelativeLayout moderationBar = (RelativeLayout) getActivity()
				.findViewById(R.id.moderationBar);
		moderationBar.setVisibility(View.VISIBLE);
		moderationBar.startAnimation(set);
	}

	interface XMLRPCMethodCallback {
		void callFinished(Object[] result);
	}

	class XMLRPCMethod extends Thread {
		private String method;
		private Object[] params;
		private Handler handler;
		private XMLRPCMethodCallback callBack;

		public XMLRPCMethod(String method, XMLRPCMethodCallback callBack) {
			this.method = method;
			this.callBack = callBack;
			handler = new Handler();
		}

		public void call() {
			call(null);
		}

		public void call(Object[] params) {
			this.params = params;
			start();
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try {
				// get the total comments
				HashMap<Object, Object> countResult = new HashMap<Object, Object>();
				Object[] countParams = { blog.getBlogId(), blog.getUsername(),
						blog.getPassword(), 0 };
				try {
					countResult = (HashMap<Object, Object>) client.call("wp.getCommentCount",
							countParams);
					totalComments = Integer.valueOf(countResult.get(
							"awaiting_moderation").toString())
							+ Integer.valueOf(countResult.get("approved")
									.toString());
				} catch (XMLRPCException e) {
					e.printStackTrace();
				}
				final Object[] result = (Object[]) client.call(method, params);
				handler.post(new Runnable() {
					public void run() {

						callBack.callFinished(result);
					}
				});
			} catch (final XMLRPCFault e) {
				handler.post(new Runnable() {
					public void run() {
						if (pd.isShowing()) {
							pd.dismiss();
						}
						onAnimateRefreshButton.onAnimateRefreshButton(false);
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						WPAlertDialogFragment alert = WPAlertDialogFragment
						        .newInstance(e.getLocalizedMessage());
						    alert.show(ft, "alert");
					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {
						if (pd.isShowing()) {
							pd.dismiss();
						}
						onAnimateRefreshButton.onAnimateRefreshButton(false);
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						WPAlertDialogFragment alert = WPAlertDialogFragment
						        .newInstance(e.getLocalizedMessage());
						    alert.show(ft, "alert");
					}
				});
			}
		}
	}

	interface XMLRPCMethodCallbackEditComment {
		void callFinished(Object result);
	}

	class XMLRPCMethodEditComment extends Thread {
		private String method;
		private Object[] params;
		private Handler handler;
		private XMLRPCMethodCallbackEditComment callBack;

		public XMLRPCMethodEditComment(String method,
				XMLRPCMethodCallbackEditComment callBack) {
			this.method = method;
			this.callBack = callBack;
			handler = new Handler();
		}

		public void call() {
			call(null);
		}

		public void call(Object[] params) {
			this.params = params;
			start();
		}

		@Override
		public void run() {
			try {
				final Object result = (Object) client.call(method, params);
				handler.post(new Runnable() {
					public void run() {
						callBack.callFinished(result);
					}
				});
			} catch (final XMLRPCFault e) {
				handler.post(new Runnable() {
					public void run() {
						getActivity().dismissDialog(ID_DIALOG_MODERATING);
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						WPAlertDialogFragment alert = WPAlertDialogFragment
						        .newInstance(e.getFaultString());
						    alert.show(ft, "alert");
					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {
						getActivity().dismissDialog(ID_DIALOG_MODERATING);
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						WPAlertDialogFragment alert = WPAlertDialogFragment
						        .newInstance(e.getLocalizedMessage());
						    alert.show(ft, "alert");
					}
				});
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		/* Switch on the ID of the item, to get what the user selected. */
		switch (item.getItemId()) {
		case 0:
			onCommentStatusChangeListener.onCommentStatusChanged("approve");
			return true;
		case 1:
			onCommentStatusChangeListener.onCommentStatusChanged("hold");
			return true;
		case 2:
			onCommentStatusChangeListener.onCommentStatusChanged("spam");
			return true;
		case 3:
			onCommentStatusChangeListener.onCommentStatusChanged("reply");
			return true;
		case 4:
			onCommentStatusChangeListener.onCommentStatusChanged("delete");
			return true;
		}
		return false;
	}

	private class getRecentCommentsTask extends
			AsyncTask<Void, Void, HashMap<String, HashMap<?, ?>>> {

		protected void onPostExecute(
				HashMap<String, HashMap<?, ?>> commentsResult) {

			if (commentsResult == null) {
				onAnimateRefreshButton.onAnimateRefreshButton(false);
				if (!moderateErrorMsg.equals("")) {
				    FragmentTransaction ft = getFragmentManager().beginTransaction();
					WPAlertDialogFragment alert = WPAlertDialogFragment
					        .newInstance(moderateErrorMsg);
					    alert.show(ft, "alert");
				}
				return;
			}

			if (commentsResult.size() == 0) {
				// no comments found
				if (pd.isShowing()) {
					pd.dismiss();
				}
				onAnimateRefreshButton.onAnimateRefreshButton(false);
			} else {

				if (commentsResult.size() < 30) {
					// end of list reached
					getListView().removeFooterView(switcher);
				}

				allComments.putAll(commentsResult);

				// loop this!
				for (int ctr = 0; ctr < commentsResult.size(); ctr++) {
					if (loadMore) {
						checkedComments.add("false");
					}
				}

				if (!doInBackground) {
					loadComments(loadMore, refreshOnly);
				}

				onAnimateRefreshButton.onAnimateRefreshButton(false);

			}

			if (!loadMore && !doInBackground) {
				onAnimateRefreshButton.onAnimateRefreshButton(false);
			} else if (loadMore) {
				switcher.showPrevious();
			}

		}

		@Override
		protected HashMap<String, HashMap<?, ?>> doInBackground(Void... args) {

			HashMap<String, HashMap<?, ?>> commentsResult;
			try {
				commentsResult = ApiHelper.refreshComments(getActivity()
						.getApplicationContext(), commentParams, loadMore);
			} catch (XMLRPCException e) {
				if (!getActivity().isFinishing())
					moderateErrorMsg = e.getLocalizedMessage();
				return null;
			}

			return commentsResult;

		}

	}

	public interface OnCommentSelectedListener {
		public void onCommentSelected(Comment comment);
	}

	public interface OnAnimateRefreshButtonListener {
		public void onAnimateRefreshButton(boolean start);
	}

	public interface OnContextCommentStatusChangeListener {
		public void onCommentStatusChanged(String status);
	}
}