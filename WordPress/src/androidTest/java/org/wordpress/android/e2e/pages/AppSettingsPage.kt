package org.wordpress.android.e2e.pages

import android.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import junit.framework.AssertionFailedError
import org.hamcrest.Matchers

class AppSettingsPage {
    init {
        appSettingsLabel.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun setEditor(editorType: String?): AppSettingsPage {
        try {
            editorTypeButton.check(ViewAssertions.matches(ViewMatchers.withText(editorType)))
        } catch (e: AssertionFailedError) {
            editorTypeButton.perform(ViewActions.click())
            Espresso.onView(
                Matchers.allOf(
                    ViewMatchers.withId(R.id.text1),
                    ViewMatchers.withText(editorType)
                )
            ).perform(ViewActions.click())
        }
        return this
    }

    fun goBack() {
        backArrow.perform(ViewActions.click())
    }

    companion object {
        private val appSettingsLabel = Espresso.onView(ViewMatchers.withText("App Settings"))
        private val backArrow = Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
        private val editorTypeButton = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.summary),
                ViewMatchers.hasSibling(ViewMatchers.withText("Set editor type"))
            )
        )
    }
}
