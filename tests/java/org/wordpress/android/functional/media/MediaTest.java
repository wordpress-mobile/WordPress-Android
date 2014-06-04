package org.wordpress.android.functional.media;

import com.robotium.solo.Solo;

import org.wordpress.android.ActivityRobotiumTestCase;
import org.wordpress.android.R;
import org.wordpress.android.RobotiumUtils;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostsActivity;

public class MediaTest extends ActivityRobotiumTestCase<PostsActivity> {
    public MediaTest() {
        super(PostsActivity.class);
    }

    public void testLoadMediaLandscape() throws Exception {
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        login();
        mSolo.waitForText(mSolo.getString(R.string.media));
        mSolo.clickOnText(mSolo.getString(R.string.media));
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.waitForText("wpid-pony.jpg", 1 , 10000);
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.waitForText("wpid-pony.jpg", 1 , 10000);
    }


    public void testDeleteMedia() throws Exception {
        login();
        mSolo.waitForText(mSolo.getString(R.string.media));
        mSolo.clickOnText(mSolo.getString(R.string.media));
        // wait for reloading
        mSolo.waitForText("wpid-pony.jpg", 1 , 10000);
        mSolo.clickLongOnText("wpid-pony.jpg");
        // seems not to work on CAB
        // mSolo.clickOnActionBarItem(R.id.media_multiselect_actionbar_trash);
        RobotiumUtils.clickOnId(mSolo, "media_multiselect_actionbar_trash");
    }

    public void testCreateGalleryCancel() throws Exception {
        login();
        mSolo.waitForText(mSolo.getString(R.string.media));
        mSolo.clickOnText(mSolo.getString(R.string.media));
        // wait for reloading
        mSolo.waitForText("wpid-pony.jpg", 1, 10000);
        mSolo.clickLongOnText("wpid-pony.jpg");
        mSolo.clickOnImage(0);
        RobotiumUtils.clickOnId(mSolo, "media_multiselect_actionbar_gallery");
        mSolo.goBack();
        mSolo.assertCurrentActivity("Should be back on MediaBrowserActivity", MediaBrowserActivity.class);
    }

    public void testCreateGalleryConfirm() throws Exception {
        login();
        mSolo.waitForText(mSolo.getString(R.string.media));
        mSolo.clickOnText(mSolo.getString(R.string.media));
        // wait for reloading
        mSolo.waitForText("wpid-pony.jpg", 1, 10000);
        mSolo.clickLongOnText("wpid-pony.jpg");
        mSolo.clickOnImage(0);
        RobotiumUtils.clickOnId(mSolo, "media_multiselect_actionbar_gallery");
        RobotiumUtils.clickOnId(mSolo, "menu_save");
        mSolo.assertCurrentActivity("Should display EditPostActivity", EditPostActivity.class);
    }
}
