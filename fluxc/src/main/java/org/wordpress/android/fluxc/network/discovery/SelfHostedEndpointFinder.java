package org.wordpress.android.fluxc.network.discovery;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Xml;
import android.webkit.URLUtil;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.network.BaseRequestFuture;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.store.Store.OnChangedError;
import org.wordpress.android.fluxc.utils.WPUrlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

public class SelfHostedEndpointFinder {
    public static final int TIMEOUT_MS = 60000;

    private final Dispatcher mDispatcher;
    private final BaseXMLRPCClient mClient;

    public enum DiscoveryError implements OnChangedError {
        INVALID_URL,
        MISSING_XMLRPC_METHOD,
        ERRONEOUS_SSL_CERTIFICATE,
        HTTP_AUTH_REQUIRED,
        NO_SITE_ERROR,
        WORDPRESS_COM_SITE,
        GENERIC_ERROR
    }

    static class DiscoveryException extends Exception {
        public final DiscoveryError discoveryError;
        public final String failedUrl;

        DiscoveryException(DiscoveryError failureType, String failedUrl) {
            this.discoveryError = failureType;
            this.failedUrl = failedUrl;
        }
    }

    public static class DiscoveryResultPayload extends Payload {
        public String xmlRpcEndpoint;
        public String wpRestEndpoint;
        public DiscoveryError discoveryError;
        public String failedEndpoint;

        public DiscoveryResultPayload(String xmlRpcEndpoint, String wpRestEndpoint) {
            this.xmlRpcEndpoint = xmlRpcEndpoint;
            this.wpRestEndpoint = wpRestEndpoint;
        }

        public DiscoveryResultPayload(DiscoveryError discoveryError, String failedEndpoint) {
            this.discoveryError = discoveryError;
            this.failedEndpoint = failedEndpoint;
        }

        public boolean isDiscoveryError() {
            return discoveryError != null;
        }
    }

    @Inject
    public SelfHostedEndpointFinder(Dispatcher dispatcher, BaseXMLRPCClient baseXMLRPCClient) {
        mDispatcher = dispatcher;
        mClient = baseXMLRPCClient;
    }

