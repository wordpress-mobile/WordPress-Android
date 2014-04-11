package org.wordpress.android.util;

import org.json.JSONObject;

public class WPStatsTrackerMixpanel implements WPStats.Tracker {

    @Override
    public void track(WPStats.Stat stat) {
        System.out.println("Tracking Mixpanel Stat");
    }

    @Override
    public void track(WPStats.Stat stat, JSONObject properties) {
        System.out.println("Tracking Mixpanel Stat with Properties");
    }

    @Override
    public void beginSession() {
        System.out.println("Beginning Session for Mixpanel");
    }

    @Override
    public void endSession() {
        System.out.println("Ending Session for Mixpanel");
    }
}
