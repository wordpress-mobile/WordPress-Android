package org.wordpress.android.util;


import android.text.Editable;
import android.text.TextWatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WPLowerCaseTextWatcher implements TextWatcher {

    private static final Pattern UPPER_CASE_REGEX = Pattern.compile("[A-Z]");

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        Matcher matcher = UPPER_CASE_REGEX.matcher(s);
        while (matcher.find()) {
            CharSequence upperCaseRegion = s.subSequence(matcher.start(), matcher.end());
            s.replace(matcher.start(), matcher.end(), upperCaseRegion.toString().toLowerCase());
        }
    }

}
