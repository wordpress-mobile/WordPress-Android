package org.wordpress.android.ui.accounts.helpers;

import android.os.AsyncTask;
import android.webkit.URLUtil;

import org.apache.http.conn.ConnectTimeoutException;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.HealthCheckUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

public class FetchBlogListWPOrg extends FetchBlogListAbstract {
    private String mSelfHostedUrl;
    private String mHttpUsername;
    private String mHttpPassword;
    private boolean mHttpAuthRequired;
    private boolean mErroneousSslCertificate;
    private int mErrorMsgId;

    public FetchBlogListWPOrg(String username, String password, String selfHostedUrl) {
        super(username, password);
        mSelfHostedUrl = selfHostedUrl;
    }

    public void setHttpCredentials(String username, String password) {
        mHttpUsername = username;
        mHttpPassword = password;
    }

    private void handleXmlRpcFault(XMLRPCFault xmlRpcFault) {
        AppLog.e(T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
        switch (xmlRpcFault.getFaultCode()) {
            case 403:
                mErrorMsgId = org.wordpress.android.R.string.username_or_password_incorrect;
                break;
            case 404:
                mErrorMsgId = org.wordpress.android.R.string.xmlrpc_error;
                break;
            case 425:
                mErrorMsgId = org.wordpress.android.R.string.account_two_step_auth_enabled;
                break;
            default:
                mErrorMsgId = org.wordpress.android.R.string.no_site_error;
                break;
        }
    }

    public void fetchBlogList(Callback callback) {
        (new FetchBlogListTask(callback)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class FetchBlogListTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
        private final Callback mCallback;
        private String mClientResponse = "";

        public FetchBlogListTask(Callback callback) {
            mCallback = callback;
        }

        private void trackInvalidInsertedURL(String url){
            Map<String, Object> properties = new HashMap<>();
            properties.put("user_inserted_url", url);
            AnalyticsTracker.track(Stat.LOGIN_INSERTED_INVALID_URL, properties);
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Void... notUsed) {
            String baseUrl;
            try {
                baseUrl = HealthCheckUtils.canonicalizeSiteUrl(mSelfHostedUrl);
            } catch (HealthCheckUtils.HealthCheckException hce) {
                mErrorMsgId = hce.errorMsgId;
                mHttpAuthRequired = (hce.kind == HealthCheckUtils.HealthCheckException.Kind.HTTP_AUTH_REQUIRED);
                mErroneousSslCertificate = (hce.kind == HealthCheckUtils.HealthCheckException.Kind
                        .ERRONEOUS_SSL_CERTIFICATE);
                trackInvalidInsertedURL(hce.failedUrl);
                return null;
            }

            //Retrieve the XML-RPC Endpoint address
            String xmlrpcUrl;
            try {
                xmlrpcUrl = HealthCheckUtils.getSelfHostedXmlrpcUrl(baseUrl, mHttpUsername, mHttpPassword);
            } catch (HealthCheckUtils.HealthCheckException hce) {
                mErrorMsgId = hce.errorMsgId;
                mHttpAuthRequired = (hce.kind == HealthCheckUtils.HealthCheckException.Kind.HTTP_AUTH_REQUIRED);
                mErroneousSslCertificate = (hce.kind == HealthCheckUtils.HealthCheckException.Kind
                        .ERRONEOUS_SSL_CERTIFICATE);
                return null;
            }

            // Validate the XML-RPC URL we've found before. This check prevents a crash that can occur
            // during the setup of self-hosted sites that have malformed xmlrpc URLs in their declaration.
            if (!URLUtil.isValidUrl(xmlrpcUrl)) {
                mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
                trackInvalidInsertedURL(xmlrpcUrl);
                return null;
            }

            // The XML-RPC address is now available. Call wp.getUsersBlogs and load the sites.
            URI xmlrpcUri;
            xmlrpcUri = URI.create(xmlrpcUrl);
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(xmlrpcUri, mHttpUsername, mHttpPassword);
            Object[] params = {mUsername, mPassword};
            try {
                Object[] userBlogs = (Object[]) client.call(Method.GET_BLOGS, params);
                if (userBlogs == null) {
                    // Could happen if the returned server response is truncated
                    mErrorMsgId = org.wordpress.android.R.string.xmlrpc_malformed_response_error;
                    mClientResponse = client.getResponse();
                    return null;
                }
                Arrays.sort(userBlogs, BlogUtils.BlogNameComparator);
                List<Map<String, Object>> userBlogList = new ArrayList<Map<String, Object>>();
                for (Object blog : userBlogs) {
                    try {
                        userBlogList.add((Map<String, Object>) blog);
                    } catch (ClassCastException e) {
                        AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs");
                    }
                }
                return userBlogList;
            } catch (XmlPullParserException parserException) {
                mErrorMsgId = org.wordpress.android.R.string.xmlrpc_error;
                AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs", parserException);
            } catch (XMLRPCFault xmlRpcFault) {
                handleXmlRpcFault(xmlRpcFault);
            } catch (XMLRPCException xmlRpcException) {
                AppLog.e(T.NUX, "XMLRPCException received from XMLRPC call wp.getUsersBlogs", xmlRpcException);
                mErrorMsgId = org.wordpress.android.R.string.no_site_error;
            } catch (SSLHandshakeException e) {
                if (WPUrlUtils.isWordPressCom(xmlrpcUri)) {
                    mErroneousSslCertificate = true;
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            } catch (ConnectTimeoutException e) {
                AppLog.e(T.NUX, "Timeout exception when calling wp.getUsersBlogs", e);
                mErrorMsgId = org.wordpress.android.R.string.site_timeout_error;
            } catch (IOException e) {
                AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
                mErrorMsgId = org.wordpress.android.R.string.no_site_error;
            }
            mClientResponse = client.getResponse();
            return null;
        }

        protected void onPostExecute(List<Map<String, Object>> userBlogList) {
            if (userBlogList == null) {
                mCallback.onError(mErrorMsgId, false, mHttpAuthRequired, mErroneousSslCertificate, mClientResponse);
            } else {
                mCallback.onSuccess(userBlogList);
            }
        }

        protected void onCancelled() {
            mCallback.onError(mErrorMsgId, false, mHttpAuthRequired, mErroneousSslCertificate, mClientResponse);
        }
    }
}
