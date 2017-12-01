package org.wordpress.android.ui.end2end.tests;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.end2end.components.MasterbarComponent;
import org.wordpress.android.ui.end2end.components.SnackbarComponent;
import org.wordpress.android.ui.end2end.flows.LoginFlow;
import org.wordpress.android.ui.end2end.pages.AppSettingsPage;
import org.wordpress.android.ui.end2end.pages.EditorPage;
import org.wordpress.android.ui.end2end.pages.MePage;
import org.wordpress.android.ui.end2end.pages.MySitesPage;

public class EditorTests extends BaseTest {

    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Test
    public void testPublishPost() {
        String siteAddress = (String) WordPress.getBuildConfigValue(mActivityTestRule.getActivity().getApplication(), "DEBUG_E2ETEST_DEFAULT_URL");
        String email = (String) WordPress.getBuildConfigValue(mActivityTestRule.getActivity().getApplication(), "DEBUG_E2ETEST_DEFAULT_EMAIL");
        String password = (String) WordPress.getBuildConfigValue(mActivityTestRule.getActivity().getApplication(), "DEBUG_E2ETEST_DEFAULT_PASSWORD");

        String title = "Title";
        String content = "Content";

        new LoginFlow()
                .wpcomLoginEmailPassword(email, password);

        // Confirm Aztec is enabled
        new MasterbarComponent()
                .goToMeTab();

        new MePage()
                .openAppSettings();

        // Workaround to set Aztec as the editor
        // See https://github.com/wordpress-mobile/WordPress-Android/issues/6932
        new AppSettingsPage()
                .setEditor("Visual")
                .setEditor("Beta")
                .goBack();

        new MasterbarComponent()
                .goToMySitesTab();

        new MySitesPage()
                .startNewPost(siteAddress);

        new EditorPage()
                .enterTitle(title)
                .enterContent(content)
                .publishPost();

        new MySitesPage();

        // Need to wait for the post to be published.
        // Added a sleep statement for now.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new SnackbarComponent()
                .verifyPostPublished();
    }
}
