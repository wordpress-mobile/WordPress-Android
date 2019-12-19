package org.wordpress.android.fluxc.network.discovery;

import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.BuildConfig;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.Store.OnChangedError;
import org.wordpress.android.fluxc.utils.WPUrlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelfHostedEndpointFinder {
    public static final int TIMEOUT_MS = 60000;

    private final Dispatcher mDispatcher;
    private final DiscoveryXMLRPCClient mDiscoveryXMLRPCClient;
    private final DiscoveryWPAPIRestClient mDiscoveryWPAPIRestClient;

    public enum DiscoveryError implements OnChangedError {
        INVALID_URL,
        MISSING_XMLRPC_METHOD,
        ERRONEOUS_SSL_CERTIFICATE,
        HTTP_AUTH_REQUIRED,
        NO_SITE_ERROR,
        WORDPRESS_COM_SITE,
        XMLRPC_BLOCKED,
        XMLRPC_FORBIDDEN,
        GENERIC_ERROR
    }

    public static class DiscoveryException extends Exception {
        private static final long serialVersionUID = -300904137122546854L;

        public final DiscoveryError discoveryError;
        public final String failedUrl;

        DiscoveryException(DiscoveryError failureType, String failedUrl) {
            this.discoveryError = failureType;
            this.failedUrl = failedUrl;
        }
    }

    public static class DiscoveryResultPayload extends Payload<DiscoveryError> {
        public String xmlRpcEndpoint;
        public String wpRestEndpoint;
        public String failedEndpoint;

        public DiscoveryResultPayload(String xmlRpcEndpoint, String wpRestEndpoint) {
            this.xmlRpcEndpoint = xmlRpcEndpoint;
            this.wpRestEndpoint = wpRestEndpoint;
        }

        public DiscoveryResultPayload(DiscoveryError discoveryError, String failedEndpoint) {
            this.error = discoveryError;
            this.failedEndpoint = failedEndpoint;
        }
    }

    public SelfHostedEndpointFinder(Dispatcher dispatcher, DiscoveryXMLRPCClient discoveryXMLRPCClient,
                                    DiscoveryWPAPIRestClient discoveryWPAPIRestClient) {
        mDispatcher = dispatcher;
        mDiscoveryXMLRPCClient = discoveryXMLRPCClient;
        mDiscoveryWPAPIRestClient = discoveryWPAPIRestClient;
    }

    public void findEndpoint(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String wpRestEndpoint = "";
                    if (BuildConfig.ENABLE_WPAPI) {
                        wpRestEndpoint = discoverWPRESTEndpoint(url);
                    }
                    // TODO: Eventually make the XML-RPC discovery only run if WP-API discovery fails
                    String xmlRpcEndpoint = verifyOrDiscoverXMLRPCEndpoint(url);
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

    private String verifyOrDiscoverXMLRPCEndpoint(final String siteUrl) throws DiscoveryException {
        if (TextUtils.isEmpty(siteUrl)) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, siteUrl);
        }

        if (WPUrlUtils.isWordPressCom(sanitizeSiteUrl(siteUrl, false))) {
            throw new DiscoveryException(DiscoveryError.WORDPRESS_COM_SITE, siteUrl);
        }

        String xmlrpcUrl = verifyXMLRPCUrl(siteUrl);

        if (xmlrpcUrl == null) {
            AppLog.w(T.NUX, "The XML-RPC endpoint was not found by using our 'smart' cleaning approach. "
                            + "Time to start the Endpoint discovery process");
            xmlrpcUrl = discoverXMLRPCEndpoint(siteUrl);
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

    private String verifyXMLRPCUrl(@NonNull final String siteUrl) throws DiscoveryException {
        // Ordered set of Strings that contains the URLs we want to try
        final LinkedHashSet<String> urlsToTry = getOrderedVerifyUrlsToTry(siteUrl);

        AppLog.i(T.NUX, "Calling system.listMethods on the following URLs: " + urlsToTry);
        for (String url : urlsToTry) {
            try {
                if (checkXMLRPCEndpointValidity(url)) {
                    // Endpoint found and works fine.
                    return url;
                }
            } catch (DiscoveryException e) {
                // Stop execution for errors requiring user interaction
                if (e.discoveryError == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE
                    || e.discoveryError == DiscoveryError.HTTP_AUTH_REQUIRED
                    || e.discoveryError == DiscoveryError.MISSING_XMLRPC_METHOD
                    || e.discoveryError == DiscoveryError.XMLRPC_BLOCKED) {
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
    private String discoverXMLRPCEndpoint(String siteUrl) throws DiscoveryException {
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

        AppLog.i(AppLog.T.NUX, "Running RSD discovery process on the following URLs: " + urlsToTry);

        String xmlrpcUrl = null;
        boolean isWpSite = false;
        for (String currentURL : urlsToTry) {
            if (!URLUtil.isValidUrl(currentURL)) {
                continue;
            }
            // Download the HTML content
            AppLog.i(AppLog.T.NUX, "Downloading the HTML content at the following URL: " + currentURL);
            String responseHTML = mDiscoveryXMLRPCClient.getResponse(currentURL);
            if (TextUtils.isEmpty(responseHTML)) {
                AppLog.w(AppLog.T.NUX, "Content downloaded but it's empty or null. Skipping this URL");
                continue;
            }

            // Try to find the RSD tag with a regex
            String rsdUrl = getRSDMetaTagHrefRegEx(responseHTML);
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
                // If the site contains RSD link, it is WP.org site
                isWpSite = true;
                AppLog.i(AppLog.T.NUX, "RSD endpoint found at the following address: " + rsdUrl);
                AppLog.i(AppLog.T.NUX, "Downloading the RSD document...");
                String rsdEndpointDocument = mDiscoveryXMLRPCClient.getResponse(rsdUrl);
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
                AppLog.i(AppLog.T.NUX, "Found the XML-RPC endpoint in the HTML document");
                break;
            } else {
                AppLog.i(AppLog.T.NUX, "XML-RPC endpoint not found");
            }
        }

        if (URLUtil.isValidUrl(xmlrpcUrl)) {
            if (checkXMLRPCEndpointValidity(xmlrpcUrl)) {
                // Endpoint found and works fine.
                return xmlrpcUrl;
            }
        }
        if (!isWpSite) {
            throw new DiscoveryException(DiscoveryError.NO_SITE_ERROR, xmlrpcUrl);
        } else {
            throw new DiscoveryException(DiscoveryError.MISSING_XMLRPC_METHOD, xmlrpcUrl);
        }
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
    private String getRSDMetaTagHrefRegEx(@NonNull String html) throws DiscoveryException {
        Matcher matcher = RSD_LINK.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String sanitizeSiteUrl(String siteUrl, boolean addHttps) throws DiscoveryException {
        // Remove padding whitespace
        String url = siteUrl.trim();

        if (TextUtils.isEmpty(url)) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, siteUrl);
        }

        try {
            // Convert IDN names to punycode if necessary
            url = UrlUtils.convertUrlToPunycodeIfNeeded(url);
        } catch (IllegalArgumentException e) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, siteUrl);
        }

        // Add http to the beginning of the URL if needed
        url = UrlUtils.addUrlSchemeIfNeeded(UrlUtils.removeScheme(url), addHttps);

        // Strip url from known usual trailing paths
        url = DiscoveryUtils.stripKnownPaths(url);

        if (!(URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url))) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        return url;
    }

    private boolean checkXMLRPCEndpointValidity(String url) throws DiscoveryException {
        try {
            Object[] methods = mDiscoveryXMLRPCClient.listMethods(url);
            if (methods == null) {
                AppLog.e(T.NUX, "The response of system.listMethods was empty for " + url);
                return false;
            }
            // Exit the loop on the first URL that replies with a XML-RPC doc.
            AppLog.i(T.NUX, "system.listMethods replied with XML-RPC objects for " + url);
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
            AppLog.e(T.NUX, "system.listMethods failed for " + url, e);
            if (DiscoveryUtils.isHTTPAuthErrorMessage(e)
                || e.discoveryError.equals(DiscoveryError.HTTP_AUTH_REQUIRED)) {
                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
            } else if (e.discoveryError.equals(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE)) {
                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
            } else if (e.discoveryError.equals(DiscoveryError.XMLRPC_BLOCKED)) {
                throw new DiscoveryException(DiscoveryError.XMLRPC_BLOCKED, url);
            } else if (e.discoveryError.equals(DiscoveryError.MISSING_XMLRPC_METHOD)) {
                throw new DiscoveryException(DiscoveryError.MISSING_XMLRPC_METHOD, url);
            }
        } catch (IllegalArgumentException e) {
            // The XML-RPC client returns this error in case of redirect to an invalid URL.
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        return false;
    }

    private String discoverWPRESTEndpoint(String url) throws DiscoveryException {
        if (TextUtils.isEmpty(url)) {
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        if (WPUrlUtils.isWordPressCom(sanitizeSiteUrl(url, false))) {
            throw new DiscoveryException(DiscoveryError.WORDPRESS_COM_SITE, url);
        }

        // TODO: Implement URL validation in this and its called methods, and http/https neutrality

        final String wpApiBaseUrl = mDiscoveryWPAPIRestClient.discoverWPAPIBaseURL(url);

        if (wpApiBaseUrl != null && !wpApiBaseUrl.isEmpty()) {
            AppLog.i(AppLog.T.NUX, "Base WP-API URL found - verifying that the wp/v2 namespace is supported");
            return mDiscoveryWPAPIRestClient.verifyWPAPIV2Support(wpApiBaseUrl);
        }
        return null;
    }
}
