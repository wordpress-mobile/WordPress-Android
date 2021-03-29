package org.wordpress.android.e2e.pages;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class BlockEditorPage {
    private static ViewInteraction titleField = onView(withHint("Add title"));

    private ViewInteraction mEditor;

    public BlockEditorPage() {
        mEditor = onView(withId(R.id.gutenberg_container));
        mEditor.check(matches(isDisplayed()));
    }

    public void waitForTitleDisplayed() {
        waitForElementToBeDisplayed(titleField);
    }

    public void enterTitle(String postTitle) {
        titleField.perform(typeText(postTitle), ViewActions.closeSoftKeyboard());
    }

    public void previewPost() {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        clickOn(onView(withText(R.string.menu_preview)));
    }
}
