package org.wordpress.android.networking;

import android.test.InstrumentationTestCase;

public class AuthenticatorRequestTest extends InstrumentationTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testExtractSiteIdFromUrl1() {
        String url = "";
        assertEquals(null, AuthenticatorRequest.extractSiteIdFromUrl(url));
    }

    public void testExtractSiteIdFromUrl2() {
        String url = null;
        assertEquals(null, AuthenticatorRequest.extractSiteIdFromUrl(url));
    }

    public void testExtractSiteIdFromUrl3() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F57991476%2Fstats%2Freferrers%3Fdate%3D2014-05-08&urls%5B%5D=%2Fsites%2F57991476%2Fstats%2Freferrers%3Fdate%3D2014-05-07";
        assertEquals("57991476", AuthenticatorRequest.extractSiteIdFromUrl(url));
    }

    public void testExtractSiteIdFromUrl4() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/59073674/stats";
        assertEquals("59073674", AuthenticatorRequest.extractSiteIdFromUrl(url));
    }

    public void testExtractSiteIdFromUrl5() {
        String url = "https://public-api.wordpress.com/rest/v1/sites//stats";
        assertEquals("", AuthenticatorRequest.extractSiteIdFromUrl(url));
    }

    public void testExtractSiteIdFromUrl6() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F";
        assertEquals(null, AuthenticatorRequest.extractSiteIdFromUrl(url));
    }

    public void testExtractSiteIdFromUrl7() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/";
        assertEquals(null, AuthenticatorRequest.extractSiteIdFromUrl(url));
    }
}