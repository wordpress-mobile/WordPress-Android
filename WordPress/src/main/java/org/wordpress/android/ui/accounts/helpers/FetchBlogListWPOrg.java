package org.wordpress.android.ui.accounts.helpers;

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
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
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

    private void getBlogList(URI xmlrpcUri, final Callback callback) {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(xmlrpcUri, mHttpUsername, mHttpPassword);
        Object[] params = {mUsername, mPassword};
        try {
            Object[] userBlogs = (Object[]) client.call("wp.getUsersBlogs", params);
            if (userBlogs == null) {
                // Could happen if the returned server response is truncated
                mErrorMsgId = org.wordpress.android.R.string.xmlrpc_error;
                callback.onError(mErrorMsgId, false, false, false, client.getResponse());
                return;
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
            callback.onSuccess(userBlogList);
            return;
        } catch (XmlPullParserException parserException) {
            mErrorMsgId = org.wordpress.android.R.string.xmlrpc_error;
            AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs", parserException);
        } catch (XMLRPCFault xmlRpcFault) {
            handleXmlRpcFault(xmlRpcFault);
        } catch (XMLRPCException xmlRpcException) {
            AppLog.e(T.NUX, "XMLRPCException received from XMLRPC call wp.getUsersBlogs", xmlRpcException);
            mErrorMsgId = org.wordpress.android.R.string.no_site_error;
        } catch (SSLHandshakeException e) {
            if (xmlrpcUri.getHost().endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
        } catch (IOException e) {
            AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
            mErrorMsgId = org.wordpress.android.R.string.no_site_error;
        }
        callback.onError(mErrorMsgId, false, mHttpAuthRequired, mErroneousSslCertificate, client.getResponse());
    }

    public void fetchBlogList(Callback callback) {
        String xmlrpcUrl = null;
        if (mSelfHostedUrl != null && mSelfHostedUrl.length() != 0) {
            xmlrpcUrl = getSelfHostedXmlrpcUrl(mSelfHostedUrl);
        }

        if (xmlrpcUrl == null) {
            if (!mHttpAuthRequired && mErrorMsgId == 0) {
                mErrorMsgId = org.wordpress.android.R.string.no_site_error;
            }
            callback.onError(mErrorMsgId, false, mHttpAuthRequired, mErroneousSslCertificate, "");
            return;
        }

        // Validate the URL found before calling the client. Prevent a crash that can occur
        // during the setup of self-hosted sites.
        URI xmlrpcUri;
        xmlrpcUri = URI.create(xmlrpcUrl);
        getBlogList(xmlrpcUri, callback);
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
            mHttpAuthRequired = true;
            return true;
        }
        return false;
    }

    private String getXmlrpcByUserEnteredPath(String baseUrl) {
        String xmlRpcUrl;
        if (!UrlUtils.isValidUrlAndHostNotNull(baseUrl)) {
            AppLog.e(T.NUX, "invalid URL: " + baseUrl);
            mErrorMsgId = org.wordpress.android.R.string.invalid_url_message;
            return null;
        }
        URI uri = URI.create(baseUrl);
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, mHttpUsername, mHttpPassword);
        try {
            client.call("system.listMethods");
            xmlRpcUrl = baseUrl;
            return xmlRpcUrl;
        } catch (XMLRPCException e) {
            AppLog.i(T.NUX, "system.listMethods failed on: " + baseUrl);
            if (isHTTPAuthErrorMessage(e)) {
                return null;
            }
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (SSLPeerUnverifiedException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (IOException e) {
            AppLog.i(T.NUX, "system.listMethods failed on: " + baseUrl);
            if (isHTTPAuthErrorMessage(e)) {
                return null;
            }
        } catch (XmlPullParserException e) {
            AppLog.i(T.NUX, "system.listMethods failed on: " + baseUrl);
            if (isHTTPAuthErrorMessage(e)) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            // TODO: Hopefully a temporary log - remove it if we find a pattern of failing URLs
            CrashlyticsUtils.setString(ExtraKey.ENTERED_URL, baseUrl);
            CrashlyticsUtils.logException(e, ExceptionType.SPECIFIC, T.NUX);
            mErrorMsgId = org.wordpress.android.R.string.invalid_url_message;
            return null;
        }

        // Guess the xmlrpc path
        String guessURL = baseUrl;
        if (guessURL.substring(guessURL.length() - 1, guessURL.length()).equals("/")) {
            guessURL = guessURL.substring(0, guessURL.length() - 1);
        }
        guessURL += "/xmlrpc.php";
        uri = URI.create(guessURL);
        client = XMLRPCFactory.instantiate(uri, mHttpUsername, mHttpPassword);
        try {
            client.call("system.listMethods");
            xmlRpcUrl = guessURL;
            return xmlRpcUrl;
        } catch (XMLRPCException e) {
            AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(T.NUX, "system.listMethods failed on: " + guessURL, e);
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (SSLPeerUnverifiedException e) {
            if (!UrlUtils.getDomainFromUrl(baseUrl).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLPeerUnverifiedException failed. Erroneous SSL certificate detected.");
            return null;
        } catch (IOException e) {
            AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(T.NUX, "system.listMethods failed on: " + guessURL, e);
        } catch (XmlPullParserException e) {
            AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(T.NUX, "system.listMethods failed on: " + guessURL, e);
        }

        return null;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site, in this order:
    // 1: Try to retrieve it by finding the ?rsd url in the site's header
    // 2: Take whatever URL the user entered to see if that returns a correct response
    // 3: Finally, just guess as to what the xmlrpc url should be
    private String getSelfHostedXmlrpcUrl(String url) {
        String xmlrpcUrl;

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(url, false);

        if (!URLUtil.isValidUrl(url)) {
            mErrorMsgId = org.wordpress.android.R.string.invalid_url_message;
            return null;
        }

        // Attempt to get the XMLRPC URL via RSD
        String rsdUrl;
        try {
            rsdUrl = UrlUtils.addUrlSchemeIfNeeded(getRsdUrl(url), false);
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(url).endsWith("wordpress.com")) {
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
            } else {
                xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXmlrpcByUserEnteredPath(url), false);
            }
        } catch (SSLHandshakeException e) {
            if (!UrlUtils.getDomainFromUrl(url).endsWith("wordpress.com")) {
                mErroneousSslCertificate = true;
            }
            AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            return null;
        }

        return xmlrpcUrl;
    }
}
