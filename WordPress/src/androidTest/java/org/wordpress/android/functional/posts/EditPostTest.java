package org.wordpress.android.functional.posts;

import org.wordpress.android.ActivityRobotiumTestCase;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.posts.PostsActivity;

public class EditPostTest extends ActivityRobotiumTestCase<PostsActivity> {
    public EditPostTest() {
        super(PostsActivity.class);
    }

    public void testEditNullPostId() throws Exception {
        XMLRPCFactoryTest.setPrefixAllInstances("malformed-null-postid");
        login();
        mSolo.clickOnText(mSolo.getString(R.string.posts));
        WordPressDB wpdb = WordPress.wpDB;
        Post post = new Post(59073674, false);
        post.setRemotePostId(null);
        wpdb.savePost(post);
        mSolo.clickOnText("null postid");
    }
}
