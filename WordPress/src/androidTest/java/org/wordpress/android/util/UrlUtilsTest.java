package org.wordpress.android.util;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class UrlUtilsTest {
    @Test
    public void testGetHost1() {
        assertEquals("a.com", UrlUtils.getHost("http://a.com/test"));
    }

    @Test
    public void testGetHost2() {
        assertEquals("a.com", UrlUtils.getHost("http://a.com#.b.com/test"));
    }

    @Test
    public void testGetHost3() {
        assertEquals("a.com", UrlUtils.getHost("https://a.com"));
    }

    @Test
    public void testGetHost4() {
        assertEquals("a.com", UrlUtils.getHost("https://a.com/test#test"));
    }

    @Test
    public void testGetHost5() {
        assertEquals("", UrlUtils.getHost("a.com"));
    }
}
