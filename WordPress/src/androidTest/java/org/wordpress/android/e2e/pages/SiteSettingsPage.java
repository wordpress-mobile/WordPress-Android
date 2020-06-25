package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.PreferenceMatchers.withTitle;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.ensureSwitchPreferenceIsChecked;

public class SiteSettingsPage {
    private static ViewInteraction settings = onView(withText(R.string.site_settings_title_title));

    public SiteSettingsPage() {
        settings.check(matches(isDisplayed()));
    }

    public void setEditorToClassic() {
        onData(withTitle(R.string.site_settings_gutenberg_default_for_new_posts))
                .perform(scrollTo())
                .perform(ensureSwitchPreferenceIsChecked(false));
    }

    public void setEditorToGutenberg() {
        onData(withTitle(R.string.site_settings_gutenberg_default_for_new_posts))
                .perform(scrollTo())
                .perform(ensureSwitchPreferenceIsChecked(true));
    }
}
