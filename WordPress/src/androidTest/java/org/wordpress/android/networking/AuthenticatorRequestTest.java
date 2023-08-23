package org.wordpress.android.networking;

import com.wordpress.rest.RestClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wordpress.android.FactoryUtils;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class AuthenticatorRequestTest {
    RestClient mRestClient;
    AuthenticatorRequest mAuthenticatorRequest;

    @Before
    public void setUp() {
        FactoryUtils.initWithTestFactories();
        mRestClient = RestClientFactory.instantiate(null);
        mAuthenticatorRequest = new AuthenticatorRequest(null, mRestClient, null);
    }

    @After
    public void tearDown() {
        FactoryUtils.clearFactories();
    }

    @Test
    public void testExtractSiteIdFromUrl1() {
        String url = "";
        assertNull(mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl2() {
        String url = null;
        assertNull(mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl3() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F57991476%2Fstats%2F"
                     + "referrers%3Fdate%3D2014-05-08&urls%5B%5D=%2Fsites%2F57991476%2Fstats%2F"
                     + "referrers%3Fdate%3D2014-05-07";
        assertEquals("57991476", mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl4() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/59073674/stats";
        assertEquals("59073674", mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl5() {
        String url = "https://public-api.wordpress.com/rest/v1/sites//stats";
        assertEquals("", mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    public void testExtractSiteIdFromUrl6() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F";
        assertNull(mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl7() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/";
        assertNull(mAuthenticatorRequest.extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }
}
