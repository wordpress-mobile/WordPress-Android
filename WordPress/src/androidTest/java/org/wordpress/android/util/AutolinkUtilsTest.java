package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

public class AutolinkUtilsTest extends InstrumentationTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testNullString() {
        AutolinkUtils.autoCreateLinks(null);
    }

    public void testEmptyString() {
        String sourceTest = "";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    public void testNonBlacklistedUrl1() {
        String sourceTest = "http://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    public void testNonBlacklistedUrl2() {
        String sourceTest = "http://test.com http://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a> <a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    public void testNonBlacklistedUrl3() {
        String sourceTest = "http://test.com\nhttp://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a>\n<a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    public void testBlacklistedUrl1() {
        String sourceTest = "http://youtube.com/watch?test";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    public void testMixedUrls1() {
        String sourceTest = "hey http://youtube.com/watch?test salut http://test.com hello";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "hey http://youtube.com/watch?test salut <a href=\"http://test.com\">http://test.com</a> hello";
        assertEquals(expected, output);
    }

    public void testExistingAHref1() {
        String sourceTest = "<a href=\"http://test.com\">http://test.com</a>";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    public void testUndetectable1() {
        String sourceTest = "testhttp://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    public void testUndetectable2() {
        String sourceTest = "\"http://test.com\"";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    public void testMixedUrls2() {
        String sourceTest = "http://test.com http://www.youtube.com/watch?test http://test.com http://youtu.be/wat";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a> http://www.youtube.com/watch?test <a href=\"http://test.com\">http://test.com</a> http://youtu.be/wat";
        assertEquals(expected, output);
    }
}
