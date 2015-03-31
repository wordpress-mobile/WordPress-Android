package org.wordpress.android.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.helpshift.Helpshift;

import org.apache.commons.lang.ArrayUtils;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HelpshiftHelper {
    public static String ORIGIN_KEY = "ORIGIN_KEY";
    private static String HELPSHIFT_SCREEN_KEY = "helpshift_screen";
    private static String HELPSHIFT_ORIGIN_KEY = "origin";
    private static HelpshiftHelper mInstance = null;
    private static HashMap<String, Object> mMetadata = new HashMap<String, Object>();

    public enum MetadataKey {
        USER_ENTERED_URL("user-entered-url"),
        USER_ENTERED_USERNAME("user-entered-username");

        private final String mStringValue;

        private MetadataKey(final String stringValue) {
            mStringValue = stringValue;
        }

        public String toString() {
            return mStringValue;
        }
    }

    public enum Tag {
        ORIGIN_UNKNOWN("origin:unknown"),
        ORIGIN_LOGIN_SCREEN_HELP("origin:login-screen-help"),
        ORIGIN_LOGIN_SCREEN_ERROR("origin:login-screen-error"),
        ORIGIN_SETTINGS_SCREEN_HELP("origin:settings-screen-help");

        private final String mStringValue;

        private Tag(final String stringValue) {
            mStringValue = stringValue;
        }

        public String toString() {
            return mStringValue;
        }

        public static String[] toString(Tag[] tags) {
            if (tags == null) {
                return null;
            }
            String[] res = new String[tags.length];
            for (int i = 0; i < res.length; i++) {
                res[0] = tags[0].toString();
            }
            return res;
        }
    }

    private HelpshiftHelper() {
    }

    public static synchronized HelpshiftHelper getInstance() {
        if (mInstance == null) {
            mInstance = new HelpshiftHelper();
        }
        return mInstance;
    }

    public static void init(Application application) {
        HashMap<String, Boolean> config = new HashMap<String, Boolean>();
        config.put("enableInAppNotification", false);
        Helpshift.install(application, BuildConfig.HELPSHIFT_API_KEY, BuildConfig.HELPSHIFT_API_DOMAIN,
                BuildConfig.HELPSHIFT_API_ID, config);
    }

    /**
     * Show conversation activity
     * Automatically add default metadata to this conversation
     */
    public void showConversation(Activity activity, Tag origin) {
        if (origin == null) {
            origin = Tag.ORIGIN_UNKNOWN;
        }
        // track origin and helpshift screen in analytics
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(HELPSHIFT_SCREEN_KEY, "conversation");
        properties.put(HELPSHIFT_ORIGIN_KEY, origin.toString());
        AnalyticsTracker.track(Stat.SUPPORT_OPENED_HELPSHIFT_SCREEN, properties);
        // Add tags to Helpshift metadata
        addTags(new Tag[]{origin});
        HashMap config = getHelpshiftConfig(activity);
        Helpshift.showConversation(activity, config);
    }

    /**
     * Show FAQ activity
     * Automatically add default metadata to this conversation (users can start a conversation from FAQ screen).
     */
    public void showFAQ(Activity activity, Tag origin) {
        if (origin == null) {
            origin = Tag.ORIGIN_UNKNOWN;
        }
        // track origin and helpshift screen in analytics
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(HELPSHIFT_SCREEN_KEY, "faq");
        properties.put(HELPSHIFT_ORIGIN_KEY, origin.toString());
        AnalyticsTracker.track(Stat.SUPPORT_OPENED_HELPSHIFT_SCREEN, properties);
        // Add tags to Helpshift metadata
        addTags(new Tag[]{origin});
        HashMap config = getHelpshiftConfig(activity);
        Helpshift.showFAQs(activity, config);
    }

    /**
     * Register a GCM device token to Helpshift servers
     *
     * @param regId registration id
     */
    public void registerDeviceToken(Context context, String regId) {
        if (!TextUtils.isEmpty(regId)) {
            Helpshift.registerDeviceToken(context, regId);
        }
    }

    public void setTags(Tag[] tags) {
        mMetadata.put(Helpshift.HSTagsKey, Tag.toString(tags));
    }

    public void addTags(Tag[] tags) {
        String[] oldTags = (String[]) mMetadata.get(Helpshift.HSTagsKey);
        // Concatenate arrays
        mMetadata.put(Helpshift.HSTagsKey, ArrayUtils.addAll(oldTags, Tag.toString(tags)));
    }

    /**
     * Handle push notification
     */
    public void handlePush(Context context, Intent intent) {
        Helpshift.handlePush(context, intent);
    }

    /**
     * Add metadata to Helpshift conversations
     *
     * @param key map key
     * @param object to store. Be careful with the type used. Nothing is specified in the documentation. Better to use
     *               String but String[] is needed for specific key like Helpshift.HSTagsKey
     */
    public void addMetaData(MetadataKey key, Object object) {
        mMetadata.put(key.toString(), object);
    }

    public Object getMetaData(MetadataKey key) {
        return mMetadata.get(key.toString());
    }

    private void addDefaultMetaData(Context context) {
        // Use plain text log (unfortunately Helpshift can't display this correctly)
        mMetadata.put("log", AppLog.toPlainText(context));

        // List blogs name and url
        int counter = 1;
        for (Map<String, Object> account : WordPress.wpDB.getAllAccounts()) {
            mMetadata.put("blog-name-" + counter, MapUtils.getMapStr(account, "blogName"));
            mMetadata.put("blog-url-" + counter, MapUtils.getMapStr(account, "url"));
            counter += 1;
        }

        // wpcom user
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        mMetadata.put("wpcom-username", username);
    }

    private HashMap getHelpshiftConfig(Context context) {
        String emailAddress = UserEmailUtils.getPrimaryEmail(context);
        // Use the user entered username to pre-fill name
        String name = (String) getMetaData(MetadataKey.USER_ENTERED_USERNAME);
        // If it's null or empty, use split email address to pre-fill name
        if (TextUtils.isEmpty(name)) {
            String[] splitEmail = TextUtils.split(emailAddress, "@");
            if (splitEmail.length >= 1) {
                name = splitEmail[0];
            }
        }
        Helpshift.setNameAndEmail(name, emailAddress);
        addDefaultMetaData(context);
        HashMap config = new HashMap ();
        config.put(Helpshift.HSCustomMetadataKey, mMetadata);
        return config;
    }
}
