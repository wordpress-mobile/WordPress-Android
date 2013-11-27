package org.wordpress.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Config;
import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.prefs.ReaderPrefs;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.quantcast.measurement.service.QuantcastClient;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;


/**
 * Created by Eric on 11/20/13.
 *
 * WPMobileStatsUtils handles stats logging. Its public methods should for the most part be service
 * neutral to allow to ease possible changes in the stats libs we use in the future.
 *
 * WPMobileStatsUtils is not thread safe so only call it from the main thread.
 *
 */
public class WPMobileStatsUtil {

    /* Events */

    public static final String StatsEventAppOpened = "Application Opened";

    // Posts
    public static final String StatsEventPostsOpened = "Posts - Opened";
    public static final String StatsEventPostsClosed= "Posts - Closed";
    public static final String StatsEventPostsClickedNewPost = "Posts - Clicked New Post";

    // Post Detail
    public static final String StatsPropertyPostMenuClickedEdit = "menu_clicked_edit";
    public static final String StatsPropertyPostMenuClickedComment = "menu_clicked_comment";
    public static final String StatsPropertyPostMenuClickedShare = "menu_clicked_share";
    public static final String StatsPropertyPostMenuClickedPreview = "menu_clicked_preview";
    public static final String StatsPropertyPostMenuClickedDelete = "menu_clicked_delete";
    public static final String StatsPropertyPostDetailClickedEdit = "clicked_edit";
    public static final String StatsPropertyPostDetailClickedComment = "clicked_comment";
    public static final String StatsPropertyPostDetailClickedShare = "clicked_share";
    public static final String StatsPropertyPostDetailClickedPreview = "clicked_preview";
    public static final String StatsPropertyPostDetailClickedDelete = "clicked_delete";
    public static final String StatsPropertyPostDetailClickedShowCategories = "clicked_show_categories";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarBoldButton = "clicked_keyboard_toolbar_bold_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarItalicButton = "clicked_keyboard_toolbar_italic_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarUnderlineButton = "clicked_keyboard_toolbar_underline_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarLinkButton = "clicked_keyboard_toolbar_link_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarBlockquoteButton = "clicked_keyboard_toolbar_blockquote_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarDelButton = "clicked_keyboard_toolbar_del_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarMoreButton = "clicked_keyboard_toolbar_more_button";
    public static final String StatsPropertyPostDetailClickedKeyboardToolbarPictureButton = "clicked_keyboard_toolbar_picture_button";
    public static final String StatsPropertyPostDetailClickedUpdate = "clicked_update_button";
    public static final String StatsPropertyPostDetailClickedPublish = "clicked_publish_button";

    public static final String StatsEventPostDetailOpenedEditor = "Post - Opened Editor";
    public static final String StatsEventPostDetailClosedEditor = "Post - Closed Editor";

    // Post Detail - Settings
    public static final String StatsPropertyPostDetailSettingsClickedStatus = "settings_clicked_status";
    public static final String StatsPropertyPostDetailSettingsClickedScheduleFor = "settings_clicked_schedule_for";
    public static final String StatsPropertyPostDetailSettingsClickedPostFormat = "settings_clicked_post_format";
    public static final String StatsPropertyPostDetailSettingsClickedAddLocation = "settings_clicked_add_location";
    public static final String StatsPropertyPostDetailSettingsClickedUpdateLocation = "settings_clicked_update_location";
    public static final String StatsPropertyPostDetailSettingsClickedRemoveLocation = "settings_clicked_remove_location";

    // Pages
    public static final String StatsPropertyPagesOpened = "pages_opened";
    public static final String StatsEventPagesOpened = "Pages - Opened";
    public static final String StatsEventPagesClosed= "Pages - Closed";
    public static final String StatsEventPagesClickedNewPage = "Pages - Clicked New Page";
    public static final String StatsEventPageDetailOpenedEditor = "Page - Opened Editor";
    public static final String StatsEventPageDetailClosedEditor = "Page - Closed Editor";

    /* /Events  */

    private static final WPMobileStatsUtil instance = new WPMobileStatsUtil();

