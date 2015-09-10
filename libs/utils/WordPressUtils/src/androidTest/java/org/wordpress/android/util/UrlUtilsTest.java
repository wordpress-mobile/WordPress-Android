package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

import java.util.HashMap;
import java.util.Map;

public class UrlUtilsTest extends InstrumentationTestCase {
    public void testGetDomainFromUrlWithEmptyStringDoesNotReturnNull() {
        assertNotNull(UrlUtils.getDomainFromUrl(""));
    }

    public void testGetDomainFromUrlWithNoHostDoesNotReturnNull() {
        assertNotNull(UrlUtils.getDomainFromUrl("wordpress"));
    }

    public void testGetDomainFromUrlWithHostReturnsHost() {
        String url = "http://www.wordpress.com";
        String host = UrlUtils.getDomainFromUrl(url);

        assertTrue(host.equals("www.wordpress.com"));
    }

    public void testAppendUrlParameter1() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test", "preview", "true");
        assertEquals("http://wp.com/test?preview=true", url);
    }

    public void testAppendUrlParameter2() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test?q=pony", "preview", "true");
        assertEquals("http://wp.com/test?q=pony&preview=true", url);
    }

    public void testAppendUrlParameter3() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test?q=pony#unicorn", "preview", "true");
        assertEquals("http://wp.com/test?q=pony&preview=true#unicorn", url);
    }

    public void testAppendUrlParameter4() {
        String url = UrlUtils.appendUrlParameter("/relative/test", "preview", "true");
        assertEquals("/relative/test?preview=true", url);
    }

    public void testAppendUrlParameter5() {
        String url = UrlUtils.appendUrlParameter("/relative/", "preview", "true");
        assertEquals("/relative/?preview=true", url);
    }

    public void testAppendUrlParameter6() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test/", "preview", "true");
        assertEquals("http://wp.com/test/?preview=true", url);
    }

    public void testAppendUrlParameter7() {
        String url = UrlUtils.appendUrlParameter("http://wp.com/test/?q=pony", "preview", "true");
        assertEquals("http://wp.com/test/?q=pony&preview=true", url);
    }

    public void testAppendUrlParameters1() {
        Map<String, String> params = new HashMap<>();
        params.put("w", "200");
        params.put("h", "300");
        String url = UrlUtils.appendUrlParameters("http://wp.com/test", params);
        assertEquals("http://wp.com/test?h=300&w=200", url);
    }

    public void testAppendUrlParameters2() {
        Map<String, String> params = new HashMap<>();
        params.put("h", "300");
        params.put("w", "200");
        String url = UrlUtils.appendUrlParameters("/relative/test", params);
        assertEquals("/relative/test?h=300&w=200", url);
    }
}
