package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.pages.ReaderPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.WPLaunchActivity;


public class ReaderTests extends BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Rule
    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
        new ReaderPage().go();
    }

    @Test
    public void viewPost() {
        String title = "Sit Elit Adipiscing Elit Dolor Lorem";

        new ReaderPage()
                .tapFollowingTab()
                .openPost(title);
    }
}
