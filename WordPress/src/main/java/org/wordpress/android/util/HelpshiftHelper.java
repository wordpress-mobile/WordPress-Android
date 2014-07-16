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
import java.util.List;
import java.util.Map;

public class HelpshiftHelper {
    private static HelpshiftHelper mInstance = null;
    private static Application mApplication = null;
    private static HashMap mMetadata = new HashMap();

    public enum TAGS {
        LOGIN_SCREEN("login-screen"),
        SETTINGS_SCREEN("settings-screen");

        private final String mStringValue;

        private TAGS(final String stringValue) {
            mStringValue = stringValue;
        }

        public String toString() {
            return mStringValue;
        }

        public static String[] toString(TAGS[] tags) {
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

    public void showConversation(Activity activity) {
        Helpshift.setNameAndEmail("", UserEmail.getPrimaryEmail(activity));
        setDefaultMetaData(activity);
        Helpshift.setMetadataCallback(new HSCallable() {
            public HashMap call() {
                return mMetadata;
            }
        });
        Helpshift.showConversation(activity);
    }

    public void registerDeviceToken(Context context, String regId) {
        if (!TextUtils.isEmpty(regId)) {
            Helpshift.registerDeviceToken(context, regId);
        }
    }

    public void handlePush(Context context, Intent intent) {
        Helpshift.handlePush(context, intent);
    }
}
