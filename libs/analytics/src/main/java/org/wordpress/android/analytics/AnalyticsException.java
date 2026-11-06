package org.wordpress.android.analytics;

// CI probe: library changes must keep library lint enabled.
public class AnalyticsException extends Exception {
    public AnalyticsException(String message) {
        super(message);
    }
}
