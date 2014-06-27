package org.wordpress.android.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;

public class PostTest extends InstrumentationTestCase {
    protected Context mTestContext;
    protected Context mTargetContext;

    @Override
    protected void setUp() throws Exception {

        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        mTestContext = getInstrumentation().getContext();

        super.setUp();
    }

    public void testInvalidPostIdLoad() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(mTargetContext, mTestContext, "taliwutt-blogs-sample.sql");
        Post post = WordPress.wpDB.getPostForLocalTablePostId(-1);

        assertNull(post);
    }

    public void testPostSaveAndLoad() {
        SQLiteDatabase db = TestUtils.loadDBFromDump(mTargetContext, mTestContext, "taliwutt-blogs-sample.sql");
        Post post = new Post(1, false);
        post.setTitle("test-post");
        WordPress.wpDB.savePost(post);

        Post loadedPost = WordPress.wpDB.getPostForLocalTablePostId(post.getLocalTablePostId());

        assertNotNull(loadedPost);
        assertEquals(loadedPost.getTitle(), post.getTitle());
    }
}