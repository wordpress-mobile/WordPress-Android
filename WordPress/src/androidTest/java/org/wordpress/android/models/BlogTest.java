package org.wordpress.android.models;

import android.test.InstrumentationTestCase;

public class BlogTest extends InstrumentationTestCase {
    private Blog blog;

    @Override
    protected void setUp() throws Exception {
        blog = new Blog("http://www.example.com", "username", "password");

        super.setUp();
    }

    public void testBlogTestUrlUsernamePassword() {
        assertEquals("http://www.example.com", blog.getUrl());
        assertEquals("username", blog.getUsername());
        assertEquals("password", blog.getPassword());
        assertEquals(-1, blog.getLocalTableBlogId());
    }

    public void testGetSetLocalTableBlogId() {
        assertEquals(-1, blog.getLocalTableBlogId());
        blog.setLocalTableBlogId(0);
        assertEquals(0, blog.getLocalTableBlogId());
    }

    public void testGetSetUrl() {
        assertEquals("http://www.example.com", blog.getUrl());
        blog.setUrl(null);
        assertNull(blog.getUrl());
        blog.setUrl("http://example.com/two");
        assertEquals("http://example.com/two", blog.getUrl());
    }

    public void testGetSetHomeURL() {
        assertNull(blog.getHomeURL());
        blog.setHomeURL("http://www.homeurl.com");
        assertEquals("http://www.homeurl.com", blog.getHomeURL());
    }

    public void testGetSetBlogName() {
        assertNull(blog.getBlogName());
        blog.setBlogName("blogName");
        assertEquals("blogName", blog.getBlogName());
    }

    public void testGetSetUsername() {
        assertEquals("username", blog.getUsername());
        blog.setUsername(null);
        // getUsername never returns null
        assertEquals("", blog.getUsername());
    }

    public void testGetSetPassword() {
        assertEquals("password", blog.getPassword());
        blog.setPassword(null);
        // getPassword never returns null
        assertEquals("", blog.getPassword());
    }

    public void testGetSetImagePlacement() {
        assertNull(blog.getImagePlacement());
        blog.setImagePlacement("test");
        assertEquals("test", blog.getImagePlacement());
    }

    public void testGetSetFeaturedImageCapable() {
        assertFalse(blog.isFeaturedImageCapable());
        blog.setFeaturedImageCapable(true);
        assertTrue(blog.isFeaturedImageCapable());
    }

    public void testBsetFeaturedImageCapable() {
        assertFalse(blog.isFeaturedImageCapable());
        boolean val = blog.bsetFeaturedImageCapable(false);
        assertFalse(val);
        assertFalse(blog.isFeaturedImageCapable());
        val = blog.bsetFeaturedImageCapable(true);
        assertTrue(val);
        assertTrue(blog.isFeaturedImageCapable());
        val = blog.bsetFeaturedImageCapable(false);
        assertTrue(val);
    }

    public void testGetSetFullSizeImage() {
        assertFalse(blog.isFullSizeImage());
        blog.setFullSizeImage(true);
        assertTrue(blog.isFullSizeImage());
    }

    public void testGetSetMaxImageWidth() {
        assertEquals("", blog.getMaxImageWidth());
        blog.setMaxImageWidth("1");
        assertEquals("1", blog.getMaxImageWidth());
    }

    public void testGetSetMaxImageWidthId() {
        assertEquals(0, blog.getMaxImageWidthId());
        blog.setMaxImageWidthId(1);
        assertEquals(1, blog.getMaxImageWidthId());
    }

    public void testGetSetRemoteBlogId() {
        assertEquals(0, blog.getRemoteBlogId());
        blog.setRemoteBlogId(1);
        assertEquals(1, blog.getRemoteBlogId());
    }

    public void testGetSetDotcom_username() {
        assertNull(blog.getDotcom_username());
        blog.setDotcom_username("username");
        assertEquals("username", blog.getDotcom_username());
    }

    public void testGetSetDotcom_password() {
        assertNull(blog.getDotcom_password());
        blog.setDotcom_password("password");
        assertEquals("password", blog.getDotcom_password());
    }

    public void testGetSetApi_key() {
        assertNull(blog.getApi_key());
        blog.setApi_key("123");
        assertEquals("123", blog.getApi_key());
    }

    public void testGetSetApi_blogid() {
        assertNull(blog.getApi_blogid());
        blog.setApi_blogid("123");
        assertEquals("123", blog.getApi_blogid());
    }

    public void testGetSetDotcomFlag() {
        assertFalse(blog.isDotcomFlag());
        blog.setDotcomFlag(true);
        assertTrue(blog.isDotcomFlag());
    }

    public void testGetSetWpVersion() {
        assertNull(blog.getWpVersion());
        blog.setWpVersion("123");
        assertEquals("123", blog.getWpVersion());
    }

