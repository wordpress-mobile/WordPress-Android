package org.xmlrpc.android;

import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Xml;
import android.webkit.URLUtil;

import com.android.volley.TimeoutError;

import org.apache.http.conn.ConnectTimeoutException;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.XMLRPCUtils.XMLRPCUtilsException.Kind;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

public class XMLRPCUtils {

    public static class XMLRPCUtilsException extends Exception {
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

        public XMLRPCUtilsException(Kind kind, @StringRes int errorMsgId, String failedUrl, String clientResponse) {
            this.kind = kind;
            this.errorMsgId = errorMsgId;
            this.failedUrl = failedUrl;
            this.clientResponse = clientResponse;
        }
    }

    private static @StringRes int handleXmlRpcFault(XMLRPCFault xmlRpcFault) {
        AppLog.e(AppLog.T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
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

    private static boolean isHTTPAuthErrorMessage(Exception e) {
        return e != null && e.getMessage() != null && e.getMessage().contains("401");
    }

    private static Object doSystemListMethodsXMLRPC(String url, String httpUsername, String httpPassword) throws
            XMLRPCException, IOException, XmlPullParserException, XMLRPCUtilsException {
        if (!UrlUtils.isValidUrlAndHostNotNull(url)) {
            AppLog.e(AppLog.T.NUX, "invalid URL: " + url);
            throw new XMLRPCUtilsException(Kind.INVALID_URL, org.wordpress.android.R.string
                    .invalid_site_url_message, url, null);
        }

        AppLog.i(AppLog.T.NUX, "Trying system.listMethods on the following URL: " + url);
        URI uri = URI.create(url);
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, httpUsername, httpPassword);
        return client.call(ApiHelper.Method.LIST_METHODS);
    }

    private static boolean validateListMethodsResponse(Object[] availableMethods) {
        if (availableMethods == null) {
            AppLog.e(AppLog.T.NUX, "The response of system.listMethods was empty!");
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
                AppLog.e(AppLog.T.NUX, "The following XML-RPC method: " + currentRequiredMethod + " is missing on the" +
                        " server.");
                return false;
            }
        }
        return true;
    }

    // Append "xmlrpc.php" if missing in the URL
    private static String appendXMLRPCPath(String url) {
        // Don't use 'ends' here! Some hosting wants parameters passed to baseURL/xmlrpc-php?my-authcode=XXX
        if (url.contains("xmlrpc.php")) {
            return url;
        } else {
            return url + "/xmlrpc.php";
        }
    }

    /**
     * Truncate a string beginning at the marker
     * @param url input string
     * @param marker the marker to begin the truncation from
     * @return new string truncated to the begining of the marker or the input string if marker is not found
     */
    private static String truncateUrl(String url, String marker) {
        if (TextUtils.isEmpty(marker) || url.indexOf(marker) < 0) {
            return url;
        }

        final String newUrl = url.substring(0, url.indexOf(marker));

        return URLUtil.isValidUrl(newUrl) ? newUrl : url;
    }

    public static String sanitizeSiteUrl(String siteUrl, boolean addHttps) throws XMLRPCUtilsException {
        // remove padding whitespace
        String url = siteUrl.trim();

        if (TextUtils.isEmpty(url)) {
            throw new XMLRPCUtilsException(XMLRPCUtilsException.Kind.SITE_URL_CANNOT_BE_EMPTY, R.string
                    .invalid_site_url_message, siteUrl, null);
        }

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(url, addHttps);

        // strip url from known usual trailing paths
        url = XMLRPCUtils.stripKnownPaths(url);

        if (!(URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url))) {
            throw new XMLRPCUtilsException(Kind.INVALID_URL, R.string.invalid_site_url_message, url, null);
        }

