package org.wordpress.android.models;

import android.test.InstrumentationTestCase;

import org.wordpress.android.WordPressDB;

/**
 * Created by roundhill on 3/12/14.
 */
public class PostTest extends InstrumentationTestCase {
    private WordPressDB mDB;

    @Override
    protected void setUp() throws Exception {
        mDB = new WordPressDB(getInstrumentation().getContext());

        super.setUp();
    }

    public void testInvalidPostIdLoad() {
        Post post = mDB.getPostForLocalTablePostId(-1);

        assertNull(post);
    }

    public void testPostSaveAndLoad() {
        Post post = new Post(1, false);
        post.setTitle("test-post");
        post.save();

        Post loadedPost = mDB.getPostForLocalTablePostId(post.getLocalTablePostId());

        assertNotNull(loadedPost);
        assertEquals(loadedPost.getTitle(), post.getTitle());
    }


}
