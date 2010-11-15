package org.wordpress.android;

import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class moderateCommentsTab extends ListActivity {
	private static final int[] IMAGE_IDS={R.id.avatar};
	private ThumbnailAdapter thumbs=null;
	private ArrayList<CommentEntry> model=null;
	private XMLRPCClient client;
	private String id = "", accountName = "", sUsername = "", sPassword = "", moderateErrorMsg = "", selectedPostID = "";
	int sBlogId;
	public Object[] origComments;
	public int[] changedStatuses;
	public HashMap<String, HashMap<?, ?>> allComments = new HashMap<String, HashMap<?, ?>>();
	public int ID_DIALOG_MODERATING = 1;
	public int ID_DIALOG_REPLYING = 2;
	public int ID_DIALOG_DELETING = 3;
	public boolean initializing = true;
	public int selectedID = 0;
	public int rowID = 0;
	public ProgressDialog pd;
	private ViewSwitcher switcher;
	private int numRecords = 0;
	boolean loadMore = false;
	int totalComments = 0;
	int commentsToLoad = 30;
	private Vector<String> checkedComments;
	private int checkedCommentTotal = 0; 
	private boolean inModeration = false;
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.moderatecomments);
		boolean fromNotification = false;
		Bundle extras = getIntent().getExtras();
		if(extras !=null)
		{
			id = extras.getString("id");
			accountName = extras.getString("accountName");
			pd = new ProgressDialog(this);
			fromNotification = extras.getBoolean("fromNotification", false);       		
		}      

		//create the ViewSwitcher in the current context
		switcher = new ViewSwitcher(this);
		Button footer = (Button)View.inflate(this, R.layout.list_footer_btn, null);
		footer.setText(getResources().getText(R.string.load_more) + " " + getResources().getText(R.string.tab_comments));

		footer.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				switcher.showNext();
				refreshComments(true, false, false);
			}
		});
		
		View progress = View.inflate(this, R.layout.list_footer_progress, null);

		switcher.addView(footer);
		switcher.addView(progress);

		if (fromNotification) //dismiss the notification 
		{
			//NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			//nm.cancel(22 + Integer.valueOf(id));
			//loadComments(false, false);
		}

		this.setTitle(accountName + " - Moderate Comments");

		boolean loadedComments = loadComments(false, false);

		if (!loadedComments){

			refreshComments(false, false, false);
		}

		final ImageButton refresh = (ImageButton) findViewById(R.id.refreshComments);   

		refresh.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {

				refreshComments(false, true, false);

			}
		});
		Button bulkEdit = (Button) findViewById(R.id.bulkEdit);   

		bulkEdit.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				inModeration = !inModeration;
				showOrHideBulkCheckBoxes();
			}
		}); 

		ImageButton deleteComments = (ImageButton) findViewById(R.id.deleteComment);   

		deleteComments.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				showDialog(ID_DIALOG_DELETING);
				new Thread() {
					public void run() { 
						Looper.prepare();
						deleteComments();
					}
				}.start();

			}
		});

		Button approveComments = (Button) findViewById(R.id.approveComment);   

		approveComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() { 
						Looper.prepare();
						moderateComments("approve");
					}
				}.start();
			}
		});

		Button unapproveComments = (Button) findViewById(R.id.unapproveComment);   

		unapproveComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() { 
						Looper.prepare();
						moderateComments("hold");
					}
				}.start();
			}
		});

		Button spamComments = (Button) findViewById(R.id.markSpam);   

		spamComments.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				showDialog(ID_DIALOG_MODERATING);
				new Thread() {
					public void run() { 
						Looper.prepare();
						moderateComments("spam");
					}
				}.start();
			}
		});

	}

	protected void showOrHideBulkCheckBoxes() {
		ListView listView = getListView();
		int loopMax = 0;
		if (listView.getFooterViewsCount() >= 1){
			//we don't want a checkmark on the footer view 
			if (listView.getLastVisiblePosition() == thumbs.getCount()){
				loopMax = listView.getChildCount() - 1;
			}
			else{
				loopMax = listView.getChildCount();
			}
		}
		else{
			loopMax = listView.getChildCount();
		}
		for (int i=0; i < loopMax;i++){
			RelativeLayout rl = (RelativeLayout) (View)listView.getChildAt(i).findViewById(R.id.bulkEditGroup);
			if (inModeration){
				showBulkCheckBoxes(rl);
			}
			else{
				hideBulkCheckBoxes(rl);
			}
		}

	}

	@SuppressWarnings("unchecked")
	protected void moderateComments(String newStatus) {
		//handles bulk moderation
		Vector<Object> settings = new Vector<Object>();
		WordPressDB settingsDB = new WordPressDB(moderateCommentsTab.this);
		settings = settingsDB.loadSettings(moderateCommentsTab.this, id);

		String sURL = "";
		if (settings.get(0).toString().contains("xmlrpc.php"))
		{
			sURL = settings.get(0).toString();
		}
		else
		{
			sURL = settings.get(0).toString() + "xmlrpc.php";
		}
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
		int sBlogId = Integer.parseInt(settings.get(10).toString());

		for (int i=0;i < checkedComments.size(); i++)
		{
			if (checkedComments.get(i).toString().equals("true")){

				client = new XMLRPCClient(sURL);

				CommentEntry listRow = (CommentEntry) getListView().getItemAtPosition(i);
				String curCommentID = listRow.commentID;

				HashMap contentHash, postHash = new HashMap();
				contentHash = (HashMap) allComments.get(curCommentID);
				postHash.put("status", newStatus);
				Date d = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");  
				String cDate = contentHash.get("commentDate").toString();
				try{  
					d = sdf.parse(cDate);
				} catch (ParseException pe){  
					pe.printStackTrace();  
				}  
				postHash.put("date_created_gmt", d);
				postHash.put("content", contentHash.get("comment"));
				postHash.put("author", contentHash.get("author"));
				postHash.put("author_url", contentHash.get("url"));
				postHash.put("author_email", contentHash.get("email"));

				Object[] params = {
						sBlogId,
						sUsername,
						sPassword,
						curCommentID,
						postHash
				};

				Object result = null;
				try {
					result = (Object) client.call("wp.editComment", params);
					boolean bResult = Boolean.parseBoolean(result.toString());
					if (bResult){
						checkedComments.set(i, "false");
						listRow.status = newStatus;
						model.set(i, listRow);
						settingsDB.updateCommentStatus(moderateCommentsTab.this, id, listRow.commentID, newStatus);
					}
				} catch (XMLRPCException e) {
					moderateErrorMsg = e.getLocalizedMessage();
				}	
			}
		}
		dismissDialog(ID_DIALOG_MODERATING);
		Thread action = new Thread() 
		{ 
			public void run() 
			{
				if (moderateErrorMsg == ""){
					Toast.makeText(moderateCommentsTab.this, getResources().getText(R.string.comments_moderated), Toast.LENGTH_SHORT).show();
				}
				else{
					//there was an xmlrpc error
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
					dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
					dialogBuilder.setMessage(moderateErrorMsg);
					dialogBuilder.setPositiveButton("OK",  new
							DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Just close the window.

						}
					});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()){
						dialogBuilder.create().show();
					}
				}
			} 
		}; 
		this.runOnUiThread(action);
		if (moderateErrorMsg == ""){
			//no errors, refresh list
			checkedCommentTotal = 0;
			inModeration = false;
			Thread action2 = new Thread() 
			{ 
				public void run() 
				{
					pd = new ProgressDialog(moderateCommentsTab.this);  // to avoid crash
					showOrHideBulkCheckBoxes();
					hideModerationBar();
					thumbs.notifyDataSetChanged();
				} 
			}; 
			this.runOnUiThread(action2);
		}
	}

	protected void hideBulkCheckBoxes(RelativeLayout rl) {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);
		rl.startAnimation(set);
		rl.setVisibility(View.GONE);
		if (checkedCommentTotal > 0){
			hideModerationBar();
		}
	}

	protected void showBulkCheckBoxes(RelativeLayout rl) {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);
		rl.setVisibility(View.VISIBLE);
		rl.startAnimation(set);
		if (checkedCommentTotal > 0){
			showModerationBar();
		}
	}

	protected void deleteComments() {
		//bulk detete comments
		Vector<Object> settings = new Vector<Object>();
		WordPressDB settingsDB = new WordPressDB(moderateCommentsTab.this);
		settings = settingsDB.loadSettings(moderateCommentsTab.this, id);
		String sURL = "";
		if (settings.get(0).toString().contains("xmlrpc.php"))
		{
			sURL = settings.get(0).toString();
		}
		else
		{
			sURL = settings.get(0).toString() + "xmlrpc.php";
		}
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
		int sBlogId = Integer.parseInt(settings.get(10).toString());
		
		for (int i=0;i < checkedComments.size(); i++)
		{
			if (checkedComments.get(i).toString().equals("true")){

				client = new XMLRPCClient(sURL);

				CommentEntry listRow = (CommentEntry) getListView().getItemAtPosition(i);
				String curCommentID = listRow.commentID;

				Object[] params = {
						sBlogId,
						sUsername,
						sPassword,
						curCommentID
				};

				try {
					client.call("wp.deleteComment", params);
				} catch (final XMLRPCException e) {
					moderateErrorMsg = e.getLocalizedMessage();
				}
			}
		}
		dismissDialog(ID_DIALOG_DELETING);
		Thread action = new Thread() 
		{ 
			public void run() 
			{
				if (moderateErrorMsg == ""){
					Toast.makeText(moderateCommentsTab.this, getResources().getText(R.string.comment_moderated), Toast.LENGTH_SHORT).show();
				}
				else{
					//error occured during delete request
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
					dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
					dialogBuilder.setMessage(moderateErrorMsg);
					dialogBuilder.setPositiveButton("OK",  new
							DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Just close the window.

						}
					});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()){
						dialogBuilder.create().show();
					}
				}
			} 
		}; 
		this.runOnUiThread(action);
		Thread action2 = new Thread() 
		{ 
			public void run() 
			{
				if (moderateErrorMsg == ""){
				pd = new ProgressDialog(moderateCommentsTab.this);  // to avoid crash
				refreshComments(false, true, true);
				}
			} 
		}; 
		this.runOnUiThread(action2);
		checkedCommentTotal = 0;
		inModeration = false;

	}

	@SuppressWarnings("unchecked")
	private boolean loadComments(boolean addMore, boolean refreshOnly) {
		WordPressDB postStoreDB = new WordPressDB(this);
		String author, postID, commentID, comment, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
		if (!addMore){
			Vector<?> loadedPosts = postStoreDB.loadComments(moderateCommentsTab.this, id);
			if (loadedPosts != null){
				HashMap<Object, Object> countHash = new HashMap<Object, Object>();
				countHash = (HashMap) loadedPosts.get(0);
				numRecords = Integer.parseInt(countHash.get("numRecords").toString());
				if (refreshOnly){
					if (model != null){
						model.clear();
					}
				}	
				else{
					model=new ArrayList<CommentEntry>();
				}
				
				//fixes trac #72 (1.5 bug)
				int sdk_int = 0;
				try {
					sdk_int = Integer.valueOf(android.os.Build.VERSION.SDK);
				} catch (Exception e1) {
					sdk_int = 3; //assume they are on cupcake
				}
				
				checkedComments = new Vector();
				for (int i=1; i < loadedPosts.size(); i++){
					checkedComments.add(i-1, "false");
					HashMap contentHash = (HashMap) loadedPosts.get(i);
					allComments.put(contentHash.get("commentID").toString(), contentHash);
					author = escapeUtils.unescapeHtml(contentHash.get("author").toString());
					commentID = contentHash.get("commentID").toString();
					postID = contentHash.get("postID").toString();
					comment = escapeUtils.unescapeHtml(contentHash.get("comment").toString());
					dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
					status = contentHash.get("status").toString();
					authorEmail = escapeUtils.unescapeHtml(contentHash.get("email").toString());
					authorURL = escapeUtils.unescapeHtml(contentHash.get("url").toString());
					postTitle = escapeUtils.unescapeHtml(contentHash.get("postTitle").toString());
					
					//more 1.5 htc sense fix
					if (sdk_int == 3){
						postTitle = postTitle.replace("Ô", "'");
						postTitle = postTitle.replace("Õ", "'");
						postTitle = postTitle.replace('Ó', '"');
						postTitle = postTitle.replace('Ò', '"');
						postTitle = postTitle.replace('Ð', '-');
						postTitle = postTitle.replaceAll("[^a-zA-Z0-9\'\"-]", " ");
						author = author.replaceAll("[^a-zA-Z0-9\'\"-]", " ");
						authorURL = authorURL.replaceAll("[^a-zA-Z0-9:'/'/.-]", " ");
					}
					
					if (model == null){
						model=new ArrayList<CommentEntry>();
					}

					//add to model
					model.add(new CommentEntry(postID,
							commentID, 
							author,
							dateCreatedFormatted,
							comment,
							status,
							postTitle,
							authorURL,
							authorEmail,
							URI.create("http://gravatar.com/avatar/" + getMd5Hash(authorEmail.trim()) + "?s=60&d=identicon")));
				}

				if (!refreshOnly){
					try {
						ThumbnailBus bus = new ThumbnailBus();
						thumbs=new ThumbnailAdapter(this, new CommentAdapter(),new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus),IMAGE_IDS);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					ListView listView = (ListView) findViewById(android.R.id.list);
					listView.removeFooterView(switcher);
					if (loadedPosts.size() >= 30){
						listView.addFooterView(switcher);
					}
					setListAdapter(thumbs);
					
					listView.setOnItemClickListener(new OnItemClickListener() {

						public void onItemClick(AdapterView<?> arg0, View arg1,
								int position, long arg3) {
							Intent intent = new Intent(moderateCommentsTab.this, viewComment.class);
							//intent.putExtra("pageID", pageIDs[(int) arg3]);
							//intent.putExtra("postTitle", titles[(int) arg3]);
							intent.putExtra("id", id);
							intent.putExtra("accountName", accountName);
							intent.putExtra("comment", model.get((int) arg3).comment);
							intent.putExtra("name", model.get((int) arg3).name);
							intent.putExtra("email", model.get((int) arg3).authorEmail);
							intent.putExtra("url", model.get((int) arg3).authorURL);
							intent.putExtra("date", model.get((int) arg3).dateCreatedFormatted);
							intent.putExtra("status", model.get((int) arg3).status);
							intent.putExtra("comment_id", model.get((int) arg3).commentID);
							intent.putExtra("post_id", model.get((int) arg3).postID);
							intent.putExtra("position", position);
							startActivityForResult(intent, 1);
						}
					});

					listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

						public void onCreateContextMenu(ContextMenu menu, View v,
								ContextMenuInfo menuInfo) {
							AdapterView.AdapterContextMenuInfo info;
							try {
								info = (AdapterView.AdapterContextMenuInfo) menuInfo;
							} catch (ClassCastException e) {
								//Log.e(TAG, "bad menuInfo", e);
								return;
							}

							selectedID = info.targetView.getId();
							rowID = info.position;
							selectedPostID = model.get(info.position).postID;

							menu.setHeaderTitle(getResources().getText(R.string.comment_actions));
							menu.add(0, 0, 0, getResources().getText(R.string.mark_approved));
							menu.add(0, 1, 0, getResources().getText(R.string.mark_unapproved));
							menu.add(0, 2, 0, getResources().getText(R.string.mark_spam));
							menu.add(0, 3, 0, getResources().getText(R.string.reply));
							menu.add(0, 4, 0, getResources().getText(R.string.delete));
						}
					});
				}
				else{
					thumbs.notifyDataSetChanged();
				}
				return true;
			}
			else{
				return false;
			}
		}
		else{
			Vector latestComments = postStoreDB.loadMoreComments(moderateCommentsTab.this, id, commentsToLoad);
			if (latestComments != null){
				numRecords += latestComments.size();
				for (int i=latestComments.size(); i > 0; i--){
					HashMap contentHash = (HashMap) latestComments.get(i-1);
					allComments.put(contentHash.get("commentID").toString(), contentHash);
					author = escapeUtils.unescapeHtml(contentHash.get("author").toString());
					commentID = contentHash.get("commentID").toString();
					postID = contentHash.get("postID").toString();
					comment = escapeUtils.unescapeHtml(contentHash.get("comment").toString());
					dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
					status = contentHash.get("status").toString();
					authorEmail = escapeUtils.unescapeHtml(contentHash.get("email").toString());
					authorURL = escapeUtils.unescapeHtml(contentHash.get("url").toString());
					postTitle = escapeUtils.unescapeHtml(contentHash.get("postTitle").toString());

					//add to model
					model.add(new CommentEntry(postID,
							commentID, 
							author,
							dateCreatedFormatted,
							comment,
							status,
							postTitle,
							authorURL,
							authorEmail,
							URI.create("http://gravatar.com/avatar/" + getMd5Hash(authorEmail.trim()) + "?s=60&d=identicon")));
				}
				thumbs.notifyDataSetChanged();
			}
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private void refreshComments(final boolean loadMore, final boolean refreshOnly, final boolean doInBackground) {

		if (!loadMore && !doInBackground){
			showProgressBar();
		}

		Vector<Object> settings = new Vector<Object>();
		WordPressDB settingsDB = new WordPressDB(this);
		settings = settingsDB.loadSettings(this, id); 

		String sURL = "";
		if (settings.get(0).toString().contains("xmlrpc.php"))
		{
			sURL = settings.get(0).toString();
		}
		else
		{
			sURL = settings.get(0).toString() + "xmlrpc.php";
		}
		sUsername = settings.get(2).toString();
		sPassword = settings.get(3).toString();
		sBlogId = Integer.parseInt(settings.get(10).toString());

		client = new XMLRPCClient(sURL);

		HashMap hPost = new HashMap();
		hPost.put("status", "");
		hPost.put("post_id", "");
		if (loadMore){
			hPost.put("offset", numRecords);
		}
		if (totalComments != 0 && ((totalComments - numRecords) < 30)){
			commentsToLoad = totalComments - numRecords;
			hPost.put("number", commentsToLoad);
		}
		else{
			hPost.put("number", 30);
		}

		XMLRPCMethod method = new XMLRPCMethod("wp.getComments", new XMLRPCMethodCallback() {
			public void callFinished(Object[] result) {
				if (result.length == 0){
					// no comments found
					if (pd.isShowing())
					{
						pd.dismiss();
					}
				}
				else{
					origComments = result;
					String author, postID, commentID, comment, dateCreated, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;

					HashMap contentHash = new HashMap();
					Vector dbVector = new Vector();

					Date d = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
					Calendar cal = Calendar.getInstance();
					TimeZone tz = cal.getTimeZone();
					String shortDisplayName = "";
					shortDisplayName = tz.getDisplayName(true, TimeZone.SHORT);
					if (result.length < 30){
						//end of list reached
						getListView().removeFooterView(switcher);
					}
					//loop this!
					for (int ctr = 0; ctr < result.length; ctr++){
						if (loadMore){
							checkedComments.add("false"); 
						}
						HashMap<String, String> dbValues = new HashMap();
						contentHash = (HashMap) result[ctr];
						allComments.put(contentHash.get("comment_id").toString(), contentHash);
						comment = contentHash.get("content").toString();
						author = contentHash.get("author").toString();
						status = contentHash.get("status").toString();
						postID = contentHash.get("post_id").toString();
						commentID = contentHash.get("comment_id").toString();
						dateCreated = contentHash.get("date_created_gmt").toString();
						authorURL = contentHash.get("author_url").toString();
						authorEmail = contentHash.get("author_email").toString();
						postTitle = contentHash.get("post_title").toString();

						//make the date pretty
						String cDate = dateCreated.replace(tz.getID(), shortDisplayName);
						try{  
							d = sdf.parse(cDate);
							SimpleDateFormat sdfOut = new SimpleDateFormat("MMMM dd, yyyy hh:mm a"); 
							dateCreatedFormatted = sdfOut.format(d);
						} catch (ParseException pe){  
							pe.printStackTrace();
							dateCreatedFormatted = dateCreated;  //just make it the ugly date if it doesn't work
						} 

						dbValues.put("blogID", id);
						dbValues.put("postID", postID);
						dbValues.put("commentID", commentID);
						dbValues.put("author", author);
						dbValues.put("comment", comment);
						dbValues.put("commentDate", dateCreated);
						dbValues.put("commentDateFormatted", dateCreatedFormatted);
						dbValues.put("status", status);
						dbValues.put("url", authorURL);
						dbValues.put("email", authorEmail);
						dbValues.put("postTitle", postTitle);
						dbVector.add(ctr, dbValues);
					}

					WordPressDB postStoreDB = new WordPressDB(moderateCommentsTab.this);
					postStoreDB.saveComments(moderateCommentsTab.this, dbVector, loadMore);

					if (!doInBackground){
						loadComments(loadMore, refreshOnly);
					}

					if (pd.isShowing())
					{
						pd.dismiss();
					}

				}  
				
				if (!loadMore && !doInBackground){
					closeProgressBar();
				}
				else if (loadMore){
					switcher.showPrevious();
				}

			}
		});
		Object[] params = {
				sBlogId,
				sUsername,
				sPassword,
				hPost
		};

		method.call(params);

	}

	public void showProgressBar() {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);
		LayoutAnimationController controller =
			new LayoutAnimationController(set, 0.5f);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);       
		loading.setVisibility(View.VISIBLE);
		loading.setLayoutAnimation(controller);
	}

	public void closeProgressBar() {

		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, -1.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);       
		loading.startAnimation(set);
		loading.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return(model);
	}

	private void goBlooey(Throwable t) {
		Log.e("WordPress", "Exception!", t);

		AlertDialog.Builder builder=new AlertDialog.Builder(this);

		builder
		.setTitle("Error")
		.setMessage(t.toString())
		.setPositiveButton("OK", null)
		.show();
	}

	class CommentEntry {
		String postID="";
		String commentID="";
		String name="";
		String emailURL="";
		String status="";
		String comment="";
		String postTitle="";
		String authorURL="";
		String authorEmail="";
		String dateCreatedFormatted="";
		URI profileImageUrl=null;

		CommentEntry(String postID, String commentID, String name, String dateCreatedFormatted,
				String comment, String status, String postTitle, String authorURL, String authorEmail, URI profileImageUrl) {
			this.postID=postID;
			this.commentID=commentID;
			this.name=name;
			this.emailURL=authorEmail;
			this.status=status;
			this.comment=comment;
			this.postTitle=postTitle;
			this.authorURL=authorURL;
			this.authorEmail=authorEmail;
			this.profileImageUrl=profileImageUrl;
			this.dateCreatedFormatted=dateCreatedFormatted;
		}
	}

	class CommentAdapter extends ArrayAdapter<CommentEntry> {
		CommentAdapter() {
			super(moderateCommentsTab.this, R.layout.row, model);
		}

		public View getView(int position, View convertView,
				ViewGroup parent) {
			View row=convertView;
			CommentEntryWrapper wrapper=null;

			if (row==null) {													
				LayoutInflater inflater=getLayoutInflater();

				row=inflater.inflate(R.layout.row, null);
				wrapper=new CommentEntryWrapper(row);
				row.setTag(wrapper);
			}
			else {
				wrapper=(CommentEntryWrapper)row.getTag();
			}
			row.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_bg_selector));
			wrapper.populateFrom(getItem(position), position);

			return(row);
		}
	}

	class CommentEntryWrapper {
		private TextView name=null;
		private TextView emailURL=null;
		private TextView comment=null;
		private TextView status=null;
		private TextView postTitle=null;
		private ImageView avatar=null;
		private View row=null;
		private CheckBox bulkCheck=null;
		private RelativeLayout bulkEditGroup=null;

		CommentEntryWrapper(View row) {
			this.row=row;

		}

		void populateFrom(CommentEntry s, final int position) {
			getName().setText(s.name);

			String fEmailURL = s.authorURL;
			// use the required email address if the commenter didn't leave a url
			if (fEmailURL == ""){
				fEmailURL = s.emailURL;
			}

			getEmailURL().setText(fEmailURL);
			getComment().setText(s.comment);
			getPostTitle().setText(getResources().getText(R.string.on) + " " + s.postTitle);

			row.setId(Integer.valueOf(s.commentID));

			String prettyComment,textColor = "";

			if (s.status.equals("spam")){
				prettyComment = getResources().getText(R.string.spam).toString();
				textColor = "#FF0000";
			}
			else if (s.status.equals("hold")){
				prettyComment = getResources().getText(R.string.unapproved).toString();
				textColor = "#D54E21";
			}
			else{
				prettyComment = getResources().getText(R.string.approved).toString();
				textColor = "#006505";
			}

			if (inModeration){
				getBulkEditGroup().setVisibility(View.VISIBLE);
			}
			else{
				getBulkEditGroup().setVisibility(View.GONE);
			}

			getStatus().setText(prettyComment);
			getStatus().setTextColor(Color.parseColor(textColor));

			getBulkCheck().setChecked(Boolean.parseBoolean(checkedComments.get(position).toString()));
			getBulkCheck().setTag(position);
			getBulkCheck().setOnClickListener(new OnClickListener() { 

				public void onClick(View arg0) { 
					checkedComments.set(position, String.valueOf(getBulkCheck().isChecked()));
					showOrHideModerateButtons();
				} 
			}); 

			if (s.profileImageUrl!=null) {
				try {
					getAvatar().setImageResource(R.drawable.placeholder);
					getAvatar().setTag(s.profileImageUrl.toString());
				}
				catch (Throwable t) {
					goBlooey(t);
				}
			}
		}

		TextView getName() {
			if (name==null) {
				name=(TextView)row.findViewById(R.id.name);
			}

			return(name);
		}

		TextView getEmailURL() {
			if (emailURL==null) {
				emailURL=(TextView)row.findViewById(R.id.email_url);
			}

			return(emailURL);
		}

		TextView getComment() {
			if (comment==null) {
				comment=(TextView)row.findViewById(R.id.comment);
			}

			return(comment);
		}

		TextView getStatus() {
			if (status==null) {
				status=(TextView)row.findViewById(R.id.status);
			}

			status.setTextSize(10);

			return(status);
		}

		TextView getPostTitle() {
			if (postTitle==null) {
				postTitle=(TextView)row.findViewById(R.id.postTitle);
			}

			return(postTitle);
		}

		ImageView getAvatar() {
			if (avatar==null) {
				avatar=(ImageView)row.findViewById(R.id.avatar);
			}

			return(avatar);
		}

		CheckBox getBulkCheck() {
			if (bulkCheck==null) {
				bulkCheck=(CheckBox)row.findViewById(R.id.bulkCheck);
			}

			return(bulkCheck);
		}

		RelativeLayout getBulkEditGroup() {
			if (bulkEditGroup==null) {
				bulkEditGroup=(RelativeLayout)row.findViewById(R.id.bulkEditGroup);
			}

			return(bulkEditGroup);
		}

		protected void showOrHideModerateButtons() {
			int previousTotal = checkedCommentTotal;
			checkedCommentTotal = 0;
			for (int i=0;i < checkedComments.size();i++){
				if (checkedComments.get(i).equals("true")){
					checkedCommentTotal++;
				}
			}
			if (checkedCommentTotal > 0 && previousTotal == 0){
				showModerationBar();
			}
			if (checkedCommentTotal == 0 && previousTotal > 0){

				hideModerationBar();

			}

		}
	}

	public static String getMd5Hash(String input) {
		try     {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1,messageDigest);
			String md5 = number.toString(16);

			while (md5.length() < 32)
				md5 = "0" + md5;

			return md5;
		} catch(NoSuchAlgorithmException e) {
			Log.e("MD5", e.getLocalizedMessage());
			return null;
		}
	}

	public void hideModerationBar() {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);
		RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.moderationBar);       
		moderationBar.clearAnimation();
		moderationBar.startAnimation(set);
		moderationBar.setVisibility(View.INVISIBLE);

	}

	public void showModerationBar(){
		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 0.0f
		);
		animation.setDuration(500);
		set.addAnimation(animation);
		RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.moderationBar);       
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
				//get the total comments
				HashMap<Object, Object> countResult = new HashMap<Object, Object>();
				Object[] countParams = {
						sBlogId,
						sUsername,
						sPassword,
						0
				};
				try {
					countResult = (HashMap) client.call("wp.getCommentCount", countParams);
					totalComments = Integer.valueOf(countResult.get("awaiting_moderation").toString()) + Integer.valueOf(countResult.get("approved").toString());
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
						if (pd.isShowing())
						{
							pd.dismiss();
						}
						closeProgressBar();
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
						String msg = e.getLocalizedMessage();
						dialogBuilder.setMessage(e.getFaultString());
						if (msg.contains("403")){
							dialogBuilder.setMessage(e.getFaultString() + " " + getResources().getString(R.string.load_settings));
							dialogBuilder.setPositiveButton(getResources().getString(R.string.yes),  new
									DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									Intent i = new Intent(moderateCommentsTab.this, settings.class);
									i.putExtra("id", id);
									i.putExtra("accountName", accountName);
									startActivity(i);

								}
							});
							
							dialogBuilder.setNegativeButton(getResources().getString(R.string.no),  new
									DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Just close the window.

								}
							});
						}
						else{
						dialogBuilder.setPositiveButton("OK",  new
								DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Just close the window.

							}
						});
						}
						dialogBuilder.setCancelable(true);
						if (!isFinishing()){
							dialogBuilder.create().show();
						}
					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {
						if (pd.isShowing())
						{
							pd.dismiss();
						}
						closeProgressBar();
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
						dialogBuilder.setMessage(e.getLocalizedMessage());
						dialogBuilder.setPositiveButton("OK",  new
								DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Just close the window.

							}
						});
						dialogBuilder.setCancelable(true);
						if (!isFinishing()){
							dialogBuilder.create().show();
						}
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
		public XMLRPCMethodEditComment(String method, XMLRPCMethodCallbackEditComment callBack) {
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
						dismissDialog(ID_DIALOG_MODERATING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
						dialogBuilder.setMessage(e.getFaultString());
						dialogBuilder.setPositiveButton("OK",  new
								DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Just close the window.

							}
						});
						dialogBuilder.setCancelable(true);
						if (!isFinishing()){
							dialogBuilder.create().show();
						}
					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {
						dismissDialog(ID_DIALOG_MODERATING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
						dialogBuilder.setMessage(e.getLocalizedMessage());
						dialogBuilder.setPositiveButton("OK",  new
								DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Just close the window.
							}
						});
						dialogBuilder.setCancelable(true);
						if (!isFinishing()){
							dialogBuilder.create().show();
						}
					}
				});
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == ID_DIALOG_MODERATING){
			ProgressDialog loadingDialog = new ProgressDialog(this);
			if (checkedCommentTotal <= 1){
				loadingDialog.setMessage(getResources().getText(R.string.moderating_comment));
			}
			else{
				loadingDialog.setMessage(getResources().getText(R.string.moderating_comments));
			}
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}
		else if (id == ID_DIALOG_REPLYING){
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getResources().getText(R.string.replying_comment));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}
		else if (id == ID_DIALOG_DELETING){
			ProgressDialog loadingDialog = new ProgressDialog(this);
			if (checkedCommentTotal <= 1){
				loadingDialog.setMessage(getResources().getText(R.string.deleting_comment));
			}
			else{
				loadingDialog.setMessage(getResources().getText(R.string.deleting_comments));
			}
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
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
			showDialog(ID_DIALOG_MODERATING);
			new Thread() {
				public void run() {
					Looper.prepare();
					changeCommentStatus("approve", selectedID, rowID);
				}
			}.start();
			return true;
		case 1:
			showDialog(ID_DIALOG_MODERATING);
			new Thread() {
				public void run() { 
					Looper.prepare();
					changeCommentStatus("hold", selectedID, rowID);
				}
			}.start();

			return true;
		case 2:
			showDialog(ID_DIALOG_MODERATING);
			new Thread() {
				public void run() { 
					Looper.prepare();
					changeCommentStatus("spam", selectedID, rowID);
				}
			}.start();
			return true;
		case 3:
			Intent i = new Intent(this, replyToComment.class);
			i.putExtra("commentID", selectedID);
			i.putExtra("accountName", accountName);
			i.putExtra("postID", selectedPostID);
			startActivityForResult(i, 0);

			return true;
		case 4:
			showDialog(ID_DIALOG_DELETING);
			new Thread() {
				public void run() { 
					Looper.prepare();
					deleteComment(selectedID);
				}
			}.start();
			return true;

		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void changeCommentStatus(final String newStatus, final int selCommentID, int position) {
		//for individual comment moderation
		String sSelCommentID = String.valueOf(selCommentID);
		ListView lv = getListView();
		CommentEntry ce = (CommentEntry)lv.getItemAtPosition(position);
		Vector<Object> settings = new Vector<Object>();
		WordPressDB settingsDB = new WordPressDB(moderateCommentsTab.this);
		settings = settingsDB.loadSettings(moderateCommentsTab.this, id);
		String sURL = "";
		if (settings.get(0).toString().contains("xmlrpc.php"))
		{
			sURL = settings.get(0).toString();
		}
		else
		{
			sURL = settings.get(0).toString() + "xmlrpc.php";
		}
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
		int sBlogId = Integer.parseInt(settings.get(10).toString());

		client = new XMLRPCClient(sURL);

		HashMap contentHash, postHash = new HashMap();
		contentHash = (HashMap) allComments.get(sSelCommentID);
		postHash.put("status", newStatus);
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");  
		String cDate = contentHash.get("commentDate").toString();
		try{  
			d = sdf.parse(cDate);
		} catch (ParseException pe){  
			pe.printStackTrace();  
		}  
		postHash.put("date_created_gmt", d);
		postHash.put("content", contentHash.get("comment"));
		postHash.put("author", contentHash.get("author"));
		postHash.put("author_url", contentHash.get("url"));
		postHash.put("author_email", contentHash.get("email"));


		Object[] params = {
				sBlogId,
				sUsername,
				sPassword,
				sSelCommentID,
				postHash
		};

		Object result = null;
		try {
			result = (Object) client.call("wp.editComment", params);
			boolean bResult = Boolean.parseBoolean(result.toString());
			if (bResult){
				ce.status = newStatus;
				model.set(position, ce);
				settingsDB.updateCommentStatus(moderateCommentsTab.this, id, ce.commentID, newStatus);
			}
			dismissDialog(ID_DIALOG_MODERATING);
			Thread action = new Thread() 
			{ 
				public void run() 
				{
					Toast.makeText(moderateCommentsTab.this, getResources().getText(R.string.comment_moderated), Toast.LENGTH_SHORT).show();
				} 
			}; 
			this.runOnUiThread(action);
			Thread action2 = new Thread() 
			{ 
				public void run() 
				{
					thumbs.notifyDataSetChanged();				  
				} 
			}; 
			this.runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_MODERATING);
			Thread action3 = new Thread() 
			{ 
				public void run() 
				{
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
					dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
					dialogBuilder.setMessage(e.getLocalizedMessage());
					dialogBuilder.setPositiveButton("OK",  new
							DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Just close the window.

						}
					});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()){
						dialogBuilder.create().show();
					}
				}
			}; 
			this.runOnUiThread(action3);
		}	
	}

	private void deleteComment(final int selCommentID) {
		//delete individual comment
		Vector<Object> settings = new Vector<Object>();
		WordPressDB settingsDB = new WordPressDB(moderateCommentsTab.this);
		settings = settingsDB.loadSettings(moderateCommentsTab.this, id);

		String sURL = "";
		if (settings.get(0).toString().contains("xmlrpc.php"))
		{
			sURL = settings.get(0).toString();
		}
		else
		{
			sURL = settings.get(0).toString() + "xmlrpc.php";
		}
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
		int sBlogId = Integer.parseInt(settings.get(10).toString());

		client = new XMLRPCClient(sURL);

		Object[] params = {
				sBlogId,
				sUsername,
				sPassword,
				selCommentID
		};

		try {
			client.call("wp.deleteComment", params);
			dismissDialog(ID_DIALOG_DELETING);
			Thread action = new Thread() 
			{ 
				public void run() 
				{
					Toast.makeText(moderateCommentsTab.this, getResources().getText(R.string.comment_moderated), Toast.LENGTH_SHORT).show();
				} 
			}; 
			this.runOnUiThread(action);
			Thread action2 = new Thread() 
			{ 
				public void run() 
				{
					pd = new ProgressDialog(moderateCommentsTab.this);  // to avoid crash
					refreshComments(false, true, false);				  } 
			}; 
			this.runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_DELETING);
			Thread action3 = new Thread() 
			{ 
				public void run() 
				{
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
					dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
					dialogBuilder.setMessage(e.getLocalizedMessage());
					dialogBuilder.setPositiveButton("OK",  new
							DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Just close the window.

						}
					});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()){
						dialogBuilder.create().show();
					}
				}
			}; 
			this.runOnUiThread(action3);
		}	
	}

	private void replyToComment(final String postID, final int commentID, final String comment) {
		//reply to individual comment
		Vector<Object> settings = new Vector<Object>();
		WordPressDB settingsDB = new WordPressDB(moderateCommentsTab.this);
		settings = settingsDB.loadSettings(moderateCommentsTab.this, id);

		String sURL = "";
		if (settings.get(0).toString().contains("xmlrpc.php"))
		{
			sURL = settings.get(0).toString();
		}
		else
		{
			sURL = settings.get(0).toString() + "xmlrpc.php";
		}
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
		int sBlogId = Integer.parseInt(settings.get(10).toString());

		client = new XMLRPCClient(sURL);

		HashMap<String, Object> replyHash = new HashMap<String, Object>();
		replyHash.put("comment_parent", commentID);
		replyHash.put("content", comment);
		replyHash.put("author", "");
		replyHash.put("author_url", "");
		replyHash.put("author_email", "");

		Object[] params = {
				sBlogId,
				sUsername,
				sPassword,
				Integer.valueOf(postID),
				replyHash
		};

		try {
			client.call("wp.newComment", params);
			dismissDialog(ID_DIALOG_REPLYING);
			Thread action = new Thread() 
			{ 
				public void run() 
				{
					Toast.makeText(moderateCommentsTab.this, getResources().getText(R.string.reply_added), Toast.LENGTH_SHORT).show();
				} 
			}; 
			this.runOnUiThread(action);
			Thread action2 = new Thread() 
			{ 
				public void run() 
				{
					pd = new ProgressDialog(moderateCommentsTab.this);  // to avoid crash
					refreshComments(false, true, false);				  } 
			}; 
			this.runOnUiThread(action2);

		} catch (final XMLRPCException e) {
			dismissDialog(ID_DIALOG_REPLYING);
			Thread action3 = new Thread() 
			{ 
				public void run() 
				{
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
					dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
					dialogBuilder.setMessage(e.getLocalizedMessage());
					dialogBuilder.setPositiveButton("OK",  new
							DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Just close the window.

						}
					});
					dialogBuilder.setCancelable(true);
					if (!isFinishing()){
						dialogBuilder.create().show();
					}
				}
			}; 
			this.runOnUiThread(action3);

		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null)
		{

			Bundle extras = data.getExtras();

			switch(requestCode) {
			case 0:
				final String returnText = extras.getString("replyText");

				if (!returnText.equals("CANCEL")){
					final String postID = extras.getString("postID");
					final int commentID = extras.getInt("commentID");
					showDialog(ID_DIALOG_REPLYING);

					new Thread(new Runnable(){
						public void run(){
							Looper.prepare();
							pd = new ProgressDialog(moderateCommentsTab.this);  // to avoid crash
							replyToComment(postID, commentID, returnText);
						}
					}).start();
				}


				break;
			case 1:
				if (resultCode == RESULT_OK){

					String comment_id;
					final String action;
					comment_id = extras.getString("comment_id");
					final int position = extras.getInt("position");

					action = extras.getString("action");
					if (action.equals("approve") || action.equals("hold") || action.equals("spam")){
						final int commentID = Integer.parseInt(comment_id);
						showDialog(ID_DIALOG_MODERATING);
						new Thread() {
							public void run() {
								Looper.prepare();
								changeCommentStatus(action, commentID, position);
							}
						}.start();
					}
					else if (action.equals("delete")){
						final int commentID_del = Integer.parseInt(comment_id);
						showDialog(ID_DIALOG_DELETING);
						new Thread() {
							public void run() {	    		
								deleteComment(commentID_del);
							}
						}.start();
					}
					else if (action.equals("reply")){

						Intent i = new Intent(this, replyToComment.class);
						i.putExtra("commentID", Integer.parseInt(comment_id));
						i.putExtra("accountName", accountName);
						i.putExtra("postID", extras.getString("post_id"));
						startActivityForResult(i, 0);
					}

				}
				break;
			}
		}
	}
}
