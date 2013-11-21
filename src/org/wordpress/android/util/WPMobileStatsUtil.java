package org.wordpress.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.prefs.ReaderPrefs;
import org.xmlrpc.android.ApiHelper;

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
 */
public class WPMobileStatsUtil {

    /* Events */

    // Posts
    public static final String StatsPropertyPostsOpened = "posts_opened";
    public static final String StatsEventPostsClickedNewPost = "Posts - Clicked New Post";

    // Post Detail
    public static final String StatsPropertyPostDetailClickedEdit = "clicked_edit";
    public static final String StatsPropertyPostDetailClickedSettings = "clicked_settings";
    public static final String StatsPropertyPostDetailClickedMedia = "clicked_media";
    public static final String StatsPropertyPostDetailClickedPreview = "clicked_preview";
    public static final String StatsPropertyPostDetailClickedMediaOptions = "clicked_media_options";
    public static final String StatsPropertyPostDetailClickedAddVideo = "clicked_add_video";
    public static final String StatsPropertyPostDetailClickedAddPhoto = "clicked_add_photo";
    public static final String StatsPropertyPostDetailClickedShowCategories = "clicked_show_categories";
    public static final String StatsEventPostDetailClickedKeyboardToolbarBoldButton = "clicked_keyboard_toolbar_bold_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarItalicButton = "clicked_keyboard_toolbar_italic_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarLinkButton = "clicked_keyboard_toolbar_link_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarBlockquoteButton = "clicked_keyboard_toolbar_blockquote_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarDelButton = "clicked_keyboard_toolbar_del_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarUnorderedListButton = "clicked_keyboard_toolbar_unordered_list_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarOrderedListButton = "clicked_keyboard_toolbar_ordered_list_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarListItemButton = "clicked_keyboard_toolbar_list_item_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarCodeButton = "clicked_keyboard_toolbar_code_button";
    public static final String StatsEventPostDetailClickedKeyboardToolbarMoreButton = "clicked_keyboard_toolbar_more_button";
    public static final String StatsEventPostDetailAddedPhoto = "Added Photo";
    public static final String StatsEventPostDetailRemovedPhoto = "Removed Photo";
    public static final String StatsEventPostDetailClickedSchedule = "Clicked Schedule Button";
    public static final String StatsEventPostDetailClickedSave = "Clicked Save Button";
    public static final String StatsEventPostDetailClickedUpdate = "Clicked Update Button";
    public static final String StatsEventPostDetailClickedPublish = "Clicked Publish Button";
    public static final String StatsEventPostDetailOpenedEditor = "Opened Editor";
    public static final String StatsEventPostDetailClosedEditor = "Closed Editor";

    // Post Detail - Settings
    public static final String StatsPropertyPostDetailSettingsClickedStatus = "settings_clicked_status";
    public static final String StatsPropertyPostDetailSettingsClickedVisibility = "settings_clicked_visibility";
    public static final String StatsPropertyPostDetailSettingsClickedScheduleFor = "settings_clicked_schedule_for";
    public static final String StatsPropertyPostDetailSettingsClickedPostFormat = "settings_clicked_post_format";
    public static final String StatsEventPostDetailSettingsClickedSetFeaturedImage = "Settings - Clicked Set Featured Image";
    public static final String StatsEventPostDetailSettingsClickedRemoveFeaturedImage = "Settings - Clicked Remove Featured Image";
    public static final String StatsEventPostDetailSettingsClickedAddLocation = "Settings - Clicked Add Location";
    public static final String StatsEventPostDetailSettingsClickedUpdateLocation = "Settings - Clicked Update Location";
    public static final String StatsEventPostDetailSettingsClickedRemoveLocation = "Settings - Clicked Remove Location";

    // Pages
    public static final String StatsPropertyPagedOpened = "pages_opened";
    public static final String StatsEventPagesClickedNewPage = "Pages - Clicked New Page";

    /* /Events  */

    private static final String MIXPANEL_TOKEN = "mixpanel.token";
    private static final String QUANTCAST_KEY = "quantcast.key";
    private static final WPMobileStatsUtil instance = new WPMobileStatsUtil();

    private static MixpanelAPI mixpanel;
    private HashMap<String, JSONObject> aggregatedProperties;

    private WPMobileStatsUtil(){
        aggregatedProperties = new HashMap<String, JSONObject>();
    }

    public static WPMobileStatsUtil getInstance() {
        return instance;
    }


    public static void initialize() {
        mixpanel = MixpanelAPI.getInstance(WordPress.getContext(), MIXPANEL_TOKEN);

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
            properties.put("session_count", sessionCount);
            properties.put("connected_to_dotcom", connected);
            properties.put("number_of_blogs", numBlogs);
            mixpanel.registerSuperProperties(properties);
        } catch(JSONException e) {
            //
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
                //
            }
        }
    }

    // WPCom Stats

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

    public static void resumeSession(Context context) {
        String userId = "";
        try {
            String wpcomId = String.format("%d%n", ReaderPrefs.getCurrentUserId());
            userId = new String(MessageDigest.getInstance("md5").digest(wpcomId.getBytes()));
        } catch( NoSuchAlgorithmException e) {
            //
        }

        QuantcastClient.activityStart(context, QUANTCAST_KEY, userId, null);
    }

    public static void endSession() {
        QuantcastClient.activityStop();
    }

    public static void logQuantcastEvent(String event) {
        QuantcastClient.logEvent(event);
    }

    // Generic

    public static void trackEventForSelfHostedAndWPCom( String event ) {
        WPMobileStatsUtil.instance.track(false, event);
    }

    public static void trackEventForSelfHostedAndWPCom( String event, JSONObject properties ) {
        WPMobileStatsUtil.instance.track(false, event, properties);
    }

    public static void trackEventForSelfHostedAndWPComWithSavedProperties( String event ) {
        WPMobileStatsUtil.instance.trackWithSavedProperties(false, event);
    }

    public static void trackEventForWPCom( String event ) {
        WPMobileStatsUtil.instance.track(true, event);
    }

    public static void trackEventForWPCom( String event, JSONObject properties ) {
        WPMobileStatsUtil.instance.track(true, event, properties);
    }

    public static void trackEventForWPComWithSavedProperties( String event ) {
        WPMobileStatsUtil.instance.trackWithSavedProperties(true, event);
    }

    public static void clearPropertiesForAllEvents() {
        WPMobileStatsUtil.instance.clearProperties();
    }

    public static void incrementProperty( String property, String event ) {
        WPMobileStatsUtil.instance.increment(property, event);
    }

    public static void flagProperty( String property, String event ) {
        WPMobileStatsUtil.instance.flag(property, event);
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
    }

    private void clearProperties() {
        aggregatedProperties.clear();
    }

    private void increment(String event, String property) {
        Integer count = (Integer)propertyForEvent(event, property);
        count++;
        saveProperty(event, property, count);
    }

    private void flag(String event, String property) {
        saveProperty(event, property, true);
    }

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

    private void saveProperty(String event, String property, Object value) {
        JSONObject properties = aggregatedProperties.get(event);
        if (properties == null) {
            properties = new JSONObject();
            aggregatedProperties.put(event, properties);
        }
        try {
            properties.put(property, value);
        } catch (JSONException e) {
            //
        }
    }

    private JSONObject propertiesForEvent(String event) {
        return aggregatedProperties.get(event);
    }

}
