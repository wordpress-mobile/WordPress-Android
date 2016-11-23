package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class WPUrlUtilsTest extends InstrumentationTestCase {

    private static final String wpcomAddress1 = "http://wordpress.com/xmlrpc.php";
    private static final String wpcomAddress2 = "http://wordpress.com#.b.com/test";
    private static final String wpcomAddress3 = "http://wordpress.com/xmlrpc.php";
    private static final String wpcomAddress4 = "https://wordpress.com";
    private static final String wpcomAddress5 = "https://wordpress.com/test#test";
    private static final String wpcomAddress6 = "https://developer.wordpress.com";
    private static final String notWpcomAddress1 = "http://i2.wp.com.eritreo.it#.files.wordpress.com/testpicture.gif?strip=all&quality=100&resize=1024,768";
    private static final String notWpcomAddress2 = "wordpress.com";
    private static final String notWpcomAddress3 = "https://thisisnotwordpress.com";


    public void testSafeToAddAuthToken1() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(wpcomAddress1));
    }

    public void testSafeToAddAuthToken2() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(wpcomAddress2));
    }

    public void testSafeToAddAuthToken3() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(wpcomAddress3));
    }

    public void testSafeToAddAuthToken4() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(wpcomAddress4));
    }
    public void testSafeToAddAuthToken5() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(wpcomAddress5));
    }

    public void testSafeToAddAuthToken6() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(notWpcomAddress1));
    }

    public void testSafeToAddAuthToken7() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(notWpcomAddress2));
    }

    public void testSafeToAddAuthToken8() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(wpcomAddress1)));
    }

    public void testSafeToAddAuthToken9() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(wpcomAddress2)));
    }

    public void testSafeToAddAuthToken10() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(wpcomAddress3)));
    }

    public void testSafeToAddAuthToken11() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(wpcomAddress4)));
    }
    public void testSafeToAddAuthToken12() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(wpcomAddress5)));
    }

    public void testSafeToAddAuthToken13() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(notWpcomAddress1)));
    }

    public void testSafeToAddAuthToken14() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(notWpcomAddress2)));
    }

    public void testSafeToAddAuthToken15() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(wpcomAddress1)));
    }

    public void testSafeToAddAuthToken16() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(wpcomAddress2)));
    }

    public void testSafeToAddAuthToken17() {
        // Not HTTPS
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(wpcomAddress3)));
    }

    public void testSafeToAddAuthToken18() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(wpcomAddress4)));
    }
    public void testSafeToAddAuthToken19() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(wpcomAddress5)));
    }

    public void testSafeToAddAuthToken20() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(notWpcomAddress1)));
    }

    public void testSafeToAddAuthToken21() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(notWpcomAddress2)));
    }

    public void testSafeToAddAuthToken22() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(notWpcomAddress3));
    }

    public void testSafeToAddAuthToken23() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(notWpcomAddress3)));
    }

    public void testSafeToAddAuthToken24() {
        // Not wpcom
        assertFalse(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(notWpcomAddress3)));
    }

    public void testSafeToAddAuthToken25() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(wpcomAddress6));
    }

    public void testSafeToAddAuthToken26() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURL(wpcomAddress6)));
    }

    public void testSafeToAddAuthToken27() {
        assertTrue(WPUrlUtils.safeToAddWordPressComAuthToken(buildURI(wpcomAddress6)));
    }


    public void testIsWPCOMString1() {
        assertTrue(WPUrlUtils.isWordPressCom(wpcomAddress1));
    }

    public void testIsWPCOMString2() {
        assertTrue(WPUrlUtils.isWordPressCom(wpcomAddress2));
    }

    public void testIsWPCOMString3() {
        assertTrue(WPUrlUtils.isWordPressCom(wpcomAddress3));
    }

    public void testIsWPCOMString4() {
        assertTrue(WPUrlUtils.isWordPressCom(wpcomAddress4));
    }

    public void testIsWPCOMString5() {
        assertTrue(WPUrlUtils.isWordPressCom(wpcomAddress5));
    }

    public void testIsWPCOMString6() {
        assertTrue(WPUrlUtils.isWordPressCom(wpcomAddress6));
    }

    private URL buildURL(String address) {
        URL url = null;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {}
        return url;
    }

    public void testIsWPCOMURL1() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(wpcomAddress1)));
    }

    public void testIsWPCOMURL2() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(wpcomAddress2)));
    }

    public void testIsWPCOMURL3() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(wpcomAddress3)));
    }

    public void testIsWPCOMURL4() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(wpcomAddress4)));
    }

    public void testIsWPCOMURL5() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(wpcomAddress5)));
    }

    public void testIsWPCOMURL6() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURL(wpcomAddress6)));
    }


    private URI buildURI(String address) {
        URI uri = null;
        try {
            uri = new URI(address);
        } catch (URISyntaxException e) {}
        return uri;
    }

    public void testIsWPCOMURI1() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(wpcomAddress1)));
    }

    public void testIsWPCOMURI2() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(wpcomAddress2)));
    }

    public void testIsWPCOMURI3() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(wpcomAddress3)));
    }

    public void testIsWPCOMURI4() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(wpcomAddress4)));
    }

    public void testIsWPCOMURI5() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(wpcomAddress5)));
    }

    public void testIsWPCOMURI6() {
        assertTrue(WPUrlUtils.isWordPressCom(buildURI(wpcomAddress6)));
    }

    public void testIsNOTWPCOM1() {
        assertFalse(WPUrlUtils.isWordPressCom(notWpcomAddress1));
    }

    public void testIsNOTWPCOM2() {
        assertFalse(WPUrlUtils.isWordPressCom(notWpcomAddress2));
    }

    public void testIsNOTWPCOM3() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURL(notWpcomAddress1)));
    }

    public void testIsNOTWPCOM4() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURL(notWpcomAddress2)));
    }

    public void testIsNOTWPCOM5() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURI(notWpcomAddress1)));
    }

    public void testIsNOTWPCOM6() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURI(notWpcomAddress2)));
    }

    public void testIsNOTWPCOM7() {
        assertFalse(WPUrlUtils.isWordPressCom(notWpcomAddress3));
    }

    public void testIsNOTWPCOM8() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURL(notWpcomAddress3)));
    }

    public void testIsNOTWPCOM9() {
        assertFalse(WPUrlUtils.isWordPressCom(buildURI(notWpcomAddress3)));
    }

}