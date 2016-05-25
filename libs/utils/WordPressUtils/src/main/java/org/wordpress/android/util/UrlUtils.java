package org.wordpress.android.util;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import org.wordpress.android.util.AppLog.T;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class UrlUtils {
    public static String urlEncode(final String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

    public static String urlDecode(final String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

    /**
     * @param urlString url to get host from
     * @return host of uri if available. Empty string otherwise.
     */
    public static String getHost(final String urlString) {
        if (urlString != null) {
            Uri uri = Uri.parse(urlString);
            if (uri.getHost() != null) {
                return uri.getHost();
            }
        }
        return "";
    }

    /**
     * Convert IDN names to punycode if necessary
     */
    public static String convertUrlToPunycodeIfNeeded(String url) {
        if (!Charset.forName("US-ASCII").newEncoder().canEncode(url)) {
            if (url.toLowerCase().startsWith("http://")) {
                url = "http://" + IDN.toASCII(url.substring(7));
            } else if (url.toLowerCase().startsWith("https://")) {
                url = "https://" + IDN.toASCII(url.substring(8));
            } else {
                url = IDN.toASCII(url);
            }
        }
        return url;
    }

    /**
     * Remove leading double slash, and inherit protocol scheme
     */
    public static String removeLeadingDoubleSlash(String url, String scheme) {
        if (url != null && url.startsWith("//")) {
            url = url.substring(2);
            if (scheme != null) {
                if (scheme.endsWith("://")){
                    url = scheme + url;
                } else {
                    AppLog.e(T.UTILS, "Invalid scheme used: " + scheme);
                }
            }
        }
        return url;
    }

    /**
     * Add scheme prefix to an URL. This method must be called on all user entered or server fetched URLs to ensure
     * http client will work as expected.
     *
     * @param url url entered by the user or fetched from a server
     * @param addHttps true and the url is not starting with http://, it will make the url starts with https://
     * @return url prefixed by http:// or https://
     */
    public static String addUrlSchemeIfNeeded(String url, boolean addHttps) {
        if (url == null) {
            return null;
        }

        // Remove leading double slash (eg. //example.com), needed for some wporg instances configured to
        // switch between http or https
        url = removeLeadingDoubleSlash(url, (addHttps ? "https" : "http") + "://");

        // If the URL is a valid http or https URL, we're good to go
        if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {
            return url;
        }

        // Else, remove the old scheme and prefix it by https:// or http://
        return (addHttps ? "https" : "http") + "://" + removeScheme(url);
    }

    /**
     * normalizes a URL, primarily for comparison purposes, for example so that
     * normalizeUrl("http://google.com/") = normalizeUrl("http://google.com")
     */
    public static String normalizeUrl(final String urlString) {
        if (urlString == null) {
            return null;
        }

        // this routine is called from some performance-critical code and creating a URI from a string
        // is slow, so skip it when possible - if we know it's not a relative path (and 99.9% of the
        // time it won't be for our purposes) then we can normalize it without java.net.URI.normalize()
        if (urlString.startsWith("http") &&
                !urlString.contains("build/intermediates/exploded-aar/org.wordpress/graphview/3.1.1")) {
            // return without a trailing slash
            if (urlString.endsWith("/")) {
                return urlString.substring(0, urlString.length() - 1);
            }
            return urlString;
        }

        // url is relative, so fall back to using slower java.net.URI normalization
        try {
            URI uri = URI.create(urlString);
            return uri.normalize().toString();
        } catch (IllegalArgumentException e) {
            return urlString;
        }
    }


    /**
     * returns the passed url without the scheme
     */
    public static String removeScheme(final String urlString) {
        if (urlString == null) {
            return null;
        }

        int doubleslash = urlString.indexOf("//");
        if (doubleslash == -1) {
            doubleslash = 0;
        } else {
            doubleslash += 2;
        }

        return urlString.substring(doubleslash, urlString.length());
    }

    /**
     * returns the passed url without the query parameters
     */
    public static String removeQuery(final String urlString) {
        if (urlString == null) {
            return null;
        }
        return Uri.parse(urlString).buildUpon().clearQuery().toString();
    }

    /**
     * returns true if passed url is https:
     */
    public static boolean isHttps(final String urlString) {
        return (urlString != null && urlString.startsWith("https:"));
    }

    public static boolean isHttps(URL url) {
        return url != null && "https".equals(url.getProtocol());
    }

    public static boolean isHttps(URI uri) {
        if (uri == null) return false;

        String protocol = uri.getScheme();
        return protocol != null && protocol.equals("https");
    }

    /**
     * returns https: version of passed http: url
     */
    public static String makeHttps(final String urlString) {
        if (urlString == null || !urlString.startsWith("http:")) {
            return urlString;
        }
        return "https:" + urlString.substring(5, urlString.length());
    }

    /**
     * see http://stackoverflow.com/a/8591230/1673548
     */
    public static String getUrlMimeType(final String urlString) {
        if (urlString == null) {
            return null;
        }

        String extension = MimeTypeMap.getFileExtensionFromUrl(urlString);
        if (extension == null) {
            return null;
        }

        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String mimeType = mime.getMimeTypeFromExtension(extension);
        if (mimeType == null) {
            return null;
        }

        return mimeType;
    }

    /**
     * returns false if the url is not valid or if the url host is null, else true
     */
    public static boolean isValidUrlAndHostNotNull(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    // returns true if the passed url is for an image
    public static boolean isImageUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;

        String cleanedUrl = removeQuery(url.toLowerCase());

        return cleanedUrl.endsWith("jpg") || cleanedUrl.endsWith("jpeg") ||
                cleanedUrl.endsWith("gif") || cleanedUrl.endsWith("png");
    }

    public static String appendUrlParameter(String url, String paramName, String paramValue) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(paramName, paramValue);
        return appendUrlParameters(url, parameters);
    }

    public static String appendUrlParameters(String url, Map<String, String> parameters) {
        Uri.Builder uriBuilder = Uri.parse(url).buildUpon();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            uriBuilder.appendQueryParameter(parameter.getKey(), parameter.getValue());
        }
        return uriBuilder.build().toString();
    }
}
