package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPOrgAPIEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Plugins info
        assertEquals("/plugins/info/1.0/akismet/", WPORGAPI.plugins.info.version("1.0").slug("akismet").getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://api.wordpress.org/plugins/info/1.0/akismet.json",
                WPORGAPI.plugins.info.version("1.0").slug("akismet").getUrl());
    }
}
