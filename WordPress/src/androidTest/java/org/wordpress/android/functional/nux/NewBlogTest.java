package org.wordpress.android.functional.nux;

import org.wordpress.android.ActivityRobotiumTestCase;
import org.wordpress.android.R;
import org.wordpress.android.RobotiumUtils;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.prefs.PreferencesActivity;
import org.wordpress.android.util.EditTextUtils;

public class NewBlogTest  extends ActivityRobotiumTestCase<PostsActivity> {
    public NewBlogTest() {
        super(PostsActivity.class);
    }

    public void testCreateAccountSuccess() throws Exception {
        login();
        mSolo.clickOnActionBarItem(R.id.menu_settings);
        mSolo.clickOnText(mSolo.getString(R.string.create_new_blog_wpcom));
        mSolo.enterText(0, "Site name");
        String siteUrl = EditTextUtils.getText(mSolo.getEditText(1));
        assertEquals(siteUrl, "sitename");
        RobotiumUtils.clickOnId(mSolo, "signup_button");
        mSolo.assertCurrentActivity("Should display PreferencesActivity", PreferencesActivity.class);
    }

    public void testCreateAccountSiteReserved() throws Exception {
        login();
        RestClientFactoryTest.setPrefixAllInstances("site-reserved");
        mSolo.clickOnActionBarItem(R.id.menu_settings);
        mSolo.clickOnText(mSolo.getString(R.string.create_new_blog_wpcom));
        mSolo.enterText(0, "Site reserved");
        RobotiumUtils.clickOnId(mSolo, "signup_button");
        assertTrue(mSolo.searchText(mSolo.getString(R.string.blog_name_reserved)));
    }

    public void testCreateAccountTimeout() throws Exception {
        login();
        RestClientFactoryTest.setPrefixAllInstances("timeout");
        mSolo.clickOnActionBarItem(R.id.menu_settings);
        mSolo.clickOnText(mSolo.getString(R.string.create_new_blog_wpcom));
        mSolo.enterText(0, "timeout");
        RobotiumUtils.clickOnId(mSolo, "signup_button");
        assertTrue(mSolo.searchText(mSolo.getString(R.string.error)));
    }
}

