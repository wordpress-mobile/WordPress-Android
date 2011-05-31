package org.wordpress.android;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.wordpress.android.models.Blog;
import org.xmlpull.v1.XmlPullParser;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class viewPosts extends ListActivity {
	/** Called when the activity is first created. */
	private XMLRPCClient client;
	private String[] postIDs, titles, dateCreated, dateCreatedFormatted, draftIDs, draftTitles, publish;
	private Integer[] uploaded;
	private String id = "",accountName = "", newID = "", selectedID = "";
	int rowID = 0;
	private int ID_DIALOG_DELETING = 1, ID_DIALOG_POSTING = 2;
	Vector<String> selectedCategories = new Vector<String>();
	public boolean inDrafts = false;
	private Vector<Uri> selectedImageIDs = new Vector<Uri>();
	private int selectedImageCtr = 0;
	public String imgHTML, sImagePlacement = "", sMaxImageWidth = "";
	public boolean centerThumbnail = false;
	public Vector<String> imageUrl = new Vector<String>();
	public String imageTitle = null;
	public boolean thumbnailOnly, secondPass, xmlrpcError = false;
	public String submitResult = "", mediaErrorMsg = "";
	ProgressDialog loadingDialog;
	public int totalDrafts = 0;
	public boolean isPage = false, vpUpgrade = false;
	boolean largeScreen = false;
	int numRecords = 30;
	private ViewSwitcher switcher;
	private PostListAdapter pla;
	private Blog blog;
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.viewposts);

		Bundle extras = getIntent().getExtras();
		String action = null;
		if (extras != null) {
			id = extras.getString("id");
			blog = new Blog(id, this);
			accountName = extras.getString("accountName");
			isPage = extras.getBoolean("viewPages");
			action = extras.getString("action");
		}

		createSwitcher();

		// user came from action intent
		if (action != null && !isPage) {
			if (action.equals("upload")) {
				selectedID = String.valueOf(extras.getInt("uploadID"));
				showDialog(ID_DIALOG_POSTING);

				new Thread() {
					public void run() {

						try {
							submitResult = submitPost();

						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}.start();
			} else {

				boolean loadedPosts = loadPosts(false);
				if (!loadedPosts) {
					refreshPosts(false);
				}
			}
		} else {

			// query for posts and refresh view
			boolean loadedPosts = loadPosts(false);

			if (!loadedPosts) {
				refreshPosts(false);
			}
		}

		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		if (width > 480 || height > 480) {
			largeScreen = true;
		}

		final ImageButton addNewPost = (ImageButton) findViewById(R.id.newPost);

		addNewPost.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {

				Intent i = new Intent(viewPosts.this, editPost.class);
				i.putExtra("accountName", accountName);
				i.putExtra("id", id);
				i.putExtra("isNew", true);
				if (isPage) {
					i.putExtra("isPage", true);
				}
				startActivityForResult(i, 0);

			}
		});

		final ImageButton refresh = (ImageButton) findViewById(R.id.refresh);

		refresh.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {

				refreshPosts(false);

			}
		});

	}

	private void createSwitcher() {
		// add footer view
		if (!isPage) {
			// create the ViewSwitcher in the current context
			switcher = new ViewSwitcher(this);
			Button footer = (Button) View.inflate(this,
					R.layout.list_footer_btn, null);
			footer.setText(getResources().getText(R.string.load_more) + " "
					+ getResources().getText(R.string.tab_posts));

			footer.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					// first view is showing, show the second progress view
					switcher.showNext();
					// get 30 more posts
					numRecords += 30;
					refreshPosts(true);
				}
			});

			View progress = View.inflate(this, R.layout.list_footer_progress,
					null);

			switcher.addView(footer);
			switcher.addView(progress);
		}

	}

	private void refreshPosts(final boolean loadMore) {

		if (!loadMore) {
			showProgressBar();
		}

		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

		XMLRPCMethod method = new XMLRPCMethod((isPage) ? "wp.getPageList"
				: "blogger.getRecentPosts", new XMLRPCMethodCallback() {
			public void callFinished(Object[] result) {

				if (result.length == 0) {
					if (!loadMore) {
						closeProgressBar();
					} else {
						switcher.showPrevious();
					}
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							viewPosts.this);
					dialogBuilder.setTitle(getResources().getText(
							(isPage) ? R.string.pages_not_found
									: R.string.posts_not_found));
					dialogBuilder.setMessage(getResources().getText(
							(isPage) ? R.string.pages_no_pages
									: R.string.posts_no_posts));
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

					HashMap<?, ?> contentHash = new HashMap<Object, Object>();

					String rTitles[] = new String[result.length];
					String rPostIDs[] = new String[result.length];
					String rDateCreated[] = new String[result.length];
					String rDateCreatedFormatted[] = new String[result.length];
					String rParentID[] = new String[result.length];
					Vector<HashMap<?, ?>> dbVector = new Vector<HashMap<?, ?>>();
					WordPressDB postStoreDB = new WordPressDB(viewPosts.this);
					Date d = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat(
							"EEE MMM dd HH:mm:ss z yyyy");
					Calendar cal = Calendar.getInstance();
					TimeZone tz = cal.getTimeZone();
					String shortDisplayName = "";
					shortDisplayName = tz.getDisplayName(true, TimeZone.SHORT);

					// loop this!
					for (int ctr = 0; ctr < result.length; ctr++) {
						HashMap<String, String> dbValues = new HashMap<String, String>();
						contentHash = (HashMap<?, ?>) result[ctr];
						if (isPage) {
							rTitles[ctr] = escapeUtils.unescapeHtml(contentHash
									.get("page_title").toString());
							rPostIDs[ctr] = contentHash.get("page_id")
									.toString();
							rDateCreated[ctr] = contentHash.get("dateCreated")
									.toString();
							rParentID[ctr] = contentHash.get("page_parent_id")
									.toString();
						} else {
							rTitles[ctr] = escapeUtils.unescapeHtml(contentHash
									.get("content").toString().substring(
											contentHash.get("content")
													.toString().indexOf(
															"<title>") + 7,
											contentHash.get("content")
													.toString().indexOf(
															"</title>")));
							rPostIDs[ctr] = contentHash.get("postid")
									.toString();
							rDateCreated[ctr] = contentHash.get("dateCreated")
									.toString();

						}

						// make the date pretty
						String cDate = rDateCreated[ctr].replace(tz.getID(),
								shortDisplayName);
						try {
							d = sdf.parse(cDate);
							SimpleDateFormat sdfOut = new SimpleDateFormat(
									"MMMM dd, yyyy hh:mm a");
							rDateCreatedFormatted[ctr] = sdfOut.format(d);
						} catch (ParseException pe) {
							pe.printStackTrace();
							rDateCreatedFormatted[ctr] = rDateCreated[ctr]; 
						}

						dbValues.put("blogID", id);
						dbValues.put("title", rTitles[ctr]);

						if (isPage) {
							dbValues.put("pageID", rPostIDs[ctr]);
							dbValues.put("pageDate", rDateCreated[ctr]);
							dbValues.put("pageDateFormatted",
									rDateCreatedFormatted[ctr]);
							dbValues.put("parentID", rParentID[ctr]);
							dbVector.add(ctr, dbValues);
						} else {
							dbValues.put("postID", rPostIDs[ctr]);
							dbValues.put("postDate", rDateCreated[ctr]);
							dbValues.put("postDateFormatted",
									rDateCreatedFormatted[ctr]);
							dbVector.add(ctr, dbValues);

						}

					}// end for loop

					if (isPage) {
						postStoreDB.savePages(viewPosts.this, dbVector);
					} else {
						postStoreDB.savePosts(viewPosts.this, dbVector);
					}

					loadPosts(loadMore);
					if (!loadMore) {
						closeProgressBar();
					} else {
						switcher.showPrevious();
					}
				}

			}
		});
		if (isPage) {
			Object[] params = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), };
			method.call(params);
		} else {
			Object[] params = { "spacer", blog.getBlogId(), blog.getUsername(), blog.getPassword(),
					numRecords };
			method.call(params);
		}

	}

	public Map<String, ?> createItem(String title, String caption) {
		Map<String, String> item = new HashMap<String, String>();
		item.put("title", title);
		item.put("caption", caption);
		return item;
	}

	private boolean loadPosts(boolean loadMore) { // loads posts from the db

		WordPressDB postStoreDB = new WordPressDB(this);
		Vector<?> loadedPosts;
		if (isPage) {
			loadedPosts = postStoreDB.loadPages(viewPosts.this, id);
		} else {
			loadedPosts = postStoreDB.loadSavedPosts(viewPosts.this, id);
		}

		if (loadedPosts != null) {
			titles = new String[loadedPosts.size()];
			postIDs = new String[loadedPosts.size()];
			dateCreated = new String[loadedPosts.size()];
			dateCreatedFormatted = new String[loadedPosts.size()];
		} else {
			titles = new String[0];
			postIDs = new String[0];
			dateCreated = new String[0];
			dateCreatedFormatted = new String[0];
			if (pla != null) {
				pla.notifyDataSetChanged();
			}
		}
		if (loadedPosts != null) {
			for (int i = 0; i < loadedPosts.size(); i++) {
				HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
				titles[i] = escapeUtils.unescapeHtml(contentHash.get("title")
						.toString());
				if (isPage) {
					postIDs[i] = contentHash.get("pageID").toString();
					dateCreated[i] = contentHash.get("pageDate").toString();
					dateCreatedFormatted[i] = contentHash.get(
							"pageDateFormatted").toString();
				} else {
					postIDs[i] = contentHash.get("postID").toString();
					dateCreated[i] = contentHash.get("postDate").toString();
					dateCreatedFormatted[i] = contentHash.get(
							"postDateFormatted").toString();
				}
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

			List<String> dateFormattedList = Arrays.asList(dateCreatedFormatted);
			List<String> newDateFormattedList = new ArrayList<String>();
			newDateFormattedList.add("postsHeader");
			newDateFormattedList.addAll(dateFormattedList);
			dateCreatedFormatted = (String[]) newDateFormattedList
					.toArray(new String[newDateFormattedList.size()]);
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

			List<String> publishList = Arrays.asList(publish);
			List<String> newPublishList = new ArrayList<String>();
			newPublishList.add("draftsHeader");
			newPublishList.addAll(publishList);
			publish = (String[]) newPublishList.toArray(new String[newPublishList.size()]);

			postIDs = StringHelper.mergeStringArrays(draftIDs, postIDs);
			titles = StringHelper.mergeStringArrays(draftTitles, titles);
			dateCreatedFormatted = StringHelper.mergeStringArrays(publish,
					dateCreatedFormatted);
		} else {
			if (pla != null) {
				pla.notifyDataSetChanged();
			}
		}

		if (loadedPosts != null || drafts == true) {
			ListView listView = (ListView) findViewById(android.R.id.list);

			if (!isPage) {
				listView.removeFooterView(switcher);
				if (loadedPosts != null) {
					if (loadedPosts.size() >= 30) {
						listView.addFooterView(switcher);
					}
				}
			}

			if (loadMore) {
				pla.notifyDataSetChanged();
			} else {
				pla = new PostListAdapter(viewPosts.this);
				listView.setAdapter(pla);

				listView.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						if (arg1 != null) {
							arg1.performLongClick();
						}

					}

				});

				listView
						.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

							public void onCreateContextMenu(ContextMenu menu,
									View v, ContextMenuInfo menuInfo) {
								AdapterView.AdapterContextMenuInfo info;
								try {
									info = (AdapterView.AdapterContextMenuInfo) menuInfo;
								} catch (ClassCastException e) {
									// Log.e(TAG, "bad menuInfo", e);
									return;
								}

								
								Object[] args = { R.id.row_post_id };
								
								try {
									Method m = android.view.View.class.getMethod("getTag");
									m.invoke(selectedID, args);
								}
								 catch (NoSuchMethodException e) {
									 	selectedID = String.valueOf(info.targetView.getId()); 
									} catch (IllegalArgumentException e) {
									 	selectedID = String.valueOf(info.targetView.getId()); 
									} catch (IllegalAccessException e) {
									 	selectedID = String.valueOf(info.targetView.getId()); 
									} catch (InvocationTargetException e) {
									 	selectedID = String.valueOf(info.targetView.getId()); 
									}
								//selectedID = (String) info.targetView.getTag(R.id.row_post_id);
								rowID = info.position;

								if (totalDrafts > 0 && rowID <= totalDrafts
										&& rowID != 0) {
									menu.clear();
									menu.setHeaderTitle(getResources().getText(R.string.draft_actions));
									menu.add(1, 0, 0, getResources().getText(R.string.edit_draft));
									menu.add(1, 1, 0, getResources().getText(R.string.upload));
									menu.add(1, 2, 0, getResources().getText(R.string.delete_draft));
								} else if (rowID == 1
										|| ((rowID != (totalDrafts + 1)) && rowID != 0)) {
									menu.clear();

									if (isPage) {
										menu.setHeaderTitle(getResources().getText(R.string.page_actions));
										menu.add(2,0,0,getResources().getText(R.string.preview_page));
										menu.add(2,1,0,getResources().getText(R.string.view_comments));
										menu.add(2, 2, 0, getResources().getText(R.string.edit_page));
										menu.add(2, 3, 0, getResources().getText(R.string.delete_page));
										menu.add(2, 4, 0, getResources().getText(R.string.share_url));
									} else {
										menu.setHeaderTitle(getResources().getText(R.string.post_actions));
										menu.add(0,0,0,getResources().getText(R.string.preview_post));
										menu.add(0,1,0,getResources().getText(R.string.view_comments));
										menu.add(0, 2, 0, getResources().getText(R.string.edit_post));
										menu.add(0, 3, 0, getResources().getText(R.string.delete_post));
										menu.add(0, 4, 0, getResources().getText(R.string.share_url));
									}
								}
							}
						});
			}
			return true;
		} else {
			return false;
		}

	}

	class ViewWrapper {
		View base;
		TextView title = null;
		TextView date = null;

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
	}

	private boolean loadDrafts() { // loads drafts from the db

		WordPressDB lDraftsDB = new WordPressDB(this);
		Vector<?> loadedPosts;
		if (isPage) {
			loadedPosts = lDraftsDB.loadPageDrafts(viewPosts.this, id);
		} else {
			loadedPosts = lDraftsDB.loadPosts(viewPosts.this, id);
		}
		if (loadedPosts != null) {
			draftIDs = new String[loadedPosts.size()];
			draftTitles = new String[loadedPosts.size()];
			publish = new String[loadedPosts.size()];
			uploaded = new Integer[loadedPosts.size()];
			totalDrafts = loadedPosts.size();

			for (int i = 0; i < loadedPosts.size(); i++) {
				HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
				draftIDs[i] = contentHash.get("id").toString();
				draftTitles[i] = escapeUtils.unescapeHtml(contentHash.get(
						"title").toString());
				if (contentHash.get("status") != null){
					publish[i] = contentHash.get("status").toString();
				}
				else {
					publish[i] = "";
				}
				uploaded[i] = (Integer) contentHash.get("uploaded");
			}

			return true;
		} else {
			totalDrafts = 0;
			return false;
		}
	}

	private class PostListAdapter extends BaseAdapter {
		

		public PostListAdapter(Context context) {
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
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.row_post_page, parent, false);
				wrapper = new ViewWrapper(pv);
				if (position == 0) {
					//dateHeight = wrapper.getDate().getHeight();
				}
				pv.setTag(wrapper);
				wrapper = new ViewWrapper(pv);
				pv.setTag(wrapper);
			} else {
				wrapper = (ViewWrapper) pv.getTag();
			}

			String date = dateCreatedFormatted[position];
			if (date.equals("postsHeader") || date.equals("draftsHeader")) {

				pv.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.list_header_bg));

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

				if (date.equals("draftsHeader")) {
					inDrafts = true;
					date = "";
				} else if (date.equals("postsHeader")) {
					inDrafts = false;
					date = "";
				}
			} else {
				pv.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.list_bg_selector));
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

				Object[] args = { R.id.row_post_id, postIDs[position] };
				
				try {
				    Method m = android.view.View.class.getMethod("setTag");
				    m.invoke(pv, args);
				} catch (NoSuchMethodException e) {
					pv.setId(Integer.valueOf(postIDs[position]));
				} catch (IllegalArgumentException e) {
					pv.setId(Integer.valueOf(postIDs[position]));
				} catch (IllegalAccessException e) {
					pv.setId(Integer.valueOf(postIDs[position]));
				} catch (InvocationTargetException e) {
					pv.setId(Integer.valueOf(postIDs[position]));
				}
				
				//pv.setId(Integer.valueOf(postIDs[position]));
				//pv.setTag(R.id.row_post_id, postIDs[position]);
				
				if (wrapper.getDate().getHeight() == 0) {
					wrapper.getDate().setHeight(
							(int) wrapper.getTitle().getTextSize()
									+ wrapper.getDate().getPaddingBottom());
				}
				String customDate = date;

				if (customDate.equals("draft")) {
					customDate = getResources().getText(R.string.draft).toString();
				} else if (customDate.equals("pending")) {
					customDate = getResources().getText(R.string.pending_review).toString();
				}
				else if (customDate.equals("private")) {
					customDate = getResources().getText(R.string.post_private).toString();
				}
				else if (customDate.equals("publish")) {
					customDate = getResources().getText(R.string.publish_post).toString();
					wrapper.getDate().setTextColor(Color.parseColor("#006505"));
				}
				date = customDate;

			}
			wrapper.getTitle().setText(titles[position]);
			wrapper.getDate().setText(date);

			return pv;

		}

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

		@Override
		public void run() {
			try {
				final Object[] result = (Object[]) client.call(method, params);
				handler.post(new Runnable() {
					public void run() {

						callBack.callFinished(result);
					}
				});
			} catch (final XMLRPCFault e) {
				handler.post(new Runnable() {
					public void run() {
						closeProgressBar();
						if (e.getFaultCode() != 500) {
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
									viewPosts.this);
							dialogBuilder.setTitle(getResources().getText(
									R.string.connection_error));
							String msg = e.getLocalizedMessage();
							dialogBuilder.setMessage(e.getFaultString());
							if (msg.contains("403")) {
								dialogBuilder.setMessage(e.getFaultString()
										+ " "
										+ getResources().getString(
												R.string.load_settings));
								dialogBuilder.setPositiveButton(getResources()
										.getString(R.string.yes),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												Intent i = new Intent(
														viewPosts.this,
														settings.class);
												i.putExtra("id", id);
												i.putExtra("accountName",
														accountName);
												startActivity(i);

											}
										});

								dialogBuilder.setNegativeButton(getResources()
										.getString(R.string.no),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												// Just close the window.

											}
										});
							} else {
								dialogBuilder.setPositiveButton("OK",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												// Just close the window.

											}
										});
							}
							dialogBuilder.setCancelable(true);
							if (!isFinishing()) {
								dialogBuilder.create().show();
							}
						} else {
							WordPressDB postStoreDB = new WordPressDB(
									viewPosts.this);
							postStoreDB.clearPosts(viewPosts.this, id);
							loadPosts(false);
						}
					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {
						closeProgressBar();
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
								viewPosts.this);
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
				});
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String returnResult = extras.getString("returnStatus");

			if (returnResult != null) {
				switch (requestCode) {
				case 0:
					if (returnResult.equals("OK")) {
						boolean uploadNow = false;
						uploadNow = extras.getBoolean("upload");
						if (uploadNow) {
							int uploadID = extras.getInt("newID");
							selectedID = String.valueOf(uploadID);
							showDialog(ID_DIALOG_POSTING);

							new Thread() {
								public void run() {

									try {
										submitResult = submitPost();

									} catch (IOException e) {
										e.printStackTrace();
									}

								}
							}.start();

						} else {
							loadPosts(false);
						}
					}
					break;
				case 1:
					if (returnResult.equals("OK")) {
						refreshPosts(false);
					}
					break;
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		/* Switch on the ID of the item, to get what the user selected. */
		if (item.getGroupId() == 0) {
			switch (item.getItemId()) {
			case 0:
				Intent i0 = new Intent(viewPosts.this, viewPost.class);
				i0.putExtra("postID", String.valueOf(selectedID));
				i0.putExtra("id", id);
				i0.putExtra("accountName", accountName);
				startActivity(i0);
				return true;
			case 1:
				Intent i = new Intent(viewPosts.this, viewComments.class);
				i.putExtra("postID", String.valueOf(selectedID));
				i.putExtra("id", id);
				i.putExtra("accountName", accountName);
				startActivity(i);
				return true;
			case 2:
				Intent i2 = new Intent(viewPosts.this, editPost.class);
				i2.putExtra("postID", String.valueOf(selectedID));
				i2.putExtra("id", id);
				i2.putExtra("accountName", accountName);
				startActivityForResult(i2, 1);
				return true;
			case 3:
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						viewPosts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.delete_post));
				dialogBuilder.setMessage(getResources().getText(
						R.string.delete_sure_post)
						+ " '" + titles[rowID] + "'?");
				dialogBuilder.setPositiveButton(getResources().getText(
						R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						showDialog(ID_DIALOG_DELETING);
						new Thread() {
							public void run() {
								deletePost();
							}
						}.start();

					}
				});
				dialogBuilder.setNegativeButton(getResources().getText(
						R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Just close the window.

					}
				});
				dialogBuilder.setCancelable(true);
				if (!isFinishing()) {
					dialogBuilder.create().show();
				}

				return true;
			case 4:
				loadingDialog = ProgressDialog.show(this, getResources().getText(R.string.share_url), getResources().getText(R.string.attempting_fetch_url), true, false);				
				Thread action = new Thread() { 
				  public void run() {
					  Looper.prepare();
					  shareURL(id, String.valueOf(selectedID), false);
					  Looper.loop();
				  } 
				};
				action.start();
				return true;
			}

		} else if (item.getGroupId() == 2) {
			switch (item.getItemId()) {
			case 0:
				Intent i0 = new Intent(viewPosts.this, viewPost.class);
				i0.putExtra("postID", String.valueOf(selectedID));
				i0.putExtra("id", id);
				i0.putExtra("accountName", accountName);
				i0.putExtra("isPage", true);
				startActivity(i0);
				return true;
			case 1:
				Intent i = new Intent(viewPosts.this, viewComments.class);
				i.putExtra("postID", String.valueOf(selectedID));
				i.putExtra("id", id);
				i.putExtra("accountName", accountName);
				startActivity(i);
				return true;
			case 2:
				Intent i2 = new Intent(viewPosts.this, editPost.class);
				i2.putExtra("postID", String.valueOf(selectedID));
				i2.putExtra("id", id);
				i2.putExtra("accountName", accountName);
				i2.putExtra("isPage", true);
				startActivityForResult(i2, 1);
				return true;
			case 3:
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						viewPosts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.delete_page));
				dialogBuilder.setMessage(getResources().getText(
						R.string.delete_sure_page)
						+ " '" + titles[rowID] + "'?");
				dialogBuilder.setPositiveButton(getResources().getText(
						R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						showDialog(ID_DIALOG_DELETING);
						new Thread() {
							public void run() {
								deletePost();
							}
						}.start();
					}
				});
				dialogBuilder.setNegativeButton(getResources().getText(
						R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Just close the window.

					}
				});
				dialogBuilder.setCancelable(true);
				if (!isFinishing()) {
					dialogBuilder.create().show();
				}
				return true;
			case 4:
				loadingDialog = ProgressDialog.show(this, getResources().getText(R.string.share_url), getResources().getText(R.string.attempting_fetch_url), true, false);				
				Thread action = new Thread() { 
				  public void run() {
					  Looper.prepare();
					  shareURL(id, String.valueOf(selectedID), true);
					  Looper.loop();
				  } 
				};
				action.start();
				return true;
			}

		} else {
			switch (item.getItemId()) {
			case 0:
				Intent i2 = new Intent(viewPosts.this, editPost.class);
				i2.putExtra("postID", String.valueOf(selectedID));
				i2.putExtra("id", id);
				if (isPage) {
					i2.putExtra("isPage", true);
				}
				i2.putExtra("accountName", accountName);
				i2.putExtra("localDraft", true);
				startActivityForResult(i2, 0);
				return true;
			case 1:
				showDialog(ID_DIALOG_POSTING);

				new Thread() {
					public void run() {

						try {
							submitResult = submitPost();

						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}.start();
				return true;
			case 2:
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
						viewPosts.this);
				dialogBuilder.setTitle(getResources().getText(
						R.string.delete_draft));
				dialogBuilder.setMessage(getResources().getText(
						R.string.delete_sure)
						+ " '" + titles[rowID] + "'?");
				dialogBuilder.setPositiveButton(getResources().getText(
						R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						WordPressDB lDraftsDB = new WordPressDB(viewPosts.this);
						if (isPage) {
							lDraftsDB.deletePageDraft(viewPosts.this, String
									.valueOf(selectedID));
						} else {
							lDraftsDB.deletePost(viewPosts.this, String
									.valueOf(selectedID));
						}
						loadPosts(false);

					}
				});
				dialogBuilder.setNegativeButton(getResources().getText(
						R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Just close the window.

					}
				});
				dialogBuilder.setCancelable(true);
				if (!isFinishing()) {
					dialogBuilder.create().show();
				}

			}
		}

		return false;
	}

	private void deletePost() {
		
		String selPostID = String.valueOf(selectedID);
		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

		Object[] postParams = { "", selPostID, blog.getUsername(), blog.getPassword() };
		Object[] pageParams = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), selPostID };

		
		try {
			client.call((isPage) ? "wp.deletePage"
					: "blogger.deletePost", (isPage) ? pageParams : postParams);
			dismissDialog(ID_DIALOG_DELETING);
			Thread action = new Thread() {
				public void run() {
					Toast.makeText(
							viewPosts.this,
							getResources().getText(
									(isPage) ? R.string.page_deleted
											: R.string.post_deleted),
							Toast.LENGTH_SHORT).show();
				}
			};
			this.runOnUiThread(action);
			Thread action2 = new Thread() {
				public void run() {
					refreshPosts(false);
				}
			};
			this.runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_DELETING);
			Thread action3 = new Thread() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							viewPosts.this);
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
			this.runOnUiThread(action3);
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_POSTING) {
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setTitle(getResources().getText(
					R.string.uploading_content));
			loadingDialog.setMessage(getResources().getText(
					(isPage) ? R.string.page_attempt_upload
							: R.string.post_attempt_upload));
			loadingDialog.setCancelable(false);
			return loadingDialog;
		} else if (id == ID_DIALOG_DELETING) {
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setTitle(getResources().getText(
					(isPage) ? R.string.delete_page : R.string.delete_post));
			loadingDialog.setMessage(getResources().getText(
					(isPage) ? R.string.attempt_delete_page
							: R.string.attempt_delete_post));
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
	}

	public String submitPost() throws IOException {

		// grab the form data
		final WordPressDB lDraftsDB = new WordPressDB(this);
		Vector<?> post;
		if (isPage) {
			post = lDraftsDB.loadPageDraft(this, String.valueOf(selectedID));
		} else {
			post = lDraftsDB.loadPost(this, String.valueOf(selectedID));
		}

		HashMap<?, ?> postHashMap = (HashMap<?, ?>) post.get(0);

		String title = postHashMap.get("title").toString();
		String content = StringHelper.convertHTMLTagsForUpload(postHashMap.get(
				"content").toString());
		String password = null;
		if(postHashMap.get("password") != null)
			password = postHashMap.get("password").toString();
		String picturePaths = postHashMap.get("picturePaths").toString();

		if (!picturePaths.equals("")) {
			String[] pPaths = picturePaths.split(",");

			for (int i = 0; i < pPaths.length; i++) {
				Uri imagePath = Uri.parse(pPaths[i]);
				selectedImageIDs.add(selectedImageCtr, imagePath);
				imageUrl.add(selectedImageCtr, pPaths[i]);
				selectedImageCtr++;
			}

		}

		String tags = "";
		if (!isPage) {
			String categories = postHashMap.get("categories").toString();
			if (!categories.equals("")) {

				String[] aCategories = categories.split(",");

				for (int i = 0; i < aCategories.length; i++) {
					selectedCategories.add(aCategories[i]);
				}

			}

			tags = postHashMap.get("tags").toString();

		}
		String status = "publish";
		if (postHashMap.get("status") != null){
			status = postHashMap.get("status").toString();
		}

		Boolean publishThis = false;

		String imageContent = "";
		boolean mediaError = false;
		if (selectedImageCtr > 0) { // did user add media to post?
			// upload the images and return the HTML
			String state = android.os.Environment.getExternalStorageState();
			if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
				// we need an SD card to submit media, stop this train!
				mediaError = true;
			} else {
				imageContent = uploadImages();
			}
		}
		String res = "";
		if (!mediaError) {

			Thread updateDialog = new Thread() {
				public void run() {
					loadingDialog.setMessage(getResources().getText(
							(isPage) ? R.string.page_attempt_upload
									: R.string.post_attempt_upload));
				}
			};
			this.runOnUiThread(updateDialog);

			String[] theCategories = new String[selectedCategories.size()];

			for (int i = 0; i < selectedCategories.size(); i++) {
				theCategories[i] = selectedCategories.get(i).toString();
			}

			//
			WordPressDB settingsDB = new WordPressDB(this);

			Map<String, Object> contentStruct = new HashMap<String, Object>();

			if (imageContent != "") {
				if (sImagePlacement.equals("Above Text")) {
					content = imageContent + content;
				} else {
					content = content + imageContent;
				}
			}

			if (!isPage) {
				// add the tagline
				HashMap<?, ?> globalSettings = settingsDB.getNotificationOptions(this);
				boolean taglineValue = false;
				String tagline = "";

				if (globalSettings != null) {
					if (globalSettings.get("tagline_flag").toString().equals(
							"1")) {
						taglineValue = true;
					}

					if (taglineValue) {
						tagline = globalSettings.get("tagline").toString();
						if (!tagline.equals("")) {
							content += "\n\n<span class=\"post_sig\">"
									+ tagline + "</span>\n\n";
						}
					}
				}
			}

			contentStruct.put("post_type", (isPage) ? "page" : "post");
			contentStruct.put("title", title);
			long pubDate = Long.parseLong(postHashMap.get("pubDate").toString());
	    	if (pubDate != 0){
	    		Date date = new Date(pubDate);
	    		contentStruct.put("date_created_gmt", date);
	    	}
			// for trac #53, add <p> and <br /> tags
			content = content.replace("/\n\n/g", "</p><p>");
			content = content.replace("/\n/g", "<br />");
			contentStruct.put("description", content);
			if (!isPage) {
				if (tags != "") {
					contentStruct.put("mt_keywords", tags);
				}
				if (theCategories.length > 0) {
					contentStruct.put("categories", theCategories);
				}
			}
			contentStruct.put((isPage) ? "page_status" : "post_status", status);
			Double latitude = 0.0;
			Double longitude = 0.0;
			if (!isPage) {
				latitude = (Double) postHashMap.get("latitude");
				longitude = (Double) postHashMap.get("longitude");

				if (latitude > 0) {
					HashMap<Object, Object> hLatitude = new HashMap<Object, Object>();
					hLatitude.put("key", "geo_latitude");
					hLatitude.put("value", latitude);

					HashMap<Object, Object> hLongitude = new HashMap<Object, Object>();
					hLongitude.put("key", "geo_longitude");
					hLongitude.put("value", longitude);

					HashMap<Object, Object> hPublic = new HashMap<Object, Object>();
					hPublic.put("key", "geo_public");
					hPublic.put("value", 1);

					Object[] geo = { hLatitude, hLongitude, hPublic };

					contentStruct.put("custom_fields", geo);
				}
			}
			
			client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());
			if(password != null && !"".equals(password)){
				contentStruct.put("wp_password", password);
			}
			Object[] params = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), contentStruct,
					publishThis };

			Object result = null;
			boolean success = false;
			try {
				result = (Object) client.call("metaWeblog.newPost", params);
				success = true;
			} catch (final XMLRPCException e) {
				Thread prompt = new Thread() {
					public void run() {
						dismissDialog(ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
								viewPosts.this);
						dialogBuilder.setTitle(getResources().getText(
								R.string.connection_error));
						dialogBuilder.setMessage(e.getLocalizedMessage());
						dialogBuilder.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										clearCounters();

									}
								});
						dialogBuilder.setCancelable(true);
						if (!isFinishing()) {
							dialogBuilder.create().show();
						}
					}
				};
				this.runOnUiThread(prompt);
			}

			if (success) {
				newID = result.toString();
				res = "OK";
				dismissDialog(ID_DIALOG_POSTING);
				Thread action = new Thread() {
					public void run() {
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
								viewPosts.this);
						dialogBuilder.setTitle(getResources().getText(
								R.string.success));
						if (xmlrpcError) {
							if (vpUpgrade) {
								dialogBuilder
										.setMessage(getResources().getText(
												(isPage) ? R.string.page_id
														: R.string.post_id)
												+ " "
												+ getResources()
														.getText(
																R.string.added_successfully_image_error)
												+ ": " + mediaErrorMsg);
								dialogBuilder.setNegativeButton(getResources()
										.getString(R.string.no),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												boolean isInteger = false;

												try {
													Integer.parseInt(newID);
													isInteger = true;
												} catch (NumberFormatException e) {

												}

												if (isInteger) {

													clearCounters();
													// post made it, so let's
													// delete the draft
													if (isPage) {
														lDraftsDB.deletePageDraft(viewPosts.this,String.valueOf(selectedID));
													} else {
														lDraftsDB.deletePost(viewPosts.this,String.valueOf(selectedID));
													}
													refreshPosts(false);
												}

											}
										});
							}
							dialogBuilder
									.setMessage(getResources().getText(
											(isPage) ? R.string.page_id
													: R.string.post_id)
											+ " "
											+ getResources()
													.getText(
															R.string.added_successfully_image_error)
											+ ": " + mediaErrorMsg);
						} else {
							dialogBuilder.setMessage(getResources().getText(
									(isPage) ? R.string.page_id
											: R.string.post_id)
									+ " "
									+ getResources().getText(
											R.string.added_successfully));
						}
						dialogBuilder.setPositiveButton(
								(vpUpgrade) ? getResources().getString(
										R.string.yes) : "OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {

										boolean isInteger = false;

										try {
											Integer.parseInt(newID);
											isInteger = true;
										} catch (NumberFormatException e) {

										}

										if (isInteger) {

											clearCounters();
											// post made it, so let's delete the
											// draft
											if (isPage) {
												lDraftsDB
														.deletePageDraft(
																viewPosts.this,
																String
																		.valueOf(selectedID));
											} else {
												lDraftsDB
														.deletePost(
																viewPosts.this,
																String
																		.valueOf(selectedID));
											}
											refreshPosts(false);
										}

										if (vpUpgrade) {
											String url = "http://videopress.com";
											Intent i = new Intent(
													Intent.ACTION_VIEW);
											i.setData(Uri.parse(url));
											startActivity(i);
										}

									}
								});
						dialogBuilder.setCancelable(true);
						if (!isFinishing()) {
							dialogBuilder.create().show();
						}

					}
				};
				this.runOnUiThread(action);
			}
		} else {
			Thread prompt = new Thread() {
				public void run() {
					dismissDialog(ID_DIALOG_POSTING);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
							viewPosts.this);
					dialogBuilder.setTitle(getResources().getText(
							R.string.sdcard_title));
					dialogBuilder.setMessage(getResources().getText(
							R.string.sdcard_message));
					dialogBuilder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// start over
									clearCounters();
								}

							});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()) {
						dialogBuilder.create().show();
					}
				}
			};
			this.runOnUiThread(prompt);
		}
		return res;
	}

	private void clearCounters() {
		// resets counter variables
		selectedImageIDs.clear();
		imageUrl.clear();
		selectedImageCtr = 0;
		selectedCategories.clear();
		xmlrpcError = false;

	}

	public String uploadImages() {
		String content = "";

		// images variables
		String finalThumbnailUrl = null;
		String finalImageUrl = null;

		//loop for multiple images

		for (int it = 0; it < selectedImageCtr; it++) {
			final int printCtr = it;
			Thread prompt = new Thread() {
				public void run() {
					loadingDialog.setMessage("Uploading Media File #"
							+ String.valueOf(printCtr + 1));
				}
			};
			this.runOnUiThread(prompt);
			// check for image, and upload it
			if (imageUrl.get(it) != null) {
				client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

				String curImagePath = "";

				curImagePath = imageUrl.get(it).toString();
				boolean video = false;
				if (curImagePath.contains("video")) {
					video = true;
				}

				if (video) { // upload the video

					Uri videoUri = Uri.parse(curImagePath);
					File fVideo = null;
					String mimeType = "", xRes = "", yRes = "";
					MediaFile mf = null;

					if (videoUri.toString().contains("content:")) { 
						String[] projection;
						Uri imgPath;

						projection = new String[] { Video.Media._ID,
								Video.Media.DATA, Video.Media.MIME_TYPE,
								Video.Media.RESOLUTION };
						imgPath = videoUri;

						Cursor cur = this.managedQuery(imgPath, projection,
								null, null, null);
						String thumbData = "";

						if (cur.moveToFirst()) {

							int mimeTypeColumn, resolutionColumn, dataColumn;

							dataColumn = cur.getColumnIndex(Video.Media.DATA);
							mimeTypeColumn = cur
									.getColumnIndex(Video.Media.MIME_TYPE);
							resolutionColumn = cur
									.getColumnIndex(Video.Media.RESOLUTION);
							
							mf = new MediaFile();

							thumbData = cur.getString(dataColumn);
							mimeType = cur.getString(mimeTypeColumn);
							fVideo = new File(thumbData);
							mf.setFilePath(fVideo.getPath());
							String resolution = cur.getString(resolutionColumn);
							if (resolution != null) {
								String[] resx = resolution.split("x");
								xRes = resx[0];
								yRes = resx[1];
							} else {
								// set the width of the video to the thumbnail
								// width, else 640x480
								if (!sMaxImageWidth.equals("Original Size")) {
									xRes = sMaxImageWidth;
									yRes = String.valueOf(Math.round(Integer
											.valueOf(sMaxImageWidth) * 0.75));
								} else {
									xRes = "640";
									yRes = "480";
								}

							}

						}
					} else { // file is not in media library
						fVideo = new File(videoUri.toString().replace(
								"file://", ""));
					}

					imageTitle = fVideo.getName();

					// try to upload the video
					HashMap<String, Object> m = new HashMap<String, Object>();

					m.put("name", imageTitle);
					m.put("type", mimeType);
					m.put("bits", mf);
					m.put("overwrite", true);

					Object[] params = { 1, blog.getUsername(), blog.getPassword(), m };

					Object result = null;

					try {
						result = (Object) client.call("wp.uploadFile", params);
					} catch (XMLRPCException e) {
						mediaErrorMsg = e.getLocalizedMessage();
						if (video) {
							if (mediaErrorMsg.contains("Invalid file type")) {
								mediaErrorMsg = getResources().getString(
										R.string.vp_upgrade);
								vpUpgrade = true;
							}
						}
						xmlrpcError = true;
						break;
					}

					HashMap<?, ?> contentHash = new HashMap<Object, Object>();

					contentHash = (HashMap<?, ?>) result;

					String resultURL = contentHash.get("url").toString();
					if (contentHash.containsKey("videopress_shortcode")) {
						resultURL = contentHash.get("videopress_shortcode")
								.toString()
								+ "\n";
					} else {
						resultURL = "<object classid=\"clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B\" width=\""
								+ xRes
								+ "\" height=\""
								+ (Integer.valueOf(yRes) + 16)
								+ "\" codebase=\"http://www.apple.com/qtactivex/qtplugin.cab\"><param name=\"scale\" value=\"aspect\"><param name=\"src\" value=\""
								+ resultURL
								+ "\" /><param name=\"autoplay\" value=\"false\" /><param name=\"controller\" value=\"true\" /><object type=\"video/quicktime\" data=\""
								+ resultURL
								+ "\" width=\""
								+ xRes
								+ "\" height=\""
								+ (Integer.valueOf(yRes) + 16)
								+ "\"><param name=\"scale\" value=\"aspect\"><param name=\"autoplay\" value=\"false\" /><param name=\"controller\" value=\"true\" /></object></object>\n";
					}

					content = content + resultURL;

				} // end video
				else {
					for (int i = 0; i < 2; i++) {

						curImagePath = imageUrl.get(it).toString();

						if (i == 0 || blog.isFullSizeImage()) {

							Uri imageUri = Uri.parse(curImagePath);
							File jpeg = null;
							String mimeType = "", orientation = "", path = "";
							MediaFile mf = null;

							if (imageUri.toString().contains("content:")) {
								String[] projection;
								Uri imgPath;

								projection = new String[] { Images.Media._ID,
										Images.Media.DATA,
										Images.Media.MIME_TYPE,
										Images.Media.ORIENTATION };
	
								imgPath = imageUri;

								Cursor cur = this.managedQuery(imgPath,
										projection, null, null, null);
								String thumbData = "";

								if (cur.moveToFirst()) {

									int dataColumn, mimeTypeColumn, orientationColumn;

									dataColumn = cur
											.getColumnIndex(Images.Media.DATA);
									mimeTypeColumn = cur
											.getColumnIndex(Images.Media.MIME_TYPE);
									orientationColumn = cur
											.getColumnIndex(Images.Media.ORIENTATION);

									mf = new MediaFile();
									orientation = cur
											.getString(orientationColumn);
									thumbData = cur.getString(dataColumn);
									mimeType = cur.getString(mimeTypeColumn);
									jpeg = new File(thumbData);
									path = thumbData;
									mf.setFilePath(jpeg.getPath());

								}
							} else { // file is not in media library
								mf = new MediaFile();
								path = imageUri.toString().replace("file://",
										"");
								jpeg = new File(path);
								mf.setFilePath(path);
							}
							
							//check if the file is now gone! (removed SD card, etc)
							if (jpeg == null)
							{
								xmlrpcError = true;
								mediaErrorMsg = "Media file not found.";
								break;
							}

							imageHelper ih = imageHelper.getInstance();
							orientation = ih.getExifOrientation(path,
									orientation);

							imageTitle = jpeg.getName();

							byte[] finalBytes = null;

							if (i == 0) {
								byte[] bytes = new byte[(int) jpeg.length()];

								DataInputStream in = null;
								try {
									in = new DataInputStream(
											new FileInputStream(jpeg));
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								}
								try {
									in.readFully(bytes);
								} catch (IOException e) {
									e.printStackTrace();
								}
								try {
									in.close();
								} catch (IOException e) {
									e.printStackTrace();
								}

								imageHelper ih2 = imageHelper.getInstance();
								finalBytes = ih2.createThumbnail(bytes,
										sMaxImageWidth, orientation, false);
							}

							// try to upload the image
							Map<String, Object> m = new HashMap<String, Object>();

							m.put("name", imageTitle);
							m.put("type", mimeType);
							if (i == 0) {
								m.put("bits", finalBytes);
							} else {
								m.put("bits", mf);
							}
							m.put("overwrite", true);

							Object[] params = { 1, blog.getUsername(), blog.getPassword(), m };

							Object result = null;

							try {
								result = (Object) client.call("wp.uploadFile",
										params);
							} catch (XMLRPCException e) {
								e.printStackTrace();
								e.getLocalizedMessage();
								xmlrpcError = true;
								break;
							}

							HashMap<?, ?> contentHash = new HashMap<Object, Object>();

							contentHash = (HashMap<?, ?>) result;

							String resultURL = contentHash.get("url")
									.toString();

							if (i == 0) {
								finalThumbnailUrl = resultURL;
							} else {
								if (blog.isFullSizeImage()) {
									finalImageUrl = resultURL;
								} else {
									finalImageUrl = "";
								}
							}

							// prepare the centering css if desired from user
							String centerCSS = " ";
							if (centerThumbnail) {
								centerCSS = "style=\"display:block;margin-right:auto;margin-left:auto;\" ";
							}

							if (i != 0 && blog.isFullSizeImage()) {
								if (resultURL != null) {

									if (sImagePlacement.equals("Above Text")) {
										content = content
												+ "<a alt=\"image\" href=\""
												+ finalImageUrl + "\"><img "
												+ centerCSS
												+ "alt=\"image\" src=\""
												+ finalThumbnailUrl
												+ "\" /></a>\n\n";
									} else {
										content = content
												+ "\n<a alt=\"image\" href=\""
												+ finalImageUrl + "\"><img "
												+ centerCSS
												+ "alt=\"image\" src=\""
												+ finalThumbnailUrl
												+ "\" /></a>";
									}

								}
							} else {
								if (i == 0 && blog.isFullSizeImage() == false
										&& resultURL != null) {

									if (sImagePlacement.equals("Above Text")) {

										content = content + "<img " + centerCSS
												+ "alt=\"image\" src=\""
												+ finalThumbnailUrl
												+ "\" />\n\n";
									} else {
										content = content + "\n<img "
												+ centerCSS
												+ "alt=\"image\" src=\""
												+ finalThumbnailUrl + "\" />";
									}

								}
							}

						} // end if statement

					}// end image check

				}

			}// end image stuff
		}// end new for loop

		return content;
	}

	interface XMLRPCMethodCallbackImages {
		void callFinished(Object result);
	}

	class XMLRPCMethodImages extends Thread {
		private String method;
		private Object[] params;
		private Handler handler;
		private XMLRPCMethodCallbackImages callBack;

		public XMLRPCMethodImages(String method,
				XMLRPCMethodCallbackImages callBack) {
			this.method = method;
			this.callBack = callBack;
		}

		public void call() throws InterruptedException {
			call(null);
		}

		public void call(Object[] params) throws InterruptedException {
			this.params = params;
			final Object result;
			try {
				result = (Object) client.call(method, params);
				callBack.callFinished(result);
			} catch (XMLRPCException e) {
				xmlrpcError = true;
				e.printStackTrace();
			}
		}

		@Override
		public void run() {

			try {
				final Object result;
				result = (Object) client.call(method, params);
				handler.post(new Runnable() {
					public void run() {

						callBack.callFinished(result);

					}
				});
			} catch (final XMLRPCFault e) {
				e.printStackTrace();

			} catch (final XMLRPCException e) {
				e.printStackTrace();
			}

		}
	}

	public void showProgressBar() {
		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(
				set, 0.5f);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);
		loading.setVisibility(View.VISIBLE);
		loading.setLayoutAnimation(controller);
	}

	public void closeProgressBar() {

		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, -1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(
				set, 0.5f);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);

		loading.setLayoutAnimation(controller);

		loading.setVisibility(View.INVISIBLE);
	}
	
	private void shareURL(String accountId, String postId, final boolean isPage) {

	    String errorStr = null;

		client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());
	    
	    Object versionResult = new Object();
	    try {
	    	if(isPage) {
	    		Object[] vParams = { blog.getBlogId(), postId, blog.getUsername(), blog.getPassword() };
	    		versionResult = (Object) client.call("wp.getPage", vParams);
	    	} else {
	    		Object[] vParams = { postId, blog.getUsername(), blog.getPassword() };
	    		versionResult = (Object) client.call("metaWeblog.getPost", vParams);
	    	}
		} catch (XMLRPCException e) {
			errorStr = e.getMessage();
			Log.d("WP", "Error", e);
		}
		
		if (errorStr == null && versionResult != null){
			try {
				HashMap<?, ?> contentHash = (HashMap<?, ?>) versionResult;
				
				if((isPage && !"publish".equals(contentHash.get("page_status").toString())) ||
						(!isPage && !"publish".equals(contentHash.get("post_status").toString()))) {
					Thread prompt = new Thread() {
						public void run() {
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
							dialogBuilder.setTitle(getResources().getText(R.string.share_url));
							if(isPage) {
								dialogBuilder.setMessage(viewPosts.this.getResources().getText(R.string.page_not_published));
							} else {
								dialogBuilder.setMessage(viewPosts.this.getResources().getText(R.string.post_not_published));
							}
							dialogBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {}
							});
							dialogBuilder.setCancelable(true);
							dialogBuilder.create().show();
						}
					};
					this.runOnUiThread(prompt);
				} else {
					String postURL = contentHash.get("permaLink").toString();
					String shortlink = getShortlinkTagHref(postURL);
					Intent share = new Intent(Intent.ACTION_SEND);
					share.setType("text/plain");
					if(shortlink == null) {
						share.putExtra(Intent.EXTRA_TEXT, postURL);						
					} else {
						share.putExtra(Intent.EXTRA_TEXT, shortlink);
					}
					share.putExtra(Intent.EXTRA_SUBJECT, contentHash.get("title").toString());
					startActivity(Intent.createChooser(share, this.getText(R.string.share_url)));
				}
			} catch (Exception e) {
				errorStr = e.getMessage();
				Log.d("WP", "Error", e);
			}
		}
		
		loadingDialog.dismiss();
		if(errorStr != null) {
			final String fErrorStr = errorStr; 
			Thread prompt = new Thread() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
					dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
					dialogBuilder.setMessage(fErrorStr);
					dialogBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {}
					});
					dialogBuilder.setCancelable(true);
					dialogBuilder.create().show();
				}
			};
			this.runOnUiThread(prompt);
		}
	}
	
	private String getShortlinkTagHref(String urlString) {
		InputStream in = getResponse(urlString);

		if(in != null) {
			XmlPullParser parser = Xml.newPullParser();
			try {
	            // auto-detect the encoding from the stream
	            parser.setInput(in, null);
	            int eventType = parser.getEventType();
	            while (eventType != XmlPullParser.END_DOCUMENT){
	                String name = null;
	                String rel="";
					String href="";
	                switch (eventType){
	                    case XmlPullParser.START_TAG:
	                        name = parser.getName();
	                            if (name.equalsIgnoreCase("link")){
	                            	for (int i = 0; i < parser.getAttributeCount(); i++) {
	      							  String attrName = parser.getAttributeName(i);
	      							  String attrValue = parser.getAttributeValue(i);
	      					           if(attrName.equals("rel")){
	      					        	   rel = attrValue;
	      					           } else if(attrName.equals("href")) {
	      					        	   href = attrValue;
	      					           }
	      					        }
	      							
	      						  if(rel.equals("shortlink")){
	      							  return href;
	      						  }
	                            }                          
	                        break;
	                }
	                eventType = parser.next();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }

		}
		return null;  //never found the shortlink tag
	}
	
	private InputStream getResponse(String urlString) {
		InputStream in = null;
		int response = -1;
        
        URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		} 
        URLConnection conn = null;
		try {
			conn = url.openConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
        
        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.addRequestProperty("user-agent", "Mozilla/5.0");
            httpConn.connect(); 

            response = httpConn.getResponseCode();                 
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();                                 
            }                     
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;           
        } 
		return in;
	}

}
