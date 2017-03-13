package org.wordpress.android.fluxc.utils;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class ErrorUtils {
    public static class OnUnexpectedError {
        public Exception exception;
        public String description;
        public AppLog.T type;

        public OnUnexpectedError(Exception exception) {
            this(exception, "");
        }

        public OnUnexpectedError(Exception exception, String description) {
            this(exception, description, T.API);
        }

        public OnUnexpectedError(Exception exception, String description, T type) {
            this.exception = exception;
            this.description = description;
            this.type = type;
        }
    }
}
