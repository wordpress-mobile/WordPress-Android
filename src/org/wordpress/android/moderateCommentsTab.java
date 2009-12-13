package org.wordpress.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.wordpress.android.moderateComments.XMLRPCMethodCallback;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class moderateCommentsTab extends ListActivity {
	private static final String FETCH_ACTION="com.commonsware.cwac.cache.demo.FETCH_ACTION";
	private static final int[] IMAGE_IDS={R.id.avatar};
	private SharedPreferences prefs=null;
	//private Twitter client=null;
	private ThumbnailAdapter thumbs=null;
	private ArrayList<CommentEntry> model=null;
	private XMLRPCClient client;
	private String id = "";
	private String postID = "";
	private String accountName = "";
	private String postTitle = "";
	public Object[] origComments;
	public int[] changedStatuses;
	public HashMap allComments = new HashMap();
	private HashMap changedComments = new HashMap();
	public int ID_DIALOG_POSTING = 1;
	public boolean initializing = true;
	public int selectedID = 0;
	public int rowID = 0;
	public ProgressDialog pd;
	
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
         fromNotification = extras.getBoolean("fromNotification", false);       		
        }      
        
        if (fromNotification) //dismiss the notification 
        {
        	NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        	nm.cancel(22 + Integer.valueOf(id));
        }
        
        this.setTitle(accountName + " - Moderate Comments");
        
        boolean loadedComments = loadComments();
        
        if (!loadedComments){
        	
        	Thread action = new Thread() 
    		{ 
    		  public void run() 
    		  {
    			  pd = ProgressDialog.show(moderateCommentsTab.this,
    		                "Refresh Comments", "Attempting to get comments", true, false);
    		  } 
    		}; 
    		runOnUiThread(action);
        	
        	refreshComments();
        }
        
        final customMenuButton refresh = (customMenuButton) findViewById(R.id.refreshComments);   
        
        refresh.setOnClickListener(new customMenuButton.OnClickListener() {
            public void onClick(View v) {
            	Thread action = new Thread() 
        		{ 
        		  public void run() 
        		  {
        			  pd = ProgressDialog.show(moderateCommentsTab.this,
        		                "Refresh Comments", "Attempting to get comments", true, false);
        		  } 
        		}; 
        		runOnUiThread(action);
            	refreshComments();
            	 
            }
    });
		

	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		loadComments();																			
	}
	
	private boolean loadComments() {
		postStoreDB postStoreDB = new postStoreDB(this);
	    Vector loadedPosts = postStoreDB.loadComments(moderateCommentsTab.this, id);
	 	if (loadedPosts != null){
	 	String author, commentID, comment, dateCreated, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
	 	model=new ArrayList<CommentEntry>();
						    for (int i=0; i < loadedPosts.size(); i++){
						        HashMap contentHash = (HashMap) loadedPosts.get(i);
						        allComments.put(contentHash.get("commentID").toString(), contentHash);
						        author = escapeUtils.unescapeHtml(contentHash.get("author").toString());
						        commentID = contentHash.get("commentID").toString();
						        comment = escapeUtils.unescapeHtml(contentHash.get("comment").toString());
						        dateCreated = contentHash.get("commentDate").toString();
						        dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
						        status = contentHash.get("status").toString();
						        authorEmail = escapeUtils.unescapeHtml(contentHash.get("email").toString());
						        authorURL = escapeUtils.unescapeHtml(contentHash.get("url").toString());
						        postTitle = escapeUtils.unescapeHtml(contentHash.get("postTitle").toString());
						        
						        //add to model
						        model.add(new CommentEntry(commentID, 
						        		author,
						        		dateCreatedFormatted,
						        		comment,
						        		status,
						        		postTitle,
						        		authorURL,
						        		authorEmail,
						        		URI.create("http://gravatar.com/avatar/" + getMd5Hash(authorEmail.trim()) + "?s=48&d=identicon")));
						    }
						   
						    try {
						    	ThumbnailBus bus = new ThumbnailBus();
								thumbs=new ThumbnailAdapter(this, new CommentAdapter(),new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus),IMAGE_IDS);
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
				        
							   setListAdapter(thumbs);				   
						
						   ListView listView = (ListView) findViewById(android.R.id.list);
						   listView.setSelector(R.layout.list_selector);

						   listView.setOnItemClickListener(new OnItemClickListener() {

								public void onNothingSelected(AdapterView<?> arg0) {
									
								}

								public void onItemClick(AdapterView<?> arg0, View arg1,
										int arg2, long arg3) {
									Intent intent = new Intent(moderateCommentsTab.this, viewComment.class);
				                    //intent.putExtra("pageID", pageIDs[(int) arg3]);
				                    //intent.putExtra("postTitle", titles[(int) arg3]);
				                    intent.putExtra("id", id);
				                    intent.putExtra("accountName", accountName);
				                    intent.putExtra("comment", model.get((int) arg3).comment);
				                    intent.putExtra("name", model.get((int) arg3).name);
				                    intent.putExtra("email", model.get((int) arg3).authorEmail);
				                    intent.putExtra("url", model.get((int) arg3).authorURL);
				                    startActivity(intent);
									
								}

				            });
						   
		        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

		               public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						// TODO Auto-generated method stub
		            	   AdapterView.AdapterContextMenuInfo info;
		                   try {
		                        info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		                   } catch (ClassCastException e) {
		                       //Log.e(TAG, "bad menuInfo", e);
		                       return;
		                   }
		                   
		                   selectedID = info.targetView.getId();
		                   rowID = info.position;
		                   
					 menu.setHeaderTitle("Comment Actions");
	                 menu.add(0, 0, 0, "Mark Approved");
	                 menu.add(0, 1, 0, "Mark Unapproved");
	                 menu.add(0, 2, 0, "Mark Spam");
					}
		          });
	 	
		return true;
	  }
	 	else{
	 		return false;
	 	}
		}


	private void refreshComments() {
		
		Vector settings = new Vector();
	    settingsDB settingsDB = new settingsDB(this);
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
		String sUsername = settings.get(2).toString();
		String sPassword = settings.get(3).toString();
	        
	        HashMap hPost = new HashMap();
	        hPost.put("status", "");
	        hPost.put("post_id", "");
	        hPost.put("number", 30);
	        
	    	
	    	List<Object> list = new ArrayList<Object>();
	    	
	    	//haxor
	    	
	    	client = new XMLRPCClient(sURL);
	    	
	    	XMLRPCMethod method = new XMLRPCMethod("wp.getComments", new XMLRPCMethodCallback() {
				public void callFinished(Object[] result) {
					String s = "done";
					if (pd.isShowing())
					{
					pd.dismiss();
					}
					if (result.length == 0){
						
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						  dialogBuilder.setTitle("No Comments Found");
			              dialogBuilder.setMessage("You don't have any comments on your blog");
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            // Just close the window.
	                        	
	                        }
	                    });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
					}
					else{
					s = result.toString();
					origComments = result;
					String author, commentID, comment, dateCreated, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
					
					HashMap contentHash = new HashMap();
					    
					    
					    Vector dbVector = new Vector();
						
						//loop this!
						    for (int ctr = 0; ctr < result.length; ctr++){
						    	HashMap<String, String> dbValues = new HashMap();
						        contentHash = (HashMap) result[ctr];
						        allComments.put(contentHash.get("comment_id").toString(), contentHash);
						        comment = contentHash.get("content").toString();
						        author = contentHash.get("author").toString();
						        status = contentHash.get("status").toString();
						        commentID = contentHash.get("comment_id").toString();
						        dateCreated = contentHash.get("date_created_gmt").toString();
						        authorURL = contentHash.get("author_url").toString();
						        authorEmail = contentHash.get("author_email").toString();
						        postTitle = contentHash.get("post_title").toString();
						        
						        //make the date pretty
						        Date d = new Date();
								SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy"); 
						        String cDate = dateCreated.replace("America/Los_Angeles", "PST");
						        try{  
						        	d = sdf.parse(cDate);
						        	SimpleDateFormat sdfOut = new SimpleDateFormat("MMMM dd, yyyy hh:mm a"); 
						        	dateCreatedFormatted = sdfOut.format(d);
						        } catch (ParseException pe){  
						            pe.printStackTrace();
						            dateCreatedFormatted = dateCreated;  //just make it the ugly date if it doesn't work
						        } 
						        
						        dbValues.put("blogID", id);
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
						    
						    postStoreDB postStoreDB = new postStoreDB(moderateCommentsTab.this);
						    postStoreDB.saveComments(moderateCommentsTab.this, dbVector);
						   
						   loadComments();
					    
					}  
		        
					
					 

				}
	        });
	        Object[] params = {
	        		1,
	        		sUsername,
	        		sPassword,
	        		hPost
	        };
	        
	        
	        method.call(params);
			
		}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		thumbs.close();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return(model);
	}
	
	/*private ThumbnailBus getBus() {
		return(((Application)getApplication()).getBus());		
	}*/
	
	private void goBlooey(Throwable t) {
		Log.e("WordPress", "Exception!", t);
		
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		
		builder
			.setTitle("UHOH")
			.setMessage(t.toString())
			.setPositiveButton("OK", null)
			.show();
	}
	
	class CommentEntry {
		String commentID="";
		String name="";
		String emailURL="";
		String status="";
		String comment="";
		String postTitle="";
		String authorURL="";
		String authorEmail="";
		URI profileImageUrl=null;
		
		CommentEntry(String commentID, String name, String emailURL,
									String comment, String status, String postTitle, String authorURL, String authorEmail, URI profileImageUrl) {
			this.commentID=commentID;
			this.name=name;
			this.emailURL=authorEmail;
			this.status=status;
			this.comment=comment;
			this.postTitle=postTitle;
			this.authorURL=authorURL;
			this.authorEmail=authorEmail;
			this.profileImageUrl=profileImageUrl;
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
			
			wrapper.populateFrom(getItem(position));
			
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
		
		CommentEntryWrapper(View row) {
			this.row=row;
			
		}
		
		void populateFrom(CommentEntry s) {
			getName().setText(s.name);
			
			String fEmailURL = s.authorURL;
			// use the required email address if the commenter didn't leave a url
			if (fEmailURL == ""){
				fEmailURL = s.emailURL;
			}
			
			getEmailURL().setText(fEmailURL);
			getComment().setText(s.comment);
			getPostTitle().setText("on " + s.postTitle);
			
			row.setId(Integer.valueOf(s.commentID));
			
			String prettyComment,textColor = "";
			
			if (s.status.equals("spam")){
				prettyComment = "Spam";
				textColor = "#FF0000";
			}
			else if (s.status.equals("hold")){
				prettyComment = "Unapproved";
				textColor = "#D54E21";
			}
			else{
				prettyComment = "Approved";
				textColor = "#006505";
			}
			
			getStatus().setText(prettyComment);
			getStatus().setTextColor(Color.parseColor(textColor));
			
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
	            Log.e("MD5", e.getMessage());
	            return null;
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
				final long t0 = System.currentTimeMillis();
				final Object[] result = (Object[]) client.call(method, params);
				final long t1 = System.currentTimeMillis();
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
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                          public void onClick(DialogInterface dialog, int whichButton) {
	                              // Just close the window.
	                      
	                          }
	                      });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();

					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {
						if (pd.isShowing())
						{
						pd.dismiss();
						}
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getMessage());
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            // Just close the window.
	                    
	                        }
	                    });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
						
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
				final long t0 = System.currentTimeMillis();
				final Object result = (Object) client.call(method, params);
				final long t1 = System.currentTimeMillis();
				handler.post(new Runnable() {
					public void run() {
						
						callBack.callFinished(result);
						
					}
				});
			} catch (final XMLRPCFault e) {
				handler.post(new Runnable() {
					public void run() {
						dismissDialog(ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getFaultString());
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                          public void onClick(DialogInterface dialog, int whichButton) {
	                              // Just close the window.
	                      
	                          }
	                      });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();

					}
				});
			} catch (final XMLRPCException e) {
				handler.post(new Runnable() {
					public void run() {

						Throwable couse = e.getCause();
						dismissDialog(ID_DIALOG_POSTING);
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateCommentsTab.this);
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getMessage());
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            // Just close the window.
	                    
	                        }
	                    });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
						
						//Log.d("Test", "error", e);
					}
				});
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
	if(id == ID_DIALOG_POSTING){
	ProgressDialog loadingDialog = new ProgressDialog(this);
	loadingDialog.setMessage("Moderating Comment...");
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
	        	showDialog(ID_DIALOG_POSTING);
	        	  new Thread() {
	                  public void run() {
	                	  Looper.prepare();
				changeCommentStatus("approve", selectedID);
	                  }
	              }.start();
	        	  return true;
	          case 1:
	        	  showDialog(ID_DIALOG_POSTING);
	        	  new Thread() {
	                  public void run() { 
	                	  Looper.prepare();
	        	  changeCommentStatus("hold", selectedID);
	                  }
	              }.start();
	              
	        	  return true;
	          case 2:
	        	  showDialog(ID_DIALOG_POSTING);
	        	  new Thread() {
	                  public void run() { 
	                	  Looper.prepare();
	        	  changeCommentStatus("spam", selectedID);
	                  }
	              }.start();
	             return true;
	        	  
	     }
	     return false;
	}

	private void changeCommentStatus(final String newStatus, final int selCommentID) {

	        	//Thread t = new Thread() {
					//public void run() {
					//Looper.prepare();
	    		String sSelCommentID = String.valueOf(selCommentID);
	        	Vector settings = new Vector();
	            settingsDB settingsDB = new settingsDB(moderateCommentsTab.this);
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
	        	
	        	client = new XMLRPCClient(sURL);
	        	
	        	ListView lv = getListView(); 
	        	
	        	Object curListItem;
	        	ListAdapter la = lv.getAdapter();
	        	
	            
	        		HashMap contentHash, postHash = new HashMap();
	        		contentHash = (HashMap) allComments.get(sSelCommentID);
			        postHash.put("status", newStatus);
			        Date d = new Date();
			        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");  
			        String cDate = contentHash.get("commentDate").toString().replace("America/Los_Angeles", "PST");
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
		        		1,
		        		sUsername,
		        		sPassword,
		        		sSelCommentID,
		        		postHash
		        };
		        
		        Object result = null;
		        try {
		    		result = (Object) client.call("wp.editComment", params);
		    		dismissDialog(ID_DIALOG_POSTING);
		    		Thread action = new Thread() 
					{ 
					  public void run() 
					  {
						  Toast.makeText(moderateCommentsTab.this, "Comment Moderated Succesfully", Toast.LENGTH_SHORT).show();
					  } 
					}; 
					this.runOnUiThread(action);
					Thread action2 = new Thread() 
					{ 
					  public void run() 
					  {
						  pd = new ProgressDialog(moderateCommentsTab.this);  // to avoid crash
						  refreshComments();				  } 
					}; 
					this.runOnUiThread(action2);
					
		    	} catch (XMLRPCException e) {
		    		dismissDialog(ID_DIALOG_POSTING);
		    		e.printStackTrace();
		    	}
	        
	        //}
	        
	        	
	        	
			//};
			//t.start();	
			
			
			
	 
		
	}
}
