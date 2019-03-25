package org.wordpress.android.e2e;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.components.MasterbarComponent;
import org.wordpress.android.e2e.pages.EditorPage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.WPLaunchActivity;

import static junit.framework.TestCase.assertTrue;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_SITE_ADDRESS;
import static org.wordpress.android.support.WPSupportUtils.sleep;

public class EditorTests extends BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Test
    public void testPublishPost() {
        String title = "Title";
        String content = "Content";

        wpLogin();

        MasterbarComponent mb = new MasterbarComponent().goToMySitesTab();
        sleep();
        mb.clickBlogPosts();

        new MySitesPage()
                .startNewPost(E2E_WP_COM_USER_SITE_ADDRESS);

        boolean isPublished = new EditorPage()
                .enterTitle(title)
                .enterContent(content)
                .publishPost();
        assertTrue(isPublished);
    }
}