    private static MixpanelAPI mixpanel;
    private HashMap<String, JSONObject> aggregatedProperties;

    /*
        Singleton
     */
    public static WPMobileStatsUtil getInstance() {
        return instance;
    }

    private WPMobileStatsUtil(){
        aggregatedProperties = new HashMap<String, JSONObject>();
    }

    /*
        Sets up and configures some session wide tracking.
        Should be called when the application is launched and at sign in/out.
     */
    public static void initialize() {
        mixpanel = MixpanelAPI.getInstance(WordPress.getContext(), Config.MIXPANEL_TOKEN);

        // Tracking session count will help us isolate users who just installed the app
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt("sessionCount", 0);
        sessionCount++;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("sessionCount", sessionCount);
        editor.commit();

        // Register super properties
        boolean connected = WordPress.hasValidWPComCredentials(WordPress.getContext());
        int numBlogs = WordPress.wpDB.getAccounts().size();
        try {
            JSONObject properties = new JSONObject();
            properties.put("platform", "Android");
            properties.put("session_count", sessionCount);
            properties.put("connected_to_dotcom", connected);
            properties.put("number_of_blogs", numBlogs);
            mixpanel.registerSuperProperties(properties);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        // Application opened and start.
        if (connected) {
            String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            mixpanel.identify(username);
            mixpanel.getPeople().increment("Application Opened", 1);

            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("$username", username);
                jsonObj.put("$first_name", username);
                mixpanel.getPeople().set(jsonObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // WPCom Stats

    /*
        Bump WordPress.com stats.

        @param statName The name of the stat to bump.
     */
    public static void pingWPComStats(String statName) {
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                String errMsg = String.format("Error pinging WPCom Stats: %s", volleyError.getMessage());
                Log.w("WORDPRESS", errMsg);
            }
        };

        int rnd = (int)(Math.random() * 100000);
        String statsURL = String.format("%s%s%s%s%d", Constants.readerURL_v3, "&template=stats&stats_name=", statName, "&rnd=", rnd );
        StringRequest req = new StringRequest(Request.Method.GET, statsURL, null, errorListener);
        WordPress.requestQueue.add(req);
    }

    // Quantcast

    /*
        Begin tracking an activity.  This should be called from an activity's onStart method.

        @param context The activity being tracked.
     */
    public static void resumeSession(Context context) {
        String userId = "";
        try {
            String wpcomId = String.format("%d%n", ReaderPrefs.getCurrentUserId());
            userId = new String(MessageDigest.getInstance("md5").digest(wpcomId.getBytes()));
        } catch( NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        QuantcastClient.activityStart(context, Config.QUANTCAST_KEY, userId, null);
    }

    /*
        Stop tracking an activity.
     */
    public static void endSession() {
        QuantcastClient.activityStop();
    }

    /*
        Log an activities event. resumeSession must be called prior to calling logQuantCastEvent.

        @param event  The name of the event to log.
     */
    public static void logQuantcastEvent(String event) {
        QuantcastClient.logEvent(event);
    }

    // Generic

    /*
        Track an event for WordPress self-hosted and .com users.

        @param event One of the constants representing the event to track.
     */
    public static void trackEventForSelfHostedAndWPCom(String event) {
        WPMobileStatsUtil.instance.track(false, event);
    }

    /*
        Track an event for WordPress self-hosted and .com users.

        @param event One of the constants representing the event to track.
        @param properties JSONObject of keys/values to track. Valid properties are boolean, integer and strings.
     */
    public static void trackEventForSelfHostedAndWPCom(String event, JSONObject properties) {
        WPMobileStatsUtil.instance.track(false, event, properties);
    }

    /*
        Track an event for WordPress self-hosted and .com users.

        @param event One of the constants representing the event to track.
     */
    public static void trackEventForSelfHostedAndWPComWithSavedProperties(String event) {
        WPMobileStatsUtil.instance.trackWithSavedProperties(false, event);
    }

    /*
        Track an event WordPress only for .com users.

        @param event One of the constants representing the event to track.
     */
    public static void trackEventForWPCom(String event) {
        WPMobileStatsUtil.instance.track(true, event);
    }

    /*
        Track an event WordPress only for .com users.

        @param event One of the constants representing the event to track.
        @param properties JSONObject of keys/values to track. Valid properties are boolean, integer and strings.
     */
    public static void trackEventForWPCom(String event, JSONObject properties) {
        WPMobileStatsUtil.instance.track(true, event, properties);
    }

    /*
        Track an event WordPress only for .com users.

        @param event One of the constants representing the event to track.
     */
    public static void trackEventForWPComWithSavedProperties( String event ) {
        WPMobileStatsUtil.instance.trackWithSavedProperties(true, event);
    }

    /*
        Clears saved properties for the current session.
     */
    public static void clearPropertiesForAllEvents() {
        WPMobileStatsUtil.instance.clearProperties();
    }

    /*
        Increments the specified property.  If the property does not exist it is created.
        A subsequent call to trackEventWithSavedProperties or trackEventForWPComWithSavedProperties
        should be made to record the property with the stats service.

        @param event One of the constants representing the event to track.
        @param property The property to increment.
     */
    public static void incrementProperty( String event, String property ) {
        WPMobileStatsUtil.instance.increment(event, property);
    }

    /*
        Flags the specified property.  If the property does not exist it is created.
        A subsequent call to trackEventWithSavedProperties or trackEventForWPComWithSavedProperties
        should be made to record the property with the stats service.

        @param event One of the constants representing the event to track.
        @param property The property to flag.
     */
    public static void flagProperty( String event, String property ) {
        WPMobileStatsUtil.instance.flag(event, property);
    }


    // Instance methods

    private boolean connectedToWPCom() {
        return WordPress.hasValidWPComCredentials(WordPress.getContext());
    }

    private void track(boolean isWPCom, String event) {
        if (isWPCom && !connectedToWPCom()) {
            return;
        }
        mixpanel.track(event, null);
    }

    private void track(boolean isWPCom, String event, JSONObject properties) {
        if (isWPCom && !connectedToWPCom()) {
            return;
        }
        mixpanel.track(event, properties);
    }

    private void trackWithSavedProperties(boolean isWPCom, String event) {
        if (isWPCom && !connectedToWPCom()) {
            return;
        }

        JSONObject properties = aggregatedProperties.get(event);
        if (properties == null) {
            properties = new JSONObject();
            aggregatedProperties.put(event, properties);
        }
        mixpanel.track(event, properties);
        mixpanel.flush();
    }

    private void clearProperties() {
        aggregatedProperties.clear();
    }

    /*
        Increments the specified property, for the specified event.

        @param event One of the constants representing the event to track.
        @param property The name of the property.
     */
    private void increment(String event, String property) {
        Integer count = (Integer)propertyForEvent(event, property);
        count++;
        saveProperty(event, property, count);
    }

    /*
        Flags the specified property as true, for the specified event.

        @param event One of the constants representing the event to track.
        @param property The name of the property.
     */
    private void flag(String event, String property) {
        saveProperty(event, property, true);
    }


    /*
        Returns the value of the specified property for the specified event.
        If the property does not exist 0 is returned.
        Return values are a boxed primitive of type boolean, integer, or string.

        @param event One of the constants representing the event to track.
        @param property The name of the property.
     */
    private Object propertyForEvent(String event, String property) {
        JSONObject properties = aggregatedProperties.get(event);
        Object val = 0;
        try {
            val = properties.get(property);
        } catch (JSONException e) {
            // The key didn't exist. No worries, use the default of 0.
        }
        return val;
    }

    /*
        Save locally the specified property and value for the specified event.

        @param event One of the constants representing the event to track.
        @param property The name of the property.
        @param value A boxed primitive of type boolean, integer or string.
     */
    private void saveProperty(String event, String property, Object value) {
        JSONObject properties = aggregatedProperties.get(event);
        if (properties == null) {
            properties = new JSONObject();
            aggregatedProperties.put(event, properties);
        }
        try {
            properties.put(property, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject propertiesForEvent(String event) {
        return aggregatedProperties.get(event);
    }

}
