
package org.wordpress.android;

import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.EscapeUtils;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.HashMap;

public class ViewPost extends Activity {
    /** Called when the activity is first created. */
    private XMLRPCClient client;
    public String[] authors;
    public String[] comments;
    private int id;
    private String postID = "";
    private boolean isPage = false;
    private Blog blog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.viewpost);
        // setProgressBarIndeterminateVisibility(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("id");
            blog = new Blog(id, this);
            postID = extras.getString("postID");
            isPage = extras.getBoolean("isPage");
        }

        if (isPage) {
            this.setTitle(EscapeUtils.unescapeHtml(blog.getBlogName()) + " - "
                    + getResources().getText(R.string.preview_page));
        } else {
            this.setTitle(EscapeUtils.unescapeHtml(blog.getBlogName()) + " - "
                    + getResources().getText(R.string.preview_post));
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
        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

        Object[] vParams = {
                postID,
                blog.getUsername(),
                blog.getPassword()
        };

        Object versionResult = new Object();
        try {
            versionResult = (Object) client.call("metaWeblog.getPost", vParams);
        } catch (XMLRPCException e) {
            // e.printStackTrace();
        }

        String permaLink = null, status = "", html = "";

        if (versionResult != null) {
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
            if (permaLink != null) {
                WebView wv = (WebView) findViewById(R.id.webView);
                wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                // pretend we're a desktop browser
                // wv.getSettings().setUserAgentString("Mozilla/5.0 (Linux) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Safari/530.17");
                wv.getSettings().setBuiltInZoomControls(true);
                wv.getSettings().setJavaScriptEnabled(true);

                wv.setWebChromeClient(new WebChromeClient() {
                    public void onProgressChanged(WebView view, int progress)
                {
                    ViewPost.this.setTitle("Loading...");
                    ViewPost.this.setProgress(progress * 100);

                    if (progress == 100) {
                        if (isPage) {
                            ViewPost.this.setTitle(EscapeUtils.unescapeHtml(blog.getBlogName())
                                    + " - " + getResources().getText(R.string.preview_page));
                        }
                        else {
                            ViewPost.this.setTitle(EscapeUtils.unescapeHtml(blog.getBlogName())
                                    + " - " + getResources().getText(R.string.preview_post));
                        }
                    }
                }
                });

                wv.setWebViewClient(new WordPressWebViewClient());
                if (status.equals("publish")) {
                    int sdk_int = 0;
                    try {
                        sdk_int = Integer.valueOf(android.os.Build.VERSION.SDK);
                    } catch (Exception e1) {
                        sdk_int = 3; // assume they are on cupcake
                    }
                    if (sdk_int >= 8) {
                        // only 2.2 devices can load https correctly
                        wv.loadUrl(permaLink);
                    }
                else {
                    String url = permaLink.replace("https:", "http:");
                    wv.loadUrl(url);
                }

            }
                else {
                    String encodedHTML = "<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><body>"
                            + html + "</body></html>";
                    wv.loadData(encodedHTML, "text/html", "utf-8");
                    Toast.makeText(ViewPost.this, getResources().getText(R.string.basic_html),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                setProgressBarIndeterminateVisibility(false);
                if (!isFinishing()) {
                    AlertUtil.showAlert(ViewPost.this, R.string.connection_error,
                            R.string.permalink_not_found);
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
        public void onPageFinished(WebView view, String url) {
            // setProgressBarIndeterminateVisibility(false);
            view.clearCache(true);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host,
                String realm) {
            handler.proceed(blog.getHttpuser(), blog.getHttppassword());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }
}
