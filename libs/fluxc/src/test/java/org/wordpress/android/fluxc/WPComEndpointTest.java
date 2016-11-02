package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPComEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Sites
        assertEquals("/sites/", WPCOMREST.sites.getEndpoint());
        assertEquals("/sites/new/", WPCOMREST.sites.new_.getEndpoint());
        assertEquals("/sites/56/", WPCOMREST.sites.site(56).getEndpoint());
        assertEquals("/sites/56/post-formats/", WPCOMREST.sites.site(56).post_formats.getEndpoint());

        // Sites - Posts
        assertEquals("/sites/56/posts/", WPCOMREST.sites.site(56).posts.getEndpoint());
        assertEquals("/sites/56/posts/78/", WPCOMREST.sites.site(56).posts.post(78).getEndpoint());
        assertEquals("/sites/56/posts/78/delete/", WPCOMREST.sites.site(56).posts.post(78).delete.getEndpoint());
        assertEquals("/sites/56/posts/new/", WPCOMREST.sites.site(56).posts.new_.getEndpoint());
        assertEquals("/sites/56/posts/slug:fluxc/", WPCOMREST.sites.site(56).posts.slug("fluxc").getEndpoint());

        // Sites - Media
        assertEquals("/sites/56/media/", WPCOMREST.sites.site(56).media.getEndpoint());
        assertEquals("/sites/56/media/78/", WPCOMREST.sites.site(56).media.item(78).getEndpoint());
        assertEquals("/sites/56/media/78/delete/", WPCOMREST.sites.site(56).media.item(78).delete.getEndpoint());
        assertEquals("/sites/56/media/new/", WPCOMREST.sites.site(56).media.new_.getEndpoint());

        // Sites - Taxonomies
        assertEquals("/sites/56/taxonomies/category/terms/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.getEndpoint());
        assertEquals("/sites/56/taxonomies/category/terms/new/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.new_.getEndpoint());
        assertEquals("/sites/56/taxonomies/category/terms/slug:fluxc/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("category").terms.slug("fluxc").getEndpoint());
        assertEquals("/sites/56/taxonomies/post_tag/terms/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("post_tag").terms.getEndpoint());
        assertEquals("/sites/56/taxonomies/post_tag/terms/new/",
                WPCOMREST.sites.site(56).taxonomies.taxonomy("post_tag").terms.new_.getEndpoint());

        // Me
        assertEquals("/me/", WPCOMREST.me.getEndpoint());
        assertEquals("/me/settings/", WPCOMREST.me.settings.getEndpoint());
        assertEquals("/me/sites/", WPCOMREST.me.sites.getEndpoint());

        // Users
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
