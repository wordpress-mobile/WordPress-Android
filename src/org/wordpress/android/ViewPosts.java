package org.wordpress.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.StringHelper;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ViewPosts extends ListFragment {
	/** Called when the activity is first created. */
	private XMLRPCClient client;
	private String[] postIDs, titles, dateCreated, dateCreatedFormatted,
			draftIDs, draftTitles, draftDateCreated, statuses, draftStatuses;
	private Integer[] uploaded;
	int rowID = 0;
	long selectedID;
	public boolean inDrafts = false;
	public Vector<String> imageUrl = new Vector<String>();
	public String errorMsg = "";
	public int totalDrafts = 0, selectedPosition;
	public boolean isPage = false, vpUpgrade = false;
	public boolean largeScreen = false, shouldSelectAfterLoad = false;
	public int numRecords = 20;
	public ViewSwitcher switcher;
	private PostListAdapter pla;
	private OnPostSelectedListener onPostSelectedListener;
	private OnRefreshListener onRefreshListener;
	private OnPostActionListener onPostActionListener;
	public getRecentPostsTask getPostsTask;
	private Posts parentActivity;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// setContentView(R.layout.viewposts);

		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			isPage = extras.getBoolean("viewPages");
		}

	}

	@Override
	public void onActivityCreated(Bundle bundle) {
		super.onActivityCreated(bundle);

		createSwitcher();

		Display display = ((WindowManager) getActivity()
				.getApplicationContext().getSystemService(
						Context.WINDOW_SERVICE)).getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		if (width > 480 || height > 480) {
			largeScreen = true;
		}
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			onPostSelectedListener = (OnPostSelectedListener) activity;
			onRefreshListener = (OnRefreshListener) activity;
			onPostActionListener = (OnPostActionListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}

	public void onResume() {
		super.onResume();
		
		parentActivity = (Posts) getActivity();

	}

	public void createSwitcher() {
		// create the ViewSwitcher in the current context
		switcher = new ViewSwitcher(getActivity().getApplicationContext());
		Button footer = (Button) View.inflate(getActivity()
				.getApplicationContext(), R.layout.list_footer_btn, null);
		footer.setText(getResources().getText(R.string.load_more));

		footer.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// first view is showing, show the second progress view
				switcher.showNext();
				// get 30 more posts
				numRecords += 30;
				refreshPosts(true);
			}
		});

		View progress = View.inflate(getActivity().getApplicationContext(),
				R.layout.list_footer_progress, null);

		switcher.addView(footer);
		switcher.addView(progress);

	}

	public void refreshPosts(final boolean loadMore) {

		if (!loadMore) {
			onRefreshListener.onRefresh(true);
		}
		Vector<Object> apiArgs = new Vector<Object>();
		apiArgs.add(WordPress.currentBlog);
		apiArgs.add(isPage);
		apiArgs.add(numRecords);
		apiArgs.add(loadMore);
		getPostsTask = new getRecentPostsTask();
		getPostsTask.execute(apiArgs);
	}

	public Map<String, ?> createItem(String title, String caption) {
		Map<String, String> item = new HashMap<String, String>();
		item.put("title", title);
		item.put("caption", caption);
		return item;
	}

	public boolean loadPosts(boolean loadMore) { // loads posts from the db
		Vector<?> loadedPosts;
		try {
			if (isPage) {
				loadedPosts = WordPress.wpDB.loadUploadedPosts(getActivity()
						.getApplicationContext(), WordPress.currentBlog.getId(),
						true);
			} else {
				loadedPosts = WordPress.wpDB.loadUploadedPosts(getActivity()
						.getApplicationContext(), WordPress.currentBlog.getId(),
						false);
			}
		} catch (Exception e1) {
			return false;
		}

		if (loadedPosts != null) {
			titles = new String[loadedPosts.size()];
			postIDs = new String[loadedPosts.size()];
			dateCreated = new String[loadedPosts.size()];
			dateCreatedFormatted = new String[loadedPosts.size()];
			statuses = new String[loadedPosts.size()];
		} else {
			titles = new String[0];
			postIDs = new String[0];
			dateCreated = new String[0];
			dateCreatedFormatted = new String[0];
			statuses = new String[0];
			if (pla != null) {
				pla.notifyDataSetChanged();
			}
		}
		if (loadedPosts != null) {
			Date d = new Date();
			for (int i = 0; i < loadedPosts.size(); i++) {
				HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
				titles[i] = EscapeUtils.unescapeHtml(contentHash.get("title")
						.toString());

				postIDs[i] = contentHash.get("id").toString();
				dateCreated[i] = contentHash.get("date_created_gmt").toString();

				if (contentHash.get("post_status") != null) {
					String api_status = contentHash.get("post_status")
							.toString();
					if (api_status.equals("publish")) {
						statuses[i] = getResources()
								.getText(R.string.published).toString();
					} else if (api_status.equals("draft")) {
						statuses[i] = getResources().getText(R.string.draft)
								.toString();
					} else if (api_status.equals("pending")) {
						statuses[i] = getResources().getText(
								R.string.pending_review).toString();
					} else if (api_status.equals("private")) {
						statuses[i] = getResources().getText(
								R.string.post_private).toString();
					}

					if ((Long) contentHash.get("date_created_gmt") > d
							.getTime() && api_status.equals("publish")) {
						statuses[i] = getResources()
								.getText(R.string.scheduled).toString();
					}
				}

				// dateCreatedFormatted[i] =
				// contentHash.get("postDateFormatted").toString();
				int flags = 0;
				flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
				flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
				flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
				flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
				long localTime = (Long) contentHash.get("date_created_gmt");
				dateCreatedFormatted[i] = DateUtils
						.formatDateTime(getActivity().getApplicationContext(),
								localTime, flags);
			}

			// add the header
			List<String> postIDList = Arrays.asList(postIDs);
			List<String> newPostIDList = new ArrayList<String>();
			newPostIDList.add("postsHeader");
			newPostIDList.addAll(postIDList);
			postIDs = (String[]) newPostIDList.toArray(new String[newPostIDList
					.size()]);

			List<String> postTitleList = Arrays.asList(titles);
			List<CharSequence> newPostTitleList = new ArrayList<CharSequence>();
			newPostTitleList.add(getResources().getText(
					(isPage) ? R.string.tab_pages : R.string.tab_posts));
			newPostTitleList.addAll(postTitleList);
			titles = (String[]) newPostTitleList
					.toArray(new String[newPostTitleList.size()]);

			List<String> dateList = Arrays.asList(dateCreated);
			List<String> newDateList = new ArrayList<String>();
			newDateList.add("postsHeader");
			newDateList.addAll(dateList);
			dateCreated = (String[]) newDateList.toArray(new String[newDateList
					.size()]);

			List<String> dateFormattedList = Arrays
					.asList(dateCreatedFormatted);
			List<String> newDateFormattedList = new ArrayList<String>();
			newDateFormattedList.add("postsHeader");
			newDateFormattedList.addAll(dateFormattedList);
			dateCreatedFormatted = (String[]) newDateFormattedList
					.toArray(new String[newDateFormattedList.size()]);

			List<String> statusList = Arrays.asList(statuses);
			List<String> newStatusList = new ArrayList<String>();
			newStatusList.add("postsHeader");
			newStatusList.addAll(statusList);
			statuses = (String[]) newStatusList
					.toArray(new String[newStatusList.size()]);
		}
		// load drafts
		boolean drafts = loadDrafts();

		if (drafts) {

			List<String> draftIDList = Arrays.asList(draftIDs);
			List<String> newDraftIDList = new ArrayList<String>();
			newDraftIDList.add("draftsHeader");
			newDraftIDList.addAll(draftIDList);
			draftIDs = (String[]) newDraftIDList
					.toArray(new String[newDraftIDList.size()]);

			List<String> titleList = Arrays.asList(draftTitles);
			List<CharSequence> newTitleList = new ArrayList<CharSequence>();
			newTitleList.add(getResources().getText(R.string.local_drafts));
			newTitleList.addAll(titleList);
			draftTitles = (String[]) newTitleList
					.toArray(new String[newTitleList.size()]);

			List<String> draftDateList = Arrays.asList(draftDateCreated);
			List<String> newDraftDateList = new ArrayList<String>();
			newDraftDateList.add("draftsHeader");
			newDraftDateList.addAll(draftDateList);
			draftDateCreated = (String[]) newDraftDateList
					.toArray(new String[newDraftDateList.size()]);

			List<String> draftStatusList = Arrays.asList(draftStatuses);
			List<String> newDraftStatusList = new ArrayList<String>();
			newDraftStatusList.add("draftsHeader");
			newDraftStatusList.addAll(draftStatusList);
			draftStatuses = (String[]) newDraftStatusList
					.toArray(new String[newDraftStatusList.size()]);

			postIDs = StringHelper.mergeStringArrays(draftIDs, postIDs);
			titles = StringHelper.mergeStringArrays(draftTitles, titles);
			dateCreatedFormatted = StringHelper.mergeStringArrays(
					draftDateCreated, dateCreatedFormatted);
			statuses = StringHelper.mergeStringArrays(draftStatuses, statuses);
		} else {
			if (pla != null) {
				pla.notifyDataSetChanged();
			}
		}

		if (loadedPosts != null || drafts == true) {
			ListView listView = getListView();
			listView.removeFooterView(switcher);
			if (loadedPosts != null) {
				if (loadedPosts.size() >= 20) {
					listView.addFooterView(switcher);
				}
			}

			if (loadMore) {
				pla.notifyDataSetChanged();
			} else {
				pla = new PostListAdapter(getActivity().getApplicationContext());
				listView.setAdapter(pla);

				listView.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> arg0, View v,
							int position, long id) {
						if (position < postIDs.length) {
							if (v != null
									&& !postIDs[position]
											.equals("draftsHeader")
									&& !postIDs[position].equals("postsHeader")
									&& !parentActivity.isRefreshing) {
								selectedPosition = position;
								selectedID = v.getId();
								Post post = new Post(WordPress.currentBlog
										.getId(), selectedID, isPage,
										getActivity().getApplicationContext());
								if (post.getId() >= 0) {
									WordPress.currentPost = post;
									onPostSelectedListener.onPostSelected(post);
									pla.notifyDataSetChanged();
								} else {
									if (!getActivity().isFinishing()) {
										FragmentTransaction ft = getFragmentManager()
												.beginTransaction();
										WPAlertDialogFragment alert = WPAlertDialogFragment
												.newInstance(getString(R.string.post_not_found));
										alert.show(ft, "alert");
									}
								}
							}
						}
					}

				});

				listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						AdapterView.AdapterContextMenuInfo info;
						try {
							info = (AdapterView.AdapterContextMenuInfo) menuInfo;
						} catch (ClassCastException e) {
							// Log.e(TAG, "bad menuInfo", e);
							return;
						}
						
						if (parentActivity.isRefreshing)
							return;

						Object[] args = { R.id.row_post_id };

						try {
							Method m = android.view.View.class
									.getMethod("getTag");
							m.invoke(selectedID, args);
						} catch (NoSuchMethodException e) {
							selectedID = info.targetView.getId();
						} catch (IllegalArgumentException e) {
							selectedID = info.targetView.getId();
						} catch (IllegalAccessException e) {
							selectedID = info.targetView.getId();
						} catch (InvocationTargetException e) {
							selectedID = info.targetView.getId();
						}
						// selectedID = (String)
						// info.targetView.getTag(R.id.row_post_id);
						rowID = info.position;

						if (totalDrafts > 0 && rowID <= totalDrafts
								&& rowID != 0) {
							menu.clear();
							menu.setHeaderTitle(getResources().getText(
									R.string.draft_actions));
							menu.add(1, 0, 0,
									getResources().getText(R.string.edit_draft));
							menu.add(
									1,
									1,
									0,
									getResources().getText(
											R.string.delete_draft));
						} else if (rowID == 1
								|| ((rowID != (totalDrafts + 1)) && rowID != 0)) {
							menu.clear();

							if (isPage) {
								menu.setHeaderTitle(getResources().getText(
										R.string.page_actions));
								menu.add(
										2,
										0,
										0,
										getResources().getText(
												R.string.edit_page));
								menu.add(
										2,
										1,
										0,
										getResources().getText(
												R.string.delete_page));
								menu.add(
										2,
										2,
										0,
										getResources().getText(
												R.string.share_url));
							} else {
								menu.setHeaderTitle(getResources().getText(
										R.string.post_actions));
								menu.add(
										0,
										0,
										0,
										getResources().getText(
												R.string.edit_post));
								menu.add(
										0,
										1,
										0,
										getResources().getText(
												R.string.delete_post));
								menu.add(
										0,
										2,
										0,
										getResources().getText(
												R.string.share_url));
							}
						}
					}
				});
			}

			if (this.shouldSelectAfterLoad) {
				if (postIDs != null) {
					if (postIDs.length >= 1) {

						Post post = new Post(WordPress.currentBlog.getId(),
								Integer.valueOf(postIDs[1]), isPage,
								getActivity().getApplicationContext());
						if (post.getId() >= 0) {
							WordPress.currentPost = post;
							onPostSelectedListener.onPostSelected(post);
							selectedPosition = 1;
							pla.notifyDataSetChanged();
						}
					}
				}
				shouldSelectAfterLoad = false;
			}

			if (loadedPosts == null) {
				refreshPosts(false);
			}

			return true;
		} else {

			if (loadedPosts == null) {
				refreshPosts(false);
			}

			return false;
		}

	}

	class ViewWrapper {
		View base;
		TextView title = null;
		TextView date = null;
		TextView status = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getTitle() {
			if (title == null) {
				title = (TextView) base.findViewById(R.id.title);
			}
			return (title);
		}

		TextView getDate() {
			if (date == null) {
				date = (TextView) base.findViewById(R.id.date);
			}
			return (date);
		}

		TextView getStatus() {
			if (status == null) {
				status = (TextView) base.findViewById(R.id.status);
			}
			return (status);
		}
	}

	private boolean loadDrafts() { // loads drafts from the db

		Vector<?> loadedPosts;
		if (isPage) {
			loadedPosts = WordPress.wpDB.loadDrafts(
					WordPress.currentBlog.getId(), true);
		} else {
			loadedPosts = WordPress.wpDB.loadDrafts(
					WordPress.currentBlog.getId(), false);
		}
		if (loadedPosts != null) {
			draftIDs = new String[loadedPosts.size()];
			draftTitles = new String[loadedPosts.size()];
			draftDateCreated = new String[loadedPosts.size()];
			uploaded = new Integer[loadedPosts.size()];
			totalDrafts = loadedPosts.size();
			draftStatuses = new String[loadedPosts.size()];

			for (int i = 0; i < loadedPosts.size(); i++) {
				HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
				draftIDs[i] = contentHash.get("id").toString();
				draftTitles[i] = EscapeUtils.unescapeHtml(contentHash.get(
						"title").toString());
				// drafts won't show the date in the list
				draftDateCreated[i] = "";
				uploaded[i] = (Integer) contentHash.get("uploaded");
				// leaving status blank for local drafts since it's pretty clear
				// that they are already local drafts
				draftStatuses[i] = "";
			}

			return true;
		} else {
			totalDrafts = 0;
			return false;
		}
	}

	private class PostListAdapter extends BaseAdapter {

		int sdk_version = 7;
		boolean detailViewVisible = false;

		public PostListAdapter(Context context) {
			sdk_version = android.os.Build.VERSION.SDK_INT;
			FragmentManager fm = getActivity().getSupportFragmentManager();
			ViewPostFragment f = (ViewPostFragment) fm
					.findFragmentById(R.id.postDetail);
			if (f != null && f.isInLayout())
				detailViewVisible = true;
		}

		public int getCount() {
			return postIDs.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv = convertView;
			ViewWrapper wrapper = null;
			if (pv == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				pv = inflater.inflate(R.layout.row_post_page, parent, false);
				wrapper = new ViewWrapper(pv);
				if (position == 0) {
					// dateHeight = wrapper.getDate().getHeight();
				}
				pv.setTag(wrapper);
				wrapper = new ViewWrapper(pv);
				pv.setTag(wrapper);
			} else {
				wrapper = (ViewWrapper) pv.getTag();
			}

			String date = dateCreatedFormatted[position];
			String status_text = statuses[position];
			if (date.equals("postsHeader") || date.equals("draftsHeader")) {

				pv.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.title_text_bg));

				wrapper.getTitle().setTextColor(Color.parseColor("#EEEEEE"));
				wrapper.getTitle().setShadowLayer(1, 1, 1,
						Color.parseColor("#444444"));
				if (largeScreen) {
					wrapper.getTitle().setPadding(12, 0, 12, 3);
				} else {
					wrapper.getTitle().setPadding(8, 0, 8, 2);
				}
				wrapper.getTitle().setTextScaleX(1.2f);
				wrapper.getTitle().setTextSize(17);
				wrapper.getDate().setHeight(0);
				wrapper.getStatus().setHeight(0);

				if (date.equals("draftsHeader")) {
					inDrafts = true;
					date = "";
					status_text = "";
				} else if (date.equals("postsHeader")) {
					inDrafts = false;
					date = "";
					status_text = "";
				}
			} else {
				if (position == selectedPosition && sdk_version >= 11
						&& detailViewVisible) {
					pv.setBackgroundDrawable(getResources().getDrawable(
							R.drawable.list_highlight_bg));
				} else {
					pv.setBackgroundDrawable(getResources().getDrawable(
							R.drawable.list_bg_selector));
				}
				if (largeScreen) {
					wrapper.getTitle().setPadding(12, 12, 12, 0);
				} else {
					wrapper.getTitle().setPadding(8, 8, 8, 0);
				}
				wrapper.getTitle().setTextColor(Color.parseColor("#444444"));
				wrapper.getTitle().setShadowLayer(0, 0, 0,
						Color.parseColor("#444444"));
				wrapper.getTitle().setTextScaleX(1.0f);
				wrapper.getTitle().setTextSize(16);
				wrapper.getDate().setTextColor(Color.parseColor("#888888"));

				pv.setTag(R.id.row_post_id, postIDs[position]);
				pv.setId(Integer.valueOf(postIDs[position]));

				if (wrapper.getDate().getHeight() == 0) {
					wrapper.getDate().setHeight(
							(int) wrapper.getTitle().getTextSize()
									+ wrapper.getDate().getPaddingBottom());
					wrapper.getStatus().setHeight(
							(int) wrapper.getTitle().getTextSize()
									+ wrapper.getStatus().getPaddingBottom());
				}
			}
			String titleText = titles[position];
			if (titleText == "")
				titleText = "(" + getResources().getText(R.string.untitled)
						+ ")";
			wrapper.getTitle().setText(titleText);
			wrapper.getDate().setText(date);
			wrapper.getStatus().setText(status_text);

			return pv;

		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Post post = new Post(WordPress.currentBlog.getId(), selectedID, isPage,
				getActivity().getApplicationContext());
		
		if (post.getId() < 0) {
			if (!getActivity().isFinishing()) {
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				WPAlertDialogFragment alert = WPAlertDialogFragment
						.newInstance(getString(R.string.post_not_found));
				alert.show(ft, "alert");
			}
			return false;
		}
		
		/* Switch on the ID of the item, to get what the user selected. */
		if (item.getGroupId() == 0) {
			switch (item.getItemId()) {
			case 0:
				Intent i2 = new Intent(getActivity().getApplicationContext(),
						EditPost.class);
				i2.putExtra("postID", selectedID);
				i2.putExtra("id", WordPress.currentBlog.getId());
				startActivityForResult(i2, 0);
				return true;
			case 1:
				onPostActionListener.onPostAction(Posts.POST_DELETE, post);
				return true;
			case 2:
				onPostActionListener.onPostAction(Posts.POST_SHARE, post);
				return true;
			}

		} else if (item.getGroupId() == 2) {
			switch (item.getItemId()) {
			case 0:
				Intent i2 = new Intent(getActivity().getApplicationContext(),
						EditPost.class);
				i2.putExtra("postID", selectedID);
				i2.putExtra("id", WordPress.currentBlog.getId());
				i2.putExtra("isPage", true);
				startActivityForResult(i2, 0);
				return true;
			case 1:
				onPostActionListener.onPostAction(Posts.POST_DELETE, post);
				return true;
			case 2:
				onPostActionListener.onPostAction(Posts.POST_SHARE, post);
				return true;
			}

		} else {
			switch (item.getItemId()) {
			case 0:
				Intent i2 = new Intent(getActivity().getApplicationContext(),
						EditPost.class);
				i2.putExtra("postID", selectedID);
				i2.putExtra("id", WordPress.currentBlog.getId());
				if (isPage) {
					i2.putExtra("isPage", true);
				}
				i2.putExtra("localDraft", true);
				startActivityForResult(i2, 0);
				return true;
			case 1:

				onPostActionListener.onPostAction(Posts.POST_DELETE, post);
				return true;
			}
		}

		return false;
	}

	public class getRecentPostsTask extends
			AsyncTask<Vector<?>, Void, Void> {

		Context ctx;
		boolean isPage, loadMore;

		protected void onPostExecute(Void result) {
			if (isCancelled()) {
				onRefreshListener.onRefresh(false);
				return;
			}

			if (loadMore)
				switcher.showPrevious();
			onRefreshListener.onRefresh(false);

			if (errorMsg != "" && !getActivity().isFinishing()) {
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				WPAlertDialogFragment alert = WPAlertDialogFragment
						.newInstance(String.format(getResources().getString(R.string.error_refresh), (isPage) ? getResources().getText(R.string.pages) : getResources().getText(R.string.posts)), errorMsg);
				alert.show(ft, "alert");
				errorMsg = "";
			} else {
				loadPosts(loadMore);
			}
			
		}

		@Override
		protected Void doInBackground(Vector<?>... args) {

			Vector<?> arguments = args[0];
			WordPress.currentBlog = (Blog) arguments.get(0);
			isPage = (Boolean) arguments.get(1);
			int numRecords = (Integer) arguments.get(2);
			loadMore = (Boolean) arguments.get(3);
			client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
					WordPress.currentBlog.getHttpuser(),
					WordPress.currentBlog.getHttppassword());

			Object[] result = null;
			Object[] params = { WordPress.currentBlog.getBlogId(),
					WordPress.currentBlog.getUsername(),
					WordPress.currentBlog.getPassword(), numRecords };
			try {
				result = (Object[]) client.call((isPage) ? "wp.getPages"
						: "metaWeblog.getRecentPosts", params);
				if (result != null) {
					if (result.length > 0) {
						HashMap<?, ?> contentHash = new HashMap<Object, Object>();
						Vector<HashMap<?, ?>> dbVector = new Vector<HashMap<?, ?>>();

						if (!loadMore) {
							WordPress.wpDB.deleteUploadedPosts(
									WordPress.currentBlog.getId(), isPage);
						}

						for (int ctr = 0; ctr < result.length; ctr++) {
							HashMap<String, Object> dbValues = new HashMap<String, Object>();
							contentHash = (HashMap<?, ?>) result[ctr];
							dbValues.put("blogID",
									WordPress.currentBlog.getBlogId());
							dbVector.add(ctr, contentHash);
						}

						WordPress.wpDB.savePosts(dbVector,
								WordPress.currentBlog.getId(), isPage);
						numRecords += 20;
					} else {
						if (pla != null) {
							if (postIDs.length == 2) {
								try {
									WordPress.wpDB.deleteUploadedPosts(
											WordPress.currentBlog.getId(),
											WordPress.currentPost.isPage());
									onPostActionListener
									.onPostAction(Posts.POST_CLEAR,
											WordPress.currentPost);
								} catch (Exception e) {
									e.printStackTrace();
								}
								WordPress.currentPost = null;
							}
						}
					}
				}
			} catch (XMLRPCException e) {
				errorMsg = e.getLocalizedMessage();
			}

			return null;
		}
	}

	public interface OnPostSelectedListener {
		public void onPostSelected(Post post);
	}

	public interface OnRefreshListener {
		public void onRefresh(boolean start);
	}

	public interface OnPostActionListener {
		public void onPostAction(int action, Post post);
	}

}
