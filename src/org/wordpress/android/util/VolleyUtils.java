package org.wordpress.android.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.WPTrustManager;
import org.wordpress.android.util.AppLog.T;

/**
 * Created by nbradbury on 9/3/13.
 */
public class VolleyUtils {

    /*
     * returns REST API error string from the response in the passed VolleyError
     * for example, returns "already_subscribed" from this response:
     *  {
	 *      "error": "already_subscribed",
	 *      "message": "You are already subscribed to the specified topic."
	 *  }
     */
    public static String errStringFromVolleyError(VolleyError volleyError) {
        JSONObject json = volleyErrorToJSON(volleyError);
        if (json==null)
            return "";
        return JSONUtil.getString(json, "error");
    }

    /*
     * attempts to return JSON from a volleyError - useful for WP REST API failures, which often
     * contain JSON in the response
     */
    public static JSONObject volleyErrorToJSON(VolleyError volleyError) {
        if (volleyError==null
                || volleyError.networkResponse==null
                || volleyError.networkResponse.data==null
                || volleyError.networkResponse.headers==null)
            return null;

        String contentType = volleyError.networkResponse.headers.get("Content-Type");
        if (contentType==null || !contentType.equals("application/json"))
            return null;

        try {
            String response = new String(volleyError.networkResponse.data, "UTF-8");
            JSONObject json = new JSONObject(response);
            return json;
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    /*
     * cancel all Volley requests that aren't for images
     */
    public static void cancelAllNonImageRequests(RequestQueue requestQueue) {
        if (requestQueue==null)
            return;
        RequestQueue.RequestFilter filter = new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                if (request instanceof ImageRequest)
                    return false;
                return true;
            }
        };
        requestQueue.cancelAll(filter);
    }

    /*
     * cancel all Volley requests
     */
    public static void cancelAllRequests(RequestQueue requestQueue) {
        if (requestQueue==null)
            return;
        RequestQueue.RequestFilter filter = new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        };
        requestQueue.cancelAll(filter);
    }

    /*
     * Return true if the blog is protected with HTTP Basic Auth 
     */
    public static boolean isCustomHTTPClientStackNeeded(Blog currentBlog) {
        if (currentBlog.hasValidHTTPAuthCredentials())
            return true;
        
        return false;
    }
    
    private static void addDefaultHeaders(Request<?> request, Map<String, String> headers, Blog blog){
        if (request.getUrl() != null && !StringUtils.getHost(request.getUrl()).endsWith("wordpress.com") 
                && blog != null && blog.hasValidHTTPAuthCredentials()) {
            HashMap<String, String> authParams = new HashMap<String, String>();
            String creds = String.format("%s:%s", blog.getHttpuser(), blog.getHttppassword());
            String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
            authParams.put("Authorization", auth);
            headers.putAll(authParams);
        }

        HashMap<String, String> defaultHeaders = new HashMap<String, String>();
        defaultHeaders.put("User-Agent", WordPress.getUserAgent());
        headers.putAll(defaultHeaders);
    }
    
    
    public static HttpStack getCustomHTTPClientStack(final Blog currentBlog) {
        SSLSocketFactory mSslSocketFactory = null;
        try {
            TrustManager[] trustAllowedCerts = new TrustManager[]{ new WPTrustManager() };
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, trustAllowedCerts, new SecureRandom());
            mSslSocketFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            AppLog.e(T.API, e);
        } catch (KeyManagementException e) {
            AppLog.e(T.API, e);
        }

        HurlStack stack = new HurlStack(null, mSslSocketFactory) {
            @Override
            public HttpResponse performRequest(Request<?> request, Map<String, String> headers)
                    throws IOException, AuthFailureError {
                addDefaultHeaders(request, headers, currentBlog);
                return super.performRequest(request, headers);
            }
        };

        return stack;
    }
    
    public static HttpStack getDefaultHTTPClientStack(final Context ctx) {
        SSLSocketFactory mSslSocketFactory = null;
        try {
            TrustManager[] trustAllowedCerts = new TrustManager[]{ new WPTrustManager() };
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, trustAllowedCerts, new SecureRandom());
            mSslSocketFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            AppLog.e(T.API, e);
        } catch (KeyManagementException e) {
            AppLog.e(T.API, e);
        }

        HurlStack stack = new HurlStack(null, mSslSocketFactory) {
            @Override
            public HttpResponse performRequest(Request<?> request, Map<String, String> headers)
                    throws IOException, AuthFailureError {
                addDefaultHeaders(request, headers, null);

                if (request.getUrl() != null && StringUtils.getHost(request.getUrl()).endsWith("files.wordpress.com") && WordPress.getWPComAuthToken(ctx) != null) {
                    // Add the auth header to access private WP.com files
                    HashMap<String, String> authParams = new HashMap<String, String>();
                    authParams.put("Authorization", "Bearer " + WordPress.getWPComAuthToken(ctx));
                    headers.putAll(authParams);
                }

                return super.performRequest(request, headers);
            }
        };

        return stack;
    }

}
