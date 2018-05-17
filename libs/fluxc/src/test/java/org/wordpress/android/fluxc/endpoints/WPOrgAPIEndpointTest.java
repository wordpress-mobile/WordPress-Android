package org.wordpress.android.fluxc.endpoints;

import org.junit.Test;
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI;

import static org.junit.Assert.assertEquals;

public class WPOrgAPIEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Plugins info
        assertEquals("/plugins/info/1.0/akismet/", WPORGAPI.plugins.info.version("1.0").slug("akismet").getEndpoint());
        assertEquals("/plugins/info/1.1/", WPORGAPI.plugins.info.version("1.1").getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://api.wordpress.org/plugins/info/1.0/akismet.json",
                WPORGAPI.plugins.info.version("1.0").slug("akismet").getUrl());
        assertEquals("https://api.wordpress.org/plugins/info/1.1/",
                WPORGAPI.plugins.info.version("1.1").getUrl());
    }
}
