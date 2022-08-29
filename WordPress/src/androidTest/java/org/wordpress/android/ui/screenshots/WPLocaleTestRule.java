package org.wordpress.android.ui.screenshots;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.LocaleManager;

import tools.fastlane.screengrab.locale.LocaleUtil;

public class WPLocaleTestRule implements TestRule {
    private String mTestLocaleCode;

    public WPLocaleTestRule() {
        this(LocaleUtil.getTestLocale());
    }

    public WPLocaleTestRule(String testLocaleCode) {
        mTestLocaleCode = testLocaleCode;
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
