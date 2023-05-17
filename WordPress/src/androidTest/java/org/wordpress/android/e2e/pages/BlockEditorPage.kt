package org.wordpress.android.e2e.pages

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import junit.framework.TestCase
import org.junit.Assert
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class BlockEditorPage {
    private val mEditor: ViewInteraction

    init {
        mEditor = Espresso.onView(ViewMatchers.withId(R.id.gutenberg_container))
        mEditor.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun waitForTitleDisplayed(): BlockEditorPage {
        WPSupportUtils.waitForElementToBeDisplayed(titleField)
        return this
    }

    fun enterTitle(postTitle: String?): BlockEditorPage {
        WPSupportUtils.clickOn(titleField)
        titleField.perform(ViewActions.typeText(postTitle), ViewActions.closeSoftKeyboard())
        return this
    }

    fun enterParagraphText(paragraphText: String?): BlockEditorPage {
        val startWritingPrompt =
            Espresso.onView(ViewMatchers.withHint(R.string.gutenberg_native_start_writing))
        WPSupportUtils.clickOn(startWritingPrompt)
        WPSupportUtils.populateTextField(startWritingPrompt, paragraphText)
        return this
    }

    fun switchToHtmlMode(): BlockEditorPage {
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.menu_html_mode)))
        return this
    }

    fun switchToVisualMode(): BlockEditorPage {
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.menu_visual_mode)))
        return this
    }

    fun addPostSettings(categoryName: String?, tagName: String?): BlockEditorPage {
        openPostSetting()
        addCategory(categoryName)
        addTag(tagName)
        Espresso.pressBack()
        return this
    }

    /**
     * Taps the three vertical dots menu button located at the editor screen top right.
     * Since the tap does not always succeed on FTL, one retry is used
     */
    fun openPostKebabMenu() {
        WPSupportUtils.waitForElementToBeDisplayed(R.id.toolbar_main)

        // First attempt to tap the kebab menu (three dots)
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())

        // Check if the attempt succeeded, retry once if not
        if (!WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(postSettingButton)) {
            Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        }
        if (!WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(postSettingButton)) {
            Assert.fail("Failed to open post menu.")
        }
    }

    fun openPostSetting() {
        openPostKebabMenu()
        WPSupportUtils.clickOn(postSettingButton)
    }

    fun addCategory(category: String?) {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.post_categories_container)))
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(category)))
        Espresso.pressBack()
    }

    fun addTag(tag: String?) {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.post_tags_container)))
        val tagsField = Espresso.onView(ViewMatchers.withId(R.id.tags_edit_text))
        WPSupportUtils.populateTextField(tagsField, tag)
        Espresso.pressBack()
    }

    fun publish(): BlockEditorPage {
        clickPublish()
        confirmPublish()
        return this
    }

    fun clickPublish(): BlockEditorPage {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withResourceName("menu_primary_action")))
        return this
    }

    fun confirmPublish(): BlockEditorPage {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.publish_now)))
        dismissBloggingRemindersAlertIfNeeded()
        return this
    }

    private fun dismissBloggingRemindersAlertIfNeeded() {
        val bloggingRemindersButton = Espresso.onView(
            ViewMatchers.withText(R.string.blogging_prompt_set_reminders)
        )

        if (WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(bloggingRemindersButton)) {
            WPSupportUtils.clickOn(R.id.touch_outside)
        }
    }

    fun verifyPostPublished() {
        TestCase.assertTrue(
            "'Post published' toast was not displayed",
            WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(
                Espresso.onView(
                    ViewMatchers.withText(
                        R.string.post_published
                    )
                )
            )
        )
    }

    fun verifyPostSettings(categoryName: String?, tagName: String?): BlockEditorPage {
        TestCase.assertTrue(
            "Category is not present in Post confirmation panel",
            WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(
                Espresso.onView(
                    ViewMatchers.withText(
                        categoryName
                    )
                )
            )
        )
        TestCase.assertTrue(
            "Tag is not present in Post confirmation panel",
            WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(
                Espresso.onView(
                    ViewMatchers.withText(
                        tagName
                    )
                )
            )
        )
        return this
    }

    fun verifyPostElementText(postText: String?): BlockEditorPage {
        TestCase.assertTrue(
            "Expected text is not displayed",
            WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(
                Espresso.onView(
                    ViewMatchers.withText(
                        postText
                    )
                )
            )
        )
        return this
    }

    fun addImage(): BlockEditorPage {
        WPSupportUtils.clickOnViewWithTag("add-block-button")
        WPSupportUtils.clickOn("Image")
        WPSupportUtils.clickOn("WordPress Media Library")
        WPSupportUtils.waitForElementToBeDisplayed(Espresso.onView(ViewMatchers.withText("WordPress media")))
        WPSupportUtils.waitForElementToBeDisplayed(
            Espresso.onView(
                WPSupportUtils.withIndex(
                    ViewMatchers.withId(R.id.text_selection_count),
                    0
                )
            )
        )
        Espresso.onView(WPSupportUtils.withIndex(ViewMatchers.withId(R.id.text_selection_count), 0))
            .perform(ViewActions.click())
        WPSupportUtils.clickOn(R.id.mnu_confirm_selection)
        return this
    }

    fun previewPost() {
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.menu_preview)))
    }

    companion object {
        private val titleField = Espresso.onView(ViewMatchers.withHint("Add title"))
        private val postSettingButton =
            Espresso.onView(ViewMatchers.withText(R.string.post_settings))
    }
}