    public void testBsetWpVersion() {
        assertNull(blog.getWpVersion());
        boolean val = blog.bsetWpVersion("123");
        assertTrue(val);
        assertEquals("123", blog.getWpVersion());
        val = blog.bsetWpVersion("123");
        assertFalse(val);
    }

    public void testGetSetHttpuser() {
        assertEquals(blog.getHttpuser(), "");
        blog.setHttpuser("user");
        assertEquals("user", blog.getHttpuser());
    }

    public void testGetSetHttppassword() {
        assertEquals(blog.getHttppassword(), "");
        blog.setHttppassword("password");
        assertEquals("password", blog.getHttppassword());
    }

    public void testGetSetHidden() {
        assertFalse(blog.isHidden());
        blog.setHidden(true);
        assertTrue(blog.isHidden());
    }

    public void testGetSetPostFormats() {
        assertNull(blog.getPostFormats());
        blog.setPostFormats("test");
        assertEquals("test", blog.getPostFormats());
    }

    public void testBSetPostFormats() {
        assertNull(blog.getPostFormats());
        boolean val = blog.bsetPostFormats("test");
        assertTrue(val);
        assertEquals("test", blog.getPostFormats());
        val = blog.bsetPostFormats("test");
        assertFalse(val);
        val = blog.bsetPostFormats("test2");
        assertTrue(val);
    }

    public void testGetSetScaledImage() {
        assertFalse(blog.isScaledImage());
        blog.setScaledImage(true);
        assertTrue(blog.isScaledImage());
    }

    public void testGetSetScaledImageWidth() {
        assertEquals(0, blog.getScaledImageWidth());
        blog.setScaledImageWidth(1);
        assertEquals(1, blog.getScaledImageWidth());
    }

    public void testGetSetBlogOptions() {
        assertEquals("{}", blog.getBlogOptions());
        blog.setBlogOptions("{option:1}");
        assertEquals("{option:1}", blog.getBlogOptions());
    }

    public void testBSetBlogOptions() {
        assertEquals("{}", blog.getBlogOptions());
        boolean val = blog.bsetBlogOptions("{option:1}");
        assertTrue(val);
        val = blog.bsetBlogOptions("{option:1}");
        assertFalse(val);
        val = blog.bsetBlogOptions("{option:2}");
        assertTrue(val);
    }

    public void testGetSetAdmin() {
        assertFalse(blog.isAdmin());
        blog.setAdmin(true);
        assertTrue(blog.isAdmin());
    }

    public void testBSetAdmin() {
        assertFalse(blog.isAdmin());
        boolean val = blog.bsetAdmin(false);
        assertFalse(val);
        val = blog.bsetAdmin(true);
        assertTrue(val);
        val = blog.bsetAdmin(true);
        assertFalse(val);
    }

    public void testGetSetAdminUrl() {
        blog.setBlogOptions("{\"admin_url\": {\"value\": \"https://muppets.com/wp-admin/\" } }");
        assertEquals("https://muppets.com/wp-admin/", blog.getAdminUrl());
    }

    public void testGetSetPrivate() {
        assertFalse(blog.isPrivate());
        blog.setBlogOptions("{ \"blog_public\" : { \"value\" : \"-1\" } }");

        // blog cannot be private if not a wpcom one
        assertFalse(blog.isPrivate());

        // set the blog as a WPCom one
        blog.setDotcomFlag(true);
        // blog should now appear as private
        assertTrue(blog.isPrivate());
    }

    public void testGetSetJetpackPowered() {
        assertFalse(blog.isJetpackPowered());
        blog.setBlogOptions("{ jetpack_client_id : {} }");
        assertTrue(blog.isJetpackPowered());
    }

    public void testIsPhotonCapableJetpack() {
        assertFalse(blog.isPhotonCapable());

        blog.setBlogOptions("{ jetpack_client_id : {} }");
        assertTrue(blog.isPhotonCapable());
    }

    public void testIsPhotonCapableWPComPublic() {
        assertFalse(blog.isPhotonCapable());
        assertFalse(blog.isPrivate());
        blog.setBlogOptions("");
        blog.setDotcomFlag(true);
        assertTrue(blog.isPhotonCapable());
    }

    public void testIsPhotonCapableWPComPrivate() {
        assertFalse(blog.isPhotonCapable());

        blog.setBlogOptions("{ \"blog_public\" : { \"value\" : \"-1\" } }");
        assertFalse(blog.isPhotonCapable());
    }

    public void testGetSetHasValidJetpackCredentials() {
        assertFalse(blog.hasValidJetpackCredentials());
    }

    public void testGetSetDotComBlogId() {
        assertNull(blog.getDotComBlogId());
        assertFalse(blog.isDotcomFlag());
        blog.setApi_blogid("1");
        blog.setRemoteBlogId(2);
        assertEquals("1", blog.getDotComBlogId());
        blog.setDotcomFlag(true);
        assertEquals("2", blog.getDotComBlogId());
    }
}