        return url;
    }

    private static String stripKnownPaths(String url) {
        // Remove 'wp-login.php' if available in the URL
        String sanitizedURL = truncateUrl(url, "wp-login.php");

        // Remove '/wp-admin' if available in the URL
        sanitizedURL = truncateUrl(sanitizedURL, "/wp-admin");

        // Remove '/wp-content' if available in the URL
        sanitizedURL = truncateUrl(sanitizedURL, "/wp-content");

        sanitizedURL = truncateUrl(sanitizedURL, "/xmlrpc.php?rsd");

        // remove any trailing slashes
        while (sanitizedURL.endsWith("/")) {
            sanitizedURL = sanitizedURL.substring(0, sanitizedURL.length() - 1);
        }

        return sanitizedURL;
    }

    private static boolean checkXMLRPCEndpointValidity(String url, String httpUsername, String httpPassword) throws
            XMLRPCUtilsException {
        try {
            Object[] methods = (Object[]) doSystemListMethodsXMLRPC(url, httpUsername, httpPassword);
            if (methods == null) {
                AppLog.e(AppLog.T.NUX, "The response of system.listMethods was empty!");
                return false;
            }
            // Exit the loop on the first URL that replies with a XML-RPC doc.
            AppLog.i(AppLog.T.NUX, "system.listMethods replied with XML-RPC objects on the URL: " + url);
            AppLog.i(AppLog.T.NUX, "Validating the XML-RPC response...");
            if (validateListMethodsResponse(methods)) {
                // Endpoint address found and works fine.
                AppLog.i(AppLog.T.NUX, "Validation ended with success!!! Endpoint found!!!");
                return true;
            } else {
                // Endpoint found, but it has problem.
                AppLog.w(AppLog.T.NUX, "Validation ended with errors!!! Endpoint found but doesn't contain all the " +
                        "required methods.");
                throw new XMLRPCUtilsException(Kind.MISSING_XMLRPC_METHOD, org.wordpress.android
                        .R.string.xmlrpc_missing_method_error, url, null);
            }
        } catch (XMLRPCException e) {
            AppLog.e(AppLog.T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                throw new XMLRPCUtilsException(Kind.HTTP_AUTH_REQUIRED, 0, url, null);
            }
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            if (!WPUrlUtils.isWordPressCom(url)) {
                throw new XMLRPCUtilsException(Kind.ERRONEOUS_SSL_CERTIFICATE, 0, url, null);
            }
            AppLog.e(AppLog.T.NUX, "SSL error. Erroneous SSL certificate detected.", e);
        } catch (IOException | XmlPullParserException e) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            AppLog.e(AppLog.T.NUX, "system.listMethods failed on: " + url, e);
            if (isHTTPAuthErrorMessage(e)) {
                throw new XMLRPCUtilsException(Kind.HTTP_AUTH_REQUIRED, 0, url, null);
            }
        } catch (IllegalArgumentException e) {
            // The XML-RPC client returns this error in case of redirect to an invalid URL.
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED_TO_GUESS_XMLRPC);
            throw new XMLRPCUtilsException(Kind.INVALID_URL, org.wordpress.android.R.string
                    .invalid_site_url_message, url, null);
        }

        return false;
    }

    public static String verifyOrDiscoverXmlRpcUrl(final String siteUrl, final String httpUsername, final String
            httpPassword) throws XMLRPCUtilsException {
        String xmlrpcUrl = XMLRPCUtils.verifyXmlrpcUrl(siteUrl, httpUsername, httpPassword);

        if (xmlrpcUrl == null) {
            AppLog.w(AppLog.T.NUX, "The XML-RPC endpoint was not found by using our 'smart' cleaning approach" +
                    ". Time to start the Endpoint discovery process");

            // Try to discover the XML-RPC Endpoint address
            xmlrpcUrl = XMLRPCUtils.discoverSelfHostedXmlrpcUrl(siteUrl, httpUsername, httpPassword);
        }

        // Validate the XML-RPC URL we've found before. This check prevents a crash that can occur
        // during the setup of self-hosted sites that have malformed xmlrpc URLs in their declaration.
        if (!URLUtil.isValidUrl(xmlrpcUrl)) {
            throw new XMLRPCUtilsException(Kind.NO_SITE_ERROR, R.string.invalid_site_url_message, xmlrpcUrl, null);
        }

        return xmlrpcUrl;
    }

    private static String verifyXmlrpcUrl(final String siteUrl, final String httpUsername, final String httpPassword)
            throws XMLRPCUtilsException {
        // Ordered set of Strings that contains the URLs we want to try. No discovery ;)
        final Set<String> urlsToTry = new LinkedHashSet<>();

        final String sanitizedSiteUrlHttps = XMLRPCUtils.sanitizeSiteUrl(siteUrl, true);
        final String sanitizedSiteUrlHttp = XMLRPCUtils.sanitizeSiteUrl(siteUrl, false);

        // start by adding the https URL with 'xmlrpc.php'. This will be the first URL to try.
        urlsToTry.add(XMLRPCUtils.appendXMLRPCPath(sanitizedSiteUrlHttp));
        urlsToTry.add(XMLRPCUtils.appendXMLRPCPath(sanitizedSiteUrlHttps));

        // add the sanitized https URL without the '/xmlrpc.php' suffix added to it
        urlsToTry.add(sanitizedSiteUrlHttp);
        urlsToTry.add(sanitizedSiteUrlHttps);

        // add the user provided URL as well
        urlsToTry.add(siteUrl);

        AppLog.i(AppLog.T.NUX, "The app will call system.listMethods on the following URLs: " + urlsToTry);
        for (String url : urlsToTry) {
            try {
                if (XMLRPCUtils.checkXMLRPCEndpointValidity(url, httpUsername, httpPassword)) {
                    // Endpoint found and works fine.
                    return url;
                }
            } catch (XMLRPCUtilsException e) {
                if (e.kind == XMLRPCUtilsException.Kind.ERRONEOUS_SSL_CERTIFICATE ||
                    e.kind == XMLRPCUtilsException.Kind.HTTP_AUTH_REQUIRED ||
                    e.kind == XMLRPCUtilsException.Kind.MISSING_XMLRPC_METHOD) {
                    throw e;
                }
                // swallow the error since we are just verifying various URLs
                continue;
            } catch (RuntimeException re) {
                // depending how corrupt the user entered URL is, it can generate several kind of runtime exceptions,
                // ignore them
                continue;
            }
        }

        // input url was not verified to be working
        return null;
    }

    // Attempts to retrieve the xmlrpc url for a self-hosted site.
    // See diagrams here https://github.com/wordpress-mobile/WordPress-Android/issues/3805 for details about the
    // whole process.
    private static String discoverSelfHostedXmlrpcUrl(String siteUrl, String httpUsername, String httpPassword) throws
            XMLRPCUtilsException {
        // Ordered set of Strings that contains the URLs we want to try
        final Set<String> urlsToTry = new LinkedHashSet<>();

        // add the url as provided by the user
        urlsToTry.add(siteUrl);

        // add a sanitized version of the https url (if the user didn't specify it)
        urlsToTry.add(sanitizeSiteUrl(siteUrl, true));

        // add a sanitized version of the http url (if the user didn't specify it)
        urlsToTry.add(sanitizeSiteUrl(siteUrl, false));

        AppLog.i(AppLog.T.NUX, "The app will call the RSD discovery process on the following URLs: " + urlsToTry);

        String xmlrpcUrl = null;
        for (String currentURL : urlsToTry) {
            try {
                // Download the HTML content
                AppLog.i(AppLog.T.NUX, "Downloading the HTML content at the following URL: " + currentURL);
                String responseHTML = ApiHelper.getResponse(currentURL);
                if (TextUtils.isEmpty(responseHTML)) {
                    AppLog.w(AppLog.T.NUX, "Content downloaded but it's empty or null. Skipping this URL");
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
                    AppLog.i(AppLog.T.NUX, "Can't find the RSD endpoint in the HTML document. Try to check the " +
                            "pingback tag, and the apiLink tag.");
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCPingback(responseHTML), false);
                    if (xmlrpcUrl == null) {
                        xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCApiLink(responseHTML), false);
                    }
                } else {
                    AppLog.i(AppLog.T.NUX, "RSD endpoint found at the following address: " + rsdUrl);
                    AppLog.i(AppLog.T.NUX, "Downloading the RSD document...");
                    String rsdEndpointDocument = ApiHelper.getResponse(rsdUrl);
                    if (TextUtils.isEmpty(rsdEndpointDocument)) {
                        AppLog.w(AppLog.T.NUX, "Content downloaded but it's empty or null. Skipping this RSD document" +
                                " URL.");
                        continue;
                    }
                    AppLog.i(AppLog.T.NUX, "Extracting the XML-RPC Endpoint address from the RSD document");
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(getXMLRPCApiLink(rsdEndpointDocument), false);
                }
                if (xmlrpcUrl != null) {
                    AppLog.i(AppLog.T.NUX, "Found the XML-RPC endpoint in the HTML document!!!");
                    break;
                } else {
                    AppLog.i(AppLog.T.NUX, "XML-RPC endpoint NOT found");
                }
            } catch (SSLHandshakeException e) {
                if (!WPUrlUtils.isWordPressCom(currentURL)) {
                    throw new XMLRPCUtilsException(Kind.ERRONEOUS_SSL_CERTIFICATE, 0, currentURL, null);
                }
                AppLog.w(AppLog.T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
                return null;
            } catch (TimeoutError | TimeoutException e) {
                AppLog.w(AppLog.T.NUX, "Timeout error while connecting to the site: " + currentURL);
                throw new XMLRPCUtilsException(Kind.SITE_TIME_OUT, org.wordpress.android.R
                        .string.site_timeout_error, currentURL, null);
            }
        }

        if (URLUtil.isValidUrl(xmlrpcUrl)) {
            if (checkXMLRPCEndpointValidity(xmlrpcUrl, httpUsername, httpPassword)) {
                // Endpoint found and works fine.
                return xmlrpcUrl;
            }
        }

        throw new XMLRPCUtilsException(Kind.NO_SITE_ERROR, org.wordpress.android.R.string.no_site_error, null, null);
    }

    public static List<Map<String, Object>> getUserBlogsList(URI xmlrpcUri, String username, String password, String
            httpUsername, String httpPassword) throws XMLRPCUtilsException {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(xmlrpcUri, httpUsername, httpPassword);
        Object[] params = { username, password };
        try {
            Object[] userBlogs = (Object[]) client.call(ApiHelper.Method.GET_BLOGS, params);
            if (userBlogs == null) {
                // Could happen if the returned server response is truncated
                throw new XMLRPCUtilsException(Kind.XMLRPC_MALFORMED_RESPONSE, R.string.xmlrpc_malformed_response_error,
                        xmlrpcUri.toString(), client.getResponse());
            }
            Arrays.sort(userBlogs, BlogUtils.BlogNameComparator);
            List<Map<String, Object>> userBlogList = new ArrayList<>();
            for (Object blog : userBlogs) {
                try {
                    userBlogList.add((Map<String, Object>) blog);
                } catch (ClassCastException e) {
                    AppLog.e(AppLog.T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs");
                }
            }
            return userBlogList;
        } catch (XmlPullParserException parserException) {
            AppLog.e(AppLog.T.NUX, "invalid data received from XMLRPC call wp.getUsersBlogs", parserException);
            throw new XMLRPCUtilsException(Kind.XMLRPC_ERROR, R.string.xmlrpc_error, xmlrpcUri.toString(), client
                    .getResponse());
        } catch (XMLRPCFault xmlRpcFault) {
            AppLog.e(AppLog.T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
            throw new XMLRPCUtilsException(Kind.XMLRPC_ERROR, handleXmlRpcFault(xmlRpcFault), xmlrpcUri.toString()
                    , client.getResponse());
        } catch (XMLRPCException xmlRpcException) {
            AppLog.e(AppLog.T.NUX, "XMLRPCException received from XMLRPC call wp.getUsersBlogs", xmlRpcException);
            throw new XMLRPCUtilsException(Kind.XMLRPC_ERROR, R.string.no_site_error, xmlrpcUri.toString(), client
                    .getResponse());
        } catch (SSLHandshakeException e) {
            if (!WPUrlUtils.isWordPressCom(xmlrpcUri.toString())) {
                throw new XMLRPCUtilsException(Kind.ERRONEOUS_SSL_CERTIFICATE, 0, xmlrpcUri.toString(), null);
            }
            AppLog.w(AppLog.T.NUX, "SSLHandshakeException failed. Erroneous SSL certificate detected.");
        } catch (ConnectTimeoutException e) {
            AppLog.e(AppLog.T.NUX, "Timeout exception when calling wp.getUsersBlogs", e);
            throw new XMLRPCUtilsException(Kind.SITE_TIME_OUT, R.string.site_timeout_error,
                    xmlrpcUri.toString(), client.getResponse());
        } catch (IOException e) {
            AppLog.e(AppLog.T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
            throw new XMLRPCUtilsException(Kind.XMLRPC_ERROR, R.string.no_site_error, xmlrpcUri.toString(), client
                    .getResponse());
        }

        throw new XMLRPCUtilsException(Kind.XMLRPC_ERROR, R.string.no_site_error, xmlrpcUri.toString(), client
                .getResponse());
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
    private static String getRSDMetaTagHrefRegEx(String html) {
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
    private static String getRSDMetaTagHref(String data) {
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
                AppLog.e(AppLog.T.API, e);
                return null;
            } catch (IOException e) {
                AppLog.e(AppLog.T.API, e);
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
    private static String getXMLRPCApiLink(String html) {
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
    private static String getXMLRPCPingback(String html) {
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
