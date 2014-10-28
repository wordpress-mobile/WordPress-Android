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
        AutolinkUtils.autoCreateLinks("");
    }

    public void testNonBlacklistedUrl1() {
        String sourceTest = "http://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    public void testBlacklistedUrl1() {
        String sourceTest = "http://youtube.com/xxx";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    public void testMixedUrls1() {
        String sourceTest = "hey http://youtube.com/watch salut http://test.com hello";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "hey http://youtube.com/watch salut <a href=\"http://test.com\">http://test.com</a> hello";
        assertEquals(expected, output);
    }

    public void testMixedUrls2() {
        String sourceTest = "http://test.com http://www.youtube.com/wat http://test.com http://youtu.be/wat";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a> http://www.youtube.com/wat <a href=\"http://test.com\">http://test.com</a> http://youtu.be/wat";
        assertEquals(expected, output);
    }
}
