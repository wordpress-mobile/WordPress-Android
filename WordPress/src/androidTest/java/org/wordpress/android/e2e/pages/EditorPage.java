package org.wordpress.android.e2e.pages;

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;

import org.hamcrest.CoreMatchers;
import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
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
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class EditorPage {
    private static ViewInteraction publishButton = onView(withId(R.id.menu_save_post));

    private static ViewInteraction editor = onView(withId(R.id.aztec));

    private static ViewInteraction titleField = onView(allOf(withId(R.id.title), withHint("Title")));

    private static ViewInteraction publishConfirmation = onView(
            CoreMatchers.allOf(withId(android.support.design.R.id.snackbar_text),
                    withText("Post published")));

    public EditorPage() {
        editor.check(matches(isDisplayed()));
    }

    public EditorPage enterTitle(String postTitle) {
        titleField.perform(typeText(postTitle), ViewActions.closeSoftKeyboard());

        return this;
    }

    public EditorPage enterContent(String postContent) {
        editor.perform(typeText(postContent), ViewActions.closeSoftKeyboard());

        return this;
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
