package org.wordpress.android.networking;

import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.HttpStack;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * An {@link HttpStack} based on the code of {@link com.android.volley.toolbox.HurlStack} that internally
 * uses a {@link HttpURLConnection}.
 *
 * This implementation of {@link HttpStack} internally initializes {@link SelfSignedSSLCertsManager} in a secondary
 * thread since initialization could take a few seconds.
 */
public class WPDelayedHurlStack implements HttpStack {
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private SSLSocketFactory mSslSocketFactory;
    private final Blog mCurrentBlog;
    private final Context mCtx;
    private final Object monitor = new Object();

    public WPDelayedHurlStack(final Context ctx, final Blog currentBlog) {
        mCurrentBlog = currentBlog;
        mCtx = ctx;

        // initializes SelfSignedSSLCertsManager in a separate thread.
        Thread sslContextInitializer = new Thread() {
            @Override
            public void run() {
                try {
                    TrustManager[] trustAllowedCerts = new TrustManager[]{
                            new WPTrustManager(SelfSignedSSLCertsManager.getInstance(ctx).getLocalKeyStore())
                    };
                    SSLContext context = SSLContext.getInstance("SSL");
                    context.init(null, trustAllowedCerts, new SecureRandom());
                    mSslSocketFactory = context.getSocketFactory();
                } catch (NoSuchAlgorithmException e) {
                    AppLog.e(T.API, e);
                } catch (KeyManagementException e) {
                    AppLog.e(T.API, e);
                } catch (GeneralSecurityException e) {
                    AppLog.e(T.API, e);
                } catch (IOException e) {
                    AppLog.e(T.API, e);
                }
            }
        };
        sslContextInitializer.start();
    }


    private static boolean hasAuthorizationHeader(Request request) {
        try {
            if (request.getHeaders() != null && request.getHeaders().containsKey("Authorization")) {
                return true;
            }
        } catch (AuthFailureError e) {
            // nope
        }

        return false;
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        if (request.getUrl() != null) {
            if (!WPUrlUtils.isWordPressCom(request.getUrl()) && mCurrentBlog != null
                    && mCurrentBlog.hasValidHTTPAuthCredentials()) {
                String creds = String.format("%s:%s", mCurrentBlog.getHttpuser(), mCurrentBlog.getHttppassword());
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                additionalHeaders.put("Authorization", auth);
            }

            /**
             *  Add the Authorization header to access private WP.com files.
             *
             *  Note: Additional headers have precedence over request headers, so add Authorization only it it's not already
             *  available in the request.
             *
             */
            if (WPUrlUtils.safeToAddWordPressComAuthToken(request.getUrl()) && mCtx != null
                    && AccountHelper.isSignedInWordPressDotCom() && !hasAuthorizationHeader(request)) {
                additionalHeaders.put("Authorization", "Bearer " + AccountHelper.getDefaultAccount().getAccessToken());
            }
        }

        additionalHeaders.put("User-Agent", WordPress.getUserAgent());

        String url = request.getUrl();

        // Ensure that an HTTPS request is made to wpcom when Authorization is set.
        if (additionalHeaders.containsKey("Authorization") || hasAuthorizationHeader(request)) {
            url = UrlUtils.makeHttps(url);
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.putAll(request.getHeaders());
        map.putAll(additionalHeaders);

        URL parsedUrl = new URL(url);
        HttpURLConnection connection = openConnection(parsedUrl, request);
        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        setConnectionParametersForRequest(connection, request);
        // Initialize HttpResponse with data from the HttpURLConnection.
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }
        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                connection.getResponseCode(), connection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));
        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }
        return response;
    }

    /**
     * Initializes an {@link HttpEntity} from the given {@link HttpURLConnection}.
     * @param connection
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        // Check that the custom SslSocketFactory is not null on HTTPS connections
        if (UrlUtils.isHttps(url) && !WPUrlUtils.isWordPressCom(url)
                && !WPUrlUtils.isGravatar(url)) {
            // WordPress.com doesn't need the custom mSslSocketFactory
            synchronized (monitor) {
                while (mSslSocketFactory == null) {
                    try {
                        monitor.wait(500);
                    } catch (InterruptedException e) {
                        // we can't do much here.
                    }
                }
            }
        }

        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     * @param url
     * @return an open connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

    @SuppressWarnings("deprecation")
    /* package */ static void setConnectionParametersForRequest(HttpURLConnection connection,
            Request<?> request) throws IOException, AuthFailureError {
        switch (request.getMethod()) {
            case Method.DEPRECATED_GET_OR_POST:
                // This is the deprecated way that needs to be handled for backwards compatibility.
                // If the request's post body is null, then the assumption is that the request is
                // GET.  Otherwise, it is assumed that the request is a POST.
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    // Prepare output. There is no need to set Content-Length explicitly,
                    // since this is handled by HttpURLConnection using the size of the prepared
                    // output stream.
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HEADER_CONTENT_TYPE,
                            request.getPostBodyContentType());
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.write(postBody);
                    out.close();
                }
                break;
            case Method.GET:
                // Not necessary to set the request method because connection defaults to GET but
                // being explicit here.
                connection.setRequestMethod("GET");
                break;
            case Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static void addBodyIfExists(HttpURLConnection connection, Request<?> request)
            throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
}
