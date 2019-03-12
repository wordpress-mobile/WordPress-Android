package org.wordpress.android.support;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Matcher;

import static android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;

public class FlashCaretViewAction implements ViewAction {
    @Override
    public void perform(UiController uiController, View view) {
        if (!(view instanceof EditText)) {
            return;
        }

        EditText et = (EditText) view;

        // disable the suggestions UI to prevent word underlining
        et.setInputType(TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        // The best way to get the caret to show at a given time is to request focus – that resets the
        // cursor loop (and as a bonus, moves the cursor to the end of the EditText).
        // If, for some reason, this doesn't work consistently for us, an alternative approach would be to
        // reverse-engineer how `TextView` draws the caret – in `getUpdatedHighlightPath`, the logic for how the
        // whether to show the caret is defined:
        //
        // (SystemClock.uptimeMillis() - mEditor.mShowCursor) % (2 * Editor.BLINK) < Editor.BLINK
        //
        // Right now this is simpler though.
        et.clearFocus();
        et.requestFocus();
    }

    @Override
    public Matcher<View> getConstraints() {
        return allOf(instanceOf(EditText.class));
    }

    @Override
    public String getDescription() {
        return "Moving Text Edit Caret to the end of the text";
    }
}
