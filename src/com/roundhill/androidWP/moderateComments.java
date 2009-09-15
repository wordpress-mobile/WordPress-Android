//by Dan Roundhill, danroundhill.com/wptogo
package com.roundhill.androidWP;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


public class moderateComments extends ListActivity implements RadioGroup.OnCheckedChangeListener{
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
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
       
        setContentView(R.layout.moderatecomments);
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");
        }      
        
        this.setTitle(accountName + " - Moderate Comments");
        Vector settings = new Vector();
        settingsDB settingsDB = new settingsDB(this);
    	settings = settingsDB.loadSettings(this, id);
    	
    	//set up save button
    	final Button moderateButton = (Button) findViewById(R.id.moderate);   
        
        moderateButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	showDialog(ID_DIALOG_POSTING);
            	
            	Thread t = new Thread() {
        			String resultCode = "";
    				public void run() {
    				Looper.prepare();
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
            	
            	
            	
            	Set set= changedComments.keySet(); 
                Iterator iter = set.iterator(); 
                int i=1; 
                while ( iter.hasNext (  )  )  {  
                  HashMap changedComment = (HashMap) changedComments.get(iter.next());
                  String commentID = changedComment.get("commentID").toString();
                  String commentStatus = changedComment.get("commentStatus").toString();
                  
                  if (commentStatus.equals("Approved")){
                	  commentStatus = "approve";
                  }
                  else if (commentStatus.equals("Unapproved")){
                	  commentStatus = "hold";
                  }
                  else if (commentStatus.equals("Spam")){
                	  commentStatus = "spam";
                  }
                  i++; 
                
            		HashMap contentHash, postHash = new HashMap();
            		contentHash = (HashMap) allComments.get(String.valueOf(commentID));
    		        postHash.put("status", commentStatus);
    		        Date blah = new Date();
    		        blah.setTime(blah.parse(contentHash.get("date_created_gmt").toString()));
    		        postHash.put("date_created_gmt", blah);
    		        postHash.put("content", contentHash.get("content"));
    		        postHash.put("author", contentHash.get("author"));
    		        postHash.put("author_url", contentHash.get("author_url"));
    		        postHash.put("author_email", contentHash.get("author_email"));

    		        
            		
            	
            	XMLRPCMethodEditComment method = new XMLRPCMethodEditComment("wp.editComment", new XMLRPCMethodCallbackEditComment() {
    				public void callFinished(Object result) {
    					String s = "done";
    					s = result.toString();

    					

    				}
    	        });
    	        Object[] params = {
    	        		1,
    	        		sUsername,
    	        		sPassword,
    	        		commentID,
    	        		postHash
    	        };
    	        
    	        
    	        method.call(params);
            	
            	}
                dismissDialog(ID_DIALOG_POSTING);
                changedComments.clear();
                
           
            Toast.makeText(moderateComments.this, "Comment Moderated Succesfully", 20);
            }
            
            	
            	
			};
			t.start();	
			
			
			
            }  
        });
    	
        
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
						comments = new String[1];
						authors = new String[1];
						status = new String[1];
						comments[0] = "There's no comments on your blog? Sad.";
						authors[0] = "";
						status[0] = "";
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
					}  
		        
					 setListAdapter(new CommentListAdapter(moderateComments.this));
					 changedComments.clear();
					 initializing = false;

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

					Throwable couse = e.getCause();
					if (couse instanceof HttpHostConnectException) {
						//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
					} else {
						//status.setText("Error " + e.getMessage());
					}
					//Log.d("Test", "error", e);
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
		try {
			join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
					Looper.myLooper().quit();
					
				}
			});
		} catch (final XMLRPCFault e) {
			handler.post(new Runnable() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(moderateComments.this);
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

					Throwable couse = e.getCause();
					if (couse instanceof HttpHostConnectException) {
						//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
					} else {
						//status.setText("Error " + e.getMessage());
					}
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
            //cv.setStatus(status[position]);
            cv.setCommentID(commentID[position]);
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

        // Here we build the child views in code. They could also have
        // been specified in an XML file.

        tvAuthor = new TextView(context);
        tvAuthor.setTextColor(Color.parseColor("#444444"));
        tvAuthor.setPadding(4, 4, 4, 4);
        tvAuthor.setText(author);
        addView(tvAuthor, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        tvComment = new TextView(context);
        tvComment.setTextColor(Color.parseColor("#444444"));
        tvComment.setPadding(4, 4, 4, 4);
        tvComment.setText(comment);
        addView(tvComment, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        
        /*tvStatus = new TextView(context);
        tvStatus.setTextColor(Color.parseColor("#444444"));
        tvStatus.setPadding(4, 4, 4, 4);
        tvStatus.setText(status);
        addView(tvStatus, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));*/
        
        rgCommentStatus = new RadioGroup(moderateComments.this);
        rgCommentStatus.setOnCheckedChangeListener(moderateComments.this);
        rgCommentStatus.setPadding(4, 4, 4, 4);
        addView(rgCommentStatus, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        rgCommentStatus.setOrientation(0);
        rgCommentStatus.setId(Integer.valueOf(commentID));
        
              
        RadioButton rb1 = new RadioButton(context);
        RadioButton rb2 = new RadioButton(context);
        RadioButton rb3 = new RadioButton(context);
        
 
        rgCommentStatus.addView(rb3, 0);
        rgCommentStatus.addView(rb2, 0);
        rgCommentStatus.addView(rb1, 0);
       
        rb3.setText("Spam");
        if (status.equals("spam")){
        	rb3.toggle();
        }
        rb3.setTextColor(Color.parseColor("#444444"));

        rb2.setText("Unapproved");
        if (status.equals("hold")){
        	rb2.toggle();
        }
        rb2.setTextColor(Color.parseColor("#444444"));
  
        
        rb1.setText("Approved");
        if (status.equals("approve")){
        	rb1.toggle();
        }
        rb1.setTextColor(Color.parseColor("#444444"));
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
        tvStatus.setText(status);
    }
    public void setCommentID(String commentID){
    	rgCommentStatus.setId(Integer.valueOf(commentID));
    }

    private TextView tvAuthor;
    private TextView tvComment;
    private TextView tvStatus;
	private RadioGroup rgCommentStatus;    
}

public void onCheckedChanged(RadioGroup group, int checkedId) {
	
	HashMap commentHash = new HashMap();
	RadioButton tempButton = (RadioButton) group.findViewById(checkedId);
	String commentValue = tempButton.getText().toString();
	
	boolean checkOrigStatus = checkOrigStatus(group.getId(), commentValue);
	if (!checkOrigStatus){
	commentHash.put("commentID", group.getId());
	commentHash.put("commentStatus", commentValue);
	changedComments.put(String.valueOf(group.getId()), commentHash);
	}
	else if (changedComments.containsKey(String.valueOf(group.getId()))){
		changedComments.remove(String.valueOf(group.getId()));
	}
	
}

private boolean checkOrigStatus(int id2, String commentValue) {
	
	Set set= allComments.keySet(); 
    Iterator iter = set.iterator(); 
    boolean origStatus = false;
    HashMap comment = (HashMap) allComments.get(String.valueOf(id2));
    String convertStatusName = comment.get("status").toString();
    
    if (convertStatusName.equals("approve")){
    	convertStatusName = "Approved";
    }
    else if (convertStatusName.equals("hold")){
    	convertStatusName = "Unapproved";
    }
    else if (convertStatusName.equals("spam")){
    	convertStatusName = "Spam";
    }
    
    if (!initializing){
    	if (String.valueOf(id2).equals(comment.get("comment_id".toString())) && commentValue.equals(convertStatusName)){
    	origStatus = true;
    	}
    }
	
	return origStatus;
}

@Override
protected Dialog onCreateDialog(int id) {
if(id == ID_DIALOG_POSTING){
ProgressDialog loadingDialog = new ProgressDialog(this);
loadingDialog.setMessage("Editing Comment(s)...");
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

}



