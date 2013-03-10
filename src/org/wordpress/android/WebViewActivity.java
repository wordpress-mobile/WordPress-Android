package org.wordpress.android;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;

import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;

public class WebViewActivity extends SherlockActivity {
    /** Called when the activity is first created. */
    public String[] authors;
    public String[] comments;
    private String httpuser = "";
    private String httppassword = "";
    private String loginURL = "";
    private boolean loadReader = false;
    private boolean loadAdmin = false;
    private boolean isPage = false;
    ImageButton backButton, forwardButton, refreshButton;
    public ProgressDialog pd;
    private WebView wv;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.reader);

        // setProgressBarIndeterminateVisibility(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            loadReader = extras.getBoolean("loadReader");
            loadAdmin = extras.getBoolean("loadAdmin");
        }

        if (WordPress.currentBlog == null) {
            if (WordPress.setCurrentBlogToLastActive() == null) {
                Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        httpuser = WordPress.currentBlog.getHttpuser();
        httppassword = WordPress.currentBlog.getHttppassword();

        if (loadReader || loadAdmin) {

            this.setTitle(getResources().getText(R.string.reader));
            wv = (WebView) findViewById(R.id.webView);
            wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            new loadReaderTask().execute(null, null, null, null);

        } else {
            if (isPage) {
                this.setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
                        .getBlogName())
                        + " - "
                        + getResources().getText(R.string.preview_page));
            } else {
                this.setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
                        .getBlogName())
                        + " - "
                        + getResources().getText(R.string.preview_post));
            }

            loadPostFromPermalink();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (loadReader) {
            menu.add(0, 0, 0, getResources().getText(R.string.home));
            MenuItem menuItem = menu.findItem(0);
            menuItem.setIcon(R.drawable.ic_menu_home);

            menu.add(0, 1, 0, getResources().getText(R.string.view_in_browser));
            menuItem = menu.findItem(1);
            menuItem.setIcon(android.R.drawable.ic_menu_view);

            menu.add(0, 2, 0, getResources().getText(R.string.refresh));
            menuItem = menu.findItem(2);
            menuItem.setIcon(R.drawable.ic_menu_refresh);
        }
        return true;
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case 0:
            finish();
            break;
        case 1:
            if (!wv.getUrl().contains("wp-login.php")) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(wv.getUrl()));
                startActivity(i);
            }
            break;
        case 2:
            wv.reload();
            new Thread(new Runnable() {
                public void run() {
                    // refresh stat
                    try {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpProtocolParams.setUserAgent(httpclient.getParams(),
                                "wp-android");
                        String readerURL = Constants.readerURL + "/?template=stats&stats_name=home_page_refresh";
                        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
                            readerURL += "&per_page=20";
                        }

                        httpclient.execute(new HttpGet(readerURL));
                    } catch (Exception e) {
                        // oh well
                    }
                }
            }).start();
            break;
        }

        return false;
    }

    protected void loadPostFromPermalink() {

        WebView wv = (WebView) findViewById(R.id.webView);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setJavaScriptEnabled(true);

        wv.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                WebViewActivity.this.setTitle("Loading...");
                WebViewActivity.this.setProgress(progress * 100);

                if (progress == 100) {
                    if (isPage) {
                        WebViewActivity.this.setTitle(EscapeUtils
                                .unescapeHtml(WordPress.currentBlog
                                        .getBlogName())
                                + " - "
                                + getResources().getText(R.string.preview_page));
                    } else {
                        WebViewActivity.this.setTitle(EscapeUtils
                                .unescapeHtml(WordPress.currentBlog
                                        .getBlogName())
                                + " - "
                                + getResources().getText(R.string.preview_post));
                    }
                }
            }
        });

        wv.setWebViewClient(new WordPressWebViewClient());
        if (WordPress.currentPost != null) {
            Post post = WordPress.currentPost;
            String previewUrl = post.getPermaLink();
            boolean isPrivate = false;
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> blogOptions = gson.fromJson(WordPress.currentBlog.getBlogOptions(), type);
                StringMap<?> blogPublicOption = (StringMap<?>)blogOptions.get("blog_public");
                String blogPublicOptionValue = blogPublicOption.get("value").toString();
                if (blogPublicOptionValue.equals("-1")) {
                    isPrivate = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isPrivate || post.isLocalDraft() || post.isLocalChange() || !post.getPost_status().equals("publish")) {
                if (-1 == previewUrl.indexOf('?')) {
                    previewUrl = previewUrl.concat("?preview=true");
                } else {
                    previewUrl = previewUrl.concat("&preview=true");
                }
                if (WordPress.currentBlog.getUrl().lastIndexOf("/") != -1)
                    loginURL = WordPress.currentBlog.getUrl().substring(0, WordPress.currentBlog.getUrl().lastIndexOf("/")) + "/wp-login.php";
                else
                    loginURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php", "wp-login.php");

                String postData = String.format("log=%s&pwd=%s&redirect_to=%s", WordPress.currentBlog.getUsername(), WordPress.currentBlog.getPassword(), URLEncoder.encode(previewUrl));
                wv.postUrl(loginURL, EncodingUtils.getBytes(postData, "utf-8"));
            } else {
                wv.loadUrl(previewUrl);
            }
        }

    }

    private class WordPressWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            handler.proceed(httpuser, httppassword);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    private class loadReaderTask extends AsyncTask<String, Void, List<?>> {

        protected void onPostExecute(List<?> result) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpProtocolParams.setUserAgent(httpclient.getParams(),
                                "wp-android");
                        String readerURL = Constants.readerURL + "/?template=stats&stats_name=home_page";
                        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
                            readerURL += "&per_page=20";
                        }

                        httpclient.execute(new HttpGet(readerURL));
                    } catch (Exception e) {
                        // oh well
                    }
                }
            }).start();
        }

        @Override
        protected List<?> doInBackground(String... args) {

            if (WordPress.currentBlog == null) {
                WordPress.setCurrentBlogToLastActive();
            }

            loginURL = WordPress.currentBlog.getUrl()
                    .replace("xmlrpc.php", "wp-login.php");
            if (WordPress.currentBlog.getUrl().lastIndexOf("/") != -1)
                loginURL = WordPress.currentBlog.getUrl().substring(0, WordPress.currentBlog.getUrl().lastIndexOf("/")) + "/wp-login.php";
            else
                loginURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php", "wp-login.php");
            String readerURL = Constants.readerURL;
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
                readerURL += "/?per_page=20";
            }
            if (loadAdmin) {
                if (WordPress.currentBlog.getUrl().lastIndexOf("/") != -1)
                    readerURL = WordPress.currentBlog.getUrl().substring(0, WordPress.currentBlog.getUrl().lastIndexOf("/")) + "/wp-admin";
                else
                    readerURL = WordPress.currentBlog.getUrl().replace("xmlrpc.php", "wp-admin");
            }
            try {
                String responseContent = "<head>"
                        + "<script type=\"text/javascript\">"
                        + "function submitform(){document.loginform.submit();} </script>"
                        + "</head>"
                        + "<body onload=\"submitform()\">"
                        + "<form style=\"visibility:hidden;\" name=\"loginform\" id=\"loginform\" action=\""
                        + loginURL
                        + "\" method=\"post\">"
                        + "<input type=\"text\" name=\"log\" id=\"user_login\" value=\""
                        + WordPress.currentBlog.getUsername()
                        + "\"/></label>"
                        + "<input type=\"password\" name=\"pwd\" id=\"user_pass\" value=\""
                        + WordPress.currentBlog.getPassword()
                        + "\" /></label>"
                        + "<input type=\"submit\" name=\"wp-submit\" id=\"wp-submit\" value=\"Log In\" />"
                        + "<input type=\"hidden\" name=\"redirect_to\" value=\""
                        + readerURL + "\" />" + "</form>" + "</body>";

                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view,
                            String url) {
                        view.loadUrl(url);
                        return false;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                    }

                    @Override
                    public void onReceivedHttpAuthRequest(WebView view,
                            HttpAuthHandler handler, String host, String realm) {
                        if (!httpuser.equals(""))
                            handler.proceed(httpuser, httppassword);
                    }
                });

                wv.setWebChromeClient(new WebChromeClient() {
                    public void onProgressChanged(WebView view, int progress) {
                        WebViewActivity.this.setTitle("Loading...");
                        WebViewActivity.this.setProgress(progress * 100);

                        if (progress == 100) {
                            if (loadReader)
                                WebViewActivity.this.setTitle(getResources().getText(
                                        R.string.reader));
                            else
                                WebViewActivity.this.setTitle(getResources().getText(
                                        R.string.wp_admin));
                        }
                    }
                });

                wv.getSettings().setUserAgentString("wp-android");
                wv.getSettings().setCacheMode(
                        WebSettings.LOAD_NO_CACHE);
                wv.getSettings().setSavePassword(false);
                wv.getSettings().setBuiltInZoomControls(true);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.getSettings().setPluginsEnabled(true);
                wv.getSettings().setDomStorageEnabled(true);
                wv.loadData(Uri.encode(responseContent), "text/html", HTTP.UTF_8);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;

        }

    }

    @Override
    public boolean onKeyDown(int i, KeyEvent event) {

        if (i == KeyEvent.KEYCODE_BACK) {
            if (loadReader) {
                if (wv.canGoBack()
                        && !wv.getUrl().startsWith(Constants.readerURL)
                        && !wv.getUrl().equals(loginURL)) {
                    wv.goBack();
                } else {
                    finish();
                }
            } else {
                finish();
            }
        }

        return false;
    }

}
