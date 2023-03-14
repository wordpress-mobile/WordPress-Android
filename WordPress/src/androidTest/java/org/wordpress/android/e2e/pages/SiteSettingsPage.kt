package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.PreferenceMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.wordpress.android.R
import org.wordpress.android.support.BetterScrollToAction.Companion.scrollTo
import org.wordpress.android.support.WPSupportUtils

class SiteSettingsPage {
    init {
        settings.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun setEditorToClassic() {
        Espresso.onData(PreferenceMatchers.withTitle(R.string.site_settings_gutenberg_default_for_new_posts))
            .perform(scrollTo())
            .perform(WPSupportUtils.ensureSwitchPreferenceIsChecked(false))
    }

    fun setEditorToGutenberg() {
        Espresso.onData(PreferenceMatchers.withTitle(R.string.site_settings_gutenberg_default_for_new_posts))
            .perform(scrollTo())
            .perform(WPSupportUtils.ensureSwitchPreferenceIsChecked(true))
    }

    companion object {
        private val settings =
            Espresso.onView(ViewMatchers.withText(R.string.site_settings_title_title))
    }
}
