package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPMeShortlinks;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.util.helpers.WPWebChromeClient;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Activity for opening external WordPress links in a webview.
 *
 * Try to use one of the methods below to open the webview:
 * - openURL
 * - openUrlByUsingMainWPCOMCredentials
 * - openUrlByUsingWPCOMCredentials
 * - openUrlByUsingBlogCredentials (for self hosted sites)
 *
 * If you need to start the activity with delay, start activity with result, or none of the methods above are enough for your needs,
 * you can start the activity by passing the required parameters, depending on what you need to do.
 *
 * 1. Load a simple URL (without any kind of authentication)
 * - Start the activity with the parameter URL_TO_LOAD set to the URL to load.
 *
 * 2. Load a WordPress.com URL
 * Start the activity with the following parameters:
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the WordPress.com authentication endpoint. Please use WPCOM_LOGIN_URL.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 *
 * 3. Load a WordPress.org URL with authentication
 * - URL_TO_LOAD: target URL to load in the webview.
 * - AUTHENTICATION_URL: The address of the authentication endpoint. Please use the value of getBlogLoginUrl()
 * to retrieve the correct address of the authentication endpoint.
 * - AUTHENTICATION_USER: username.
 * - AUTHENTICATION_PASSWD: password.
 * - LOCAL_BLOG_ID: local id of the blog in the app database. This is required since some blogs could have HTTP Auth,
 * or self-signed certs in place.
 *
 */
public class WPWebViewActivity extends WebViewActivity implements DownloadListener {
    public static final String AUTHENTICATION_URL = "authenticated_url";
    public static final String AUTHENTICATION_USER = "authenticated_user";
    public static final String AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String URL_TO_LOAD = "url_to_load";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";
    public static final String LOCAL_BLOG_ID = "local_blog_id";
    public static final String SHARABLE_URL = "sharable_url";
    public static final int EXTERNAL_WRITE_STORAGE_REQUEST_CODE = 1;

    private static final String ENCODING_UTF8 = "UTF-8";

    private DownloadContentHeader mDownloadContentHeader;

    public static void openUrlByUsingWPCOMCredentials(Context context, String url, String user) {
        openWPCOMURL(context, url, user);
    }

    public static void openUrlByUsingBlogCredentials(Context context, Blog blog, Post post, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (blog == null) {
            AppLog.e(AppLog.T.UTILS, "Blog obj is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String authURL = WPWebViewActivity.getBlogLoginUrl(blog);
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, blog.getUsername());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, blog.getPassword());
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        if (post != null) {
            intent.putExtra(WPWebViewActivity.SHARABLE_URL, WPMeShortlinks.getPostShortlink(blog, post));
        }
        context.startActivity(intent);
    }

    public static void openURL(Context context, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        context.startActivity(intent);
    }

