//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class moderateComments extends ListActivity{
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	public String[] authors;
	public String[] comments;
	public String[] status;
	public String[] commentID;
	public String[] authorURL;
	public String[] authorEmail;
	public String[] dateCreated;
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
        
        refreshComments();
        
        final customImageButton refresh = (customImageButton) findViewById(R.id.refreshComments);   
        
        refresh.setOnClickListener(new customImageButton.OnClickListener() {
            public void onClick(View v) {
            	
            	refreshComments();
            	 
            }
    });
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
				
				if (result.length == 0){
					
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateComments.this);
					  dialogBuilder.setTitle("No Comments Found");
		              dialogBuilder.setMessage("You don't have any comments on your blog");
		              dialogBuilder.setPositiveButton("OK",  new
		            		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Just close the window.
                        	finish();
                        }
                    });
		              dialogBuilder.setCancelable(true);
		             dialogBuilder.create().show();
				}
				else{
				s = result.toString();
				origComments = result;
				comments = new String[result.length];
				authors = new String[result.length];
				status = new String[result.length];
				commentID = new String[result.length];
				authorEmail = new String[result.length];
				dateCreated = new String[result.length];
				authorURL = new String[result.length];
				
				HashMap contentHash = new HashMap();
				    
				int ctr = 0;
				
				//loop this!
				    for (Object item : result){
				        contentHash = (HashMap) result[ctr];
				        allComments.put(contentHash.get("comment_id").toString(), contentHash);
				        comments[ctr] = contentHash.get("content").toString();
				        authors[ctr] = contentHash.get("author").toString();
				        status[ctr] = contentHash.get("status").toString();
				        commentID[ctr] = contentHash.get("comment_id").toString();
				        ctr++;
				    }
				    
				    setListAdapter(new CommentListAdapter(moderateComments.this));
				    
				    ListView listView = (ListView) findViewById(android.R.id.list);
					   listView.setSelector(R.layout.list_selector);
					   
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
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateComments.this);
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
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateComments.this);
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
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateComments.this);
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
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateComments.this);
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
        return authors.length;
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
            cv = new CommentView(mContext, authors[position],
                    comments[position], status[position], commentID[position]);
        } else {
            cv = (CommentView) convertView;
            cv.setAuthor(authors[position]);
            cv.setComment(comments[position]);
            cv.setStatus(status[position]);
            cv.setId(Integer.valueOf(commentID[position]));
        }
        
        changedComments.clear();
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
    public CommentView(Context context, String author, String comment, String status, String commentID) {
        super(context);

        this.setOrientation(VERTICAL);
        this.setId(Integer.valueOf(commentID));

        // Here we build the child views in code. They could also have
        // been specified in an XML file.

        tvAuthor = new TextView(context);
        tvAuthor.setTextColor(Color.parseColor("#444444"));
        tvAuthor.setPadding(4, 4, 4, 0);
        tvAuthor.setText(author);
        tvAuthor.setTextSize(10);
        addView(tvAuthor, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        tvComment = new TextView(context);
        tvComment.setTextColor(Color.parseColor("#444444"));
        tvComment.setPadding(4, 4, 4, 4);
        tvComment.setText(comment);
        addView(tvComment, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        
        tvStatus = new TextView(context);
        tvStatus.setPadding(4, 4, 4, 4);
        if (status.equals("approve")){
        tvStatus.setTextColor(Color.parseColor("#006505"));
        tvStatus.setText("Approved");
        }
        else if (status.equals("hold")){
            tvStatus.setTextColor(Color.parseColor("#D54E21"));
            tvStatus.setText("Unapproved");
            }
        else if (status.equals("spam")){
            tvStatus.setTextColor(Color.parseColor("#FF0000"));
            tvStatus.setText("Spam");
            }
        addView(tvStatus, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    
    }

    /**
     * Convenience methods
     */
    public void setAuthor(String authorName) {
        tvAuthor.setText(authorName);
    }
    public void setComment(String comment) {
        tvComment.setText(comment);
    }
    public void setStatus(String status) {
    	if (status.equals("approve")){
            tvStatus.setTextColor(Color.parseColor("#006505"));
            tvStatus.setText("Approved");
            }
            else if (status.equals("hold")){
                tvStatus.setTextColor(Color.parseColor("#D54E21"));
                tvStatus.setText("Unapproved");
                }
            else if (status.equals("spam")){
                tvStatus.setTextColor(Color.parseColor("#FF0000"));
                tvStatus.setText("Spam");
                }
    }

    private TextView tvAuthor;
    private TextView tvComment;
    private TextView tvStatus;   
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
            settingsDB settingsDB = new settingsDB(moderateComments.this);
        	settings = settingsDB.loadSettings(moderateComments.this, id);
            
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
		        Date blah = new Date();
		        try {
					blah.setTime(blah.parse(contentHash.get("date_created_gmt").toString()));
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        postHash.put("date_created_gmt", blah);
		        postHash.put("content", contentHash.get("content"));
		        postHash.put("author", contentHash.get("author"));
		        postHash.put("author_url", contentHash.get("author_url"));
		        postHash.put("author_email", contentHash.get("author_email"));

		        
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
					  Toast.makeText(moderateComments.this, "Comment Moderated Succesfully", Toast.LENGTH_SHORT).show();
				  } 
				}; 
				this.runOnUiThread(action);
				Thread action2 = new Thread() 
				{ 
				  public void run() 
				  {
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



