package org.wordpress.android.database;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.TestUtils;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Comment;

public class CommentTableTest extends InstrumentationTestCase {
    protected Context mTargetContext;
    protected Context mTestContext;

    @Override
    protected void setUp() throws Exception {
        // Clean application state
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        mTestContext = getInstrumentation().getContext();
        TestUtils.clearApplicationState(mTargetContext);
        TestUtils.resetEventBus();
    }

    public void testGetCommentEqualTo1024K() {
        createAndGetComment(1024 * 1024);
    }

    public void testGetCommentEqualTo2096550() {
        createAndGetComment(2096550);  // 1024 * 1024 * 2 - 603
    }

    public void testGetCommentEqualTo2096549() {
        createAndGetComment(2096549); // 1024 * 1024 * 2 - 602
    }

    public void testGetCommentEqualTo2048K() {
        createAndGetComment(1024 * 1024 * 2);
    }

    private void createAndGetComment(int commentLength) {
        // Load a sample DB and inject it into WordPress.wpdb
        TestUtils.loadDBFromDump(mTargetContext, mTestContext, "taliwutt-blogs-sample.sql");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commentLength; ++i) {
            sb.append('a');
        }
        Comment bigComment = new Comment(0,
                1,
                "author",
                "0",
                sb.toString(),
                "approve",
                "arst",
                "http://mop.com",
                "mop@mop.com",
                "");
        CommentTable.addComment(0, bigComment);
        CommentTable.getCommentsForBlog(0);
    }
}
