package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.pages.BlockEditorPage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.e2e.pages.PostPreviewPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.WPLaunchActivity;

import static org.wordpress.android.support.WPSupportUtils.sleep;

public class BlockEditorTests extends BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Rule
    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
    }

    @Test
    public void testPreview() {
        String title = "Hello Espresso!";

        MySitesPage mySitesPage = new MySitesPage().go();
        sleep();

        mySitesPage.startNewPost();

        BlockEditorPage blockEditorPage = new BlockEditorPage();
        blockEditorPage.waitForTitleDisplayed();

        blockEditorPage.enterTitle(title);

        blockEditorPage.previewPost();
        sleep();

        new PostPreviewPage();
    }
}
