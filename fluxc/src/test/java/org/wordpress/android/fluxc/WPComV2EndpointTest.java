package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPComV2EndpointTest {
    @Test
    public void testAllEndpoints() {
        // Users
        assertEquals("/users/username/suggestions/", WPCOMV2.users.username.suggestions.getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://public-api.wordpress.com/wpcom/v2/users/username/suggestions/",
                WPCOMV2.users.username.suggestions.getUrl());
    }
}
