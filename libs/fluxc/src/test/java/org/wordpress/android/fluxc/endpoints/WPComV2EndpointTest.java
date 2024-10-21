package org.wordpress.android.fluxc.endpoints;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;

public class WPComV2EndpointTest {
    @Test
    public void testAllEndpoints() {
        // Users
        assertEquals("/users/username/suggestions/", WPCOMV2.users.username.suggestions.getEndpoint());
        assertEquals("/plugins/featured/", WPCOMV2.plugins.featured.getEndpoint());

        // Sites - Jetpack Social
        assertEquals("/sites/56/jetpack-social/", WPCOMV2.sites.site(56).jetpack_social.getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://public-api.wordpress.com/wpcom/v2/users/username/suggestions/",
                WPCOMV2.users.username.suggestions.getUrl());
        assertEquals("https://public-api.wordpress.com/wpcom/v2/sites/56/jetpack-social/",
                WPCOMV2.sites.site(56).jetpack_social.getUrl());
    }
}
