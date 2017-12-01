package org.wordpress.android.ui.end2end.components;

import android.support.test.espresso.ViewInteraction;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class SnackbarComponent {

    private static ViewInteraction publishConfirmation = onView(allOf(withId(android.support.design.R.id.snackbar_text), withText("Post published")));
    private static ViewInteraction postUploading = onView(allOf(withId(android.support.design.R.id.snackbar_text), withText("Your post is uploading")));

    public SnackbarComponent verifyPostUploading() {
        postUploading.check(matches(isDisplayed()));

        return this;
    }

    public void verifyPostPublished() {
        publishConfirmation.check(matches(isDisplayed()));
    }
}
