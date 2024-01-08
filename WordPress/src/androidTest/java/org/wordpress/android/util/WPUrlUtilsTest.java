package org.wordpress.android.util;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class WPUrlUtilsTest {
    private static final String WPCOM_ADDRESS_1 = "http://wordpress.com/xmlrpc.php";
    private static final String WPCOM_ADDRESS_2 = "http://wordpress.com#.b.com/test";
    private static final String WPCOM_ADDRESS_3 = "http://wordpress.com/xmlrpc.php";
    private static final String WPCOM_ADDRESS_4 = "https://wordpress.com";
    private static final String WPCOM_ADDRESS_5 = "https://wordpress.com/test#test";
    private static final String WPCOM_ADDRESS_6 = "https://developer.wordpress.com";
    private static final String NOT_WPCOM_ADDRESS_1 =
            "http://i2.wp.com.eritreo.it#.files.wordpress.com/testpicture.gif?strip=all&quality=100&resize=1024,768";
    private static final String NOT_WPCOM_ADDRESS_2 = "wordpress.com";
    private static final String NOT_WPCOM_ADDRESS_3 = "https://thisisnotwordpress.com";

    @Test
    public void testSafeToAddAuthToken1() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(WPCOM_ADDRESS_1));
    }

    @Test
    public void testSafeToAddAuthToken2() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(WPCOM_ADDRESS_2));
    }

    @Test
    public void testSafeToAddAuthToken3() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(WPCOM_ADDRESS_3));
    }

    @Test
    public void testSafeToAddAuthToken4() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(WPCOM_ADDRESS_4));
    }

    @Test
    public void testSafeToAddAuthToken5() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(WPCOM_ADDRESS_5));
    }

    @Test
    public void testSafeToAddAuthToken6() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(NOT_WPCOM_ADDRESS_1));
    }

    @Test
    public void testSafeToAddAuthToken7() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(NOT_WPCOM_ADDRESS_2));
    }

    @Test
    public void testSafeToAddAuthToken8() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(WPCOM_ADDRESS_1)));
    }

    @Test
    public void testSafeToAddAuthToken9() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(WPCOM_ADDRESS_2)));
    }

    @Test
    public void testSafeToAddAuthToken10() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(WPCOM_ADDRESS_3)));
    }

    @Test
    public void testSafeToAddAuthToken11() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(WPCOM_ADDRESS_4)));
    }

    @Test
    public void testSafeToAddAuthToken12() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(WPCOM_ADDRESS_5)));
    }

    @Test
    public void testSafeToAddAuthToken13() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(NOT_WPCOM_ADDRESS_1)));
    }

    @Test
    public void testSafeToAddAuthToken14() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(NOT_WPCOM_ADDRESS_2)));
    }

    @Test
    public void testSafeToAddAuthToken15() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(WPCOM_ADDRESS_1)));
    }

    @Test
    public void testSafeToAddAuthToken16() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(WPCOM_ADDRESS_2)));
    }

    @Test
    public void testSafeToAddAuthToken17() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(WPCOM_ADDRESS_3)));
    }

    @Test
    public void testSafeToAddAuthToken18() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(WPCOM_ADDRESS_4)));
    }

    @Test
    public void testSafeToAddAuthToken19() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(WPCOM_ADDRESS_5)));
    }

    @Test
    public void testSafeToAddAuthToken20() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(NOT_WPCOM_ADDRESS_1)));
    }

    @Test
    public void testSafeToAddAuthToken21() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(NOT_WPCOM_ADDRESS_2)));
    }

    @Test
    public void testSafeToAddAuthToken22() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(NOT_WPCOM_ADDRESS_3));
    }

    @Test
    public void testSafeToAddAuthToken23() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(NOT_WPCOM_ADDRESS_3)));
    }

    @Test
    public void testSafeToAddAuthToken24() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(NOT_WPCOM_ADDRESS_3)));
    }

    @Test
    public void testSafeToAddAuthToken25() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(WPCOM_ADDRESS_6));
    }

    @Test
    public void testSafeToAddAuthToken26() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(WPCOM_ADDRESS_6)));
    }

    @Test
    public void testSafeToAddAuthToken27() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(WPCOM_ADDRESS_6)));
    }

    @Test
    public void testIsWPCOMString1() {
        assertTrue(WPUrlUtils.isWordPressCom(WPCOM_ADDRESS_1));
    }

    @Test
    public void testIsWPCOMString2() {
        assertTrue(WPUrlUtils.isWordPressCom(WPCOM_ADDRESS_2));
    }

    @Test
    public void testIsWPCOMString3() {
        assertTrue(WPUrlUtils.isWordPressCom(WPCOM_ADDRESS_3));
    }

    @Test
    public void testIsWPCOMString4() {
        assertTrue(WPUrlUtils.isWordPressCom(WPCOM_ADDRESS_4));
    }

    @Test
    public void testIsWPCOMString5() {
        assertTrue(WPUrlUtils.isWordPressCom(WPCOM_ADDRESS_5));
    }

    @Test
    public void testIsWPCOMString6() {
        assertTrue(WPUrlUtils.isWordPressCom(WPCOM_ADDRESS_6));
    }

    private URL buildURL(String address) {
        URL url = null;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
        }
        return url;
    }

    @Test
    public void testIsWPCOMURL1() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(WPCOM_ADDRESS_1)));
    }

    @Test
    public void testIsWPCOMURL2() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(WPCOM_ADDRESS_2)));
    }

    @Test
    public void testIsWPCOMURL3() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(WPCOM_ADDRESS_3)));
    }

    @Test
    public void testIsWPCOMURL4() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(WPCOM_ADDRESS_4)));
    }

    @Test
    public void testIsWPCOMURL5() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(WPCOM_ADDRESS_5)));
    }

    @Test
    public void testIsWPCOMURL6() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(WPCOM_ADDRESS_6)));
    }


    private URI buildURI(String address) {
        URI uri = null;
        try {
            uri = new URI(address);
        } catch (URISyntaxException e) {
        }
        return uri;
    }

    @Test
    public void testIsWPCOMURI1() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(WPCOM_ADDRESS_1)));
    }

    @Test
    public void testIsWPCOMURI2() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(WPCOM_ADDRESS_2)));
    }

    @Test
    public void testIsWPCOMURI3() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(WPCOM_ADDRESS_3)));
    }

    @Test
    public void testIsWPCOMURI4() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(WPCOM_ADDRESS_4)));
    }

    @Test
    public void testIsWPCOMURI5() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(WPCOM_ADDRESS_5)));
    }

    @Test
    public void testIsWPCOMURI6() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(WPCOM_ADDRESS_6)));
    }

    @Test
    public void testIsNOTWPCOM1() {
        assertFalse(WPUrlUtils.isWordPressCom(NOT_WPCOM_ADDRESS_1));
    }

    @Test
    public void testIsNOTWPCOM2() {
        assertFalse(WPUrlUtils.isWordPressCom(NOT_WPCOM_ADDRESS_2));
    }

    @Test
    public void testIsNOTWPCOM3() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURL(NOT_WPCOM_ADDRESS_1)));
    }

    @Test
    public void testIsNOTWPCOM4() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURL(NOT_WPCOM_ADDRESS_2)));
    }

    @Test
    public void testIsNOTWPCOM5() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURI(NOT_WPCOM_ADDRESS_1)));
    }

    @Test
    public void testIsNOTWPCOM6() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURI(NOT_WPCOM_ADDRESS_2)));
    }

    @Test
    public void testIsNOTWPCOM7() {
        assertFalse(WPUrlUtils.isWordPressCom(NOT_WPCOM_ADDRESS_3));
    }

    @Test
    public void testIsNOTWPCOM8() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURL(NOT_WPCOM_ADDRESS_3)));
    }

    @Test
    public void testIsNOTWPCOM9() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURI(NOT_WPCOM_ADDRESS_3)));
    }
}
