package org.wordpress.android.ui.accounts.helpers;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Xml;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        AppLog.i(T.NUX, "Trying system.listMethods on the following URL: " + url);
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

    // Append "xmlrpc.php" if missing in the URL
    private String appendXMLRPCPath(String url) throws IllegalArgumentException {
        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("Input URL " + url + " is not valid!");
        }

        if (!url.contains("xmlrpc.php")) { // Do not use 'ends' here! Some hosting wants parameters passed to baseURL/xmlrpc-php?my-authcode=XXX
            if (url.substring(url.length() - 1, url.length()).equals("/")) {
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
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "wp-login.php" );
            // Remove '/wp-admin' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-admin" );
            // Remove '/wp-content' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-content" );
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/xmlrpc.php?rsd" );
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
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "wp-login.php" );
            // Remove '/wp-admin' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-admin" );
            // Remove '/wp-content' if available in the URL
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-content" );
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

    private boolean checkXMLRPCEndpointValidity(String url) {
        try {
            Object[] methods = (Object[]) doSystemListMethodsXMLRPC(url);
            // Exit the loop on the first URL that replies with a XML-RPC doc.
            AppLog.i(T.NUX, "system.listMethods replied with XML-RPC objects on the URL: " + url);
            AppLog.i(T.NUX, "Validating the XML-RPC response...");
            if (validateListMethodsResponse(methods)) {
                // Endpoint address found and works fine.
                AppLog.i(T.NUX, "Validation ended with success!!! Endpoint found!!!");
                return true;
            } else {
                // Endpoint found, but it has problem.
                AppLog.w(T.NUX, "Validation ended with errors!!! Endpoint found but doesn't contain all the required methods.");
                mErrorMsgId = org.wordpress.android.R.string.xmlrpc_missing_method_error;
                return false;
            }
        } catch (XMLRPCException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                mHttpAuthRequired = true;
            }
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            if (!WPUrlUtils.isWordPressCom(url)) {
                mErroneousSslCertificate = true;
            }
            AppLog.e(T.NUX, "SSL error. Erroneous SSL certificate detected.", e);
        } catch (IOException e) {
            AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                mHttpAuthRequired = true;
            }
        } catch (XmlPullParserException e) {
            AnalyticsTracker.track(Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                mHttpAuthRequired = true;
            }
        } catch (IllegalArgumentException e) {
            // TODO: Hopefully a temporary log - remove it if we find a pattern of failing URLs
            CrashlyticsUtils.setString(ExtraKey.ENTERED_URL, url);
            CrashlyticsUtils.logException(e, ExceptionType.SPECIFIC, T.NUX);
            mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
        }

        return false;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site.
    // See diagrams here https://github.com/wordpress-mobile/WordPress-Android/issues/3805 for details about the whole process.
    private String getSelfHostedXmlrpcUrl(String url) {
        // Start cleaning the url: Array of Strings that contains the URLs we want to try in the first step. No discovery ;)
        ArrayList<String> urlsToTry = smartURLCleanerForXMLRPCCalls(url);
        AppLog.i(T.NUX, "The app will call system.listMethods on the following URLs: " + urlsToTry);

        for (String currentURL: urlsToTry) {
            boolean isValid = checkXMLRPCEndpointValidity(currentURL);
            if (!isValid) {
                // should stop immediately if SSL or Basic Auth Error
                // of if any of the required XML-RPC methods are missing from the endpoint
                if (mErroneousSslCertificate || mHttpAuthRequired ||
                        mErrorMsgId == org.wordpress.android.R.string.xmlrpc_missing_method_error) {
                    return null;
                }
            } else {
                // Endpoint found and works fine.
                return currentURL;
            }
        }

        AppLog.w(T.NUX, "The XML-RPC endpoint was not found by using our 'smart' cleaning approach. Time to start the Endpoint discovery process");

        urlsToTry = smartURLCleanerForRSD(url);
        AppLog.i(T.NUX, "The app will call the RSD discovery process on the following URLs: " + urlsToTry);

        String xmlrpcUrl = null;
        for (String currentURL: urlsToTry) {
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
                    AppLog.i(T.NUX, "Can't find the RSD endpoint in the HTML document. Try to check the pingback tag, and the apiLink tag.");
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCPingback(responseHTML), false);
                    if (xmlrpcUrl == null) {
                        xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCApiLink(responseHTML), false);
                    }
                } else {
                    AppLog.i(T.NUX, "RSD endpoint found at the following address: " + rsdUrl);
                    AppLog.i(T.NUX, "Getting the XML-RPC url by downloading the RSD doc");
                    String rsdEndpointDocument = ApiHelper.getResponse(rsdUrl);
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCApiLink(rsdEndpointDocument), false);
                }
                if (xmlrpcUrl != null) {
                    AppLog.i(T.NUX, "Found the XML-RPC endpoint in the HTML document!!!");
                    break;
                }
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(url)) {
                    mErroneousSslCertificate = true;
                }
                AppLog.w(T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                return null;
            }
        }

        if (URLUtil.isValidUrl(xmlrpcUrl)) {
            boolean isValid = checkXMLRPCEndpointValidity(xmlrpcUrl);
            if (!isValid) {
                return null;
            } else {
                // Endpoint found and works fine.
                return xmlrpcUrl;
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

        private boolean isBBPluginInstalled(String url){
            PluginsCheckerWPOrg pluginsCheckerWPOrg = new PluginsCheckerWPOrg(url);
            List<PluginsCheckerWPOrg.Plugin> listOfBadBehaviourPlugins = pluginsCheckerWPOrg.checkForPlugins();
            if (listOfBadBehaviourPlugins != null && listOfBadBehaviourPlugins.size() > 0) {
                return true;
            }
            return false;
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Void... notUsed) {
            if (TextUtils.isEmpty(mSelfHostedUrl)) {
                mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
                // TODO: Bump analytics here?
                return null;
            }

            // Convert IDN names to punycode if necessary
            String baseURL = UrlUtils.convertUrlToPunycodeIfNeeded(mSelfHostedUrl);
            // Add http to the beginning of the URL if needed
            baseURL = UrlUtils.addUrlSchemeIfNeeded(baseURL, false);
            if (!URLUtil.isValidUrl(baseURL)) {
                mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
                // TODO: Bump analytics here?
                return null;
            }

            //Retrieve the XML-RPC Endpoint address
            String xmlrpcUrl = getSelfHostedXmlrpcUrl(baseURL);

            if (xmlrpcUrl == null) {
                if (mErroneousSslCertificate || mHttpAuthRequired ||
                        mErrorMsgId == org.wordpress.android.R.string.xmlrpc_missing_method_error) {
                    return null;
                }

                // Check if some Bad Behavior plugins in installed on the server
                if (isBBPluginInstalled(baseURL)) {
                    //TODO: Instead of returning a silly error message. Better to provide the list of plugins
                    // that could give error
                    mErrorMsgId = org.wordpress.android.R.string.site_plugins_error;
                    return null;
                }

                // CAn't find the XML-RPC endpoint return a generic message
                if (mErrorMsgId == 0) {
                    mErrorMsgId = org.wordpress.android.R.string.no_site_error;
                }
                return null;
            }

            // Validate the XML-RPC URL we've found before. This check prevents a crash that can occur
            // during the setup of self-hosted sites that have malformed xmlrpc URLs in their declaration.
            if (!URLUtil.isValidUrl(xmlrpcUrl)) {
                mErrorMsgId = org.wordpress.android.R.string.invalid_site_url_message;
                // TODO: Bump analytics here?
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
                    //TODO: please return a different error message here
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
                if (isBBPluginInstalled(xmlrpcUrl)) {
                    mErrorMsgId = org.wordpress.android.R.string.site_plugins_error;
                    return null;
                }
                // TODO : Catch org.apache.http.conn.ConnectTimeoutException and rturn a different message
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

    /**
     * Regex pattern for matching the RSD link found in most WordPress sites.
     */
    private static final Pattern rsdLink = Pattern.compile(
            "<link\\s*?rel=\"EditURI\"\\s*?type=\"application/rsd\\+xml\"\\s*?title=\"RSD\"\\s*?href=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns RSD URL based on regex match
     * @param html
     * @return String RSD url
     */
    public static String getRSDMetaTagHrefRegEx(String html) {
        if (html != null) {
            Matcher matcher = rsdLink.matcher(html);
            if (matcher.find()) {
                String href = matcher.group(1);
                return href;
            }
        }
        return null;
    }

    /**
     * Returns RSD URL based on html tag search
     * @param data
     * @return String RSD url
     */
    public static String getRSDMetaTagHref(String data) {
        // parse the html and get the attribute for xmlrpc endpoint
        if (data != null) {
            // Many WordPress configs can output junk before the xml response (php warnings for example), this cleans it.
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
                    String name = null;
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
     * @param html
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
     * @param html
     * @return String XML-RPC url
     */
    public static String getXMLRPCPingback(String html) {
        Pattern pingbackLink = Pattern.compile(
                "<link\\s*?rel=\"pingback\"\\s*?href=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        if (html != null) {
            Matcher matcher = pingbackLink.matcher(html);
            if (matcher.find()) {
                String href = matcher.group(1);
                return href;
            }
        }
        return null;
    }
}
