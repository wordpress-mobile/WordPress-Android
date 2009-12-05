//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.HashMap;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class viewPages extends ListActivity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	private String[] pageIDs;
	private String[] titles;
	private String[] dateCreated;
	private String[] parentIDs;
	private String id = "";
	private String accountName = "";
	Vector postNames = new Vector();
	int selectedID = 0;
	private int ID_DIALOG_REFRESHING = 1;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        setContentView(R.layout.viewposts);
        
        TextView longPress = (TextView) findViewById(R.id.longPress);
        longPress.setText("Long press on a page to view actions");
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }
        
        this.setTitle(escapeUtils.unescapeHtml(accountName) + " - View Pages");
        //query for posts and refresh view


        boolean loadedPages = loadPages();
        
        if (!loadedPages){
        	refreshPages();
        }
        

            final customMenuButton addNewPost = (customMenuButton) findViewById(R.id.newPost);   
            
            addNewPost.setOnClickListener(new customMenuButton.OnClickListener() {
                public void onClick(View v) {
                	
                	Intent i = new Intent(viewPages.this, newPost.class);
                	i.putExtra("accountName", accountName);
 	                i.putExtra("id", id);
 	                startActivityForResult(i, 0);
                	 
                }
        });
            
final customMenuButton refresh = (customMenuButton) findViewById(R.id.refresh);   
            
            refresh.setOnClickListener(new customMenuButton.OnClickListener() {
                public void onClick(View v) {
                	
                	refreshPages();
                	 
                }
        });
        
        
    }
    
    private void refreshPages(){
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
        	
        	XMLRPCMethod method = new XMLRPCMethod("wp.getPageList", new XMLRPCMethodCallback() {
				public void callFinished(Object[] result) {
					String s = "done";
					s = result.toString();

					HashMap contentHash = new HashMap();
					    
					titles = new String[result.length];
					pageIDs = new String[result.length];
					dateCreated = new String[result.length];
					parentIDs = new String[result.length];
					Vector dbVector = new Vector();
					
					//loop this!
					    for (int ctr = 0; ctr < result.length; ctr++){
					    	HashMap<String, String> dbValues = new HashMap();
					        contentHash = (HashMap) result[ctr];
					        titles[ctr] = escapeUtils.unescapeHtml(contentHash.get("page_title").toString());
					        pageIDs[ctr] = contentHash.get("page_id").toString();
					        dateCreated[ctr] = contentHash.get("dateCreated").toString();	
					        parentIDs[ctr] = contentHash.get("page_parent_id").toString();	
					        dbValues.put("blogID", id);
					        dbValues.put("pageID", pageIDs[ctr]);
					        dbValues.put("parentID", parentIDs[ctr]);
					        dbValues.put("title", titles[ctr]);
					        dbValues.put("pageDate", dateCreated[ctr]);
					        dbVector.add(ctr, dbValues);
					    }
					    
					    postStoreDB postStoreDB = new postStoreDB(viewPages.this);
					    postStoreDB.savePages(viewPages.this, dbVector);
					   
			        
					   setListAdapter(new CommentListAdapter(viewPages.this));
					   dismissDialog(viewPages.this.ID_DIALOG_REFRESHING);
			        
				}
	        });
	        Object[] params = {
	        		1,
	        		sUsername,
	        		sPassword
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
					Intent intent = new Intent(viewPages.this, editPost.class);
                    intent.putExtra("postID", pageIDs[(int) arg3]);
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
    
    private boolean loadPages(){ //loads posts from the db
   	
       postStoreDB postStoreDB = new postStoreDB(this);
      Vector loadedPosts = postStoreDB.loadPages(viewPages.this, id);
   	if (loadedPosts != null){
   	titles = new String[loadedPosts.size()];
   	pageIDs = new String[loadedPosts.size()];
   	dateCreated = new String[loadedPosts.size()];
					    for (int i=0; i < loadedPosts.size(); i++){
					        HashMap contentHash = (HashMap) loadedPosts.get(i);
					        titles[i] = escapeUtils.unescapeHtml(contentHash.get("title").toString());
					        pageIDs[i] = contentHash.get("pageID").toString();
					        dateCreated[i] = contentHash.get("pageDate").toString();					        
					    }
					   
			        
					   setListAdapter(new CommentListAdapter(viewPages.this));
					
					   ListView listView = (ListView) findViewById(android.R.id.list);
					   listView.setSelector(R.layout.list_selector);
					   
					   listView.setOnItemClickListener(new OnItemClickListener() {

							public void onNothingSelected(AdapterView<?> arg0) {
								
							}

							public void onItemClick(AdapterView<?> arg0, View arg1,
									int arg2, long arg3) {
								Intent intent = new Intent(viewPages.this, editPost.class);
			                    intent.putExtra("pageID", pageIDs[(int) arg3]);
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
	                   
				 menu.setHeaderTitle("Post Actions");
				 menu.add(0, 0, 0, "Preview Page");
                 menu.add(0, 1, 0, "View Comments");
                 menu.add(0, 2, 0, "Edit Page");
				}
	          });
   	
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
            return pageIDs.length;
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
            if (convertView == null) {
                cv = new CommentView(mContext, titles[position],
                        dateCreated[position]);
            } else {
                cv = (CommentView) convertView;
                cv.setTitle(titles[position]);
                cv.setDate(dateCreated[position]);
            }

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
        public CommentView(Context context, String title, String date) {
            super(context);

            this.setOrientation(VERTICAL);
            this.setPadding(4, 4, 4, 4);

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

public String parseDate(String date){
	int hour = Integer.parseInt(date.substring(11, 13));
    
    String amPM = "AM";
    if (hour >= 12){
    	if (hour > 12){
    		hour = hour - 12;
    	}
    	amPM = "PM";
    }
    else if (hour == 0){
    	hour = 24;
    	amPM = "AM";
    }
    
    String monthName = date.substring(4, 7);
    
    if (monthName.equals("Jan")){
    	monthName = "January";
    }
    else if (monthName.equals("Feb")){
    	monthName = "February";
    }
    else if (monthName.equals("Mar")){
    	monthName = "March";
    }
    else if (monthName.equals("Apr")){
    	monthName = "April";
    }
    else if (monthName.equals("May")){
    	monthName = "May";
    }
    else if (monthName.equals("Jun")){
    	monthName = "June";
    }
    else if (monthName.equals("Jul")){
    	monthName = "July";
    }
    else if (monthName.equals("Aug")){
    	monthName = "August";
    }
    else if (monthName.equals("Sep")){
    	monthName = "September";
    }
    else if (monthName.equals("Oct")){
    	monthName = "October";
    }
    else if (monthName.equals("Nov")){
    	monthName = "November";
    }
    else if (monthName.equals("Dec")){
    	monthName = "December";
    }
    
    String customDate =  monthName + " " + date.substring(8, 10) + ", " + date.substring(24) + " " + hour + ":" + date.substring(14, 16) + " " + amPM;
    return customDate;
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
					dismissDialog(viewPages.this.ID_DIALOG_REFRESHING);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPages.this);
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
					dismissDialog(viewPages.this.ID_DIALOG_REFRESHING);
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPages.this);
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
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPages.this);
		  dialogBuilder.setTitle("Delete Account");
      dialogBuilder.setMessage("Are you sure you want to delete this blog from wpToGo?");
      dialogBuilder.setPositiveButton("Yes",  new
    		  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // User clicked Accept so set that they've agreed to the eula.
            	settingsDB settingsDB = new settingsDB(viewPages.this);
              boolean deleteSuccess = settingsDB.deleteAccount(viewPages.this, id);
              if (deleteSuccess)
              {
            	  Toast.makeText(viewPages.this, "Blog deleted successfully",
                          Toast.LENGTH_SHORT).show();
            	  finish();
              }
              else
              {
            	  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPages.this);
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
     switch (item.getItemId()) {
     	  case 0:
     		 Intent i0 = new Intent(viewPages.this, viewPost.class);
             i0.putExtra("postID", pageIDs[selectedID]);
             i0.putExtra("postTitle", titles[selectedID]);
             i0.putExtra("id", id);
             i0.putExtra("accountName", accountName);
             startActivity(i0);
             return true;
          case 1:     
        	  Intent i = new Intent(viewPages.this, viewComments.class);
              i.putExtra("postID", pageIDs[selectedID]);
              i.putExtra("postTitle", titles[selectedID]);
              i.putExtra("id", id);
              i.putExtra("accountName", accountName);
              startActivity(i);
              return true; 
          case 2:
        	  Intent i2 = new Intent(viewPages.this, editPost.class);
              i2.putExtra("postID", pageIDs[selectedID]);
              i2.putExtra("postTitle", titles[selectedID]);
              i2.putExtra("id", id);
              i2.putExtra("accountName", accountName);
              startActivity(i2);
        	  return true;
     }
     return false;
}

@Override
protected Dialog onCreateDialog(int id) {
if(id == ID_DIALOG_REFRESHING){
ProgressDialog loadingDialog = new ProgressDialog(this);
loadingDialog.setMessage("Please wait while refreshing pages...");
loadingDialog.setIndeterminate(true);
loadingDialog.setCancelable(true);
return loadingDialog;
}

return super.onCreateDialog(id);
}

}


