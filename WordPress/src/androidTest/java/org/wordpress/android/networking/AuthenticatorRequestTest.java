package org.wordpress.android.networking;

import android.test.InstrumentationTestCase;

import com.wordpress.rest.RestClient;

import org.wordpress.android.FactoryUtils;

public class AuthenticatorRequestTest extends InstrumentationTestCase {
    RestClient mRestClient;
    AuthenticatorRequest mAuthenticatorRequest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FactoryUtils.initWithTestFactories();
        mRestClient = RestClientFactory.instantiate(null);
        mAuthenticatorRequest = new AuthenticatorRequest(null, null, mRestClient, null);
    }

    @Override
    protected void tearDown() throws Exception {
        FactoryUtils.clearFactories();
        super.tearDown();
    }

    public void testExtractSiteIdFromUrl1() {
        String url = "";
        assertEquals(null, mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl2() {
        String url = null;
        assertEquals(null, mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl3() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F57991476%2Fstats%2Freferrers%3Fdate%3D2014-05-08&urls%5B%5D=%2Fsites%2F57991476%2Fstats%2Freferrers%3Fdate%3D2014-05-07";
        assertEquals("57991476", mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl4() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/59073674/stats";
        assertEquals("59073674", mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl5() {
        String url = "https://public-api.wordpress.com/rest/v1/sites//stats";
        assertEquals("", mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl6() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F";
        assertEquals(null, mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl7() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/";
        assertEquals(null, mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }
}