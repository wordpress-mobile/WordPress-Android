package org.wordpress.android.ui.suggestion.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

public class SuggestionTokenizer implements MultiAutoCompleteTextView.Tokenizer {

    @Override
    public CharSequence terminateToken(CharSequence text) {
        int i = text.length();
        while (i > 0 && text.charAt(i - 1) == ' ') {
            i--;
        }
        if (text instanceof Spanned) {
            SpannableString sp = new SpannableString(text + " ");
            TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
            return sp;
        } else {
            return text + " ";
        }
    }

    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;
        while (i > 0 && text.charAt(i - 1) != '@') {
            i--;
        }

        if (i < 1 || text.charAt(i - 1) != '@') {
            return cursor;
        }

        return i;
    }

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();
        while (i < len) {
            if (text.charAt(i) == ' ') {
                return i;
            } else {
                i++;
            }
        }
        return len;
    }
}
