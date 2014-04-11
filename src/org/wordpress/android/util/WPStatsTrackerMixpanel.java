package org.wordpress.android.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Config;
import org.wordpress.android.WordPress;

public class WPStatsTrackerMixpanel implements WPStats.Tracker {

    private MixpanelAPI mMixpanel;

    @Override
    public void track(WPStats.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(WPStats.Stat stat, JSONObject properties) {
        WPStatsTrackerMixpanelInstructionsForStat instructions = instructionsForStat(stat);

        if (stat == null)
            return;

        trackMixpanelDataForInstructions(instructions, properties);
    }

    private void trackMixpanelDataForInstructions(WPStatsTrackerMixpanelInstructionsForStat instructions, JSONObject properties) {
        String eventName = instructions.getMixpanelEventName();
        if (eventName != null && !eventName.isEmpty()) {
            mMixpanel.track(eventName, properties);
        }
    }

    @Override
    public void beginSession() {
        mMixpanel = MixpanelAPI.getInstance(WordPress.getContext(), Config.MIXPANEL_TOKEN);

        // Tracking session count will help us isolate users who just installed the app
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt("sessionCount", 0);
        sessionCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("sessionCount", sessionCount);
        editor.commit();

        // Register super properties
        boolean connected = WordPress.hasValidWPComCredentials(WordPress.getContext());
        int numBlogs = WordPress.wpDB.getVisibleAccounts().size();
        try {
            JSONObject properties = new JSONObject();
            properties.put("platform", "Android");
            properties.put("session_count", sessionCount);
            properties.put("connected_to_dotcom", connected);
            properties.put("number_of_blogs", numBlogs);
            mMixpanel.registerSuperProperties(properties);
        } catch(JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        // Application opened and start.
        if (connected) {
            String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            mMixpanel.identify(username);
            mMixpanel.getPeople().increment("Application Opened", 1);

            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("$username", username);
                jsonObj.put("$first_name", username);
                mMixpanel.getPeople().set(jsonObj);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.UTILS, e);
            }
        }
    }

    @Override
    public void endSession() {
        System.out.println("Ending Session for Mixpanel");
    }

    private WPStatsTrackerMixpanelInstructionsForStat instructionsForStat(WPStats.Stat stat)
    {
        WPStatsTrackerMixpanelInstructionsForStat instructions = null;
        switch (stat) {
            case APPLICATION_OPENED:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Application Opened");
                break;
            case APPLICATION_CLOSED:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Application Closed");
                break;
            case THEMES_ACCESSED_THEMES_BROWSER:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Themes - Accessed Theme Browser");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_theme_browser");
                break;
            case THEMES_CHANGED_THEME:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Themes - Changed Theme");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_changed_theme");
                break;
            case READER_ACCESSED:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Accessed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_accessed_reader");
                break;
            case READER_OPENED_ARTICLE:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Opened Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_opened_article");
                break;
            case READER_LIKED_ARTICLE:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Liked Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_liked_article");
                break;
            case READER_REBLOGGED_ARTICLE:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Reblogged Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_reblogged_article");
                break;
            case READER_INFINITE_SCROLL:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Infinite Scroll");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_reader_performed_infinite_scroll");
                break;
            case READER_FOLLOWED_READER_TAG:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Followed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_followed_reader_tag");
                break;
            case READER_UNFOLLOWED_READER_TAG:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Unfollowed Reader Tag");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_unfollowed_reader_tag");
                break;
            case READER_LOADED_TAG:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Loaded Tag");
                break;
            case READER_LOADED_FRESHLY_PRESSED:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Loaded Freshly Pressed");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_loaded_freshly_pressed");
                break;
            case READER_COMMENTED_ON_ARTICLE:
                instructions = WPStatsTrackerMixpanelInstructionsForStat.mixpanelInstructionsForEventName("Reader - Commented on Article");
                instructions.setSuperPropertyAndPeoplePropertyToIncrement("number_of_times_commented_on_article");
                break;
        }
        return instructions;
    }
}
