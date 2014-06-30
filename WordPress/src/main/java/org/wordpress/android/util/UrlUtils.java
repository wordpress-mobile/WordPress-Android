package org.wordpress.android.util;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

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

    public static String getDomainFromUrl(final String urlString) {
        if (urlString == null) {
            return "";
        }
        Uri uri = Uri.parse(urlString);
        return uri.getHost();
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

    public static String addUrlSchemeIfNeeded(String url, boolean isHTTPS) {
        if (url == null) {
            return null;
        }

        if (!URLUtil.isValidUrl(url)) {
            if (!(url.toLowerCase().startsWith("http://")) && !(url.toLowerCase().startsWith("https://"))) {
                url = (isHTTPS ? "https" : "http") + "://" + url;
            }
        }

        return url;
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
        if (urlString.startsWith("http") && !urlString.contains("..")) {
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
     * returns the passed url without the query parameters
     */
    public static String removeQuery(final String urlString) {
        if (urlString == null) {
            return null;
        }
        int pos = urlString.indexOf("?");
        if (pos == -1) {
            return urlString;
        }
        return urlString.substring(0, pos);
    }

    /**
     * returns true if passed url is https:
     */
    public static boolean isHttps(final String urlString) {
        return (urlString != null && urlString.startsWith("https:"));
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
}
