package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.checkViewHasText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.withIndex;

public class EditorPage {
    private static ViewInteraction publishButton = onView(withId(R.id.menu_primary_action));
    private static ViewInteraction optionsButton = onView(withContentDescription("More options"));
    private static ViewInteraction editor = onView(withId(R.id.aztec));
    private static ViewInteraction titleField = onView(allOf(withId(R.id.title),
            withHint("Title")));
    private static ViewInteraction publishConfirmation = onView(allOf(
            withText("Post published"), isDescendantOfA(withId(R.id.snackbar))));
    private static ViewInteraction allowMediaAccessButton = onView(allOf(withId(R.id.button),
            withText("Allow")));
    private static ViewInteraction confirmButton = onView(withId(R.id.mnu_confirm_selection));


    public EditorPage() {
        editor.check(matches(isDisplayed()));
    }

    public void hasTitle(String title) {
        checkViewHasText(titleField, title);
    }

    public void enterTitle(String postTitle) {
        titleField.perform(typeText(postTitle), ViewActions.closeSoftKeyboard());
    }

    public void enterContent(String postContent) {
        editor.perform(typeText(postContent), ViewActions.closeSoftKeyboard());
    }

    // Image needs a little time to be uploaded after entering the image
    public void enterImage() {
        // Click on add media button
        String addMediaButtonId = "id/media_button_container";
        clickOn(addMediaButtonId);

        String mediaBarButtonId = "id/media_bar_button_library";
        clickOn(mediaBarButtonId);

        if (isElementDisplayed(allowMediaAccessButton)) {
            // Click on Allow button
            clickOn(allowMediaAccessButton);
        }

        // Click on a random image
        waitForElementToBeDisplayed(onView(withText("WordPress media")));
        // wait for images to load before clicking
        idleFor(2000);
        onView(withIndex(withId(R.id.media_grid_item_image), 0)).perform(click());

        // Click the confirm button
        clickOn(confirmButton);

        if (isElementDisplayed(onView(withText("LEAVE OFF")))) {
            // Accept alert for media access
            clickOn(onView(withText("LEAVE OFF")).inRoot(isDialog()));
        }

        waitForElementToBeDisplayed(publishButton);
    }

    public void openSettings() {
        clickOn(optionsButton);
        clickOn("Post settings");
    }

    public void addACategory(String category) {
        clickOn(onView(withId(R.id.post_categories_container)));
        clickOn(onView(withText(category)));
        pressBack();
    }

    public void addATag(String tag) {
        clickOn(onView(withId(R.id.post_tags_container)));
        ViewInteraction tagsField = onView(withId(R.id.tags_edit_text));
        populateTextField(tagsField, tag);
        pressBack();
    }

    public void setFeaturedImage() {
        clickOn(onView(withId(R.id.post_add_featured_image_button)));
        clickOn(onView(withId(R.id.icon_wpmedia)));
        onView(withIndex(withId(R.id.media_grid_item_image), 0)).perform(click());
    }

    public boolean publishPost() {
        clickOn(publishButton);
        clickOn(onView(withText("PUBLISH NOW")));
        waitForElementToBeDisplayed(publishConfirmation);
        return isElementDisplayed(publishConfirmation);
    }

    public void previewPost() {
        clickOn(optionsButton);
        clickOn(onView(withText(R.string.menu_preview)));
    }
}
