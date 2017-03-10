package org.wordpress.android.fluxc.utils;

import com.android.volley.ParseError;

public class ErrorUtils {
    public static class OnParseError {
        public ParseError parseError;
        public OnParseError(ParseError parseError) {
            this.parseError = parseError;
        }
    }
}
