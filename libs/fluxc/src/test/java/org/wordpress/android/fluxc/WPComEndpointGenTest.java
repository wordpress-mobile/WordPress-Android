package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.rest.wpcom.WPCOMRESTGen;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPComEndpointGenTest {
    @Test
    public void testAllEndpoints() {
        // Sites
        assertEquals("/sites/", WPCOMRESTGen.sites.getEndpoint());
        assertEquals("/sites/new/", WPCOMRESTGen.sites.new_.getEndpoint());
        assertEquals("/sites/56/", WPCOMRESTGen.sites.site(56).getEndpoint());
        assertEquals("/sites/56/post-formats/", WPCOMRESTGen.sites.site(56).post_formats.getEndpoint());

        // Sites - Posts
        assertEquals("/sites/56/posts/", WPCOMRESTGen.sites.site(56).posts.getEndpoint());
        assertEquals("/sites/56/posts/new/", WPCOMRESTGen.sites.site(56).posts.new_.getEndpoint());
        assertEquals("/sites/56/posts/78/", WPCOMRESTGen.sites.site(56).posts.post(78).getEndpoint());
        assertEquals("/sites/56/posts/78/delete/", WPCOMRESTGen.sites.site(56).posts.post(78).delete.getEndpoint());
        assertEquals("/sites/56/posts/new/", WPCOMRESTGen.sites.site(56).posts.new_.getEndpoint());

        assertEquals("/67/", WPCOMRESTGen.test(67).getEndpoint());
        assertEquals("/67/sub/", WPCOMRESTGen.test(67).sub.getEndpoint());

        // Me
        //assertEquals("/me/", WPCOMRESTGen.me.getEndpoint());
        //assertEquals("/me/settings/", WPCOMRESTGen.me.settings.getEndpoint());
        //assertEquals("/me/sites/", WPCOMRESTGen.me.sites.getEndpoint());

        // Users
        assertEquals("/users/new/", WPCOMRESTGen.users.new_.getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://public-api.wordpress.com/rest/v1/sites/", WPCOMRESTGen.sites.getUrlV1());
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/", WPCOMRESTGen.sites.getUrlV1_1());
        assertEquals("https://public-api.wordpress.com/rest/v1.2/sites/", WPCOMRESTGen.sites.getUrlV1_2());
        assertEquals("https://public-api.wordpress.com/rest/v1.3/sites/", WPCOMRESTGen.sites.getUrlV1_3());
    }
}
