package org.wordpress.android.ui.screenshots;

import android.Manifest;
import android.content.Context;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Objects;

// This TestRule allows to change the Input Method Editor (IME) before a test.
// Note: It requires the `WRITE_SECURE_SETTINGS` permission being set in your `AndroidManifest.xml`
//
// This is especially useful when you run an instrumentation test (like for screenshots)
// in a language like `zh-CN` (Chinese) and the System would otherwise show you a prompt
// when you enter an input field, to ask if you want to grant some permissions to the Pinyin IME.
//
// By explicitly setting the IME to Latin instead, this avoids the prompt and thus avoids interrupting the tests.
public class ImeTestRule implements TestRule {
    private final String mAllowedIme;

    public static final String LATIN_IME
            = "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME";

    public ImeTestRule() {
        this(LATIN_IME);
    }

    public ImeTestRule(@NotNull String allowedIme) {
        Objects.requireNonNull(allowedIme);
        this.mAllowedIme = allowedIme;
    }

    @Override
    public Statement apply(@NotNull final Statement base, @NotNull Description description) {
        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String original = null;
                try {
                    original = forceIme(mAllowedIme);
                    base.evaluate();
                } finally {
                    if (original != null) {
                        forceIme(original);
                    }
                }
            }
        };
        // Apply GrantPermissionRule first, then this ImeTestRule next
        return GrantPermissionRule.grant(Manifest.permission.WRITE_SECURE_SETTINGS).apply(statement, description);
    }

    private String forceIme(String ime) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String original = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.setInputMethod(null, ime);
        return original;
    }
}
