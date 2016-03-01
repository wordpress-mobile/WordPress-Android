package org.wordpress.android.ui.accounts.helpers;

import android.os.AsyncTask;
import android.webkit.URLUtil;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.CrashlyticsUtils.ExceptionType;
import org.wordpress.android.util.CrashlyticsUtils.ExtraKey;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

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

    private String getRsdUrl(String baseUrl) throws SSLHandshakeException {
        String rsdUrl;
        rsdUrl = ApiHelper.getRSDMetaTagHrefRegEx(baseUrl);
        if (rsdUrl == null) {
            rsdUrl = ApiHelper.getRSDMetaTagHref(baseUrl);
        }
        return rsdUrl;
    }

    private boolean isHTTPAuthErrorMessage(Exception e) {
        if (e != null && e.getMessage() != null && e.getMessage().contains("401")) {
            return true;
        }
        return false;
    }

    private Object doSystemListMethodsXMLRPC(String url) throws XMLRPCException, IOException, XmlPullParserException {
        if (!UrlUtils.isValidUrlAndHostNotNull(url)) {
            AppLog.e(T.NUX, "invalid URL: " + url);
            mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
            return null;
        }
        URI uri = URI.create(url);
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, mHttpUsername, mHttpPassword);
        return client.call(Method.LIST_METHODS);
    }

    private boolean validateListMethodsResponse(Object[] availableMethods) {
        if (availableMethods == null) {
            AppLog.e(T.NUX, "The response of system.listMethods was empty! All required methods are missing on the server.");
            return false;
        }
        // validate xmlrpc methods
        String[] requiredMethods =  { "wp.getUsersBlogs", "wp.getPage", "wp.getCommentStatusList", "wp.newComment",
                "wp.editComment", "wp.deleteComment", "wp.getComments",	"wp.getComment", "wp.setOptions",
                "wp.getOptions", "wp.getPageTemplates", "wp.getPageStatusList", "wp.getPostStatusList",
                "wp.getCommentCount", "wp.uploadFile", "wp.suggestCategories", "wp.deleteCategory", "wp.newCategory",
                "wp.getTags", "wp.getCategories", "wp.getAuthors", "wp.getPageList", "wp.editPage", "wp.deletePage",
                "wp.newPage", "wp.getPages", "mt.publishPost", "mt.getTrackbackPings",
                "mt.supportedTextFilters", "mt.supportedMethods", "mt.setPostCategories", "mt.getPostCategories",
                "mt.getRecentPostTitles", "mt.getCategoryList", "metaWeblog.getUsersBlogs",
                "metaWeblog.deletePost", "metaWeblog.newMediaObject", "metaWeblog.getCategories",
                "metaWeblog.getRecentPosts", "metaWeblog.getPost", "metaWeblog.editPost", "metaWeblog.newPost",
                "blogger.deletePost", "blogger.editPost", "blogger.newPost",
                "blogger.getRecentPosts", "blogger.getPost", "blogger.getUserInfo", "blogger.getUsersBlogs" };

        for (String currentRequiredMethod: requiredMethods) {
            boolean match = false;
            for (Object currentAvailableMethod: availableMethods) {
                if ((currentAvailableMethod).equals(currentRequiredMethod)) {
                    match = true;
                    continue;
                }
            }

            if (!match) {
                AppLog.e(T.NUX, "The following XML-RPC method: " + currentRequiredMethod + " is missing on the server.");
                return false;
            }
        }
        return true;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site. See diagrams here XXX for details
    private String getSelfHostedXmlrpcUrl(String url) {
        String xmlrpcUrl = null;

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(url, false);

        if (!URLUtil.isValidUrl(url)) {
            mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
            // TODO: Bump analytics here?
            return null;
        }

        // Remove wp-login.php if available in the URL
        if (url.contains("wp-login.php")) {
            url = url.substring(0, url.indexOf("wp-login.php"));
        }

        // 1. Create an array of Strings that contains the URLs we want to try in the first step. No discovery ;)
        String[] urlToTry;
        // Append "xmlrpc.php" if missing in the URL
        if (!url.endsWith("xmlrpc.php")) {
            String guessedEndpointURL = url;
            if (guessedEndpointURL.substring(guessedEndpointURL.length() - 1, guessedEndpointURL.length()).equals("/")) {
                guessedEndpointURL = guessedEndpointURL.substring(0, guessedEndpointURL.length() - 1);
            }
            guessedEndpointURL += "/xmlrpc.php";
            urlToTry = new String[2];
            urlToTry[0] = guessedEndpointURL;
            urlToTry[1] = url;
        } else {
            urlToTry = new String[1];
            urlToTry[0] = url;
        }

        for (String currentURL: urlToTry) {
            try {
                Object[] methods = (Object[]) doSystemListMethodsXMLRPC(currentURL);
                if (validateListMethodsResponse(methods)) {
                    xmlrpcUrl = currentURL;
                    // Exit the loop if the endpoint is found!
                    break;
                } else {
                    mErrorMsgId = org.wordpress.android.R.string.xmlrpc_missing_method_error;
                }
            } catch (XMLRPCException e) {
                AppLog.i(T.NUX, "system.listMethods failed on: " + currentURL);
                if (isHTTPAuthErrorMessage(e)) {
                    mHttpAuthRequired = true;
                    return null;
                }
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(currentURL)) {
                    mErroneousSslCertificate = true;
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                return null;
            } catch (SSLPeerUnverifiedException e) {
                if (!WPUrlUtils.isWordPressCom(currentURL)) {
                    mErroneousSslCertificate = true;
                }
                AppLog.w(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected.");
                return null;
            } catch (IOException e) {
                AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
                AppLog.i(T.NUX, "system.listMethods failed on: " + currentURL);
                if (isHTTPAuthErrorMessage(e)) {
                    mHttpAuthRequired = true;
                    return null;
                }
            } catch (XmlPullParserException e) {
                AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
                AppLog.i(T.NUX, "system.listMethods failed on: " + currentURL);
                if (isHTTPAuthErrorMessage(e)) {
                    mHttpAuthRequired = true;
                    return null;
                }
            } catch (IllegalArgumentException e) {
                // TODO: Hopefully a temporary log - remove it if we find a pattern of failing URLs
                CrashlyticsUtils.setString(ExtraKey.ENTERED_URL, currentURL);
                CrashlyticsUtils.logException(e, ExceptionType.SPECIFIC, T.NUX);
                mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
                return null;
            }
        }

        if (xmlrpcUrl == null) {
            // Attempt to get the XMLRPC URL via RSD if the xmlrpc URL is still null here.
            String rsdUrl;
            try {
                rsdUrl = UrlUtils.addUrlSchemeIfNeeded(getRsdUrl(url), false);
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(url)) {
                    mErroneousSslCertificate = true;
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                return null;
            }

            try {
                if (rsdUrl != null) {
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(ApiHelper.getXMLRPCUrl(rsdUrl), false);
                    if (xmlrpcUrl == null) {
                        xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(rsdUrl.replace("?rsd", ""), false);
                    }
                }
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(url)) {
                    mErroneousSslCertificate = true;
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                return null;
            }

        }
        return xmlrpcUrl;
    }

    public class FetchBlogListTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
        private final Callback mCallback;
        private String mClientResponse = "";

        public FetchBlogListTask(Callback callback) {
            mCallback = callback;
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Void... notUsed) {
            String xmlrpcUrl = null;
            if (mSelfHostedUrl != null && mSelfHostedUrl.length() != 0) {
                xmlrpcUrl = getSelfHostedXmlrpcUrl(mSelfHostedUrl);
            }

            if (xmlrpcUrl == null) {
                if (!mHttpAuthRequired && mErrorMsgId == 0) {
                    mErrorMsgId = org.wordpress.android.R.string.no_site_error;
                }
                return null;
            }

            // Validate the URL found before calling the client. Prevent a crash that can occur
            // during the setup of self-hosted sites.
            URI xmlrpcUri;
            xmlrpcUri = URI.create(xmlrpcUrl);
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(xmlrpcUri, mHttpUsername, mHttpPassword);
            Object[] params = {mUsername, mPassword};
            try {
                Object[] userBlogs = (Object[]) client.call(Method.GET_BLOGS, params);
                if (userBlogs == null) {
                    // Could happen if the returned server response is truncated
                    mErrorMsgId = org.wordpress.android.R.string.xmlrpc_error;
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
