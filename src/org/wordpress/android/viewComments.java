package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;


public class viewComments extends ListActivity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	public String[] authors;
	public String[] comments;
	public String[] status;
	private String id = "";
	private String postID = "";
	private String accountName = "";
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
       
        setContentView(R.layout.viewcomments);
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         postID = extras.getString("postID");
         accountName = extras.getString("accountName");
        }      
        
        this.setTitle(accountName + " - " + getResources().getText(R.string.view_comments));
        WordPressDB settingsDB = new WordPressDB(this);
        Vector<?> settings = settingsDB.loadSettings(this, id);
        
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
		String sHttpuser = settings.get(4).toString();
		String sHttppassword = settings.get(5).toString();
            
            HashMap<String, Object> hPost = new HashMap<String, Object>();
            hPost.put("status", "approve");
            hPost.put("post_id", postID);
            hPost.put("number", 10);

        	client = new XMLRPCClient(sURL, sHttpuser, sHttppassword);
        	
        	XMLRPCMethod method = new XMLRPCMethod("wp.getComments", new XMLRPCMethodCallback() {
				public void callFinished(Object[] result) {
					
					if (result.length == 0){
						comments = new String[1];
						authors = new String[1];
						comments[0] = getResources().getText(R.string.no_approved_comments).toString();
						authors[0] = "";
					}
					else{
						comments = new String[result.length];
						authors = new String[result.length];
						
					    for (int ctr = 0; ctr < result.length; ctr++) {
					    	HashMap<?, ?> contentHash = (HashMap<?, ?>) result[ctr];
					        comments[ctr] = contentHash.get("content").toString();
					        authors[ctr] = contentHash.get("author").toString();
					    }
					}  
		        
					 setListAdapter(new CommentListAdapter(viewComments.this));

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
			final Object[] result = (Object[]) client.call(method, params);
			handler.post(new Runnable() {
				public void run() {

					callBack.callFinished(result);
				}
			});
		} catch (final XMLRPCFault e) {
			handler.post(new Runnable() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewComments.this);
					  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
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
                    comments[position]);
        } else {
            cv = (CommentView) convertView;
            cv.setAuthor(authors[position]);
            cv.setComment(comments[position]);
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
    public CommentView(Context context, String author, String comment) {
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

    private TextView tvAuthor;
    private TextView tvComment;
}

}


