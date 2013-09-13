/**
 *
 */
package org.wordpress.android.ui;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ConnectionClient;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

/**
 * @author Eric
 *
 */
public class OldStatsActivity extends WPActionBarActivity {

    static String lastAuthedName = "";

    private ConnectionClient client;
    private HttpPost postMethod;
    private HttpParams httpParams;
    protected String errorMsg = "";

    private WebView webView;
    boolean loginShowing = false;
    boolean authed = false;
    boolean isRetrying = false;
    private AsyncTask<String, Void, List<?>> currentTask = null;
    private MenuItem refreshMenuItem;

    private boolean isAnimatingRefreshButton;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.view_web_stats);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.tab_stats));
        
        EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
        EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new StatsWebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSavePassword(false);
        clearCookies();
        webView.clearCache(false);

        Button saveStatsLogin = (Button) findViewById(R.id.saveDotcom);
        saveStatsLogin.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
                EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

                String dcUsername = dotcomUsername.getText().toString();
                String dcPassword = dotcomPassword.getText().toString();

                if (dcUsername.equals("") || dcPassword.equals("")) {
                    showErrorDialog(
                        getResources().getText(R.string.required_fields).toString(),
                        getResources().getText(R.string.username_password_required).toString()
                    );

                } else {
                    WordPress.currentBlog.setDotcom_username(dcUsername);
                    WordPress.currentBlog.setDotcom_password(dcPassword);
                    WordPress.currentBlog.save(WordPress.currentBlog.getUsername());
                    hideLoginForm();

                    initStats(); // start over again now that we have the login
                }
            }
        });

        TextView wpcomHelp = (TextView) findViewById(R.id.wpcomHelp);
        wpcomHelp.setOnClickListener(new TextView.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://jetpack.me/about"));
                startActivity(intent);

            }
        });

        CookieSyncManager.createInstance(this);

        this.initStats();
    }


    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }


    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.clearCache(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.old_stats_menu, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            reloadStats();
        }

        return super.onOptionsItemSelected(item);
    }


    private void initStats() {
        this.startAnimatingRefreshButton(refreshMenuItem);
        Blog blog = WordPress.getCurrentBlog();
        if (blog == null) {
            return;
        }
        if (!blog.isDotcomFlag() && blog.getApi_blogid() == null) {
            // first run or was deleted.
            this.checkAPIBlogInfo();
        } else if(!blog.isDotcomFlag() && blog.getDotcom_username() == null) {
            // .org blog with no corresponding .com jetpack login.
            this.showLoginForm();
        } else {
            this.loadStats();
        }
    }

    private void checkAPIBlogInfo() {
        String sUsername, sPassword;
        Blog blog = WordPress.currentBlog;

        if (blog.isDotcomFlag()) {
            sUsername = blog.getUsername();
            sPassword = blog.getPassword();
        } else {
            // we have an alternate login, use that instead
            sUsername = blog.getDotcom_username();
            sPassword = blog.getDotcom_password();
        }

        this.startAnimatingRefreshButton(refreshMenuItem);

        // Start an async task to retrieve the blog's data from the api.
        currentTask = new StatsAPIBlogInfoAsyncTask().execute(sUsername, sPassword);
    }

    private void clearCookies() {
        //get rid of old auth cookie
        CookieSyncManager.createInstance(OldStatsActivity.this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    protected void authStats() {
        String sUsername, sPassword;
        Blog blog = WordPress.currentBlog;

        if (blog.isDotcomFlag()) {
            sUsername = blog.getUsername();
            sPassword = blog.getPassword();
        } else {
            // we have an alternate login, use that instead
            sUsername = blog.getDotcom_username();
            sPassword = blog.getDotcom_password();
        }

        if(lastAuthedName.equals(sUsername)) {
            // Check for a valid auth cookie for this username.
            CookieManager cookieManager = CookieManager.getInstance();
            if(cookieManager.hasCookies()){
                String rawCookieString = cookieManager.getCookie("wordpress.com");
                if (rawCookieString != null && rawCookieString.length() > 0) {
                    rawCookieString = rawCookieString.toLowerCase();
                    Log.d("cookeis", rawCookieString);
                    String[] rawCookies = rawCookieString.split(";");
                    String[] rawCookieNameAndValue = rawCookies[0].split("=");
                    String val = rawCookieNameAndValue[1].trim();
                    if (val.indexOf(sUsername.toLowerCase()) != -1) {
                        authed = true;
                        this.loadStats();
                        return;
                    }
                }
            }
        }

        currentTask = new AuthStatsAsyncTask().execute(sUsername, sPassword);
    }


    protected void loadStats() {
        if (!authed) {
            this.authStats();
            return;
        }

        Blog blog = WordPress.currentBlog;
        String id = "";
        if(blog.isDotcomFlag()) {
            id = Integer.toString( WordPress.currentBlog.getBlogId() );
        } else {
            id = blog.getApi_blogid();
        }

        webView.setVisibility(View.VISIBLE);
        String path = "http://wordpress.com/?no-chrome#!/my-stats/?unit=1&blog=" + id;
        webView.loadUrl(path);

        // Clear the history here so in a case where the user has changed blogs via the titlebar,
        // tapping the back button will not try to load the previous blog's stats.
        webView.clearHistory();
    }


    public void reloadStats() {
        webView.reload();
    }


    private void configureClient(URI uri, String username, String password) {
        postMethod = new HttpPost(uri);

        postMethod.addHeader("charset", "UTF-8");
        postMethod.addHeader("User-Agent", "wp-android/" + WordPress.versionName);

        httpParams = postMethod.getParams();
        HttpProtocolParams.setUseExpectContinue(httpParams, false);
        UsernamePasswordCredentials creds;
        // username & password for basic http auth
        if (username != null) {
            creds = new UsernamePasswordCredentials(username, password);
        } else {
            creds = new UsernamePasswordCredentials("", "");
        }

        // this gets connections working over https
        if (uri.getScheme() != null) {
            if (uri.getScheme().equals("https")) {
                if (uri.getPort() == -1)
                    try {
                        client = new ConnectionClient(creds, 443);
                    } catch (KeyManagementException e) {
                        client = new ConnectionClient(creds);
                    } catch (NoSuchAlgorithmException e) {
                        client = new ConnectionClient(creds);
                    } catch (KeyStoreException e) {
                        client = new ConnectionClient(creds);
                    } catch (UnrecoverableKeyException e) {
                        client = new ConnectionClient(creds);
                    }
                else
                    try {
                        client = new ConnectionClient(creds, uri.getPort());
                    } catch (KeyManagementException e) {
                        client = new ConnectionClient(creds);
                    } catch (NoSuchAlgorithmException e) {
                        client = new ConnectionClient(creds);
                    } catch (KeyStoreException e) {
                        client = new ConnectionClient(creds);
                    } catch (UnrecoverableKeyException e) {
                        client = new ConnectionClient(creds);
                    }
            } else {
                client = new ConnectionClient(creds);
            }
        } else {
            client = new ConnectionClient(creds);
        }
    }


    public void showLoginForm() {
        if (loginShowing) return;
        loginShowing = true;

        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);
        moderationBar.setVisibility(View.VISIBLE);
        moderationBar.startAnimation(set);
    }


    public void hideLoginForm() {
        if(!loginShowing) return;
        loginShowing = false;

        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);
        ;
        RelativeLayout moderationBar = (RelativeLayout) findViewById(R.id.dotcomLogin);
        moderationBar.clearAnimation();
        moderationBar.startAnimation(set);
        moderationBar.setVisibility(View.INVISIBLE);

    }


    public void showErrorDialog(String title, String msg) {
        if(isFinishing()) return;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(OldStatsActivity.this);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(msg);
        dialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.
                    }
                });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ( webView.canGoBack() ) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     *
     */
    protected class StatsWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //Log.d("WP", url);
            startAnimatingRefreshButton(refreshMenuItem);
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            if (authed) {
                // The webview loads an empty string during init/auth. We don't want to stop the refresh icon in this case.
                stopAnimatingRefreshButton(refreshMenuItem);
            }
        }


        @Override
        public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
            stopAnimatingRefreshButton(refreshMenuItem);
        }


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!url.equalsIgnoreCase(Constants.readerDetailURL)) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);

                return true;
            }
            return false;
        }
    }


    /*
     * Call to authenticate so we can display stats.  If auth fails prompt
     * for updated wp.com credentials.
     */
    private class AuthStatsAsyncTask extends AsyncTask<String, Void, List<?>> {

        int statusCode = 0;

        @Override
        protected List<?> doInBackground(String... args) {
            List<String> result = null;

            String sUsername = args[0];
            String sPassword = args[1];
            try {

                URI uri = URI.create("https://wordpress.com/wp-login.php");
                configureClient(uri, sUsername, sPassword);

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                nameValuePairs.add(new BasicNameValuePair("log", sUsername));
                nameValuePairs.add(new BasicNameValuePair("pwd", sPassword));
                nameValuePairs.add(new BasicNameValuePair("rememberme", "forever"));
                nameValuePairs.add(new BasicNameValuePair("wp-submit", "Log In"));
                nameValuePairs.add(new BasicNameValuePair("redirect_to", "/"));
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

                HttpResponse response = client.execute(postMethod);

                statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    throw new IOException("HTTP status code: " + statusCode
                            + " was returned. "
                            + response.getStatusLine().getReasonPhrase());
                }

                List<Cookie> cookies = client.getCookieStore().getCookies();
                if(!cookies.isEmpty()) {

                    CookieManager cookieManager = CookieManager.getInstance();

                    for (Cookie cookie : cookies){
                        if (cookie.getName().equalsIgnoreCase("wordpress_logged_in")) {
                            // Auth cookie found so mark the parent class authed and set the lastAuthedName
                            authed = true;
                            lastAuthedName = sUsername;
                        }

                        String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
                        cookieManager.setCookie("wordpress.com", cookieString);
                        CookieSyncManager.getInstance().sync();

                    }
                }

                // Uncomment to review the html returned from the auth call.
//                HttpEntity entity = response.getEntity();
//                InputStream is = entity.getContent();
//                InputStreamReader isr = new InputStreamReader(is);
//                BufferedReader br = new BufferedReader(isr);
//
//                StringBuffer sb = new StringBuffer("");
//                String line = "";
//                String NL = System.getProperty("line.separator");
//                while((line = br.readLine()) != null){
//                    sb.append(line + NL);
//                }
//                br.close();
//
//                String res = sb.toString();
//                Log.d("AuthStatsAsyncTask", res);

            } catch (Exception e) {
                e.printStackTrace();
                errorMsg = e.getMessage();
            }

            return result;
        }


        protected void onPostExecute(List<?> result) {
            currentTask = null;
            stopAnimatingRefreshButton(refreshMenuItem);
            if(authed) {
                loadStats();
            } else {
                // if we're here and not authed then there was either a server error
                // or the auth cookie was not set.
                if(errorMsg == "" || statusCode == 401){
                    // Either there wasn't an error and the auth cookie wasn't set, or
                    // we received a 401 error.
                    Toast.makeText(
                            OldStatsActivity.this,
                            getResources().getText(R.string.invalid_login),
                            Toast.LENGTH_SHORT).show();
                    showLoginForm();
                } else {
                    // server error of some kind.
                    Toast.makeText(
                            OldStatsActivity.this,
                            getResources().getText(R.string.stats_service_error),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /*
     * AsyncTask for retrieving a blog's key and id from the API
     */
    private class StatsAPIBlogInfoAsyncTask extends AsyncTask<String, Void, List<?>> {

        int statusCode = 0;

        @Override
        protected List<?> doInBackground(String... args) {

            String username = args[0];
            String password = args[1];
            String url = WordPress.currentBlog.getUrl();
            String homeURL = WordPress.currentBlog.getHomeURL();
            String storedBlogID = String.valueOf(WordPress.currentBlog.getBlogId());
            String wwwURL = "";
            List<String> apiInfo = null;

            if (homeURL.equals("")) {
                //get the 'homePageLink' url from RSD to match with the stats api
                String homePageLink = ApiHelper.getHomePageLink(url + "?rsd");
                if (homePageLink != null) {
                    url = homePageLink;
                    //home url was added in 2.2.2, it may need to be set if the user upgraded
                    WordPress.currentBlog.setHomeURL(url);
                    WordPress.currentBlog.save("");
                } else {
                    url = url.replace("xmlrpc.php", "");
                }
            } else {
                url = homeURL;
            }

            url = url.replace("https://", "http://");

            if (url.indexOf("http://www.") >= 0) {
                wwwURL = url;
                url = url.replace("http://www.", "http://");
            } else {
                wwwURL = url.replace("http://", "http://www.");
            }

            if (!url.endsWith("/")) {
                url += "/";
                wwwURL += "/";
            }

            URI uri = URI.create("https://public-api.wordpress.com/get-user-blogs/1.0");
            configureClient(uri, username, password);

            // execute HTTP POST request
            HttpResponse response;
            try {
                response = client.execute(postMethod);
                /*
                 * ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                 * response.getEntity().writeTo(outstream); String text =
                 * outstream.toString(); Log.i("WordPress", text);
                 */
                // check status code
                this.statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {

                    throw new IOException("HTTP status code: " + statusCode
                            + " was returned. "
                            + response.getStatusLine().getReasonPhrase());
                }

                // setup pull parser
                try {
                    XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
                    HttpEntity entity = response.getEntity();
                    // change to pushbackinput stream 1/18/2010 to handle self
                    // installed wp sites that insert the BOM
                    PushbackInputStream is = new PushbackInputStream(entity.getContent());

                    // get rid of junk characters before xml response. 60 = '<'.
                    // Added stopper to prevent infinite loop
                    int bomCheck = is.read();
                    int stopper = 0;
                    while (bomCheck != 60 && stopper < 20) {
                        bomCheck = is.read();
                        stopper++;
                    }
                    is.unread(bomCheck);

                    pullParser.setInput(is, "UTF-8");

                    int eventType = pullParser.getEventType();
                    String apiKey = "";
                    boolean foundKey = false;
                    boolean foundID = false;
                    boolean foundURL = false;
                    String curBlogID = "";
                    String curBlogURL = "";
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_DOCUMENT) {
                            // System.out.println("Start document");
                        } else if (eventType == XmlPullParser.END_DOCUMENT) {
                            // System.out.println("End document");
                        } else if (eventType == XmlPullParser.START_TAG) {
                            if (pullParser.getName().equals("apikey")) {
                                foundKey = true;
                            } else if (pullParser.getName().equals("id")) {
                                foundID = true;
                            } else if (pullParser.getName().equals("url")) {
                                foundURL = true;
                            }
                        } else if (eventType == XmlPullParser.END_TAG) {
                            // System.out.println("End tag "+pullParser.getName());
                        } else if (eventType == XmlPullParser.TEXT) {
                            // System.out.println("Text "+pullParser.getText().toString());
                            if (foundKey) {
                                apiKey = pullParser.getText();
                                foundKey = false;
                            } else if (foundID) {
                                curBlogID = pullParser.getText();
                                foundID = false;
                            } else if (foundURL) {
                                curBlogURL = pullParser.getText();
                                foundURL = false;

                                StringMap<?> jetpackClientIDOption = null;
                                try {
                                    Gson gson = new Gson();
                                    Type type = new TypeToken<Map<?, ?>>(){}.getType();
                                    Map<?, ?> blogOptions = gson.fromJson(WordPress.currentBlog.getBlogOptions(), type);
                                    jetpackClientIDOption = blogOptions != null ? (StringMap<?>) blogOptions.get("jetpack_client_id") : null;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if( jetpackClientIDOption != null ) {
                                    //Try to match the ID first. The WordPress.com ID of the Jetpack blog was introduced in options in Jetpack 1.8.3 or higher
                                    if (jetpackClientIDOption.get("value") instanceof Double) {
                                        double clientID = (Double)jetpackClientIDOption.get("value");
                                        String jetpackClientIDString = String.valueOf((long) clientID);
                                        if ( jetpackClientIDString.equals(curBlogID) && !curBlogID.equals("1") ) {
                                            // yay, found a match
                                            apiInfo = new Vector<String>();
                                            apiInfo.add(apiKey);
                                            apiInfo.add(curBlogID);
                                        }
                                    }
                                } else {
                                    // make sure we're matching with a '/' at the end of
                                    // the string, the api returns both with and w/o
                                    if (!curBlogURL.endsWith("/"))
                                        curBlogURL += "/";

                                    if (((curBlogURL.equals(url) || (curBlogURL.equals(wwwURL))) || storedBlogID.equals(curBlogID)) && !curBlogID.equals("1")) {
                                        // yay, found a match
                                        apiInfo = new Vector<String>();
                                        apiInfo.add(apiKey);
                                        apiInfo.add(curBlogID);
                                    }
                                }
                            }
                        }
                        eventType = pullParser.next();
                    }

                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                    errorMsg = e.getMessage();
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                errorMsg = e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                errorMsg = e.getMessage();
            }

            return apiInfo;
        }


        protected void onPostExecute(List<?> result) {
            currentTask = null;
            stopAnimatingRefreshButton(refreshMenuItem);
            if (result != null) {
                // store the api key and blog id
                final String apiKey = result.get(0).toString();
                final String apiBlogID = result.get(1).toString();
                WordPress.currentBlog.setApi_blogid(apiBlogID);
                WordPress.currentBlog.setApi_key(apiKey);
                WordPress.currentBlog.save("");

                if (!isFinishing())
                    authStats();

            } else {
                // Either there was a server error, an auth error,
                // or the blog could not be found among the list of blogs
                // returned by the API.
                if (isRetrying) {
                    if (errorMsg.equals("") || statusCode == 401) {

                        Toast.makeText(
                                OldStatsActivity.this,
                                getResources().getText(R.string.invalid_jp_login),
                                Toast.LENGTH_SHORT).show();

                    } else {

                        Toast.makeText(
                                OldStatsActivity.this,
                                getResources().getText(R.string.invalid_login)
                                        + " "
                                        + getResources().getText(
                                                R.string.site_not_found),
                                Toast.LENGTH_SHORT).show();

                        errorMsg = "";
                        showErrorDialog(
                                getResources().getText(R.string.connection_error).toString(),
                                getResources().getText(R.string.connection_error_occured).toString()
                        );
                    }
                }

                showLoginForm();
                isRetrying = true;
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // disable animation when finishing
        overridePendingTransition(0, 0);
    }
    
    public void startAnimatingRefreshButton(MenuItem refreshItem) {
        if (refreshItem != null && !isAnimatingRefreshButton) {
            isAnimatingRefreshButton = true;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(
                    getResources().getLayout(R.layout.menu_refresh_view), null);
            RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setInterpolator(new LinearInterpolator());
            anim.setRepeatCount(Animation.INFINITE);
            anim.setDuration(1400);
            iv.startAnimation(anim);
            refreshItem.setActionView(iv);
        }
    }

    public void stopAnimatingRefreshButton(MenuItem refreshItem) {
        isAnimatingRefreshButton = false;
        if (refreshItem != null && refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }
}

