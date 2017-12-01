package org.wordpress.android.ui.end2end.pages;

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class EditorPage {

    private static ViewInteraction postSettingsButton = onView(withId(R.id.menu_post_settings));
    private static ViewInteraction publishButton = onView(withId(R.id.menu_save_post));
    private static ViewInteraction undoButton = onView(withId(R.id.undo));
    private static ViewInteraction redoButton = onView(withId(R.id.redo));

    private static ViewInteraction editor = onView(withId(R.id.aztec));
    private static ViewInteraction htmlEditor = onView(withId(R.id.source));

    private static ViewInteraction titleField = onView(withId(R.id.title));

    private static ViewInteraction insertMediaButton = onView(withId(R.id.format_bar_button_media));
    private static ViewInteraction headingButton = onView(withId(R.id.format_bar_button_heading));
    private static ViewInteraction listButton = onView(withId(R.id.format_bar_button_list));
    private static ViewInteraction quoteButton = onView(withId(R.id.format_bar_button_quote));
    private static ViewInteraction boldButton = onView(withId(R.id.format_bar_button_bold));
    private static ViewInteraction italicsButton = onView(withId(R.id.format_bar_button_italic));
    private static ViewInteraction linkButton = onView(withId(R.id.format_bar_button_link));
    private static ViewInteraction underlineButton = onView(withId(R.id.format_bar_button_underline));
    private static ViewInteraction strikethroughButton = onView(withId(R.id.format_bar_button_strikethrough));
    private static ViewInteraction horizontalButton = onView(withId(R.id.format_bar_button_horizontal_rule));
    private static ViewInteraction moreRuleButton = onView(withId(R.id.format_bar_button_more));
    private static ViewInteraction pageButton = onView(withId(R.id.format_bar_button_page));
    private static ViewInteraction htmlButton = onView(withId(R.id.format_bar_button_html));

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

    public void publishPost() {
        publishButton.perform(click());
    }
}
