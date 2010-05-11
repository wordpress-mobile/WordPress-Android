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
        
        settingsDB settingsDB = new settingsDB(this);
        Vector account = settingsDB.loadSettings(this, id);
        String blogURL = account.get(0).toString();
        blogURL = blogURL.replace("xmlrpc.php", "") + "?p=" + postID;
        
        WebView wv = (WebView) findViewById(R.id.webView);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		//pretend we're a desktop browser
		wv.getSettings().setUserAgentString("Mozilla/5.0 (Linux) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Safari/530.17");
		wv.getSettings().setBuiltInZoomControls(true);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.setWebViewClient(new WordPressWebViewClient());

		wv.loadUrl(blogURL);
        
        
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


