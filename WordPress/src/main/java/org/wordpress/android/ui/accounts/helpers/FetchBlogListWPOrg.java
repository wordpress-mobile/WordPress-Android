package org.wordpress.android.ui.accounts.helpers;

import com.android.volley.TimeoutError;

import org.apache.http.conn.ConnectTimeoutException;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListWPOrg.WPOrgUtilsException.Kind;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import android.os.AsyncTask;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Xml;
import android.webkit.URLUtil;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

public class FetchBlogListWPOrg extends FetchBlogListAbstract {
    private String mSelfHostedUrl;
    private String mHttpUsername;
    private String mHttpPassword;

    public static class WPOrgUtilsException extends Exception {
        public enum Kind {
            SITE_URL_CANNOT_BE_EMPTY,
            INVALID_URL,
            MISSING_XMLRPC_METHOD,
            ERRONEOUS_SSL_CERTIFICATE,
            HTTP_AUTH_REQUIRED,
            SITE_TIME_OUT,
            NO_SITE_ERROR,
            XMLRPC_MALFORMED_RESPONSE,
            XMLRPC_ERROR
        }

        public final Kind kind;
        public final
        @StringRes
        int errorMsgId;
        public final String failedUrl;
        public final String clientResponse;

        public WPOrgUtilsException(Kind kind, @StringRes int errorMsgId, String failedUrl, String clientResponse) {
            this.kind = kind;
            this.errorMsgId = errorMsgId;
            this.failedUrl = failedUrl;
            this.clientResponse = clientResponse;
        }
    }

    public FetchBlogListWPOrg(String username, String password, String selfHostedUrl) {
        super(username, password);
        mSelfHostedUrl = selfHostedUrl;
    }

    public void setHttpCredentials(String username, String password) {
        mHttpUsername = username;
        mHttpPassword = password;
    }

    private @StringRes int handleXmlRpcFault(XMLRPCFault xmlRpcFault) {
        AppLog.e(T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
        switch (xmlRpcFault.getFaultCode()) {
            case 403:
                return org.wordpress.android.R.string.username_or_password_incorrect;
            case 404:
                return org.wordpress.android.R.string.xmlrpc_error;
            case 425:
                return org.wordpress.android.R.string.account_two_step_auth_enabled;
            default:
                return org.wordpress.android.R.string.no_site_error;
        }
    }

    public void fetchBlogList(Callback callback) {
        (new FetchBlogListTask(callback)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean isHTTPAuthErrorMessage(Exception e) {
        return e != null && e.getMessage() != null && e.getMessage().contains("401");
    }

    private Object doSystemListMethodsXMLRPC(String url, String httpUsername, String httpPassword) throws
            XMLRPCException, IOException, XmlPullParserException, WPOrgUtilsException {
        if (!UrlUtils.isValidUrlAndHostNotNull(url)) {
            AppLog.e(AppLog.T.NUX, "invalid URL: " + url);
            throw new WPOrgUtilsException(WPOrgUtilsException.Kind.INVALID_URL, org.wordpress.android.R.string
                    .invalid_site_url_message, url, null);
        }

        AppLog.i(T.NUX, "Trying system.listMethods on the following URL: " + url);
        URI uri = URI.create(url);
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, httpUsername, httpPassword);
        return client.call(ApiHelper.Method.LIST_METHODS);
    }

    private static boolean validateListMethodsResponse(Object[] availableMethods) {
        if (availableMethods == null) {
            AppLog.e(T.NUX, "The response of system.listMethods was empty!");
            return false;
        }
        // validate xmlrpc methods
        String[] requiredMethods = {"wp.getUsersBlogs", "wp.getPage", "wp.getCommentStatusList", "wp.newComment",
                "wp.editComment", "wp.deleteComment", "wp.getComments", "wp.getComment",
                "wp.getOptions", "wp.uploadFile", "wp.newCategory",
                "wp.getTags", "wp.getCategories", "wp.editPage", "wp.deletePage",
                "wp.newPage", "wp.getPages"};

        for (String currentRequiredMethod : requiredMethods) {
            boolean match = false;
            for (Object currentAvailableMethod : availableMethods) {
                if ((currentAvailableMethod).equals(currentRequiredMethod)) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                AppLog.e(T.NUX, "The following XML-RPC method: " + currentRequiredMethod + " is missing on the" +
                        " server.");
                return false;
            }
        }
        return true;
    }

    // Append "xmlrpc.php" if missing in the URL
    private static String appendXMLRPCPath(String url) throws IllegalArgumentException {
        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("Input URL " + url + " is not valid!");
        }

        if (!url.contains("xmlrpc.php")) { // Do not use 'ends' here! Some hosting wants parameters passed to
            // baseURL/xmlrpc-php?my-authcode=XXX
            if (url.charAt(url.length() - 1) == '/') {
                url = url.substring(0, url.length() - 1);
            }
            url += "/xmlrpc.php";
            if (!URLUtil.isValidUrl(url)) {
                throw new IllegalArgumentException("The new URL " + url + " is not valid!");
            }
        }
        return url;
    }

    private String truncateURLAtPrefix(String url, String prefix) throws IllegalArgumentException {
        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("Input URL " + url + " is not valid!");
        }
        if (TextUtils.isEmpty(prefix)) {
            throw new IllegalArgumentException("Input prefix is empty or null");
        }

        if (url.indexOf(prefix) > 0) {
            url = url.substring(0, url.indexOf(prefix));
        }

        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("The new URL " + url + " is not valid!");
        }

