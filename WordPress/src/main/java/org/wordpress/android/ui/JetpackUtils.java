package org.wordpress.android.ui;

import org.wordpress.android.analytics.AnalyticsTracker;

import java.util.HashMap;

/**
 * Wraps utility methods for Jetpack
 */
public class JetpackUtils {
    private JetpackUtils() {
    }

    /**
     * Adds source as a parameter to the tracked Stat
     * @param stat to be tracked
     * @param source of tracking
     */
    static void trackWithSource(AnalyticsTracker.Stat stat, JetpackConnectionWebViewActivity.Source source) {
        HashMap<String, String> sourceMap = new HashMap<>();
        sourceMap.put("source", source.value());
        AnalyticsTracker.track(stat, sourceMap);
    }
}
