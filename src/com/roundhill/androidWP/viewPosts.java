	//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class viewPosts extends ListActivity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	private String[] postIDs;
	private String[] titles;
	private String[] dateCreated;
	private String[] draftIDs;
	private String[] draftTitles;
	private String[] publish;
	private Integer[] uploaded;
	private String id = "";
	private String accountName = "";
	Vector postNames = new Vector();
	int selectedID = 0;
	int rowID = 0;
	private int ID_DIALOG_REFRESHING = 1;
	private boolean inDrafts = false;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.viewposts);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }
        
        this.setTitle(escapeUtils.unescapeHtml(accountName) + " - Recent Posts");
        //query for posts and refresh view


        boolean loadedPosts = loadPosts();
        
        if (!loadedPosts){
        	refreshPosts();
        }
        

            final customImageButton addNewPost = (customImageButton) findViewById(R.id.newPost);   
            
            addNewPost.setOnClickListener(new customImageButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent i = new Intent(viewPosts.this, newPost.class);
                	i.putExtra("accountName", accountName);
 	                i.putExtra("id", id);
 	                startActivityForResult(i, 0);
                	 
                }
        });
            
final customImageButton refresh = (customImageButton) findViewById(R.id.refresh);   
            
            refresh.setOnClickListener(new customImageButton.OnClickListener() {
                public void onClick(View v) {
                	
                	refreshPosts();
                	 
                }
        });
        
        
    }
    
    private void refreshPosts(){
    	showDialog(ID_DIALOG_REFRESHING);
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
        
        	
        	client = new XMLRPCClient(sURL);
        	
        	XMLRPCMethod method = new XMLRPCMethod("blogger.getRecentPosts", new XMLRPCMethodCallback() {
				public void callFinished(Object[] result) {
					String s = "done";
					s = result.toString();

					HashMap contentHash = new HashMap();
					    
					titles = new String[result.length];
					postIDs = new String[result.length];
					dateCreated = new String[result.length];
					Vector dbVector = new Vector();
					
					//loop this!
					    for (int ctr = 0; ctr < result.length; ctr++){
					    	HashMap<String, String> dbValues = new HashMap();
					        contentHash = (HashMap) result[ctr];
					        titles[ctr] = escapeUtils.unescapeHtml(contentHash.get("content").toString().substring(contentHash.get("content").toString().indexOf("<title>") + 7, contentHash.get("content").toString().indexOf("</title>")));
					        postIDs[ctr] = contentHash.get("postid").toString();
					        dateCreated[ctr] = contentHash.get("dateCreated").toString();					        
					        dbValues.put("blogID", id);
					        dbValues.put("postID", postIDs[ctr]);
					        dbValues.put("title", titles[ctr]);
					        dbValues.put("postDate", dateCreated[ctr]);
					        dbVector.add(ctr, dbValues);
					    }
					    
					    postStoreDB postStoreDB = new postStoreDB(viewPosts.this);
					    postStoreDB.savePosts(viewPosts.this, dbVector);
					    
					    
			        
					   setListAdapter(new CommentListAdapter(viewPosts.this));
					   dismissDialog(viewPosts.this.ID_DIALOG_REFRESHING);
			        
				}
	        });
	        Object[] params = {
	        		"spacer",
	        		1,
	        		sUsername,
	        		sPassword,
	        		30
	        };
	        
	        
	        method.call(params);
	        
	        ListView listView = (ListView) findViewById(android.R.id.list);
	        
	        listView.setOnLongClickListener(new OnLongClickListener(){
				public boolean onLongClick(View v)  {
					// TODO Auto-generated method stub
					
					return false;
				}
		           
	        });
	        
	        
	        listView.setOnItemClickListener(new OnItemClickListener() {

				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}

				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					Intent intent = new Intent(viewPosts.this, editPost.class);
                    intent.putExtra("postID", postIDs[(int) arg3]);
                    intent.putExtra("postTitle", titles[(int) arg3]);
                    intent.putExtra("id", id);
                    intent.putExtra("accountName", accountName);
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
	                   
	             selectedID = info.position;
	            	   menu.add(0, 0, 0, "Preview Post");
	                   menu.add(0, 1, 0, "View Comments");
	                   menu.add(0, 2, 0, "Edit Post");
				}
	          }); 
    }
    
    private boolean loadPosts(){ //loads posts from the db
   	
       postStoreDB postStoreDB = new postStoreDB(this);
   	Vector loadedPosts = postStoreDB.loadPosts(viewPosts.this, id);
   	if (loadedPosts != null){
   	titles = new String[loadedPosts.size()];
   	postIDs = new String[loadedPosts.size()];
   	dateCreated = new String[loadedPosts.size()];
					    for (int i=0; i < loadedPosts.size(); i++){
					        HashMap contentHash = (HashMap) loadedPosts.get(i);
					        titles[i] = escapeUtils.unescapeHtml(contentHash.get("title").toString());
					        postIDs[i] = contentHash.get("postID").toString();
					        dateCreated[i] = contentHash.get("postDate").toString();					        
					    }
					    
					    //add the header
					    List postIDList = Arrays.asList(postIDs);  
				    	List newPostIDList = new ArrayList();   
				    	newPostIDList.add("postsHeader");
				    	newPostIDList.addAll(postIDList);
				    	postIDs = (String[]) newPostIDList.toArray(new String[newPostIDList.size()]);
				    	
				    	List postTitleList = Arrays.asList(titles);  
				    	List newPostTitleList = new ArrayList();   
				    	newPostTitleList.add("Posts");
				    	newPostTitleList.addAll(postTitleList);
				    	titles = (String[]) newPostTitleList.toArray(new String[newPostTitleList.size()]);
				    	
				    	List dateList = Arrays.asList(dateCreated);  
				    	List newDateList = new ArrayList();   
				    	newDateList.add("postsHeader");
				    	newDateList.addAll(dateList);
				    	dateCreated = (String[]) newDateList.toArray(new String[newDateList.size()]);
					    
					    
					    //load drafts
					    boolean drafts = loadDrafts();
					    
					    if (drafts){
					    	
					    	List draftIDList = Arrays.asList(draftIDs);  
					    	List newDraftIDList = new ArrayList();   
					    	newDraftIDList.add("draftsHeader");
					    	newDraftIDList.addAll(draftIDList);
					    	draftIDs = (String[]) newDraftIDList.toArray(new String[newDraftIDList.size()]);
					    	
					    	List titleList = Arrays.asList(draftTitles);  
					    	List newTitleList = new ArrayList();   
					    	newTitleList.add("Local Drafts");
					    	newTitleList.addAll(titleList);
					    	draftTitles = (String[]) newTitleList.toArray(new String[newTitleList.size()]);
					    	
					    	List publishList = Arrays.asList(publish);  
					    	List newPublishList = new ArrayList();   
					    	newPublishList.add("draftsHeader");
					    	newPublishList.addAll(publishList);
					    	publish = (String[]) newPublishList.toArray(new String[newPublishList.size()]);
					    	
					    	postIDs = mergeStringArrays(draftIDs, postIDs);
					    	titles = mergeStringArrays(draftTitles, titles);
					    	dateCreated = mergeStringArrays(publish, dateCreated);
					    }
					   
					   setListAdapter(new CommentListAdapter(viewPosts.this));
					
					   ListView listView = (ListView) findViewById(android.R.id.list);
					   listView.setSelector(R.layout.list_selector);
   
   	
	return true;
    }
   	else{
   		return false;
   	}
   }
    
    
    private boolean loadDrafts(){ //loads posts from the db
       	
        localDraftsDB lDraftsDB = new localDraftsDB(this);
    	Vector loadedPosts = lDraftsDB.loadPosts(viewPosts.this, id);
    	if (loadedPosts != null){
    	draftIDs = new String[loadedPosts.size()];
    	draftTitles = new String[loadedPosts.size()];
    	publish = new String[loadedPosts.size()];
    	uploaded = new Integer[loadedPosts.size()];
    	
 					    for (int i=0; i < loadedPosts.size(); i++){
 					        HashMap contentHash = (HashMap) loadedPosts.get(i);
 					        draftIDs[i] = contentHash.get("id").toString();
 					        draftTitles[i] = escapeUtils.unescapeHtml(contentHash.get("title").toString());
 					        publish[i] = contentHash.get("publish").toString();
 					        uploaded[i] = (Integer) contentHash.get("uploaded");
 					    }
 					   
 			        /*
 					   setListAdapter(new CommentListAdapter(viewPosts.this));
 					
 					   ListView listView = (ListView) findViewById(android.R.id.list);
 					   listView.setSelector(R.layout.list_selector);
 					   
 					   listView.setOnItemClickListener(new OnItemClickListener() {

 							public void onNothingSelected(AdapterView<?> arg0) {
 								
 							}

 							public void onItemClick(AdapterView<?> arg0, View arg1,
 									int arg2, long arg3) {
 								Intent intent = new Intent(viewPosts.this, editPost.class);
 			                    intent.putExtra("postID", postIDs[(int) arg3]);
 			                    intent.putExtra("postTitle", titles[(int) arg3]);
 			                    intent.putExtra("id", id);
 			                    intent.putExtra("accountName", accountName);
 			                    intent.putExtra("localDraft", true);
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
 	                   
 				 menu.setHeaderTitle("Post Actions");
                  menu.add(0, 0, 0, "Edit Draft");
                  menu.add(0, 1, 0, "Upload Draft to Blog");
                  menu.add(0, 2, 0, "Delete Draft");
 				}
 	          });*/
    	
 	return true;
     }
    	else{
    		return false;
    	}
    }

    private class CommentListAdapter extends BaseAdapter {
    	
        public CommentListAdapter(Context context) {
            mContext = context;
        }

        /**
         * The number of items in the list is determined by the number of speeches
         * in our array.
         * 
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return postIDs.length;
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         * 
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         * 
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a SpeechView to hold each row.
         * 
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
        	
            CommentView cv;
                cv = new CommentView(mContext, postIDs[position], titles[position],
                        dateCreated[position]);

            return cv;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        private Context mContext;
        
        /**
         * Our data, part 1.
         */
    }

    private class CommentView extends LinearLayout {
        public CommentView(Context context, String postID, String title, String date) {
            super(context);

            this.setOrientation(VERTICAL);
            this.setPadding(4, 4, 4, 4);
            
            if (date.equals("postsHeader") || date.equals("draftsHeader")){
            	
            	
            	this.setPadding(8, 1, 1, 1);
            	this.setBackgroundColor(Color.parseColor("#999999"));
            	
            	tvTitle = new TextView(context);
                tvTitle.setText(title);
                tvTitle.setTextSize(23);
                tvTitle.setShadowLayer(1, 1, 1, Color.parseColor("#444444"));
                tvTitle.setTextColor(Color.parseColor("#EEEEEE"));
                addView(tvTitle, new LinearLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                
                tvDate = new TextView(context);
                
                if (date.equals("draftsHeader")){
                	inDrafts = true;
                }
                else if (date.equals("postsHeader")){
                	inDrafts = false;
                }
            }
            else{
            // Here we build the child views in code. They could also have
            // been specified in an XML file.
            	
            	

            tvTitle = new TextView(context);
            tvTitle.setText(title);
            tvTitle.setTextSize(20);
            tvTitle.setTextColor(Color.parseColor("#444444"));
            addView(tvTitle, new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

            tvDate = new TextView(context);
            
            String customDate = date;
            tvDate.setText(customDate);
            addView(tvDate, new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            
            if (inDrafts){  
            	//listener for drafts
            	this.setId(Integer.valueOf(postID));
        		this.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

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
  	                   
  	                   
  	                   
  	                   selectedID = v.getId();
  	                   
  	                   //rowID = (int) info.id;
  	                   //rowID = info.position;
  	                   
  				 menu.setHeaderTitle("Draft Actions");
                   menu.add(1, 0, 0, "Edit Draft");
                   menu.add(1, 1, 0, "Upload Draft to Blog");
                   menu.add(1, 2, 0, "Delete Draft");
  				}
  	          });
        	}
            else{
            //selection listeners for posts
            this.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

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
	                   
				 menu.setHeaderTitle("Post Actions");
				 menu.add(0, 0, 0, "Preview Post");
               menu.add(0, 1, 0, "View Comments");
               menu.add(0, 2, 0, "Edit Post");
				}
	          });
            
            }
            
            }
        }

        /**
         * Convenience method to set the title of a SpeechView
         */
        public void setTitle(String titleName) {
            tvTitle.setText(titleName);
        }

        /**
         * Convenience method to set the dialogue of a SpeechView
         */
        public void setDate(String date) {
            tvDate.setText(date);
        }

        private TextView tvTitle;
        private TextView tvDate;
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
					dismissDialog(viewPosts.this.ID_DIALOG_REFRESHING);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
					  dialogBuilder.setTitle("Connection Error");
		              dialogBuilder.setMessage(e.getFaultString());
		              dialogBuilder.setPositiveButton("Ok",  new
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
					dismissDialog(viewPosts.this.ID_DIALOG_REFRESHING);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
					  dialogBuilder.setTitle("Connection Error");
		              dialogBuilder.setMessage(e.getMessage());
		              dialogBuilder.setPositiveButton("Ok",  new
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
//Add settings to menu
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, 0, 0, "Blog Settings");
    MenuItem menuItem1 = menu.findItem(0);
    menuItem1.setIcon(R.drawable.ic_menu_preferences);
    menu.add(0, 1, 0, "Delete Blog");
    MenuItem menuItem2 = menu.findItem(1);
    menuItem2.setIcon(R.drawable.ic_notification_clear_all);
    
    return true;
}
//Menu actions
@Override
public boolean onOptionsItemSelected(final MenuItem item){
    switch (item.getItemId()) {
    case 0:
    	
    	Bundle bundle = new Bundle();
		bundle.putString("id", id);
		bundle.putString("accountName", accountName);
    	Intent i = new Intent(this, settings.class);
    	i.putExtras(bundle);
    	startActivityForResult(i, 0);
    	
    	return true;
	case 1:
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
		  dialogBuilder.setTitle("Delete Account");
      dialogBuilder.setMessage("Are you sure you want to delete this blog from wpToGo?");
      dialogBuilder.setPositiveButton("Yes",  new
    		  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // User clicked Accept so set that they've agreed to the eula.
            	settingsDB settingsDB = new settingsDB(viewPosts.this);
              boolean deleteSuccess = settingsDB.deleteAccount(viewPosts.this, id);
              if (deleteSuccess)
              {
            	  Toast.makeText(viewPosts.this, "Blog deleted successfully",
                          Toast.LENGTH_SHORT).show();
            	  finish();
              }
              else
              {
            	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
      			  dialogBuilder.setTitle("Error");
                  dialogBuilder.setMessage("Could not delete blog, you may need to reinstall wpToGo.");
                  dialogBuilder.setPositiveButton("OK",  new
                		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // just close the dialog
                        	
                    
                        }
                    });
                  dialogBuilder.setCancelable(true);
                 dialogBuilder.create().show();
              }
        
            }
        });
      dialogBuilder.setNegativeButton("No", new
    		  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();  //goodbye!
            }
        });
      dialogBuilder.setCancelable(false);
     dialogBuilder.create().show();
	
	
	return true;
}
  return false;	
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	// TODO Auto-generated method stub
	super.onActivityResult(requestCode, resultCode, data);

	switch(requestCode) {
	case 0:
	    //displayAccounts();
	    //Toast.makeText(wpAndroid.this, title, Toast.LENGTH_SHORT).show();
		//refreshPosts();
	    break;
	}

}

