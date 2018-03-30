package org.wordpress.android.fluxc.endpoints;

import org.junit.Test;
import org.wordpress.android.fluxc.generated.endpoint.WPAPI;

import static org.junit.Assert.assertEquals;

public class WPAPIEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Posts
        assertEquals("/posts/", WPAPI.posts.getEndpoint());
        assertEquals("/posts/56/", WPAPI.posts.id(56).getEndpoint());

        // Pages
        assertEquals("/pages/", WPAPI.pages.getEndpoint());
        assertEquals("/pages/56/", WPAPI.pages.id(56).getEndpoint());

        // Media
        assertEquals("/media/", WPAPI.media.getEndpoint());
        assertEquals("/media/56/", WPAPI.media.id(56).getEndpoint());

        // Comments
        assertEquals("/comments/", WPAPI.comments.getEndpoint());
        assertEquals("/comments/56/", WPAPI.comments.id(56).getEndpoint());

        // Settings
        assertEquals("/settings/", WPAPI.settings.getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("wp/v2/posts/", WPAPI.posts.getUrlV2());
        assertEquals("wp/v2/pages/", WPAPI.pages.getUrlV2());
        assertEquals("wp/v2/media/", WPAPI.media.getUrlV2());
        assertEquals("wp/v2/comments/", WPAPI.comments.getUrlV2());
        assertEquals("wp/v2/settings/", WPAPI.settings.getUrlV2());
    }
}
