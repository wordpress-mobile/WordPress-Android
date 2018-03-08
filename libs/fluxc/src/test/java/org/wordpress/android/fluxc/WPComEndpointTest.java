package org.wordpress.android.fluxc;

import org.junit.Test;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

import static org.junit.Assert.assertEquals;

public class WPComEndpointTest {
    @Test
    public void testAllEndpoints() {
        // Sites
        assertEquals("/sites/", WPCOMREST.sites.getEndpoint());
        assertEquals("/sites/new/", WPCOMREST.sites.new_.getEndpoint());
        assertEquals("/sites/56/", WPCOMREST.sites.site(56).getEndpoint());
        assertEquals("/sites/56/post-formats/", WPCOMREST.sites.site(56).post_formats.getEndpoint());

        assertEquals("/sites/mysite.wordpress.com/", WPCOMREST.sites.siteUrl("mysite.wordpress.com").getEndpoint());

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

        // Plugins
        assertEquals("/sites/56/plugins/", WPCOMREST.sites.site(56).plugins.getEndpoint());
        assertEquals("/sites/56/plugins/akismet/", WPCOMREST.sites.site(56).plugins.name("akismet").getEndpoint());
        assertEquals("/sites/56/plugins/akismet/install/", WPCOMREST.sites.site(56).plugins.slug("akismet")
                .install.getEndpoint());
        assertEquals("/sites/56/plugins/akismet/delete/", WPCOMREST.sites.site(56).plugins.name("akismet")
                .delete.getEndpoint());

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
        assertEquals("/me/username/", WPCOMREST.me.username.getEndpoint());

        // Users
        assertEquals("/users/new/", WPCOMREST.users.new_.getEndpoint());
        assertEquals("/users/new/", WPCOMREST.users.new_.getEndpoint());

        // Availability
        assertEquals("/is-available/email/", WPCOMREST.is_available.email.getEndpoint());
        assertEquals("/is-available/username/", WPCOMREST.is_available.username.getEndpoint());
        assertEquals("/is-available/blog/", WPCOMREST.is_available.blog.getEndpoint());
        assertEquals("/is-available/domain/", WPCOMREST.is_available.domain.getEndpoint());

        // Magic link email sender
        assertEquals("/auth/send-login-email/", WPCOMREST.auth.send_login_email.getEndpoint());
        assertEquals("/auth/send-signup-email/", WPCOMREST.auth.send_signup_email.getEndpoint());

        assertEquals("/read/feed/56/", WPCOMREST.read.feed.feed_url_or_id(56).getEndpoint());
        assertEquals("/read/feed/somewhere.site/", WPCOMREST.read.feed.feed_url_or_id("somewhere.site").getEndpoint());
    }

    @Test
    public void testUrls() {
        assertEquals("https://public-api.wordpress.com/rest/v1/sites/", WPCOMREST.sites.getUrlV1());
        assertEquals("https://public-api.wordpress.com/rest/v1.1/sites/", WPCOMREST.sites.getUrlV1_1());
        assertEquals("https://public-api.wordpress.com/rest/v1.2/sites/", WPCOMREST.sites.getUrlV1_2());
        assertEquals("https://public-api.wordpress.com/rest/v1.3/sites/", WPCOMREST.sites.getUrlV1_3());
        assertEquals("https://public-api.wordpress.com/is-available/email/", WPCOMREST.is_available.email.getUrlV0());
    }
}
