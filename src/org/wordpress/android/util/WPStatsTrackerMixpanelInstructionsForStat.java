package org.wordpress.android.util;

public class WPStatsTrackerMixpanelInstructionsForStat {
    private String mMixpanelEventName;
    private String mSuperPropertyToIncrement;
    private String mPeoplePropertyToIncrement;
    private String mSuperPropertyToFlag;
    private WPStats.Stat mStatToAttachProperty;
    private WPStats.Stat mStat;
    private String mPropertyToIncrement;
    private boolean mDisableForSelfHosted;

    public static WPStatsTrackerMixpanelInstructionsForStat mixpanelInstructionsForEventName(String eventName) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setMixpanelEventName(eventName);
        return instructions;
    }

    public static WPStatsTrackerMixpanelInstructionsForStat
    mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(String property) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setSuperPropertyAndPeoplePropertyToIncrement(property);
        return instructions;
    }

    public static WPStatsTrackerMixpanelInstructionsForStat mixpanelInstructionsWithPropertyIncrementor(
            String property, WPStats.Stat stat) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = new WPStatsTrackerMixpanelInstructionsForStat();
        instructions.setStatToAttachProperty(stat);
        instructions.setPropertyToIncrement(property);
        return instructions;
    }

    public String getMixpanelEventName() {
        return mMixpanelEventName;
    }

    public void setMixpanelEventName(String mixpanelEventName) {
        this.mMixpanelEventName = mixpanelEventName;
    }

    public String getSuperPropertyToIncrement() {
        return mSuperPropertyToIncrement;
    }

    public void setSuperPropertyToIncrement(String superPropertyToIncrement) {
        this.mSuperPropertyToIncrement = superPropertyToIncrement;
    }

    public String getPeoplePropertyToIncrement() {
        return mPeoplePropertyToIncrement;
    }

    public void setPeoplePropertyToIncrement(String peoplePropertyToIncrement) {
        this.mPeoplePropertyToIncrement = peoplePropertyToIncrement;
    }

    public void setSuperPropertyAndPeoplePropertyToIncrement(String property) {
        setSuperPropertyToIncrement(property);
        setPeoplePropertyToIncrement(property);
    }

    public WPStats.Stat getStatToAttachProperty() {
        return mStatToAttachProperty;
    }

    public void setStatToAttachProperty(WPStats.Stat statToAttachProperty) {
        this.mStatToAttachProperty = statToAttachProperty;
    }

    public String getPropertyToIncrement() {
        return mPropertyToIncrement;
    }

    public void setPropertyToIncrement(String propertyToIncrement) {
        this.mPropertyToIncrement = propertyToIncrement;
    }

    public boolean getDisableForSelfHosted() {
        return mDisableForSelfHosted;
    }

    public void setDisableForSelfHosted(boolean disableForSelfHosted) {
        this.mDisableForSelfHosted = disableForSelfHosted;
    }

    public WPStats.Stat getStat() {
        return mStat;
    }

    public void setStat(WPStats.Stat stat) {
        this.mStat = stat;
    }

    public String getSuperPropertyToFlag() {
        return mSuperPropertyToFlag;
    }

    public void setSuperPropertyToFlag(String superPropertyToFlag) {
        this.mSuperPropertyToFlag = superPropertyToFlag;
    }
}
