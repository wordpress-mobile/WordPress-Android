package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

public class UrlUtilsTest extends InstrumentationTestCase {
    public void testGetHost1() {
        assertEquals("a.com", UrlUtils.getHost("http://a.com/test"));
    }

    public void testGetHost2() {
        assertEquals("a.com", UrlUtils.getHost("http://a.com#.b.com/test"));
    }

    public void testGetHost3() {
        assertEquals("a.com", UrlUtils.getHost("https://a.com"));
    }

    public void testGetHost4() {
        assertEquals("a.com", UrlUtils.getHost("https://a.com/test#test"));
    }

    public void testGetHost5() {
        assertEquals("", UrlUtils.getHost("a.com"));
    }
}
