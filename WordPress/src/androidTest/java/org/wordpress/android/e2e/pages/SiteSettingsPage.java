package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class SiteSettingsPage {
    private static ViewInteraction settings = onView(withText(R.string.site_settings_title_title));

    public SiteSettingsPage() {
        settings.check(matches(isDisplayed()));
    }

    public void toggleGutenbergSetting() {
        onView(withText(R.string.site_settings_gutenberg_default_for_new_posts))
                .perform(scrollTo())
                .perform((click()));
    }
}
