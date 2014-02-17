package org.wordpress.android.functional.media;

import org.wordpress.android.ActivityRobotiumTestCase;
import org.wordpress.android.R;
import org.wordpress.android.RobotiumUtils;
import org.wordpress.android.ui.posts.PostsActivity;

public class MediaTest extends ActivityRobotiumTestCase<PostsActivity> {
    public MediaTest() {
        super(PostsActivity.class);
    }

    public void testLoadMedia() throws Exception {
        login();
        mSolo.clickOnText(mSolo.getString(R.string.media));
        mSolo.scrollDown();
    }

    public void testDeleteMedia() throws Exception {
        login();
        mSolo.clickOnText(mSolo.getString(R.string.media));
        mSolo.clickLongOnText("wpid-pony.jpg");
        // seems not to work on CAB
        // mSolo.clickOnActionBarItem(R.id.media_multiselect_actionbar_trash);
        RobotiumUtils.clickOnId(mSolo, "media_multiselect_actionbar_trash");
    }
    
    public void testDeleteMultipleMedias() throws Exception {
        login();
        mSolo.clickOnText(mSolo.getString(R.string.media));
        mSolo.clickLongOnText("wpid-pony.jpg");
        mSolo.clickOnImage(3);
        mSolo.clickOnImage(2);
        RobotiumUtils.clickOnId(mSolo, "media_multiselect_actionbar_trash");
    }
}
