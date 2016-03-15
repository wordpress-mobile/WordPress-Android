package org.wordpress.android.ui.accounts.helpers;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;

import org.xmlrpc.android.XMLRPCUtils;
import org.xmlrpc.android.XMLRPCUtils.XMLRPCUtilsException;

import android.os.AsyncTask;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchBlogListWPOrg extends FetchBlogListAbstract {
    private String mSelfHostedUrl;
    private String mHttpUsername;
    private String mHttpPassword;

    public FetchBlogListWPOrg(String username, String password, String selfHostedUrl) {
        super(username, password);
        mSelfHostedUrl = selfHostedUrl;
    }

    public void setHttpCredentials(String username, String password) {
        mHttpUsername = username;
        mHttpPassword = password;
    }

    public void fetchBlogList(Callback callback) {
        (new FetchBlogListTask(callback)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class FetchBlogListTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
        private final Callback mCallback;
        private boolean mHttpAuthRequired;
        private boolean mErroneousSslCertificate;
        private int mErrorMsgId;
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
            try {
                String xmlrpcUrl = XMLRPCUtils.verifyOrDiscoverXmlRpcUrl(mSelfHostedUrl, mHttpUsername, mHttpPassword);

                // The XML-RPC address is now available. Call wp.getUsersBlogs and load the sites.
                return XMLRPCUtils.getUserBlogsList(URI.create(xmlrpcUrl), mUsername, mPassword, mHttpUsername,
                        mHttpPassword);
            } catch (XMLRPCUtilsException hce) {
                mErrorMsgId = hce.errorMsgId;
                mHttpAuthRequired = (hce.kind == XMLRPCUtilsException.Kind.HTTP_AUTH_REQUIRED);
                mErroneousSslCertificate = (hce.kind == XMLRPCUtilsException.Kind.ERRONEOUS_SSL_CERTIFICATE);
                trackInvalidInsertedURL(hce.failedUrl);
                return null;
            }
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
