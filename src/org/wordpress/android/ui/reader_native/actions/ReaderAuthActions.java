package org.wordpress.android.ui.reader_native.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.util.ReaderLog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nbradbury on 8/28/13.
 */
public class ReaderAuthActions {
    private static final String URI_LOGIN = "https://wordpress.com/wp-login.php";
    private static final int HTTPS_PORT = 443;

    /*
     * login to WP using a DefaultHttpClient so we can capture the response cookies and add them to
     * the CookieSyncManager so they'll be available in our webView on post detail - only needs to
     * be done once per session
     */
    public static void updateCookies(Context context) {
        // http://developer.android.com/reference/android/webkit/CookieSyncManager.html
        CookieSyncManager.createInstance(context.getApplicationContext());
        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        // nothing more to do if login doesn't exist yet
        if (!WordPress.hasValidWPComCredentials(context))
            return;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, "");
        final String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, ""));

        new Thread() {
            @Override
            public void run() {
                try {
                    URI uri = URI.create(URI_LOGIN);
                    HttpPost postMethod = new HttpPost(uri);
                    postMethod.addHeader("charset", "UTF-8");
                    postMethod.addHeader("User-Agent", Constants.USER_AGENT);

                    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);

                    DefaultHttpClient client = getHttpClient(creds);

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                    nameValuePairs.add(new BasicNameValuePair("log", username));
                    nameValuePairs.add(new BasicNameValuePair("pwd", password));
                    nameValuePairs.add(new BasicNameValuePair("rememberme", "forever"));
                    nameValuePairs.add(new BasicNameValuePair("wp-submit", "Log In"));
                    nameValuePairs.add(new BasicNameValuePair("redirect_to", "/"));
                    postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

                    HttpResponse response = client.execute(postMethod);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        ReaderLog.w(String.format("failed to retrieve cookies, status %d", statusCode));
                        return;
                    }

                    List<Cookie> cookies = client.getCookieStore().getCookies();
                    if(!cookies.isEmpty()) {
                        for (Cookie cookie : cookies){
                            String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
                            cookieManager.setCookie("wordpress.com", cookieString);
                        }
                        CookieSyncManager.getInstance().sync();
                    }

                } catch (UnsupportedEncodingException e) {
                    ReaderLog.e(e);
                } catch (IOException e) {
                    ReaderLog.e(e);
                }
            }
        }.start();
     }

    private static DefaultHttpClient getHttpClient(UsernamePasswordCredentials creds) {
        DefaultHttpClient client = new DefaultHttpClient();

        BasicCredentialsProvider cP = new BasicCredentialsProvider();
        cP.setCredentials(AuthScope.ANY, creds);
        client.setCredentialsProvider(cP);
        client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", SSLSocketFactory.getSocketFactory(), HTTPS_PORT));

        return client;
    }
}
