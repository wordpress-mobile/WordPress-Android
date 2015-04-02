package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

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
}
