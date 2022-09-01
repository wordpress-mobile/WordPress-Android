package org.wordpress.android.ui.screenshots;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.LocaleManager;

import java.util.Locale;

import tools.fastlane.screengrab.locale.LocaleUtil;

public class WPLocaleTestRule implements TestRule {
    private String mTestLocaleCode;

    public WPLocaleTestRule() {
        this(LocaleUtil.getTestLocale());
    }

    public WPLocaleTestRule(String testLocaleCode) {
        // Parse as `Locale` then back to `String`, to ensure that when we get `fr-FR` as input
        // (which is what is expected to be passed via fastlane's Screengrab and `LocaleUtil.getTestLocale()`),
        // we get `fr_FR` (the locale code format expected by `java.util.Locale` and our `LocaleManager`) back.
        Locale locale = LocaleUtil.localeFromString(testLocaleCode);
        if (locale != null) {
            mTestLocaleCode = locale.toString();
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String original = null;
                try {
                    if (mTestLocaleCode != null) {
                        original = changeLocale(mTestLocaleCode);
                    }
                    base.evaluate();
                } finally {
                    if (original != null) {
                        changeLocale(original);
                    }
                }
            }
        };
    }

    private static String changeLocale(String localeCode) {
        Context context = ApplicationProvider.getApplicationContext();
        String original = LocaleManager.getLanguage(context);
        LocaleManager.setNewLocale(context, localeCode);
        WordPress.updateContextLocale(context);
        return original;
    }
}
