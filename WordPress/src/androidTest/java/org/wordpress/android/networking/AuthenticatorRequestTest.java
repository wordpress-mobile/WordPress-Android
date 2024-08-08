package org.wordpress.android.networking;

import com.wordpress.rest.RestClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wordpress.android.FactoryUtils;

import dagger.hilt.android.testing.HiltAndroidTest;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

@HiltAndroidTest
public class AuthenticatorRequestTest {
    RestClient mRestClient;

    @Before
    public void setUp() {
        FactoryUtils.initWithTestFactories();
        mRestClient = RestClientFactory.instantiate(null);
    }

    @After
    public void tearDown() {
        FactoryUtils.clearFactories();
    }

    @Test
    public void testExtractSiteIdFromUrl1() {
        String url = "";
        assertNull(extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl2() {
        String url = null;
        assertNull(extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl3() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F57991476%2Fstats%2F"
                     + "referrers%3Fdate%3D2014-05-08&urls%5B%5D=%2Fsites%2F57991476%2Fstats%2F"
                     + "referrers%3Fdate%3D2014-05-07";
        assertEquals("57991476", extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl4() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/59073674/stats";
        assertEquals("59073674", extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl5() {
        String url = "https://public-api.wordpress.com/rest/v1/sites//stats";
        assertEquals("", extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl6() {
        String url = "https://public-api.wordpress.com/rest/v1/batch/?urls%5B%5D=%2Fsites%2F";
        assertNull(extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    @Test
    public void testExtractSiteIdFromUrl7() {
        String url = "https://public-api.wordpress.com/rest/v1/sites/";
        assertNull(extractSiteIdFromUrl(mRestClient.getEndpointURL(), url));
    }

    /* HELPER */

    /**
     * Parse out the site ID from an URL.
     * Note: For batch REST API calls, only the first siteID is returned
     *
     * @return The site ID
     */
    private String extractSiteIdFromUrl(String restEndpointUrl, String url) {
        if (url == null) {
            return null;
        }

        final String sitePrefix = restEndpointUrl.endsWith("/")
                ? restEndpointUrl + "sites/"
                : restEndpointUrl + "/sites/";
        final String batchCallPrefix = restEndpointUrl.endsWith("/")
                ? restEndpointUrl + "batch/?urls%5B%5D=%2Fsites%2F"
                : restEndpointUrl + "/batch/?urls%5B%5D=%2Fsites%2F";

        if (url.startsWith(sitePrefix) && !sitePrefix.equals(url)) {
            int marker = sitePrefix.length();
            if (url.indexOf("/", marker) < marker) {
                return null;
            }
            return url.substring(marker, url.indexOf("/", marker));
        } else if (url.startsWith(batchCallPrefix) && !batchCallPrefix.equals(url)) {
            int marker = batchCallPrefix.length();
            if (url.indexOf("%2F", marker) < marker) {
                return null;
            }
            return url.substring(marker, url.indexOf("%2F", marker));
        }

        // not a sites/$siteId request or a batch request
        return null;
    }
}
