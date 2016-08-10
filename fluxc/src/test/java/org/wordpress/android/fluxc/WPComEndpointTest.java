package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.rest.wpcom.WPCOMREST;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPComEndpointTest {
    @Test
    public void testAllEndpoints() {
        assertEquals("/sites/", WPCOMREST.sites.getEndpoint());
        assertEquals("/sites/56/", WPCOMREST.sites.site(56).getEndpoint());
        assertEquals("/sites/56/posts/", WPCOMREST.sites.site(56).posts.getEndpoint());
        assertEquals("/sites/56/posts/new/", WPCOMREST.sites.site(56).posts.new_.getEndpoint());
        assertEquals("/sites/56/posts/78/", WPCOMREST.sites.site(56).posts.post(78).getEndpoint());
        assertEquals("/sites/56/posts/78/delete/", WPCOMREST.sites.site(56).posts.post(78).delete.getEndpoint());
        assertEquals("/sites/56/posts/new/", WPCOMREST.sites.site(56).posts.new_.getEndpoint());
        assertEquals("/sites/new/", WPCOMREST.sites.new_.getEndpoint());

        assertEquals("/me/", WPCOMREST.me.getEndpoint());
        assertEquals("/me/settings/", WPCOMREST.me.settings.getEndpoint());
        assertEquals("/me/sites/", WPCOMREST.me.sites.getEndpoint());

        assertEquals("/users/new/", WPCOMREST.users.new_.getEndpoint());
        assertEquals("/users/new/", WPCOMREST.users.new_.getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://public-api.wordpress.com/rest/v1/sites/", WPCOMREST.sites.getUrlV1());
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/", WPCOMREST.sites.getUrlV1_1());
        assertEquals("https://public-api.wordpress.com/rest/v1.2/sites/", WPCOMREST.sites.getUrlV1_2());
        assertEquals("https://public-api.wordpress.com/rest/v1.3/sites/", WPCOMREST.sites.getUrlV1_3());
    }
}
