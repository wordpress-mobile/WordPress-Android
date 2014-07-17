package org.wordpress.android.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.helpshift.Helpshift;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.util.AppLog.T;

public class HelpshiftHelper {
    private static HelpshiftHelper mInstance = null;
    private static Application mApplication = null;

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
        Helpshift.showConversation(activity);
    }

    public void showFAQ(Activity activity) {
        Helpshift.showFAQs(activity);
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
