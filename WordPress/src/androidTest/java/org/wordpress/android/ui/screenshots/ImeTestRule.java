package org.wordpress.android.ui.screenshots;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import androidx.test.platform.app.InstrumentationRegistry;

import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Objects;

// This TestRule allows to change the Input Method Editor (IME) before a test.
//
// This is especially useful when you run an instrumentation test (like for screenshots)
// in a language like `zh-CN` (Chinese) and the System would otherwise show you a prompt
// when you enter an input field to ask if you want to grant some permissions to the Pinyin IME.
//
// By explicitly setting the IME to Latin instead, this avoids the prompt and thus interrupting the tests.
public class ImeTestRule implements TestRule {
    private final String mAllowedIme;

    public static final String LATIN_IME = "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME";

    public ImeTestRule() {
        this(LATIN_IME);
    }

    public ImeTestRule(@NotNull String allowedIme) {
        Objects.requireNonNull(allowedIme);
        this.mAllowedIme = allowedIme;
    }

    @Override
    public Statement apply(@NotNull final Statement base, @NotNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String original = null;
                try {
                    forceIme(mAllowedIme);
                    base.evaluate();
                } finally {
                    if (original != null) {
                        forceIme(original);
                    }
                }
            }
        };
    }

    private void forceIme(String ime) {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.setInputMethod(null, ime);
    }
}