    public void findEndpoint(final String url, final String username, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String xmlRpcEndpoint = verifyOrDiscoverXMLRPCEndpoint(url, username, password);
                    String wpRestEndpoint = discoverWPRESTEndpoint(url);
                    DiscoveryResultPayload payload = new DiscoveryResultPayload(xmlRpcEndpoint, wpRestEndpoint);
                    mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoveryResultAction(payload));
                } catch (DiscoveryException e) {
                    // TODO: Handle tracking of XMLRPCDiscoveryException
                    // If a DiscoveryException is caught this high up, it means that either:
                    // 1. The discovery process has completed, and did not turn up a valid WordPress.com site
                    // 2. Discovery was halted early because the given site requires SSL validation, or HTTP AUTH login,
                    // or is a WordPress.com site, or is a completely invalid URL
                    DiscoveryResultPayload payload = new DiscoveryResultPayload(e.discoveryError, e.failedUrl);
                    mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoveryResultAction(payload));
                }
            }
        }).start();
    }

    private String verifyOrDiscoverXMLRPCEndpoint(final String siteUrl, final String httpUsername,
                                                 final String httpPassword) throws DiscoveryException {
        if (TextUtils.isEmpty(siteUrl)) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, siteUrl);
        }

        if (WPUrlUtils.isWordPressCom(sanitizeSiteUrl(siteUrl, false))) {
            throw new DiscoveryException(DiscoveryError.WORDPRESS_COM_SITE, siteUrl);
        }

        String xmlrpcUrl = verifyXMLRPCUrl(siteUrl, httpUsername, httpPassword);

        if (xmlrpcUrl == null) {
            AppLog.w(T.NUX, "The XML-RPC endpoint was not found by using our 'smart' cleaning approach. "
                            + "Time to start the Endpoint discovery process");
            xmlrpcUrl = discoverXMLRPCEndpoint(siteUrl, httpUsername, httpPassword);
        }

        // Validate the XML-RPC URL we've found before. This check prevents a crash that can occur
        // during the setup of self-hosted sites that have malformed xmlrpc URLs in their declaration.
        if (!URLUtil.isValidUrl(xmlrpcUrl)) {
            throw new DiscoveryException(DiscoveryError.NO_SITE_ERROR, xmlrpcUrl);
        }

        return xmlrpcUrl;
    }

    private LinkedHashSet<String> getOrderedVerifyUrlsToTry(String siteUrl) throws DiscoveryException {
        LinkedHashSet<String> urlsToTry = new LinkedHashSet<>();
        final String sanitizedSiteUrlHttps = sanitizeSiteUrl(siteUrl, true);
        final String sanitizedSiteUrlHttp = sanitizeSiteUrl(siteUrl, false);

        // Start by adding the URL with 'xmlrpc.php'. This will be the first URL to try.
        // Prioritize https, unless the user specified the http:// protocol
        if (siteUrl.startsWith("http://")) {
            urlsToTry.add(DiscoveryUtils.appendXMLRPCPath(sanitizedSiteUrlHttp));
            urlsToTry.add(DiscoveryUtils.appendXMLRPCPath(sanitizedSiteUrlHttps));
        } else {
            urlsToTry.add(DiscoveryUtils.appendXMLRPCPath(sanitizedSiteUrlHttps));
            urlsToTry.add(DiscoveryUtils.appendXMLRPCPath(sanitizedSiteUrlHttp));
        }

        // Add the sanitized URL without the '/xmlrpc.php' suffix added to it
        // Prioritize https, unless the user specified the http:// protocol
        if (siteUrl.startsWith("http://")) {
            urlsToTry.add(sanitizedSiteUrlHttp);
            urlsToTry.add(sanitizedSiteUrlHttps);
        } else {
            urlsToTry.add(sanitizedSiteUrlHttps);
            urlsToTry.add(sanitizedSiteUrlHttp);
        }

        // Add the user provided URL as well
        urlsToTry.add(siteUrl);
        return urlsToTry;
    }

    private String verifyXMLRPCUrl(@NonNull final String siteUrl, final String httpUsername, final String httpPassword)
            throws DiscoveryException {
        // Ordered set of Strings that contains the URLs we want to try
        final LinkedHashSet<String> urlsToTry = getOrderedVerifyUrlsToTry(siteUrl);

        AppLog.i(T.NUX, "The app will call system.listMethods on the following URLs: " + urlsToTry);
        for (String url : urlsToTry) {
            try {
                if (checkXMLRPCEndpointValidity(url, httpUsername, httpPassword)) {
                    // Endpoint found and works fine.
                    return url;
                }
            } catch (DiscoveryException e) {
                // Stop execution for errors requiring user interaction
                if (e.discoveryError == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE
                    || e.discoveryError == DiscoveryError.HTTP_AUTH_REQUIRED
                    || e.discoveryError == DiscoveryError.MISSING_XMLRPC_METHOD) {
                    throw e;
                }
                // Otherwise. swallow the error since we are just verifying various URLs
            } catch (RuntimeException re) {
                // Depending how corrupt the user entered URL is, it can generate several kinds of runtime exceptions,
                // ignore them
            }
        }
        // Input url was not verified to be working
        return null;
    }

    // Attempts to retrieve the XML-RPC url for a self-hosted site.
    // See diagrams here https://github.com/wordpress-mobile/WordPress-Android/issues/3805 for details about the
    // whole process.
    private String discoverXMLRPCEndpoint(String siteUrl, String httpUsername, String httpPassword) throws
            DiscoveryException {
        // Ordered set of Strings that contains the URLs we want to try
        final Set<String> urlsToTry = new LinkedHashSet<>();

        // Add the url as provided by the user
        urlsToTry.add(siteUrl);

        // Add the sanitized URL url, prioritizing https, unless the user specified the http:// protocol
        if (siteUrl.startsWith("http://")) {
            urlsToTry.add(sanitizeSiteUrl(siteUrl, false));
            urlsToTry.add(sanitizeSiteUrl(siteUrl, true));
        } else {
            urlsToTry.add(sanitizeSiteUrl(siteUrl, true));
            urlsToTry.add(sanitizeSiteUrl(siteUrl, false));
        }

        AppLog.i(AppLog.T.NUX, "The app will call the RSD discovery process on the following URLs: " + urlsToTry);

        String xmlrpcUrl = null;
        for (String currentURL : urlsToTry) {
            if (!URLUtil.isValidUrl(currentURL)) {
                continue;
            }
            // Download the HTML content
            AppLog.i(AppLog.T.NUX, "Downloading the HTML content at the following URL: " + currentURL);
            String responseHTML = getResponse(currentURL);
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

            // If the RSD URL is empty here, try to see if the pingback or Apilink are in the doc, as the user
            // could have inserted a direct link to the XML-RPC endpoint
            if (rsdUrl == null) {
                AppLog.i(AppLog.T.NUX, "Can't find the RSD endpoint in the HTML document. Try to check the "
                                       + "pingback tag, and the apiLink tag.");
                xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(DiscoveryUtils.getXMLRPCPingback(responseHTML), false);
                if (xmlrpcUrl == null) {
                    xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(DiscoveryUtils.getXMLRPCApiLink(responseHTML), false);
                }
            } else {
                AppLog.i(AppLog.T.NUX, "RSD endpoint found at the following address: " + rsdUrl);
                AppLog.i(AppLog.T.NUX, "Downloading the RSD document...");
                String rsdEndpointDocument = getResponse(rsdUrl);
                if (TextUtils.isEmpty(rsdEndpointDocument)) {
                    AppLog.w(AppLog.T.NUX, "Content downloaded but it's empty or null. Skipping this RSD document"
                                           + " URL.");
                    continue;
                }
                AppLog.i(AppLog.T.NUX, "Extracting the XML-RPC Endpoint address from the RSD document");
                xmlrpcUrl = UrlUtils.addUrlSchemeIfNeeded(DiscoveryUtils.getXMLRPCApiLink(rsdEndpointDocument),
                        false);
            }
            if (xmlrpcUrl != null) {
                AppLog.i(AppLog.T.NUX, "Found the XML-RPC endpoint in the HTML document!!!");
                break;
            } else {
                AppLog.i(AppLog.T.NUX, "XML-RPC endpoint not found");
            }
        }

        if (URLUtil.isValidUrl(xmlrpcUrl)) {
            if (checkXMLRPCEndpointValidity(xmlrpcUrl, httpUsername, httpPassword)) {
                // Endpoint found and works fine.
                return xmlrpcUrl;
            }
        }

        throw new DiscoveryException(DiscoveryError.NO_SITE_ERROR, xmlrpcUrl);
    }


    private String discoverWPRESTEndpoint(String url) {
        // TODO: See http://v2.wp-api.org/guide/discovery/
        return url + "/wp-json/wp/v2/";
    }

    /**
     * Regex pattern for matching the RSD link found in most WordPress sites.
     */
    private static final Pattern RSD_LINK = Pattern.compile(
            "<link\\s*?rel=\"EditURI\"\\s*?type=\"application/rsd\\+xml\"\\s*?title=\"RSD\"\\s*?href=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns RSD URL based on regex match.
     */
    private String getRSDMetaTagHrefRegEx(String urlString) throws DiscoveryException {
        String html = getResponse(urlString);
        if (html != null) {
            Matcher matcher = RSD_LINK.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Returns RSD URL based on html tag search.
     */
    private String getRSDMetaTagHref(String urlString) throws DiscoveryException {
        // Get the html code
        String data = getResponse(urlString);

        // Parse the html and get the attribute for xmlrpc endpoint
        if (data != null) {
            StringReader stringReader = new StringReader(data);
            XmlPullParser parser = Xml.newPullParser();
            try {
                // Auto-detect the encoding from the stream
                parser.setInput(stringReader);
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name;
                    String rel = "";
                    String type = "";
                    String href = "";
                    if (eventType == XmlPullParser.START_TAG) {
                        name = parser.getName();
                        if (name.equalsIgnoreCase("link")) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                String attrValue = parser.getAttributeValue(i);
                                if (attrName.equals("rel")) {
                                    rel = attrValue;
                                } else if (attrName.equals("type")) {
                                    type = attrValue;
                                } else if (attrName.equals("href")) {
                                    href = attrValue;
                                }
                            }

                            if (rel.equals("EditURI") && type.equals("application/rsd+xml")) {
                                return href;
                            }
                        }
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
        return null; // Never found the rsd tag
    }

    /**
     * Obtain the HTML response from a GET request for the given URL.
     */
    private String getResponse(String url) throws DiscoveryException {
        BaseRequestFuture<String> future = BaseRequestFuture.newFuture();
        DiscoveryRequest request = new DiscoveryRequest(url, future, future);
        mClient.add(request);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(T.API, "Couldn't get XML-RPC response.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AuthFailureError) {
                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
            } else if (e.getCause() instanceof NoConnectionError && e.getCause().getCause() != null
                       && e.getCause().getCause() instanceof SSLHandshakeException) {
                // In the event of an SSL error we should stop attempting discovery
                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
            }
        }
        return null;
    }

    private String sanitizeSiteUrl(String siteUrl, boolean addHttps) throws DiscoveryException {
        // Remove padding whitespace
        String url = siteUrl.trim();

        if (TextUtils.isEmpty(url)) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, siteUrl);
        }

        // Convert IDN names to punycode if necessary
        url = UrlUtils.convertUrlToPunycodeIfNeeded(url);

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(UrlUtils.removeScheme(url), addHttps);

        // Strip url from known usual trailing paths
        url = DiscoveryUtils.stripKnownPaths(url);

        if (!(URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url))) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        return url;
    }

    private boolean checkXMLRPCEndpointValidity(String url, String httpUsername, String httpPassword)
            throws DiscoveryException {
        try {
            Object[] methods = doSystemListMethodsXMLRPC(url, httpUsername, httpPassword);
            if (methods == null) {
                AppLog.e(T.NUX, "The response of system.listMethods was empty for " + url);
                return false;
            }
            // Exit the loop on the first URL that replies with a XML-RPC doc.
            AppLog.i(T.NUX, "system.listMethods replied with XML-RPC objects on the URL: " + url);
            AppLog.i(T.NUX, "Validating the XML-RPC response...");
            if (DiscoveryUtils.validateListMethodsResponse(methods)) {
                // Endpoint address found and works fine.
                AppLog.i(T.NUX, "Validation ended with success! Endpoint found!");
                return true;
            } else {
                // Endpoint found, but it has problem.
                AppLog.w(T.NUX, "Validation ended with errors! Endpoint found but doesn't contain all the "
                                + "required methods.");
                throw new DiscoveryException(DiscoveryError.MISSING_XMLRPC_METHOD, url);
            }
        } catch (DiscoveryException e) {
            AppLog.e(T.NUX, "system.listMethods failed on: " + url, e);
            if (DiscoveryUtils.isHTTPAuthErrorMessage(e)
                || e.discoveryError.equals(DiscoveryError.HTTP_AUTH_REQUIRED)) {
                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
            } else if (e.discoveryError.equals(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE)) {
                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
            }
        } catch (IllegalArgumentException e) {
            // The XML-RPC client returns this error in case of redirect to an invalid URL.
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        return false;
    }

    private Object[] doSystemListMethodsXMLRPC(String url, String httpUsername, String httpPassword) throws
            DiscoveryException {
        if (!UrlUtils.isValidUrlAndHostNotNull(url)) {
            AppLog.e(T.NUX, "invalid URL: " + url);
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        AppLog.i(T.NUX, "Trying system.listMethods on the following URL: " + url);

        List<Object> params = new ArrayList<>(2);
        params.add(httpUsername);
        params.add(httpPassword);

        BaseRequestFuture<Object[]> future = BaseRequestFuture.newFuture();
        DiscoveryXMLRPCRequest request = new DiscoveryXMLRPCRequest(url, XMLRPC.LIST_METHODS, params, future, future);
        mClient.add(request);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(T.API, "Couldn't get XML-RPC response.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AuthFailureError) {
                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
            } else if (e.getCause() instanceof NoConnectionError && e.getCause().getCause() != null
                       && e.getCause().getCause() instanceof SSLHandshakeException) {
                // In the event of an SSL error we should stop attempting discovery
                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
            }
        }
        return null;
    }
}