    private static void openWPCOMURL(Context context, String url, String user) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openUrlByUsingMainWPCOMCredentials");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(user)) {
            AppLog.e(AppLog.T.UTILS, "Username empty/null");
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, user);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, WPCOM_LOGIN_URL);
        context.startActivity(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            AppLog.e(AppLog.T.UTILS, "No valid parameters passed to WPWebViewActivity");
            finish();
            return;
        }

        if (extras.getInt(LOCAL_BLOG_ID, -1) > -1) {
            Blog blog = WordPress.getBlog(extras.getInt(LOCAL_BLOG_ID, -1));
            if (blog == null) {
                AppLog.e(AppLog.T.UTILS, "No valid parameters passed to WPWebViewActivity");
                finish();
            }
            mWebView.setWebViewClient(new WPWebViewClient(blog));
        } else {
            mWebView.setWebViewClient(new WebViewClient());
        }
        mWebView.setWebChromeClient(new WPWebChromeClient(this, (ProgressBar) findViewById(R.id.progress_bar)));
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setDownloadListener(this);

        String addressToLoad = extras.getString(URL_TO_LOAD);
        String username = extras.getString(AUTHENTICATION_USER, "");
        String password = extras.getString(AUTHENTICATION_PASSWD, "");
        String authURL = extras.getString(AUTHENTICATION_URL);

        if (TextUtils.isEmpty(addressToLoad) || !UrlUtils.isValidUrlAndHostNotNull(addressToLoad)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null or invalid URL passed to WPWebViewActivity");
            Toast.makeText(this, getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        if (TextUtils.isEmpty(authURL) && TextUtils.isEmpty(username) && TextUtils.isEmpty(password)) {
            // Only the URL to load is passed to this activity. Use a the normal loader not authenticated.
            loadUrl(addressToLoad);
        } else {
            if (TextUtils.isEmpty(authURL) || !UrlUtils.isValidUrlAndHostNotNull(authURL)) {
                AppLog.e(AppLog.T.UTILS, "Empty or null or invalid auth URL passed to WPWebViewActivity");
                Toast.makeText(this, getText(R.string.invalid_url_message),
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            if (TextUtils.isEmpty(username)) {
                AppLog.e(AppLog.T.UTILS, "Username empty/null");
                Toast.makeText(this, getText(R.string.incorrect_credentials), Toast.LENGTH_SHORT).show();
                finish();
            }

            this.loadAuthenticatedUrl(authURL, addressToLoad, username, password);
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     *
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String password) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                AccountHelper.getDefaultAccount().getAccessToken());

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    public static String getAuthenticationPostData(String authenticationUrl, String urlToLoad, String username, String password, String token) {
        if (TextUtils.isEmpty(authenticationUrl)) return "";

        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(StringUtils.notNullStr(username), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(password), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(urlToLoad), ENCODING_UTF8)
            );

            // Add token authorization when signing in to WP.com
            if (WPUrlUtils.safeToAddWordPressComAuthToken(authenticationUrl)
                    && authenticationUrl.contains("wordpress.com/wp-login.php") && !TextUtils.isEmpty(token)) {
                postData += "&authorization=Bearer " + URLEncoder.encode(token, ENCODING_UTF8);
            }

            return postData;
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        return "";
    }

    /**
     * Get the URL of the WordPress login page.
     *
     * @return URL of the login page.
     */
    public static String getBlogLoginUrl(Blog blog) {
        String loginURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(blog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("login_url");
            if (homeURLMap != null) {
                loginURL = homeURLMap.get("value").toString();
            }
        }
        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if (loginURL == null) {
            if (blog.getUrl().lastIndexOf("/") != -1) {
                return blog.getUrl().substring(0, blog.getUrl().lastIndexOf("/"))
                        + "/wp-login.php";
            } else {
                return blog.getUrl().replace("xmlrpc.php", "wp-login.php");
            }
        }

        return loginURL;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null) {
            return false;
        }

        int itemID = item.getItemId();
        if (itemID == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        } else if (itemID == R.id.menu_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            // Use the preferred sharable URL or the default webview URL
            Bundle extras = getIntent().getExtras();
            String sharableUrl = extras.getString(SHARABLE_URL, null);
            if (sharableUrl == null) {
                sharableUrl = mWebView.getUrl();
            }
            share.putExtra(Intent.EXTRA_TEXT, sharableUrl);
            startActivity(Intent.createChooser(share, getText(R.string.share_link)));
            return true;
        } else if (itemID == R.id.menu_browser) {
            String url = mWebView.getUrl();
            if (url != null) {
                Uri uri = Uri.parse(url);
                if (uri != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(uri);
                    startActivity(i);
                    AppLockManager.getInstance().setExtendedTimeout();
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (PermissionUtils.checkAndRequestStoragePermission(this, EXTERNAL_WRITE_STORAGE_REQUEST_CODE)) {
//            mDownloadContentHeader = new DownloadContentHeader(url, contentDisposition, mimetype);
//            new DownloadExportFileTask().execute();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.allowScanningByMediaScanner();
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setMimeType(mimetype);
            request.setTitle("yup.xml");
            request.setDescription("WordPress backup");
            request.addRequestHeader("Authentication", "Bearer " + AccountHelper.getDefaultAccount().getAccessToken());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "");
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(this, "Downloading File", //To notify the Client that the file is being downloaded
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_WRITE_STORAGE_REQUEST_CODE:
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        ToastUtils.showToast(this, "Permissions required in order to download export file");
                        return;
                    }
                }

                new DownloadExportFileTask().execute();
                break;
            default:
                break;
        }
    }

    private class DownloadContentHeader {
        String url;
        String contentDisposition;
        String mimeType;

        DownloadContentHeader(String url, String contentDisposition, String mimetype) {
            this.url = url;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimetype;
        }

        public String fileNameFromDisposition() {
            String[] split = contentDisposition.split("filename=");
            if (split.length > 1) {
                return split[1];
            } else {
                return "WordPress.xml";
            }
        }
    }

    private class DownloadExportFileTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
            builder.setSmallIcon(R.drawable.hs__action_download);
            builder.setContentTitle("WordPress");
            builder.setContentTitle("Exporting your WordPress site");

            Notification notification = builder.build();
            Intent resultIntent = new Intent(getApplicationContext(), DownloadManager.class);


            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
            mNotificationManager.notify(0, builder.build());
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL(mDownloadContentHeader.url);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mDownloadContentHeader.fileNameFromDisposition());

                FileOutputStream fileOutput = new FileOutputStream(file);
                InputStream inputStream = urlConnection.getInputStream();

                byte[] buffer = new byte[1024];
                int bufferLength = 0; //used to store a temporary size of the buffer

                //now, read through the input buffer and write the contents to the file
                while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                    //add the data in the buffer to the file in the file output stream (the file on the sd card
                    fileOutput.write(buffer, 0, bufferLength);
                }
                //close the output stream when done
                fileOutput.close();

                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                downloadManager.addCompletedDownload(mDownloadContentHeader.fileNameFromDisposition(), "WordPress.com export file", true, mDownloadContentHeader.mimeType, file.getPath(), file.length(), true);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
