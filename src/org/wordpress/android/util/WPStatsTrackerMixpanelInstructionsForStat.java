package org.wordpress.android.util;

public class WPStatsTrackerMixpanelInstructionsForStat {

    public static WPStatsTrackerMixpanelInstructionsForStat mixpanelInstructionsForEventName(String eventName) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setMixpanelEventName(eventName);
        return instructions;
    }

    public static WPStatsTrackerMixpanelInstructionsForStat mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(String property) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setSuperPropertyAndPeoplePropertyToIncrement(property);
        return instructions;
    }

    private String mixpanelEventName;
    private String superPropertyToIncrement;
    private String peoplePropertyToIncrement;

    public String getMixpanelEventName() {
        return mixpanelEventName;
    }

    public void setMixpanelEventName(String mixpanelEventName) {
        this.mixpanelEventName = mixpanelEventName;
    }

    public String getSuperPropertyToIncrement() {
        return superPropertyToIncrement;
    }

    public void setSuperPropertyToIncrement(String superPropertyToIncrement) {
        this.superPropertyToIncrement = superPropertyToIncrement;
    }

    public String getPeoplePropertyToIncrement() {
        return peoplePropertyToIncrement;
    }

    public void setPeoplePropertyToIncrement(String peoplePropertyToIncrement) {
        this.peoplePropertyToIncrement = peoplePropertyToIncrement;
    }

    public void setSuperPropertyAndPeoplePropertyToIncrement(String property) {
        setSuperPropertyToIncrement(property);
        setPeoplePropertyToIncrement(property);
    }
}
