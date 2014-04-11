package org.wordpress.android.util;

public class WPStatsTrackerMixpanelInstructionsForStat {

    public static WPStatsTrackerMixpanelInstructionsForStat mixpanelInstructionsForEventName(String eventName) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setMixpanelEventName(eventName);
        return instructions;
    }

    private String mixpanelEventName;

    public String getMixpanelEventName() {
        return mixpanelEventName;
    }

    public void setMixpanelEventName(String mixpanelEventName) {
        this.mixpanelEventName = mixpanelEventName;
    }
}
