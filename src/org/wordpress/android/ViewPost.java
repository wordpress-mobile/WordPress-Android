package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import org.apache.http.protocol.HTTP;
import org.wordpress.android.util.EscapeUtils;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


public class ViewPost extends Activity {
    /** Called when the activity is first created. */
	private XMLRPCClient client;
	public String[] authors;
	public String[] comments;
	private int id;
	private String postID = "";
	private String accountName = "";
	private String httpuser = "";
	private String httppassword = "";
	private boolean loadReader = false;
	private boolean isPage = false;
	ImageButton backButton, forwardButton;
	public ProgressDialog pd;
	private WebView wv;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.viewpost);
        //setProgressBarIndeterminateVisibility(true);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = WordPress.currentBlog.getId();
         postID = extras.getString("postID");
         accountName = extras.getString("accountName");
         isPage = extras.getBoolean("isPage");
         loadReader = extras.getBoolean("loadReader");
        }   
        
        if (loadReader) { 
            this.setTitle(getResources().getText(R.string.reader));
            wv = (WebView) findViewById(R.id.webView);
            wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            backButton = (ImageButton) findViewById(R.id.browserBack);
            backButton.setVisibility(View.VISIBLE);
            backButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    wv.goBack();
                }
            });
            forwardButton = (ImageButton) findViewById(R.id.browserForward);
            forwardButton.setVisibility(View.VISIBLE);
            forwardButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    wv.goForward();
                }
            });
            new loadReaderTask().execute(null, null, null, null);
        }
        else {
            if (isPage){
            	this.setTitle(EscapeUtils.unescapeHtml(accountName) + " - " + getResources().getText(R.string.preview_page));
            }
            else{
            	this.setTitle(EscapeUtils.unescapeHtml(accountName) + " - " + getResources().getText(R.string.preview_post));
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
    }

protected void loadPostFromPermalink() {
    WordPressDB settingsDB = new WordPressDB(this);
    Vector<?> settings = settingsDB.loadSettings(this, id);
    
	String username = settings.get(2).toString();
	String password = settings.get(3).toString();
	httpuser = settings.get(4).toString();
	httppassword = settings.get(5).toString();
	
	String url = settings.get(0).toString();
	
	client = new XMLRPCClient(url, httpuser, httppassword);
    
    Object[] vParams = {
    		postID,
    		username,
    		password
    };
    
    Object versionResult = new Object();
    try {
		versionResult = (Object) client.call("metaWeblog.getPost", vParams);
	} catch (XMLRPCException e) {
		//e.printStackTrace();
	}
	
	String permaLink = null, status = "", html = "";
	
	if (versionResult != null){
		try {
			HashMap<?, ?> contentHash = (HashMap<?, ?>) versionResult;
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
		
		wv.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress)
            {
                ViewPost.this.setTitle("Loading...");
                ViewPost.this.setProgress(progress * 100);
 
                if(progress == 100){
                	if (isPage){
                    	ViewPost.this.setTitle(EscapeUtils.unescapeHtml(accountName) + " - " + getResources().getText(R.string.preview_page));
                    }
                    else{
                    	ViewPost.this.setTitle(EscapeUtils.unescapeHtml(accountName) + " - " + getResources().getText(R.string.preview_post));
                    }
                }
            }
        });
		
		wv.setWebViewClient(new WordPressWebViewClient());
		if (status.equals("publish")){
			int sdk_int = 0;
			try {
				sdk_int = Integer.valueOf(android.os.Build.VERSION.SDK);
			} catch (Exception e1) {
				sdk_int = 3; //assume they are on cupcake
			}
			if (sdk_int >= 8){
				//only 2.2 devices can load https correctly
				wv.loadUrl(permaLink);
			}
			else{
				String url = permaLink.replace("https:", "http:");
				wv.loadUrl(url);
			}
			
		}
		else{
			wv.loadData(html, "text/html", "utf-8");
			Toast.makeText(ViewPost.this, getResources().getText(R.string.basic_html), Toast.LENGTH_SHORT).show();
		}
	}
	else{
    	setProgressBarIndeterminateVisibility(false);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ViewPost.this);
		  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
      dialogBuilder.setMessage(getResources().getText(R.string.permalink_not_found));
      dialogBuilder.setPositiveButton("OK",  new
    		  DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
            // Just close the window.
        	
        }
    });
      dialogBuilder.setCancelable(true);
      if (!isFinishing()) {
    	  dialogBuilder.create().show();
      }
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
    	//setProgressBarIndeterminateVisibility(false);
    	view.clearCache(true);
    }
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){ 
    	handler.proceed();
    }
    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
    	handler.proceed(httpuser, httppassword);
    }
  }
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
  //ignore orientation change
      super.onConfigurationChanged(newConfig); 
  }
  
  private class loadReaderTask extends AsyncTask<String, Void, Vector<?>> {

      protected void onPostExecute(Vector<?> result) {
          
      }

      @Override
      protected Vector<?> doInBackground(String... args) {

          WordPressDB settingsDB = new WordPressDB(ViewPost.this);
          Vector<?> settings = settingsDB.loadSettings(ViewPost.this, id); 
          try {
              String responseContent = "<head>"
                  +"<script type=\"text/javascript\">"
                  +"function submitform(){document.loginform.submit();} </script>"
                  +"</head>"
                  +"<body onload=\"submitform()\">"
                  + "<form style=\"visibility:hidden;\" name=\"loginform\" id=\"loginform\" action=\"" + settings.get(0).toString().replace("xmlrpc.php", "wp-login.php") + "\" method=\"post\">"
                  + "<input type=\"text\" name=\"log\" id=\"user_login\" value=\""+settings.get(2).toString()+"\"/></label>"
                  + "<input type=\"password\" name=\"pwd\" id=\"user_pass\" value=\""+settings.get(3).toString()+"\" /></label>"
                  + "<input type=\"submit\" name=\"wp-submit\" id=\"wp-submit\" value=\"Log In\" />"
                  + "<input type=\"hidden\" name=\"redirect_to\" value=\""+"http://wordpress.com/reader/mobile"+"\" />"
                  + "</form>"
                  +"</body>";

              wv.setWebViewClient(new WebViewClient() {
                  public boolean shouldOverrideUrlLoading(WebView view, String url){
                      view.loadUrl(url);
                      return false;
                 }
              });
              
              wv.setWebChromeClient(new WebChromeClient() {
                  public void onProgressChanged(WebView view, int progress)
                  {
                      ViewPost.this.setTitle("Loading...");
                      ViewPost.this.setProgress(progress * 100);
       
                      if(progress == 100){
                           ViewPost.this.setTitle(getResources().getText(R.string.reader));
                           //commenting out for now, may not be possible to support this with the WP.com reader
                           /*if (wv.getTitle() != null) {
                              if (!wv.canGoBack() || wv.getTitle().equals("WordPress.com Mobile Reader Ñ WordPress.com")) {
                                  backButton.setEnabled(false);
                              }
                              else {
                                  backButton.setEnabled(true);
                              }

                              if (!wv.canGoForward()) {
                                  forwardButton.setEnabled(false);
                              }
                              else {
                                  forwardButton.setEnabled(true);
                              }
                           }*/
                      }
                  }
              });
              
              wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
              wv.getSettings().setBuiltInZoomControls(true);
              wv.getSettings().setJavaScriptEnabled(true);
              wv.loadData(responseContent, "text/html", HTTP.UTF_8);
          } catch (Exception ex) {
              ex.printStackTrace();
          }
          return null;

      }

  }
  
}


