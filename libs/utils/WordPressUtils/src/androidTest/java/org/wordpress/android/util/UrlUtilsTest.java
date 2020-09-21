package org.wordpress.android.util;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UrlUtilsTest {
    @Test
    public void testGetDomainFromUrlWithEmptyStringDoesNotReturnNull() {
        assertNotNull(UrlUtils.getHost(""));
    }

    @Test
    public void testGetDomainFromUrlWithNoHostDoesNotReturnNull() {
        assertNotNull(UrlUtils.getHost("wordpress"));
    }

    @Test
    public void testGetDomainFromUrlWithHostReturnsHost() {
        String url = "http://www.wordpress.com";
        String host = UrlUtils.getHost(url);

        assertTrue(host.equals("www.wordpress.com"));
    }

    @Test
    public void testAppendUrlParameter1() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test", "preview", "true");
        assertEquals("http://wp.com/test?preview=true", url);
    }

    @Test
    public void testAppendUrlParameter2() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test?q=pony", "preview", "true");
        assertEquals("http://wp.com/test?q=pony&preview=true", url);
    }

    @Test
    public void testAppendUrlParameter3() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test?q=pony#unicorn", "preview", "true");
        assertEquals("http://wp.com/test?q=pony&preview=true#unicorn", url);
    }

    @Test
    public void testAppendUrlParameter4() {
        String url = UrlUtils.appendUrlParameter("/relative/test", "preview", "true");
        assertEquals("/relative/test?preview=true", url);
    }

    @Test
    public void testAppendUrlParameter5() {
        String url = UrlUtils.appendUrlParameter("/relative/", "preview", "true");
        assertEquals("/relative/?preview=true", url);
    }

    @Test
    public void testAppendUrlParameter6() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test/", "preview", "true");
        assertEquals("http://wp.com/test/?preview=true", url);
    }

    @Test
    public void testAppendUrlParameter7() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test/?q=pony", "preview", "true");
        assertEquals("http://wp.com/test/?q=pony&preview=true", url);
    }

    @Test
    public void testAppendUrlParameters1() {
        Map<String, String> params = new HashMap<>();
        params.put("w", "200");
        params.put("h", "300");
        String url = UrlUtils.appendUrlParameters("http://wp.com/test", params);
        if (!url.equals("http://wp.com/test?h=300&w=200") && !url.equals("http://wp.com/test?w=200&h=300")) {
            assertTrue("failed test on url: " + url, false);
        }
    }

    @Test
    public void testAppendUrlParameters2() {
        Map<String, String> params = new HashMap<>();
        params.put("h", "300");
        params.put("w", "200");
        String url = UrlUtils.appendUrlParameters("/relative/test", params);
        if (!url.equals("/relative/test?h=300&w=200") && !url.equals("/relative/test?w=200&h=300")) {
            assertTrue("failed test on url: " + url, false);
        }
    }

    @Test
    public void testHttps1() {
        assertFalse(UrlUtils.isHttps(buildURL("http://wordpress.com/xmlrpc.php")));
    }

    @Test
    public void testHttps2() {
        assertFalse(UrlUtils.isHttps(buildURL("http://wordpress.com#.b.com/test")));
    }

    @Test
    public void testHttps3() {
        assertFalse(UrlUtils.isHttps(buildURL("http://wordpress.com/xmlrpc.php")));
    }

    @Test
    public void testHttps4() {
        assertTrue(UrlUtils.isHttps(buildURL("https://wordpress.com")));
    }

    @Test
    public void testHttps5() {
        assertTrue(UrlUtils.isHttps(buildURL("https://wordpress.com/test#test")));
    }

    private URL buildURL(String address) {
        URL url = null;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
        }
        return url;
    }
}
