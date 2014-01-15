package org.wordpress.android.models;

import android.test.InstrumentationTestCase;

import org.wordpress.android.TestUtils;

/**
 * Created by aaron on 1/14/14.
 */
public class BlogTest extends InstrumentationTestCase {
    private Blog blog;

    @Override
    protected void setUp() throws Exception {
        blog = new Blog("http://www.example.com/xml-rpc.php", "username", "password");

        super.setUp();
    }

    public void testBlogTestUrlUsernamePassword() {
        assertEquals("http://www.example.com/xml-rpc.php", blog.getUrl());
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
        assertEquals("http://www.example.com/xml-rpc.php", blog.getUrl());
        blog.setUrl(null);
        assertNull(blog.getUrl());
        blog.setUrl("http://example.com/two");
        assertEquals("http://example.com/two", blog.getUrl());
    }

//    public void testGetSetHomeURL() {
//    }
//
//    public void testGetSetBlogName() {
//    }
//
//    public void testGetSetUsername() {
//    }
//
//    public void testGetSetPassword() {
//    }
//
//    public void testGetSetImagePlacement() {
//    }
//
//    public void testGetSetFeaturedImageCapable() {
//    }
//
//    public void testBsetFeaturedImageCapable() {
//    }
//
//    public void testGetSetFullSizeImage() {
//    }
//
//    public void testGetSetMaxImageWidth() {
//    }
//
//    public void testGetSetMaxImageWidthId() {
//    }
//
//    public void testGetSetLastCommentId() {
//    }
//
//    public void testGetSetRunService() {
//    }
//
//    public void testGetSetRemoteBlogId() {
//    }
//
//    public void testGetSetLocation() {
//    }
//
//    public void testGetSetDotcom_username() {
//    }
//
//    public void testGetSetDotcom_password() {
//    }
//
//    public void testGetSetApi_key() {
//    }
//
//    public void testGetSetApi_blogid() {
//    }
//
//    public void testGetSetDotcomFlag() {
//    }
//
//    public void testGetSetWpVersion() {
//    }
//
//    public void testBsetWpVersion() {
//    }
//
//    public void testGetSetHttpuser() {
//    }
//
//    public void testGetSetHttppassword() {
//    }
//
//    public void testGetSetHidden() {
//    }
//
//    public void testGetSetPostFormats() {
//    }
//
//    public void testBSetPostFormats() {
//    }
//
//    public void testGetSetUnmoderatedCommentCount() {
//    }
//
//    public void testGetSetScaledImage() {
//    }
//
//    public void testGetSetScaledImageWidth() {
//    }
//
//    public void testGetSetBlogOptions() {
//    }
//
//    public void testBSetBlogOptions() {
//    }
//
//    public void testGetSetActive() {
//    }
//
//    public void testGetSetAdmin() {
//    }
//
//    public void testBSetAdmin(boolean isAdmin) {
//    }
//
//    public void testGetSetAdminUrl() {
//    }
//
//    public void testGetSetPrivate() {
//    }
//
//    public void testGetSetJetpackPowered() {
//    }
//
//    public void testGetSetPhotonCapable() {
//    }
//
//    public void testGetSetHasValidJetpackCredentials() {
//    }
//
//    public void testGetSetDotComBlogId() {
//    }
}
