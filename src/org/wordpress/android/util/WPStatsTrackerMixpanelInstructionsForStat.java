package org.wordpress.android.util;

public class WPStatsTrackerMixpanelInstructionsForStat {

    private String mixpanelEventName;
    private String superPropertyToIncrement;
    private String peoplePropertyToIncrement;
    private String superPropertyToFlag;
    private WPStats.Stat statToAttachProperty;
    private WPStats.Stat stat;
    private String propertyToIncrement;
    private boolean disableForSelfHosted;

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

    public static WPStatsTrackerMixpanelInstructionsForStat mixpanelInstructionsWithPropertyIncrementor(String property, WPStats.Stat stat) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setStatToAttachProperty(stat);
        instructions.setPropertyToIncrement(property);
        return instructions;
    }

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

    public WPStats.Stat getStatToAttachProperty() {
        return statToAttachProperty;
    }

    public void setStatToAttachProperty(WPStats.Stat statToAttachProperty) {
        this.statToAttachProperty = statToAttachProperty;
    }

    public String getPropertyToIncrement() {
        return propertyToIncrement;
    }

    public void setPropertyToIncrement(String propertyToIncrement) {
        this.propertyToIncrement = propertyToIncrement;
    }

    public boolean getDisableForSelfHosted() {
        return disableForSelfHosted;
    }

    public void setDisableForSelfHosted(boolean disableForSelfHosted) {
        this.disableForSelfHosted = disableForSelfHosted;
    }

    public WPStats.Stat getStat() {
        return stat;
    }

    public void setStat(WPStats.Stat stat) {
        this.stat = stat;
    }

    public String getSuperPropertyToFlag() {
        return superPropertyToFlag;
    }

    public void setSuperPropertyToFlag(String superPropertyToFlag) {
        this.superPropertyToFlag = superPropertyToFlag;
    }
}
