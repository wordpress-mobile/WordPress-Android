package org.wordpress.android.ui.screenshots;

import android.support.test.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.LocaleManager;

public class WPLocaleTestRule implements TestRule {
    private static final String FASTLANE_TEST_LOCALE_KEY = "testLocale";
    private static final String FASTLANE_ENDING_LOCALE_KEY = "endingLocale";

    private String mTestLocaleCode;
    private String mEndLocaleCode;

    public WPLocaleTestRule() {
        this(localeCodeFromInstrumentation(FASTLANE_TEST_LOCALE_KEY),
                localeCodeFromInstrumentation(FASTLANE_ENDING_LOCALE_KEY));
    }

    public WPLocaleTestRule(String testLocaleCode, String endLocaleCode) {
        mTestLocaleCode = testLocaleCode;
        mEndLocaleCode = endLocaleCode;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    if (mTestLocaleCode != null) {
                        changeLocale(mTestLocaleCode);
                    }
                    base.evaluate();
                } finally {
                    if (mEndLocaleCode != null) {
                        changeLocale(mEndLocaleCode);
                    }
                }
            }
        };
    }

    private static void changeLocale(String localeCode) {
        LocaleManager.setNewLocale(InstrumentationRegistry.getTargetContext(), localeCode);
        WordPress.updateContextLocale();
    }

    private static String localeCodeFromInstrumentation(String key) {
        return InstrumentationRegistry.getArguments().getString(key);
    }
}
