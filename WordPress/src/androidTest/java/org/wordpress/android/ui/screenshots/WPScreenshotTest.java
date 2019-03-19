package org.wordpress.android.ui.screenshots;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.CardView;
import android.support.test.filters.LargeTest;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.util.image.ImageType;

import java.util.function.Supplier;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.clickOnChildAtIndex;
import static org.wordpress.android.support.WPSupportUtils.focusEditPostTitle;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.pressBackUntilElementIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.selectItemAtIndexInSpinner;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementOfTypeToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForAtLeastOneElementWithIdToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForConditionToBeTrue;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToNotBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForImagesOfTypeWithPlaceholder;
import static org.wordpress.android.support.WPSupportUtils.waitForRecyclerViewToStopReloading;
import static org.wordpress.android.support.WPSupportUtils.waitForSwipeRefreshLayoutToStopReloading;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WPScreenshotTest extends BaseTest {
    @ClassRule
    public static final WPLocaleTestRule LOCALE_TEST_RULE = new WPLocaleTestRule();


    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class,
            false, false);

    @Test
    public void wPScreenshotTest() {
        mActivityTestRule.launchActivity(null);
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        wpLogin();
        editBlogPost();
        navigateReader();
        navigateNotifications();
        navigateStats();
        wpLogout();
    }

    private void navigateReader() {
        // Choose the "Reader" tab in the nav
        clickOn(R.id.nav_reader);

        // Choose "Discover" from the spinner, but first, choose another item
        // to force a re-load – this avoids locale issues
        selectItemAtIndexInSpinner(getDiscoverTagIndex() == 0 ? 1 : 0, R.id.filter_spinner);
        selectItemAtIndexInSpinner(getDiscoverTagIndex(), R.id.filter_spinner);

        // Wait for the blog articles to load
        waitForAtLeastOneElementOfTypeToBeDisplayed(ReaderSiteHeaderView.class);
        waitForAtLeastOneElementOfTypeToBeDisplayed(CardView.class);
        waitForImagesOfTypeWithPlaceholder(R.id.image_featured, ImageType.PHOTO);
        waitForImagesOfTypeWithPlaceholder(R.id.image_avatar, ImageType.AVATAR);
        waitForImagesOfTypeWithPlaceholder(R.id.image_blavatar, ImageType.BLAVATAR);

        waitForRecyclerViewToStopReloading();

        Screengrab.screenshot("screenshot_2");
    }

    private void editBlogPost() {
        // Choose the "sites" tab in the nav
        clickOn(R.id.nav_sites);

        waitForImagesOfTypeWithPlaceholder(R.id.my_site_blavatar, ImageType.BLAVATAR);
        Screengrab.screenshot("screenshot_3");

        // Click on the "Blog Posts" row
        scrollToThenClickOn(R.id.row_blog_posts);

        // Wait for the blog posts to load, then edit the first post
        waitForSwipeRefreshLayoutToStopReloading();
        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.card_view);
        clickOnChildAtIndex(0, R.id.recycler_view, R.id.card_view);

        // Wait for the editor to appear and load all images
        waitForElementToBeDisplayed(R.id.aztec);
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return editPostActivityIsNoLongerLoadingImages();
            }
        });

        // Click in the post title editor and ensure the caret is at the end of the title editor
        focusEditPostTitle();

        Screengrab.screenshot("screenshot_1");

        // Exit back to the main activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private void navigateNotifications() {
        // Click on the "Notifications" tab in the nav
        clickOn(R.id.nav_notifications);

        waitForAtLeastOneElementWithIdToBeDisplayed(R.id.note_content_container);
        waitForImagesOfTypeWithPlaceholder(R.id.note_avatar, ImageType.AVATAR);

        Screengrab.screenshot("screenshot_5");
    }

    private void navigateStats() {
        // Click on the "Sites" tab in the nav, then choose "Stats"
        clickOn(R.id.nav_sites);
        scrollToThenClickOn(R.id.row_stats);

        // Wait for the dialog, but don't fai if its not there
        waitForElementToBeDisplayedWithoutFailure(R.id.promo_dialog_button_positive);
        // If there's a pop-up message, dismiss it
        if (isElementDisplayed(R.id.promo_dialog_button_positive)) {
            clickOn(R.id.promo_dialog_button_positive);
        }

        // Select "Days" from the spinner
        selectItemAtIndexInSpinner(1, R.id.filter_spinner);

        // Wait for the stats to load
        waitForElementToNotBeDisplayed(R.id.stats_empty_module_placeholder);

        Screengrab.screenshot("screenshot_4");

        // Exit the Stats Activity
        pressBackUntilElementIsDisplayed(R.id.nav_sites);
    }

    private static int getDiscoverTagIndex() {
        ReaderTagList tagList = ReaderTagTable.getDefaultTags();
        for (int i = 0; i < tagList.size(); i++) {
            ReaderTag tag = tagList.get(i);
            if (tag.isDiscover()) {
                return i;
            }
        }
        return -1;
    }

    private boolean editPostActivityIsNoLongerLoadingImages() {
        EditPostActivity editPostActivity = (EditPostActivity) getCurrentActivity();
        return editPostActivity.getAztecImageLoader().getNumberOfImagesBeingDownloaded() == 0;
    }
}
