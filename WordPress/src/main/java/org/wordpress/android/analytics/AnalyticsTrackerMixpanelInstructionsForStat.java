package org.wordpress.android.analytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AnalyticsTrackerMixpanelInstructionsForStat {
    private String mMixpanelEventName;
    private String mSuperPropertyToIncrement;
    private String mPeoplePropertyToIncrement;
    private ArrayList<String> mSuperPropertiesToFlag;
    private AnalyticsTracker.Stat mStatToAttachProperty;
    private AnalyticsTracker.Stat mStat;
    private String mPropertyToIncrement;
    private boolean mDisableForSelfHosted;
    private Map<String, Object> mPeoplePropertiesToAssign;

    public AnalyticsTrackerMixpanelInstructionsForStat() {
        mSuperPropertiesToFlag = new ArrayList<String>();
        mPeoplePropertiesToAssign = new HashMap<String, Object>();
    }

    public static AnalyticsTrackerMixpanelInstructionsForStat mixpanelInstructionsForEventName(String eventName) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions = new AnalyticsTrackerMixpanelInstructionsForStat();
        instructions.setMixpanelEventName(eventName);
        return instructions;
    }

    public static AnalyticsTrackerMixpanelInstructionsForStat
    mixpanelInstructionsWithSuperPropertyAndPeoplePropertyIncrementor(String property) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions = new AnalyticsTrackerMixpanelInstructionsForStat();
        instructions.setSuperPropertyAndPeoplePropertyToIncrement(property);
        return instructions;
    }

    public static AnalyticsTrackerMixpanelInstructionsForStat mixpanelInstructionsWithPropertyIncrementor(
            String property, AnalyticsTracker.Stat stat) {
        AnalyticsTrackerMixpanelInstructionsForStat instructions = new AnalyticsTrackerMixpanelInstructionsForStat();
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

    public AnalyticsTracker.Stat getStatToAttachProperty() {
        return mStatToAttachProperty;
    }

    public void setStatToAttachProperty(AnalyticsTracker.Stat statToAttachProperty) {
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

    public AnalyticsTracker.Stat getStat() {
        return mStat;
    }

    public void setStat(AnalyticsTracker.Stat stat) {
        this.mStat = stat;
    }

    public ArrayList<String> getSuperPropertiesToFlag() {
        return mSuperPropertiesToFlag;
    }

    public void addSuperPropertyToFlag(String superPropertyToFlag) {
        if (!mSuperPropertiesToFlag.contains(superPropertyToFlag)) {
            mSuperPropertiesToFlag.add(superPropertyToFlag);
        }
    }

    private static final ThreadLocal<DateFormat> AnalyticsDateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };

    public void setCurrentDateForPeopleProperty(String property) {
        setPeoplePropertyToValue(property, AnalyticsDateFormat.get().format(new Date()));
    }

    public void setPeoplePropertyToValue(String property, Object value) {
        mPeoplePropertiesToAssign.put(property, value);
    }

    public Map<String, Object> getPeoplePropertiesToAssign() {
       return mPeoplePropertiesToAssign;
    }
}
