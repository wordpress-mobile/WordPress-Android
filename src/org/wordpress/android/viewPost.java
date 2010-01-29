package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;
import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;


public class viewPost extends Activity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	public String[] authors;
	public String[] comments;
	private String id = "";
	private String postID = "";
	private String accountName = "";
	private String postTitle = "";
	public ProgressDialog pd;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
       
        setContentView(R.layout.viewpost);
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         postID = extras.getString("postID");
         accountName = extras.getString("accountName");
        }   
        
        pd = ProgressDialog.show(viewPost.this,
                "Getting Preview", "Attempting to get preview", true, false);
        
        this.setTitle(accountName + " - Preview");
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
        	
        	XMLRPCMethod method = new XMLRPCMethod("metaWeblog.getPost", new XMLRPCMethodCallback() {
				public void callFinished(Object result) {
					pd.dismiss();
					String s = "done";
					s = result.toString();
					
					HashMap resultHash = (HashMap) result;
					
					String HTML = resultHash.get("description").toString();
					
					WebView wv = (WebView) findViewById(R.id.webView);
					
					String mimetype = "text/html";
					String encoding = "UTF-8";

					wv.loadData(HTML, mimetype, encoding);
					wv.clearCache(true);

				}
	        });
        	
	        Object[] params = {
	        		postID,
	        		sUsername,
	        		sPassword
	        };
	        
	        
	        method.call(params);
        	  
        
        
        
    }


interface XMLRPCMethodCallback {
	void callFinished(Object result);
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
					pd.dismiss();
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPost.this);
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
					pd.dismiss();
					Throwable couse = e.getCause();
					if (couse instanceof HttpHostConnectException) {
						//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
					} else {
						//status.setText("Error " + e.getMessage());
					}
				}
			});
		}
	}
}


}


