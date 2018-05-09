package org.wordpress.android.ui;

import org.wordpress.android.analytics.AnalyticsTracker;

import java.util.HashMap;

/**
 * Wraps utility methods for Jetpack
 */
class JetpackConnectionUtils {
    /**
     * Adds source as a parameter to the tracked Stat
     * @param stat to be tracked
     * @param source of tracking
     */
    static void trackWithSource(AnalyticsTracker.Stat stat, JetpackConnectionSource source) {
        HashMap<String, String> sourceMap = new HashMap<>();
        sourceMap.put("source", source.toString());
        AnalyticsTracker.track(stat, sourceMap);
    }

    /**
     * Adds source and reason as a parameter to the tracked Stat
     * @param source of tracking
     */
    static void trackFailureWithSource(JetpackConnectionSource source,
                                       String failureReason) {
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("source", source.toString());
        paramMap.put("reason", failureReason);
        AnalyticsTracker.track(AnalyticsTracker.Stat.CONNECT_JETPACK_FAILED, paramMap);
    }
}
