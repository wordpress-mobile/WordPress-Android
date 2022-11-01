package org.wordpress.android.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class AutolinkUtilsTest {
    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Before
    public void setUp() {
        hiltRule.inject();
    }

    @Test
    public void testNullString() {
        AutolinkUtils.autoCreateLinks(null);
    }

    @Test
    public void testEmptyString() {
        String sourceTest = "";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testNonDenylistedUrl1() {
        String sourceTest = "http://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    @Test
    public void testNonDenylistedUrl2() {
        String sourceTest = "http://test.com http://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected =
                "<a href=\"http://test.com\">http://test.com</a> <a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    @Test
    public void testNonDenylistedUrl3() {
        String sourceTest = "http://test.com\nhttp://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected =
                "<a href=\"http://test.com\">http://test.com</a>\n<a href=\"http://test.com\">http://test.com</a>";
        assertEquals(expected, output);
    }

    @Test
    public void testDenylistedUrl1() {
        String sourceTest = "http://youtube.com/watch?test";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testDenylistedUrlIgnoreCase1() {
        String sourceTest = "http://youtube.com/WATCH?test";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testDenylistedUrlKickStarter1() {
        String sourceTest = "testing https://www.kickstarter.com/projects/583173617/raspi-boy-retro-"
                            + "handheld-emulation-console-electro ponies";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testDenylistedUrlKickStarter2() {
        String sourceTest = "testing http://kck.st/2gNq7KK ponies";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testMixedUrls1() {
        String sourceTest = "hey http://youtube.com/watch?test salut http://test.com hello";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected =
                "hey http://youtube.com/watch?test salut <a href=\"http://test.com\">http://test.com</a> hello";
        assertEquals(expected, output);
    }

    @Test
    public void testExistingAHref1() {
        String sourceTest = "<a href=\"http://test.com\">http://test.com</a>";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testUndetectable1() {
        String sourceTest = "testhttp://test.com";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testUndetectable2() {
        String sourceTest = "\"http://test.com\"";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        assertEquals(sourceTest, output);
    }

    @Test
    public void testMixedUrls2() {
        String sourceTest = "http://test.com http://www.youtube.com/watch?test http://test.com http://youtu.be/wat";
        String output = AutolinkUtils.autoCreateLinks(sourceTest);
        String expected = "<a href=\"http://test.com\">http://test.com</a> http://www.youtube.com/watch?test "
                          + "<a href=\"http://test.com\">http://test.com</a> http://youtu.be/wat";
        assertEquals(expected, output);
    }
}
