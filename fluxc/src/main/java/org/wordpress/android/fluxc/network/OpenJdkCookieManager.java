package org.wordpress.android.fluxc.network;

import android.os.Build;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenJdkCookieManager extends CookieManager {
    /* ---------------- Fields -------------- */

    private CookiePolicy policyCallback;

    /* ---------------- Ctors -------------- */

    /**
     * Create a new cookie manager.
     *
     * <p>This constructor will create new cookie manager with default
     * cookie store and accept policy. The effect is same as
     * {@code CookieManager(null, null)}.
     */
    public OpenJdkCookieManager() {
        super(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        this.policyCallback = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
    }

    /**
     * Create a new cookie manager with specified cookie store and cookie policy.
     *
     * @param store     a {@code CookieStore} to be used by cookie manager.
     *                  if {@code null}, cookie manager will use a default one,
     *                  which is an in-memory CookieStore implementation.
     * @param cookiePolicy      a {@code CookiePolicy} instance
     *                          to be used by cookie manager as policy callback.
     */
    public OpenJdkCookieManager(CookieStore store,
                                @NonNull CookiePolicy cookiePolicy)
    {
        super(store, cookiePolicy);
        this.policyCallback = cookiePolicy;
    }

    public Map<String, List<String>>
    get(URI uri, Map<String, List<String>> requestHeaders)
            throws IOException {
        // pre-condition check
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        Map<String, List<String>> cookieMap = new java.util.HashMap<>();
        // if there's no default CookieStore, no way for us to get any cookie
        if (getCookieStore() == null)
            return Collections.unmodifiableMap(cookieMap);

        boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
        List<HttpCookie> cookies = new java.util.ArrayList<>();
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        for (HttpCookie cookie : getCookieStore().get(uri)) {
            // apply path-matches rule (RFC 2965 sec. 3.3.4)
            // and check for the possible "secure" tag (i.e. don't send
            // 'secure' cookies over unsecure links)
            if (pathMatches(path, cookie.getPath()) &&
                    (secureLink || !cookie.getSecure())) {
                // Enforce httponly attribute
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cookie.isHttpOnly()) {
                    String s = uri.getScheme();
                    if (!"http".equalsIgnoreCase(s) && !"https".equalsIgnoreCase(s)) {
                        continue;
                    }
                }
                // Let's check the authorize port list if it exists
                String ports = cookie.getPortlist();
                if (ports != null && !ports.isEmpty()) {
                    int port = uri.getPort();
                    if (port == -1) {
                        port = "https".equals(uri.getScheme()) ? 443 : 80;
                    }
                    if (isInPortList(ports, port)) {
                        cookies.add(cookie);
                    }
                } else {
                    cookies.add(cookie);
                }
            }
        }

        // apply sort rule (RFC 2965 sec. 3.3.4)
        List<String> cookieHeader = sortByPathAndAge(cookies);

        cookieMap.put("Cookie", cookieHeader);
        return Collections.unmodifiableMap(cookieMap);
    }

    public void
    put(URI uri, Map<String, List<String>> responseHeaders)
            throws IOException {
        // pre-condition check
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }


        // if there's no default CookieStore, no need to remember any cookie
        if (getCookieStore() == null)
            return;

        for (String headerKey : responseHeaders.keySet()) {
            // RFC 2965 3.2.2, key must be 'Set-Cookie2'
            // we also accept 'Set-Cookie' here for backward compatibility
            if (headerKey == null
                    || !(headerKey.equalsIgnoreCase("Set-Cookie2")
                    || headerKey.equalsIgnoreCase("Set-Cookie")
            )
            ) {
                continue;
            }

            for (String headerValue : responseHeaders.get(headerKey)) {
                try {
                    List<HttpCookie> cookies;
                    try {
                        cookies = HttpCookie.parse(headerValue);
                    } catch (IllegalArgumentException e) {
                        // Bogus header, make an empty list and log the error
                        cookies = java.util.Collections.emptyList();
                    }
                    for (HttpCookie cookie : cookies) {
                        if (cookie.getPath() == null) {
                            // If no path is specified, then by default
                            // the path is the directory of the page/doc
                            String path = uri.getPath();
                            if (!path.endsWith("/")) {
                                int i = path.lastIndexOf('/');
                                if (i > 0) {
                                    path = path.substring(0, i + 1);
                                } else {
                                    path = "/";
                                }
                            }
                            cookie.setPath(path);
                        }

                        // As per RFC 2965, section 3.3.1:
                        // Domain  Defaults to the effective request-host.  (Note that because
                        // there is no dot at the beginning of effective request-host,
                        // the default Domain can only domain-match itself.)
                        if (cookie.getDomain() == null) {
                            String host = uri.getHost();
                            if (host != null && !host.contains("."))
                                host += ".local";
                            cookie.setDomain(host);
                        }
                        String ports = cookie.getPortlist();
                        if (ports != null) {
                            int port = uri.getPort();
                            if (port == -1) {
                                port = "https".equals(uri.getScheme()) ? 443 : 80;
                            }
                            if (ports.isEmpty()) {
                                // Empty port list means this should be restricted
                                // to the incoming URI port
                                cookie.setPortlist("" + port);
                                if (shouldAcceptInternal(uri, cookie)) {
                                    getCookieStore().add(uri, cookie);
                                }
                            } else {
                                // Only store cookies with a port list
                                // IF the URI port is in that list, as per
                                // RFC 2965 section 3.3.2
                                if (isInPortList(ports, port) &&
                                        shouldAcceptInternal(uri, cookie)) {
                                    getCookieStore().add(uri, cookie);
                                }
                            }
                        } else {
                            if (shouldAcceptInternal(uri, cookie)) {
                                getCookieStore().add(uri, cookie);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // invalid set-cookie header string
                    // no-op
                }
            }
        }
    }

    public void setPolicyCallback(CookiePolicy policyCallback) {
        super.setCookiePolicy(policyCallback);
        this.policyCallback = policyCallback;
    }

    /* ---------------- Private operations -------------- */

    // to determine whether or not accept this cookie
    private boolean shouldAcceptInternal(URI uri, HttpCookie cookie) {
        try {
            return policyCallback.shouldAccept(uri, cookie);
        } catch (Exception ignored) { // protect against malicious callback
            return false;
        }
    }


    private static boolean isInPortList(String lst, int port) {
        int i = lst.indexOf(',');
        int val = -1;
        while (i > 0) {
            try {
                val = Integer.parseInt(lst.substring(0, i));
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException numberFormatException) {
            }
            lst = lst.substring(i + 1);
            i = lst.indexOf(',');
        }
        if (!lst.isEmpty()) {
            try {
                val = Integer.parseInt(lst);
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException numberFormatException) {
            }
        }
        return false;
    }

    /*
     * path-matches algorithm, as defined by RFC 2965
     */
    private boolean pathMatches(String path, String pathToMatchWith) {
        if (Objects.equals(path, pathToMatchWith))
            return true;
        if (path == null || pathToMatchWith == null)
            return false;
        if (path.startsWith(pathToMatchWith))
            return true;

        return false;
    }


    /*
     * sort cookies with respect to their path and age: those with more longer Path attributes
     * precede those with shorter, as defined in RFC 6265. Cookies with the same length
     * path are distinguished by creation time (older first). Method made PP to enable testing.
     */
    static List<String> sortByPathAndAge(List<HttpCookie> cookies) {
        Collections.sort(cookies, new CookiePathComparator());

        List<String> cookieHeader = new java.util.ArrayList<>();
        for (HttpCookie cookie : cookies) {
            // Netscape cookie spec and RFC 2965 have different format of Cookie
            // header; RFC 2965 requires a leading $Version="1" string while Netscape
            // does not.
            // The workaround here is to add a $Version="1" string in advance
            if (cookies.indexOf(cookie) == 0 && cookie.getVersion() > 0) {
                cookieHeader.add("$Version=\"1\"");
            }

            cookieHeader.add(cookie.toString());
        }
        return cookieHeader;
    }


    static class CookiePathComparator implements Comparator<HttpCookie> {
        public int compare(HttpCookie c1, HttpCookie c2) {
            if (c1 == c2) return 0;
            if (c1 == null) return -1;
            if (c2 == null) return 1;

            // path rule only applies to the cookies with same name
            if (!c1.getName().equals(c2.getName())) return 0;

            // those with more specific Path attributes precede those with less specific
            if (c1.getPath().startsWith(c2.getPath()))
                return -1;
            else if (c2.getPath().startsWith(c1.getPath()))
                return 1;
            else
                return 0;
        }
    }
}
