package org.wordpress.android.e2e.pages;

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToNotBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.withIndex;

public class EditorPage {
    private static ViewInteraction publishButton = onView(withId(R.id.menu_save_post));
    private static ViewInteraction editor = onView(withId(R.id.aztec));
    private static ViewInteraction titleField = onView(allOf(withId(R.id.title),
            withHint("Title")));
    private static ViewInteraction publishConfirmation = onView(withText("Post published"));
    private static ViewInteraction addMediaButton = onView(withId(R.id.media_button_container));
    private static ViewInteraction allowMediaAccessButton = onView(allOf(withId(R.id.button),
            withText("Allow")));
    private static ViewInteraction confirmButton = onView(withId(R.id.mnu_confirm_selection));

    public EditorPage() {
        editor.check(matches(isDisplayed()));
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
        addMediaButton.perform(click());

        if (isElementDisplayed(allowMediaAccessButton)) {
            // Click on Allow button
            allowMediaAccessButton.perform(click());

            // Accept alert for media access
            onView(withText("ALLOW")).inRoot(isDialog()).perform(click());
        }

        // Click on a random image
        onView(withIndex(withId(R.id.image_thumbnail), 0)).perform(click());

        // Click the confirm button
        confirmButton.perform(click());

        if (isElementDisplayed(onView(withText("LEAVE OFF")))) {
            // Accept alert for media access
            onView(withText("LEAVE OFF")).inRoot(isDialog()).perform(click());
        }
    }

    public void openSettings() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Post settings")).perform(click());
    }

    public void addACategory(String category) {
        onView(withId(R.id.post_categories_container)).perform(click());
        onView(withText(category)).perform(click());
        pressBack();
    }

    public void addATag(String tag) {
        onView(withId(R.id.post_tags_container)).perform(click());
        ViewInteraction tagsField = onView(withId(R.id.tags_edit_text));
        populateTextField(tagsField, tag);
        pressBack();
    }

    public void setFeaturedImage() {
        onView(withId(R.id.post_add_featured_image_button)).perform(click());
        onView(withIndex(withId(R.id.image_thumbnail), 0)).perform(click());
        onView(withId(R.id.mnu_confirm_selection)).perform(click());
        waitForElementToNotBeDisplayed(onView(withText(R.string.uploading_media)));
    }

    public boolean publishPost() {
        publishButton.perform(click());
        onView(withText("PUBLISH NOW"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());
        waitForElementToBeDisplayed(publishConfirmation);
        return isElementDisplayed(publishConfirmation);
    }
}
