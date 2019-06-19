package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;

import junit.framework.AssertionFailedError;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class AppSettingsPage {
    private static ViewInteraction appSettingsLabel = onView(withText("App Settings"));
    private static ViewInteraction backArrow = onView(withContentDescription("Navigate up"));

    private static ViewInteraction editorTypeButton = onView(allOf(withId(android.R.id.summary),
            hasSibling(withText("Set editor type"))));

    public AppSettingsPage() {
        appSettingsLabel.check(matches(isDisplayed()));
    }

    public AppSettingsPage setEditor(String editorType) {
        try {
            editorTypeButton.check(matches(withText(editorType)));
        } catch (AssertionFailedError e) {
            editorTypeButton.perform(click());
            onView(allOf(withId(android.R.id.text1), withText(editorType))).perform(click());
        }

        return this;
    }

    public void goBack() {
        backArrow.perform(click());
    }
}
