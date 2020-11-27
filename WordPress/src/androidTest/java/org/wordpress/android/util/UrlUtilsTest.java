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

    public void testIsNotHomePage() {
        assertEquals(false, UrlUtils.isHomePage("https://a.com/slug/?param1=true"));
    }

    public void testIsNotHomePage2() {
        assertEquals(false, UrlUtils.isHomePage(null));
    }

    public void testIsHomePage1() {
        assertEquals(true, UrlUtils.isHomePage("https://a.com/?param1=true"));
    }

    public void testIsHomePage2() {
        assertEquals(true, UrlUtils.isHomePage("https://a.com?param1=true"));
    }

    public void testAppendToPath1() {
        String result = UrlUtils.appendToPath("https://a.com?param1=true", "slug");
        assertEquals("https://a.com/slug?param1=true", result);
    }

    public void testAppendToPath2() {
        String result = UrlUtils.appendToPath("https://a.com/?param1=true", "slug");
        assertEquals("https://a.com/slug?param1=true", result);
    }

    public void testAppendToPath3() {
        String result = UrlUtils.appendToPath("https://a.com/path/?param1=true", "slug");
        assertEquals("https://a.com/path/slug?param1=true", result);
    }
}