        return url;
    }

    private ArrayList<String> smartURLCleanerForXMLRPCCalls(String url) {
        String sanitizedURL = url;
        try {
            // Remove 'wp-login.php' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "wp-login.php");
            // Remove '/wp-admin' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-admin");
            // Remove '/wp-content' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-content");
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/xmlrpc.php?rsd");
            while (sanitizedURL.endsWith("/")) {
                sanitizedURL = sanitizedURL.substring(0, sanitizedURL.length() - 1);
            }
        } catch (IllegalArgumentException e) {
            AppLog.e(T.NUX, "Can't clean the original url: " + sanitizedURL, e);
        }

        ArrayList<String> urlsToTry = new ArrayList<>();
        // Append "xmlrpc.php" if missing in the URL
        try {
            urlsToTry.add(appendXMLRPCPath(sanitizedURL));
        } catch (IllegalArgumentException e) {
            AppLog.e(T.NUX, "Can't append 'xmlrpc.php' to the original url: " + url, e);
        }

        // add the sanitized URL without the '/xmlrpc.php' prefix added to it
        if (!urlsToTry.contains(sanitizedURL)) {
            urlsToTry.add(sanitizedURL);
        }

        // add the original URL
        if (!urlsToTry.contains(url)) {
            urlsToTry.add(url);
        }

        return urlsToTry;
    }

    private ArrayList<String> smartURLCleanerForRSD(String url) {
        ArrayList<String> urlsToTry = new ArrayList<>();
        urlsToTry.add(url);

        String sanitizedURL = url;
        try {
            // Remove 'wp-login.php' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "wp-login.php");
            // Remove '/wp-admin' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-admin");
            // Remove '/wp-content' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-content");
        } catch (IllegalArgumentException e) {
            AppLog.e(T.NUX, "Can't clean the original url: " + sanitizedURL, e);
        }

        if (!urlsToTry.contains(sanitizedURL)) {
            urlsToTry.add(sanitizedURL);
        }

        try {
            String xmlrpcURL = appendXMLRPCPath(sanitizedURL);
            if (!urlsToTry.contains(xmlrpcURL)) {
                xmlrpcURL += "?rsd";
                urlsToTry.add(xmlrpcURL);
            }
        } catch (IllegalArgumentException e) {
            AppLog.e(T.NUX, "Can't append 'xmlrpc.php' to the original url: " + url, e);
        }

        return urlsToTry;
    }

    private boolean checkXMLRPCEndpointValidity(String url, String httpUsername, String httpPassword) throws
            WPOrgUtilsException {
        try {
            Object[] methods = (Object[]) doSystemListMethodsXMLRPC(url, httpUsername, httpPassword);
            if (methods == null) {
                AppLog.e(T.NUX, "The response of system.listMethods was empty!");
                return false;
            }
            // Exit the loop on the first URL that replies with a XML-RPC doc.
            AppLog.i(T.NUX, "system.listMethods replied with XML-RPC objects on the URL: " + url);
            AppLog.i(T.NUX, "Validating the XML-RPC response...");
            if (validateListMethodsResponse(methods)) {
                // Endpoint address found and works fine.
                AppLog.i(T.NUX, "Validation ended with success!!! Endpoint found!!!");
                return true;
            } else {
                // Endpoint found, but it has problem.
                AppLog.w(T.NUX, "Validation ended with errors!!! Endpoint found but doesn't contain all the " +
                        "required methods.");
                throw new WPOrgUtilsException(WPOrgUtilsException.Kind.MISSING_XMLRPC_METHOD, org.wordpress.android
                        .R.string.xmlrpc_missing_method_error, url, null);
            }
        } catch (XMLRPCException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                throw new WPOrgUtilsException(WPOrgUtilsException.Kind.HTTP_AUTH_REQUIRED, 0, url, null);
            }
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            if (!WPUrlUtils.isWordPressCom(url)) {
                throw new WPOrgUtilsException(WPOrgUtilsException.Kind.ERRONEOUS_SSL_CERTIFICATE, 0, url, null);
            }
            AppLog.e(T.NUX, "SSL error. Erroneous SSL certificate detected.", e);
        } catch (IOException | XmlPullParserException e) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                throw new WPOrgUtilsException(WPOrgUtilsException.Kind.HTTP_AUTH_REQUIRED, 0, url, null);
            }
        } catch (IllegalArgumentException e) {
            // The XML-RPC client returns this error in case of redirect to an invalid URL.
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            throw new WPOrgUtilsException(WPOrgUtilsException.Kind.INVALID_URL, org.wordpress.android.R.string
                    .invalid_site_url_message, url, null);
        }

        return false;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site.
    // See diagrams here https://github.com/wordpress-mobile/WordPress-Android/issues/3805 for details about the
    // whole process.
    public String getSelfHostedXmlrpcUrl(String url, String httpUsername, String httpPassword) throws
            WPOrgUtilsException {
        // Start cleaning the url: Array of Strings that contains the URLs we want to try in the first step. No
        // discovery ;)
        ArrayList<String> urlsToTry = smartURLCleanerForXMLRPCCalls(url);
        AppLog.i(T.NUX, "The app will call system.listMethods on the following URLs: " + urlsToTry);

        for (String currentURL : urlsToTry) {
            if (checkXMLRPCEndpointValidity(currentURL, httpUsername, httpPassword)) {
                // Endpoint found and works fine.
                return currentURL;
            }
        }

        AppLog.w(T.NUX, "The XML-RPC endpoint was not found by using our 'smart' cleaning approach. Time to " +
                "start the Endpoint discovery process");

        urlsToTry = smartURLCleanerForRSD(url);
        AppLog.i(T.NUX, "The app will call the RSD discovery process on the following URLs: " + urlsToTry);

        String xmlrpcUrl = null;
        for (String currentURL : urlsToTry) {
            try {
                // Download the HTML content
                AppLog.i(T.NUX, "Downloading the HTML content at the following URL: " + currentURL);
                String responseHTML = ApiHelper.getResponse(currentURL);
                if (TextUtils.isEmpty(responseHTML)) {
                    AppLog.w(T.NUX, "Content downloaded but it's empty or null. Skipping this URL");
                    continue;
                }

                // Try to find the RSD tag with a regex
                String rsdUrl = getRSDMetaTagHrefRegEx(responseHTML);
                // If the regex approach fails try to parse the HTML doc and retrieve the RSD tag.
                if (rsdUrl == null) {
                    rsdUrl = getRSDMetaTagHref(responseHTML);
                }
                rsdUrl = UrlUtils.addUrlSchemeIfNeeded(rsdUrl, false);

                // if the RSD URL is empty here, try to see if there is already the pingback or the Apilink in the doc
                // the user could have inserted a direct link to the xml-rpc endpoint
                if (rsdUrl == null) {
                    AppLog.i(T.NUX, "Can't find the RSD endpoint in the HTML document. Try to check the " +
                            "pingback tag, and the apiLink tag.");
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCPingback(responseHTML), false);
                    if (xmlrpcUrl == null) {
                        xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCApiLink(responseHTML), false);
                    }
                } else {
                    AppLog.i(T.NUX, "RSD endpoint found at the following address: " + rsdUrl);
                    AppLog.i(T.NUX, "Downloading the RSD document...");
                    String rsdEndpointDocument = ApiHelper.getResponse(rsdUrl);
                    if (TextUtils.isEmpty(rsdEndpointDocument)) {
                        AppLog.w(T.NUX, "Content downloaded but it's empty or null. Skipping this RSD document" +
                                " URL.");
                        continue;
                    }
                    AppLog.i(T.NUX, "Extracting the XML-RPC Endpoint address from the RSD document");
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCApiLink(rsdEndpointDocument), false);
                }
                if (xmlrpcUrl != null) {
                    AppLog.i(T.NUX, "Found the XML-RPC endpoint in the HTML document!!!");
                    break;
                } else {
                    AppLog.i(T.NUX, "XML-RPC endpoint NOT found");
                }
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(url)) {
                    throw new WPOrgUtilsException(WPOrgUtilsException.Kind.ERRONEOUS_SSL_CERTIFICATE, 0, url, null);
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                return null;
            } catch (TimeoutError | TimeoutException e) {
                AppLog.w(T.NUX, "Timeout error while connecting to the site: " + currentURL);
                throw new WPOrgUtilsException(WPOrgUtilsException.Kind.SITE_TIME_OUT, org.wordpress.android.R
                        .string.site_timeout_error, url, null);
            }
        }

        if (URLUtil.isValidUrl(xmlrpcUrl)) {
            if (checkXMLRPCEndpointValidity(xmlrpcUrl, httpUsername, httpPassword)) {
                // Endpoint found and works fine.
                return xmlrpcUrl;
            }
        }

        throw new WPOrgUtilsException(WPOrgUtilsException.Kind.NO_SITE_ERROR, org.wordpress.android.R
                .string.no_site_error, null, null);
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
                final String baseUrl = canonicalizeSiteUrl(mSelfHostedUrl);

                //Retrieve the XML-RPC Endpoint address
                final String xmlrpcUrl = getSelfHostedXmlrpcUrl(baseUrl, mHttpUsername, mHttpPassword);

                // Validate the XML-RPC URL we've found before. This check prevents a crash that can occur
                // during the setup of self-hosted sites that have malformed xmlrpc URLs in their declaration.
                if (!URLUtil.isValidUrl(xmlrpcUrl)) {
                    mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
                    return null;
                }

                // The XML-RPC address is now available. Call wp.getUsersBlogs and load the sites.
                return getUserBlogList(URI.create(xmlrpcUrl), mUsername, mPassword, mHttpUsername,
                        mHttpPassword);
            } catch (WPOrgUtilsException hce) {
                mErrorMsgId = hce.errorMsgId;
                mHttpAuthRequired = (hce.kind == WPOrgUtilsException.Kind.HTTP_AUTH_REQUIRED);
                mErroneousSslCertificate = (hce.kind == WPOrgUtilsException.Kind.ERRONEOUS_SSL_CERTIFICATE);
                trackInvalidInsertedURL(hce.failedUrl);
                return null;
            }
        }

        private List<Map<String, Object>> getUserBlogList(URI xmlrpcUri, String username, String password, String
                httpUsername, String httpPassword)
                throws WPOrgUtilsException {
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(xmlrpcUri, httpUsername, httpPassword);
            Object[] params = { username, password };
            try {
                Object[] userBlogs = (Object[]) client.call(ApiHelper.Method.GET_BLOGS, params);
                if (userBlogs == null) {
                    // Could happen if the returned server response is truncated
                    throw new WPOrgUtilsException(Kind.XMLRPC_MALFORMED_RESPONSE, R.string.xmlrpc_malformed_response_error,
                            xmlrpcUri.toString(), client.getResponse());
                }
                Arrays.sort(userBlogs, BlogUtils.BlogNameComparator);
                List<Map<String, Object>> userBlogList = new ArrayList<>();
                for (Object blog : userBlogs) {
                    try {
                        userBlogList.add((Map<String, Object>) blog);
                    } catch (ClassCastException e) {
                        AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs");
                    }
                }
                return userBlogList;
            } catch (XmlPullParserException parserException) {
                AppLog.e(T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs", parserException);
                throw new WPOrgUtilsException(Kind.XMLRPC_ERROR, R.string.xmlrpc_error, xmlrpcUri.toString(), client
                        .getResponse());
            } catch (XMLRPCFault xmlRpcFault) {
                AppLog.e(T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
                throw new WPOrgUtilsException(Kind.XMLRPC_ERROR, handleXmlRpcFault(xmlRpcFault), xmlrpcUri.toString()
                        , client.getResponse());
            } catch (XMLRPCException xmlRpcException) {
                AppLog.e(T.NUX, "XMLRPCException received from XMLRPC call wp.getUsersBlogs", xmlRpcException);
                throw new WPOrgUtilsException(Kind.XMLRPC_ERROR, R.string.no_site_error, xmlrpcUri.toString(), client
                        .getResponse());
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(xmlrpcUri.toString())) {
                    throw new WPOrgUtilsException(WPOrgUtilsException.Kind.ERRONEOUS_SSL_CERTIFICATE, 0, xmlrpcUri
                            .toString(), null);
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
            } catch (ConnectTimeoutException e) {
                AppLog.e(T.NUX, "Timeout exception when calling wp.getUsersBlogs", e);
                throw new WPOrgUtilsException(WPOrgUtilsException.Kind.SITE_TIME_OUT, R.string.site_timeout_error,
                        xmlrpcUri.toString(), client.getResponse());
            } catch (IOException e) {
                AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
                throw new WPOrgUtilsException(Kind.XMLRPC_ERROR, R.string.no_site_error, xmlrpcUri.toString(), client
                        .getResponse());
            }

            throw new WPOrgUtilsException(Kind.XMLRPC_ERROR, R.string.no_site_error, xmlrpcUri.toString(), client
                    .getResponse());
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

    public static String canonicalizeSiteUrl(String siteUrl) throws WPOrgUtilsException {
        if (TextUtils.isEmpty(siteUrl) || TextUtils.isEmpty(siteUrl.trim())) {
            throw new WPOrgUtilsException(WPOrgUtilsException.Kind.SITE_URL_CANNOT_BE_EMPTY, org.wordpress.android
                    .R.string.invalid_site_url_message, siteUrl, null);
        }

        // remove padding whitespace
        String baseURL = siteUrl.trim();

        // Convert IDN names to punycode if necessary
        baseURL = UrlUtils.convertUrlToPunycodeIfNeeded(baseURL);
        // Add http to the beginning of the URL if needed
        baseURL = UrlUtils.addUrlSchemeIfNeeded(baseURL, false);
        if (!URLUtil.isValidUrl(baseURL)) {
            throw new WPOrgUtilsException(WPOrgUtilsException.Kind.INVALID_URL, org.wordpress.android.R.string
                    .invalid_site_url_message, baseURL, null);
        }

        return baseURL;
    }

    /**
     * Regex pattern for matching the RSD link found in most WordPress sites.
     */
    private static final Pattern rsdLink = Pattern.compile(
            "<link\\s*?rel=\"EditURI\"\\s*?type=\"application/rsd\\+xml\"\\s*?title=\"RSD\"\\s*?href=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns RSD URL based on regex match
     *
     * @return String RSD url
     */
    public static String getRSDMetaTagHrefRegEx(String html) {
        if (html != null) {
            Matcher matcher = rsdLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Returns RSD URL based on html tag search
     *
     * @return String RSD url
     */
    public static String getRSDMetaTagHref(String data) {
        // parse the html and get the attribute for xmlrpc endpoint
        if (data != null) {
            // Many WordPress configs can output junk before the xml response (php warnings for example), this cleans
            // it.
            int indexOfFirstXML = data.indexOf("<?xml");
            if (indexOfFirstXML > 0) {
                data = data.substring(indexOfFirstXML);
            }
            StringReader stringReader = new StringReader(data);
            XmlPullParser parser = Xml.newPullParser();
            try {
                // auto-detect the encoding from the stream
                parser.setInput(stringReader);
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name;
                    String rel = "";
                    String type = "";
                    String href = "";
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            name = parser.getName();
                            if (name.equalsIgnoreCase("link")) {
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    String attrName = parser.getAttributeName(i);
                                    String attrValue = parser.getAttributeValue(i);
                                    if (attrName.equals("rel")) {
                                        rel = attrValue;
                                    } else if (attrName.equals("type"))
                                        type = attrValue;
                                    else if (attrName.equals("href"))
                                        href = attrValue;
                                }

                                if (rel.equals("EditURI") && type.equals("application/rsd+xml")) {
                                    return href;
                                }
                                // currentMessage.setLink(parser.nextText());
                            }
                            break;
                    }
                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, e);
                return null;
            } catch (IOException e) {
                AppLog.e(T.API, e);
                return null;
            }
        }
        return null; // never found the rsd tag
    }

    /**
     * Find the XML-RPC endpoint for the WordPress API.
     *
     * @return XML-RPC endpoint for the specified blog, or null if unable to discover endpoint.
     */
    public static String getXMLRPCApiLink(String html) {
        Pattern xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        if (html != null) {
            Matcher matcher = xmlrpcLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null; // never found the api link tag
    }

    /**
     * Find the XML-RPC endpoint by using the pingback tag
     *
     * @return String XML-RPC url
     */
    public static String getXMLRPCPingback(String html) {
        Pattern pingbackLink = Pattern.compile(
                "<link\\s*?rel=\"pingback\"\\s*?href=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        if (html != null) {
            Matcher matcher = pingbackLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
