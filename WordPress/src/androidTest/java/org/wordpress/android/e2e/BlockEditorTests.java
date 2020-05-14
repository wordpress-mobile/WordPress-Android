package org.wordpress.android.e2e;
//
//import android.Manifest.permission;
//
//import androidx.test.rule.ActivityTestRule;
//import androidx.test.rule.GrantPermissionRule;
//
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.wordpress.android.e2e.components.MasterbarComponent;
//import org.wordpress.android.e2e.pages.BlockEditorPage;
//import org.wordpress.android.e2e.pages.EditorPage;
//import org.wordpress.android.e2e.pages.MySitesPage;
//import org.wordpress.android.e2e.pages.PostPreviewPage;
//import org.wordpress.android.e2e.pages.SiteSettingsPage;
//import org.wordpress.android.support.BaseTest;
//import org.wordpress.android.ui.WPLaunchActivity;
//
//import static androidx.test.espresso.Espresso.pressBack;
//import static org.wordpress.android.support.WPSupportUtils.sleep;
//
//public class BlockEditorTests extends BaseTest {
//    @Rule
//    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);
//
//    @Rule
//    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);
//
//    @Before
//    public void setUp() {
//        logoutIfNecessary();
//        wpLogin();
//    }
//
//    @Test
//    public void testSwitchToClassicAndPreview() {
//        String title = "Hello Espresso!";
//
//        MasterbarComponent mb = new MasterbarComponent().goToMySitesTab();
//        sleep();
//
//        MySitesPage mySitesPage = new MySitesPage();
//        mySitesPage.gotoSiteSettings();
//
//        // Set to Gutenberg. Apparently the site is defaulting to Aztec still.
//        new SiteSettingsPage().toggleGutenbergSetting();
//
//        // exit the Settings page
//        pressBack();
//
//        mb.clickBlogPosts();
//
//        new MySitesPage()
//                .startNewPost();
//
//        BlockEditorPage blockEditorPage = new BlockEditorPage();
//        blockEditorPage.waitForTitleDisplayed();
//
//        blockEditorPage.enterTitle(title);
//
//        blockEditorPage.switchToClassic();
//
//        EditorPage editorPage = new EditorPage();
//        editorPage.hasTitle(title);
//
//        editorPage.previewPost();
//        sleep();
//
//        new PostPreviewPage();
//    }
//}
