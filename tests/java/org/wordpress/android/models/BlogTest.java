package org.wordpress.android.models;

import android.app.Instrumentation;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.InstrumentationTestSuite;
import android.test.RenamingDelegatingContext;

import junit.framework.Test;
import junit.framework.TestSuite;

import static junit.framework.Assert.*;

import org.wordpress.android.TestUtils;
import org.wordpress.android.ui.accounts.SetupBlog;

/**
 * Created by aaron on 1/14/14.
 */
public class BlogTest extends InstrumentationTestCase {
    protected Context testContext;
    protected Context targetContext;
    private Blog originalBlog;

    @Override
    protected void setUp() throws Exception {
        // Run tests in an isolated context
        targetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        testContext = getInstrumentation().getContext();

        SQLiteDatabase db = TestUtils.loadDBFromDump(targetContext, testContext,
                "empty_tables.sql");

        super.setUp();
    }

    private Blog createBlog() {
        Blog blog = new Blog("http://example.com/xml-rpc.php", "username", "password");
        blog.setHomeURL("http://example.com");
        blog.setHttpuser("mHttpUsername");
        blog.setHttppassword("mHttpPassword");
        blog.setBlogName("blogName");
        blog.setImagePlacement(""); //deprecated
        blog.setFullSizeImage(false);
        blog.setMaxImageWidth("100");
        blog.setMaxImageWidthId(0); //deprecated
        blog.setRunService(false); //deprecated
        blog.setRemoteBlogId(Integer.parseInt("1"));
        blog.setDotcomFlag(false);
        blog.setWpVersion(""); // assigned later in getOptions call
        blog.setAdmin(true);
        //blog.save();

        return blog;
    }

    public void testValidateFields() {
        Blog blog = createBlog();

        assertEquals("blogName", blog.getBlogName());
        assertEquals("http://example.com/xml-rpc.php", blog.getUrl());
        assertEquals("http://example.com", blog.getHomeURL());
        assertEquals(1, blog.getRemoteBlogId());
        assertEquals("username", blog.getUsername());
        assertEquals("password", blog.getPassword());
        assertTrue(blog.isAdmin());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        TestUtils.dropDB(targetContext, testContext);

        testContext = null;
        targetContext = null;
    }
}
