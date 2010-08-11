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
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;


public class viewPost extends Activity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	public String[] authors;
	public String[] comments;
	private String id = "";
	private String postID = "";
	private String accountName = "";
	private String postTitle = "";
	private boolean isPage = false;
	public ProgressDialog pd;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.viewpost);
        setProgressBarIndeterminateVisibility(true);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         postID = extras.getString("postID");
         accountName = extras.getString("accountName");
         isPage = extras.getBoolean("isPage");
        }   
        
        if (isPage){
        	this.setTitle(escapeUtils.unescapeHtml(accountName) + " - " + getResources().getText(R.string.preview_page));
        }
        else{
        	this.setTitle(escapeUtils.unescapeHtml(accountName) + " - " + getResources().getText(R.string.preview_post));
        }
        
        Thread t = new Thread() 
		{ 
		  public void run() 
		  {
			  loadPostFromPermalink();
		  } 
		}; 
		t.start();

    }

protected void loadPostFromPermalink() {
	Vector settings = new Vector();
    WordPressDB settingsDB = new WordPressDB(this);
	settings = settingsDB.loadSettings(this, id);
    
	String username = settings.get(2).toString();
	String password = settings.get(3).toString();
	
	String url = settings.get(0).toString();
	
	client = new XMLRPCClient(url);
    
    HashMap hPost = new HashMap();
    Object[] vParams = {
    		postID,
    		username,
    		password
    };
    
    Object versionResult = new Object();
    try {
		versionResult = (Object) client.call("metaWeblog.getPost", vParams);
	} catch (XMLRPCException e) {
		// TODO Auto-generated catch block
		//e.printStackTrace();
	}
	
	String permaLink = null, status = "", html = "";
	
	if (versionResult != null){
		try {
			HashMap contentHash = (HashMap) versionResult;
			permaLink = contentHash.get("permaLink").toString();
			status = contentHash.get("post_status").toString();
			html = contentHash.get("description").toString();
		} catch (Exception e) {
		}
	}
	
	displayResults(permaLink, html, status);

		
	}

private void displayResults(final String permaLink, final String html, final String status) {
	Thread t = new Thread() 
	{ 
	  public void run() 
	  {
	if (permaLink != null){
		WebView wv = (WebView) findViewById(R.id.webView);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		//pretend we're a desktop browser
		//wv.getSettings().setUserAgentString("Mozilla/5.0 (Linux) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Safari/530.17");
		wv.getSettings().setBuiltInZoomControls(true);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.setWebViewClient(new WordPressWebViewClient());
		if (status.equals("publish")){
			wv.loadUrl(permaLink);
		}
		else{
			wv.loadData(html, "text/html", "utf-8");
			Toast.makeText(viewPost.this, "Showing basic HTML preview until post is published.", Toast.LENGTH_SHORT).show();
		}
	}
	else{
    	setProgressBarIndeterminateVisibility(false);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(viewPost.this);
		  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
      dialogBuilder.setMessage(getResources().getText(R.string.permalink_not_found));
      dialogBuilder.setPositiveButton("OK",  new
    		  DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
            // Just close the window.
        	
        }
    });
      dialogBuilder.setCancelable(true);
     dialogBuilder.create().show();
	}
	  }
	};
	this.runOnUiThread(t);
	
}

private class WordPressWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }
    @Override
    public void onPageFinished(WebView  view, String  url){
    	setProgressBarIndeterminateVisibility(false);
    	view.clearCache(true);
    }
}

@Override
public void onConfigurationChanged(Configuration newConfig) {
  //ignore orientation change
  super.onConfigurationChanged(newConfig);
}

}


