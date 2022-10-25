package org.wordpress.android.e2e.pages;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.clickOnViewWithTag;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;
import static junit.framework.TestCase.assertTrue;
import static org.wordpress.android.support.WPSupportUtils.withIndex;

public class BlockEditorPage {
    private static ViewInteraction titleField = onView(withHint("Add title"));
    private static ViewInteraction postSettingButton = onView(withText(R.string.post_settings));

    private ViewInteraction mEditor;

    public BlockEditorPage() {
        mEditor = onView(withId(R.id.gutenberg_container));
        mEditor.check(matches(isDisplayed()));
    }

    public BlockEditorPage waitForTitleDisplayed() {
        waitForElementToBeDisplayed(titleField);
        return this;
    }

    public BlockEditorPage enterTitle(String postTitle) {
        clickOn(titleField);
        titleField.perform(typeText(postTitle), ViewActions.closeSoftKeyboard());
        return this;
    }

    public BlockEditorPage enterParagraphText(String paragraphText) {
        ViewInteraction startWritingPrompt = onView(withHint(R.string.gutenberg_native_start_writing));
        clickOn(startWritingPrompt);
        populateTextField(startWritingPrompt, paragraphText);
        return this;
    }

    public BlockEditorPage switchToHtmlMode() {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        clickOn(onView(withText(R.string.menu_html_mode)));
        return this;
    }

    public BlockEditorPage switchToVisualMode() {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        clickOn(onView(withText(R.string.menu_visual_mode)));
        return this;
    }

    public BlockEditorPage addPostSettings(String categoryName, String tagName) {
        openPostSetting();
        addCategory(categoryName);
        addTag(tagName);
        pressBack();
        return this;
    }

    /**
     * Taps the three vertical dots menu button located at the editor screen top right.
     * Since the tap does not always succeed on FTL, one retry is used
     */
    public void openPostKebabMenu() {
        waitForElementToBeDisplayed(R.id.toolbar_main);

        // First attempt to tap the kebab menu (three dots)
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        // Check if the attempt succeeded, retry once if not
        if (!waitForElementToBeDisplayedWithoutFailure(postSettingButton)) {
            openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        }
    }

    public void openPostSetting() {
        openPostKebabMenu();
        clickOn(postSettingButton);
    }

    public void addCategory(String category) {
        clickOn(onView(withId(R.id.post_categories_container)));
        clickOn(onView(withText(category)));
        pressBack();
    }

    public void addTag(String tag) {
        clickOn(onView(withId(R.id.post_tags_container)));
        ViewInteraction tagsField = onView(withId(R.id.tags_edit_text));
        populateTextField(tagsField, tag);
        pressBack();
    }

    public BlockEditorPage publish() {
        clickPublish();
        confirmPublish();
        return this;
    }

    public BlockEditorPage clickPublish() {
        clickOn(onView(withResourceName("menu_primary_action")));
        return this;
    }

    public BlockEditorPage confirmPublish() {
        clickOn(onView(withText(R.string.publish_now)));
        dismissBloggingRemindersAlertIfNeeded();
        return this;
    }

    public void dismissBloggingRemindersAlertIfNeeded() {
        ViewInteraction bloggingRemindersAlertTitle = onView(withText(R.string.set_your_blogging_reminders_title));

        if (waitForElementToBeDisplayedWithoutFailure(bloggingRemindersAlertTitle)) {
            bloggingRemindersAlertTitle.perform(swipeDown());
        }
    }

    public void verifyPostPublished() {
        assertTrue("'Post published' toast was not displayed",
                waitForElementToBeDisplayedWithoutFailure(onView(withText(R.string.post_published))));
    }

    public BlockEditorPage verifyPostSettings(String categoryName, String tagName) {
        assertTrue("Category is not present in Post confirmation panel",
                waitForElementToBeDisplayedWithoutFailure(onView(withText(categoryName))));
        assertTrue("Tag is not present in Post confirmation panel",
                waitForElementToBeDisplayedWithoutFailure(onView(withText(tagName))));
        return this;
    }

    public BlockEditorPage verifyPostElementText(String postText) {
        assertTrue("Expected text is not displayed",
                waitForElementToBeDisplayedWithoutFailure(onView(withText(postText))));
        return this;
    }

    public BlockEditorPage addImage() {
        clickOnViewWithTag("add-block-button");
        clickOn("Image");
        clickOn("WordPress Media Library");
        waitForElementToBeDisplayed(onView(withText("WordPress media")));
        waitForElementToBeDisplayed(onView(withIndex(withId(R.id.text_selection_count), 0)));
        onView(withIndex(withId(R.id.text_selection_count), 0)).perform(click());
        clickOn(R.id.mnu_confirm_selection);
        return this;
    }

    public void previewPost() {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        clickOn(onView(withText(R.string.menu_preview)));
    }
}
