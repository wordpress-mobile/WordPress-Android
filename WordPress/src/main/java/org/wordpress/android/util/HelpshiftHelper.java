package org.wordpress.android.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.helpshift.Helpshift;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;
import java.util.Map;

public class HelpshiftHelper {
    private static HelpshiftHelper mInstance = null;
    private static Application mApplication = null;
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
        LOGIN_SCREEN("login-screen"),
        SETTINGS_SCREEN("settings-screen");

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
            if (mApplication == null) {
                AppLog.e(T.UTILS, "You must call HelpshiftHelper.init(Application application) before getInstance()");
            }
            mInstance = new HelpshiftHelper();
            Helpshift.install(mApplication, BuildConfig.HELPSHIFT_API_KEY, BuildConfig.HELPSHIFT_API_DOMAIN,
                    BuildConfig.HELPSHIFT_API_ID);
        }
        return mInstance;
    }

    public static void init(Application application) {
        mApplication = application;
    }

    /**
     * Show conversation activity
     * Automatically add default metadata to this conversation
     */
    public void showConversation(Activity activity) {
        String emailAddress = UserEmail.getPrimaryEmail(activity);
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
        addDefaultMetaData(activity);
        HashMap config = new HashMap ();
        config.put(Helpshift.HSCustomMetadataKey, mMetadata);
        Helpshift.showConversation(activity, config);
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
        StringBuilder blogList = new StringBuilder();
        for (Map<String, Object> account : WordPress.wpDB.getAllAccounts()) {
            blogList.append(MapUtils.getMapStr(account, "blogName"));
            blogList.append(": ");
            blogList.append(MapUtils.getMapStr(account, "url"));
            blogList.append("\n");
        }
        mMetadata.put("blogs", blogList.toString());

        // wpcom user
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        mMetadata.put("wpcom-username", username);
    }
}