@Override
public boolean onContextItemSelected(MenuItem item) {

	
	
     /* Switch on the ID of the item, to get what the user selected. */
	if (item.getGroupId() == 0){
     switch (item.getItemId()) {
     	  case 0:
     		 Intent i0 = new Intent(viewPosts.this, viewPost.class);
             i0.putExtra("postID", postIDs[selectedID]);
             i0.putExtra("postTitle", titles[selectedID]);
             i0.putExtra("id", id);
             i0.putExtra("accountName", accountName);
             startActivity(i0);
             return true;
          case 1:     
        	  Intent i = new Intent(viewPosts.this, viewComments.class);
              i.putExtra("postID", postIDs[selectedID]);
              i.putExtra("postTitle", titles[selectedID]);
              i.putExtra("id", id);
              i.putExtra("accountName", accountName);
              startActivity(i);
              return true; 
          case 2:
        	  Intent i2 = new Intent(viewPosts.this, editPost.class);
              i2.putExtra("postID", postIDs[selectedID]);
              i2.putExtra("postTitle", titles[selectedID]);
              i2.putExtra("id", id);
              i2.putExtra("accountName", accountName);
              startActivity(i2);
        	  return true;
     }
     
	}
	else{
		switch (item.getItemId()) {
        case 0:
      	  Intent i2 = new Intent(viewPosts.this, editPost.class);
            i2.putExtra("postID", String.valueOf(selectedID));
            //i2.putExtra("postTitle", titles[rowID]);
            i2.putExtra("id", id);
            i2.putExtra("accountName", accountName);
            i2.putExtra("localDraft", true);
            startActivity(i2);
      	  return true;
        case 1:
      	 /* showDialog(ID_DIALOG_POSTING);
      	  
      	  
      	  new Thread() {
                public void run() { 	  
      	  
		try {
			submitResult = submitPost();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
                }
            }.start(); 
            	
            */
            
            
      	  return true;
        case 2:
      	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPosts.this);
			  dialogBuilder.setTitle("Delete Post?");
            dialogBuilder.setMessage("Are you sure you want to delete the draft '" + titles[rowID] + "'?");
            dialogBuilder.setPositiveButton("OK",  new
          		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
              	  localDraftsDB lDraftsDB = new localDraftsDB(viewPosts.this);
              	  
              	  lDraftsDB.deletePost(viewPosts.this, String.valueOf(selectedID));
              	  loadPosts();
            
                }
            });
            dialogBuilder.setNegativeButton("Cancel",  new
          		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Just close the window.
            
                }
            });
            dialogBuilder.setCancelable(true);
           dialogBuilder.create().show();
      	  
		}
	}
	
	
	return false;
}

@Override
protected Dialog onCreateDialog(int id) {
if(id == ID_DIALOG_REFRESHING){
ProgressDialog loadingDialog = new ProgressDialog(this);
loadingDialog.setMessage("Please wait while refreshing posts...");
loadingDialog.setIndeterminate(true);
loadingDialog.setCancelable(true);
return loadingDialog;
}

return super.onCreateDialog(id);
}

public static String[] mergeStringArrays(String array1[], String array2[]) {  
	if (array1 == null || array1.length == 0)  
	return array2;  
	if (array2 == null || array2.length == 0)  
	return array1;  
	List array1List = Arrays.asList(array1);  
	List array2List = Arrays.asList(array2);  
	List result = new ArrayList(array1List);    
	List tmp = new ArrayList(array1List);  
	tmp.retainAll(array2List);  
	result.removeAll(tmp);  
	result.addAll(array2List);    
	return ((String[]) result.toArray(new String[result.size()]));  
	}

}


